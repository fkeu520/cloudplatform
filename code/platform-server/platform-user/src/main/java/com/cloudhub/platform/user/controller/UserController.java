package com.cloudhub.platform.user.controller;

import com.cloudhub.platform.common.annotation.Log;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.user.domain.entity.User;
import com.cloudhub.platform.user.domain.vo.LoginVO;
import com.cloudhub.platform.user.domain.vo.UserPageVO;
import com.cloudhub.platform.user.domain.vo.UserVO;
import com.cloudhub.platform.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理", description = "用户CRUD/登录/角色分配/密码管理")
@RequiredArgsConstructor
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    // ========== 认证相关 ==========

    @Operation(summary = "用户登录")
    @Log(title = "用户管理", businessType = 0)
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Validated Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        if (username == null || password == null) {
            throw new BizException("用户名和密码不能为空");
        }
        LoginVO vo = userService.login(username, password);
        return Result.ok(vo);
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/info")
    public Result<UserVO> getCurrentUser(
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        if (userId == null || userId.isBlank()) {
            throw new BizException("未登录");
        }
        return Result.ok(userService.getById(Long.parseLong(userId)));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId != null && !userId.isBlank()) {
            userService.logout(Long.parseLong(userId));
        }
        return Result.ok();
    }

    @Operation(summary = "内部验证密码（供 auth 服务调用）")
    @PostMapping("/internal/validate")
    public Result<UserVO> validatePassword(@RequestBody @Validated Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        if (username == null || password == null) {
            throw new BizException("用户名和密码不能为空");
        }
        // 内部接口: auth 服务调用时无 JWT, 预先设置默认租户上下文
        // 否则 P0-1 TenantLineInnerInterceptor 会使用 9999 导致查不到 admin(tenant_id=1)
        TenantContextHolder.setTenantId(1L);
        try {
            UserVO vo = userService.validatePassword(username, password);
            return Result.ok(vo);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Operation(summary = "内部按用户名查找用户（供 auth 服务调用）")
    @GetMapping("/internal/by-username/{username}")
    public Result<UserVO> getByUsername(@PathVariable String username) {
        TenantContextHolder.setTenantId(1L);
        try {
            return Result.ok(userService.getByUsername(username));
        } finally {
            TenantContextHolder.clear();
        }
    }

    // ========== 用户管理 ==========

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "orgId", required = false) Long orgId,
            @RequestParam(name = "orgIds", required = false) String orgIds,
            @RequestParam(name = "deptId", required = false) Long deptId,
            @RequestParam(name = "postId", required = false) Long postId,
            @RequestParam(name = "tenantId", required = false) Integer tenantId,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "userType", required = false) Integer userType,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
    ) {
        return Result.ok(userService.page(keyword, orgId, orgIds, deptId, postId, tenantId, status, userType, pageNum, pageSize));
    }

    @Operation(summary = "查询所有用户列表")
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "orgId", required = false) Long orgId,
            @RequestParam(name = "status", required = false) Integer status
    ) {
        return Result.ok(userService.list(keyword, orgId, status));
    }

    @Operation(summary = "根据ID查询用户")
    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable(name = "id") Long id) {
        return Result.ok(userService.getById(id));
    }

    @Operation(summary = "新增用户")
    @Log(title = "用户管理", businessType = 1)
    @PostMapping
    public Result<Void> create(@RequestBody @Validated Map<String, Object> params) {
        userService.create(params);
        return Result.ok();
    }

    @Operation(summary = "更新用户")
    @Log(title = "用户管理", businessType = 2)
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable(name = "id") Long id, @RequestBody @Validated Map<String, Object> params) {
        params.put("id", id);
        userService.update(params);
        return Result.ok();
    }

    @Operation(summary = "删除用户")
    @Log(title = "用户管理", businessType = 3)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable(name = "id") Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "修改密码")
    @PostMapping("/{id}/password")
    public Result<Void> changePassword(
            @PathVariable(name = "id") Long id,
            @RequestBody @Validated Map<String, String> params
    ) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            throw new BizException("原密码和新密码不能为空");
        }
        userService.changePassword(id, oldPassword, newPassword);
        return Result.ok();
    }

    @Operation(summary = "重置密码")
    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(
            @PathVariable(name = "id") Long id,
            @RequestBody @Validated Map<String, String> params
    ) {
        String newPassword = params.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            throw new BizException("新密码不能为空");
        }
        userService.resetPassword(id, newPassword);
        return Result.ok();
    }

    @Operation(summary = "切换状态（启用/禁用）")
    @PostMapping("/{id}/toggle-status")
    public Result<Void> toggleStatus(@PathVariable(name = "id") Long id) {
        userService.toggleStatus(id);
        return Result.ok();
    }

    @Operation(summary = "分配角色")
    @PostMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable(name = "id") Long id, @RequestBody Map<String, Object> params) {
        userService.assignRoles(id, params.get("roleIds"));
        return Result.ok();
    }

    @Operation(summary = "获取用户直接授权的菜单ID列表")
    @GetMapping("/{id}/menuIds")
    public Result<List<Long>> getUserMenuIds(@PathVariable(name = "id") Long id) {
        return Result.ok(userService.getUserMenuIds(id));
    }

    @Operation(summary = "分配用户直接授权菜单（运营管理员专用）")
    @PostMapping("/{id}/menus")
    public Result<Void> assignUserMenus(@PathVariable(name = "id") Long id, @RequestBody Map<String, Object> params) {
        userService.assignUserMenus(id, params.get("menuIds"));
        return Result.ok();
    }
}