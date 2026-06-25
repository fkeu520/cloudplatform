package com.cloudhub.platform.space.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Building;
import com.cloudhub.platform.space.service.BuildingService;
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
 * 园区楼宇 Controller (park-space 业务, 从 park-property 迁移)
 * <p>路由: /building/** 由 platform-gateway 转发到 park-space</p>
 */
@Tag(name = "园区楼宇", description = "park-space 业务 - 楼宇管理")
@RequiredArgsConstructor
@RestController
@RequestMapping("/building")
public class BuildingController {

    private final BuildingService buildingService;

    @Operation(summary = "分页查询楼宇列表 (支持 parkId/areaId 级联过滤)")
    @GetMapping("/page")
    public Result<PageResult<Building>> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "parkId", required = false) Long parkId,
            @RequestParam(name = "areaId", required = false) Long areaId,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return buildingService.page(keyword, parkId, areaId, status, pageNum, pageSize);
    }

    @Operation(summary = "校验同园区楼栋编号是否已存在")
    @GetMapping("/check-code")
    public Result<Boolean> checkCode(
            @RequestParam(name = "parkId") Long parkId,
            @RequestParam(name = "buildingCode") String buildingCode,
            @RequestParam(name = "excludeId", required = false) Long excludeId) {
        return buildingService.checkCode(parkId, buildingCode, excludeId);
    }

    @Operation(summary = "查询楼宇详情")
    @GetMapping("/{id}")
    public Result<Building> getById(@PathVariable Long id) {
        return buildingService.getById(id);
    }

    @Operation(summary = "新增楼宇")
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return buildingService.create(params);
    }

    @Operation(summary = "更新楼宇字段")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return buildingService.update(id, params);
    }

    @Operation(summary = "删除楼宇 (软删除)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return buildingService.delete(id);
    }
}