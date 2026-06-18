package com.cloudhub.platform.park.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解 (csyh cn.flyrise.framework.shiro.RequiresPermissions 适配壳)
 * <p>设计原则: 保留 Shiro 原注解的"语义" (value = "user:add"),
 * 业务代码改 import 即可, 方法签名不变.</p>
 * <p>实际鉴权由 {@link com.cloudhub.platform.park.common.security.aspect.RequiresPermissionsAspect}
 * 切面拦截, 委托 platform-user 服务的 /user/internal/check-permission 端点.</p>
 * <p>W2 阶段: 只做"壳" — 注解本身 + 切面. 不引入 spring-security 依赖.
 * W3 阶段: 切换到 Spring Security (PreAuthorize "hasAuthority") 实现, 切面内部替换.</p>
 * <p>使用示例:
 * <pre>{@code
 * @RequiresPermissions("user:add")
 * @PostMapping("/user")
 * public Result<Void> createUser(@RequestBody UserDTO dto) { ... }
 * }</pre>
 * </p>
 * @see org.apache.shiro.authz.annotation.RequiresPermissions (源类)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermissions {

    /**
     * 权限字符串, 多个值表示 AND 关系 (Shiro 默认行为)
     * <p>例: {@code @RequiresPermissions("user:add")} 要求用户拥有 user:add 权限.
     * 例: {@code @RequiresPermissions({"user:view", "user:edit"})} 要求同时拥有两个权限.</p>
     */
    String[] value();

    /**
     * 多个值之间的逻辑关系. 默认 AND (Shiro 默认行为保持一致).
     * <p>例: {@code @RequiresPermissions(value = {"user:view", "user:export"}, logical = Logical.OR)}
     * 表示拥有任一权限即可.</p>
     */
    Logical logical() default Logical.AND;
}
