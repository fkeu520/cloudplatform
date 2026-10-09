"""会话「软隐藏」P0 回归 (hidden_by_customer).

目的
----
1. `GET /api/kefu/sessions/my` 的 SQL 必须带 `hidden_by_customer=0` 谓词
   + customer_id 归属过滤 (本人视角); 管理端 `list_sessions` 不加该过滤。
2. `svc.hide_my_session` 发出按 id + customer_id (+tenant) 收敛的 UPDATE,
   rowcount 决定返回值。
3. `DELETE /api/kefu/sessions/{sid}/mine` 在会话不归属当前用户 (rowcount=0)
   时返回 404, 归属时返回 {"ok": true, "id": sid}。

不连真实 MySQL: 用 `unittest.mock.patch` 把 `get_db_connection` 替换为假连接
(fake cursor 风格同 tests/test_backfill_run_p0.py)。
"""
from __future__ import annotations

import asyncio
import os
import sys
from pathlib import Path
from unittest.mock import MagicMock, patch

# 与 test_backfill_run_p0.py 一致: 先给 app.config 三个必填项占位值,
# 否则 pydantic-settings 在 import 期 ValidationError, 模块连 collect 都进不去。
os.environ.setdefault("DEEPSEEK_API_KEY", "test-only-placeholder")
os.environ.setdefault("SILICONFLOW_API_KEY", "test-only-placeholder")
os.environ.setdefault("JWT_SECRET", "test-only-jwt-secret-not-a-real-key-0000")

# 禁止读真实 MYSQL_HOST — 本测试只接受 fake DB
assert "MYSQL_HOST" not in os.environ, (
    "test_session_hide_p0 不应依赖真实 DB; 若环境已设 MYSQL_HOST, 请清除后重跑"
)

import pytest  # noqa: E402

_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(_ROOT / "app"))

from app.models import database as db_mod  # noqa: E402
from app.services import session_service as svc  # noqa: E402


# ---------------------------------------------------------------------------
# 假游标 / 假连接 — 记录 execute(sql, args) 与 rowcount
# ---------------------------------------------------------------------------

class FakeCursor:
    def __init__(self, rowcount: int = 0, fetchall_rows: list | None = None) -> None:
        self.rowcount = rowcount
        self.fetchall_rows = fetchall_rows or []
        self.execute_calls: list[tuple[str, tuple]] = []

    async def execute(self, sql: str, args=None) -> None:
        self.execute_calls.append((sql, args or ()))

    async def fetchall(self) -> list:
        return self.fetchall_rows

    async def fetchone(self):
        return self.fetchall_rows[0] if self.fetchall_rows else None

    def close(self) -> None:
        pass


class _AsyncCursorCtx:
    def __init__(self, inner: FakeCursor) -> None:
        self.inner = inner

    async def __aenter__(self):
        return self.inner

    async def __aexit__(self, exc_type, exc, tb) -> None:
        self.inner.close()
        return False


class FakeConn:
    def __init__(self, rowcount: int = 0, fetchall_rows: list | None = None) -> None:
        self.rowcount = rowcount
        self.fetchall_rows = fetchall_rows or []
        self.cursors: list[FakeCursor] = []
        self.close_called = False

    def cursor(self):
        cur = FakeCursor(rowcount=self.rowcount, fetchall_rows=self.fetchall_rows)
        self.cursors.append(cur)
        return _AsyncCursorCtx(cur)

    def close(self) -> None:
        self.close_called = True


def _patch_get_conn(fake_conn: FakeConn):
    """把 svc / db_mod 内的 get_db_connection 换成 fake, 返回两个 patcher。

    `patch.object` 返回 context manager, 须 `.start()` 后 `.stop()` 还原。
    """
    async def _fake_get_conn(*_a, **_kw):
        return fake_conn

    p1 = patch.object(svc, "get_db_connection", _fake_get_conn)
    p2 = patch.object(db_mod, "get_db_connection", _fake_get_conn)
    p1.start()
    p2.start()
    return [p1, p2]


def _stop(pachers_list) -> None:
    for p in reversed(pachers_list):
        p.stop()


# ---------------------------------------------------------------------------
# 1. list_my_sessions SQL 契约
# ---------------------------------------------------------------------------

@pytest.mark.asyncio
async def test_list_my_sessions_sql_has_hidden_predicate_and_customer_scope():
    """「我的会话」SQL 必须同时带 hidden_by_customer=0 与 customer_id 过滤。"""
    row = [
        "s-1", 1, 99, "Tester", "13800000000", "web", "AI",
        None, None, None, None, None, None, None,
    ]
    conn = FakeConn(rowcount=0, fetchall_rows=[row])
    patchers = _patch_get_conn(conn)
    try:
        items = await svc.list_my_sessions(
            customer_id=99, tenant_id=1, status=None, limit=50, offset=0,
        )
        assert conn.close_called, "finally 路径必须 close()"
        assert len(items) == 1, "1 行结果应映射出 1 个 session dict"
        assert items[0]["id"] == "s-1"

        select_calls = [
            (sql, args) for sql, args in conn.cursors[0].execute_calls
            if sql.upper().startswith("SELECT")
        ]
        assert len(select_calls) == 1, "list_my_sessions 应只发 1 条 SELECT"
        sql, args = select_calls[0]
        # 关键谓词: 隐藏过滤 + 本人归属 + 租户收敛
        assert "hidden_by_customer=0" in sql, (
            f"SELECT 必须过滤已软隐藏会话, 实际 SQL: {sql}"
        )
        assert "customer_id=%s" in sql, f"必须按 customer_id 归属, 实际: {sql}"
        assert "tenant_id=%s" in sql, f"必须按 tenant_id 收敛, 实际: {sql}"
        # 参数顺序: customer_id → tenant_id → limit → offset
        assert list(args) == [99, 1, 50, 0], f"参数序列应为 [99,1,50,0], 实际 {args}"
    finally:
        _stop(patchers)


@pytest.mark.asyncio
async def test_list_sessions_admin_path_has_no_hidden_predicate():
    """管理端 list_sessions 不加 hidden_by_customer 过滤 (P0 语义: 管理员仍可见)。"""
    row = [
        "s-1", 1, 99, "Tester", "13800000000", "web", "AI",
        None, None, None, None, None, None, None,
    ]
    conn = FakeConn(rowcount=0, fetchall_rows=[row])
    patchers = _patch_get_conn(conn)
    try:
        items = await svc.list_sessions(tenant_id=1, limit=50, offset=0)
        assert len(items) == 1
        sql, _args = conn.cursors[0].execute_calls[0]
        assert "hidden_by_customer" not in sql, (
            "管理端列表不得过滤 hidden_by_customer (否则管理员看不到已软隐藏会话), "
            f"实际 SQL: {sql}"
        )
        # 管理端不按 customer_id 过滤 (全租户可见, 非本人归属)
        _where = sql.split("WHERE", 1)[1] if "WHERE" in sql else ""
        _first_cond = _where.split("AND", 1)[0] if _where else ""
        assert "customer_id" not in _first_cond, (
            "管理端列表不按 customer_id 过滤 (全租户可见), 实际 SQL: " + sql
        )
    finally:
        _stop(patchers)


# ---------------------------------------------------------------------------
# 2. hide_my_session UPDATE 契约
# ---------------------------------------------------------------------------

@pytest.mark.asyncio
async def test_hide_my_session_issues_scoped_update_with_tenant():
    """带 tenant: UPDATE 按 id + customer_id + tenant_id 收敛, rowcount 决定返回。"""
    conn = FakeConn(rowcount=1, fetchall_rows=[])
    patchers = _patch_get_conn(conn)
    try:
        affected = await svc.hide_my_session("s-1", customer_id=99, tenant_id=1)
        assert affected is True, "rowcount=1 时应返回 True"
        assert conn.close_called

        update_calls = [
            (sql, args) for sql, args in conn.cursors[0].execute_calls
            if sql.upper().startswith("UPDATE")
        ]
        assert len(update_calls) == 1, "hide_my_session 应发 1 条 UPDATE"
        sql, args = update_calls[0]
        assert "SET hidden_by_customer=1" in sql, (
            f"UPDATE 应置位 hidden_by_customer, 实际: {sql}"
        )
        for frag in ("id=%s", "customer_id=%s", "tenant_id=%s"):
            assert frag in sql, f"UPDATE 必须按 {frag} 收敛, 实际: {sql}"
        assert list(args) == ["s-1", 99, 1], f"参数序列应为 ['s-1',99,1], 实际 {args}"
    finally:
        _stop(patchers)


@pytest.mark.asyncio
async def test_hide_my_session_without_tenant_omits_tenant_clause():
    """tenant_id=None: UPDATE 仅按 id + customer_id 收敛 (不出现 tenant_id)。"""
    conn = FakeConn(rowcount=0, fetchall_rows=[])
    patchers = _patch_get_conn(conn)
    try:
        affected = await svc.hide_my_session("s-1", customer_id=99, tenant_id=None)
        assert affected is False, "rowcount=0 时应返回 False"
        sql, args = conn.cursors[0].execute_calls[0]
        assert "tenant_id=%s" not in sql, (
            f"tenant_id=None 时不得出现 tenant 子句, 实际: {sql}"
        )
        assert "customer_id=%s" in sql and "id=%s" in sql
        assert list(args) == ["s-1", 99], f"参数序列应为 ['s-1',99], 实际 {args}"
    finally:
        _stop(patchers)


# ---------------------------------------------------------------------------
# 3. DELETE 端点 — 归属 / 越权 404
# ---------------------------------------------------------------------------

def _fake_request(user: dict):
    """构造带 request.state.user 的 MagicMock Request, 驱动真实 handler 逻辑。

    直接调 handler 函数 (不走 ASGI), 避免起 TestClient + 中间件全套;
    _require_permission / _extract_tenant 走真实实现, 仅注入 user 上下文。
    """
    from fastapi import Request
    import app.api.sessions as api_mod

    class _FakeState:
        def __init__(self, user):
            self.user = user

    fake_request = MagicMock(spec=Request)
    fake_request.state = _FakeState(user)
    return fake_request, api_mod


def test_delete_mine_returns_404_when_session_not_owned():
    """rowcount=0 (非本人/不存在) → HTTPException(404)。"""
    fake_request, api_mod = _fake_request({
        "userId": "99", "username": "tester", "tenantId": 1,
        "userType": 1, "permissions": ["kefu:chat", "kefu:sessions"],
    })
    conn = FakeConn(rowcount=0)
    patchers = _patch_get_conn(conn)
    try:
        from fastapi import HTTPException
        try:
            asyncio.run(api_mod.hide_my_session(fake_request, "s-missing"))
        except HTTPException as e:
            assert e.status_code == 404, (
                f"越权/不存在应 404, 实际 status={e.status_code}: {e.detail}"
            )
        else:
            pytest.fail("rowcount=0 时应抛 HTTPException(404), 未抛")
    finally:
        _stop(patchers)


def test_delete_mine_returns_ok_when_owned():
    """rowcount=1 (本人会话) → 返回 {"ok": True, "id": sid}。"""
    fake_request, api_mod = _fake_request({
        "userId": "99", "username": "tester", "tenantId": 1,
        "userType": 1, "permissions": ["kefu:chat", "kefu:sessions"],
    })
    conn = FakeConn(rowcount=1)
    patchers = _patch_get_conn(conn)
    try:
        result = asyncio.run(api_mod.hide_my_session(fake_request, "s-1"))
        assert result == {"ok": True, "id": "s-1"}, f"成功应返回 ok+id, 实际 {result}"
        # 校验 UPDATE 确实被发出且按本人收敛
        sql, args = conn.cursors[0].execute_calls[0]
        assert "hidden_by_customer=1" in sql
        assert list(args) == ["s-1", 99, 1], (
            f"应按 (sid, customer_id, tenant_id) 收敛, 实际 {args}"
        )
    finally:
        _stop(patchers)


def test_delete_mine_requires_kefu_chat_permission():
    """无 kefu:chat 权限 → 403 (fail-fast, 不触达 DB)。"""
    fake_request, api_mod = _fake_request({
        "userId": "99", "tenantId": 1,
        "userType": 1, "permissions": ["kefu:sessions"],  # 缺 kefu:chat
    })
    conn = FakeConn(rowcount=1)
    patchers = _patch_get_conn(conn)
    try:
        from fastapi import HTTPException
        with pytest.raises(HTTPException) as exc_info:
            asyncio.run(api_mod.hide_my_session(fake_request, "s-1"))
        assert exc_info.value.status_code == 403, (
            f"无 kefu:chat 权限应 403, 实际 status={exc_info.value.status_code}"
        )
        assert not conn.cursors, "越权路径不应触达 DB (fail-fast)"
    finally:
        _stop(patchers)
