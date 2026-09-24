"""Kefu 租户访问控制 — fail-closed P0 contract.

设计原则
--------
1. 缺失 tenantId 视为无租户：normalize_tenant_id(None) 抛出 ValueError，调用方据此拒绝请求。
2. 不信任客户端 header / query 参数作为租户身份来源。只从 request.state.user
   （由 auth_middleware.py 从 X-User-* 或 JWT claim 注入）读取 tenantId。
3. 平台管理员 (userType==2) 语义：tenantId 可以为 None，此时跨租户可见（fail-open
   仅在 userType==2 时开启；其他角色必须带非空 tenantId）。
    4. 旧行迁移后先落在 tenant 0；普通租户查询不会返回这些行，平台管理员仍可审计。

"""
from __future__ import annotations

from typing import Any


def normalize_tenant_id(claim: Any) -> int:
    """将 tenantId 声明归一化为 int。

    Java JwtUtil 产出 Long，Pydantic/fastapi 可能传入 str / int / None。
    - None  → 抛出 ValueError（fail-closed）
    - "7"   → 7
    - 7     → 7
    - 0 / -1 / 负数 → 抛出 ValueError（无效租户）

    调用方须在捕获 ValueError 后返回 403/401。
    """
    if claim is None:
        raise ValueError("tenantId is missing; tenant-scoped access denied")
    if isinstance(claim, str):
        claim = claim.strip()
        if claim == "":
            raise ValueError("tenantId is empty; tenant-scoped access denied")
        try:
            value = int(claim)
        except ValueError:
            raise ValueError(f"tenantId has invalid string value: {claim!r}")
    elif isinstance(claim, bool):
        raise ValueError("tenantId has unsupported boolean value")
    elif isinstance(claim, int):
        value = claim
    elif isinstance(claim, float):
        if not claim.is_integer():
            raise ValueError("tenantId must be an integer")
        value = int(claim)
    else:
        raise ValueError(f"tenantId has unsupported type: {type(claim).__name__}")

    if value <= 0:
        raise ValueError(f"tenantId must be positive, got {value}")
    return value


def has_permission(user: dict[str, Any], permission: str) -> bool:
    """检查用户是否拥有给定权限。

    user 结构由 auth_middleware.py 注入到 request.state.user，预期 keys:
      - userId (str|int)
      - username (str)
      - tenantId (str|int|None)
      - userType (str|int|None): 0=普通, 1=租户管理员, 2=运营管理员
      - permissions (list[str])

    平台管理员 (userType == 2) 绕过权限检查返回 True（与 platform-server 的
    opsAdmin/regularUser 语义保持一致）。
    """
    if not isinstance(user, dict):
        return False
    perms_raw = user.get("permissions") or []
    if isinstance(perms_raw, str):
        perms = [p.strip() for p in perms_raw.split(",") if p.strip()]
    elif isinstance(perms_raw, (list, tuple, set)):
        perms = perms_raw
    else:
        return False
    if permission in perms:
        return True
    # 运营管理员 (userType=2) 拥有所有 kefu 权限
    user_type_raw = user.get("userType")
    try:
        user_type = int(user_type_raw) if user_type_raw is not None else None
    except (TypeError, ValueError):
        user_type = None
    if user_type == 2:
        return True
    return False
