"""后台文档处理队列 + 固定 worker 池。

设计要点 (P1 异步上传):
- 上传端只把「MinIO put + INSERT」做完就返回, 不阻塞事件循环。
- 实际 extract / chunk / embed 交给专用 ThreadPoolExecutor(max_workers=N),
  经 loop.run_in_executor 入线程 —— 事件循环永远不被同步 CPU 调用占住。
- vector_store.add_vectors 留在事件循环: 它无 await, 保持与 _save()
  里 _all_instances 重载遍历的原子性 (挪到线程会引发竞态)。
- 队列采用 asyncio.Queue(maxsize=settings.doc_queue_maxsize);
  submit 用 put_nowait, 超上限抛 QueueBusy, docs.py 把它映射成 503。
- worker 数 = settings.doc_worker_count, 由 lifespan 启动/关闭, 常驻不重建。
- _process_document 可重入: 开头先把该 doc_id 的旧 chunks + vectors 清掉,
  再写新行 —— 崩溃重入或 reprocess 都走同一路径。
"""
from __future__ import annotations

import asyncio
import logging
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from typing import Optional

from app.config import settings
from app.models.database import get_db_connection

log = logging.getLogger(__name__)


class QueueBusy(RuntimeError):
    """队列已满, 新任务进不来。上层的 docs.py 应把它映射成 HTTP 503。"""


class ProcessingQueue:
    def __init__(self) -> None:
        self._queue: asyncio.Queue[dict] = asyncio.Queue(
            maxsize=settings.doc_queue_maxsize
        )
        self._executor: Optional[ThreadPoolExecutor] = None
        self._workers: list[asyncio.Task] = []
        self._running: bool = False

    # ------------------------------------------------------------------ #
    # 生命周期
    # ------------------------------------------------------------------ #
    def start(self) -> None:
        """在 lifespan 里被调用: 起线程池 + 常驻 worker task。"""
        if self._running:
            return
        self._executor = ThreadPoolExecutor(
            max_workers=settings.doc_executor_workers,
            thread_name_prefix="kefu-doc",
        )
        self._running = True
        for i in range(settings.doc_worker_count):
            t = asyncio.create_task(self._worker(f"doc-worker-{i}"))
            self._workers.append(t)
        log.info("processing_queue started: workers=%d queue_maxsize=%d",
                 settings.doc_worker_count, settings.doc_queue_maxsize)

    def stop(self) -> None:
        """shutdown 时停 worker + 关闭线程池。已排队的任务会在 worker 退出前被消费完。"""
        self._running = False
        for t in self._workers:
            t.cancel()
        self._workers.clear()
        if self._executor:
            self._executor.shutdown(wait=True)
            self._executor = None
        log.info("processing_queue stopped")

    # ------------------------------------------------------------------ #
    # 入队
    # ------------------------------------------------------------------ #
    def submit(self, job: dict) -> None:
        """把一条处理任务丢进队列; 满则抛 QueueBusy。

        job 字段:
            doc_id   str
            tenant_id int
            object_name str (MinIO 中的对象名)
            file_content bytes (已读到内存, 避免 worker 再去 MinIO 下载)
            filename str
            ext str
        """
        try:
            self._queue.put_nowait(job)
        except asyncio.QueueFull:
            raise QueueBusy(
                f"processing queue full ({settings.doc_queue_maxsize} items queued)"
            )

    # ------------------------------------------------------------------ #
    # 对外暴露的 blocking helper (D8)
    # ------------------------------------------------------------------ #
    async def run_blocking(self, fn, *args) -> any:
        """通过自有线程池运行同步阻塞函数; 外部可用以避免直接访问 _executor."""
        loop = asyncio.get_running_loop()
        if self._executor is not None:
            return await loop.run_in_executor(self._executor, fn, *args)
        # 未启动时用默认 executor (线程池仍有效)
        return await loop.run_in_executor(None, fn, *args)

    # ------------------------------------------------------------------ #
    # Worker
    # ------------------------------------------------------------------ #
    async def _worker(self, name: str) -> None:
        while self._running:
            try:
                job = await self._queue.get()
            except asyncio.CancelledError:
                break
            try:
                await self._process_document(**job)
            except Exception as exc:
                # 兜底: 异常要把行置 failed, 避免卡成僵尸
                log.exception("worker %s failed on doc=%s: %s", name, job.get("doc_id"), exc)
                await self._mark_failed(job["doc_id"], job["tenant_id"], str(exc))
            finally:
                self._queue.task_done()

    # ------------------------------------------------------------------ #
    # 核心处理 (重入: 先清旧 chunks/vectors, 再写新的)
    # ------------------------------------------------------------------ #
    async def _process_document(
        self,
        *,
        doc_id: str,
        tenant_id: int,
        object_name: str,
        file_content: bytes,
        filename: str,
        ext: str,
    ) -> None:
        conn = await get_db_connection()
        try:
            async with conn.cursor() as cur:
                await cur.execute(
                    "UPDATE documents SET status='processing', started_at=NOW() "
                    "WHERE doc_id=%s AND tenant_id=%s",
                    (doc_id, tenant_id),
                )
                # 清旧: 幂等 reprocess 入口
                await cur.execute(
                    "SELECT chunk_id FROM chunks WHERE doc_id=%s AND tenant_id=%s",
                    (doc_id, tenant_id),
                )
                old_chunk_ids = [r[0] for r in await cur.fetchall()]
                if old_chunk_ids:
                    from app.services.vector_store import get_store
                    vs = get_store(Path(__file__).parent.parent / "data" / "vector_index")
                    vs.delete_by_chunk_ids(old_chunk_ids)
                    await cur.execute(
                        "DELETE FROM chunks WHERE doc_id=%s AND tenant_id=%s",
                        (doc_id, tenant_id),
                    )
        finally:
            conn.close()

        # 以下是同步阻塞调用, 全部交给 executor offload:
        #   processor.extract_text · chunker.split · embedder.embed
        loop = asyncio.get_running_loop()
        tmp_path = Path(__file__).parent.parent / "data" / "tmp" / object_name
        tmp_path.parent.mkdir(parents=True, exist_ok=True)
        await self.run_blocking(tmp_path.write_bytes, file_content)

        try:
            text = await self.run_blocking(self._extract_text_from_path, tmp_path, ext)
            chunks = await self.run_blocking(self._split_chunks, text)
        finally:
            tmp_path.unlink(missing_ok=True)

        if not chunks:
            await self._mark_ready(doc_id, tenant_id, 0)
            return

        chunk_texts = chunks
        vectors = await self.run_blocking(self._embed_texts, chunk_texts)

        # vector_store.add_vectors 留事件循环 —— 无 await, 天然原子
        from app.services.vector_store import get_store
        vector_store = get_store(Path(__file__).parent.parent / "data" / "vector_index")

        # (D7) 先 INSERT chunk 行, 再 add_vectors, 再 UPDATE vector_id
        # 保证 DB 失败时不会留下孤立向量
        import uuid as _uuid
        conn = await get_db_connection()
        try:
            async with conn.cursor() as cur:
                chunk_ids: list[str] = []
                for i, ct in enumerate(chunk_texts):
                    cid = _uuid.uuid4().hex[:32]
                    chunk_ids.append(cid)
                    await cur.execute(
                        "INSERT INTO chunks (chunk_id, tenant_id, doc_id, content, token_count, index_in_doc) "
                        "VALUES (%s, %s, %s, %s, %s, %s)",
                        (cid, tenant_id, doc_id, ct, self._estimate_tokens(ct), i),
                    )
                vector_ids = vector_store.add_vectors(chunk_ids, vectors)
                for cid, vid in zip(chunk_ids, vector_ids):
                    await cur.execute(
                        "UPDATE chunks SET vector_id=%s WHERE chunk_id=%s AND tenant_id=%s",
                        (vid, cid, tenant_id),
                    )
                await cur.execute(
                    "UPDATE documents SET status='ready', chunk_count=%s, finished_at=NOW() "
                    "WHERE doc_id=%s AND tenant_id=%s",
                    (len(chunk_ids), doc_id, tenant_id),
                )
        finally:
            conn.close()

    # ------------------------------------------------------------------ #
    # 帮助方法 —— 同步阻塞函数体, 由 executor 线程执行
    # ------------------------------------------------------------------ #
    @staticmethod
    def _extract_text_from_path(path: Path, ext: str) -> str:
        from app.services.document_processor import DocumentProcessor
        proc = DocumentProcessor(path.parent)
        return proc.extract_text(path, ext)

    @staticmethod
    def _split_chunks(text: str) -> list[str]:
        from app.services.chunker import Chunker
        return Chunker().split(text)

    @staticmethod
    def _embed_texts(texts: list[str]) -> list:
        from app.services.embedder import embedder
        return embedder.embed(texts)

    @staticmethod
    def _estimate_tokens(text: str) -> int:
        from app.services.chunker import Chunker
        return Chunker().estimate_tokens(text)

    # ------------------------------------------------------------------ #
    # 状态写回
    # ------------------------------------------------------------------ #
    async def _mark_ready(self, doc_id: str, tenant_id: int, chunk_count: int) -> None:
        conn = await get_db_connection()
        try:
            async with conn.cursor() as cur:
                await cur.execute(
                    "UPDATE documents SET status='ready', chunk_count=%s, finished_at=NOW() "
                    "WHERE doc_id=%s AND tenant_id=%s",
                    (chunk_count, doc_id, tenant_id),
                )
        finally:
            conn.close()

    async def _mark_failed(self, doc_id: str, tenant_id: int, error_message: str) -> None:
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


# 全局单例, 由 main.py lifespan 管理
queue: Optional[ProcessingQueue] = None


def get_queue() -> ProcessingQueue:
    global queue
    if queue is None:
        queue = ProcessingQueue()
    return queue
