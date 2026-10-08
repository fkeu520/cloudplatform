"""Phase-1 全量摄取 — 纯函数单元测试 (无 DB, 无网络).

覆盖:
- build_profile_text: 包含 creditCode 值 + 中文标签, name 在最前, 空字段省略
- profile_hash: 相同输入 → 相同 hash, 字段变更 → hash 变化
- explode_fields: creditCode / taxNumber / phoneNumber / email 标记为 exact_matchable
- _norm: 大小写 / 空格 / 连字符归一化
"""
from __future__ import annotations

import os
import sys

# ── env 占位 (与 test_kefu.py / test_processing_queue_p0.py 一致) ──────────────
os.environ.setdefault("JWT_SECRET", "test-only-jwt-secret-not-a-real-key-0000")
os.environ.setdefault("DEEPSEEK_API_KEY", "test-only-placeholder")
os.environ.setdefault("SILICONFLOW_API_KEY", "test-only-placeholder")

# ── aiomysql stub (避免 import 期连库) ────────────────────────────────────────
if "aiomysql" not in sys.modules:
    try:
        import aiomysql  # noqa: F401
    except ModuleNotFoundError:
        _stub = type(sys)("aiomysql")
        class _Pool:
            pass
        class _Conn:
            pass
        _stub.Pool = _Pool
        _stub.Connection = _Conn
        def _no(*a, **kw):
            raise RuntimeError("aiomysql stub — do not connect")
        _stub.create_pool = _no
        sys.modules["aiomysql"] = _stub

from app.services.datasource.sync_service import (
    build_profile_text,
    profile_hash,
    explode_fields,
    _norm,
)


# --------------------------------------------------------------------------- #
# Fixtures
# --------------------------------------------------------------------------- #
def _sample_record() -> dict:
    """A realistic park-enterprise record with all fields populated."""
    return {
        "id": "101",
        "tenantId": "2",
        "name": "华为技术有限公司",
        "alias": "华为",
        "creditCode": "91440300708476283K",
        "taxNumber": "91440300708476283K",
        "regNumber": "44030012345678",
        "orgNumber": "98765432",
        "legalPersonName": "任正非",
        "regStatus": "存续",
        "regCapital": "2,100,000 万元",
        "estiblishTime": "1987-09-15",
        "regLocation": "广东省深圳市",
        "industry": "电子信息",
        "businessScope": "计算机、通信设备、网络设备的研发、生产",
        "phoneNumber": "0755-2878-0000",
        "email": "info@huawei.com",
        "staffNumRange": "1000人以上",
        "socialStaffNum": "80000",
        "websiteList": ["https://www.huawei.com", "https://e.huawei.com"],
        "tags": ["上市公司", "500强"],
    }


def _sparse_record() -> dict:
    """A record with many empty/None fields — should be omitted from profile."""
    return {
        "id": "202",
        "tenantId": "1",
        "name": "小公司",
        "alias": "",
        "creditCode": "91110105MA001ABCDE",
        "taxNumber": None,
        "regNumber": None,
        "orgNumber": None,
        "legalPersonName": None,
        "regStatus": "注销",
        "regCapital": "",
        "estiblishTime": None,
        "regLocation": None,
        "industry": None,
        "businessScope": None,
        "phoneNumber": None,
        "email": "",
        "staffNumRange": None,
        "socialStaffNum": None,
        "websiteList": [],
        "tags": [],
    }


# --------------------------------------------------------------------------- #
# build_profile_text
# --------------------------------------------------------------------------- #
class TestBuildProfileText:
    def test_includes_credit_code_value(self):
        profile = build_profile_text(_sample_record())
        assert "91440300708476283K" in profile, "creditCode value must appear in profile"

    def test_includes_chinese_labels(self):
        profile = build_profile_text(_sample_record())
        assert "企业名称" in profile
        assert "统一社会信用代码" in profile
        assert "法定代表人" in profile
        assert "注册状态" in profile

    def test_name_appears_first(self):
        profile = build_profile_text(_sample_record())
        # The first field in _FIELD_MAP is (name, "企业名称"), so the profile
        # should begin with "企业名称：华为技术有限公司"
        assert profile.startswith("企业名称：华为技术有限公司"), (
            f"profile must start with the name field, got: {profile[:50]!r}"
        )

    def test_omits_empty_fields(self):
        profile = build_profile_text(_sparse_record())
        # alias is "" → should NOT appear
        assert "简称" not in profile, "empty alias label must be omitted"
        # email is "" → should NOT appear
        assert "邮箱" not in profile, "empty email label must be omitted"
        # websiteList is [] → should NOT appear
        assert "官网" not in profile, "empty websiteList label must be omitted"
        # tags is [] → should NOT appear
        assert "标签" not in profile, "empty tags label must be omitted"
        # creditCode is non-empty → must appear
        assert "91110105MA001ABCDE" in profile

    def test_handles_list_fields(self):
        profile = build_profile_text(_sample_record())
        assert "官网：https://www.huawei.com、https://e.huawei.com" in profile
        assert "标签：上市公司、500强" in profile

    def test_string_valued_list_fields(self):
        # websiteList / tags may arrive as a plain string, not a list
        rec = _sparse_record()
        rec["websiteList"] = "https://example.com"
        rec["tags"] = "上市"
        profile = build_profile_text(rec)
        assert "官网：https://example.com" in profile
        assert "标签：上市" in profile


# --------------------------------------------------------------------------- #
# profile_hash
# --------------------------------------------------------------------------- #
class TestProfileHash:
    def test_stable_for_identical_input(self):
        h1 = profile_hash("企业名称：华为；统一社会信用代码：91440300708476283K")
        h2 = profile_hash("企业名称：华为；统一社会信用代码：91440300708476283K")
        assert h1 == h2, "identical input must produce identical hash"
        assert len(h1) == 64, "SHA-256 hexdigest must be 64 chars"

    def test_changes_when_a_field_changes(self):
        h_before = profile_hash("企业名称：华为；统一社会信用代码：91440300708476283K")
        h_after = profile_hash("企业名称：华为；统一社会信用代码：914403000000000000X")
        assert h_before != h_after, "hash must change when the text changes"


# --------------------------------------------------------------------------- #
# explode_fields
# --------------------------------------------------------------------------- #
class TestExplodeFields:
    def test_marks_identifiers_as_exact(self):
        fields = explode_fields(_sample_record())
        by_key = {f[0]: f for f in fields}
        for key in ("creditCode", "taxNumber", "phoneNumber", "email"):
            assert key in by_key, f"{key} must be in exploded fields"
            assert by_key[key][3] is True, f"{key} must be marked exact_matchable"

    def test_non_identifiers_are_not_exact(self):
        fields = explode_fields(_sample_record())
        by_key = {f[0]: f for f in fields}
        for key in ("name", "alias", "legalPersonName", "regStatus"):
            assert key in by_key
            assert by_key[key][3] is False, f"{key} must NOT be exact_matchable"

    def test_empty_values_are_omitted(self):
        fields = explode_fields(_sparse_record())
        keys = [f[0] for f in fields]
        assert "alias" not in keys
        assert "taxNumber" not in keys
        assert "phoneNumber" not in keys
        assert "email" not in keys
        assert "websiteList" not in keys
        assert "tags" not in keys
        # creditCode is present (non-empty)
        assert "creditCode" in keys

    def test_list_fields_are_joined(self):
        fields = explode_fields(_sample_record())
        by_key = {f[0]: f for f in fields}
        assert by_key["websiteList"][2] == "https://www.huawei.com、https://e.huawei.com"
        assert by_key["tags"][2] == "上市公司、500强"


# --------------------------------------------------------------------------- #
# _norm
# --------------------------------------------------------------------------- #
class TestNorm:
    def test_strips_and_collapses_case(self):
        assert _norm("  AbC  ") == "ABC"
        assert _norm("abc") == "ABC"

    def test_removes_hyphens(self):
        assert _norm("0755-2878-0000") == "075528780000"

    def test_removes_spaces(self):
        assert _norm("91 4403 00 7084 7628 3K") == "91440300708476283K"

    def test_uppercases_alnum_codes(self):
        assert _norm("91440300708476283k") == "91440300708476283K"

    def test_none_returns_empty(self):
        assert _norm(None) == ""

    def test_phone_number_round_trip(self):
        # Two formatted phone numbers must normalize to the same value
        assert _norm("0755-2878-0000") == _norm("0755 2878 0000") == _norm("075528780000")


if __name__ == "__main__":
    import unittest
    unittest.main(verbosity=2)