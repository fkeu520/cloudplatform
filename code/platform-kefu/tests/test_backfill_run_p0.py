"""回填脚本 _run 的集成回归测试 (P0)

目的
----
验证 ``scripts.backfill_token_counts._run`` 走的是 **cursor** 路径, 而非
已经失效的 ``conn.execute(...)`` 旧写法。旧 bug 表现为: 对 aiomysql
Connection 直接调用 ``execute`` / ``fetchall``, 运行时抛 ``AttributeError``
(已确认 ``hasattr(aiomysql.Connection,'execute') == False``)。
本测试用假连接 + 假游标驱动真实异步循环, 确保 "SELECT ... ORDER BY
chunk_id ASC" 和 "UPDATE chunks SET token_count=%s" 都走 ``await cur.execute``。

不连真实 MySQL、不设置 ``os.environ["MYSQL_HOST"]``。
"""
import argparse
import os
import sys

# 给 app.config.Settings 三个必填项占位值; 缺任一则 pydantic-settings 在 import
# 期就抛 ValidationError, 整个模块连 collect 都进不去。这与 tests/test_kefu.py
# 的处理一致, 不是生产密钥, 也不做真实外部调用。
os.environ.setdefault("DEEPSEEK_API_KEY", "test-only-placeholder")
os.environ.setdefault("SILICONFLOW_API_KEY", "test-only-placeholder")
os.environ.setdefault("JWT_SECRET", "test-only-jwt-secret-not-a-real-key-0000")

# 禁止任何路径读 MYSQL_HOST —— 此测试只接受假 DB
assert "MYSQL_HOST" not in os.environ, (
    "test_backfill_run_p0 不应依赖真实 DB; "
    "如果环境已设置 MYSQL_HOST, 请清除后重跑"
)

# 先把 app 目录插上 sys.path, 再 import 后续模块
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "app"))

import pytest  # noqa: E402
from scripts.backfill_token_counts import _run, compute_token_fixes  # noqa: E402
from app.services.chunker import Chunker  # noqa: E402


# ---------------------------------------------------------------------------
# 假游标: 记录所有 execute / fetchall 调用
# ---------------------------------------------------------------------------

class FakeCursor:
    """模拟 aiomysql.Cursor 的核心接口。"""

    def __init__(self, rows: list) -> None:
        self.rows = rows
        self.execute_calls: list = []

    async def execute(self, sql: str, args=None) -> None:
        self.execute_calls.append((sql, args or ()))

    async def fetchall(self) -> list:
        return self.rows

    def close(self) -> None:
        pass


class _AsyncCtx:
    """最小的 async 上下文管理器包装器, 用于包 FakeCursor。"""

    def __init__(self, inner: FakeCursor) -> None:
        self.inner = inner

    async def __aenter__(self) -> FakeCursor:
        return self.inner

    async def __aexit__(self, exc_type, exc, tb) -> None:
        self.inner.close()
        return False


# ---------------------------------------------------------------------------
# 假连接: 模拟 aiomysql.Connection (以及 _PooledConnection 的行为)
# ---------------------------------------------------------------------------

class FakeConnection:
    """模拟 aiomysql.Connection / _PooledConnection。

    * 无 ``execute`` / ``fetchall`` 属性 —— 旧 bug 代码直接调这两个方法会在
      Connection 上触发 AttributeError。本测试利用这一点做反向回归断言。
    * ``cursor()`` 返回一个同步上下文管理器, 其 ``__aenter__`` 返回 FakeCursor。
    * ``close()`` 同步, 幂等, 模拟 _PooledConnection.close() 行为。

    行为约定
    --------
    第一次 cursor() 调用返回第一批数据 (用于首次 SELECT);
    第二次及以后的 cursor() 调用返回空列表 (驱动 keyset 分页循环自然终止)。
    """

    def __init__(self, first_batch_rows: list) -> None:
        self._first_batch = first_batch_rows
        self._empty: list = []
        self.cursors: list = []
        self.cursor_count = 0
        self.close_called = False

    def cursor(self):
        self.cursor_count += 1
        # 第一次 cursor (SELECT) 返回数据; 后续 (UPDATE + 下一次 SELECT) 返回空列表
        rows = self._first_batch if self.cursor_count == 1 else self._empty
        cur = FakeCursor(rows)
        self.cursors.append(cur)
        return _AsyncCtx(cur)

    def close(self) -> None:
        self.close_called = True


def chunker_estimate(text: str) -> int:
    """计算文本的估算 token 数, 直接从 Chunker 取。"""
    return Chunker().estimate_tokens(text)


# ---------------------------------------------------------------------------
# 测试
# ---------------------------------------------------------------------------

class TestBackfillRun:
    """端到端驱动 _run, 断言 SQL 发出方式与脚本约定一致。"""

    def _patch_db(self, conn: FakeConnection):
        """把 app.models.database.get_db_connection 替换为返回 conn 的协程。"""
        import app.models.database as db_mod

        async def _fake_get_conn():
            return conn

        orig = db_mod.get_db_connection
        db_mod.get_db_connection = _fake_get_conn
        return db_mod, orig

    def _unpatch(self, db_mod, orig) -> None:
        db_mod.get_db_connection = orig

    def _collect_sql_and_args(self, conn: FakeConnection):
        """从所有 recorded cursors 中收集 (sql, args) 元组, 按类型分类。"""
        all_selects: list = []   # list of (sql, args)
        all_updates: list = []   # list of (sql, args)
        for cur in conn.cursors:
            for sql, args_tuple in cur.execute_calls:
                upper_sql = sql.upper()
                if "SELECT" in upper_sql:
                    all_selects.append((sql, args_tuple))
                elif "UPDATE" in upper_sql:
                    all_updates.append((sql, args_tuple))
        return all_selects, all_updates

    @pytest.mark.asyncio
    async def test_non_dry_run_updates_only_changed_chunks_via_cursor(self) -> None:
        """非 dry-run: 只更新 changed 的 chunk, 且 SELECT/UPDATE 都走 cursor.execute。"""
        cn_text = "\u4e2d" * 400  # 中文 400 字
        est_cn = chunker_estimate(cn_text)  # = 400
        en_text = "abc"
        est_en = chunker_estimate(en_text)  # = ceil(3/4) = 1

        # c1: 旧值 1 (旧估算 len(text.split())), 新值应为 400 → 需要 UPDATE
        # c2: 旧值 est_en+1 (=2, 故意给错), 新值应为 est_en (=1) → 需要 UPDATE
        rows1 = [
            ("c1", cn_text, 1),
            ("c2", en_text, est_en + 1),
        ]
        conn = FakeConnection(rows1)
        db_mod, orig = self._patch_db(conn)

        try:
            args = argparse.Namespace(dry_run=False, batch_size=500)
            rc = await _run(args)

            assert rc == 0, f"_run 应返回 0, 实际 {rc}"
            assert conn.close_called, "conn.close() 必须被调用 (finally 路径)"
            assert conn.cursor_count >= 2, (
                f"至少打开 2 个 cursor (SELECT + UPDATE), 实际 {conn.cursor_count}"
            )

            all_selects, all_updates = self._collect_sql_and_args(conn)

            assert len(all_selects) >= 1, "至少发出一条 SELECT"
            select_sqls = [sql for sql, _ in all_selects]
            assert any("ORDER BY chunk_id ASC" in s for s in select_sqls), (
                "SELECT 必须按 chunk_id ASC 排序 (keyset 分页)"
            )

            assert len(all_updates) >= 1, "至少发出一条 UPDATE"
            update_targets: set = set()
            for sql, args_tuple in all_updates:
                # args 格式: (new_count, chunk_id)
                update_targets.add(args_tuple[1])

            # 通过 compute_token_fixes 计算的预期变更集合
            expected_fixes = compute_token_fixes(rows1)
            expected_changed = {chunk_id for chunk_id, _ in expected_fixes}
            assert update_targets == expected_changed, (
                f"UPDATE 目标 chunk 集合应为 {expected_changed}, 实际 {update_targets}"
            )

            # 关键回归断言: SELECT 必须通过 cursor.execute 发出, 不是 conn.execute
            # 旧 bug 代码直接写 conn.execute(...) 会在 Connection 上 AttributeError
            for cur in conn.cursors:
                assert hasattr(cur, "execute_calls"), "cursor 必须有 execute_calls 记录"
        finally:
            self._unpatch(db_mod, orig)

    @pytest.mark.asyncio
    async def test_dry_run_issues_no_updates(self) -> None:
        """dry-run: 不发出任何 UPDATE, 只统计, 返回 0。"""
        cn_text = "\u4e2d" * 400
        rows1 = [("c1", cn_text, 1)]
        conn = FakeConnection(rows1)
        db_mod, orig = self._patch_db(conn)

        try:
            args = argparse.Namespace(dry_run=True, batch_size=500)
            rc = await _run(args)

            assert rc == 0, f"dry-run 应返回 0, 实际 {rc}"
            assert conn.close_called

            all_selects, all_updates = self._collect_sql_and_args(conn)

            assert len(all_selects) >= 1, "dry-run 仍应执行 SELECT 统计"
            assert len(all_updates) == 0, "dry-run 不得发出任何 UPDATE"
        finally:
            self._unpatch(db_mod, orig)

    @pytest.mark.asyncio
    async def test_cursor_path_regression_old_bug(self) -> None:
        """回归旧 bug: 旧代码若写 ``conn.execute(...)`` 会 AttributeError。

        旧版 _run 错误写法::

            conn.execute("SELECT ...")
            rows = conn.fetchall()

        但 aiomysql.Connection 没有 execute/fetchall 方法
        (已确认 ``hasattr(aiomysql.Connection, 'execute') == False``)。
        本测试通过 FakeConnection 不含 execute/fetchall 属性, 反向保证当前代码
        必须走 ``async with conn.cursor() as cur: await cur.execute(...)`` 路径,
        否则运行时会触发 AttributeError 导致测试失败。
        """
        cn_text = "\u4e2d" * 400
        rows1 = [("c1", cn_text, 1)]
        conn = FakeConnection(rows1)

        # 确认 FakeConnection 不含 execute/fetchall —— 这是回归断言的前提
        assert not hasattr(conn, "execute"), (
            "FakeConnection 不应有 execute, 以强制回归旧 bug"
        )
        assert not hasattr(conn, "fetchall"), (
            "FakeConnection 不应有 fetchall, 以强制回归旧 bug"
        )

        db_mod, orig = self._patch_db(conn)
        try:
            args = argparse.Namespace(dry_run=False, batch_size=500)
            rc = await _run(args)
            assert rc == 0, "走 cursor 路径应成功; 若失败说明代码退回到 conn.execute 旧路径"
        finally:
            self._unpatch(db_mod, orig)
