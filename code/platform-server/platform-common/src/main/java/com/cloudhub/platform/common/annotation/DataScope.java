package com.cloudhub.platform.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    /**
     * 表别名 (用于多表 JOIN 场景, 如 "u" / "d" / "ur")
     * 默认空 = 主表无别名
     */
    String alias() default "";

    /**
     * 部门字段名
     * - scope=2 时: 拼 "AND {alias}.{deptAlias} = {userDeptId}"
     * - scope=3/5 时: 拼 "AND {alias}.{deptAlias} IN (...)"
     * 默认 "dept_id" (sys_user / sys_dept 等业务表标准字段)
     */
    String deptAlias() default "dept_id";

    /**
     * 用户字段名 (scope=4 时使用)
     * 拼 "AND {alias}.{userAlias} = {userId}"
     * <p>
     * 默认 "id" - 直接用主键 (适合无 create_by 字段的表)
     * 业务层若有 create_by 字段: 显式传 @DataScope(userAlias = "create_by")
     */
    String userAlias() default "id";

    /**
     * scope=3 时是否包含本部门
     * - true: WITH RECURSIVE 包含起点 (本部门)
     * - false: 仅下级
     */
    boolean includeSelf() default true;
}
