"""Phase-1 混合检索测试 (纯逻辑, 不依赖数据库/向量服务).

session_service.py 的模块导入链会构造 faiss 索引 (get_store 在 import 时跑),
而 app.config.Settings 有三个必填密钥 —— 与 test_kefu.py 一致, 这里在 import 前
注入占位环境变量, 不做任何真实外部调用。

只测纯函数 / 无 DB 依赖的行为:
  - _extract_code_candidates: 抽取 18 位信用代码 / 手机号 / 邮箱; 普通问题返回 []
  - _normalize_exact_value: 归一化 (去空格/连字符, 小写)
  - _tenant_filter: None / 0 / 其他 的 SQL 片段语义
  - _structured_lookup: DB 不可用时返回 "" 且不抛异常
"""
import asyncio
import os

# ---- import 前注入占位密钥 (非生产密钥, 不触发真实外部调用) ----
os.environ.setdefault("JWT_SECRET", "test-only-jwt-secret-not-a-real-key-0000")
os.environ.setdefault("DEEPSEEK_API_KEY", "test-only-placeholder")
os.environ.setdefault("SILICONFLOW_API_KEY", "test-only-placeholder")

import pytest  # noqa: E402
from app.services import session_service  # noqa: E402
from app.services.session_service import (  # noqa: E402
    _extract_code_candidates,
    _normalize_exact_value,
    _tenant_filter,
    _structured_lookup,
)


# ---------- _extract_code_candidates ----------

def test_extract_credit_code():
    # 18 位统一社会信用代码 (字符集 [0-9A-HJ-NPQRTUWXY])
    q = "华为的统一社会信用代码是多少 91110000710931XXXX"
    cands = _extract_code_candidates(q)
    assert "91110000710931XXXX" in cands


def test_extract_phone():
    q = "联系 13812345678 咨询"
    assert "13812345678" in _extract_code_candidates(q)


def test_extract_email():
    q = "发到 hr@huawei.com 可以吗"
    assert "hr@huawei.com" in _extract_code_candidates(q)


def test_extract_mixed_and_dedup():
    q = "电话 13900001111 邮箱 a@b.cn 信用代码 91110000710931XXXX"
    cands = _extract_code_candidates(q)
    assert "13900001111" in cands
    assert "a@b.cn" in cands
    assert "91110000710931XXXX" in cands
    # 去重: 同一候选不应出现两次
    assert len(cands) == len(set(cands))


def test_extract_plain_question_returns_empty():
    q = "华为的统一社会信用代码是多少"  # 没有具体编码
    assert _extract_code_candidates(q) == []


def test_extract_empty_question():
    assert _extract_code_candidates("") == []


# ---------- _normalize_exact_value ----------

def test_normalize_lowercases_strips_spaces_and_hyphens():
    assert _normalize_exact_value("AB-12 cd 34") == "ab12cd34"
    assert _normalize_exact_value("") == ""
    assert _normalize_exact_value(None) == ""


# ---------- _tenant_filter ----------

def test_tenant_filter_none_means_no_filter():
    sql_frag, args = _tenant_filter(None)
    assert sql_frag == ""
    assert args == []


def test_tenant_filter_zero_filters_to_zero():
    sql_frag, args = _tenant_filter(0)
    assert sql_frag == " AND tenant_id=%s"
    assert args == [0]


def test_tenant_filter_other_filters_to_value():
    sql_frag, args = _tenant_filter(7)
    assert sql_frag == " AND tenant_id=%s"
    assert args == [7]


# ---------- _structured_lookup (DB 不可用 → 降级 "" 且不抛) ----------

def test_structured_lookup_db_unavailable_returns_empty_no_raise():
    """monkeypatch get_db_connection 使其 await 即抛异常 —— 验证降级返回 "" 而非崩溃。"""

    def _fake_get_db_connection(*_a, **_kw):
        raise RuntimeError("MySQL unavailable (test)")

    orig = session_service.get_db_connection
    session_service.get_db_connection = _fake_get_db_connection
    try:
        # (a) 精确路径: 问题里有信用代码 -> 走 field 表查询 -> 连接抛错 -> 降级
        out_exact = asyncio.run(_structured_lookup("信用代码 91110000710931XXXX 是多少", 1))
        assert out_exact == ""
        # (b) 名称路径: 无候选编码 -> 走 record 表 INSTR -> 连接抛错 -> 降级
        out_name = asyncio.run(_structured_lookup("华为的信用代码是多少", None))
        assert out_name == ""
    finally:
        # 还原真实引用, 避免污染其他用例
        from app.models import database as _db
        session_service.get_db_connection = _db.get_db_connection


def test_structured_lookup_missing_table_returns_empty_no_raise():
    """模拟连接正常但表不存在 (1146 类错误) —— 仍须降级 "" 不抛。"""

    class _FakeCursor:
        def __init__(self, conn):
            self._conn = conn

        async def __aenter__(self):
            return self

        async def __aexit__(self, *exc):
            return False

        async def execute(self, sql, args=None):
            # 模拟 "table doesn't exist"
            raise RuntimeError(
                "(1146, \"Table 'kefu.kefu_datasource_record_field' doesn't exist\")"
            )

        async def fetchone(self):
            return None

        async def fetchall(self):
            return []

    class _FakeConn:
        def cursor(self):
            return _FakeCursor(self)

        def close(self):
            pass

    async def _fake_get_db_connection(*_a, **_kw):
        return _FakeConn()

    orig = session_service.get_db_connection
    session_service.get_db_connection = _fake_get_db_connection
    try:
        out = asyncio.run(_structured_lookup("华为技术有限公司 信用代码", 0))
        assert out == ""
    finally:
        from app.models import database as _db
        session_service.get_db_connection = _db.get_db_connection
