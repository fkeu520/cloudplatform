package com.cloudhub.platform.user.tenant;

import com.cloudhub.platform.common.config.DataScopeContext;
import com.cloudhub.platform.common.config.GrayMatcher;
import com.cloudhub.platform.common.config.PlatformToggleProperties;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.user.domain.entity.Dept;
import com.cloudhub.platform.user.domain.entity.Role;
import com.cloudhub.platform.user.domain.entity.User;
import com.cloudhub.platform.user.domain.mapper.DeptMapper;
import com.cloudhub.platform.user.mapper.RoleMapper;
import com.cloudhub.platform.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserDataScopeProviderImpl 单元测试 (M5 P0-2 实施)
 *
 * 配套: doc/M5-P0-2-实施子任务.md · doc/项目进度.md v7.1 §7.1 (PR1 H2 mock 桩)
 *
 * 覆盖:
 * - TC-DS-01: 角色表空 → dataScope=1 (全部)
 * - TC-DS-02: 多角色 → 取 max (决策 1: 取最严格)
 * - TC-DS-03: scope=5 → 解析 custom_dept_ids
 * - TC-DS-04: user 不存在 → fallback to all
 * - TC-DS-05: 角色表查询失败 → fallback to none
 * - TC-DS-06: PR1 灰度=true → CTE 路径 (mock deptMapper.selectChildDeptIdsByCte)
 * - TC-DS-07: PR1 灰度=false → 老 DFS 路径 (mock deptMapper.selectList)
 * - TC-DS-08: PR1 跨租户 → CTE 传 tenantId 过滤 (admin tenantId=NULL 走全量)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("M5 P0-2 DataScope Provider 测试 (8 TC, 含 PR1 灰度分支)")
class UserDataScopeProviderImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private DeptMapper deptMapper;

    /**
     * gray-release-infrastructure PR2: PlatformToggleProperties 默认值 (upgrade.enabled=false)
     * 各 TC 通过 {@link #setUpgradeEnabled(boolean)} 切换
     */
    private PlatformToggleProperties toggleProperties;

    /** gray-release-infrastructure PR5: 维度灰度匹配器 (依赖 toggleProperties) */
    private GrayMatcher grayMatcher;

    /** gray-release-infrastructure PR2: 手工 new (避免 @InjectMocks 把 toggleProperties 注入成 null) */
    private UserDataScopeProviderImpl provider;

    @BeforeEach
    void setUp() {
        toggleProperties = new PlatformToggleProperties();
        grayMatcher = new GrayMatcher(toggleProperties);
        provider = new UserDataScopeProviderImpl(userMapper, roleMapper, deptMapper, toggleProperties, grayMatcher);
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
        // 重置灰度开关为默认值, 防止 TC 间状态泄漏
        toggleProperties.getDataScope().getUpgrade().setEnabled(false);
    }

    /** gray-release-infrastructure PR2: 通过 PlatformToggleProperties 切换灰度 */
    private void setUpgradeEnabled(boolean enabled) {
        toggleProperties.getDataScope().getUpgrade().setEnabled(enabled);
    }

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

    // ============================================
    // PR1 灰度分支测试 (3 TC, 2026-06-05)
    // 配套: doc/项目进度.md v7.1 §7.1 H2 mock 桩 + §7.2 灰度开关
    // 灰度: upgradeEnabled=true → CTE; false → 老 DFS
    // ============================================

    @Test
    @DisplayName("TC-DS-06 (PR1): 灰度=true + scope=3 → 调 CTE, 不调老 DFS")
    void tc06_upgradeEnabled_callsCte_notRecursive() {
        // 1. 灰度开关 true
        setUpgradeEnabled(true);

        // 2. 准备数据: user 101 (dept=100) + role scope=3
        TenantContextHolder.setTenantId(1L);
        Role r1 = new Role();
        r1.setId(1L);
        r1.setDataScope(3); // 本部门及下级
        r1.setCode("R3");
        when(roleMapper.selectRolesByUserId(101L)).thenReturn(List.of(r1));

        User u = new User();
        u.setId(101L);
        u.setDeptId(100L);
        when(userMapper.selectById(101L)).thenReturn(u);

        // 3. mock CTE 返回: 100 + 101 + 102 (本部门 + 子部门)
        //    PR1 实施发现: sys_dept 无 tenant_id 字段, CTE 不加 tenant 过滤 (PR1 修正)
        when(deptMapper.selectChildDeptIdsByCte(100L))
                .thenReturn(List.of(100L, 101L, 102L));

        // 4. 调用
        DataScopeContext ctx = provider.getContext(101L);

        // 5. 断言: 调了 CTE, 没调老 DFS
        assertEquals(3, ctx.getMaxDataScope());
        assertEquals(100L, ctx.getUserDeptId());
        assertEquals("100,101,102", ctx.getChildDeptIds(),
                "CTE 路径应返回本部门 + 子部门 ID 列表");
        verify(deptMapper).selectChildDeptIdsByCte(100L);
        verify(deptMapper, never()).selectList(any());  // 老 DFS 不调
    }

    @Test
    @DisplayName("TC-DS-07 (PR1): 灰度=false + scope=3 → 调老 DFS, 不调 CTE")
    void tc07_upgradeDisabled_callsRecursive_notCte() {
        // 1. 灰度开关 false (PR1 部署初始状态, 走 v7.0 行为)
        setUpgradeEnabled(false);

        // 2. 准备数据
        TenantContextHolder.setTenantId(1L);
        Role r1 = new Role();
        r1.setId(1L);
        r1.setDataScope(3);
        r1.setCode("R3");
        when(roleMapper.selectRolesByUserId(101L)).thenReturn(List.of(r1));

        User u = new User();
        u.setId(101L);
        u.setDeptId(100L);
        when(userMapper.selectById(101L)).thenReturn(u);

        // 3. mock 老 DFS 返回: 加载全表, 含 100/101/102
        List<Dept> allDepts = new ArrayList<>();
        for (long id : new long[]{100L, 101L, 102L, 200L, 201L}) {
            Dept d = new Dept();
            d.setId(id);
            d.setParentId(id == 100L || id == 200L ? 0L
                    : (id == 101L || id == 201L ? (id == 101L ? 100L : 200L) : (id == 102L ? 101L : 201L)));
            d.setName("dept-" + id);
            d.setDeleted(0);
            allDepts.add(d);
        }
        when(deptMapper.selectList(null)).thenReturn(allDepts);

        // 4. 调用
        DataScopeContext ctx = provider.getContext(101L);

        // 5. 断言: 调了老 DFS, 没调 CTE
        assertEquals(3, ctx.getMaxDataScope());
        assertEquals("100,101,102", ctx.getChildDeptIds(),
                "老 DFS 应返回 dept 100 子树 (100/101/102), 不含 tenant 2 的 200/201");
        verify(deptMapper).selectList(null);  // 老 DFS 调 selectList(null)
        verify(deptMapper, never()).selectChildDeptIdsByCte(anyLong());  // CTE 不调
    }

    @Test
    @DisplayName("TC-DS-08 (PR1): admin (tenantId=NULL) + 灰度=true → CTE 走全量 (无 tenant 过滤)")
    void tc08_upgradeEnabled_adminTenantNullCteReturnsAll() {
        // 1. 灰度开关 true
        setUpgradeEnabled(true);

        // 2. admin 场景: tenantId=NULL (不设置, 模拟 admin)
        TenantContextHolder.clear();  // tenantId = null

        // 3. 准备数据
        Role r1 = new Role();
        r1.setId(1L);
        r1.setDataScope(3);
        r1.setCode("R3");
        when(roleMapper.selectRolesByUserId(1L)).thenReturn(List.of(r1));

        User u = new User();
        u.setId(1L);
        u.setDeptId(100L);
        when(userMapper.selectById(1L)).thenReturn(u);

        // 4. mock CTE 返回全量 (PR1 修正: sys_dept 无 tenant_id, CTE 不传 tenantId)
        when(deptMapper.selectChildDeptIdsByCte(100L))
                .thenReturn(List.of(100L, 101L, 102L, 200L, 201L, 202L));

        // 5. 调用
        DataScopeContext ctx = provider.getContext(1L);

        // 6. 断言: CTE 走全量 (admin 场景, sys_dept 设计就是跨租户共享)
        assertEquals("100,101,102,200,201,202", ctx.getChildDeptIds(),
                "admin 走全量, 跨 org/跨租户 dept 都返回 (sys_dept 无 tenant 字段)");
        verify(deptMapper).selectChildDeptIdsByCte(100L);
    }
}
