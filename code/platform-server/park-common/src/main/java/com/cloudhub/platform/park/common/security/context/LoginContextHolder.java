package com.cloudhub.platform.park.common.security.context;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 当前登录用户上下文 (csyh SecurityUtils.getSubject() 翻译)
 * <p>用 ThreadLocal 传递当前 LoginUser, 业务代码通过 {@link #get()} 获取.
 * 替代 Shiro 的 {@code SecurityUtils.getSubject()}, 不依赖 spring-security.</p>
 * <p><b>使用示例</b>:
 * <pre>{@code
 * // Filter/Interceptor 写入 (W3 由 JwtAuthFilter 自动完成)
 * LoginContextHolder.set(LoginUser.builder()
 *     .userId(1L).username("admin").tenantId(1L)
 *     .permissions(Set.of("user:add", "user:edit"))
 *     .build());
 * // 业务代码读取
 * LoginUser current = LoginContextHolder.get();
 * if (current != null && current.getPermissions().contains("user:add")) {
 *     // 有权限
 * }
 * // 请求结束时清除 (Filter 末尾调用, 避免线程复用泄露)
 * LoginContextHolder.clear();
 * }</pre>
 * <p><b>W2 阶段</b>: 使用 {@link ThreadLocal} (JDK 内置, 零依赖).
 * <br><b>W3 阶段</b>:
 * <ol>
 *   <li>引入 {@code com.alibaba:transmittable-thread-local} 切换到 TransmittableThreadLocal
 *       (解决 {@code @Async} / 线程池场景下上下文丢失问题)</li>
 *   <li>在 platform-auth 增加 JwtAuthFilter, 解析 JWT → 写入本 Holder</li>
 * </ol>
 */
public final class LoginContextHolder {

    private LoginContextHolder() {}

    /**
     * PC2-4: TODO 当前为 JDK ThreadLocal, @Async / 线程池场景会丢失上下文.
     * <p>升级路径: 启用 park-common/pom.xml 中的 transmittable-thread-local 依赖,
     * 把本字段改为 {@code new TransmittableThreadLocal<>()},
     * 并把业务线程池用 {@code TtlExecutors.getTtlExecutorService()} 包装.</p>
     * <p>为何未启用: 当前离线模式无外网, TTL jar 未在本地 Maven 缓存中.
     * 待 217 部署机器可联网时取消 pom 注释, 升级本类即可.</p>
     */
    private static final ThreadLocal<LoginUser> CONTEXT = new ThreadLocal<>();

    /**
     * 设置当前登录用户
     */
    public static void set(LoginUser user) {
        CONTEXT.set(user);
    }

    /**
     * 获取当前登录用户
     * @return 当前 LoginUser, 未登录返回 null
     */
    public static LoginUser get() {
        return CONTEXT.get();
    }

    /**
     * 获取当前用户 ID (便捷方法)
     */
    public static Long getUserId() {
        LoginUser u = get();
        return u == null ? null : u.getUserId();
    }

    /**
     * 获取当前用户名 (便捷方法)
     */
    public static String getUsername() {
        LoginUser u = get();
        return u == null ? null : u.getUsername();
    }

    /**
     * 获取当前租户 ID (便捷方法)
     */
    public static Long getTenantId() {
        LoginUser u = get();
        return u == null ? null : u.getTenantId();
    }

    /**
     * 获取当前用户权限集合 (便捷方法)
     */
    public static Set<String> getPermissions() {
        LoginUser u = get();
        return u == null ? Collections.emptySet() : u.getPermissions();
    }

    /**
     * 获取当前用户角色列表 (便捷方法)
     */
    public static List<String> getRoles() {
        LoginUser u = get();
        return u == null ? Collections.emptyList() : u.getRoles();
    }

    /**
     * 判断当前用户是否拥有指定权限
     */
    public static boolean hasPermission(String permission) {
        return getPermissions().contains(permission);
    }

    /**
     * 判断当前用户是否拥有全部指定权限 (AND)
     */
    public static boolean hasAllPermissions(String... permissions) {
        if (permissions == null || permissions.length == 0) return true;
        Set<String> owned = getPermissions();
        for (String p : permissions) {
            if (!owned.contains(p)) return false;
        }
        return true;
    }

    /**
     * 判断当前用户是否拥有任一指定权限 (OR)
     */
    public static boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) return true;
        Set<String> owned = getPermissions();
        for (String p : permissions) {
            if (owned.contains(p)) return true;
        }
        return false;
    }

    /**
     * 判断当前用户是否拥有指定角色
     */
    public static boolean hasRole(String role) {
        return getRoles().contains(role);
    }

    /**
     * 清除当前上下文 (Filter 末尾调用, 避免线程复用泄露)
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
