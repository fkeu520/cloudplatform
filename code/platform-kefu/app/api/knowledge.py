import uuid
from pathlib import Path
from fastapi import APIRouter, HTTPException, Request
from app.models.schemas import ChunkResponse
from app.models.database import get_db_connection
from app.services.vector_store import get_store
from app.services.embedder import embedder
from app.config import settings
from app.core.access import has_permission, normalize_tenant_id

router = APIRouter()
vector_dir = Path(__file__).parent.parent / "data" / "vector_index"
vector_store = get_store(vector_dir)


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


@router.get("/chunks")
async def list_chunks(request: Request, doc_id: str = None, page: int = 1, page_size: int = 20):
    tenant_id = _tenant_id(request, "kefu:knowledge")
    conn = await get_db_connection()
    async with conn.cursor() as cur:
        if doc_id:
            await cur.execute(
                "SELECT COUNT(*) as total FROM chunks WHERE doc_id = %s AND tenant_id = %s",
                (doc_id, tenant_id),
            )
            total_row = await cur.fetchone()
            total = total_row[0] if total_row else 0
            await cur.execute(
                "SELECT chunk_id, doc_id, content, token_count, index_in_doc, vector_id FROM chunks WHERE doc_id = %s AND tenant_id = %s "
                "ORDER BY index_in_doc LIMIT %s OFFSET %s",
                (doc_id, tenant_id, page_size, (page - 1) * page_size)
            )
        else:
            await cur.execute(
                "SELECT COUNT(*) as total FROM chunks WHERE tenant_id = %s",
                (tenant_id,),
            )
            total_row = await cur.fetchone()
            total = total_row[0] if total_row else 0
            await cur.execute(
                "SELECT chunk_id, doc_id, content, token_count, index_in_doc, vector_id FROM chunks WHERE tenant_id = %s "
                "ORDER BY doc_id, index_in_doc LIMIT %s OFFSET %s",
                (tenant_id, page_size, (page - 1) * page_size)
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
async def rebuild_index(request: Request):
    tenant_id = _tenant_id(request, "kefu:knowledge:edit")
    conn = await get_db_connection()
    async with conn.cursor() as cur:
        # The FAISS file is shared by all tenants. Rebuild only this tenant's
        # vectors by removing its existing chunk IDs first; never clear the
        # shared index, which would erase other tenants' vectors.
        await cur.execute(
            "SELECT chunk_id, content FROM chunks WHERE tenant_id=%s ORDER BY chunk_id",
            (tenant_id,),
        )
        rows = await cur.fetchall()
    conn.close()

    chunk_ids = [r[0] for r in rows]
    if chunk_ids:
        vector_store.delete_by_chunk_ids(chunk_ids)
    if not rows:
        return {"message": "没有切片需要重建"}

    texts = [r[1] for r in rows]
    vectors = embedder.embed(texts)
    vector_store.add_vectors(chunk_ids, vectors)

    return {"message": f"索引重建完成，共 {len(chunk_ids)} 个切片"}

@router.get("/docs")
async def list_knowledge_docs(request: Request):
    tenant_id = _tenant_id(request, "kefu:knowledge")
    conn = await get_db_connection()
    async with conn.cursor() as cur:
        await cur.execute(
            "SELECT doc_id, name, type, size_bytes, upload_time, status, chunk_count "
            "FROM documents WHERE tenant_id=%s ORDER BY upload_time DESC",
            (tenant_id,),
        )
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
async def knowledge_stats(request: Request):
    tenant_id = _tenant_id(request, "kefu:knowledge")
    conn = await get_db_connection()
    async with conn.cursor() as cur:
        await cur.execute("SELECT COUNT(*) as c FROM documents WHERE tenant_id=%s", (tenant_id,))
        doc_count = (await cur.fetchone())[0]
        await cur.execute("SELECT COUNT(*) as c FROM chunks WHERE tenant_id=%s", (tenant_id,))
        chunk_count = (await cur.fetchone())[0]
        await cur.execute(
            "SELECT COALESCE(SUM(size_bytes), 0) as s FROM documents WHERE tenant_id=%s",
            (tenant_id,),
        )
        total_bytes = (await cur.fetchone())[0]
    conn.close()
    return {
        "document_count": doc_count,
        "chunk_count": chunk_count,
        "total_size_mb": round(total_bytes / 1024 / 1024, 1)
    }