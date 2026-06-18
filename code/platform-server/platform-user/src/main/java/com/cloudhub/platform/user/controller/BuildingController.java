package com.cloudhub.platform.user.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.user.domain.entity.Building;
import com.cloudhub.platform.user.service.BuildingService;
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
 * 园区楼宇 Controller (park-property 业务)
 *
 * <p>W3.2 阶段: 完整 CRUD 端点.
 * 路由: 走 platform-gateway /building/** 规则 (待 W3.2 网关路由更新).</p>
 *
 * @author csyh fusion W3.2
 * @since 2026-06-18
 */
@Tag(name = "园区楼宇", description = "park-property 业务 - 楼宇管理")
@RequiredArgsConstructor
@RestController
@RequestMapping("/building")
public class BuildingController {

    private final BuildingService buildingService;

    @Operation(summary = "分页查询楼宇列表")
    @GetMapping("/page")
    public Result<PageResult<Building>> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return buildingService.page(keyword, status, pageNum, pageSize);
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
