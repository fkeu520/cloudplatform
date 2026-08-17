"""数据源管理 API 路由"""
from fastapi import APIRouter, HTTPException
from app.models.schemas import DataSourceResponse, DataSourceListResponse, DataSourceUpdateRequest, DataSourceRegisterRequest
from app.services.datasource.base import registry
from app.services.datasource.faq_adapter import FaqAdapter
from app.services.datasource.knowledge_adapter import KnowledgeAdapter
from app.services.datasource.park_enterprise_adapter import ParkEnterpriseAdapter
from app.models.database import get_db_connection
from typing import List

router = APIRouter(prefix="/api/kefu", tags=["data_sources"])

_ADAPTER_MAP = {
    "faq": FaqAdapter,
    "knowledge": KnowledgeAdapter,
    "park-enterprise": ParkEnterpriseAdapter,
}


def _ensure_registry():
    if not registry.all():
        for cls in _ADAPTER_MAP.values():
            registry.register(cls())


@router.get("/data_sources", response_model=DataSourceListResponse)
async def list_data_sources():
    _ensure_registry()
    items = [a.to_response() for a in registry.all()]
    return DataSourceListResponse(total=len(items), items=items)


@router.get("/data_sources/{source_id}", response_model=DataSourceResponse)
async def get_data_source(source_id: str):
    _ensure_registry()
    a = registry.get(source_id)
    if not a:
        raise HTTPException(404, f"数据源不存在: {source_id}")
    return a.to_response()


@router.put("/data_sources/{source_id}", response_model=DataSourceResponse)
async def update_data_source(source_id: str, req: DataSourceUpdateRequest):
    _ensure_registry()
    a = registry.get(source_id)
    if not a:
        raise HTTPException(404, f"数据源不存在: {source_id}")
    if req.enabled is not None:
        a.enabled = req.enabled
    if req.sync_strategy is not None:
        a.config["sync_strategy"] = req.sync_strategy
    if req.intent_keywords is not None:
        a.intent_keywords = req.intent_keywords
    if req.config_json is not None:
        a.config.update(req.config_json)
    return a.to_response()


@router.post("/data_sources/register", response_model=DataSourceResponse)
async def register_data_source(req: DataSourceRegisterRequest):
    """动态注册新数据源（DESIGN §七.5 step 1-2）"""
    _ensure_registry()
    if registry.get(req.id):
        raise HTTPException(400, f"数据源已存在: {req.id}")
    # 目前只支持预定义类型
    cls = _ADAPTER_MAP.get(req.id)
    if not cls:
        raise HTTPException(400, f"暂不支持动态注册类型: {req.id}（请先实现 DataSourceAdapter 子类）")
    adapter = cls()
    adapter.config = req.config_json or {}
    adapter.config["sync_strategy"] = req.sync_strategy
    adapter.intent_keywords = req.intent_keywords or []
    registry.register(adapter)
    return adapter.to_response()


@router.post("/data_sources/{source_id}/sync")
async def sync_data_source(source_id: str):
    _ensure_registry()
    a = registry.get(source_id)
    if not a:
        raise HTTPException(404, f"数据源不存在: {source_id}")
    # 目前为实时查询，无需同步
    return {"message": f"{source_id} 为实时查询，无需同步", "last_sync_at": None}