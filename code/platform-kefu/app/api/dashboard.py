"""评估报告 + 数据看板 API 路由"""
from fastapi import APIRouter
from app.models.schemas import DashboardStatsResponse, EvaluationResponse, EvaluationRequest
from app.models.database import get_db_connection
from app.services import session_service as svc
from datetime import datetime, timedelta
import uuid

router = APIRouter(prefix="/api/kefu", tags=["dashboard"])


@router.get("/dashboard", response_model=DashboardStatsResponse)
async def get_dashboard():
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            # 会话总数
            await cur.execute("SELECT COUNT(*) FROM kefu_session")
            total_sessions = (await cur.fetchone())[0]
            # 消息总数
            await cur.execute("SELECT COUNT(*) FROM kefu_message")
            total_messages = (await cur.fetchone())[0]
            # AI 解决率
            await cur.execute("SELECT COUNT(*) FROM kefu_session WHERE status='AI' AND end_time IS NOT NULL")
            ai_resolved = (await cur.fetchone())[0]
            ai_total = max(total_sessions, 1)
            ai_rate = round(ai_resolved / ai_total * 100, 1)
            # 平均满意度
            await cur.execute("SELECT AVG(satisfaction) FROM kefu_session WHERE satisfaction IS NOT NULL")
            avg_sat = (await cur.fetchone())[0]
            # 24h 会话
            await cur.execute("SELECT COUNT(*) FROM kefu_session WHERE start_time >= NOW() - INTERVAL 1 HOUR * 24")
            recent_24h = (await cur.fetchone())[0]
            # 数据源命中统计
            await cur.execute("SELECT data_source_id, SUM(hit_count) FROM kefu_session_data_source_hit GROUP BY data_source_id")
            ds_hits = {}
            async for row in cur:
                ds_hits[row[0]] = row[1]
            return DashboardStatsResponse(
                total_sessions=total_sessions, total_messages=total_messages,
                ai_resolution_rate=ai_rate, avg_satisfaction=float(avg_sat) if avg_sat else None,
                data_source_hit_counts=ds_hits, recent_24h_sessions=recent_24h,
            )
    finally:
        conn.close()


@router.post("/evaluations", response_model=EvaluationResponse)
async def create_evaluation(req: EvaluationRequest):
    eval_id = str(uuid.uuid4())
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute("SELECT session_id FROM kefu_message WHERE msg_id=%s", (req.msg_id,))
            row = await cur.fetchone()
            if not row:
                # 尝试从 session 中查找
                await cur.execute("SELECT id FROM kefu_session WHERE id=%s", (req.msg_id,))
                row = await cur.fetchone()
                sid = row[0] if row else req.msg_id
            else:
                sid = row[0]
            await cur.execute(
                "INSERT INTO kefu_evaluation (eval_id, msg_id, session_id, source_id, score, comment) VALUES (%s,%s,%s,%s,%s,%s)",
                (eval_id, req.msg_id, sid, req.source_id, req.score, req.comment),
            )
    finally:
        conn.close()
    return EvaluationResponse(
        eval_id=eval_id, msg_id=req.msg_id, session_id=sid,
        source_id=req.source_id, score=req.score, comment=req.comment,
        created_at=datetime.now().isoformat(),
    )


@router.get("/evaluations/{session_id}", response_model=list[EvaluationResponse])
async def list_evaluations(session_id: str):
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "SELECT eval_id, msg_id, session_id, source_id, score, comment, created_at FROM kefu_evaluation WHERE session_id=%s ORDER BY created_at DESC",
                (session_id,),
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