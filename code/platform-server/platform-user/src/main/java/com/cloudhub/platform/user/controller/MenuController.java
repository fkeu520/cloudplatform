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

    @Operation(summary = "获取当前用户菜单树")
    @GetMapping("/user")
    public Result<List<Map<String, Object>>> getUserMenus(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                String userId = JwtUtil.getUserId(token);
                if (userId != null && !userId.isBlank()) {
                    return Result.ok(menuService.getUserMenus(Long.parseLong(userId)));
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