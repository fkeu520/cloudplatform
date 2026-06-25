package com.cloudhub.platform.space.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.SpaceCategory;
import com.cloudhub.platform.space.service.SpaceCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 空间类别 Controller (park-space 业务) - 通用字典
 * <p>V42: 移除 parkId 参数, 名称全局唯一.</p>
 */
@Tag(name = "空间类别", description = "park-space 业务 - 空间类别")
@RequiredArgsConstructor
@RestController
@RequestMapping("/space-category")
public class SpaceCategoryController {

    private final SpaceCategoryService spaceCategoryService;

    @Operation(summary = "分页查询空间类别列表")
    @GetMapping("/page")
    public Result<PageResult<SpaceCategory>> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return spaceCategoryService.page(keyword, status, pageNum, pageSize);
    }

    @Operation(summary = "校验类别名称是否已存在 (全局唯一)")
    @GetMapping("/check-name")
    public Result<Boolean> checkName(
            @RequestParam(name = "typeName") String typeName,
            @RequestParam(name = "excludeId", required = false) Long excludeId) {
        return spaceCategoryService.checkName(typeName, excludeId);
    }

    @Operation(summary = "查询空间类别详情")
    @GetMapping("/{id}")
    public Result<SpaceCategory> getById(@PathVariable Long id) {
        return spaceCategoryService.getById(id);
    }

    @Operation(summary = "新增空间类别")
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return spaceCategoryService.create(params);
    }

    @Operation(summary = "更新空间类别")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return spaceCategoryService.update(id, params);
    }

    @Operation(summary = "删除空间类别 (软删除)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return spaceCategoryService.delete(id);
    }
}
