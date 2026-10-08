"""ProcessingQueue 单测 (无真实 MySQL / MinIO / 网络) — FIX 版.

覆盖:
1. test_submit_returns_fast_and_worker_consumes        — start 后 submit, 等 worker 消费
2. test_queue_full_raises_busy                         — maxsize=1, 第二次 submit 抛 QueueBusy
3. test_worker_marks_failed_on_exception               — _process_document 抛异常时置 status='failed'
4. test_reconcile_requeues_processing                  — reconcile_processing_docs 从 DB 捞卡住行重入队

约定:
- 所有异步测试用 asyncio.run(coro) 包装, 不在 pytest-asyncio 事件循环里嵌套创建新 loop.
- 构造一个 reusable fake async DB connection (cursor 为 async context manager,
  execute/fetchone/fetchall 均为 awaitable). 拒绝 MagicMock 当 awaitable 用 (TypeError).
- 每个测试在 finally 中恢复被覆盖的全局 (app.services.processing_queue.queue).
- 不设置 os.environ["MYSQL_HOST"] —— 不污染其他模块的 skip 逻辑.
"""
from __future__ import annotations

import asyncio
import os
import sys
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

# ── env 占位 (与 test_kefu.py / test_processing_queue_start_p0.py 一致) ───────
os.environ.setdefault("JWT_SECRET", "test-only-jwt-secret-not-a-real-key-0000")
os.environ.setdefault("DEEPSEEK_API_KEY", "test-only-placeholder")
os.environ.setdefault("SILICONFLOW_API_KEY", "test-only-placeholder")
# 故意不设 MYSQL_HOST —— 不污染其他模块的 skip 逻辑.

# ── aiomysql stub (避免 import 期连库, 与 test_docs_async_p0.py 风格一致) ─────
if "aiomysql" not in sys.modules:
    try:
        import aiomysql  # noqa: F401
    except ModuleNotFoundError:
        _stub = type(sys)("aiomysql")
        class _Pool:  # noqa: D401
            pass
        class _Conn:  # noqa: D401
            pass
        _stub.Pool = _Pool
        _stub.Connection = _Conn
        def _no(*a, **kw):  # noqa: D403
            raise RuntimeError("aiomysql stub — do not connect")
        _stub.create_pool = _no
        sys.modules["aiomysql"] = _stub


# ============================================================================ #
# Fake DB helpers — 必须真支持 async with / await, 不能用 MagicMock 冒充 awaitable
# ============================================================================ #
class _FakeCursor:
    """支持 async with 的 cursor, execute/fetchone/fetchall 均为 True coroutine."""

    def __init__(
        self,
        fetchone_result=None,
        fetchall_result=None,
        execute_side_effect=None,
        execute_side_effects=None,
    ):
        self.fetchone_result = fetchone_result
        self.fetchall_result = fetchall_result if fetchall_result is not None else []
        self.execute_side_effect = execute_side_effect
        self.execute_side_effects = execute_side_effects or []
        self.execute_calls: list = []
        self._exec_idx = 0

    async def execute(self, sql, args=None):
        self.execute_calls.append((sql, args))
        if self.execute_side_effect:
            if callable(self.execute_side_effect):
                ret = self.execute_side_effect(sql, args)
                if asyncio.iscoroutine(ret):
                    return await ret
                return ret
            raise self.execute_side_effect
        if self.execute_side_effects:
            fn = self.execute_side_effects[self._exec_idx]
            self._exec_idx += 1
            ret = fn(sql, args)
            if asyncio.iscoroutine(ret):
                return await ret
            return ret
        return None

    async def fetchone(self):
        return self.fetchone_result

    async def fetchall(self):
        return self.fetchall_result


class _FakeCursorCM:
    def __init__(self, cursor):
        self._cursor = cursor

    async def __aenter__(self):
        return self._cursor

    async def __aexit__(self, exc_type, exc, tb):
        return False


class _FakeConn:
    """fake async DB 连接: cursor() 返回 async context manager, close() 同步."""

    def __init__(
        self,
        fetchone_result=None,
        fetchall_result=None,
        execute_side_effect=None,
        execute_side_effects=None,
    ):
        self.cursor_mock = _FakeCursor(
            fetchone_result=fetchone_result,
            fetchall_result=fetchall_result,
            execute_side_effect=execute_side_effect,
            execute_side_effects=execute_side_effects,
        )
        self._cursor_cm = _FakeCursorCM(self.cursor_mock)

    def cursor(self):
        return self._cursor_cm

    def close(self):
        pass


def make_fake_conn(**kwargs):
    return _FakeConn(**kwargs)


# ============================================================================ #
# 测试 1: submit 快速入队 + worker 实际消费
# ============================================================================ #
def test_submit_returns_fast_and_worker_consumes():
    """q.start() 后 submit 一条 job, worker 在队列里把它消费掉, 通过 record 判断."""

    async def _t():
        from app.services import processing_queue as pq_mod
        from app.services.processing_queue import ProcessingQueue

        q = ProcessingQueue()
        record = []

        # 替换 _process_document 为极简 fake —— 只记录参数并立即返回
        original = q._process_document

        async def _fake_process(**job):
            record.append(job)

        q._process_document = _fake_process

        q.start()
        try:
            q.submit({
                "doc_id": "d1",
                "tenant_id": 1,
                "object_name": "d1.txt",
                "file_content": b"hello",
                "filename": "d1.txt",
                "ext": ".txt",
            })
            # worker 在独立 task 里跑, 给一点时间消化
            for _ in range(50):
                await asyncio.sleep(0.02)
                if record:
                    break
            assert len(record) == 1, f"expected 1 recorded job, got {len(record)}"
            assert record[0]["doc_id"] == "d1"
        finally:
            q._process_document = original
            q.stop()
            # 确保全局 queue 单例没被污染
            pq_mod.queue = None

    asyncio.run(_t())


# ============================================================================ #
# 测试 2: 队列满 → submit 抛 QueueBusy
# ============================================================================ #
def test_queue_full_raises_busy():
    """maxsize=1, 先 submit 一条, 第二条抛 QueueBusy."""

    from app.services.processing_queue import ProcessingQueue, QueueBusy

    q = ProcessingQueue()
    q._queue = asyncio.Queue(maxsize=1)

    q.submit({"doc_id": "a", "tenant_id": 1, "object_name": "a.txt",
              "file_content": b"x", "filename": "a.txt", "ext": ".txt"})

    with pytest.raises(QueueBusy):
        q.submit({"doc_id": "b", "tenant_id": 1, "object_name": "b.txt",
                  "file_content": b"y", "filename": "b.txt", "ext": ".txt"})

    q.stop()


# ============================================================================ #
# 测试 3: worker 捕获异常 → 调 _mark_failed, DB 里有 status='failed'
# ============================================================================ #
def test_worker_marks_failed_on_exception():
    """_process_document 抛异常时, _mark_failed 会执行, 其 execute 调用含 status='failed'."""

    async def _t():
        from app.services import processing_queue as pq_mod
        from app.services.processing_queue import ProcessingQueue

        q = ProcessingQueue()

        # 让 _process_document 直接抛异常
        async def _boom(**job):
            raise RuntimeError("intentional worker fail")

        original = q._process_document
        q._process_document = _boom

        # fake conn: 记录 execute 调用, 并在 _mark_failed 的第二次 execute 里注入 'failed'
        failed_calls = []

        def _side_effect(sql, args=None):
            failed_calls.append((sql, args))
            if "status='failed'" in str(sql):
                return None
            return None

        fake_conn = make_fake_conn(execute_side_effect=_side_effect)

        # 把 processing_queue.get_db_connection 替换成返回 fake_conn 的 async 函数
        async def _fake_get_db():
            return fake_conn

        q.start()
        try:
            with patch("app.services.processing_queue.get_db_connection", _fake_get_db):
                q.submit({
                    "doc_id": "d-fail",
                    "tenant_id": 1,
                    "object_name": "d.txt",
                    "file_content": b"x",
                    "filename": "d.txt",
                    "ext": ".txt",
                })
                for _ in range(80):
                    await asyncio.sleep(0.02)
                    if failed_calls:
                        break
            # _mark_failed 至少执行了一次 execute, SQL 含 status='failed'
            assert any("status='failed'" in sql for sql, _ in failed_calls), \
                f"未检测到 status='failed' 的 execute 调用: {failed_calls}"
        finally:
            q._process_document = original
            pq_mod.queue = None
            q.stop()

    asyncio.run(_t())


# ============================================================================ #
# 测试 4: reconcile_processing_docs 把卡住行重入队
# ============================================================================ #
def test_reconcile_requeues_processing():
    """reconcile 从 DB 捞出 status='processing' 行, 校验 MinIO 后 submit 到 queue.

    关键: reconcile 内部用 ``from app.services.processing_queue import get_queue
    as _gq; q = _gq()`` 取队列 —— 所以要 patch ``app.services.processing_queue.get_queue``
    而不是只改 ``pq_mod.queue`` 单例.
    """

    async def _t():
        import app.api.docs as docs_mod
        from app.services import processing_queue as pq_mod

        processing_row = ("d1", 1, "d1.txt")
        fake_conn = make_fake_conn(fetchall_result=[processing_row])

        async def _fake_get_db(*a, **kw):
            return fake_conn

        # fake minio: stat_object / get_object 都成功, 不真发 HTTP
        # read() 返回真实 bytes (不是 coroutine) —— run_blocking 把 read 提交到线程池
        fake_minio_resp = MagicMock()
        fake_minio_resp.read = MagicMock(return_value=b"dummy content")
        fake_minio_resp.close = MagicMock()

        # 构造一个 fake queue: run_blocking 走线程池 (与真实 ProcessingQueue 一致),
        # submit 只记录不参与队列调度
        captured_submits = []
        ex = ThreadPoolExecutor(max_workers=4)
        loop = asyncio.get_running_loop()

        class _FakeQueue:
            def submit(self, job):
                captured_submits.append(job)
            async def run_blocking(self, fn, *args):
                return await loop.run_in_executor(ex, fn, *args)

        fake_queue = _FakeQueue()

        try:
            with patch("app.api.docs.get_db_connection", _fake_get_db), \
                 patch.object(docs_mod.minio_client, "stat_object", return_value=None), \
                 patch.object(docs_mod.minio_client, "get_object", return_value=fake_minio_resp), \
                 patch("app.services.processing_queue.get_queue", return_value=fake_queue):
                await docs_mod.reconcile_processing_docs()

            assert len(captured_submits) == 1, \
                f"期望 reconcile 提交 1 条, 实际 {len(captured_submits)}"
            assert captured_submits[0]["doc_id"] == "d1"
            assert captured_submits[0]["tenant_id"] == 1
        finally:
            ex.shutdown(wait=False)
            fake_minio_resp.read.assert_called_once()

    asyncio.run(_t())
