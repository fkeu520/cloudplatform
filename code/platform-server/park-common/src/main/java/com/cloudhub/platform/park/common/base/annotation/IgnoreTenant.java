package com.cloudhub.platform.park.common.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 忽略多租户过滤 (csyh cn.flyrise.common.core.annotation.IgnoreTenant 翻译)
 * <p>csyh 出现 78 次, 标识某个 Service/Mapper 调用需绕过 TenantLineInnerInterceptor.
 * 翻译策略: 与云枢 TenantLineHandler.ignoreTable 配合, Mapper 方法或 Service 方法上标注.</p>
 * <p>W2 阶段: 注解壳就位. W3 阶段接入 MyBatis-Plus TenantLineHandler, 通过 ThreadLocal
 * 配合实现真正的 bypass.</p>
 * <p>使用示例:
 * <pre>{@code
 * @IgnoreTenant
 * public List<SysUser> findAll() { return userMapper.selectList(null); }
 * }</pre>
 * </p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreTenant {

    /**
     * 备注 (说明为什么要跳过)
     */
    String reason() default "";
}
