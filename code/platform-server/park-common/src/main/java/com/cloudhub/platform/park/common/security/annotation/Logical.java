package com.cloudhub.platform.park.common.security.annotation;

/**
 * 权限/角色注解的逻辑关系 (Shiro Logical 兼容)
 *
 * <p>对应 cn.flyrise... Logical enum. 用于 {@link RequiresPermissions#logical()}.</p>
 */
public enum Logical {
    /** 全部满足 (默认) */
    AND,
    /** 任一满足 */
    OR
}
