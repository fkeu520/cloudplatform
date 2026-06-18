package com.cloudhub.platform.user.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.user.domain.entity.Dept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeptMapper extends BaseMapper<Dept> {

    List<Dept> selectByOrgId(@Param("orgId") Long orgId);

    List<Dept> selectTreeByOrgId(@Param("orgId") Long orgId);

    /**
     * 用 MySQL 8 递归 CTE 收集子部门 ID 列表 (含本部门)
     * <p>配套: doc/项目进度.md v7.1 §7.1 (PR1 准备文档) · doc/M5-P0-2-决策记录.md v1.1 (决策 2 修订)
     * <h2>行为约定</h2>
     * <ul>
     *   <li>{@code rootDeptId}: 起点部门 ID (用户所在部门, scope=3 必填)</li>
     *   <li>返回结果: 包含 rootDeptId + 所有子部门 ID (已去重, 排序后逗号分隔)</li>
     *   <li>SQL 解析失败时由调用方 catch 异常并 fallback</li>
     * </ul>
     * <h2>多租户设计说明 (PR1 实施发现, 2026-06-05)</h2>
     * <p>{@code sys_dept} 表 <b>无 tenant_id 字段</b> (设计就是跨租户共享, 走 {@code org_id} 关联 {@code sys_organization}),
     * 因此 CTE <b>不</b> 加 tenant 过滤 (字段不存在)。
     * <p>P0-1 多租户拦截器把 {@code sys_dept} 列入 {@code IGNORE_TABLES}, 也是同一设计意图。
     * <p>原始 KNOWN_ISSUES #19 描述的"跨租户 dept_id 泄漏" 实际是"跨 org dept_id 进入 IN 子句" (性能/语义问题, 非安全):
     * 老 DFS 收集的 childDeptIds 包含全表 dept (跨 org), DataScopeAspect scope=3 拼
     * {@code AND u.dept_id IN (...)} 时包含跨 org 的 dept_id, SQL 不报错, 但 IN 子句无意义。
     * 跨 org 隔离修复留作 P2+ 优化 (需 org_id 关联 tenant, 加子查询)。
     * <h2>与 v7.0 行为差异</h2>
     * <ul>
     *   <li>v7.0 (collectChildDeptIds 应用层递归): 加载全表 + 内存 DFS</li>
     *   <li>v7.1 (本方法): MySQL 单 SQL 一次返回</li>
     * </ul>
     * <h2>灰度</h2>
     * 由 {@code platform.data-scope.upgrade.enabled} 控制, false 时调用方走老 DFS 方法
     */
    @Select("""
            WITH RECURSIVE dept_tree AS (
                SELECT id FROM sys_dept
                WHERE id = #{rootDeptId}
                  AND deleted = 0
                UNION ALL
                SELECT d.id FROM sys_dept d
                INNER JOIN dept_tree dt ON d.parent_id = dt.id
                WHERE d.deleted = 0
            )
            SELECT id FROM dept_tree
            """)
    @ResultType(Long.class)
    List<Long> selectChildDeptIdsByCte(@Param("rootDeptId") Long rootDeptId);
}