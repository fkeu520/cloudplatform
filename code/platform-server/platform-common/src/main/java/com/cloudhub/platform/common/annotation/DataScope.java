package com.cloudhub.platform.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    String alias() default "";
    String deptAlias() default "dept_id";
    String userAlias() default "created_by";
    boolean includeSelf() default true;
}
