package com.cloudhub.platform.park.common.base.validation;

import jakarta.validation.groups.Default;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 校验分组: 新增场景 (csyh cn.flyrise.common.core.validation.Create 翻译)
 *
 * <p>csyh 出现 230 次. 用法: 在 DTO 字段上标 {@code @NotNull(groups = Create.class)},
 * Controller 方法上 {@code @Validated(Create.class)} 触发.</p>
 *
 * <p>云枢用 jakarta.validation.GroupSequence, 这里提供兼容注解 (内部就是 Default 标记).</p>
 *
 * <p>使用示例:
 * <pre>{@code
 * public class RoomDTO {
 *     @NotNull(groups = Create.class, message = "新增时名称必填")
 *     private String name;
 * }
 *
 * @PostMapping
 * public Result<Void> create(@Validated(Create.class) @RequestBody RoomDTO dto) { ... }
 * }</pre>
 * </p>
 *
 * @author csyh fusion W2.1
 * @since 2026-06-16
 * @see Update
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Create {
    // 占位注解, 用于 @Validated 触发分组
}
