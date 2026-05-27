package com.cloudhub.platform.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /**
     * 模块标题
     */
    String title() default "";

    /**
     * 业务类型（0其它 1新增 2修改 3删除）
     */
    int businessType() default 0;

    /**
     * 操作类别（0其它 1后台用户 2手机端用户）
     */
    int operatorType() default 1;

    /**
     * 是否保存请求的参数
     */
    boolean isSaveRequestData() default true;

    /**
     * 是否保存响应的参数
     */
    boolean isSaveResponseData() default true;
}
