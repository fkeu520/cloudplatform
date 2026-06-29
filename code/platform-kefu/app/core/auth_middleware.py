import jwt
from fastapi import Request, HTTPException
from starlette.middleware.base import BaseHTTPMiddleware
from typing import List

EXEMPT_PATHS: List[str] = [
    "/api/kefu/health",
]

class JwtAuthMiddleware(BaseHTTPMiddleware):
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
                "userId": payload.get("userId"),
                "username": payload.get("username"),
                "tenantId": payload.get("tenantId"),
            }
        except jwt.ExpiredSignatureError:
            raise HTTPException(status_code=401, detail="Token expired")
        except jwt.InvalidTokenError:
            raise HTTPException(status_code=401, detail="Invalid token")

        return await call_next(request)