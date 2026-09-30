"""企业档案数据源适配器 - HTTP 调用 park-enterprise 服务

2026-09-30 修正 (原实现四层都对不上, 导致数据源"配了但用不了"):

| 层           | 原实现                        | 实际 (142 实测)                        |
|--------------|-------------------------------|----------------------------------------|
| 服务名       | platform-enterprise           | park-enterprise (前者 DNS 直接失败)     |
| 端口         | 8080                         | 8094                                   |
| 方法+路径    | POST /api/enterprise/query   | GET /enterprise/page                   |
| 响应结构     | data.items                   | data.records                           |

外加两个使能条件:
  - PLATFORM_ENTERPRISE_BASE_URL 之前没在 compose 里设置, base_url 恒为空,
    于是永远走 mock, 真实接口一次都没被调用过。
  - query() 开头对 tenant_id is None 直接返回空, 而平台管理员 (userType=2)
    恰恰由 _extract_tenant() 归一化成 None —— 等于对管理员永久失效。
    语义对齐 access.py / sessions.py: None = 平台管理员, 跨租户可见。

真实接口 (park-enterprise/src/main/java/.../EnterpriseController.java):
    GET /enterprise/page?keyword=&status=&pageNum=&pageSize=
    -> {code, message, data:{total, records, current, size, pages}}
无 /api 前缀 (无 server.servlet.context-path)。
"""
import logging
import os
from typing import Any, Dict, List

import httpx

from app.services.datasource.base import DataSourceAdapter, DataSourceRef, DataSourceResult

logger = logging.getLogger(__name__)

# 平台管理员 (userType==2) 走 _extract_tenant() 会得到 tenant_id=None,
# 语义是"跨租户可见", 不能当成"无数据"。
CROSS_TENANT = None


class ParkEnterpriseAdapter(DataSourceAdapter):
    """跨服务调用 park-enterprise 查询企业档案。

    两种模式:
      1. 真实 HTTP (PLATFORM_ENTERPRISE_BASE_URL 已设置时) —— 生产默认
      2. Mock (未设置时) —— 本地开发/演示兜底
    """

    def __init__(self):
        super().__init__(
            id="park-enterprise", name="企业档案", type="http_api",
            module_ref="park-enterprise:8094",
            intent_keywords=["企业", "公司", "入驻", "客户", "联系人", "法人", "注册", "档案"],
        )
        self.base_url = os.getenv("PLATFORM_ENTERPRISE_BASE_URL", "").rstrip("/")
        # mock 数据的 tenant_id 与 park-enterprise 现网数据一致 (全部 tenant 1),
        # 否则非平台管理员视角下永远过滤成空 (原实现没写这个字段, 全部默认 0)。
        self.mock_data: List[Dict[str, Any]] = [
            {"id": 1, "name": "云枢科技有限公司", "contact": "张经理", "phone": "138-0000-0001",
             "status": "已入驻", "industry": "人工智能", "area": "深圳湾科技园", "tenant_id": 1},
            {"id": 2, "name": "蓝海智能装备有限公司", "contact": "李总", "phone": "138-0000-0002",
             "status": "已入驻", "industry": "智能装备", "area": "南山智园", "tenant_id": 1},
            {"id": 3, "name": "绿能新材料股份有限公司", "contact": "王主任", "phone": "138-0000-0003",
             "status": "审核中", "industry": "新材料", "area": "宝安中心区", "tenant_id": 1},
            {"id": 4, "name": "红芯微电子有限公司", "contact": "陈工", "phone": "138-0000-0004",
             "status": "已入驻", "industry": "集成电路", "area": "福田CBD", "tenant_id": 1},
        ]

    async def query(self, question: str, params: Dict[str, Any] = None) -> DataSourceResult:
        # tenant_id 允许为 None —— 那是平台管理员的合法语义, 不是"拒绝服务"。
        tenant_id = params.get("tenant_id") if params else None
        if self.base_url:
            return await self._http_query(question, tenant_id)
        return self._mock_query(question, tenant_id)

    @staticmethod
    def _pick(record: Dict[str, Any]) -> Dict[str, Any]:
        """把 park-enterprise 的实体压成问答里用得上的几个字段。"""
        return {
            "name": record.get("name") or "",
            "alias": record.get("alias") or "",
            "industry": record.get("industry") or "",
            "legalPersonName": record.get("legalPersonName") or "",
            "regStatus": record.get("regStatus") or "",
            "phoneNumber": record.get("phoneNumber") or "",
            "websiteList": record.get("websiteList") or [],
            "tags": record.get("tags") or [],
        }

    @staticmethod
    def _describe(record: Dict[str, Any]) -> str:
        name = record.get("name") or "(未命名)"
        bits = [f"企业：{name}"]
        for key, label in (
            ("alias", "简称"),
            ("industry", "行业"),
            ("legalPersonName", "法人"),
            ("regStatus", "注册状态"),
            ("regCapital", "注册资本"),
            ("regLocation", "注册地"),
            ("businessScope", "经营范围"),
            ("phoneNumber", "电话"),
        ):
            val = record.get(key)
            if val not in (None, "", []):
                bits.append(f"{label}：{val}")
        return "（".join(bits) + "）" if len(bits) > 1 else bits[0]

    async def _fetch_page(
        self, client, tenant_id: int | None, keyword: str | None
    ) -> List[Dict[str, Any]]:
        params = {"pageNum": 1, "pageSize": 3}
        if keyword:
            params["keyword"] = keyword
        headers = {"Accept": "application/json"}
        if tenant_id is not None:
            # 服务端 controller 没有 tenantId 入参, 租户隔离靠 MyBatis 拦截器 +
            # LoginContextHolder; 这个头是尽力而为, 实测缺失也能正常返回。
            headers["X-Tenant-Id"] = str(tenant_id)
        resp = await client.get(
            f"{self.base_url}/enterprise/page", params=params, headers=headers
        )
        resp.raise_for_status()
        payload = resp.json()
        if payload.get("code") != 200:
            raise RuntimeError(
                f"park-enterprise 返回 code={payload.get('code')}: "
                f"{payload.get('message')}"
            )
        return (payload.get("data") or {}).get("records") or []

    async def _http_query(self, question: str, tenant_id: int | None) -> DataSourceResult:
        try:
            async with httpx.AsyncClient(timeout=8.0) as client:
                # 两步取数 (142 实测决定):
                #   /enterprise/page 的 keyword 是"按企业名称模糊匹配", 拿整句问去
                #   匹配必然落空 —— "有哪些企业入驻了" / "查一下企业档案" 里不含任何
                #   企业名, 实测全部 total=0。所以:
                #     1) 先带整句当 keyword, 命中说明用户确实在问某个具名企业
                #        ("华为技术有限公司" -> 1 条, 带法人/注册资本/注册地)
                #     2) 落空则不带 keyword 直接拉列表, 让上层 LLM 拿真实数据去答
                records = await self._fetch_page(client, tenant_id, keyword=question)
                if not records:
                    records = await self._fetch_page(client, tenant_id, keyword=None)
                refs = [
                    DataSourceRef(
                        "park-enterprise", "enterprise", str(rec.get("id")),
                        rec.get("name") or f"企业#{rec.get('id')}",
                        self._pick(rec),
                    )
                    for rec in records
                ]
                summary = "\n".join(self._describe(rec) for rec in records)
                logger.info(
                    "[ParkEnterprise] 命中 %d 条 (tenant_id=%s, keyword=%s)",
                    len(refs), tenant_id, "有" if records else "无",
                )
                return DataSourceResult(refs, summary)
        except Exception as e:
            logger.warning(
                "[ParkEnterprise] HTTP 调用失败 (%s)，降级到 mock: %s", self.base_url, e
            )
            return self._mock_query(question, tenant_id)

    def _mock_query(self, question: str, tenant_id: int | None) -> DataSourceResult:
        """关键词匹配 mock 数据。

        可见性按本仓库既有约定 (见 access.py / session_service.create_session):
          - None            = 平台管理员跨租户语义 (调用方未做隔离时)
          - 0               = 平台隔离桶; session_service.chat() 会把管理员的
                               None 先归一成 0 再传给数据源, 所以管理员实际走这里
          - 其他正整数      = 具体租户, 只看自己的数据
        """
        if tenant_id is CROSS_TENANT or tenant_id == 0:
            visible_data = list(self.mock_data)
        else:
            visible_data = [
                e for e in self.mock_data if e.get("tenant_id") == tenant_id
            ]
        matched = []
        for ent in visible_data:
            if any(kw in question for kw in [ent["name"][:4], ent["contact"], ent["industry"]]):
                matched.append(ent)
        if not matched and any(kw in question for kw in ["企业", "公司", "入驻", "客户", "档案"]):
            matched = visible_data[:3]
        refs = [
            DataSourceRef("park-enterprise", "enterprise", str(e["id"]), e["name"], e)
            for e in matched
        ]
        summary = "\n".join(
            f"企业：{e['name']}（{e['status']}，{e['industry']}，联系人：{e['contact']} {e['phone']}）"
            for e in matched
        )
        return DataSourceResult(refs, summary)
