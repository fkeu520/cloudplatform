package com.cloudhub.platform.common.config;

/**
 * 数据权限 Provider 接口 (M5 P0-2 实施)
 * 配套: doc/M5-P0-2-实施子任务.md
 * 设计目的: common 模块的 DataScopeAspect 不能直接 import user 模块的 Mapper
 *          通过此接口, 由 user 模块 (或其他业务模块) 实现
 * 实现示例 (user 模块):
 *   {@code
 *   @Service
 *   public class UserDataScopeProviderImpl implements DataScopeProvider {
 *       @Override
 *       public DataScopeContext getContext(Long userId) {
 *           // 1. 查 user 的所有 role
 *           List<Role> roles = roleMapper.selectRolesByUserId(userId);
 *           // 2. 取 max data_scope
 *           int max = roles.stream()
 *               .map(Role::getDataScope)
 *               .filter(Objects::nonNull)
 *               .max(Integer::compare)
 *               .orElse(1);
 *           // 3. 查 user.dept_id (用于 scope=2/3)
 *           User user = userMapper.selectById(userId);
 *           Long deptId = user != null ? user.getDeptId() : null;
 *           // 4. 解析 custom_dept_ids (scope=5)
 *           String custom = roles.stream()
 *               .filter(r -> r.getDataScope() != null && r.getDataScope() == 5)
 *               .map(Role::getCustomDeptIds)
 *               .filter(Objects::nonNull)
 *               .filter(s -> !s.isEmpty())
 *               .findFirst()
 *               .orElse(null);
 *           return DataScopeContext.builder()
 *               .maxDataScope(max)
 *               .userDeptId(deptId)
 *               .customDeptIds(custom)
 *               .build();
 *       }
 *   }
 *   }
 */
public interface DataScopeProvider {

    /**
     * 查 user 的 data_scope 上下文
     * @param userId 当前用户 ID
     * @return DataScopeContext, 永远非 null (返回 DataScopeContext.none() 表示无限制)
     */
    DataScopeContext getContext(Long userId);
}
