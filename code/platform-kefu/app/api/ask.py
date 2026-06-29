import uuid
import json
import time
import httpx
from pathlib import Path
from fastapi import APIRouter
from app.models.schemas import AskRequest, AskResponse
from app.models.database import get_db_connection
from app.services.embedder import embedder
from app.services.vector_store import VectorStore
from app.config import settings

router = APIRouter()
vector_dir = Path(__file__).parent.parent / "data" / "vector_index"
vector_store = VectorStore(vector_dir)

@router.post("", response_model=AskResponse)
async def ask_question(req: AskRequest):
    start_time = time.time()

    query_vector = embedder.embed_single(req.question)
    results = vector_store.search(query_vector, top_k=settings.rag_top_k, min_score=settings.rag_min_score)

    if not results:
        return AskResponse(
            ask_id=str(uuid.uuid4()),
            question=req.question,
            answer="知识库中没有找到相关内容，请尝试上传更多文档",
            chunks_used=[],
            model=settings.deepseek_model,
            latency_ms=int((time.time() - start_time) * 1000)
        )

    chunk_ids = [r[0] for r in results]
    conn = await get_db_connection()
    async with conn.cursor() as cur:
        placeholders = ','.join(['%s'] * len(chunk_ids))
        await cur.execute(f"SELECT chunk_id, content FROM chunks WHERE chunk_id IN ({placeholders})", chunk_ids)
        chunk_rows = await cur.fetchall()
    conn.close()

    context = "\n\n".join([row[1] for row in chunk_rows])
    prompt = f"""请基于以下知识库内容回答问题：

知识库内容：
{context}

问题：{req.question}

请用中文回答，如果知识库中没有相关信息，请明确说明。"""

    answer = await _call_llm(prompt)

    ask_id = str(uuid.uuid4())
    latency = int((time.time() - start_time) * 1000)

    conn = await get_db_connection()
    async with conn.cursor() as cur:
        await cur.execute(
            "INSERT INTO ask_logs (ask_id, question, answer, latency_ms, chunks_used, model) VALUES (%s, %s, %s, %s, %s, %s)",
            (ask_id, req.question, answer, latency, json.dumps(chunk_ids), settings.deepseek_model)
        )
    conn.close()

    return AskResponse(
        ask_id=ask_id,
        question=req.question,
        answer=answer,
        chunks_used=chunk_ids,
        model=settings.deepseek_model,
        latency_ms=latency
    )

async def _call_llm(prompt: str) -> str:
    url = f"{settings.deepseek_base_url}/chat/completions"
    headers = {
        "Authorization": f"Bearer {settings.deepseek_api_key}",
        "Content-Type": "application/json",
    }
    payload = {
        "model": settings.deepseek_model,
        "messages": [{"role": "user", "content": prompt}],
        "temperature": 0.7,
    }
    async with httpx.AsyncClient(timeout=120) as client:
        resp = await client.post(url, json=payload, headers=headers)
        resp.raise_for_status()
        data = resp.json()
    return data["choices"][0]["message"]["content"]

@router.get("/history")
async def ask_history(page: int = 1, page_size: int = 20):
    conn = await get_db_connection()
    async with conn.cursor() as cur:
        await cur.execute("SELECT COUNT(*) as c FROM ask_logs")
        total = (await cur.fetchone())[0]
        offset = (page - 1) * page_size
        await cur.execute(
            "SELECT ask_id, question, answer, user_id, username, latency_ms, create_time FROM ask_logs ORDER BY create_time DESC LIMIT %s OFFSET %s",
            (page_size, offset)
        )
        rows = await cur.fetchall()
    conn.close()
    items = []
    for row in rows:
        items.append({
            "ask_id": row[0], "question": row[1], "answer": row[2],
            "user_id": row[3], "username": row[4],
            "latency_ms": row[5], "create_time": str(row[6]) if row[6] else ""
        })
    return {"total": total, "page": page, "page_size": page_size, "items": items}
