package com.cloudhub.platform.park.common.base.constants;

/**
 * 安全 / JWT 相关常量 (csyh cn.flyrise.common.core.constant.SecurityConstants 翻译)
 * <p>csyh 出现 271 次, 包含 token 头 / 来源 / 用户名 等常量. 翻译策略:
 * 保留 csyh 常量名, 值与云枢保持兼容.</p>
 */
public final class SecurityConstants {

    private SecurityConstants() {}

    /** HTTP Header: Authorization */
    public static final String AUTHORIZATION = "Authorization";

    /** HTTP Header: token 前缀 (Bearer) */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** HTTP Header: 内部调用来源标识 */
    public static final String FROM_IN = "from=in";

    /** HTTP Header: 用户名 */
    public static final String USER_HEADER = "X-User-Name";

    /** HTTP Header: 用户 ID */
    public static final String USER_ID_HEADER = "X-User-Id";

    /** HTTP Header: 租户 ID */
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";

    /** JWT Claims: 用户名 */
    public static final String CLAIM_USERNAME = "username";

    /** JWT Claims: 用户 ID */
    public static final String CLAIM_USER_ID = "userId";

    /** JWT Claims: 租户 ID */
    public static final String CLAIM_TENANT_ID = "tenantId";

    /** JWT Claims: 角色列表 */
    public static final String CLAIM_ROLES = "roles";

    /** JWT Claims: 权限列表 */
    public static final String CLAIM_PERMISSIONS = "permissions";
}
