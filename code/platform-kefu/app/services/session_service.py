"""会话服务 - 集成数据源路由 + RAG + LLM 生成

2026-09-24 P0 租户隔离: 所有 session/message 的 SELECT/INSERT/UPDATE/DELETE
均附加 tenant_id 过滤 (平台管理员 userType==2 除外)。缺失 tenantId 时调用方
应拒绝请求 (由 auth_middleware / sessions API 层保证)。
"""
import uuid
import json
import time
import logging
from typing import Optional, List, Dict, Any
from app.models.database import get_db_connection
from app.services.datasource.base import registry
from app.services.datasource.faq_adapter import FaqAdapter
from app.services.datasource.knowledge_adapter import KnowledgeAdapter
from app.services.datasource.park_enterprise_adapter import ParkEnterpriseAdapter
from app.services.embedder import embedder
from app.services.vector_store import get_store
from pathlib import Path
import httpx

logger = logging.getLogger(__name__)


class SessionNotFoundError(LookupError):
    """Raised when a tenant attempts to use a session it does not own."""


_vector_dir = Path(__file__).parent.parent / "data" / "vector_index"
_vector_store = get_store(_vector_dir)

_SESSION_COLUMNS = (
    "id, tenant_id, customer_id, customer_name, contact, channel, status, "
    "agent_id, agent_name, start_time, end_time, satisfaction, satisfaction_comment, "
    "last_message_preview, last_message_at"
)
_MESSAGE_COLUMNS = (
    "id, msg_id, tenant_id, session_id, role, content, data_source_refs, "
    "knowledge_refs, model, latency_ms, create_time"
)

_adapters_registered = False


def ensure_adapters_registered():
    global _adapters_registered
    if _adapters_registered:
        return
    registry.register(FaqAdapter())
    registry.register(KnowledgeAdapter())
    registry.register(ParkEnterpriseAdapter())
    _adapters_registered = True


async def create_session(
    tenant_id: Optional[int] = None,
    customer_id=None,
    customer_name=None,
    contact=None,
    channel="web",
) -> Dict[str, Any]:
    # Platform-admin reads may be cross-tenant, but writes are quarantined in
    # tenant 0 rather than being written without an owner.
    tenant_id = 0 if tenant_id is None else tenant_id
    ensure_adapters_registered()
    sid = str(uuid.uuid4())
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "INSERT INTO kefu_session "
                "(id, tenant_id, customer_id, customer_name, contact, channel, status) "
                "VALUES (%s,%s,%s,%s,%s,%s,%s)",
                (sid, tenant_id, customer_id, customer_name, contact, channel, "AI"),
            )
    finally:
        conn.close()
    return await get_session(sid, tenant_id)


async def get_session(sid: str, tenant_id: Optional[int] = None) -> Optional[Dict[str, Any]]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            if tenant_id is not None:
                await cur.execute(
                    f"SELECT {_SESSION_COLUMNS} FROM kefu_session "
                    "WHERE id=%s AND tenant_id=%s",
                    (sid, tenant_id),
                )
            else:
                # 平台管理员 (userType==2) 跨租户可见, 不限制 tenant_id
                await cur.execute(
                    f"SELECT {_SESSION_COLUMNS} FROM kefu_session WHERE id=%s",
                    (sid,),
                )
            row = await cur.fetchone()
            if not row:
                return None
            return _row_to_session(row)
    finally:
        conn.close()


async def list_sessions(
    tenant_id: Optional[int] = None,
    status=None,
    channel=None,
    limit=50,
    offset=0,
) -> List[Dict[str, Any]]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            sql = f"SELECT {_SESSION_COLUMNS} FROM kefu_session WHERE 1=1"
            args = []
            # tenant_id 为 None 表示平台管理员, 不加 tenant 过滤
            if tenant_id is not None:
                sql += " AND tenant_id=%s"
                args.append(tenant_id)
            if status:
                sql += " AND status=%s"
                args.append(status)
            if channel:
                sql += " AND channel=%s"
                args.append(channel)
            sql += " ORDER BY start_time DESC LIMIT %s OFFSET %s"
            args.extend([limit, offset])
            await cur.execute(sql, args)
            rows = await cur.fetchall()
            return [_row_to_session(r) for r in rows]
    finally:
        conn.close()


async def transfer_to_human(
    sid: str,
    tenant_id: Optional[int] = None,
    agent_id=None,
    agent_name=None,
) -> Dict[str, Any]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            if tenant_id is not None:
                await cur.execute(
                    "UPDATE kefu_session SET status='HUMAN', agent_id=%s, agent_name=%s "
                    "WHERE id=%s AND tenant_id=%s",
                    (agent_id, agent_name, sid, tenant_id),
                )
            else:
                await cur.execute(
                    "UPDATE kefu_session SET status='HUMAN', agent_id=%s, agent_name=%s "
                    "WHERE id=%s",
                    (agent_id, agent_name, sid),
                )
    finally:
        conn.close()
    return await get_session(sid, tenant_id)


async def close_session(
    sid: str,
    tenant_id: Optional[int] = None,
) -> Dict[str, Any]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            if tenant_id is not None:
                await cur.execute(
                    "UPDATE kefu_session SET status='CLOSED', end_time=NOW() "
                    "WHERE id=%s AND tenant_id=%s",
                    (sid, tenant_id),
                )
            else:
                await cur.execute(
                    "UPDATE kefu_session SET status='CLOSED', end_time=NOW() WHERE id=%s",
                    (sid,),
                )
    finally:
        conn.close()
    return await get_session(sid, tenant_id)


async def rate_session(
    sid: str,
    tenant_id: Optional[int] = None,
    satisfaction: int = 0,
    comment=None,
) -> Dict[str, Any]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            if tenant_id is not None:
                await cur.execute(
                    "UPDATE kefu_session SET satisfaction=%s, satisfaction_comment=%s "
                    "WHERE id=%s AND tenant_id=%s",
                    (satisfaction, comment, sid, tenant_id),
                )
            else:
                await cur.execute(
                    "UPDATE kefu_session SET satisfaction=%s, satisfaction_comment=%s "
                    "WHERE id=%s",
                    (satisfaction, comment, sid),
                )
    finally:
        conn.close()
    return await get_session(sid, tenant_id)


async def get_messages(
    sid: str,
    tenant_id: Optional[int] = None,
    limit=100,
) -> List[Dict[str, Any]]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            if tenant_id is not None:
                await cur.execute(
                    f"SELECT {_MESSAGE_COLUMNS} FROM kefu_message "
                    "WHERE session_id=%s AND tenant_id=%s "
                    "ORDER BY create_time ASC LIMIT %s",
                    (sid, tenant_id, limit),
                )
            else:
                await cur.execute(
                    f"SELECT {_MESSAGE_COLUMNS} FROM kefu_message "
                    "WHERE session_id=%s ORDER BY create_time ASC LIMIT %s",
                    (sid, limit),
                )
            rows = await cur.fetchall()
            return [_row_to_message(r) for r in rows]
    finally:
        conn.close()


async def save_message(
    sid: str,
    tenant_id: Optional[int],
    role: str,
    content: str,
    data_source_refs=None,
    knowledge_refs=None,
    model="",
    latency_ms=0,
) -> Dict[str, Any]:
    tenant_id = 0 if tenant_id is None else tenant_id
    msg_id = str(uuid.uuid4())
    refs_json = json.dumps(data_source_refs, ensure_ascii=False) if data_source_refs else None
    krefs_json = json.dumps(knowledge_refs, ensure_ascii=False) if knowledge_refs else None
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "INSERT INTO kefu_message "
                "(msg_id, tenant_id, session_id, role, content, data_source_refs, "
                " knowledge_refs, model, latency_ms) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)",
                (msg_id, tenant_id, sid, role, content, refs_json, krefs_json, model, latency_ms),
            )
            await cur.execute(
                "SELECT create_time FROM kefu_message WHERE msg_id=%s",
                (msg_id,),
            )
            row = await cur.fetchone()
            create_time = str(row[0]) if row else ""
            await cur.execute(
                "UPDATE kefu_session SET last_message_preview=%s, last_message_at=NOW() "
                "WHERE id=%s AND tenant_id=%s",
                (content[:255], sid, tenant_id),
            )
    finally:
        conn.close()
    return {
        "msg_id": msg_id,
        "session_id": sid,
        "tenant_id": tenant_id,
        "role": role,
        "content": content,
        "data_source_refs": data_source_refs or [],
        "knowledge_refs": knowledge_refs or [],
        "model": model,
        "latency_ms": latency_ms,
        "create_time": create_time,
    }


async def chat(
    sid: str,
    tenant_id: Optional[int],
    user_content: str,
    settings=None,
) -> Dict[str, Any]:
    """核心对话流程：
    1. 保存用户消息
    2. 数据源路由查询
    3. RAG 向量检索
    4. 合并上下文
    5. LLM 生成回答
    6. 保存 AI 消息
    7. 返回
    """
    tenant_id = 0 if tenant_id is None else tenant_id
    if await get_session(sid, tenant_id) is None:
        raise SessionNotFoundError(f"会话不存在或无权访问: {sid}")
    ensure_adapters_registered()
    t0 = time.time()

    # 1. 保存用户消息 (tenant_id 必须非 None，否则上层已拒绝)
    user_msg = await save_message(sid, tenant_id, "customer", user_content)

    # 2. 数据源路由
    ds_results = await registry.route_query(user_content, tenant_id=tenant_id)
    ds_refs = []
    context_parts = []
    for source_id, result in ds_results.items():
        for ref in result.refs:
            ds_refs.append(ref.to_dict())
        if result.summary:
            context_parts.append(f"[{source_id}] {result.summary}")
        # 记录命中
        await _record_ds_hit(sid, source_id, tenant_id)

    # 3. RAG 向量检索（独立于 knowledge adapter，作为通用 fallback）
    rag_context = await _rag_search(user_content, tenant_id=tenant_id, top_k=3)
    if rag_context:
        context_parts.append(f"[knowledge-rag] {rag_context}")

    # 4. LLM 生成
    full_context = "\n\n".join(context_parts) if context_parts else "（无外部数据源命中）"
    answer, model_name = await _llm_generate(user_content, full_context, sid, settings)

    latency = int((time.time() - t0) * 1000)

    # 5. 保存 AI 消息
    ai_msg = await save_message(
        sid, tenant_id, "assistant", answer,
        data_source_refs=ds_refs,
        knowledge_refs=[r["entity_id"] for r in ds_refs if r.get("source") == "knowledge"],
        model=model_name,
        latency_ms=latency,
    )

    session = await get_session(sid, tenant_id)
    return {
        "user_message": user_msg,
        "ai_message": ai_msg,
        "session_status": session["status"] if session else "AI",
        "referenced_data_sources": ds_refs,
    }


async def _rag_search(question: str, tenant_id: int, top_k=3) -> str:
    try:
        vec = embedder.embed([question])[0]
        hits = _vector_store.search(vec, top_k=top_k)
        if not hits:
            return ""
        chunk_ids = [h[0] for h in hits]
        conn = await get_db_connection()
        try:
            async with conn.cursor() as cur:
                placeholders = ",".join(["%s"] * len(chunk_ids))
                await cur.execute(
                    f"SELECT content FROM chunks WHERE chunk_id IN ({placeholders}) "
                    "AND tenant_id=%s",
                    chunk_ids + [tenant_id],
                )
                rows = await cur.fetchall()
                return "\n---\n".join(r[0][:300] for r in rows)
        finally:
            conn.close()
    except Exception as e:
        logger.warning(f"[RAG] 检索失败: {e}")
        return ""


async def _llm_generate(question: str, context: str, sid: str, settings) -> tuple:
    """调用 DeepSeek LLM 生成回答"""
    if not settings or not settings.deepseek_api_key:
        return _mock_llm_response(question, context), "mock"

    system_prompt = f"""你是云枢园区的智能客服助手。请基于以下数据源信息回答用户问题。
如果数据源信息足够，请直接引用并给出准确回答。
如果数据源信息不足，请诚实说明，并引导用户联系人工客服。

【数据源信息】
{context}
"""
    payload = {
        "model": settings.deepseek_model,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": question},
        ],
        "max_tokens": 800,
        "temperature": 0.3,
    }
    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            resp = await client.post(
                f"{settings.deepseek_base_url}/chat/completions",
                json=payload,
                headers={"Authorization": f"Bearer {settings.deepseek_api_key}"},
            )
            resp.raise_for_status()
            data = resp.json()
            answer = data["choices"][0]["message"]["content"]
            return answer, settings.deepseek_model
    except Exception as e:
        logger.error(f"[LLM] DeepSeek 调用失败: {e}")
        return _mock_llm_response(question, context), "mock-fallback"


def _mock_llm_response(question: str, context: str) -> str:
    if context and context != "（无外部数据源命中）":
        return f"根据数据源信息：\n\n{context}\n\n如果您需要更详细的帮助，可以转接人工客服。"
    return f"抱歉，我暂时无法回答「{question}」。您可以尝试换一种问法，或转接人工客服。"


async def _record_ds_hit(sid: str, source_id: str, tenant_id: int):
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "INSERT INTO kefu_session_data_source_hit "
                "(tenant_id, session_id, data_source_id, hit_count) VALUES (%s,%s,%s,1) "
                "ON DUPLICATE KEY UPDATE hit_count=hit_count+1, last_hit_at=NOW()",
                (tenant_id, sid, source_id),
            )
    finally:
        conn.close()


def _row_to_session(row) -> Dict[str, Any]:
    # kefu_session columns (with tenant_id):
    # id=0, tenant_id=1, customer_id=2, customer_name=3, contact=4, channel=5,
    # status=6, agent_id=7, agent_name=8, start_time=9, end_time=10,
    # satisfaction=11, satisfaction_comment=12, last_message_preview=13, last_message_at=14
    return {
        "id": row[0],
        "tenant_id": row[1] if len(row) > 1 else None,
        "customer_id": row[2] if len(row) > 2 else None,
        "customer_name": row[3] if len(row) > 3 else None,
        "contact": row[4] if len(row) > 4 else None,
        "channel": row[5] if len(row) > 5 else "web",
        "status": row[6] if len(row) > 6 else "AI",
        "agent_id": row[7] if len(row) > 7 else None,
        "agent_name": row[8] if len(row) > 8 else None,
        "start_time": str(row[9]) if len(row) > 9 and row[9] else None,
        "end_time": str(row[10]) if len(row) > 10 and row[10] else None,
        "satisfaction": row[11] if len(row) > 11 else None,
        "satisfaction_comment": row[12] if len(row) > 12 else None,
        "last_message_preview": row[13] if len(row) > 13 else None,
        "last_message_at": str(row[14]) if len(row) > 14 and row[14] else None,
    }


def _row_to_message(row) -> Dict[str, Any]:
    # kefu_message columns (with tenant_id):
    # id=0, msg_id=1, tenant_id=2, session_id=3, role=4, content=5,
    # data_source_refs=6, knowledge_refs=7, model=8, latency_ms=9, create_time=10
    tenant_id = row[2] if len(row) > 2 else 0
    refs_raw = row[6] if len(row) > 6 else None
    krefs_raw = row[7] if len(row) > 7 else None
    refs = []
    krefs = []
    try:
        if refs_raw:
            refs = json.loads(refs_raw) if isinstance(refs_raw, str) else refs_raw
    except (json.JSONDecodeError, TypeError):
        refs = []
    try:
        if krefs_raw:
            krefs = json.loads(krefs_raw) if isinstance(krefs_raw, str) else krefs_raw
    except (json.JSONDecodeError, TypeError):
        krefs = []
    content = row[5] if len(row) > 5 and row[5] else ""
    return {
        "msg_id": row[1],
        "tenant_id": tenant_id,
        "session_id": row[3],
        "role": row[4],
        "content": content,
        "data_source_refs": refs,
        "knowledge_refs": krefs,
        "model": row[8] if len(row) > 8 and row[8] else "",
        "latency_ms": row[9] if len(row) > 9 and row[9] else 0,
        "create_time": str(row[10]) if len(row) > 10 and row[10] else "",
    }
