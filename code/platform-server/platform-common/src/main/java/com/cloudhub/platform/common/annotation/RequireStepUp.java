package com.cloudhub.platform.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 高敏操作 step-up 鉴权注解
 * <p>配套 StepUpAspect (platform-auth 模块) 校验 X-Step-Up-Token 头.</p>
 * <p>典型用法:</p>
 * <pre>{@code
 * @RequireStepUp(scope = "tenant:delete", description = "删除租户")
 * @DeleteMapping("/{id}")
 * public Result<Void> delete(@PathVariable Long id) { ... }
 * }</pre>
 *
 * @author cloudhub
 * @since v8.0 (2026-07)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireStepUp {

    /**
     * 允许的 scope (必须与 step-up token 签发时声明一致).
     * 多个 scope 用逗号分隔 (任一匹配即通过).
     */
    String scope();

    /**
     * 操作描述 (用于审计日志)
     */
    String description() default "";

    /**
     * 是否允许 multi-use token (默认 false, 即必须单次有效).
     * 批量操作可设为 true.
     */
    boolean allowMultiUse() default false;
}
