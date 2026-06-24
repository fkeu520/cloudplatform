package com.cloudhub.platform.park.common.security.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 当前登录用户上下文 (csyh Shiro Subject.getPrincipal() 翻译)
 * <p>替代 Shiro 的 {@code Subject} 概念, 用 ThreadLocal 传递当前用户信息.
 * 业务代码通过 {@link LoginContextHolder#get()} 获取当前 ParkUser (简化版).</p>
 * <p>字段说明:
 * <ul>
 *   <li>userId: 用户 ID (雪花)</li>
 *   <li>username: 用户名</li>
 *   <li>tenantId: 租户 ID (来自 TenantContextHolder, P0-1 多租户拦截器)</li>
 *   <li>roles: 角色编码列表 (e.g. ["admin", "manager"])</li>
 *   <li>permissions: 权限字符串列表 (e.g. ["user:add", "user:edit"])</li>
 * </ul>
 * <p>W2 阶段: 由 Filter/Interceptor 写入 (W3 接入 JwtAuthFilter 后, 自动从 JWT 解析)
 * <br>W3 阶段: 提供 {@code current()} 静态便捷方法, 业务代码无需关心 ThreadLocal.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 租户 ID */
    private Long tenantId;

    /** 角色编码列表 */
    @Builder.Default
    private List<String> roles = Collections.emptyList();

    /** 权限字符串列表 (e.g. ["user:add"]) */
    @Builder.Default
    private Set<String> permissions = Collections.emptySet();

    /** 用户类型: 0=普通 1=租户管理员 2=运营管理员 (来自 X-User-Type Header) */
    private Integer userType;
}
