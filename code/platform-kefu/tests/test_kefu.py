"""Basic tests for platform-kefu

2026-09-30 修正: 这两个用例自 P0 鉴权加固后就一直是红的, 但**它们从
2026-09 起一次都没真正测过业务逻辑** —— 不带任何凭证调用需要身份的接口,
拿到 401 后 assert 200, 于是每次都是"断言 401 失败"。

鉴权加固把默认 jwt_secret 硬编码值也删了 (config.py 里已作废, 不复述字面量),
这里统一用 settings.jwt_secret 签发与生产同构的 HS384 token, claims 结构
对齐 Java 侧 JwtUtil.generate(sub, username, tenantId, userType, permissions)。
"""
import os
import time

# app.config.Settings 有三个必填项 (deepseek_api_key / siliconflow_api_key /
# jwt_secret), 缺任一则 pydantic-settings 在 import 期就抛
# ValidationError, 整个模块连 collect 都进不去。2026-09 P0 加固把这三个从
# "有默认值的硬编码密钥"(已泄露于 git 历史) 改成必填, 但本地跑测试时没有
# .env, 于是这里给测试用的占位值。**不是生产密钥, 不做任何真实外部调用。**
os.environ.setdefault("JWT_SECRET", "test-only-jwt-secret-not-a-real-key-0000")
os.environ.setdefault("DEEPSEEK_API_KEY", "test-only-placeholder")
os.environ.setdefault("SILICONFLOW_API_KEY", "test-only-placeholder")

import jwt as pyjwt  # noqa: E402
import pytest  # noqa: E402
from fastapi.testclient import TestClient  # noqa: E402

from app.config import settings  # noqa: E402
from app.main import app  # noqa: E402

client = TestClient(app)

# 覆盖本文件用到的全部 kefu 权限点
KEFU_PERMS = [
    "kefu:chat",
    "kefu:knowledge",
    "kefu:settings",
    "kefu:faqs",
    "kefu:sessions",
]


def _auth(perms=None, user_type: int = 1, username: str = "tester") -> dict:
    now = int(time.time())
    token = pyjwt.encode(
        {
            "sub": "1999999999999999999",
            "username": username,
            "tenantId": 1,
            "userType": user_type,
            "permissions": KEFU_PERMS if perms is None else perms,
            "iat": now,
            "exp": now + 3600,
        },
        settings.jwt_secret,
        algorithm="HS384",
    )
    return {"Authorization": "Bearer " + token}


def test_health():
    # /api/kefu/health 在 EXEMPT_PATHS 里, 不需要凭证
    resp = client.get("/api/kefu/health")
    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] == "UP"
    assert data["service"] == "platform-kefu"


def test_endpoints_reject_missing_credentials():
    """无凭证一律 401 —— 这才是 P0 加固后真正的契约。

    这一组不需要 MySQL: 中间件在业务代码之前就短路返回 401。
    """
    assert client.post("/api/kefu/sessions", json={}).status_code == 401
    assert client.get("/api/kefu/data_sources").status_code == 401
    assert client.get("/api/kefu/docs").status_code == 401


# 下面两个用例会真的连 MySQL (create_session INSERT / data_sources SELECT),
# 本地跑测试时没有 .env 也没有库, 因此按需 skip; CI 与 142 上有库时会真正执行。
_DB_ENV = ("MYSQL_HOST", "JWT_SECRET")


def _db_available() -> bool:
    try:
        import aiomysql  # noqa: F401
    except ImportError:
        return False
    # 只在显式给了 MYSQL_HOST 时才认为有库 (142/CI 会注入)
    return all(os.environ.get(k) for k in _DB_ENV)


_db = pytest.mark.skipif(
    not _db_available(), reason="需要 MySQL: 设置 MYSQL_HOST 等环境变量后执行"
)


@_db
def test_create_session():
    """登录用户创建会话时, customer 身份取自 token 而非请求体。

    2026-09-28 起 create_session 会优先用 request.state.user 的
    userId / username 写入 customer 身份 (前端无需传参)。所以断言要看
    token 里的 username='tester', 而不是请求体里的 customer_name ——
    旧断言期望 'Test Customer' 是在断言一个早已不再生效的契约。
    """
    resp = client.post(
        "/api/kefu/sessions",
        json={"customer_name": "Test Customer"},
        headers=_auth(),
    )
    assert resp.status_code == 200
    data = resp.json()
    assert data["id"]
    assert data["customer_name"] == "tester"  # 来自 token, 非请求体
    assert data["status"] == "AI"


@_db
def test_list_data_sources():
    resp = client.get("/api/kefu/data_sources", headers=_auth())
    assert resp.status_code == 200
    data = resp.json()
    assert "total" in data
    assert isinstance(data["items"], list)
    # at least the seeded sources
    assert data["total"] >= 3
    ids = [ds["id"] for ds in data["items"]]
    assert "faq" in ids
    assert "knowledge" in ids
    assert "park-enterprise" in ids
