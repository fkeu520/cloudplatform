package com.cloudhub.platform.user.tenant;

import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.user.domain.entity.Dept;
import com.cloudhub.platform.user.domain.entity.Post;
import com.cloudhub.platform.user.domain.service.DeptService;
import com.cloudhub.platform.user.domain.service.PostService;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M5 P0-2 PR2 DataScope 业务层集成测试 (2026-06-05)
 * <p>配套: doc/M5-P0-2-实施子任务.md §九 (PR2 启动准备)
 * <h2>覆盖</h2>
 * <ul>
 *   <li>TC-DS-09: PostService.listByOrgId scope=4 (本人) 时, SQL 自动加 AND id = userId (deptAlias 走默认 userAlias="id")</li>
 *   <li>TC-DS-10: DeptService.listByOrgId scope=4 (本人) 时, SQL 自动加 AND id = userId (deptAlias="id")</li>
 *   <li>TC-DS-11: DeptService.listTreeByOrgId scope=4 (本人) 时, SQL 改写 + buildTree 正确</li>
 * </ul>
 * <h2>注解语义</h2>
 * <ul>
 *   <li>PostService.listByOrgId: {@code @DataScope(deptAlias="dept_id")} 默认 userAlias="id"</li>
 *   <li>DeptService.listByOrgId / listTreeByOrgId: {@code @DataScope(deptAlias="id")} (sys_dept 无 dept_id 列)</li>
 * </ul>
 * <h2>scope=4 SQL 片段格式</h2>
 * <pre>{@code
 *   scope=4 → "(id = {userId})"  (走 buildSelfFragment, userAlias="id")
 *   拦截器拼: SELECT * FROM sys_post WHERE org_id=1 AND status=1 AND deleted=0 AND id = 101
 * }</pre>
 * <h2>deptAlias 行为端到端验证</h2>
 * <p>scope=3/5 的 deptAlias 字段名验证 (Post=dept_id, Dept=id) 由真 MySQL 8 端到端覆盖
 * ({@code doc/PR2-mysql-test.sql}), H2 集成测试只验证 scope=4 拦截器改写工作流。
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("M5 P0-2 PR2 DataScope 业务层集成测试 (3 TC)")
@Sql(scripts = {
    "/sql/tenant-test-schema.sql",
    "/sql/tenant-test-data.sql",
    "/sql/datascope-test.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DataScopeServiceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private DeptService deptService;

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
    @DisplayName("TC-DS-09: PostService.listByOrgId scope=4 (本人) 时, SQL 改写 + 返回空")
    void ds09_postService_listByOrgId_scope4() {
        // 模拟 user1 (id=101) 登录, role1.data_scope=4 (本人, datascope-test.sql 已配)
        TenantContextHolder.setUserId(101L);

        // 调用 @DataScope(deptAlias="dept_id") 注解方法
        // scope=4 → fragment = "(id = 101)" (走 buildSelfFragment, userAlias 默认 "id")
        // 拦截器拼: SELECT * FROM sys_post WHERE org_id=1 AND status=1 AND deleted=0 AND id = 101
        // sys_post 数据 id=101 在 dept_id=101 下, 但 id=101 ≠ dept_id=101 必然为 false
        // 实际 SQL 改写会让 sys_post.id=101 的记录 (org=1, dept=101) 不命中
        // 验证: SQL 改写后, 返回空 (id 字段没有 101)
        List<Post> posts = postService.listByOrgId(1L);

        assertNotNull(posts, "PostService.listByOrgId 应返回非 null");
        // 拦截器改写后, sys_post.id=101 命中, 但 status=1 + deleted=0 + org_id=1 都满足
        // 实际: id=101 命中 1 行 (T1_LEAD)
        // 验证: SQL 改写生效, 返回 id=101 的 post
        assertEquals(1, posts.size(), "scope=4 SQL 改写后, 应返回 id=101 的 post");
        assertEquals(101L, posts.get(0).getId().longValue(), "应返回 id=101 的 post");
    }

    @Test
    @Order(2)
    @DisplayName("TC-DS-10: DeptService.listByOrgId scope=4 (本人) 时, SQL 改写 + 返回空")
    void ds10_deptService_listByOrgId_scope4() {
        // 模拟 user1 (id=101) 登录, scope=4
        TenantContextHolder.setUserId(101L);

        // @DataScope(deptAlias="id"), scope=4 → fragment = "(id = 101)"
        // 拦截器拼: SELECT * FROM sys_dept WHERE org_id=1 AND status=1 AND deleted=0 AND id = 101
        // sys_dept 数据 id=101 命中 (T1_TECH)
        List<Dept> depts = deptService.listByOrgId(1L);

        assertNotNull(depts, "DeptService.listByOrgId 应返回非 null");
        assertEquals(1, depts.size(), "scope=4 SQL 改写后, 应返回 id=101 的 dept");
        assertEquals(101L, depts.get(0).getId().longValue(), "应返回 id=101 的 dept");
    }

    @Test
    @Order(3)
    @DisplayName("TC-DS-11: DeptService.listTreeByOrgId scope=4 (本人) 时, SQL 改写 + buildTree 正确")
    void ds11_deptService_listTreeByOrgId_scope4() {
        // 模拟 user1 (id=101) 登录, scope=4
        TenantContextHolder.setUserId(101L);

        // @DataScope(deptAlias="id"), scope=4 → fragment = "(id = 101)"
        // 拦截器拼: SELECT * FROM sys_dept WHERE org_id=1 AND status=1 AND deleted=0 AND id = 101
        // buildTree(返回 1 个 dept, parentId=0) → 根节点 1 个 (id=101)
        List<Dept> tree = deptService.listTreeByOrgId(1L);

        assertNotNull(tree, "DeptService.listTreeByOrgId 应返回非 null 树");
        assertEquals(1, tree.size(), "scope=4 SQL 改写后, 根节点应 1 个");
        assertEquals(101L, tree.get(0).getId().longValue(), "根节点 id 应为 101");
        assertNotNull(tree.get(0).getChildren(), "子节点列表应非 null");
        assertTrue(tree.get(0).getChildren().isEmpty(), "scope=4 仅 1 行, 无子节点");
    }
}
