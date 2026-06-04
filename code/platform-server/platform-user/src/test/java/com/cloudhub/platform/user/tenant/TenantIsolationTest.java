package com.cloudhub.platform.user.tenant;

import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.user.domain.entity.Config;
import com.cloudhub.platform.user.domain.entity.DictType;
import com.cloudhub.platform.user.domain.entity.Organization;
import com.cloudhub.platform.user.domain.entity.Role;
import com.cloudhub.platform.user.domain.entity.User;
import com.cloudhub.platform.user.mapper.ConfigMapper;
import com.cloudhub.platform.user.mapper.DictTypeMapper;
import com.cloudhub.platform.user.mapper.MenuMapper;
import com.cloudhub.platform.user.mapper.OrganizationMapper;
import com.cloudhub.platform.user.mapper.RoleMapper;
import com.cloudhub.platform.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P0-1 MyBatis-Plus 多租户拦截器集成测试 (8 TC)
 *
 * 配套文档: doc/P0-1-集成测试checklist.md
 * 配套实施: doc/P0-1-M4-完成度审计.md
 *
 * <h2>测试策略</h2>
 * <ul>
 *   <li>使用 @SpringBootTest 启动真实 Spring 容器 (含 MybatisPlusConfig + TenantLineInnerInterceptor)</li>
 *   <li>使用 H2 内存数据库 (application-test.yml 配置)</li>
 *   <li>通过 TenantContextHolder.setTenantId 模拟不同租户登录</li>
 *   <li>通过 @Sql 加载 schema + 2 租户测试数据</li>
 * </ul>
 *
 * <h2>关键约束</h2>
 * <ul>
 *   <li>User.tenantId 字段类型是 Integer (历史遗留, 应统一为 Long)</li>
 *   <li>Role.tenantId / Organization.tenantId 是 Long</li>
 *   <li>DictType.tenantId / Config.tenantId 是 Integer (历史遗留)</li>
 *   <li>TenantContextHolder 是 Long (与 LongValue 拦截器匹配)</li>
 *   <li>SQL 自动类型转换兼容 H2</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("P0-1 多租户隔离测试 (8 TC)")
@Sql(scripts = {
    "/sql/tenant-test-schema.sql",
    "/sql/tenant-test-data.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TenantIsolationTest {

    @Autowired private UserMapper userMapper;
    @Autowired private RoleMapper roleMapper;
    @Autowired private OrganizationMapper orgMapper;
    @Autowired private DictTypeMapper dictTypeMapper;
    @Autowired private ConfigMapper configMapper;
    @Autowired private MenuMapper menuMapper;

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
    }

    // ============================================
    // TC-01 ~ TC-06: 业务表跨租户隔离
    // ============================================

    @Test
    @Order(1)
    @DisplayName("TC-01: 租户 1 登录后只看到本租户用户 (期望: 2 条, 全部 tenant_id=1)")
    void tc01_sameTenantUserVisible() {
        TenantContextHolder.setTenantId(1L);
        List<User> users = userMapper.selectList(null);
        assertEquals(2, users.size(), "租户 1 应只看到 2 个用户");
        assertTrue(users.stream().allMatch(u -> u.getTenantId().intValue() == 1),
                "所有用户 tenantId 应为 1");
    }

    @Test
    @Order(2)
    @DisplayName("TC-02: 租户 1 登录后看不到租户 2 用户 (核心安全测试)")
    void tc02_crossTenantUserInvisible() {
        TenantContextHolder.setTenantId(1L);
        List<User> users = userMapper.selectList(null);
        // 断言: 看不到任何 tenant_id=2 的数据
        assertTrue(users.stream().noneMatch(u -> u.getTenantId().intValue() == 2),
                "不应看到租户 2 的用户");
        // 断言: 看不到 id=201 或 202
        assertTrue(users.stream().noneMatch(u -> u.getId() == 201L || u.getId() == 202L),
                "不应看到 id=201 或 202 (租户 2 的用户)");
    }

    @Test
    @Order(3)
    @DisplayName("TC-03: sys_role 跨租户隔离")
    void tc03_crossTenantRoleInvisible() {
        TenantContextHolder.setTenantId(1L);
        List<Role> roles = roleMapper.selectList(null);
        assertTrue(roles.stream().noneMatch(r -> r.getTenantId() != null && r.getTenantId() == 2L),
                "不应看到租户 2 的角色");
        // 期望: 只看到 T1_ADMIN (id=101, tenant_id=1)
        assertEquals(1, roles.size(), "租户 1 应只看到 1 个角色");
        assertEquals(101L, roles.get(0).getId());
    }

    @Test
    @Order(4)
    @DisplayName("TC-04: sys_organization 跨租户隔离")
    void tc04_crossTenantOrgInvisible() {
        TenantContextHolder.setTenantId(1L);
        List<Organization> orgs = orgMapper.selectList(null);
        assertTrue(orgs.stream().noneMatch(o -> o.getTenantId() != null && o.getTenantId() == 2L),
                "不应看到租户 2 的组织");
        assertEquals(1, orgs.size(), "租户 1 应只看到 1 个组织");
    }

    @Test
    @Order(5)
    @DisplayName("TC-05: sys_dict_type 跨租户隔离")
    void tc05_crossTenantDictInvisible() {
        TenantContextHolder.setTenantId(1L);
        List<DictType> dicts = dictTypeMapper.selectList(null);
        assertTrue(dicts.stream().noneMatch(d -> d.getTenantId() != null && d.getTenantId().intValue() == 2),
                "不应看到租户 2 的字典");
        assertEquals(1, dicts.size(), "租户 1 应只看到 1 个字典");
    }

    @Test
    @Order(6)
    @DisplayName("TC-06: sys_config 跨租户隔离")
    void tc06_crossTenantConfigInvisible() {
        TenantContextHolder.setTenantId(1L);
        List<Config> configs = configMapper.selectList(null);
        assertTrue(configs.stream().noneMatch(c -> c.getTenantId() != null && c.getTenantId().intValue() == 2),
                "不应看到租户 2 的配置");
        assertEquals(1, configs.size(), "租户 1 应只看到 1 个配置");
    }

    // ============================================
    // TC-07: 系统表不受租户过滤
    // ============================================

    @Test
    @Order(7)
    @DisplayName("TC-07: sys_menu 不受租户过滤 (IGNORE_TABLES)")
    void tc07_systemTableNotFiltered() {
        // 租户 1 登录
        TenantContextHolder.setTenantId(1L);
        long t1MenuCount = menuMapper.selectList(null).size();

        // 切到租户 2 登录
        TenantContextHolder.setTenantId(2L);
        long t2MenuCount = menuMapper.selectList(null).size();

        // 期望: 看到的菜单数量完全一致
        assertEquals(t1MenuCount, t2MenuCount, "租户 1 和租户 2 看到的菜单数量应一致");
        assertEquals(2, t1MenuCount, "应看到 2 个菜单 (平台级)");
    }

    // ============================================
    // TC-08: 启动期越权自检 (方案 C: null → 9999)
    // ============================================

    @Test
    @Order(8)
    @DisplayName("TC-08: 无租户上下文时使用 9999, sys_user 9999 应返回 0 行 (主规划 §5.1.4)")
    void tc08_unauthorizedTenant9999() {
        // 验证 1: 无租户上下文时, 拦截器使用 9999
        TenantContextHolder.clear();
        // 不设置租户上下文, 模拟"无主"访问
        List<User> users = userMapper.selectList(null);

        // 期望: 应返回 0 行 (因为 sys_user 中没有 tenant_id=9999 的数据)
        // 如果有数据, 说明存在 9999 租户的"无主数据", 违反主规划 §5.1.4 约束
        assertEquals(0, users.size(),
                "无租户上下文 (使用 9999) 时, sys_user 应返回 0 行 (无 9999 数据)");

        // 验证 2: 9999 租户应保持为空
        assertNotNull(users, "查询不应为 null");
    }

    // ============================================
    // 辅助方法
    // ============================================

    /**
     * 验证: 租户上下文对其他租户不可见
     */
    private void assertNoTenant2Data(List<?> list, String entityName) {
        assertNotNull(list, entityName + " 查询不应为 null");
        // 通用断言: 列表中不应含 tenant_id=2 的元素
        // (各实体类型不同, 具体断言在调用方)
    }
}
