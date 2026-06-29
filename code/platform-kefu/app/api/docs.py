import uuid
import io
from pathlib import Path
from fastapi import APIRouter, UploadFile, File, HTTPException
from minio import Minio
from app.models.schemas import DocumentResponse, DocumentListResponse
from app.models.database import get_db_connection
from app.services.document_processor import DocumentProcessor
from app.services.chunker import Chunker
from app.services.embedder import embedder
from app.services.vector_store import VectorStore
from app.config import settings

router = APIRouter()
vector_dir = Path(__file__).parent.parent / "data" / "vector_index"
tmp_dir = Path(__file__).parent.parent / "data" / "tmp"

vector_store = VectorStore(vector_dir)
processor = DocumentProcessor(tmp_dir)
chunker = Chunker(chunk_size=512, overlap=64)

minio_client = Minio(
    endpoint=settings.minio_endpoint,
    access_key=settings.minio_access_key,
    secret_key=settings.minio_secret_key,
    secure=settings.minio_secure,
)

def _ensure_bucket():
    if not minio_client.bucket_exists(settings.minio_bucket):
        minio_client.make_bucket(settings.minio_bucket)

@router.post("/upload")
async def upload_document(file: UploadFile = File(...)):
    content = await file.read()
    max_size = settings.max_file_size_mb * 1024 * 1024
    if len(content) > max_size:
        raise HTTPException(400, f"文件大小超过 {settings.max_file_size_mb}MB")

    ext = Path(file.filename).suffix.lower()
    if ext not in ['.' + e for e in settings.allowed_extensions_list]:
        raise HTTPException(400, f"不支持的文件类型: {ext}")

    _ensure_bucket()
    doc_id = str(uuid.uuid4())
    object_name = f"{doc_id}{ext}"
    minio_client.put_object(
        settings.minio_bucket, object_name,
        io.BytesIO(content), len(content),
        content_type="application/octet-stream"
    )

    conn = await get_db_connection()
    async with conn.cursor() as cur:
        from datetime import datetime
        await cur.execute(
            "INSERT INTO documents (doc_id, name, type, size_bytes, upload_time, status, file_path) VALUES (%s, %s, %s, %s, %s, %s, %s)",
            (doc_id, file.filename, ext, len(content), datetime.now().isoformat(), 'processing', object_name)
        )
    conn.close()

    try:
        tmp_path = tmp_dir / object_name
        tmp_path.parent.mkdir(parents=True, exist_ok=True)
        tmp_path.write_bytes(content)
        text = processor.extract_text(tmp_path, ext)
        chunks = chunker.split(text)
        tmp_path.unlink(missing_ok=True)

        if chunks:
            chunk_texts = chunks
            vectors = embedder.embed(chunk_texts)

            conn = await get_db_connection()
            async with conn.cursor() as cur:
                chunk_ids = []
                for i, chunk_text in enumerate(chunk_texts):
                    cid = str(uuid.uuid4())
                    chunk_ids.append(cid)
                    await cur.execute(
                        "INSERT INTO chunks (chunk_id, doc_id, content, token_count, index_in_doc) VALUES (%s, %s, %s, %s, %s)",
                        (cid, doc_id, chunk_text, chunker.estimate_tokens(chunk_text), i)
                    )
                vector_ids = vector_store.add_vectors(chunk_ids, vectors)
                for cid, vid in zip(chunk_ids, vector_ids):
                    await cur.execute("UPDATE chunks SET vector_id = %s WHERE chunk_id = %s", (vid, cid))
                await cur.execute(
                    "UPDATE documents SET status = 'ready', chunk_count = %s WHERE doc_id = %s",
                    (len(chunks), doc_id)
                )
            conn.close()
    except Exception as e:
        conn = await get_db_connection()
        async with conn.cursor() as cur:
            await cur.execute("UPDATE documents SET status = 'failed' WHERE doc_id = %s", (doc_id,))
        conn.close()
        raise HTTPException(500, f"文档处理失败: {str(e)}")

    return DocumentResponse(
        doc_id=doc_id, name=file.filename, type=ext,
        size_bytes=len(content), upload_time=datetime.now().isoformat(),
        status='ready', chunk_count=len(chunks) if 'chunks' in dir() else 0
    )

@router.get("", response_model=DocumentListResponse)
async def list_documents():
    conn = await get_db_connection()
    async with conn.cursor() as cur:
        await cur.execute("SELECT * FROM documents ORDER BY upload_time DESC")
        rows = await cur.fetchall()
    conn.close()
    items = []
    for row in rows:
        items.append(DocumentResponse(
            doc_id=row[0], name=row[1], type=row[2],
            size_bytes=row[3], upload_time=row[4],
            status=row[5], chunk_count=row[6]
        ))
    return DocumentListResponse(total=len(items), items=items)

@router.delete("/{doc_id}")
async def delete_document(doc_id: str):
    conn = await get_db_connection()
    async with conn.cursor() as cur:
        await cur.execute("SELECT * FROM documents WHERE doc_id = %s", (doc_id,))
        row = await cur.fetchone()
        if not row:
            conn.close()
            raise HTTPException(404, f"文档不存在: {doc_id}")
        object_name = row[7]
        await cur.execute("SELECT chunk_id FROM chunks WHERE doc_id = %s", (doc_id,))
        chunk_ids = [r[0] for r in await cur.fetchall()]
        if chunk_ids:
            vector_store.delete_by_chunk_ids(chunk_ids)
        await cur.execute("DELETE FROM chunks WHERE doc_id = %s", (doc_id,))
        await cur.execute("DELETE FROM documents WHERE doc_id = %s", (doc_id,))
    conn.close()

    try:
        minio_client.remove_object(settings.minio_bucket, object_name)
    except Exception:
        pass

    return {"message": "删除成功"}

@router.post("/{doc_id}/reprocess")
async def reprocess_document(doc_id: str):
    conn = await get_db_connection()
    async with conn.cursor() as cur:
        await cur.execute("SELECT * FROM documents WHERE doc_id = %s", (doc_id,))
        row = await cur.fetchone()
        if not row:
            conn.close()
            raise HTTPException(404, f"文档不存在: {doc_id}")
        await cur.execute("SELECT chunk_id FROM chunks WHERE doc_id = %s", (doc_id,))
        chunk_ids = [r[0] for r in await cur.fetchall()]
        if chunk_ids:
            vector_store.delete_by_chunk_ids(chunk_ids)
        await cur.execute("DELETE FROM chunks WHERE doc_id = %s", (doc_id,))
        await cur.execute("UPDATE documents SET status = 'processing', chunk_count = 0 WHERE doc_id = %s", (doc_id,))
    conn.close()

    try:
        response = minio_client.get_object(settings.minio_bucket, row[7])
        content = response.read()
        response.close()

        tmp_path = tmp_dir / row[7]
        tmp_path.parent.mkdir(parents=True, exist_ok=True)
        tmp_path.write_bytes(content)
        text = processor.extract_text(tmp_path, row[2])
        chunks = chunker.split(text)
        tmp_path.unlink(missing_ok=True)

        if chunks:
            vectors = embedder.embed(chunks)
            conn = await get_db_connection()
            async with conn.cursor() as cur:
                cids = []
                for i, ct in enumerate(chunks):
                    cid = str(uuid.uuid4())
                    cids.append(cid)
                    await cur.execute(
                        "INSERT INTO chunks (chunk_id, doc_id, content, token_count, index_in_doc) VALUES (%s, %s, %s, %s, %s)",
                        (cid, doc_id, ct, chunker.estimate_tokens(ct), i)
                    )
                vids = vector_store.add_vectors(cids, vectors)
                for cid, vid in zip(cids, vids):
                    await cur.execute("UPDATE chunks SET vector_id = %s WHERE chunk_id = %s", (vid, cid))
                await cur.execute("UPDATE documents SET status = 'ready', chunk_count = %s WHERE doc_id = %s", (len(chunks), doc_id))
            conn.close()

        return {"message": "重新处理完成"}
    except Exception as e:
        conn = await get_db_connection()
        async with conn.cursor() as cur:
            await cur.execute("UPDATE documents SET status = 'failed' WHERE doc_id = %s", (doc_id,))
        conn.close()
        raise HTTPException(500, f"重新处理失败: {str(e)}")
