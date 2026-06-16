package com.cloudhub.platform.ops.controller;

import com.cloudhub.platform.common.annotation.Log;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.common.util.JwtUtil;
import com.cloudhub.platform.ops.domain.entity.App;
import com.cloudhub.platform.ops.service.AppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "应用管理", description = "系统应用/模块定义")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/app")
public class AppController {

    private final AppService appService;

    @Operation(summary = "分页查询应用")
    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer appType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(appService.page(keyword, appType, pageNum, pageSize));
    }

    @Operation(summary = "查询所有应用")
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) Integer appType) {
        return Result.ok(appService.list(appType));
    }

    /**
     * W3 阶段新增: 获取当前用户有权限的应用列表 (顶部 tab 数据源)
     *
     * <p>逻辑:
     * <ol>
     *   <li>从 JWT 解析 userId (Header: Authorization: Bearer xxx)</li>
     *   <li>从 TenantContextHolder 取 tenantId (TenantFilter 已注入)</li>
     *   <li>调 AppService.userApps(userId, tenantId) 返回 List&lt;App&gt;</li>
     * </ol>
     * </p>
     *
     * <p>权限过滤在 SQL 层完成:
     * <ul>
     *   <li>普通用户(userType=0): 通过角色/直接授权的 menu.app_id</li>
     *   <li>租户管理员(userType=1): 额外按 sys_tenant_app 过滤</li>
     *   <li>运营管理员(userType=2): 通常返回空 (符合预期, 走 platform-ops-admin)</li>
     * </ul>
     * </p>
     */
    @Operation(summary = "获取当前用户有权限的应用列表（顶部 tab 用）")
    @GetMapping("/user")
    public Result<List<App>> getUserApps(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                String userIdStr = JwtUtil.getUserId(token);
                if (userIdStr != null && !userIdStr.isBlank()) {
                    Long userId = Long.parseLong(userIdStr);
                    Long tenantId = TenantContextHolder.getTenantId();
                    return Result.ok(appService.userApps(userId, tenantId));
                }
            } catch (Exception e) {
                log.warn("JWT 解析失败 /app/user: {}", e.getMessage());
            }
        }
        return Result.ok(java.util.Collections.emptyList());
    }

    @Operation(summary = "根据ID查询")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.ok(appService.getById(id));
    }

    @Log(title = "应用管理", businessType = 1)
    @Operation(summary = "新增应用")
    @PostMapping
    public Result<Void> create(@RequestBody Map<String, Object> params) {
        appService.create(params);
        return Result.ok();
    }

    @Log(title = "应用管理", businessType = 2)
    @Operation(summary = "更新应用")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        appService.update(id, params);
        return Result.ok();
    }

    @Log(title = "应用管理", businessType = 3)
    @Operation(summary = "删除应用")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        appService.delete(id);
        return Result.ok();
    }
}
