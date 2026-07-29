package com.cloudhub.platform.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台 Dashboard Service (W3 P1-3)
 *
 * <p>6 个方法对应 DashboardController 的 6 个端点.
 * W3 阶段所有方法返回 Mock 数据 (结构完整, 数据待 W3+ 接入).
 * W3+ 阶段替换方法实现即可, Controller 不变.</p>
 *
 * <p>未来接入规划:
 * <ul>
 *   <li>welcome: 聚合 workflow todo + message unread + my apply count + notice count</li>
 *   <li>todos: 调 workflow /api/workflow/task/todo (Feign)</li>
 *   <li>messages: 调 message /api/message/site/page (Feign)</li>
 *   <li>shortcuts: 复用 MenuService (W3 阶段已实现)</li>
 *   <li>business-stats: 按 appCode 路由 (platform 走 UserMapper, park-* 留空)</li>
 *   <li>system-stats: HTTP 调 container-exporter /metrics (W3+ 集成)</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    // W3 阶段: 暂不注入其他 Service (避免依赖, W3+ 注入)
    // 留作字段占位, W3+ 替换:
    // private final WorkflowFeignClient workflowClient;
    // private final MessageFeignClient messageClient;
    // private final MenuService menuService;

    /**
     * 顶部欢迎条 + 今日 4 数
     *
     * @param userId 用户ID
     * @return Map: {todoCount, msgCount, myApplyCount, sysNoticeCount}
     */
    public Map<String, Object> welcome(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todoCount", 0);
        result.put("msgCount", 0);
        result.put("myApplyCount", 0);
        result.put("sysNoticeCount", 0);
        // W3+ 阶段: 调 workflowClient.countUserTodo + messageClient.countUnread + noticeClient.countRecent
        return result;
    }

    /**
     * 我的待办 Top N (W3 占位, 返回空列表)
     */
    public List<Map<String, Object>> todos(Long userId, int limit) {
        // W3+ 阶段: return workflowClient.getUserTodo(userId, limit)
        return Collections.emptyList();
    }

    /**
     * 未读消息 Top N (W3 占位)
     */
    public List<Map<String, Object>> messages(Long userId, int limit) {
        // W3+ 阶段: return messageClient.getUnreadTop(userId, limit)
        return Collections.emptyList();
    }

    /**
     * 快捷入口 (扁平菜单, 按 appCode 过滤)
     *
     * <p>W3 阶段返回空列表, 因为:
     * <ol>
     *   <li>MenuService.getUserMenus 返回树形结构, 扁平化需要递归</li>
     *   <li>前端目前从 /menu/user 拿全量菜单, 不依赖本端点</li>
     *   <li>W3+ 阶段注入 MenuService, 递归拉扁平化菜单</li>
     * </ol>
     * </p>
     */
    public List<Map<String, Object>> shortcuts(Long userId, Long appId) {
        // W3+ 阶段:
        // List<Map<String, Object>> menuTree = menuService.getUserMenus(userId, appId);
        // return flattenMenuTree(menuTree, 0); // 递归扁平化, 限制深度
        return Collections.emptyList();
    }

    /**
     * 业务数据概览 (按 appCode 路由不同 Service)
     *
     * <p>W3 阶段:
     * <ul>
     *   <li>appCode=null 或 appCode='*': 返回空 (前端不显示)</li>
     *   <li>appCode='system' 或 'platform': 返回平台统计数据</li>
     *   <li>appCode='park-space' 等 park-*: 返回空 (W3+ park-* 上线后接入)</li>
     * </ul>
     * </p>
     */
    public List<Map<String, Object>> businessStats(String appCode) {
        if (appCode == null || appCode.isBlank()) {
            return Collections.emptyList();
        }
        // W3+ 阶段: 按 appCode 路由
        // if ("system".equals(appCode) || "platform".equals(appCode)) return platformStats();
        // if (appCode.startsWith("park-")) return parkStats(appCode); // 留空
        return Collections.emptyList();
    }

    /**
     * 系统资源监控 (CPU/内存/磁盘)
     *
     * <p>W3 阶段返回 Mock 数据. W3+ 阶段:
     * <ol>
     *   <li>HTTP GET http://192.168.0.142:9100/metrics (Prometheus node-exporter)</li>
     *   <li>解析 Prometheus text format</li>
     *   <li>提取 node_cpu_seconds_total, node_memory_MemAvailable_bytes 等</li>
     *   <li>封装成统一格式返回</li>
     * </ol>
     * </p>
     */
    public Map<String, Object> systemStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cpuUsage", 0.0);
        result.put("memoryUsage", 0.0);
        result.put("diskUsage", 0.0);
        result.put("containerCount", 0);
        result.put("uptimeSeconds", 0L);
        result.put("source", "mock");
        return result;
    }
}