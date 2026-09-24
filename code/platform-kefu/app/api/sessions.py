"""会话/消息/评分 API 路由

2026-09-24 P0 租户隔离:
- 所有涉及 kefu_session / kefu_message 的接口从 request.state.user 读取 tenantId
  (auth_middleware.py 已验证 X-User-* 头或 JWT claim)
- normalize_tenant_id 缺失时返回 403（fail-closed）
- 平台管理员 (userType==2) 不限制 tenant_id（跨租户可见，语义与 platform-server
  的 opsAdmin 角色一致）
- 不信任任何客户端 header（如 X-Forwarded-For）作为租户身份
"""
from typing import Optional
from fastapi import APIRouter, HTTPException, Request
from app.models.schemas import (
    SessionCreateRequest, SessionResponse, SessionListResponse,
    MessageCreateRequest, MessageResponse, ChatResponse, SessionRateRequest,
)
from app.services import session_service as svc
from app.services.session_service import SessionNotFoundError
from app.core.access import has_permission, normalize_tenant_id

router = APIRouter(prefix="/api/kefu", tags=["sessions"])


def _extract_tenant(request: Request):
    """从 request.state.user 提取 tenant_id。

    返回 (tenant_id: int|None, is_platform_admin: bool)。
    - tenant_id=None 表示平台管理员（跨租户可见）
    - 缺失/无效 tenantId → 抛 HTTPException(403)
    """
    user = getattr(request.state, "user", None)
    if user is None:
        raise HTTPException(401, "Unauthorized: no user context")
    user_type_raw = user.get("userType")
    is_platform_admin = False
    try:
        if user_type_raw is not None:
            is_platform_admin = int(user_type_raw) == 2
    except (TypeError, ValueError):
        pass
    if is_platform_admin:
        return None, True
    raw_tenant = user.get("tenantId")
    try:
        tenant_id = normalize_tenant_id(raw_tenant)
    except ValueError as exc:
        raise HTTPException(403, f"Tenant access denied: {exc}") from exc
    return tenant_id, False


def _require_permission(request: Request, permission: str) -> None:
    user = getattr(request.state, "user", None)
    if not user or not has_permission(user, permission):
        raise HTTPException(403, f"Permission denied: {permission}")


@router.post("/sessions", response_model=SessionResponse)
async def create_session(request: Request, req: SessionCreateRequest):
    _require_permission(request, "kefu:chat")
    tenant_id, _ = _extract_tenant(request)
    return await svc.create_session(
        tenant_id=tenant_id,
        customer_id=req.customer_id,
        customer_name=req.customer_name,
        contact=req.contact,
        channel=req.channel,
    )


@router.get("/sessions/{sid}", response_model=SessionResponse)
async def get_session(request: Request, sid: str):
    _require_permission(request, "kefu:chat")
    tenant_id, _ = _extract_tenant(request)
    s = await svc.get_session(sid, tenant_id=tenant_id)
    if not s:
        raise HTTPException(404, f"会话不存在: {sid}")
    return s


@router.get("/sessions", response_model=SessionListResponse)
async def list_sessions(
    request: Request,
    status: Optional[str] = None,
    channel: Optional[str] = None,
    limit: int = 50,
    offset: int = 0,
):
    _require_permission(request, "kefu:sessions")
    tenant_id, _ = _extract_tenant(request)
    items = await svc.list_sessions(
        tenant_id=tenant_id, status=status, channel=channel, limit=limit, offset=offset,
    )
    return SessionListResponse(total=len(items), items=items)


@router.post("/sessions/{sid}/transfer")
async def transfer_to_human(
    request: Request,
    sid: str,
    agent_id: Optional[int] = None,
    agent_name: Optional[str] = None,
):
    _require_permission(request, "kefu:sessions")
    tenant_id, _ = _extract_tenant(request)
    return await svc.transfer_to_human(sid, tenant_id=tenant_id, agent_id=agent_id, agent_name=agent_name)


@router.post("/sessions/{sid}/close")
async def close_session(request: Request, sid: str):
    _require_permission(request, "kefu:sessions")
    tenant_id, _ = _extract_tenant(request)
    return await svc.close_session(sid, tenant_id=tenant_id)


@router.post("/sessions/{sid}/rate")
async def rate_session(request: Request, sid: str, req: SessionRateRequest):
    _require_permission(request, "kefu:sessions")
    tenant_id, _ = _extract_tenant(request)
    return await svc.rate_session(sid, tenant_id=tenant_id, satisfaction=req.satisfaction, comment=req.comment)


@router.get("/sessions/{sid}/messages", response_model=list[MessageResponse])
async def get_messages(request: Request, sid: str, limit: int = 100):
    _require_permission(request, "kefu:sessions")
    tenant_id, _ = _extract_tenant(request)
    return await svc.get_messages(sid, tenant_id=tenant_id, limit=limit)


@router.post("/sessions/{sid}/messages", response_model=ChatResponse)
async def send_message(request: Request, sid: str, req: MessageCreateRequest):
    _require_permission(request, "kefu:chat")
    tenant_id, _ = _extract_tenant(request)
    try:
        return await svc.chat(sid, tenant_id=tenant_id, user_content=req.content, settings=None)
    except SessionNotFoundError as exc:
        raise HTTPException(404, str(exc)) from exc


@router.get("/health")
async def health():
    return {"status": "UP", "service": "platform-kefu", "version": "1.2.0"}
