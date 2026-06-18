package com.cloudhub.platform.user.tenant;

import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.user.domain.entity.Dept;
import com.cloudhub.platform.user.domain.entity.Post;
import com.cloudhub.platform.user.domain.entity.User;
import com.cloudhub.platform.user.domain.service.DeptService;
import com.cloudhub.platform.user.domain.service.PostService;
import com.cloudhub.platform.user.mapper.UserMapper;
import com.cloudhub.platform.user.service.UserService;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M5 P0-2 PR4 D+8 写方法 @DataScope 集成测试 (2026-06-18)
 *
 * <p>配套: doc/M5-P0-2-实施子任务.md + doc/M5-P0-2-决策记录.md
 *
 * <h2>覆盖</h2>
 * <ul>
 *   <li>TC-DS-W01: UserService.update scope=4 (本人) 时, 越权改他人 → 0 行 (被 @DataScope 拦截)</li>
 *   <li>TC-DS-W02: UserService.update scope=4 (本人) 时, 改自己 → 1 行 (正常)</li>
 *   <li>TC-DS-W03: UserService.delete scope=4 (本人) 时, 越权删他人 → 0 行 (被 @DataScope 拦截)</li>
 *   <li>TC-DS-W04: PostService.update scope=4 (本人, deptAlias="dept_id" 走 userAlias="id") 时, 越权改他人岗位 → 0 行</li>
 *   <li>TC-DS-W05: DeptService.update scope=4 (本人, deptAlias="id") 时, 越权改他人部门 → 0 行</li>
 *   <li>TC-DS-W06: UserService.changePassword scope=4 (本人) 时, 改他人密码 → 0 行</li>
 * </ul>
 *
 * <h2>原理</h2>
 * <p>写方法上加 @DataScope → DataScopeAspect 触发 → DataScopeContextHolder 写 fragment
 * → DataScopeInnerInterceptor 在 UPDATE/DELETE 时把 fragment 拼到 WHERE 末尾
 * → 例如 scope=4 时 SQL: {@code UPDATE sys_user SET ... WHERE id=? AND (id = 101)}</p>
 * <p>通过对比 "调用 service 后影响行数" 来验证 @DataScope 是否生效: 越权调用应返回 0 行</p>
 *
 * <h2>排除情况 (M5.5+ 已知缺口)</h2>
 * <ul>
 *   <li>UserService.create/assignRoles/assignUserMenus - INSERT 不受拦截器处理 (DataScopeInnerInterceptor §5.2)</li>
 *   <li>RoleService.* - sys_role 无 dept_id/create_by 字段, 加 @DataScope 不适用 (M5.5+ gap #4)</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("M5 P0-2 PR4 D+8 写方法 @DataScope 集成测试 (6 TC)")
@Sql(scripts = {
    "/sql/tenant-test-schema.sql",
    "/sql/tenant-test-data.sql",
    "/sql/datascope-test.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DataScopeWriteMethodTest {

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private DeptService deptService;

    @Autowired
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        // P0-1 多租户拦截器要求设置 tenantId
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
    }

    @Test
    @Order(1)
    @DisplayName("TC-DS-W01: UserService.update scope=4, 越权改他人 nickname → 0 行 (被拦截)")
    void w01_userUpdate_otherUser_blocked() {
        // 模拟 user1 (id=101) 登录, scope=4 (本人) — datascope-test.sql 配置
        TenantContextHolder.setUserId(101L);

        // 改 user 102 (他人) 的 nickname — 应被 @DataScope 拦截
        // 拦截效果: 内部 selectById(102) 也被 scope=4 过滤 → 返回 null → 抛 BizException("用户不存在")
        Map<String, Object> params = new HashMap<>();
        params.put("id", 102L);
        params.put("nickname", "HACKED_BY_101");
        assertThrows(com.cloudhub.platform.common.exception.BizException.class,
                () -> userService.update(params),
                "scope=4 越权改他人, 应被 @DataScope 拦截抛 BizException");

        // 验证: user 102 的 nickname 未变
        User u = userMapper.selectById(102L);
        assertNotNull(u, "user 102 应存在");
        assertNotEquals("HACKED_BY_101", u.getNickname(),
                "scope=4 越权改他人 nickname 应被 @DataScope 拦截, nickname 未变");
    }

    @Test
    @Order(2)
    @DisplayName("TC-DS-W02: UserService.update scope=4, 改自己 nickname → 1 行 (正常)")
    void w02_userUpdate_self_allowed() {
        TenantContextHolder.setUserId(101L);

        Map<String, Object> params = new HashMap<>();
        params.put("id", 101L);
        params.put("nickname", "MY_NEW_NAME");
        userService.update(params);

        User u = userMapper.selectById(101L);
        assertNotNull(u, "user 101 应存在");
        assertEquals("MY_NEW_NAME", u.getNickname(),
                "scope=4 改自己 nickname 应成功");
    }

    @Test
    @Order(3)
    @DisplayName("TC-DS-W03: UserService.delete scope=4, 越权删他人 → user 仍存在 (被拦截)")
    void w03_userDelete_otherUser_blocked() {
        TenantContextHolder.setUserId(101L);

        // 删 user 102 (他人) — 应被 @DataScope 拦截
        // 拦截效果: 内部 selectById(102) 被过滤 → null → 抛 BizException
        assertThrows(com.cloudhub.platform.common.exception.BizException.class,
                () -> userService.delete(102L),
                "scope=4 越权删他人, 应被 @DataScope 拦截抛 BizException");

        // 验证: user 102 仍存在
        User u = userMapper.selectById(102L);
        assertNotNull(u, "scope=4 越权删他人, user 102 应仍存在 (被 @DataScope 拦截)");
    }

    @Test
    @Order(4)
    @DisplayName("TC-DS-W04: PostService.update scope=4, 越权改他人岗位 → 0 行 (被拦截)")
    void w04_postUpdate_otherPost_blocked() {
        TenantContextHolder.setUserId(101L);

        // 改 post 102 (id=102, dept_id=102) — deptAlias="dept_id", userAlias="id" (默认)
        // scope=4 → fragment = "(id = 101)"
        // SQL: UPDATE sys_post SET name=? WHERE id=? AND (id = 101) → 0 行
        Post p = new Post();
        p.setId(102L);
        p.setOrgId(1L);
        p.setDeptId(102L);
        p.setName("HACKED_POST");
        p.setCode("HACKED");
        p.setStatus(1);
        postService.update(p);

        Post stored = postService.getById(102L);
        assertNotNull(stored, "post 102 应仍存在");
        assertNotEquals("HACKED_POST", stored.getName(),
                "scope=4 越权改他人 post, name 未变 (被 @DataScope 拦截)");
    }

    @Test
    @Order(5)
    @DisplayName("TC-DS-W05: DeptService.update scope=4, 越权改他人部门 → 0 行 (被拦截)")
    void w05_deptUpdate_otherDept_blocked() {
        TenantContextHolder.setUserId(101L);

        // 改 dept 102 (id=102) — deptAlias="id"
        // scope=4 → fragment = "(id = 101)"
        // SQL: UPDATE sys_dept SET name=? WHERE id=? AND (id = 101) → 0 行
        Dept d = new Dept();
        d.setId(102L);
        d.setOrgId(1L);
        d.setName("HACKED_DEPT");
        d.setCode("HACKED");
        d.setStatus(1);
        deptService.update(d);

        Dept stored = deptService.getById(102L);
        assertNotNull(stored, "dept 102 应仍存在");
        assertNotEquals("HACKED_DEPT", stored.getName(),
                "scope=4 越权改他人 dept, name 未变 (被 @DataScope 拦截)");
    }

    @Test
    @Order(6)
    @DisplayName("TC-DS-W06: UserService.changePassword scope=4, 越权改他人密码 → 密码未变 (被拦截)")
    void w06_changePassword_otherUser_blocked() {
        TenantContextHolder.setUserId(101L);

        // 取 user 102 当前密码
        User before = userMapper.selectById(102L);
        String oldPwd = before.getPassword();

        // 改 user 102 密码 — 应被 @DataScope 拦截
        // 拦截效果: 内部 selectById(102) 被过滤 → null → 抛 BizException
        assertThrows(com.cloudhub.platform.common.exception.BizException.class,
                () -> userService.changePassword(102L, "anything", "new_pwd_hacked"),
                "scope=4 越权改他人密码, 应被 @DataScope 拦截抛 BizException");

        // 验证: user 102 密码未变
        User after = userMapper.selectById(102L);
        assertNotNull(after, "user 102 应仍存在");
        assertEquals(oldPwd, after.getPassword(),
                "scope=4 越权改他人密码, 密码未变 (被 @DataScope 拦截)");
    }
}
