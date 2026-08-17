"""会话/消息/评分 API 路由"""
from typing import Optional
from fastapi import APIRouter, HTTPException
from app.models.schemas import (
    SessionCreateRequest, SessionResponse, SessionListResponse,
    MessageCreateRequest, MessageResponse, ChatResponse, SessionRateRequest,
)
from app.services import session_service as svc

router = APIRouter(prefix="/api/kefu", tags=["sessions"])


@router.post("/sessions", response_model=SessionResponse)
async def create_session(req: SessionCreateRequest):
    return await svc.create_session(
        customer_id=req.customer_id, customer_name=req.customer_name,
        contact=req.contact, channel=req.channel,
    )


@router.get("/sessions/{sid}", response_model=SessionResponse)
async def get_session(sid: str):
    s = await svc.get_session(sid)
    if not s:
        raise HTTPException(404, f"会话不存在: {sid}")
    return s


@router.get("/sessions", response_model=SessionListResponse)
async def list_sessions(status: Optional[str] = None, channel: Optional[str] = None, limit: int = 50, offset: int = 0):
    items = await svc.list_sessions(status=status, channel=channel, limit=limit, offset=offset)
    return SessionListResponse(total=len(items), items=items)


@router.post("/sessions/{sid}/transfer")
async def transfer_to_human(sid: str, agent_id: Optional[int] = None, agent_name: Optional[str] = None):
    return await svc.transfer_to_human(sid, agent_id=agent_id, agent_name=agent_name)


@router.post("/sessions/{sid}/close")
async def close_session(sid: str):
    return await svc.close_session(sid)


@router.post("/sessions/{sid}/rate")
async def rate_session(sid: str, req: SessionRateRequest):
    return await svc.rate_session(sid, req.satisfaction, req.comment)


@router.get("/sessions/{sid}/messages", response_model=list[MessageResponse])
async def get_messages(sid: str, limit: int = 100):
    return await svc.get_messages(sid, limit=limit)


@router.post("/sessions/{sid}/messages", response_model=ChatResponse)
async def send_message(sid: str, req: MessageCreateRequest):
    return await svc.chat(sid, req.content, None)


@router.get("/health")
async def health():
    return {"status": "UP", "service": "platform-kefu", "version": "1.2.0"}