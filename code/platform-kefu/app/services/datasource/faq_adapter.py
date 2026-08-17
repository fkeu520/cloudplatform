"""FAQ 数据源适配器 - 从 kefu_faq 表查询"""
import uuid
from typing import Dict, Any
from app.models.database import get_db_connection
from app.services.datasource.base import DataSourceAdapter, DataSourceResult, DataSourceRef


class FaqAdapter(DataSourceAdapter):
    def __init__(self):
        super().__init__(
            id="faq", name="FAQ 库", type="internal",
            intent_keywords=["FAQ", "常见问题", "怎么", "如何", "多少", "能不能", "可以吗"],
        )

    async def query(self, question: str, params: Dict[str, Any] = None) -> DataSourceResult:
        conn = await get_db_connection()
        try:
            async with conn.cursor() as cur:
                await cur.execute(
                    "SELECT faq_id, question, answer, category FROM kefu_faq WHERE enabled=1 AND MATCH(question) AGAINST(%s) LIMIT 3",
                    (question,),
                )
                rows = await cur.fetchall()
                if not rows:
                    # fallback: LIKE
                    await cur.execute(
                        "SELECT faq_id, question, answer, category FROM kefu_faq WHERE enabled=1 AND question LIKE %s LIMIT 3",
                        (f"%{question[:30]}%",),
                    )
                    rows = await cur.fetchall()
                refs = []
                summary_parts = []
                for r in rows:
                    faq_id, q, a, cat = r[0], r[1], r[2], r[3]
                    refs.append(DataSourceRef("faq", "faq", faq_id, q, {"answer": a[:200], "category": cat}))
                    summary_parts.append(f"【{cat}】{q}: {a[:80]}")
                    await cur.execute("UPDATE kefu_faq SET hit_count=hit_count+1 WHERE faq_id=%s", (faq_id,))
                return DataSourceResult(refs, "\n".join(summary_parts))
        finally:
            conn.close()
