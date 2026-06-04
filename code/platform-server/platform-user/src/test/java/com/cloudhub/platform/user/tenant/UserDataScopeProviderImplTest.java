package com.cloudhub.platform.user.tenant;

import com.cloudhub.platform.common.config.DataScopeContext;
import com.cloudhub.platform.user.domain.entity.Role;
import com.cloudhub.platform.user.domain.entity.User;
import com.cloudhub.platform.user.mapper.RoleMapper;
import com.cloudhub.platform.user.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * UserDataScopeProviderImpl 单元测试 (M5 P0-2 实施)
 *
 * 配套: doc/M5-P0-2-实施子任务.md
 *
 * 覆盖:
 * - TC-DS-01: 角色表空 → dataScope=1 (全部)
 * - TC-DS-02: 多角色 → 取 max (决策 1: 取最严格)
 * - TC-DS-03: scope=5 → 解析 custom_dept_ids
 * - TC-DS-04: user 不存在 → fallback to all
 * - TC-DS-05: 角色表查询失败 → fallback to none
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("M5 P0-2 DataScope Provider 测试 (5 TC)")
class UserDataScopeProviderImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private UserDataScopeProviderImpl provider;

    @Test
    @DisplayName("TC-DS-01: 角色表空 → dataScope=1 (全部)")
    void tc01_noRoles_returnsAll() {
        when(roleMapper.selectRolesByUserId(1L)).thenReturn(List.of());

        DataScopeContext ctx = provider.getContext(1L);

        assertNotNull(ctx);
        assertEquals(1, ctx.getMaxDataScope(), "无角色应默认为 scope=1 (全部)");
        assertNull(ctx.getUserDeptId());
        assertNull(ctx.getCustomDeptIds());
    }

    @Test
    @DisplayName("TC-DS-02: 多角色 → 取 max (决策 1: 取最严格)")
    void tc02_multiRoles_takesMaxScope() {
        Role r1 = new Role();
        r1.setId(1L);
        r1.setDataScope(1); // 全部
        r1.setCode("R1");

        Role r2 = new Role();
        r2.setId(2L);
        r2.setDataScope(4); // 本人 (最严格)
        r2.setCode("R2");

        Role r3 = new Role();
        r3.setId(3L);
        r3.setDataScope(2); // 本部门
        r3.setCode("R3");

        when(roleMapper.selectRolesByUserId(1L)).thenReturn(List.of(r1, r2, r3));

        DataScopeContext ctx = provider.getContext(1L);

        assertEquals(4, ctx.getMaxDataScope(), "多角色应取 max (决策 1: 取最严格) = 4 (本人)");
    }

    @Test
    @DisplayName("TC-DS-03: scope=5 → 解析 custom_dept_ids")
    void tc03_customScope_parsesCustomDeptIds() {
        Role r1 = new Role();
        r1.setId(1L);
        r1.setDataScope(5); // 自定义
        r1.setCustomDeptIds("101,102,103");

        when(roleMapper.selectRolesByUserId(1L)).thenReturn(List.of(r1));

        DataScopeContext ctx = provider.getContext(1L);

        assertEquals(5, ctx.getMaxDataScope());
        assertEquals("101,102,103", ctx.getCustomDeptIds());
    }

    @Test
    @DisplayName("TC-DS-04: user 不存在 → fallback to all")
    void tc04_userNotFound_returnsAll() {
        when(roleMapper.selectRolesByUserId(999L)).thenReturn(List.of());
        // userMapper 不 mock, 返回 null (模拟 user 不存在)

        DataScopeContext ctx = provider.getContext(999L);

        assertEquals(1, ctx.getMaxDataScope(), "user 不存在时退化为全部");
    }

    @Test
    @DisplayName("TC-DS-05: 角色表查询异常 → fallback to none (无限制)")
    void tc05_roleQueryFails_returnsNone() {
        when(roleMapper.selectRolesByUserId(1L))
                .thenThrow(new RuntimeException("DB 异常"));

        DataScopeContext ctx = provider.getContext(1L);

        assertNotNull(ctx, "异常时仍应返回 Context");
        assertEquals(1, ctx.getMaxDataScope(), "异常 fallback 为全部");
    }
}
