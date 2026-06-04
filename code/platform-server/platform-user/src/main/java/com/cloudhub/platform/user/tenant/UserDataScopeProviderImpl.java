package com.cloudhub.platform.user.tenant;

import com.cloudhub.platform.common.config.DataScopeContext;
import com.cloudhub.platform.common.config.DataScopeProvider;
import com.cloudhub.platform.user.domain.entity.Dept;
import com.cloudhub.platform.user.domain.entity.Role;
import com.cloudhub.platform.user.domain.entity.User;
import com.cloudhub.platform.user.domain.mapper.DeptMapper;
import com.cloudhub.platform.user.mapper.RoleMapper;
import com.cloudhub.platform.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * User 模块 DataScopeProvider 实现 (M5 P0-2 实施)
 *
 * 配套: doc/M5-P0-2-实施子任务.md + doc/M5-P0-2-决策记录.md
 *
 * <h2>职责</h2>
 * 根据 userId 查 user 的所有 role, 取 max(data_scope) (决策 1: 取最严格)
 * 同时查 user.dept_id (用于 scope=2/3) 和 custom_dept_ids (用于 scope=5)
 *
 * <h2>数据流</h2>
 * <pre>
 *   DataScopeAspect → DataScopeProvider.getContext(userId)
 *     ↓
 *     1. roleMapper.selectRolesByUserId(userId) → List<Role>
 *     2. max(Role.dataScope) → maxDataScope
 *     3. userMapper.selectById(userId) → user.deptId
 *     4. role.customDeptIds (where dataScope=5) → customDeptIds
 *     5. return DataScopeContext
 * }</pre>
 *
 * <h2>异常处理</h2>
 * - user 不存在 → 返回 none() (无限制, 退化)
 * - 角色表空 → dataScope=1 (全部)
 * - dept_id 缺失 → scope=2/3 退化为 scope=4 (本人)
 *
 * @since 2026-06-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataScopeProviderImpl implements DataScopeProvider {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final DeptMapper deptMapper;

    @Override
    public DataScopeContext getContext(Long userId) {
        // 1. 查 user 的所有 role (含 data_scope + custom_dept_ids)
        List<Role> roles;
        try {
            roles = roleMapper.selectRolesByUserId(userId);
        } catch (Exception e) {
            log.warn("DataScopeProvider: 查 roles 失败, fallback to no-scope. userId={}", userId, e);
            return DataScopeContext.none();
        }

        if (roles == null || roles.isEmpty()) {
            log.debug("DataScopeProvider: userId={} 无角色, dataScope=1 (全部)", userId);
            return DataScopeContext.all();
        }

        // 2. 决策 1: 取最严格 (max data_scope)
        int maxDataScope = roles.stream()
                .map(Role::getDataScope)
                .filter(Objects::nonNull)
                .max(Integer::compare)
                .orElse(1); // 角色未配置 data_scope 时, 默认为 1 (全部)

        // 3. 查 user.dept_id (用于 scope=2/3)
        Long userDeptId = null;
        try {
            User user = userMapper.selectById(userId);
            if (user != null) {
                userDeptId = user.getDeptId();
            }
        } catch (Exception e) {
            log.warn("DataScopeProvider: 查 user 失败, userDeptId=null. userId={}", userId, e);
        }

        // 4. 解析 custom_dept_ids (scope=5)
        String customDeptIds = roles.stream()
                .filter(r -> r.getDataScope() != null && r.getDataScope() == 5)
                .map(Role::getCustomDeptIds)
                .filter(s -> s != null && !s.isEmpty())
                .findFirst()
                .orElse(null);

        // 5. 收集子部门 ID 列表 (scope=3, 决策 2: 应用层递归)
        //    生产环境可替换为 MySQL 8.0 递归 CTE (WITH RECURSIVE)
        String childDeptIds = null;
        if (maxDataScope == 3 && userDeptId != null) {
            childDeptIds = collectChildDeptIds(userDeptId);
        }

        log.debug("DataScopeProvider: userId={}, maxScope={}, userDeptId={}, customDeptIds={}, childDeptIds={}",
                userId, maxDataScope, userDeptId, customDeptIds, childDeptIds);

        return DataScopeContext.builder()
                .maxDataScope(maxDataScope)
                .userDeptId(userDeptId)
                .customDeptIds(customDeptIds)
                .childDeptIds(childDeptIds)
                .build();
    }

    // ============================================
    // 子部门收集 (scope=3)
    // 决策 2: A 取最严格 - 应用层递归 (生产可替换 MySQL CTE)
    // ============================================

    /**
     * 从 userDeptId 开始, 递归收集所有子部门 ID (含本部门)
     * 返回逗号分隔的 ID 字符串, 如 "100,101,102"
     */
    private String collectChildDeptIds(Long rootDeptId) {
        try {
            // 1. 加载所有部门 (数据量 < 1000, 单次查询)
            List<Dept> allDepts = deptMapper.selectList(null);
            if (allDepts == null || allDepts.isEmpty()) {
                return String.valueOf(rootDeptId);
            }

            // 2. 构建 parentId → children 映射
            Map<Long, List<Dept>> childrenMap = new HashMap<>();
            for (Dept d : allDepts) {
                if (d.getParentId() != null) {
                    childrenMap.computeIfAbsent(d.getParentId(), k -> new ArrayList<>()).add(d);
                }
            }

            // 3. DFS 收集所有后代
            Set<Long> collected = new HashSet<>();
            Queue<Long> queue = new LinkedList<>();
            queue.add(rootDeptId);

            while (!queue.isEmpty()) {
                Long current = queue.poll();
                if (collected.add(current)) {
                    List<Dept> children = childrenMap.get(current);
                    if (children != null) {
                        children.forEach(child -> queue.add(child.getId()));
                    }
                }
            }

            // 4. 排序 + 返回
            return collected.stream()
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

        } catch (Exception e) {
            log.warn("collectChildDeptIds failed, fallback to root only. rootDeptId={}", rootDeptId, e);
            return String.valueOf(rootDeptId);
        }
    }
}
