"""
Trust-boundary regression tests for Kefu authentication (P0).

These tests verify the X-Kefu-Internal-Token contract introduced 2026-09-24.
Run with: python -m unittest tests/test_auth_boundary_p0.py -v

Design contract:
  1. A request carrying a valid X-Kefu-Internal-Token (HMAC-SHA256) is allowed
     to carry X-User-* headers — this proves gateway-originated identity.
  2. A request carrying X-User-* WITHOUT a valid X-Kefu-Internal-Token is
     rejected (401) — prevents client-side header spoofing.
  3. Requests without X-User-* fall back to Bearer JWT verification (unchanged).
  4. Health endpoint remains exempt from all trust checks.
  5. The middleware MUST use hmac.compare_digest (constant-time comparison).
"""

import asyncio
import hmac
import hashlib
import json
import jwt as pyjwt
import os
import unittest
from starlette.requests import Request
from starlette.datastructures import Headers


# Seed env vars BEFORE importing app.config (which instantiates Settings at module level)
os.environ.setdefault("DEEPSEEK_API_KEY", "test-key")
os.environ.setdefault("SILICONFLOW_API_KEY", "test-key")
# 2026-09-28: config.Settings.jwt_secret 已改为必填 (无默认值, fail-closed).
# 测试必须自行 seed, 否则 `from app.core.auth_middleware import ...` 触发
# Settings() 时抛 ValidationError. 值与下方 JWT_SECRET 常量保持一致.
os.environ.setdefault("JWT_SECRET", "test-only-jwt-secret-not-a-real-key-000000")

# 2026-09-28 安全加固: 原先复用了真实生产密钥字面量. 改为明确的测试专用假值,
# 与下方 KEFU_SECRET 惯例一致 — 测试绝不能携带任何可用密钥, 即使泄露也无可利用价值.
JWT_SECRET = "test-only-jwt-secret-not-a-real-key-000000"
KEFU_SECRET = "kefu-internal-secret-32-chars-min-for-hmac-sha256!"


def _compute_kefu_token(secret: str) -> str:
    """Reproduce the gateway's HMAC-SHA256 computation."""
    mac = hmac.new(
        secret.encode("utf-8"),
        b"kefu-internal",
        hashlib.sha256,
    )
    return mac.hexdigest()


# ---------------------------------------------------------------------------
# Helpers to build a minimal Starlette Request without a running server
# ---------------------------------------------------------------------------

def _build_request(
    path: str = "/api/kefu/ask",
    method: str = "POST",
    headers: dict | None = None,
) -> Request:
    """Construct a Request bound to an ASGI pipeline without a server."""
    headers_obj = Headers(raw=[
        (k.lower().encode(), v.encode()) for k, v in (headers or {}).items()
    ])
    scope = {
        "type": "http",
        "method": method,
        "path": path,
        "headers": list(headers_obj.raw),
        # 纯 ASGI 中间件写入 scope["state"]["user"]; Starlette 的 Request.state
        # 惰性创建的就是一个普通 dict, 所以这里必须同样是 dict 而不是 State。
        "state": {},
    }
    return Request(scope)


class _CapturedResponse:
    """从 ASGI send() 消息里拼出的最小 Response 替身。

    保留 ``.status_code`` / ``.body`` / ``.json()``, 让 10 个用例的断言体
    在 BaseHTTPMiddleware → 纯 ASGI 迁移后无需改动。
    """

    def __init__(self) -> None:
        self.status_code: int | None = None
        self.body = b""

    def json(self):
        return json.loads(self.body or b"{}")


# ---------------------------------------------------------------------------
# Settings stub (mirrors app.config.Settings)
# ---------------------------------------------------------------------------

class _SettingsStub:
    """Mirrors the Settings object the middleware reads."""
    def __init__(self, kefu_internal_token: str = "", jwt_secret: str = JWT_SECRET):
        self.kefu_internal_token = kefu_internal_token
        self.jwt_secret = jwt_secret


# ---------------------------------------------------------------------------
# Test cases
# ---------------------------------------------------------------------------

class TestAuthBoundaryP0(unittest.TestCase):
    """Trust-boundary P0: X-User-* accepted ONLY with valid X-Kefu-Internal-Token."""

    def _dispatch(self, req, settings):
        """Drive the middleware through its pure-ASGI interface.

        2026-09-29 (c3399065 / 24cbeb20) 把 JwtAuthMiddleware 从 BaseHTTPMiddleware
        改成了纯 ASGI (`__call__(scope, receive, send)`), 并改为读模块级
        `settings` 单例而非 `request.app.state.settings`。本 helper 同步跟上,
        否则整个信任边界测试套件会以 "no attribute 'dispatch'" 全部 error ——
        也就是说 2026-09-29 起这些 P0 断言实际上一次都没跑过。

        返回 (result, called): result 是拼装出来的响应, called 是下游 app 收到的
        Request 列表 (断言 request.state.user 用)。
        """
        from app.core import auth_middleware
        from app.core.auth_middleware import JwtAuthMiddleware

        called: list[Request] = []
        captured = _CapturedResponse()

        async def dummy_app(scope, receive, send):
            called.append(Request(scope))
            await send({"type": "http.response.start", "status": 200, "headers": []})
            await send({"type": "http.response.body", "body": b""})

        async def receive():
            return {"type": "http.request", "body": b"", "more_body": False}

        async def send(message):
            if message["type"] == "http.response.start":
                captured.status_code = message["status"]
            elif message["type"] == "http.response.body":
                captured.body += message.get("body", b"")

        middleware = JwtAuthMiddleware(dummy_app)
        original_settings = auth_middleware.settings
        auth_middleware.settings = settings
        try:
            asyncio.run(middleware(req.scope, receive, send))
        finally:
            auth_middleware.settings = original_settings
        return captured, called

    # ---- Happy path: valid internal token + X-User-* ----

    def test_valid_kefu_token_allows_x_user(self):
        """Request with valid X-Kefu-Internal-Token + X-User-Id must be accepted."""
        token = _compute_kefu_token(KEFU_SECRET)
        req = _build_request(
            headers={
                "X-User-Id": "user-42",
                "X-User-Name": "alice",
                "X-Kefu-Internal-Token": token,
            },
        )
        settings = _SettingsStub(kefu_internal_token=KEFU_SECRET)
        result, called = self._dispatch(req, settings)
        self.assertEqual(result.status_code, 200,
                         "Valid internal token + X-User-Id must be accepted")
        self.assertTrue(len(called) > 0, "call_next must be invoked")
        self.assertEqual(called[0].state.user["userId"], "user-42")
        self.assertEqual(called[0].state.user["username"], "alice")

    def test_valid_token_passes_full_user_context(self):
        """All X-User-* headers must be propagated when token is valid."""
        token = _compute_kefu_token(KEFU_SECRET)
        req = _build_request(
            headers={
                "X-User-Id": "u1",
                "X-User-Name": "bob",
                "X-Tenant-Id": "t99",
                "X-User-Type": "1",
                "X-User-Permissions": "admin,read",
                "X-Kefu-Internal-Token": token,
            },
        )
        settings = _SettingsStub(kefu_internal_token=KEFU_SECRET)
        result, called = self._dispatch(req, settings)
        self.assertEqual(result.status_code, 200)
        u = called[0].state.user
        self.assertEqual(u["userId"], "u1")
        self.assertEqual(u["tenantId"], "t99")
        self.assertEqual(u["userType"], "1")
        self.assertEqual(u["permissions"], ["admin", "read"])

    # ---- Failure paths: spoofed / missing token ----

    def test_spoofed_x_user_rejected_without_token(self):
        """Direct request with X-User-Id but no token must be rejected (401)."""
        req = _build_request(
            headers={
                "X-User-Id": "hacked-user",
                "X-User-Name": "evil",
            },
        )
        settings = _SettingsStub(kefu_internal_token=KEFU_SECRET)
        result, called = self._dispatch(req, settings)
        self.assertEqual(result.status_code, 401,
                         "X-User-Id without internal token must return 401")
        self.assertEqual(len(called), 0,
                         "call_next must NOT be invoked for spoofed requests")

    def test_wrong_token_rejected(self):
        """Wrong token value must be rejected even with X-User-Id present."""
        req = _build_request(
            headers={
                "X-User-Id": "user-x",
                "X-Kefu-Internal-Token": "wrong-hmac-value",
            },
        )
        settings = _SettingsStub(kefu_internal_token=KEFU_SECRET)
        result, called = self._dispatch(req, settings)
        self.assertEqual(result.status_code, 401,
                         "Wrong internal token must be rejected")
        self.assertEqual(len(called), 0)

    def test_empty_token_config_rejects_x_user(self):
        """When kefu_internal_token is empty, any X-User-* must be rejected (fail-closed)."""
        req = _build_request(
            headers={
                "X-User-Id": "user-x",
                "X-Kefu-Internal-Token": "anything",
            },
        )
        settings = _SettingsStub(kefu_internal_token="")  # unset secret
        result, called = self._dispatch(req, settings)
        self.assertEqual(result.status_code, 401,
                         "Empty kefu_internal_token must fail-closed")
        self.assertEqual(len(called), 0)

    # ---- Health exemption ----

    def test_health_exempt_from_trust_check(self):
        """/api/kefu/health must bypass trust-boundary check entirely."""
        req = _build_request(
            path="/api/kefu/health",
            headers={
                "X-User-Id": "anyone",
                "X-Kefu-Internal-Token": "no-matter",
            },
        )
        settings = _SettingsStub(kefu_internal_token=KEFU_SECRET)
        result, called = self._dispatch(req, settings)
        self.assertEqual(result.status_code, 200,
                         "Health endpoint must be exempt from trust checks")
        self.assertEqual(len(called), 1, "call_next must be invoked for health")

    def test_health_subpath_exempt(self):
        """/api/kefu/health/xxx must also be exempt (prefix match)."""
        req = _build_request(
            path="/api/kefu/health/details",
            headers={},
        )
        settings = _SettingsStub(kefu_internal_token=KEFU_SECRET)
        result, called = self._dispatch(req, settings)
        self.assertEqual(result.status_code, 200)
        self.assertEqual(len(called), 1)

    # ---- Bearer JWT fallback (no X-User-*) ----

    def test_valid_bearer_jwt_works_without_token(self):
        """Request with Bearer JWT but no X-User-* must fall back to JWT verification."""
        token = pyjwt.encode(
            {"sub": "jwt-user", "username": "bob", "tenantId": "t1"},
            JWT_SECRET,
            algorithm="HS384",
        )
        req = _build_request(
            headers={"Authorization": f"Bearer {token}"},
        )
        settings = _SettingsStub(kefu_internal_token=KEFU_SECRET)
        result, called = self._dispatch(req, settings)
        self.assertEqual(result.status_code, 200,
                         "Valid Bearer JWT must work without internal token")
        self.assertEqual(called[0].state.user["userId"], "jwt-user")

    def test_missing_both_auth_methods_returns_401(self):
        """Request with neither X-User-* nor Bearer must get 401."""
        req = _build_request(headers={})
        settings = _SettingsStub(kefu_internal_token=KEFU_SECRET)
        result, called = self._dispatch(req, settings)
        self.assertEqual(result.status_code, 401,
                         "No auth must return 401")
        self.assertEqual(len(called), 0)

    def test_invalid_bearer_jwt_returns_401(self):
        """Malformed Bearer token must return 401."""
        req = _build_request(
            headers={"Authorization": "Bearer invalid-token-here"},
        )
        settings = _SettingsStub(kefu_internal_token=KEFU_SECRET)
        result, called = self._dispatch(req, settings)
        self.assertEqual(result.status_code, 401)
        self.assertEqual(len(called), 0)

    # ---- Constant-time comparison ----

    def test_hmac_compare_digest_used(self):
        """Verify the middleware uses hmac.compare_digest, not '=='."""
        from app.core import auth_middleware
        source = auth_middleware.__file__
        with open(source, "r", encoding="utf-8") as f:
            content = f.read()
        self.assertIn("hmac.compare_digest", content,
                      "Middleware must use hmac.compare_digest for constant-time comparison")


if __name__ == "__main__":
    unittest.main()
