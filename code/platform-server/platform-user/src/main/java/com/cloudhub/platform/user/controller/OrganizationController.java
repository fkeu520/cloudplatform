package com.cloudhub.platform.user.controller;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.user.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "组织机构管理", description = "组织树CRUD")
@RequiredArgsConstructor
@RestController
@RequestMapping("/org")
public class OrganizationController {

    private final OrganizationService orgService;

    @Operation(summary = "获取组织树（可按租户过滤）")
    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree(
            @RequestParam(required = false) Long tenantId) {
        return Result.ok(orgService.tree(tenantId));
    }

    @Operation(summary = "根据ID查询组织")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable(name = "id") Long id) {
        return Result.ok(orgService.getById(id));
    }

    @Operation(summary = "新增组织")
    @PostMapping
    public Result<Void> create(@RequestBody Map<String, Object> params) {
        orgService.create(params);
        return Result.ok();
    }

    @Operation(summary = "更新组织")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable(name = "id") Long id, @RequestBody Map<String, Object> params) {
        params.put("id", id);
        orgService.update(params);
        return Result.ok();
    }

    @Operation(summary = "删除组织")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable(name = "id") Long id) {
        orgService.delete(id);
        return Result.ok();
    }
}