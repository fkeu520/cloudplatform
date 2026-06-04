package com.cloudhub.platform.common.config;

import com.cloudhub.platform.common.annotation.DataScope;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 数据权限 AOP 切面 (M5 P0-2 实施, 2026-06-04 完整化)
 *
 * 配套: doc/M5-P0-2-实施子任务.md + doc/M5-P0-2-决策记录.md
 *
 * <h2>职责</h2>
 * <ol>
 *   <li>读取当前用户 (TenantContextHolder.getUserId())</li>
 *   <li>调 DataScopeProvider (user 模块实现) 查 userId 的 data_scope 上下文</li>
 *   <li>根据 maxDataScope 拼 SQL 片段</li>
 *   <li>写入 DataScopeContextHolder (供 MyBatis 拦截器读取)</li>
 * </ol>
 *
 * <h2>5 种 data_scope SQL 片段生成</h2>
 * <ul>
 *   <li>1=全部 → "" (不加条件)</li>
 *   <li>2=本部门 → " AND {alias}.{deptAlias} = {userDeptId}"</li>
 *   <li>3=本部门及下级 → " AND {alias}.{deptAlias} IN ({childDeptIds})" (TODO: CTE)</li>
 *   <li>4=本人 → " AND {alias}.{userAlias} = {userId}"</li>
 *   <li>5=自定义 → " AND {alias}.{deptAlias} IN ({customDeptIds})"</li>
 * </ul>
 *
 * <h2>多角色合并策略 (决策 1)</h2>
 * 取最严格 (max data_scope), 实现由 DataScopeProvider 完成
 *
 * <h2>dept 子树查询 (决策 2)</h2>
 * scope=3 用 MySQL 8.0 递归 CTE (TODO: 实现 selectChildDeptIds)
 *
 * <h2>跨模块依赖</h2>
 * DataScopeProvider 由 user 模块实现, common 模块通过接口注入
 * 测试场景 (无 user 模块启动) 时, Provider 为 null, 退化为无 data_scope
 *
 * @since 2026-06-04
 */
@Slf4j
@Aspect
@Component
public class DataScopeAspect {

    /**
     * 数据权限 Provider (由 user 模块实现, 可选)
     * required = false: 测试环境 (common 单测) 无 user 模块时为 null
     */
    @Autowired(required = false)
    private DataScopeProvider dataScopeProvider;

    @Before("@annotation(dataScope)")
    public void doBefore(JoinPoint point, DataScope dataScope) {
        Long userId = TenantContextHolder.getUserId();
        if (userId == null) {
            // 无用户上下文, 不加 data_scope 条件
            DataScopeContextHolder.set("");
            return;
        }

        // 1. 查 user 的 data_scope 上下文 (通过 Provider 接口)
        DataScopeContext context = lookupContext(userId);

        // 2. 拼 SQL 片段
        String fragment = buildFragment(context, userId, dataScope);
        DataScopeContextHolder.set(fragment);

        log.debug("DataScope: userId={}, scope={}, fragment=[{}]",
                userId, context.getMaxDataScope(), fragment);
    }

    @After("@annotation(dataScope)")
    public void doAfter(JoinPoint point, DataScope dataScope) {
        // 清理 ThreadLocal, 避免线程复用污染
        DataScopeContextHolder.clear();
    }

    /**
     * 查 user 的 data_scope 上下文
     * 安全降级: Provider 未注入 (测试环境) 时, 返回 none() (无限制)
     */
    private DataScopeContext lookupContext(Long userId) {
        return Optional.ofNullable(dataScopeProvider)
                .map(provider -> {
                    try {
                        return provider.getContext(userId);
                    } catch (Exception e) {
                        log.warn("DataScopeProvider.getContext failed, fallback to no-scope. userId={}", userId, e);
                        return DataScopeContext.none();
                    }
                })
                .orElse(DataScopeContext.none());
    }

    /**
     * 根据 DataScopeContext 拼 SQL 片段
     *
     * 格式约定 (2026-06-04 修正):
     *   - 表达式**不带 "AND" 前导** (jsqlparser 解析需求)
     *   - 表达式**用括号包装** (e.g. "(id = 5)" 或 "(dept_id IN (1,2,3))")
     *   - 由 DataScopeInnerInterceptor 决定 WHERE 拼接方式
     *
     * 示例:
     *   scope=1 → ""            (空, 拦截器不改 SQL)
     *   scope=2 → "(dept_id = 100)"
     *   scope=4 → "(id = 5)"
     *   scope=5 → "(dept_id IN (1,2,3))"
     */
    private String buildFragment(DataScopeContext ctx, Long userId, DataScope annotation) {
        String alias = annotation.alias().isEmpty() ? "" : annotation.alias() + ".";
        int scope = ctx.getMaxDataScope();

        switch (scope) {
            case 1: // 全部
                return "";
            case 2: // 本部门
                if (ctx.getUserDeptId() == null) {
                    log.debug("DataScope scope=2 but userDeptId is null, fallback to scope=4 (本人)");
                    return buildSelfFragment(alias, userId, annotation);
                }
                return String.format("(%s%s = %d)", alias, annotation.deptAlias(), ctx.getUserDeptId());
            case 3: // 本部门及下级 (决策 2: 应用层递归 / MySQL CTE)
                if (ctx.getChildDeptIds() != null && !ctx.getChildDeptIds().isEmpty()) {
                    return String.format("(%s%s IN (%s))", alias, annotation.deptAlias(), ctx.getChildDeptIds());
                }
                // childDeptIds 为空: 退化为 scope=2 (本部门)
                log.debug("DataScope scope=3 but childDeptIds is empty, fallback to scope=2");
                if (ctx.getUserDeptId() == null) {
                    return buildSelfFragment(alias, userId, annotation);
                }
                return String.format("(%s%s = %d)", alias, annotation.deptAlias(), ctx.getUserDeptId());
            case 4: // 本人
                return buildSelfFragment(alias, userId, annotation);
            case 5: // 自定义
                if (ctx.getCustomDeptIds() == null || ctx.getCustomDeptIds().isEmpty()) {
                    log.debug("DataScope scope=5 but customDeptIds is empty, fallback to scope=4 (本人)");
                    return buildSelfFragment(alias, userId, annotation);
                }
                return String.format("(%s%s IN (%s))", alias, annotation.deptAlias(), ctx.getCustomDeptIds());
            default: // 未知 scope, 退化
                log.warn("Unknown data_scope: {}, fallback to no-scope", scope);
                return "";
        }
    }

    private String buildSelfFragment(String alias, Long userId, DataScope annotation) {
        return String.format("(%s%s = %d)", alias, annotation.userAlias(), userId);
    }
}
