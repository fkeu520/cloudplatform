import jwt
from fastapi import Request, HTTPException
from starlette.middleware.base import BaseHTTPMiddleware
from typing import List, Optional

EXEMPT_PATHS: List[str] = [
    "/api/kefu/health",
]


class JwtAuthMiddleware(BaseHTTPMiddleware):
    """JWT 认证中间件 — 对接 platform-auth 的 JwtUtil (Java)。

    Java JwtUtil 使用 HS256 + HMAC secret，claims 结构:
    - sub: userId (subject)
    - username: 用户名
    - tenantId: 租户ID
    - userType: 用户类型 (0=普通 1=租户管理员 2=运营管理员)
    - permissions: 权限列表

    本中间件验证 Bearer token 并将用户上下文注入 request.state.user。
    豁免路径仅 /api/kefu/health。
    """

    async def dispatch(self, request: Request, call_next):
        path = request.url.path
        if any(path.startswith(p) for p in EXEMPT_PATHS):
            return await call_next(request)

        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            raise HTTPException(status_code=401, detail="Missing or invalid Authorization header")

        token = auth_header[7:]
        secret = request.app.state.settings.jwt_secret

        try:
            payload = jwt.decode(token, secret, algorithms=["HS256"])
            request.state.user = {
                "userId": payload.get("sub"),
                "username": payload.get("username"),
                "tenantId": payload.get("tenantId"),
                "userType": payload.get("userType"),
                "permissions": payload.get("permissions", []),
            }
        except jwt.ExpiredSignatureError:
            raise HTTPException(status_code=401, detail="Token expired")
        except jwt.InvalidTokenError:
            raise HTTPException(status_code=401, detail="Invalid token")

        return await call_next(request)