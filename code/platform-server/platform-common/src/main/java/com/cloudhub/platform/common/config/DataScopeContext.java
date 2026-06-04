package com.cloudhub.platform.common.config;

import lombok.Builder;
import lombok.Data;

/**
 * 数据权限上下文 (M5 P0-2 实施)
 *
 * 配套: doc/M5-P0-2-实施子任务.md + doc/M5-P0-2-决策记录.md
 *
 * 设计目的: DataScopeAspect 在 common 模块, 不能直接 import user 模块的 Mapper
 *          通过 DataScopeProvider 接口 + DataScopeContext 数据传输, 实现依赖倒置
 *
 * 使用流程:
 *   1. DataScopeProvider (user 模块实现) 查 DB 返回 DataScopeContext
 *   2. DataScopeAspect 根据 context.maxDataScope 决定 SQL 片段
 *   3. DataScopeInnerInterceptor 拼 SQL 片段到原 SQL
 *
 * @since 2026-06-04
 */
@Data
@Builder
public class DataScopeContext {

    /**
     * 最大 data_scope (合并策略: 取最严格)
     * 0 = 无 data_scope 限制 (角色未配置)
     * 1 = 全部 (不加条件)
     * 2 = 本部门
     * 3 = 本部门及下级 (需 CTE)
     * 4 = 本人
     * 5 = 自定义 (custom_dept_ids)
     */
    private final int maxDataScope;

    /**
     * 当前用户部门 ID (scope=2/3 必填)
     * null 时 scope=2/3 退化为 scope=4 (本人)
     */
    private final Long userDeptId;

    /**
     * 自定义部门 ID 列表 (scope=5 必填)
     * null 时 scope=5 退化为 scope=4 (本人)
     */
    private final String customDeptIds;

    /**
     * 静态工厂: 无限制 (空 context)
     */
    public static DataScopeContext none() {
        return DataScopeContext.builder().maxDataScope(1).build();
    }

    /**
     * 静态工厂: 全部权限
     */
    public static DataScopeContext all() {
        return DataScopeContext.builder().maxDataScope(1).build();
    }
}
