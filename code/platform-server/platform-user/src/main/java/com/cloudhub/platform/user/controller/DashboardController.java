package com.cloudhub.platform.user.controller;

import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.common.util.JwtUtil;
import com.cloudhub.platform.user.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 工作台 Dashboard Controller (W3 P1-3)
 *
 * <p>提供 6 个端点, 服务于前端 Dashboard.vue 工作台 6 大模块:
 * <ol>
 *   <li>GET /dashboard/welcome          - 顶部欢迎语 + 今日 4 数 (待办/消息/我的申请/系统通知)</li>
 *   <li>GET /dashboard/todos?limit=5   - 我的待办 Top 5</li>
 *   <li>GET /dashboard/messages?limit=5 - 未读消息 Top 5</li>
 *   <li>GET /dashboard/shortcuts?appCode=X - 当前 app 快捷入口 (扁平菜单, 按权限)</li>
 *   <li>GET /dashboard/business-stats?appCode=X - 业务数据概览</li>
 *   <li>GET /dashboard/system-stats      - 系统资源 (CPU/内存, 仅 userType=2 运营管理员)</li>
 * </ol>
 * </p>
 *
 * <p>W3 阶段策略:
 * <ul>
 *   <li>所有端点结构完整, 数据用 Mock (返回占位结构)</li>
 *   <li>W3+ 阶段逐步接入真数据 (workflow/message/container-exporter/park-* 业务)</li>
 *   <li>Ctrl 不变, Service 实现替换即可</li>
 * </ul>
 * </p>
 *
 * <p>注意: 公告数据源 {@code GET /announcement/recent} 已由 P0-3 在 platform-ops 实现, 不在此重复.</p>
 */
@Tag(name = "工作台", description = "首页 Dashboard 数据源 (6 端点)")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 顶部欢迎条 + 今日 4 数
     */
    @Operation(summary = "工作台顶部欢迎数据 (今日 4 数)")
    @GetMapping("/welcome")
    public Result<Map<String, Object>> welcome(HttpServletRequest request) {
        Long userId = extractUserId(request);
        if (userId == null) {
            return Result.ok(emptyWelcome());
        }
        return Result.ok(dashboardService.welcome(userId));
    }

    /**
     * 我的待办 Top N
     */
    @Operation(summary = "我的待办 Top N (W3 占位, W3+ 接 workflow)")
    @GetMapping("/todos")
    public Result<List<Map<String, Object>>> todos(
            HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "5") int limit) {
        Long userId = extractUserId(request);
        if (userId == null) {
            return Result.ok(Collections.emptyList());
        }
        return Result.ok(dashboardService.todos(userId, clampLimit(limit)));
    }

    /**
     * 未读消息 Top N
     */
    @Operation(summary = "未读消息 Top N (W3 占位, W3+ 接 message)")
    @GetMapping("/messages")
    public Result<List<Map<String, Object>>> messages(
            HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "5") int limit) {
        Long userId = extractUserId(request);
        if (userId == null) {
            return Result.ok(Collections.emptyList());
        }
        return Result.ok(dashboardService.messages(userId, clampLimit(limit)));
    }

    /**
     * 快捷入口 (扁平菜单列表, 按权限 + appCode 过滤)
     *
     * <p>W3 阶段: 复用 MenuService, 返回用户对当前 app 有权限的菜单(扁平,不含子菜单).
     * 数据驱动, 业务方在前端展示为图标网格.</p>
     */
    @Operation(summary = "快捷入口 (扁平菜单, 按 appCode 过滤)")
    @GetMapping("/shortcuts")
    public Result<List<Map<String, Object>>> shortcuts(
            HttpServletRequest request,
            @RequestParam(required = false) Long appId) {
        Long userId = extractUserId(request);
        if (userId == null) {
            return Result.ok(Collections.emptyList());
        }
        return Result.ok(dashboardService.shortcuts(userId, appId));
    }

    /**
     * 业务数据概览 (按 appCode 路由不同 Service)
     */
    @Operation(summary = "业务数据概览 (按 appCode 路由)")
    @GetMapping("/business-stats")
    public Result<List<Map<String, Object>>> businessStats(
            @RequestParam(required = false) String appCode) {
        // 短路: appCode 为空时直接返回空列表, 不调 service
        if (appCode == null || appCode.isBlank()) {
            return Result.ok(Collections.emptyList());
        }
        return Result.ok(dashboardService.businessStats(appCode));
    }

    /**
     * 系统资源 (CPU/内存/磁盘, 仅 userType=2 运营管理员可见)
     *
     * <p>W3 阶段: 从 container-exporter Prometheus API 拉取.
     * 数据源: http://192.168.0.217:9100/metrics (node-exporter) 或 container-exporter.</p>
     */
    @Operation(summary = "系统资源监控 (仅运营管理员可见)")
    @GetMapping("/system-stats")
    public Result<Map<String, Object>> systemStats(HttpServletRequest request) {
        Long userId = extractUserId(request);
        // W3 阶段: 仅 userType=2 (运营管理员) 返回真实数据, 其他用户返回 null
        Integer userType = extractUserType(request);
        if (userType == null || userType != 2) {
            return Result.ok(Collections.emptyMap());
        }
        return Result.ok(dashboardService.systemStats());
    }

    // ========== 内部工具方法 ==========

    private Long extractUserId(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                String userId = JwtUtil.getUserId(token.substring(7));
                if (userId != null && !userId.isBlank()) {
                    return Long.parseLong(userId);
                }
            }
        } catch (Exception e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
        }
        return null;
    }

    private Integer extractUserType(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                Integer userType = JwtUtil.getUserType(token.substring(7));
                return userType;
            }
        } catch (Exception e) {
            log.warn("JWT 解析失败 (userType): {}", e.getMessage());
        }
        return null;
    }

    private int clampLimit(int limit) {
        return Math.max(1, Math.min(limit, 20));
    }

    private Map<String, Object> emptyWelcome() {
        return Map.of(
                "todoCount", 0,
                "msgCount", 0,
                "myApplyCount", 0,
                "sysNoticeCount", 0
        );
    }
}