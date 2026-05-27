package com.cloudhub.platform.ops.controller;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.ops.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "租户管理", description = "租户CRUD/启停/类型/用户数限制")
@RequiredArgsConstructor
@RestController
@RequestMapping("/tenant")
public class TenantController {

    private final TenantService tenantService;

    @Operation(summary = "分页查询租户")
    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer tenantType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(tenantService.page(keyword, status, tenantType, pageNum, pageSize));
    }

    @Operation(summary = "查询所有租户")
    @GetMapping("/list")
    public Result<List<?>> list(@RequestParam(required = false) Integer status) {
        return Result.ok(tenantService.list(status));
    }

    @Operation(summary = "根据ID查询租户")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.ok(tenantService.getById(id));
    }

    @Operation(summary = "新增租户")
    @PostMapping
    public Result<Void> create(@RequestBody Map<String, Object> params) {
        tenantService.create(params);
        return Result.ok();
    }

    @Operation(summary = "更新租户")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        tenantService.update(id, params);
        return Result.ok();
    }

    @Operation(summary = "删除租户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tenantService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "切换租户状态（启用/禁用）")
    @PostMapping("/{id}/toggle-status")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        tenantService.toggleStatus(id);
        return Result.ok();
    }

    @Operation(summary = "校验租户用户数是否超限")
    @PostMapping("/{id}/check-user-count")
    public Result<Void> checkUserCount(@PathVariable Long id, @RequestParam long currentCount) {
        tenantService.checkUserCount(id, currentCount);
        return Result.ok();
    }

    @Operation(summary = "判断租户是否可以增加分子公司")
    @GetMapping("/{id}/can-add-subsidiary")
    public Result<Boolean> canAddSubsidiary(@PathVariable Long id) {
        return Result.ok(tenantService.canAddSubsidiary(id));
    }

    @Operation(summary = "获取租户下的组织列表")
    @GetMapping("/{id}/orgs")
    public Result<?> listOrgs(@PathVariable Long id) {
        return Result.ok(tenantService.listOrgs(id));
    }

    @Operation(summary = "获取租户管理员列表")
    @GetMapping("/{id}/admins")
    public Result<?> listAdmins(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(tenantService.listAdmins(id, pageNum, pageSize));
    }

    @Operation(summary = "新增租户管理员")
    @PostMapping("/{id}/admin")
    public Result<Void> createAdmin(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        tenantService.createAdmin(id, params);
        return Result.ok();
    }

    @Operation(summary = "删除租户管理员")
    @DeleteMapping("/{id}/admin/{userId}")
    public Result<Void> deleteAdmin(@PathVariable Long id, @PathVariable Long userId) {
        tenantService.deleteAdmin(id, userId);
        return Result.ok();
    }
}
