"""
P0 regression: 外部数据源在平台管理员视角下必须能返回数据。

Context (RED, 2026-09-30, host 142)
-----------------------------------
现象: 「数据源里配了企业档案, 但对话里用不了对应的数据信息」。

根因是三层叠加, 缺一层都不会立刻暴露:

1. ``DataSourceRegistry.route_query`` 在 ``tenant_id is None`` 时传的是
   ``params=None``。适配器里 ``params.get("tenant_id") if params else None``
   于是拿到 ``None``, 而各 http_api 适配器把 ``None`` 当成"拒绝服务"直接返回空。
   平台管理员 (userType==2) 恰恰由 ``_extract_tenant()`` 归一化成 ``None``
   —— 等于对管理员永久失效, 且只影响 http_api 类数据源, 看起来像"配了没生效"。

2. ``ParkEnterpriseAdapter`` 调的是 ``POST /api/enterprise/query``, 而真实接口是
   ``GET /enterprise/page``, 响应结构是 ``data.records`` 而非 ``data.items``。

3. ``_mock_query`` 用 ``e.get("tenant_id", 0) == tenant_id`` 过滤, 但 mock 数据
   没有 ``tenant_id`` 字段 → 全部默认 0 → 只有 tenant 0 可见。

这些断言不需要 MySQL 也不需要 park-enterprise: 全部用 fake。
Use:  python -m unittest tests.test_datasource_admin_p0
      from ``code\\platform-kefu``.
"""
from __future__ import annotations

import asyncio
import sys
import types
import unittest
from pathlib import Path

# httpx 是 adapter 的硬依赖；未安装时桩掉，只用到 AsyncClient 的存在性。
if "httpx" not in sys.modules:
    try:  # pragma: no cover
        import httpx  # noqa: F401
    except ModuleNotFoundError:
        sys.modules["httpx"] = types.ModuleType("httpx")

from app.services.datasource.base import DataSourceAdapter, DataSourceRef, DataSourceResult
from app.services.datasource.park_enterprise_adapter import ParkEnterpriseAdapter

_APP_DIR = Path(__file__).resolve().parent.parent / "app"


# ---------------------------------------------------------------------------
# Fakes
# ---------------------------------------------------------------------------
class _Resp:
    def __init__(self, payload, status=200):
        self._payload = payload
        self.status_code = status
        self.text = str(payload)

    def raise_for_status(self):
        if self.status_code >= 400:
            raise RuntimeError(f"HTTP {self.status_code}")

    def json(self):
        return self._payload


class _FakeAsyncClient:
    """记录请求并回放预置响应。"""

    last_call: dict = {}
    response: _Resp | None = None
    raise_exc: Exception | None = None

    def __init__(self, *a, **kw):
        pass

    async def __aenter__(self):
        return self

    async def __aexit__(self, *exc):
        return False

    async def get(self, url, params=None, headers=None):
        type(self).last_call = {"url": url, "params": params, "headers": headers}
        if type(self).raise_exc:
            raise type(self).raise_exc
        return type(self).response


class _StubAdapter(DataSourceAdapter):
    """模拟一个外部数据源适配器, 复现原实现对 params=None 的处理。"""

    def __init__(self, fail_on_none=True):
        super().__init__(id="stub", name="Stub", type="http_api",
                         intent_keywords=["企业"])
        self.fail_on_none = fail_on_none
        self.seen = []

    async def query(self, question, params=None):
        tenant_id = params.get("tenant_id") if params else None
        self.seen.append(params)
        if self.fail_on_none and tenant_id is None:
            return DataSourceResult([], "")
        return DataSourceResult(
            [DataSourceRef("stub", "enterprise", "1", "某企业", {})], "某企业"
        )


# 真实 park-enterprise 返回体 (字段名已按 142 实测对齐)
_PE_PAYLOAD = {
    "code": 200,
    "message": "操作成功",
    "data": {
        "total": "5",
        "records": [
            {
                "id": "1",
                "tenantId": "1",
                "name": "华为技术有限公司",
                "alias": "华为",
                "engName": "Huawei Technologies Co., Ltd.",
                "industry": "电子信息",
                "legalPersonName": "任正非",
                "regStatus": "存续",
                "regCapital": "0",
                "regLocation": "广东省深圳市",
                "businessScope": "计算机、通信设备",
                "phoneNumber": "0755-2878-0000",
                "websiteList": [],
                "tags": [],
            }
        ],
        "current": "1",
        "size": "3",
        "pages": "2",
    },
}


class TestRouteQueryReachesAdaptersForPlatformAdmin(unittest.TestCase):
    """问题 1: route_query 不能再把 params 整体丢成 None。"""

    def test_registry_passes_a_params_dict_even_when_tenant_is_none(self):
        from app.services.datasource.base import DataSourceRegistry

        adapter = _StubAdapter(fail_on_none=False)
        reg = DataSourceRegistry()
        reg.register(adapter)

        asyncio.run(reg.route_query("有哪些企业", tenant_id=None))

        self.assertEqual(len(adapter.seen), 1)
        self.assertIsNotNone(
            adapter.seen[0],
            "route_query 必须传 dict; 传 None 会让适配器无法区分"
            "「平台管理员跨租户」与「无参数」",
        )
        self.assertIn("tenant_id", adapter.seen[0])

    def test_adapter_that_rejects_none_yields_nothing_under_old_calling_convention(self):
        """锁定旧行为: 传 params=None 时 fail-on-None 的适配器返回空 —— 这就是 bug。"""
        adapter = _StubAdapter(fail_on_none=True)
        result = asyncio.run(adapter.query("有哪些企业", None))
        self.assertEqual(len(result.refs), 0)


class TestParkEnterpriseTenantVisibility(unittest.TestCase):
    """问题 2: 平台管理员 / 隔离桶 / 普通租户三种视角都要有数据。"""

    def _adapter(self, base_url=""):
        a = ParkEnterpriseAdapter()
        a.base_url = base_url
        return a

    def test_mock_data_carries_tenant_id(self):
        a = self._adapter()
        self.assertTrue(
            all("tenant_id" in e for e in a.mock_data),
            "mock 数据缺 tenant_id 会被 .get(...,0) 全部落到 0, 普通租户永远查不到",
        )

    def test_platform_admin_isolation_bucket_sees_mock_data(self):
        """session_service.chat() 会把管理员的 None 先归一成 0 再传给数据源。"""
        a = self._adapter()
        for tenant_id in (0, None):
            with self.subTest(tenant_id=tenant_id):
                r = asyncio.run(a.query("有哪些企业", {"tenant_id": tenant_id}))
                self.assertTrue(r.refs, f"tenant_id={tenant_id} 应能看到 mock 数据")
                self.assertTrue(r.summary)

    def test_tenant_1_sees_mock_data(self):
        a = self._adapter()
        r = asyncio.run(a.query("有哪些企业", {"tenant_id": 1}))
        self.assertTrue(r.refs)

    def test_unrelated_tenant_sees_nothing(self):
        a = self._adapter()
        r = asyncio.run(a.query("有哪些企业", {"tenant_id": 999}))
        self.assertEqual(r.refs, [])

    def test_module_ref_points_at_the_real_service(self):
        a = self._adapter()
        self.assertEqual(a.module_ref, "park-enterprise:8094")
        self.assertNotIn("platform-enterprise", a.module_ref)


class TestParkEnterpriseRealEndpoint(unittest.TestCase):
    """问题 3: 真实接口路径 / 方法 / 响应结构。"""

    def setUp(self):
        _FakeAsyncClient.last_call = {}
        _FakeAsyncClient.calls = []
        _FakeAsyncClient.response = _Resp(_PE_PAYLOAD)
        _FakeAsyncClient.raise_exc = None
        self._orig = sys.modules["httpx"].AsyncClient
        sys.modules["httpx"].AsyncClient = _FakeAsyncClient

    def tearDown(self):
        sys.modules["httpx"].AsyncClient = self._orig

    def test_calls_get_enterprise_page_and_parses_records(self):
        a = ParkEnterpriseAdapter()
        a.base_url = "http://park-enterprise:8094"

        r = asyncio.run(a.query("华为技术有限公司", {"tenant_id": 0}))

        call = _FakeAsyncClient.last_call
        self.assertTrue(call["url"].endswith("/enterprise/page"),
                        f"真实路径是 /enterprise/page, 实际打了 {call.get('url')}")
        self.assertNotIn("/api/", call["url"], "park-enterprise 没有 /api 前缀")
        self.assertEqual(call["params"]["pageNum"], 1)
        self.assertTrue(r.refs, "data.records 必须被解析出来")
        self.assertEqual(r.refs[0].label, "华为技术有限公司")
        self.assertIn("任正非", r.summary, "法人字段应进入摘要, 否则问答用不上")

    def test_falls_back_to_unfiltered_page_when_keyword_misses(self):
        """keyword 是按企业名称模糊匹配, 整句问必然落空 -> 必须再拉一次无 keyword 的列表。

        142 实测: keyword="有哪些企业入驻了" total=0, 不带 keyword total=5。
        """
        _FakeAsyncClient.response = _Resp(_PE_PAYLOAD)
        calls = []

        class _TwoStage(_FakeAsyncClient):
            async def get(self, url, params=None, headers=None):
                calls.append(dict(params or {}))
                if "keyword" in (params or {}):
                    return _Resp({"code": 200, "message": "ok",
                                  "data": {"total": "0", "records": []}})
                return _Resp(_PE_PAYLOAD)

        sys.modules["httpx"].AsyncClient = _TwoStage
        a = ParkEnterpriseAdapter()
        a.base_url = "http://park-enterprise:8094"

        r = asyncio.run(a.query("有哪些企业入驻了", {"tenant_id": 0}))

        self.assertEqual(len(calls), 2, "应先带 keyword 试一次, 落空后再不带 keyword")
        self.assertIn("keyword", calls[0])
        self.assertNotIn("keyword", calls[1])
        self.assertTrue(r.refs, "兜底拉列表后必须有数据, 否则问答拿不到真实档案")
        self.assertEqual(r.refs[0].label, "华为技术有限公司")

    def test_module_ref_in_sync_with_the_url_actually_called(self):
        a = ParkEnterpriseAdapter()
        a.base_url = "http://park-enterprise:8094"
        asyncio.run(a.query("企业", {"tenant_id": 0}))
        host_port = _FakeAsyncClient.last_call["url"].split("/enterprise")[0]
        self.assertIn(a.module_ref, host_port)

    def test_falls_back_to_mock_when_service_is_down(self):
        _FakeAsyncClient.raise_exc = RuntimeError("ConnectError")
        a = ParkEnterpriseAdapter()
        a.base_url = "http://park-enterprise:8094"
        r = asyncio.run(a.query("有哪些企业", {"tenant_id": 0}))
        self.assertTrue(r.refs, "HTTP 挂了应降级到 mock, 而不是返回空")
        self.assertIn("云枢科技", r.summary)

    def test_business_error_code_is_surfaced_not_swallowed(self):
        _FakeAsyncClient.response = _Resp(
            {"code": 500, "message": "boom", "data": None}, status=200
        )
        a = ParkEnterpriseAdapter()
        a.base_url = "http://park-enterprise:8094"
        r = asyncio.run(a.query("有哪些企业", {"tenant_id": 0}))
        # code=500 必须走降级 (result 里是 mock 数据), 而不是把 records 解析成空
        self.assertTrue(r.refs)


class TestNoStalePlatformServiceNames(unittest.TestCase):
    """静态守卫: 别再把 platform-<x>:8080 写回来。

    已核实的 3 个 (142 上容器存在且 /actuator/health 200):
        park-space 8091 / park-contract 8093 / park-enterprise 8094
    必须写 park-<x>:<真实端口>。

    park-business 是**明确的未决项**: compose 里根本没有这个服务, 无从核实指向,
    因此 module_ref 保持原值不编造; 它必须保持 enabled=0, 以免被误当成可用配置。
    """

    _VERIFIED = ("park-space", "park-contract", "park-enterprise")

    def _seed(self):
        db_src = (_APP_DIR / "models" / "database.py").read_text(encoding="utf-8")
        start = db_src.index("SEED_DATA_SOURCES_SQL")
        end = db_src.index("_DATA_SOURCE_REF_FIXES")
        return db_src[start:end]

    def test_verified_sources_use_park_prefix_and_real_ports(self):
        seed = self._seed()
        for source_id in self._VERIFIED:
            with self.subTest(source=source_id):
                row = next(
                    line for line in seed.splitlines()
                    if line.strip().startswith(f"('{source_id}'")
                )
                self.assertIn(
                    f"'{source_id}:", row,
                    f"{source_id} 的 module_ref 应写成 {source_id}:<真实端口>",
                )
                self.assertNotIn("platform-", row)

    def test_park_business_stays_disabled_pending_a_decision(self):
        """指向不存在的服务, 所以必须禁用 —— 别把它当成能用的配置。"""
        seed = self._seed()
        row = next(
            line for line in seed.splitlines()
            if line.strip().startswith("('park-business'")
        )
        self.assertIn(
            "'park-business', '招商管理', 'http_api', 'platform-business:8080', 0",
            row,
            "park-business 的 module_ref 未经核实, 只能保持原值; enabled 必须仍为 0",
        )


if __name__ == "__main__":
    unittest.main(verbosity=2)
