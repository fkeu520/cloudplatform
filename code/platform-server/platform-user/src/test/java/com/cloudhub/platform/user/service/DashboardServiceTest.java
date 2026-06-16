package com.cloudhub.platform.user.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DashboardService 单元测试 (W3 P1-3)
 *
 * <p>W3 阶段所有方法返回 Mock, 测试覆盖:
 * <ol>
 *   <li>welcome 返回 4 个 key, 数值类型正确</li>
 *   <li>todos / messages / shortcuts / businessStats 返回空列表 (占位)</li>
 *   <li>systemStats 返回 6 个字段</li>
 *   <li>userId=null 不抛异常</li>
 * </ol>
 * </p>
 */
class DashboardServiceTest {

    private final DashboardService service = new DashboardService();

    @Test
    void testWelcome_returnsFourKeys() {
        Map<String, Object> result = service.welcome(1L);

        assertNotNull(result);
        assertTrue(result.containsKey("todoCount"));
        assertTrue(result.containsKey("msgCount"));
        assertTrue(result.containsKey("myApplyCount"));
        assertTrue(result.containsKey("sysNoticeCount"));
        assertEquals(0, result.get("todoCount"));
        assertEquals(0, result.get("msgCount"));
    }

    @Test
    void testWelcome_nullUserId_noException() {
        // 不抛异常即可
        Map<String, Object> result = service.welcome(null);
        assertNotNull(result);
    }

    @Test
    void testTodos_returnsEmpty() {
        List<Map<String, Object>> result = service.todos(1L, 5);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testMessages_returnsEmpty() {
        List<Map<String, Object>> result = service.messages(1L, 5);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testShortcuts_returnsEmpty() {
        List<Map<String, Object>> result = service.shortcuts(1L, 1L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testShortcuts_nullAppId_returnsEmpty() {
        List<Map<String, Object>> result = service.shortcuts(1L, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testBusinessStats_nullAppCode_returnsEmpty() {
        List<Map<String, Object>> result = service.businessStats(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testBusinessStats_blankAppCode_returnsEmpty() {
        List<Map<String, Object>> result = service.businessStats("");
        assertTrue(result.isEmpty());
    }

    @Test
    void testBusinessStats_parkSpace_returnsEmpty() {
        // W3 阶段 park-* 业务数据留 W3+ 接入
        List<Map<String, Object>> result = service.businessStats("park-space");
        assertTrue(result.isEmpty());
    }

    @Test
    void testSystemStats_sixFields() {
        Map<String, Object> result = service.systemStats();

        assertNotNull(result);
        assertEquals(6, result.size());
        assertTrue(result.containsKey("cpuUsage"));
        assertTrue(result.containsKey("memoryUsage"));
        assertTrue(result.containsKey("diskUsage"));
        assertTrue(result.containsKey("containerCount"));
        assertTrue(result.containsKey("uptimeSeconds"));
        assertTrue(result.containsKey("source"));
        assertEquals("mock", result.get("source"));
    }
}