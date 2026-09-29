import hmac
import hashlib
import json
import jwt
import logging

logger = logging.getLogger(__name__)

EXEMPT_PATHS: List[str] = [
    "/api/kefu/health",
]

# Header names (canonical, case-insensitive per HTTP spec)
X_USER_ID = "x-user-id"
X_KEFU_INTERNAL_TOKEN = "x-kefu-internal-token"


def _compute_kefu_token(secret: str) -> str:
    """Reproduce the gateway's HMAC-SHA256(secret, "kefu-internal") computation."""
    if not secret:
        return ""
    mac = hmac.new(
        secret.encode("utf-8"),
        b"kefu-internal",
        hashlib.sha256,
    )
    return mac.hexdigest()


def _token_matches(stored_secret: str, provided_digest: str) -> bool:
    """Constant-time comparison: recompute HMAC and compare against provided digest."""
    expected = _compute_kefu_token(stored_secret)
    if not expected:
        return False
    return hmac.compare_digest(expected.encode("utf-8"), provided_digest.encode("utf-8"))


class JwtAuthMiddleware:
    """JWT 认证中间件 (纯 ASGI) — 对接 platform-gateway 的内部令牌契约 (P0 安全修复 2026-09-24).

    信任边界策略:
    1. 若请求携带有效的 X-Kefu-Internal-Token (HMAC-SHA256), 则信任其 X-User-* 头
       — 这是 gateway 验签后注入的内部合约头, 不可伪造.
    2. 若请求未携带有效内部令牌但携带 X-User-* 头 → 401 (防止客户端伪造身份).
    3. 若无 X-User-* 头 → 走本地 Bearer JWT 验签 (直连 kefu:8050 降级路径).
    4. /api/kefu/health 完全豁免.

    2026-09-29 修复「点击发送无响应」: 原实现继承 BaseHTTPMiddleware, 该基类会
    把请求流包一层 anyio memory object stream。带 body 的请求 (POST/PUT/PATCH)
    经它转发后, 下游拿不到 request.stream, 且响应体在流回客户端时被吞掉 —
    表现为 GET 正常 200, POST 请求发出后永不返回, 客户端最终拿到 200 + 空 body
    (前端 res.data 为空 → session_id 变undefined → 发送按钮永远 disabled)。
    FastAPI/Starlette 官方明确不建议用 BaseHTTPMiddleware 处理带 body 的请求,
    推荐改用纯 ASGI 中间件。本实现直接操作 scope/receive/send, 不做任何流包装。
    """

    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope.get("type") != "http":
            await self.app(scope, receive, send)
            return

        path = scope.get("path", "")
        if any(path.startswith(p) for p in EXEMPT_PATHS):
            await self.app(scope, receive, send)
            return

        # 构造一个轻量的 request 视图, 只为读取 headers 与写入 state
        headers = _Headers(scope)
        app_state = getattr(self.app, "state", None)
        app_settings = getattr(app_state, "settings", None)

        def _json_error(status: int, message: str):
            body = json.dumps({"code": status, "message": message}).encode("utf-8")
            async def _send(resp_send):
                await resp_send({
                    "type": "http.response.start",
                    "status": status,
                    "headers": [(b"content-type", b"application/json"),
                                (b"content-length", str(len(body)).encode())],
                })
                await resp_send({"type": "http.response.body", "body": body})
            return _send

        internal_token = getattr(app_settings, "kefu_internal_token", "")
        provided_token = headers.get(X_KEFU_INTERNAL_TOKEN)
        gateway_user_id = headers.get("X-User-Id")

        # 路径 1: 有效内部令牌 → 信任 X-User-* 头
        if _token_matches(internal_token, provided_token):
            scope.setdefault("state", {})["user"] = {
                "userId": headers.get("X-User-Id") or "",
                "username": headers.get("X-User-Name") or "",
                "tenantId": headers.get("X-Tenant-Id") or "",
                "userType": headers.get("X-User-Type") or "",
                "permissions": [p for p in (headers.get("X-User-Permissions") or "").split(",") if p],
            }
            await self.app(scope, receive, send)
            return

        # 路径 2: 有 X-User-* 但无有效内部令牌 → 拒绝 (防伪造)
        if gateway_user_id:
            logger.warning(
                "[Auth] X-User-Id present but X-Kefu-Internal-Token invalid/missing, rejecting: %s",
                gateway_user_id,
            )
            await _json_error(401, "Invalid or missing internal token")(send)
            return

        # 路径 3: 无 X-User-*, 降级 Bearer JWT 验签 (直连场景)
        auth_header = headers.get("Authorization") or ""
        if not auth_header.startswith("Bearer "):
            await _json_error(401, "Missing or invalid Authorization header")(send)
            return

        token = auth_header[7:]
        secret = getattr(app_settings, "jwt_secret", "")

        try:
            payload = jwt.decode(token, secret, algorithms=["HS384", "HS256"])
            scope.setdefault("state", {})["user"] = {
                "userId": payload.get("sub"),
                "username": payload.get("username"),
                "tenantId": payload.get("tenantId"),
                "userType": payload.get("userType"),
                "permissions": payload.get("permissions", []),
            }
        except jwt.ExpiredSignatureError:
            await _json_error(401, "Token expired")(send)
            return
        except jwt.InvalidTokenError as e:
            await _json_error(401, f"Invalid token: {e}")(send)
            return

        await self.app(scope, receive, send)


class _Headers:
    """ASGI scope headers 的小封装, 模拟 request.headers.get(不区分大小写)。"""

    __slots__ = ("_raw",)

    def __init__(self, scope):
        self._raw = scope.get("headers") or []

    def get(self, key: str, default: str = ""):
        k = key.lower().encode("latin-1")
        for name, value in self._raw:
            if name.lower() == k:
                return value.decode("latin-1")
        return default
