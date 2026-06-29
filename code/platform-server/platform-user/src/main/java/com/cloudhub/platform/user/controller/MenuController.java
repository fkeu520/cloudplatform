package com.cloudhub.platform.user.controller;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.common.util.JwtUtil;
import com.cloudhub.platform.user.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "菜单管理", description = "菜单树/导航/权限管理")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/menu")
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "获取完整菜单树（管理后台用）")
    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree() {
        return Result.ok(menuService.tree());
    }

    @Operation(summary = "获取导航菜单树（左侧菜单）")
    @GetMapping("/nav")
    public Result<List<Map<String, Object>>> navTree() {
        return Result.ok(menuService.navTree());
    }

    /**
     * 按 menu_category 列出菜单树 (V40+ 平台菜单隔离端点)
     *
     * <p>使用场景: platform-ops-admin 的 OpsUserController.getMenuTree() 代理
     * 此端点, category 硬编码 "ops-admin" (见 Constants.MenuCategory.OPS_ADMIN)。</p>
     *
     * <p>与 /menu/tree 不同: 该端点不过滤 NULL 行或全表, 仅返回 menu_category = X 的行,
     * 物理隔离 admin-platform 与 ops-admin 的菜单。</p>
     */
    @Operation(summary = "按平台类别获取菜单树 (admin / ops-admin / common)")
    @GetMapping("/by-category")
    public Result<List<Map<String, Object>>> byCategory(@RequestParam String category) {
        return Result.ok(menuService.listByCategory(category));
    }

    /**
     * 获取当前用户菜单树 (W3 P0-5 新增 appId 参数支持)
     *
     * <p>逻辑:
     * <ul>
     *   <li>不带 appId 参数: 返回用户全量菜单 (向后兼容)</li>
     *   <li>带 appId=X: 只返回该 app 下的菜单 + appId IS NULL 的公共菜单</li>
     * </ul>
     * </p>
     *
     * <p>W3 用途: Layout.vue 切换顶部 tab 时, 调用此端点获取该 app 下的左侧菜单树.</p>
     */
    @Operation(summary = "获取当前用户菜单树（支持 appId 过滤，W3 顶部 tab 切换用）")
    @GetMapping("/user")
    public Result<List<Map<String, Object>>> getUserMenus(
            HttpServletRequest request,
            @RequestParam(required = false) Long appId) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                String userId = JwtUtil.getUserId(token);
                if (userId != null && !userId.isBlank()) {
                    return Result.ok(menuService.getUserMenus(Long.parseLong(userId), appId));
                }
            } catch (Exception e) {
                log.warn("JWT解析失败: {}", e.getMessage());
            }
        }
        return Result.ok(menuService.navTree());
    }

    @Operation(summary = "获取当前用户权限列表")
    @GetMapping("/perms")
    public Result<List<String>> getUserPermissions(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                String userId = JwtUtil.getUserId(token);
                if (userId != null && !userId.isBlank()) {
                    return Result.ok(menuService.getUserPermissions(Long.parseLong(userId)));
                }
            } catch (Exception e) {
                log.warn("JWT解析失败: {}", e.getMessage());
            }
        }
        return Result.ok(new java.util.ArrayList<>());
    }

    @Operation(summary = "根据角色ID获取菜单树")
    @GetMapping("/role/{roleId}")
    public Result<List<Map<String, Object>>> getRoleMenus(@PathVariable(name = "roleId") Long roleId) {
        return Result.ok(menuService.getRoleMenus(roleId));
    }

    @Operation(summary = "根据ID查询菜单")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable(name = "id") Long id) {
        return Result.ok(menuService.getById(id));
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    public Result<Void> create(@RequestBody Map<String, Object> params) {
        menuService.create(params);
        return Result.ok();
    }

    @Operation(summary = "更新菜单")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable(name = "id") Long id, @RequestBody Map<String, Object> params) {
        params.put("id", id);
        menuService.update(params);
        return Result.ok();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable(name = "id") Long id) {
        menuService.delete(id);
        return Result.ok();
    }
}