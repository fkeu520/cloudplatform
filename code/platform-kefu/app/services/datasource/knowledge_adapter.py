"""知识库数据源适配器 - 复用现有 FAISS 向量检索"""
from typing import Dict, Any
from app.services.embedder import embedder
from app.services.vector_store import VectorStore
from pathlib import Path
from app.models.database import get_db_connection
from app.services.datasource.base import DataSourceAdapter, DataSourceResult, DataSourceRef


_vector_dir = Path(__file__).parent.parent.parent / "data" / "vector_index"
_vector_store = VectorStore(_vector_dir)


class KnowledgeAdapter(DataSourceAdapter):
    def __init__(self):
        super().__init__(
            id="knowledge", name="知识库", type="vector_search",
            intent_keywords=["知识", "文档", "手册", "流程", "规定", "制度", "说明", "介绍"],
        )

    async def query(self, question: str, params: Dict[str, Any] = None) -> DataSourceResult:
        top_k = params.get("top_k", 3) if params else 3
        vec = embedder.embed([question])[0]
        hits = _vector_store.search(vec, top_k=top_k)
        if not hits:
            return DataSourceResult([], "")
        chunk_ids = [h[0] for h in hits]
        conn = await get_db_connection()
        try:
            async with conn.cursor() as cur:
                placeholders = ",".join(["%s"] * len(chunk_ids))
                await cur.execute(
                    f"SELECT chunk_id, doc_id, content FROM chunks WHERE chunk_id IN ({placeholders})",
                    chunk_ids,
                )
                rows = await cur.fetchall()
                chunk_map = {r[0]: r for r in rows}
                refs = []
                summary_parts = []
                for cid, score in hits:
                    row = chunk_map.get(cid)
                    if not row:
                        continue
                    _, doc_id, content = row
                    refs.append(DataSourceRef("knowledge", "chunk", cid, content[:60], {"doc_id": doc_id, "score": float(score), "content": content[:300]}))
                    summary_parts.append(content[:200])
                return DataSourceResult(refs, "\n---\n".join(summary_parts))
        finally:
            conn.close()
