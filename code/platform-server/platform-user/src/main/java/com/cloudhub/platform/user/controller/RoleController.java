package com.cloudhub.platform.user.controller;

import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.user.domain.entity.Role;
import com.cloudhub.platform.user.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "角色管理", description = "角色CRUD/菜单权限分配")
@RequiredArgsConstructor
@RestController
@RequestMapping("/role")
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "分页查询角色")
    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
    ) {
        return Result.ok(roleService.page(keyword, status, pageNum, pageSize));
    }

    @Operation(summary = "查询所有角色（不分页）")
    @GetMapping("/list")
    public Result<List<Role>> list(@RequestParam(name = "status", required = false) Integer status) {
        return Result.ok(roleService.list(status));
    }

    @Operation(summary = "根据ID查询角色")
    @GetMapping("/{id}")
    public Result<Role> getById(@PathVariable(name = "id") Long id) {
        return Result.ok(roleService.getById(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    public Result<Void> create(@RequestBody @Validated Map<String, Object> params) {
        roleService.create(params);
        return Result.ok();
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable(name = "id") Long id, @RequestBody @Validated Map<String, Object> params) {
        params.put("id", id);
        roleService.update(params);
        return Result.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable(name = "id") Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "查询角色拥有的菜单ID列表")
    @GetMapping("/{id}/menuIds")
    public Result<List<Long>> getMenuIds(@PathVariable(name = "id") Long id) {
        return Result.ok(roleService.getMenuIds(id));
    }

    @Operation(summary = "给角色分配菜单权限")
    @PostMapping("/{id}/menus")
    public Result<Void> assignMenus(@PathVariable(name = "id") Long id, @RequestBody List<Object> menuIds) {
        List<Long> ids = menuIds.stream()
                .map(o -> o instanceof Number ? ((Number) o).longValue() : Long.parseLong(o.toString()))
                .collect(java.util.stream.Collectors.toList());
        roleService.assignMenus(id, ids);
        return Result.ok();
    }
}