"""企业档案数据源适配器 - HTTP 调用 platform-enterprise 服务"""
import os
import httpx
import logging
from typing import Dict, Any
from app.services.datasource.base import DataSourceAdapter, DataSourceResult, DataSourceRef

logger = logging.getLogger(__name__)


class ParkEnterpriseAdapter(DataSourceAdapter):
    """跨服务调用 platform-enterprise 查询企业档案。

    支持两种模式：
    1. 真实 HTTP 调用（当 PLATFORM_ENTERPRISE_BASE_URL 环境变量设置时）
    2. Mock 模式（返回造数据，用于开发/演示）
    """

    def __init__(self):
        super().__init__(
            id="park-enterprise", name="企业档案", type="http_api",
            module_ref="platform-enterprise",
            intent_keywords=["企业", "公司", "入驻", "客户", "联系人", "法人", "注册", "档案"],
        )
        self.base_url = os.getenv("PLATFORM_ENTERPRISE_BASE_URL", "")
        self.mock_data = [
            {"id": 1, "name": "云枢科技有限公司", "contact": "张经理", "phone": "138-0000-0001", "status": "已入驻", "industry": "人工智能", "area": "深圳湾科技园"},
            {"id": 2, "name": "蓝海智能装备有限公司", "contact": "李总", "phone": "138-0000-0002", "status": "已入驻", "industry": "智能装备", "area": "南山智园"},
            {"id": 3, "name": "绿能新材料股份有限公司", "contact": "王主任", "phone": "138-0000-0003", "status": "审核中", "industry": "新材料", "area": "宝安中心区"},
            {"id": 4, "name": "红芯微电子有限公司", "contact": "陈工", "phone": "138-0000-0004", "status": "已入驻", "industry": "集成电路", "area": "福田CBD"},
        ]

    async def query(self, question: str, params: Dict[str, Any] = None) -> DataSourceResult:
        if self.base_url:
            return await self._http_query(question)
        return self._mock_query(question)

    async def _http_query(self, question: str) -> DataSourceResult:
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                resp = await client.post(
                    f"{self.base_url}/api/enterprise/query",
                    json={"keyword": question, "limit": 3},
                    headers={"Content-Type": "application/json"},
                )
                resp.raise_for_status()
                data = resp.json()
                items = data.get("items", [])
                refs = [
                    DataSourceRef(
                        "park-enterprise", "enterprise", str(it.get("id")),
                        it.get("name", ""),
                        {"contact": it.get("contact"), "status": it.get("status"), "industry": it.get("industry")},
                    )
                    for it in items
                ]
                summary = "\n".join(f"企业：{it.get('name')}（{it.get('status')}，联系人：{it.get('contact')}）" for it in items)
                return DataSourceResult(refs, summary)
        except Exception as e:
            logger.warning(f"[ParkEnterprise] HTTP 调用失败，降级到 mock: {e}")
            return self._mock_query(question)

    def _mock_query(self, question: str) -> DataSourceResult:
        """关键词匹配 mock 数据"""
        matched = []
        for ent in self.mock_data:
            if any(kw in question for kw in [ent["name"][:4], ent["contact"], ent["industry"]]):
                matched.append(ent)
        if not matched:
            # 如果问题包含"企业""公司"等泛词，返回全部
            if any(kw in question for kw in ["企业", "公司", "入驻", "客户"]):
                matched = self.mock_data[:3]
        refs = [
            DataSourceRef("park-enterprise", "enterprise", str(e["id"]), e["name"], e)
            for e in matched
        ]
        summary = "\n".join(f"企业：{e['name']}（{e['status']}，{e['industry']}，联系人：{e['contact']} {e['phone']}）" for e in matched)
        return DataSourceResult(refs, summary)
