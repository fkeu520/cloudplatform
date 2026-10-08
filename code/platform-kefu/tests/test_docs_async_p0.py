"""docs.py 异步上传的端到端行为 (stub 化, 无需真实 MySQL/MinIO/SiliconFlow)。

核心验收:
- upload_document 返回 processing, 不阻塞等 embed
- reprocess 改为入队
- GET /{doc_id}/status 返回 {doc_id, status, chunk_count, error_message, started_at, finished_at}
- 同 hash 二次上传返回已存在行 (200), 不新建
- 队列满返回 503

T1: 不设 MYSQL_HOST —— 不污染 test_kefu.py 的 skip 逻辑
T2: patch app.api.docs.get_db_connection (不是 app.services.processing_queue.get_db_connection)
T3: stub _ensure_bucket 和 minio 所有方法, 阻止真实 HTTP 调用
T4: asyncio.Queue(maxsize=1) 用于 503 测试
T7: fake conn/cur 支持 async with 和 await execute/fetchone/fetchall
"""
from __future__ import annotations

import asyncio
import os
import sys
import time
from unittest.mock import MagicMock, patch, AsyncMock

import pytest
import jwt as pyjwt
from fastapi.testclient import TestClient

# T1: 只设密钥占位, 不设 MYSQL_HOST
os.environ.setdefault("JWT_SECRET", "test-only-jwt-secret-not-a-real-key-0000")
os.environ.setdefault("DEEPSEEK_API_KEY", "test-only-placeholder")
os.environ.setdefault("SILICONFLOW_API_KEY", "test-only-placeholder")
# 故意不设 MYSQL_HOST —— 保持 test_kefu.py 的 skip 行为不变

# 把 aiomysql 替换成 stub, 让 TestClient(app) 不真连库
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
        def _no(*a, **kw):
            raise RuntimeError("aiomysql stub")
        _stub.create_pool = _no
        sys.modules["aiomysql"] = _stub

from app.main import app  # noqa: E402
from app.services.processing_queue import ProcessingQueue, QueueBusy  # noqa: E402
from app.config import settings  # noqa: E402

import time as _time  # noqa: E402

KEFU_PERMS = [
    "kefu:chat", "kefu:knowledge", "kefu:settings", "kefu:faqs",
    "kefu:sessions", "kefu:knowledge:add", "kefu:knowledge:edit", "kefu:knowledge:delete",
]


def _auth_headers(tenant_id: int = 1) -> dict:
    token = pyjwt.encode(
        {"sub": "999", "username": "tester", "tenantId": tenant_id,
         "userType": 1, "permissions": KEFU_PERMS,
         "iat": _time.time(), "exp": _time.time() + 3600},
        settings.jwt_secret, algorithm="HS384",
    )
    return {"Authorization": "Bearer " + token}


# T7: 构造支持 async with cursor() 和 await execute/fetchone/fetchall 的 fake conn
def _make_fake_conn(fetchone_result=None, fetchall_result=None):
    fake_cur = MagicMock()
    fake_cur.fetchone = AsyncMock(return_value=fetchone_result)
    fake_cur.fetchall = AsyncMock(return_value=fetchall_result if fetchall_result is not None else [])
    fake_cur.execute = AsyncMock(return_value=None)
    fake_cm = MagicMock()
    fake_cm.__aenter__ = AsyncMock(return_value=fake_cur)
    fake_cm.__aexit__ = AsyncMock(return_value=False)
    fake_conn = MagicMock()
    fake_conn.cursor = MagicMock(return_value=fake_cm)
    fake_conn.close = MagicMock()
    return fake_conn, fake_cur


# ------------------------------------------------------------------ #
# 1. upload 立即返回 processing, 不触达 embed
# ------------------------------------------------------------------ #
def test_upload_returns_processing_fast():
    embed_called = []

    def fake_embed(texts):
        embed_called.append(len(texts))
        return [([0.1] * 1024)] * len(texts)

    fake_conn, fake_cur = _make_fake_conn(fetchone_result=None)

    q = ProcessingQueue()
    submitted = []
    q.submit = lambda job: submitted.append(job)

    from app.services import processing_queue as pq_mod
    orig_queue = pq_mod.queue
    pq_mod.queue = q

    try:
        with patch('app.api.docs.get_db_connection', return_value=fake_conn), \
             patch('app.api.docs._ensure_bucket', return_value=None), \
             patch('app.api.docs.minio_client.put_object', return_value=None), \
             patch('app.services.embedder.embedder.embed', fake_embed):
            client = TestClient(app)
            t0 = time.monotonic()
            resp = client.post(
                "/api/kefu/docs/upload",
                files={"file": ("test.txt", b"hello world")},
                headers=_auth_headers(),
            )
            elapsed = time.monotonic() - t0

            assert resp.status_code == 200, f"期望 200, 实际 {resp.status_code}: {resp.text}"
            data = resp.json()
            assert data["status"] == "processing"
            assert elapsed < 2.0, f"upload 耗时过长: {elapsed:.2f}s"
            assert len(embed_called) == 0, "embed 应在 worker 里调用, 不该在请求期间"
    finally:
        pq_mod.queue = orig_queue


# ------------------------------------------------------------------ #
# 2. 幂等: 同 hash 二次上传返回已有行 (D2 修复: hash SELECT 在 INSERT 之前)
# ------------------------------------------------------------------ #
def test_upload_dedup_returns_existing():
    existing_row = ("existing-id", "test.txt", ".txt", 11, "2026-01-01T00:00:00",
                    "ready", 3, None)

    fake_conn, fake_cur = _make_fake_conn(fetchone_result=existing_row)

    from app.services import processing_queue as pq_mod
    orig_queue = pq_mod.queue
    pq_mod.queue = ProcessingQueue()

    try:
        with patch('app.api.docs.get_db_connection', return_value=fake_conn), \
             patch('app.api.docs._ensure_bucket', return_value=None):
            client = TestClient(app)
            resp = client.post(
                "/api/kefu/docs/upload",
                files={"file": ("test.txt", b"hello world")},
                headers=_auth_headers(),
            )
            assert resp.status_code == 200
            data = resp.json()
            assert data["doc_id"] == "existing-id"
            assert data["status"] == "ready"
    finally:
        pq_mod.queue = orig_queue


# ------------------------------------------------------------------ #
# 3. 队列满 → 503 (T4: asyncio.Queue(maxsize=1))
# ------------------------------------------------------------------ #
def test_upload_queue_full_503():
    fake_conn, fake_cur = _make_fake_conn(fetchone_result=None)

    q = ProcessingQueue()
    q._queue = asyncio.Queue(maxsize=1)  # T4: maxsize=1

    # 先塞满
    q.submit({"doc_id": "a", "tenant_id": 1, "object_name": "a.txt",
              "file_content": b"x", "filename": "a.txt", "ext": ".txt"})

    from app.services import processing_queue as pq_mod
    orig_queue = pq_mod.queue
    pq_mod.queue = q

    try:
        with patch('app.api.docs.get_db_connection', return_value=fake_conn), \
             patch('app.api.docs._ensure_bucket', return_value=None), \
             patch('app.api.docs.minio_client.put_object', return_value=None):
            client = TestClient(app)
            resp = client.post(
                "/api/kefu/docs/upload",
                files={"file": ("test.txt", b"hello world")},
                headers=_auth_headers(),
            )
            assert resp.status_code == 503
    finally:
        pq_mod.queue = orig_queue


# ------------------------------------------------------------------ #
# 4. GET /{doc_id}/status 返回预期 shape
# ------------------------------------------------------------------ #
def test_get_doc_status_shape():
    fake_conn, fake_cur = _make_fake_conn(fetchone_result=(
        "d1", "ready", 5, None,
        __import__("datetime").datetime(2026, 1, 1),
        __import__("datetime").datetime(2026, 1, 1, 0, 0, 5),
    ))

    with patch('app.api.docs.get_db_connection', return_value=fake_conn):
        client = TestClient(app)
        resp = client.get("/api/kefu/docs/d1/status", headers=_auth_headers())
        assert resp.status_code == 200
        data = resp.json()
        assert set(data.keys()) == {"doc_id", "status", "chunk_count",
                                    "error_message", "started_at", "finished_at"}
        assert data["doc_id"] == "d1"
        assert data["status"] == "ready"
        assert data["chunk_count"] == 5


# ------------------------------------------------------------------ #
# 5. reprocess 改为入队 (不再 inline 处理)
# ------------------------------------------------------------------ #
def test_reprocess_enqueues():
    submitted = []
    fake_conn, fake_cur = _make_fake_conn(
        fetchone_result=("d1", "test.txt", ".txt", 10, "2026-01-01", "ready", 0, "d1.txt"),
        fetchall_result=[],
    )
    mock_resp = MagicMock()
    mock_resp.read = AsyncMock(return_value=b"content")
    mock_resp.close = MagicMock()

    q = ProcessingQueue()
    q.submit = lambda job: submitted.append(job)

    from app.services import processing_queue as pq_mod
    orig_queue = pq_mod.queue
    pq_mod.queue = q

    try:
        with patch('app.api.docs.get_db_connection', return_value=fake_conn), \
             patch('app.api.docs.minio_client.get_object', return_value=mock_resp):
            client = TestClient(app)
            resp = client.post("/api/kefu/docs/d1/reprocess", headers=_auth_headers())
            assert resp.status_code == 200
            assert len(submitted) == 1
            assert submitted[0]["doc_id"] == "d1"
    finally:
        pq_mod.queue = orig_queue
