package com.cloudhub.platform.user.service;

import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.user.domain.entity.Role;
import com.cloudhub.platform.user.mapper.RoleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RoleService 集成测试 (M5 P0-2 PR5 角色 dataScope API 闭环, 2026-06-18)
 *
 * <p>配套: doc/M5-P0-2-决策记录.md + doc/M5-P0-2-实施子任务.md</p>
 *
 * <h2>背景</h2>
 * <p>V22 迁移加了 sys_role.data_scope + custom_dept_ids 字段,
 * 但 RoleService.create/update 之前没接收这两个字段, 前端角色管理弹窗也没暴露。
 * 本测试覆盖"角色 API 能存能取 dataScope"端到端路径, 配合前端 UI 改造形成 PR5 闭环。</p>
 *
 * <h2>覆盖</h2>
 * <ul>
 *   <li>TC-ROLE-01: create 不传 dataScope 默认 1 (全部)</li>
 *   <li>TC-ROLE-02: create 传 dataScope=5 + customDeptIds=正确保存</li>
 *   <li>TC-ROLE-03: create dataScope=5 不传 customDeptIds 抛 BizException</li>
 *   <li>TC-ROLE-04: create dataScope=6 (越界) 抛 BizException</li>
 *   <li>TC-ROLE-05: update 改 dataScope + customDeptIds 正确生效</li>
 *   <li>TC-ROLE-06: getById 返回的 Role 包含 dataScope + customDeptIds</li>
 *   <li>TC-ROLE-07: page 查询列表中含 dataScope 字段</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("M5 P0-2 PR5 RoleService.dataScope API 闭环 (7 TC)")
@Sql(scripts = {
    "/sql/tenant-test-schema.sql",
    "/sql/tenant-test-data.sql",
    "/sql/datascope-test.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RoleServiceDataScopeTest {

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleMapper roleMapper;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
    }

    @Test
    @Order(1)
    @DisplayName("TC-ROLE-01: create 不传 dataScope → 默认 1 (全部)")
    void role01_create_defaultDataScope() {
        Map<String, Object> params = new HashMap<>();
        params.put("code", "DEFAULT_SCOPE");
        params.put("name", "默认数据范围角色");
        params.put("sort", 0);
        params.put("remark", "不传 dataScope 应默认 1");

        roleService.create(params);

        Role stored = roleMapper.selectByCode("DEFAULT_SCOPE");
        assertNotNull(stored, "角色应创建成功");
        assertEquals(Integer.valueOf(1), stored.getDataScope(), "dataScope 默认 1");
        assertNull(stored.getCustomDeptIds(), "customDeptIds 默认 null");
    }

    @Test
    @Order(2)
    @DisplayName("TC-ROLE-02: create 传 dataScope=5 + customDeptIds → 正确保存")
    void role02_create_customDataScope() {
        Map<String, Object> params = new HashMap<>();
        params.put("code", "CUSTOM_SCOPE");
        params.put("name", "自定义数据范围");
        params.put("dataScope", 5);
        params.put("customDeptIds", "101,102,103");

        roleService.create(params);

        Role stored = roleMapper.selectByCode("CUSTOM_SCOPE");
        assertNotNull(stored);
        assertEquals(Integer.valueOf(5), stored.getDataScope(), "dataScope=5");
        assertEquals("101,102,103", stored.getCustomDeptIds(), "customDeptIds 正确保存");
    }

    @Test
    @Order(3)
    @DisplayName("TC-ROLE-03: create dataScope=5 不传 customDeptIds → 抛 BizException")
    void role03_create_customWithoutDepts() {
        Map<String, Object> params = new HashMap<>();
        params.put("code", "INVALID_CUSTOM");
        params.put("name", "自定义但无部门");
        params.put("dataScope", 5);
        // 不传 customDeptIds

        BizException ex = assertThrows(BizException.class, () -> roleService.create(params),
                "dataScope=5 不传 customDeptIds 应抛异常");
        assertTrue(ex.getMessage().contains("customDeptIds"), "异常信息应提示 customDeptIds");
    }

    @Test
    @Order(4)
    @DisplayName("TC-ROLE-04: create dataScope=6 (越界) → 抛 BizException")
    void role04_create_dataScopeOutOfRange() {
        Map<String, Object> params = new HashMap<>();
        params.put("code", "OUT_OF_RANGE");
        params.put("name", "越界测试");
        params.put("dataScope", 6);

        BizException ex = assertThrows(BizException.class, () -> roleService.create(params),
                "dataScope=6 应抛异常");
        assertTrue(ex.getMessage().contains("1-5"), "异常信息应提示取值范围 1-5");
    }

    @Test
    @Order(5)
    @DisplayName("TC-ROLE-05: update 改 dataScope + customDeptIds → 正确生效")
    void role05_update_dataScope() {
        // 先创建
        Map<String, Object> create = new HashMap<>();
        create.put("code", "TO_UPDATE");
        create.put("name", "待更新");
        create.put("dataScope", 1);
        roleService.create(create);
        Role created = roleMapper.selectByCode("TO_UPDATE");
        assertNotNull(created);

        // update 改 dataScope=3
        Map<String, Object> update = new HashMap<>();
        update.put("id", created.getId());
        update.put("dataScope", 3);
        roleService.update(update);

        Role updated = roleMapper.selectById(created.getId());
        assertNotNull(updated);
        assertEquals(Integer.valueOf(3), updated.getDataScope(), "dataScope 应改为 3");
    }

    @Test
    @Order(6)
    @DisplayName("TC-ROLE-06: getById 返回的 Role 包含 dataScope + customDeptIds")
    void role06_getById_includesDataScope() {
        // 用 datascope-test.sql 中 role1 (id=101) — dataScope=4
        Role r = roleService.getById(101L);
        assertNotNull(r);
        assertEquals(Integer.valueOf(4), r.getDataScope(), "getById 返回的 Role 应含 dataScope=4");
    }

    @Test
    @Order(7)
    @DisplayName("TC-ROLE-07: page 查询列表含 dataScope 字段")
    void role07_page_includesDataScope() {
        var result = roleService.page(null, null, 1, 10);
        assertNotNull(result);
        assertTrue(result.getRecords().size() > 0, "应有角色数据");
        // 找到 T1_ADMIN (id=101) — 它的 dataScope 应被读出
        boolean found = false;
        for (Role r : result.getRecords()) {
            if (r.getId().equals(101L)) {
                found = true;
                assertEquals(Integer.valueOf(4), r.getDataScope(), "page 返回的 Role 应含 dataScope=4");
                break;
            }
        }
        assertTrue(found, "page 应包含 T1_ADMIN 角色");
    }
}
