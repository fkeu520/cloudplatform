from fastapi import FastAPI
from contextlib import asynccontextmanager

from app.models.database import init_db, init_tables
from app.core.auth_middleware import JwtAuthMiddleware
from app.config import settings
from app.api.health import router as health_router
from app.api.knowledge import router as knowledge_router
from app.api.ask import router as ask_router
from app.api.docs import router as docs_router

@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()
    await init_tables()
    app.state.settings = settings
    print("[OK] platform-kefu started")
    yield
    print("[OK] platform-kefu shutting down")

app = FastAPI(title="platform-kefu", version="1.2.0", lifespan=lifespan)
app.add_middleware(JwtAuthMiddleware)

app.include_router(health_router, prefix="", tags=["system"])
app.include_router(knowledge_router, prefix="/api/kefu/knowledge", tags=["knowledge"])
app.include_router(ask_router, prefix="/api/kefu/ask", tags=["ask"])
app.include_router(docs_router, prefix="/api/kefu/docs", tags=["docs"])
