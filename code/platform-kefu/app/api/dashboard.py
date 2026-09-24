"""评估报告 + 数据看板 API 路由"""
from fastapi import APIRouter, HTTPException, Request
from app.models.schemas import DashboardStatsResponse, EvaluationResponse, EvaluationRequest
from app.models.database import get_db_connection
from app.services import session_service as svc
from datetime import datetime, timedelta
from app.core.access import has_permission, normalize_tenant_id
import uuid

router = APIRouter(prefix="/api/kefu", tags=["dashboard"])


def _tenant_id(request: Request, permission: str) -> int:
    user = getattr(request.state, "user", None)
    if not user:
        raise HTTPException(401, "Unauthorized: no user context")
    if not has_permission(user, permission):
        raise HTTPException(403, f"Permission denied: {permission}")
    if str(user.get("userType", "")) == "2":
        return 0
    try:
        return normalize_tenant_id(user.get("tenantId"))
    except ValueError as exc:
        raise HTTPException(403, f"Tenant access denied: {exc}") from exc


@router.get("/dashboard", response_model=DashboardStatsResponse)
async def get_dashboard(request: Request):
    tenant_id = _tenant_id(request, "kefu:dashboard")
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "SELECT COUNT(*) FROM kefu_session WHERE tenant_id=%s",
                (tenant_id,),
            )
            total_sessions = (await cur.fetchone())[0]
            await cur.execute(
                "SELECT COUNT(*) FROM kefu_message WHERE tenant_id=%s",
                (tenant_id,),
            )
            total_messages = (await cur.fetchone())[0]
            await cur.execute(
                "SELECT COUNT(*) FROM kefu_session "
                "WHERE tenant_id=%s AND status='CLOSED' AND end_time IS NOT NULL",
                (tenant_id,),
            )
            ai_resolved = (await cur.fetchone())[0]
            ai_total = max(total_sessions, 1)
            ai_rate = round(ai_resolved / ai_total * 100, 1)
            await cur.execute(
                "SELECT AVG(satisfaction) FROM kefu_session "
                "WHERE tenant_id=%s AND satisfaction IS NOT NULL",
                (tenant_id,),
            )
            avg_sat = (await cur.fetchone())[0]
            await cur.execute(
                "SELECT COUNT(*) FROM kefu_session "
                "WHERE tenant_id=%s AND start_time >= NOW() - INTERVAL 1 HOUR * 24",
                (tenant_id,),
            )
            recent_24h = (await cur.fetchone())[0]
            await cur.execute(
                "SELECT data_source_id, SUM(hit_count) FROM kefu_session_data_source_hit "
                "WHERE tenant_id=%s GROUP BY data_source_id",
                (tenant_id,),
            )
            ds_hits = {}
            async for row in cur:
                ds_hits[row[0]] = row[1]
            return DashboardStatsResponse(
                total_sessions=total_sessions,
                total_messages=total_messages,
                ai_resolution_rate=ai_rate,
                avg_satisfaction=float(avg_sat) if avg_sat else None,
                data_source_hit_counts=ds_hits,
                recent_24h_sessions=recent_24h,
            )
    finally:
        conn.close()


@router.post("/evaluations", response_model=EvaluationResponse)
async def create_evaluation(request: Request, req: EvaluationRequest):
    tenant_id = _tenant_id(request, "kefu:evaluation")
    eval_id = str(uuid.uuid4())
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "SELECT session_id FROM kefu_message WHERE msg_id=%s AND tenant_id=%s",
                (req.msg_id, tenant_id),
            )
            row = await cur.fetchone()
            if not row:
                raise HTTPException(404, "消息不存在或无权访问")
            sid = row[0]
            await cur.execute(
                "INSERT INTO kefu_evaluation "
                "(tenant_id, eval_id, msg_id, session_id, source_id, score, comment) "
                "VALUES (%s,%s,%s,%s,%s,%s,%s)",
                (tenant_id, eval_id, req.msg_id, sid, req.source_id, req.score, req.comment),
            )
    finally:
        conn.close()
    return EvaluationResponse(
        eval_id=eval_id, msg_id=req.msg_id, session_id=sid,
        source_id=req.source_id, score=req.score, comment=req.comment,
        created_at=datetime.now().isoformat(),
    )


@router.get("/evaluations/{session_id}", response_model=list[EvaluationResponse])
async def list_evaluations(request: Request, session_id: str):
    tenant_id = _tenant_id(request, "kefu:evaluation")
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "SELECT eval_id, msg_id, session_id, source_id, score, comment, created_at "
                "FROM kefu_evaluation WHERE session_id=%s AND tenant_id=%s "
                "ORDER BY created_at DESC",
                (session_id, tenant_id),
            )
            rows = await cur.fetchall()
            return [
                EvaluationResponse(
                    eval_id=r[0], msg_id=r[1], session_id=r[2], source_id=r[3],
                    score=r[4], comment=r[5], created_at=str(r[6]) if r[6] else "",
                )
                for r in rows
            ]
    finally:
        conn.close()