package com.cloudhub.platform.common.config;

import com.cloudhub.platform.common.annotation.DataScope;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 数据权限 AOP 切面 (M5 P0-2 实施)
 *
 * 配套: doc/M5-P0-2-实施子任务.md
 *
 * <h2>职责</h2>
 * <ol>
 *   <li>读取当前用户 (TenantContextHolder.getUserId())</li>
 *   <li>查用户所有角色的 data_scope + custom_dept_ids</li>
 *   <li>合并多角色策略: 当前实现取**最严格** (scope 值大者)</li>
 *   <li>拼 SQL 片段写入 DataScopeContextHolder</li>
 *   <li>DataScopeInnerInterceptor 在 SQL 拼接时读取</li>
 * </ol>
 *
 * <h2>5 种 data_scope SQL 片段生成规则</h2>
 * <ul>
 *   <li>1=全部 → "" (不加条件)</li>
 *   <li>2=本部门 → " AND {deptAlias}.dept_id = {userDeptId}"</li>
 *   <li>3=本部门及下级 → " AND {deptAlias}.dept_id IN ({childDeptIds})"</li>
 *   <li>4=本人 → " AND {userAlias}.id = {userId}"</li>
 *   <li>5=自定义 → " AND {deptAlias}.dept_id IN ({customDeptIds})"</li>
 * </ul>
 *
 * <h2>当前限制 (M5 起步版)</h2>
 * <ul>
 *   <li>未查 DB 取 userDeptId (TODO: UserDeptQueryService)</li>
 *   <li>未递归查子部门 (scope=3 TODO: CTE)</li>
 *   <li>未跨服务调用透传 (M5 后期)</li>
 * </ul>
 *
 * @since 2026-06-04
 */
@Slf4j
@Aspect
@Component
public class DataScopeAspect {

    @Before("@annotation(dataScope)")
    public void doBefore(JoinPoint point, DataScope dataScope) {
        Long userId = TenantContextHolder.getUserId();
        if (userId == null) {
            // 无用户上下文, 不加 data_scope 条件
            DataScopeContextHolder.set("");
            return;
        }

        // === M5 起步版: 简化策略 (无 DB 查询, 仅占位) ===
        // 实际实施需:
        //   1. UserRoleMapper.selectDataScopesByUserId(userId) 返回 List<UserRoleDataScope>
        //   2. 解析 dept 子树 (CTE 递归)
        //   3. 合并多角色 (取 max)
        // 详见 doc/M5-P0-2-实施子任务.md

        // 占位 SQL 片段 - 实际生产需替换
        String fragment = buildPlaceholderFragment(userId, dataScope);
        DataScopeContextHolder.set(fragment);

        log.debug("DataScope: userId={}, alias={}, fragment=[{}]",
                userId, dataScope.alias(), fragment);
    }

    @After("@annotation(dataScope)")
    public void doAfter(JoinPoint point, DataScope dataScope) {
        // 清理 ThreadLocal, 避免线程复用污染
        DataScopeContextHolder.clear();
    }

    /**
     * 占位 SQL 片段生成 (M5 起步版, 不查 DB)
     *
     * 实际生产应替换为:
     * 1. 查 user 的所有 role + data_scope
     * 2. 合并多角色 (取 max)
     * 3. 查 user.dept_id (scope=2/3)
     * 4. 递归查 dept 子树 (scope=3)
     * 5. 解析 custom_dept_ids (scope=5)
     */
    private String buildPlaceholderFragment(Long userId, DataScope dataScope) {
        // 默认: scope=4 (本人) - 最严格, 防止越权
        // TODO: 替换为真实 data_scope 查询
        return String.format(" AND %s.id = %d", dataScope.userAlias(), userId);
    }
}
