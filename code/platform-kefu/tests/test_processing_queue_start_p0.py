"""ProcessingQueue.start() / stop() 生命周期单测.

无需 MySQL / MinIO / 任何网络. 仅测:
- start() 后 _executor 非 None、_workers 数 = settings.doc_worker_count
- _running 状态机: 重复 start 是 no-op
- stop() 后 _executor 归 None, _workers 清空
- 被 worker task 消费的文档在 stop 前已排入队列
"""
from __future__ import annotations

import asyncio
import os
import sys

# ── env 占位 (与 test_kefu.py 一致) ─────────────────────────────────────────
os.environ.setdefault("JWT_SECRET", "test-only-jwt-secret-not-a-real-key-0000")
os.environ.setdefault("DEEPSEEK_API_KEY", "test-only-placeholder")
os.environ.setdefault("SILICONFLOW_API_KEY", "test-only-placeholder")
# 故意不设 MYSQL_HOST —— 不污染其他模块的 skip 逻辑.

# ── aiomysql stub (避免 import 期连库) ───────────────────────────────────────
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

from app.config import settings  # noqa: E402
from app.services.processing_queue import ProcessingQueue  # noqa: E402


def test_start_and_stop():
    """start 后 executor/workers 就绪; stop 后全部归 None."""

    async def _t():
        q = ProcessingQueue()
        assert q._executor is None
        assert q._workers == []
        assert not q._running

        q.start()
        assert q._running
        assert q._executor is not None
        assert len(q._workers) == settings.doc_worker_count

        # 重复 start 是 no-op: worker 不会翻倍
        q.start()
        assert len(q._workers) == settings.doc_worker_count

        q.stop()
        assert not q._running
        assert q._executor is None
        assert q._workers == []

    asyncio.run(_t())


def test_start_stop_is_idempotent_on_stopped():
    """stop 后再 stop 不应抛."""

    async def _t():
        q = ProcessingQueue()
        q.start()
        q.stop()
        q.stop()  # 不应抛
        assert q._executor is None
        assert q._workers == []

    asyncio.run(_t())
