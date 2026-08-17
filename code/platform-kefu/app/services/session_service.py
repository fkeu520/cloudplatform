"""会话服务 - 集成数据源路由 + RAG + LLM 生成"""
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
from app.services.vector_store import VectorStore
from pathlib import Path
import httpx

logger = logging.getLogger(__name__)

_vector_dir = Path(__file__).parent.parent / "data" / "vector_index"
_vector_store = VectorStore(_vector_dir)

_adapters_registered = False


def ensure_adapters_registered():
    global _adapters_registered
    if _adapters_registered:
        return
    registry.register(FaqAdapter())
    registry.register(KnowledgeAdapter())
    registry.register(ParkEnterpriseAdapter())
    _adapters_registered = True


async def create_session(customer_id=None, customer_name=None, contact=None, channel="web") -> Dict[str, Any]:
    ensure_adapters_registered()
    sid = str(uuid.uuid4())
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "INSERT INTO kefu_session (id, customer_id, customer_name, contact, channel, status) VALUES (%s,%s,%s,%s,%s,%s)",
                (sid, customer_id, customer_name, contact, channel, "AI"),
            )
    finally:
        conn.close()
    return await get_session(sid)


async def get_session(sid: str) -> Optional[Dict[str, Any]]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute("SELECT * FROM kefu_session WHERE id=%s", (sid,))
            row = await cur.fetchone()
            if not row:
                return None
            return _row_to_session(row)
    finally:
        conn.close()


async def list_sessions(status=None, channel=None, limit=50, offset=0) -> List[Dict[str, Any]]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            sql = "SELECT * FROM kefu_session WHERE 1=1"
            args = []
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


async def transfer_to_human(sid: str, agent_id=None, agent_name=None) -> Dict[str, Any]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "UPDATE kefu_session SET status='HUMAN', agent_id=%s, agent_name=%s WHERE id=%s",
                (agent_id, agent_name, sid),
            )
    finally:
        conn.close()
    return await get_session(sid)


async def close_session(sid: str) -> Dict[str, Any]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute("UPDATE kefu_session SET status='CLOSED', end_time=NOW() WHERE id=%s", (sid,))
    finally:
        conn.close()
    return await get_session(sid)


async def rate_session(sid: str, satisfaction: int, comment=None) -> Dict[str, Any]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "UPDATE kefu_session SET satisfaction=%s, satisfaction_comment=%s WHERE id=%s",
                (satisfaction, comment, sid),
            )
    finally:
        conn.close()
    return await get_session(sid)


async def get_messages(sid: str, limit=100) -> List[Dict[str, Any]]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "SELECT * FROM kefu_message WHERE session_id=%s ORDER BY create_time ASC LIMIT %s",
                (sid, limit),
            )
            rows = await cur.fetchall()
            return [_row_to_message(r) for r in rows]
    finally:
        conn.close()


async def save_message(sid: str, role: str, content: str, data_source_refs=None, knowledge_refs=None, model="", latency_ms=0) -> Dict[str, Any]:
    msg_id = str(uuid.uuid4())
    refs_json = json.dumps(data_source_refs, ensure_ascii=False) if data_source_refs else None
    krefs_json = json.dumps(knowledge_refs, ensure_ascii=False) if knowledge_refs else None
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "INSERT INTO kefu_message (msg_id, session_id, role, content, data_source_refs, knowledge_refs, model, latency_ms) VALUES (%s,%s,%s,%s,%s,%s,%s,%s)",
                (msg_id, sid, role, content, refs_json, krefs_json, model, latency_ms),
            )
            await cur.execute(
                "UPDATE kefu_session SET last_message_preview=%s, last_message_at=NOW() WHERE id=%s",
                (content[:255], sid),
            )
    finally:
        conn.close()
    return {"msg_id": msg_id, "session_id": sid, "role": role, "content": content,
            "data_source_refs": data_source_refs or [], "knowledge_refs": knowledge_refs or [],
            "model": model, "latency_ms": latency_ms}


async def chat(sid: str, user_content: str, settings=None) -> Dict[str, Any]:
    """核心对话流程：
    1. 保存用户消息
    2. 数据源路由查询
    3. RAG 向量检索
    4. 合并上下文
    5. LLM 生成回答
    6. 保存 AI 消息
    7. 返回
    """
    ensure_adapters_registered()
    t0 = time.time()

    # 1. 保存用户消息
    user_msg = await save_message(sid, "customer", user_content)

    # 2. 数据源路由
    ds_results = await registry.route_query(user_content)
    ds_refs = []
    context_parts = []
    for source_id, result in ds_results.items():
        for ref in result.refs:
            ds_refs.append(ref.to_dict())
        if result.summary:
            context_parts.append(f"[{source_id}] {result.summary}")
        # 记录命中
        await _record_ds_hit(sid, source_id)

    # 3. RAG 向量检索（独立于 knowledge adapter，作为通用 fallback）
    rag_context = await _rag_search(user_content, top_k=3)
    if rag_context:
        context_parts.append(f"[knowledge-rag] {rag_context}")

    # 4. LLM 生成
    full_context = "\n\n".join(context_parts) if context_parts else "（无外部数据源命中）"
    answer, model_name = await _llm_generate(user_content, full_context, sid, settings)

    latency = int((time.time() - t0) * 1000)

    # 5. 保存 AI 消息
    ai_msg = await save_message(
        sid, "assistant", answer,
        data_source_refs=ds_refs,
        knowledge_refs=[r["entity_id"] for r in ds_refs if r.get("source") == "knowledge"],
        model=model_name,
        latency_ms=latency,
    )

    session = await get_session(sid)
    return {
        "user_message": user_msg,
        "ai_message": ai_msg,
        "session_status": session["status"] if session else "AI",
        "referenced_data_sources": ds_refs,
    }


async def _rag_search(question: str, top_k=3) -> str:
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
                await cur.execute(f"SELECT content FROM chunks WHERE chunk_id IN ({placeholders})", chunk_ids)
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


async def _record_ds_hit(sid: str, source_id: str):
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "INSERT INTO kefu_session_data_source_hit (session_id, data_source_id, hit_count) VALUES (%s,%s,1) "
                "ON DUPLICATE KEY UPDATE hit_count=hit_count+1, last_hit_at=NOW()",
                (sid, source_id),
            )
    finally:
        conn.close()


def _row_to_session(row) -> Dict[str, Any]:
    return {
        "id": row[0], "customer_id": row[1], "customer_name": row[2], "contact": row[3],
        "channel": row[4], "status": row[5], "agent_id": row[6], "agent_name": row[7],
        "start_time": str(row[8]) if row[8] else None, "end_time": str(row[9]) if row[9] else None,
        "satisfaction": row[10], "satisfaction_comment": row[11],
        "last_message_preview": row[12], "last_message_at": str(row[13]) if row[13] else None,
    }


def _row_to_message(row) -> Dict[str, Any]:
    # kefu_message columns: id=0, msg_id=1, session_id=2, role=3, content=4,
    # data_source_refs=5, knowledge_refs=6, model=7, latency_ms=8, create_time=9
    refs_raw = row[5] if len(row) > 5 else None
    krefs_raw = row[6] if len(row) > 6 else None
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
    content = row[4] if len(row) > 4 and row[4] else ""
    return {
        "msg_id": row[1], "session_id": row[2], "role": row[3], "content": content,
        "data_source_refs": refs, "knowledge_refs": krefs,
        "model": row[7] if len(row) > 7 and row[7] else "",
        "latency_ms": row[8] if len(row) > 8 and row[8] else 0,
        "create_time": str(row[9]) if len(row) > 9 and row[9] else "",
    }
