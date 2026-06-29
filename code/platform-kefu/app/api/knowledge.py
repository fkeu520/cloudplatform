import uuid
from pathlib import Path
from fastapi import APIRouter, HTTPException
from app.models.schemas import ChunkResponse
from app.models.database import get_db_connection
from app.services.vector_store import VectorStore
from app.services.embedder import embedder
from app.config import settings

router = APIRouter()
vector_dir = Path(__file__).parent.parent / "data" / "vector_index"
vector_store = VectorStore(vector_dir)

@router.get("/chunks")
async def list_chunks(doc_id: str = None, page: int = 1, page_size: int = 20):
    conn = await get_db_connection()
    async with conn.cursor() as cur:
        if doc_id:
            await cur.execute("SELECT COUNT(*) as total FROM chunks WHERE doc_id = %s", (doc_id,))
            total_row = await cur.fetchone()
            total = total_row[0] if total_row else 0
            await cur.execute(
                "SELECT * FROM chunks WHERE doc_id = %s ORDER BY index_in_doc LIMIT %s OFFSET %s",
                (doc_id, page_size, (page - 1) * page_size)
            )
        else:
            await cur.execute("SELECT COUNT(*) as total FROM chunks")
            total_row = await cur.fetchone()
            total = total_row[0] if total_row else 0
            await cur.execute(
                "SELECT * FROM chunks ORDER BY doc_id, index_in_doc LIMIT %s OFFSET %s",
                (page_size, (page - 1) * page_size)
            )
        rows = await cur.fetchall()
    conn.close()

    items = []
    for row in rows:
        items.append(ChunkResponse(
            chunk_id=row[0], doc_id=row[1], content=row[2],
            token_count=row[3], index_in_doc=row[4], vector_id=row[5]
        ))
    return {"total": total, "page": page, "page_size": page_size, "items": items}

@router.post("/rebuild-index")
async def rebuild_index():
    conn = await get_db_connection()
    async with conn.cursor() as cur:
        await cur.execute("SELECT chunk_id, content FROM chunks")
        rows = await cur.fetchall()
    conn.close()

    if not rows:
        return {"message": "没有切片需要重建"}

    vector_store.clear()
    chunk_ids = [r[0] for r in rows]
    texts = [r[1] for r in rows]
    vectors = embedder.embed(texts)
    vector_store.add_vectors(chunk_ids, vectors)

    return {"message": f"索引重建完成，共 {len(chunk_ids)} 个切片"}

@router.get("/docs")
async def list_knowledge_docs():
    conn = await get_db_connection()
    async with conn.cursor() as cur:
        await cur.execute("SELECT doc_id, name, type, size_bytes, upload_time, status, chunk_count FROM documents ORDER BY upload_time DESC")
        rows = await cur.fetchall()
    conn.close()
    items = []
    for row in rows:
        items.append({
            "doc_id": row[0], "name": row[1], "type": row[2],
            "size_bytes": row[3], "upload_time": row[4],
            "status": row[5], "chunk_count": row[6]
        })
    return {"total": len(items), "items": items}

@router.get("/stats")
async def knowledge_stats():
    conn = await get_db_connection()
    async with conn.cursor() as cur:
        await cur.execute("SELECT COUNT(*) as c FROM documents")
        doc_count = (await cur.fetchone())[0]
        await cur.execute("SELECT COUNT(*) as c FROM chunks")
        chunk_count = (await cur.fetchone())[0]
        await cur.execute("SELECT COALESCE(SUM(size_bytes), 0) as s FROM documents")
        total_bytes = (await cur.fetchone())[0]
    conn.close()
    return {
        "document_count": doc_count,
        "chunk_count": chunk_count,
        "total_size_mb": round(total_bytes / 1024 / 1024, 1)
    }