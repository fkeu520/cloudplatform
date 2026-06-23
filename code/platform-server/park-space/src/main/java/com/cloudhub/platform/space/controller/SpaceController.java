package com.cloudhub.platform.space.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Space;
import com.cloudhub.platform.space.service.SpaceService;
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
 * 空间 Controller (park-space 业务)
 * <p>W3.5 阶段: 空间 (Space) 简单 CRUD 端点.</p>
 */
@Tag(name = "空间管理", description = "park-space 业务 - 空间")
@RequiredArgsConstructor
@RestController
@RequestMapping("/space")
public class SpaceController {

    private final SpaceService spaceService;

    @Operation(summary = "分页查询空间列表")
    @GetMapping("/page")
    public Result<PageResult<Space>> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "parkId", required = false) Long parkId,
            @RequestParam(name = "areaId", required = false) Long areaId,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return spaceService.page(keyword, parkId, areaId, categoryId, pageNum, pageSize);
    }

    @Operation(summary = "校验同园区+分类下空间名称是否已存在")
    @GetMapping("/check-name")
    public Result<Boolean> checkName(
            @RequestParam(name = "parkId") Long parkId,
            @RequestParam(name = "categoryId") Long categoryId,
            @RequestParam(name = "spaceName") String spaceName,
            @RequestParam(name = "excludeId", required = false) Long excludeId) {
        return spaceService.checkName(parkId, categoryId, spaceName, excludeId);
    }

    @Operation(summary = "查询空间详情")
    @GetMapping("/{id}")
    public Result<Space> getById(@PathVariable Long id) {
        return spaceService.getById(id);
    }

    @Operation(summary = "新增空间")
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return spaceService.create(params);
    }

    @Operation(summary = "更新空间")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return spaceService.update(id, params);
    }

    @Operation(summary = "删除空间 (软删除)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return spaceService.delete(id);
    }
}