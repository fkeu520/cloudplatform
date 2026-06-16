package com.cloudhub.platform.park.common.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 内部调用标识 (csyh cn.flyrise.common.core.annotation.Inner 翻译)
 *
 * <p>csyh 出现 93 次, 标识一个 Controller/Service 是"内部调用" (来自其他微服务或定时任务),
 * 应跳过租户过滤、跳过鉴权、跳过 IP 限制等.</p>
 *
 * <p>W2 阶段: 注解壳就位, 不引入 AOP 拦截. 业务服务自行在 Filter/Interceptor 中识别.
 * W3 阶段: 接入 TenantFilter / JwtAuthFilter 识别此注解.</p>
 *
 * <p>使用示例:
 * <pre>{@code
 * @Inner
 * @GetMapping("/internal/user/{id}")
 * public Result<SysUser> internalGetUser(@PathVariable Long id) { ... }
 * }</pre>
 * </p>
 *
 * @author csyh fusion W2.1
 * @since 2026-06-16
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Inner {

    /**
     * 是否需要登录 (默认 false, 即完全匿名内部调用)
     */
    boolean requireAuth() default false;

    /**
     * 是否绕过租户隔离 (默认 true, 内部调用跨租户)
     */
    boolean bypassTenant() default true;
}
