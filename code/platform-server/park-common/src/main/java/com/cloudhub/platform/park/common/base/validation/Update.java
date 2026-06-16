package com.cloudhub.platform.park.common.base.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 校验分组: 修改场景 (csyh cn.flyrise.common.core.validation.Update 翻译)
 *
 * <p>csyh 出现 317 次. 通常与 {@link Create} 配合, 不同场景下校验规则不同
 * (如 ID 在 Create 时不需要, Update 时必填).</p>
 *
 * @author csyh fusion W2.1
 * @since 2026-06-16
 * @see Create
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Update {
    // 占位注解
}
