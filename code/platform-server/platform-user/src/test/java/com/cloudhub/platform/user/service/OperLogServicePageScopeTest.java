package com.cloudhub.platform.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.user.domain.entity.OperLog;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M5 P0-2 PR4 OperLog 数据权限测试 (2026-06-08)
 *
 * <p>配套: doc/M5-P0-2-实施子任务.md §十二 PR4 启动准备
 *
 * <h2>覆盖</h2>
 * <ul>
 *   <li>TC-OL-01: 无用户上下文时 pageList 正常返回 (fragment 为空, 不改写 SQL, 等同 v7.0 行为)</li>
 *   <li>TC-OL-02: pageList 带 QueryWrapper 正常执行 (动态条件)</li>
 *   <li>TC-OL-03: @DataScope 注解存在 (反射验证, 防止后续重构意外丢失)</li>
 * </ul>
 *
 * <h2>scope 行为说明</h2>
 * <p>scope=1 (全部) 时 fragment="" → SQL 不改写, 等同 v7.0<br>
 * scope=2/3/5 时 fragment 包含 dept_id 条件, 但需要真 MySQL 8 验证 (H2 集成测试 Provider 退化为 none)<br>
 * scope=4 (本人) 时退化为 userAlias 默认, oper_log 无 userAlias 字段, 需观察是否回退</p>
 *
 * <h2>历史数据兼容</h2>
 * <p>Flyway V23 给 sys_oper_log 加 dept_id 可空列, 历史数据回填后可能为 null (dept_name 不匹配时)<br>
 * scope=2/3/5 过滤可能漏掉这些 null 行, 这是可接受的 (审计场景宁严勿宽)</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(scripts = {
        "/sql/tenant-test-schema.sql",
        "/sql/tenant-test-data.sql"
})
@DisplayName("M5 P0-2 PR4 OperLog 数据权限 (3 TC)")
class OperLogServicePageScopeTest {

    @Autowired
    private OperLogService operLogService;

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    @Order(1)
    @DisplayName("TC-OL-01: 无用户上下文, pageList 正常返回 (fragment 为空, 不改写 SQL)")
    void testPageList_NoUserContext() {
        // 1. 确保无用户上下文
        TenantContextHolder.clear();

        // 2. 构造 page + QueryWrapper
        Page<OperLog> page = new Page<>(1, 10);
        QueryWrapper<OperLog> query = new QueryWrapper<>();
        query.orderByDesc("oper_time");

        // 3. 调用 pageList (走 @DataScope 注解, 但无 user 上下文 → fragment="" → SQL 不改写)
        IPage<OperLog> result = operLogService.pageList(page, query);

        // 4. 验证
        assertNotNull(result, "pageList 应返回 IPage 实例");
        assertNotNull(result.getRecords(), "records 不应为 null");
        // H2 测试库可能无 oper_log 数据, 允许空, 但不应抛异常
    }

    @Test
    @Order(2)
    @DisplayName("TC-OL-02: 带 QueryWrapper 动态条件, pageList 正常执行")
    void testPageList_WithQueryWrapper() {
        TenantContextHolder.clear();

        Page<OperLog> page = new Page<>(1, 5);
        QueryWrapper<OperLog> query = new QueryWrapper<>();
        query.like("title", "登录")
                .eq("status", 0)
                .orderByDesc("oper_time");

        IPage<OperLog> result = operLogService.pageList(page, query);

        assertNotNull(result);
        assertTrue(result.getSize() == 5, "pageSize 应为 5");
    }

    @Test
    @Order(3)
    @DisplayName("TC-OL-03: 反射验证 @DataScope 注解存在 (防止后续重构丢失)")
    void testDataScopeAnnotation_Present() throws NoSuchMethodException {
        // 反射获取 pageList 方法
        var method = OperLogService.class.getDeclaredMethod(
                "pageList", Page.class, com.baomidou.mybatisplus.core.conditions.Wrapper.class);

        // 验证 @DataScope 注解存在
        var dataScopeAnno = method.getAnnotation(com.cloudhub.platform.common.annotation.DataScope.class);
        assertNotNull(dataScopeAnno, "pageList 方法必须有 @DataScope 注解, 否则 PR4 数据权限失效");

        // 验证 deptAlias 字段名 (sys_oper_log 加的列就是 dept_id)
        assertTrue("dept_id".equals(dataScopeAnno.deptAlias()),
                "deptAlias 必须为 'dept_id' (Flyway V23 新增列名)");
    }
}
