import hmac
import hashlib
import jwt
import logging
from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.responses import JSONResponse
from typing import List

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


class JwtAuthMiddleware(BaseHTTPMiddleware):
    """JWT 认证中间件 — 对接 platform-gateway 的内部令牌契约 (P0 安全修复 2026-09-24).

    信任边界策略:
    1. 若请求携带有效的 X-Kefu-Internal-Token (HMAC-SHA256), 则信任其 X-User-* 头
       — 这是 gateway 验签后注入的内部合约头, 不可伪造.
    2. 若请求未携带有效内部令牌但携带 X-User-* 头 → 401 (防止客户端伪造身份).
    3. 若无 X-User-* 头 → 走本地 Bearer JWT 验签 (直连 kefu:8050 降级路径).
    4. /api/kefu/health 完全豁免.

    注意: BaseHTTPMiddleware 中直接 raise HTTPException 会被 Starlette 的
    ServerErrorMiddleware 当成 500 返回 (fastapi#2863), 必须用 JSONResponse 显式返回。
    """

    async def dispatch(self, request: Request, call_next):
        path = request.url.path
        if any(path.startswith(p) for p in EXEMPT_PATHS):
            return await call_next(request)

        internal_token = getattr(request.app.state.settings, "kefu_internal_token", "")
        provided_token = request.headers.get(X_KEFU_INTERNAL_TOKEN, "")
        gateway_user_id = request.headers.get("X-User-Id")  # case-insensitive lookup

        # 路径 1: 有效内部令牌 → 信任 X-User-* 头
        if _token_matches(internal_token, provided_token):
            request.state.user = {
                "userId": request.headers.get("X-User-Id", ""),
                "username": request.headers.get("X-User-Name", ""),
                "tenantId": request.headers.get("X-Tenant-Id", ""),
                "userType": request.headers.get("X-User-Type", ""),
                "permissions": [p for p in request.headers.get("X-User-Permissions", "").split(",") if p],
            }
            return await call_next(request)

        # 路径 2: 有 X-User-* 但无有效内部令牌 → 拒绝 (防伪造)
        if gateway_user_id:
            logger.warning(
                "[Auth] X-User-Id present but X-Kefu-Internal-Token invalid/missing, rejecting: %s",
                gateway_user_id,
            )
            return JSONResponse(
                status_code=401,
                content={"code": 401, "message": "Invalid or missing internal token"},
            )

        # 路径 3: 无 X-User-*, 降级 Bearer JWT 验签 (直连场景)
        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            return JSONResponse(
                status_code=401,
                content={"code": 401, "message": "Missing or invalid Authorization header"},
            )

        token = auth_header[7:]
        secret = request.app.state.settings.jwt_secret

        try:
            payload = jwt.decode(token, secret, algorithms=["HS384", "HS256"])
            request.state.user = {
                "userId": payload.get("sub"),
                "username": payload.get("username"),
                "tenantId": payload.get("tenantId"),
                "userType": payload.get("userType"),
                "permissions": payload.get("permissions", []),
            }
        except jwt.ExpiredSignatureError:
            return JSONResponse(
                status_code=401,
                content={"code": 401, "message": "Token expired"},
            )
        except jwt.InvalidTokenError as e:
            return JSONResponse(
                status_code=401,
                content={"code": 401, "message": f"Invalid token: {e}"},
            )

        return await call_next(request)
