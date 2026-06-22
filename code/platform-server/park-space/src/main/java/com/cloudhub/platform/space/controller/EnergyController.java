package com.cloudhub.platform.space.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Energy;
import com.cloudhub.platform.space.service.EnergyService;
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
 * 能源房间关联 Controller (park-space 业务)
 * <p>W3.4 阶段: 能耗 (Energy) 简单 CRUD 端点.</p>
 */
@Tag(name = "能耗管理", description = "park-space 业务 - 能耗与房间关联")
@RequiredArgsConstructor
@RestController
@RequestMapping("/energy")
public class EnergyController {

    private final EnergyService energyService;

    @Operation(summary = "分页查询能耗列表")
    @GetMapping("/page")
    public Result<PageResult<Energy>> page(
            @RequestParam(name = "parkId", required = false) Long parkId,
            @RequestParam(name = "roomId", required = false) Long roomId,
            @RequestParam(name = "meterId", required = false) Long meterId,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return energyService.page(parkId, roomId, meterId, status, pageNum, pageSize);
    }

    @Operation(summary = "查询能耗详情")
    @GetMapping("/{id}")
    public Result<Energy> getById(@PathVariable Long id) {
        return energyService.getById(id);
    }

    @Operation(summary = "新增能耗")
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return energyService.create(params);
    }

    @Operation(summary = "更新能耗")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return energyService.update(id, params);
    }

    @Operation(summary = "删除能耗 (软删除)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return energyService.delete(id);
    }
}