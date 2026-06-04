package com.cloudhub.platform.user.tenant;

import com.cloudhub.platform.common.config.DataScopeContext;
import com.cloudhub.platform.common.config.DataScopeProvider;
import com.cloudhub.platform.user.domain.entity.Role;
import com.cloudhub.platform.user.domain.entity.User;
import com.cloudhub.platform.user.mapper.RoleMapper;
import com.cloudhub.platform.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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

        log.debug("DataScopeProvider: userId={}, maxScope={}, userDeptId={}, customDeptIds={}",
                userId, maxDataScope, userDeptId, customDeptIds);

        return DataScopeContext.builder()
                .maxDataScope(maxDataScope)
                .userDeptId(userDeptId)
                .customDeptIds(customDeptIds)
                .build();
    }
}
