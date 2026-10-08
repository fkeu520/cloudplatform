import uuid
import io
import hashlib
import logging
from pathlib import Path
from datetime import datetime
from fastapi import APIRouter, UploadFile, File, HTTPException, Request
from minio import Minio
from app.models.schemas import DocumentResponse, DocumentListResponse, DocStatusResponse
from app.models.database import get_db_connection
from app.services.document_processor import DocumentProcessor
from app.services.chunker import Chunker
from app.services.embedder import embedder
from app.services.vector_store import get_store
from app.services.processing_queue import ProcessingQueue, QueueBusy, get_queue
from app.config import settings
from app.core.access import has_permission, normalize_tenant_id

log = logging.getLogger(__name__)

router = APIRouter()
vector_dir = Path(__file__).parent.parent / "data" / "vector_index"
tmp_dir = Path(__file__).parent.parent / "data" / "tmp"

vector_store = get_store(vector_dir)
processor = DocumentProcessor(tmp_dir)
# 2026-09-30: 不再传 512/64 —— 旧值是"词数"语义, 对中文恒不生效,
# 导致整篇文档成为单块并超过 bge-m3 的 8192 token 上限。改用 Chunker 默认值
# (1000/100 字符), 阈值依据见 services/chunker.py 顶部注释。
chunker = Chunker()

minio_client = Minio(
    endpoint=settings.minio_endpoint,
    access_key=settings.minio_access_key,
    secret_key=settings.minio_secret_key,
    secure=settings.minio_secure,
)


def _ensure_bucket():
    if not minio_client.bucket_exists(settings.minio_bucket):
        minio_client.make_bucket(settings.minio_bucket)


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


def _row_to_response(row) -> DocumentResponse:
    """把数据库行转成 DocumentResponse; 兼容旧无 error_message 列的查询结果。"""
    return DocumentResponse(
        doc_id=row[0],
        name=row[1],
        type=row[2],
        size_bytes=row[3],
        upload_time=row[4],
        status=row[5],
        chunk_count=row[6] if len(row) > 6 else 0,
        error_message=row[7] if len(row) > 7 else None,
    )


# ------------------------------------------------------------------ #
# D2: 幂等 —— hash SELECT 必须在 INSERT 之前。先查已有行,命中非 failed
# 直接返回;命中 failed 复用该行;未命中才 INSERT + 入队。
# ------------------------------------------------------------------ #
@router.post("/upload", response_model=DocumentResponse)
async def upload_document(request: Request, file: UploadFile = File(...)):
    tenant_id = _tenant_id(request, "kefu:knowledge:add")
    content = await file.read()
    max_size = settings.max_file_size_mb * 1024 * 1024
    if len(content) > max_size:
        raise HTTPException(400, f"文件大小超过 {settings.max_file_size_mb}MB")

    ext = Path(file.filename).suffix.lower()
    if ext not in ['.' + e for e in settings.allowed_extensions_list]:
        raise HTTPException(400, f"不支持的文件类型: {ext}")

    content_hash = hashlib.sha256(content).hexdigest()

    # D2: 先查幂等 —— 不走 bucket/insert/queue 任何副作用
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "SELECT doc_id, name, type, size_bytes, upload_time, status, chunk_count, error_message "
                "FROM documents WHERE tenant_id=%s AND content_hash=%s",
                (tenant_id, content_hash),
            )
            existing = await cur.fetchone()
            if existing:
                if existing[5] != 'failed':
                    return _row_to_response(existing)
                # failed 行: 复用 doc_id, 重置状态
                await cur.execute(
                    "UPDATE documents SET status='processing', error_message=NULL, started_at=NULL, finished_at=NULL "
                    "WHERE doc_id=%s AND tenant_id=%s",
                    (existing[0], tenant_id),
                )
                await cur.execute(
                    "SELECT file_path, type FROM documents WHERE doc_id=%s AND tenant_id=%s",
                    (existing[0], tenant_id),
                )
                fp_row = await cur.fetchone()
                object_name = fp_row[0] if fp_row else existing[0]
                reused_ext = fp_row[1] if fp_row else ext
                doc_id = existing[0]
                filename = existing[1]
                size_bytes = existing[3]
                upload_time = existing[4]
    finally:
        conn.close()

    # 未命中或 failed 重入: 才做 bucket 检查和 insert
    if not locals().get("doc_id"):
        _ensure_bucket()
        doc_id = str(uuid.uuid4())
        object_name = f"{doc_id}{ext}"
        size_bytes = len(content)
        filename = file.filename
        upload_time = datetime.now().isoformat()

        q_conn = await get_db_connection()
        try:
            async with q_conn.cursor() as cur:
                await cur.execute(
                    "INSERT INTO documents (doc_id, tenant_id, name, type, size_bytes, upload_time, "
                    "status, file_path, content_hash) "
                    "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)",
                    (doc_id, tenant_id, filename, ext, size_bytes, upload_time,
                     'processing', object_name, content_hash)
                )
        finally:
            q_conn.close()

    q = get_queue()
    try:
        q.submit({
            "doc_id": doc_id,
            "tenant_id": tenant_id,
            "object_name": object_name,
            "file_content": content,
            "filename": filename,
            "ext": reused_ext if locals().get("reused_ext") else ext,
        })
    except QueueBusy:
        raise HTTPException(503, "文档处理队列已满, 请稍后重试")

    return DocumentResponse(
        doc_id=doc_id, name=filename, type=ext if not locals().get("reused_ext") else reused_ext,
        size_bytes=size_bytes, upload_time=upload_time,
        status='processing', chunk_count=0, error_message=None,
    )


@router.get("", response_model=DocumentListResponse)
async def list_documents(request: Request):
    tenant_id = _tenant_id(request, "kefu:knowledge")
    conn = await get_db_connection()
    async with conn.cursor() as cur:
        await cur.execute(
            "SELECT doc_id, name, type, size_bytes, upload_time, status, chunk_count, error_message "
            "FROM documents WHERE tenant_id=%s ORDER BY upload_time DESC",
            (tenant_id,),
        )
        rows = await cur.fetchall()
    conn.close()
    items = []
    for row in rows:
        items.append(_row_to_response(row))
    return DocumentListResponse(total=len(items), items=items)


@router.get("/{doc_id}/status", response_model=DocStatusResponse)
async def get_document_status(request: Request, doc_id: str):
    tenant_id = _tenant_id(request, "kefu:knowledge")
    conn = await get_db_connection()
    async with conn.cursor() as cur:
        await cur.execute(
            "SELECT doc_id, status, chunk_count, error_message, started_at, finished_at "
            "FROM documents WHERE doc_id = %s AND tenant_id = %s",
            (doc_id, tenant_id),
        )
        row = await cur.fetchone()
    conn.close()
    if not row:
        raise HTTPException(404, f"文档不存在: {doc_id}")
    return DocStatusResponse(
        doc_id=row[0],
        status=row[1],
        chunk_count=row[2] if row[2] else 0,
        error_message=row[3],
        started_at=row[4].isoformat() if row[4] else None,
        finished_at=row[5].isoformat() if row[5] else None,
    )


@router.delete("/{doc_id}")
async def delete_document(request: Request, doc_id: str):
    tenant_id = _tenant_id(request, "kefu:knowledge:delete")
    conn = await get_db_connection()
    async with conn.cursor() as cur:
        await cur.execute(
            "SELECT doc_id, name, type, size_bytes, upload_time, status, chunk_count, file_path "
            "FROM documents WHERE doc_id = %s AND tenant_id = %s",
            (doc_id, tenant_id),
        )
        row = await cur.fetchone()
        if not row:
            conn.close()
            raise HTTPException(404, f"文档不存在: {doc_id}")
        object_name = row[7]
        await cur.execute(
            "SELECT chunk_id FROM chunks WHERE doc_id = %s AND tenant_id = %s",
            (doc_id, tenant_id),
        )
        chunk_ids = [r[0] for r in await cur.fetchall()]
        if chunk_ids:
            vector_store.delete_by_chunk_ids(chunk_ids)
        await cur.execute(
            "DELETE FROM chunks WHERE doc_id = %s AND tenant_id = %s",
            (doc_id, tenant_id),
        )
        await cur.execute(
            "DELETE FROM documents WHERE doc_id = %s AND tenant_id = %s",
            (doc_id, tenant_id),
        )
    conn.close()

    try:
        minio_client.remove_object(settings.minio_bucket, object_name)
    except Exception:
        pass

    return {"message": "删除成功"}


@router.post("/{doc_id}/reprocess")
async def reprocess_document(request: Request, doc_id: str):
    """reprocess 现在是纯「入队」操作 —— 实际处理挪到 worker 里做。"""
    tenant_id = _tenant_id(request, "kefu:knowledge:edit")
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "SELECT doc_id, name, type, size_bytes, upload_time, status, chunk_count, file_path "
                "FROM documents WHERE doc_id = %s AND tenant_id = %s",
                (doc_id, tenant_id),
            )
            row = await cur.fetchone()
            if not row:
                conn.close()
                raise HTTPException(404, f"文档不存在: {doc_id}")
            # 清旧 chunks/vectors, 重置状态
            await cur.execute(
                "SELECT chunk_id FROM chunks WHERE doc_id = %s AND tenant_id = %s",
                (doc_id, tenant_id),
            )
            chunk_ids = [r[0] for r in await cur.fetchall()]
            if chunk_ids:
                vector_store.delete_by_chunk_ids(chunk_ids)
            await cur.execute(
                "DELETE FROM chunks WHERE doc_id = %s AND tenant_id = %s",
                (doc_id, tenant_id),
            )
            await cur.execute(
                "UPDATE documents SET status = 'processing', chunk_count = 0, error_message=NULL, started_at=NULL, finished_at=NULL "
                "WHERE doc_id = %s AND tenant_id = %s",
                (doc_id, tenant_id),
            )
            object_name = row[7]
    finally:
        conn.close()

    # 读 MinIO 内容 -> 走入队路径 (D6: get_running_loop, D8: run_blocking)
    q = get_queue()
    try:
        content = await q.run_blocking(minio_client.get_object, settings.minio_bucket, object_name)
        # get_object 返回的是 MinioObject, 需要 read+close
        # 上面 run_blocking 返回的结果就是 response 对象本身
        # 需要再 read + close —— 用 lambda 包一层
        resp = content
        content_bytes = await q.run_blocking(resp.read)
        await q.run_blocking(resp.close)
        content = content_bytes
    except Exception as exc:
        # D3: _mark_failed 必须自己开 conn, 不能复用传入的连接
        await _mark_failed(doc_id, tenant_id, f"源文件读取失败: {exc}")
        raise HTTPException(500, f"重新处理失败: 源文件读取失败 {exc}")

    try:
        q.submit({
            "doc_id": doc_id,
            "tenant_id": tenant_id,
            "object_name": object_name,
            "file_content": content,
            "filename": row[1],
            "ext": row[2],
        })
    except QueueBusy:
        raise HTTPException(503, "文档处理队列已满, 请稍后重试")

    return {"message": "重新处理已入队"}


# 启动时从 DB 捞出卡住的 processing 行, 重入 worker 队列。
async def reconcile_processing_docs() -> None:
    """启动恢复: 遍历 status='processing' 的行, 校验 MinIO 对象存在后重入队。

    必须在 start_workers() 之后调用 —— 但任务队列是在 start_workers 内部由
    lifespan 顺序调用, 而 worker 在 start 后已经在 drain 队列, 所以此处 await queue.put
    不会被 worker 饿死 (它们并行 drain)。
    """
    from app.services.processing_queue import get_queue as _gq
    q = _gq()
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "SELECT doc_id, tenant_id, file_path FROM documents WHERE status='processing'"
            )
            rows = await cur.fetchall()
    finally:
        conn.close()

    if not rows:
        return

    for doc_id, tenant_id, object_name in rows:
        if not object_name:
            # D3: _mark_failed 自开 conn
            await _mark_failed(doc_id, tenant_id, "file_path 缺失")
            continue
        try:
            await q.run_blocking(minio_client.stat_object, settings.minio_bucket, object_name)
        except Exception as exc:
            await _mark_failed(doc_id, tenant_id, f"源文件缺失: {exc}")
            continue

        # 读内容以便入队
        try:
            resp = await q.run_blocking(minio_client.get_object, settings.minio_bucket, object_name)
            content = await q.run_blocking(resp.read)
            await q.run_blocking(resp.close)
        except Exception as exc:
            await _mark_failed(doc_id, tenant_id, f"源文件读取失败: {exc}")
            continue

        # 找原文件名/扩展
        fp_conn = await get_db_connection()
        try:
            async with fp_conn.cursor() as cur:
                await cur.execute(
                    "SELECT name, type FROM documents WHERE doc_id=%s AND tenant_id=%s",
                    (doc_id, tenant_id),
                )
                fp = await cur.fetchone()
        finally:
            fp_conn.close()

        filename = fp[0] if fp else object_name
        ext = fp[1] if fp else Path(object_name).suffix.lower()

        try:
            q.submit({
                "doc_id": doc_id,
                "tenant_id": tenant_id,
                "object_name": object_name,
                "file_content": content,
                "filename": filename,
                "ext": ext,
            })
        except QueueBusy:
            # D5: log 已在模块顶部定义
            log.warning("reconcile: queue full, skipping doc_id=%s", doc_id)


# D3: _mark_failed 不再接受 conn 参数 —— 自己开/关连接
async def _mark_failed(doc_id: str, tenant_id: int, error_message: str) -> None:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "UPDATE documents SET status='failed', error_message=%s, finished_at=NOW() "
                "WHERE doc_id=%s AND tenant_id=%s",
                (error_message[:512], doc_id, tenant_id),
            )
    finally:
        conn.close()
