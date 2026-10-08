from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
import logging
import asyncio

from app.models.database import init_db, init_tables, PoolExhaustedError
from app.core.auth_middleware import JwtAuthMiddleware
from app.config import settings
from app.api.health import router as health_router
from app.api.knowledge import router as knowledge_router
from app.api.ask import router as ask_router
from app.api.docs import router as docs_router, reconcile_processing_docs
from app.api.sessions import router as sessions_router
from app.api.faqs import router as faqs_router
from app.api.data_sources import router as data_sources_router
from app.api.dashboard import router as dashboard_router
from app.services.processing_queue import get_queue

logger = logging.getLogger(__name__)

@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()
    await init_tables()
    app.state.settings = settings

    # 先启动 worker, 再 reconcile (reconcile 会把卡住的 processing 行重入队)
    q = get_queue()
    q.start()
    await reconcile_processing_docs()

    # Phase-1: fire-and-forget 全量摄取 park-enterprise 数据
    # 上游不可达时仅记录日志, 绝不影响启动流程
    async def _startup_sync():
        try:
            from app.services.datasource.sync_service import sync_source
            summary = await sync_source("park-enterprise")
            logger.info("[startup-sync] park-enterprise: %s", summary)
        except Exception:
            logger.exception("[startup-sync] park-enterprise sync failed; continuing startup")

    _sync_task = asyncio.create_task(_startup_sync())
    _sync_task.add_done_callback(lambda t: t.exception())

    print("[OK] platform-kefu started")
    yield
    q.stop()
    print("[OK] platform-kefu shutting down")

app = FastAPI(title="platform-kefu", version="1.2.0", lifespan=lifespan)
app.add_middleware(JwtAuthMiddleware)


@app.exception_handler(PoolExhaustedError)
async def _pool_exhausted_handler(request: Request, exc: PoolExhaustedError):
    """连接池耗尽 → 503, 而不是让请求永久挂起。

    挂起是最坏的失败模式: /api/kefu/health 不碰库照样 200, 容器在监控里显示健康,
    界面只表现为"点击发送无响应"。返回 503 至少能让问题立刻可见。
    """
    return JSONResponse(
        status_code=503,
        content={"code": 503, "message": f"Kefu data layer unavailable: {exc}"},
    )

app.include_router(health_router, prefix="", tags=["system"])
app.include_router(knowledge_router, prefix="/api/kefu/knowledge", tags=["knowledge"])
app.include_router(ask_router, prefix="/api/kefu/ask", tags=["ask"])
app.include_router(docs_router, prefix="/api/kefu/docs", tags=["docs"])
app.include_router(sessions_router, prefix="", tags=["sessions"])
app.include_router(faqs_router, prefix="", tags=["faqs"])
app.include_router(data_sources_router, prefix="", tags=["data_sources"])
app.include_router(dashboard_router, prefix="", tags=["dashboard"])
