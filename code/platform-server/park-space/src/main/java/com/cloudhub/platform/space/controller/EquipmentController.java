package com.cloudhub.platform.space.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Equipment;
import com.cloudhub.platform.space.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 设备设施 Controller (park-space 业务)
 * <p>W3.4 阶段: 设备 (Equipment) 简单 CRUD 端点.</p>
 */
@Tag(name = "设备管理", description = "park-space 业务 - 设备设施")
@RequiredArgsConstructor
@RestController
@RequestMapping("/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    @Operation(summary = "分页查询设备列表")
    @GetMapping("/page")
    public Result<PageResult<Equipment>> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "parkId", required = false) Long parkId,
            @RequestParam(name = "kitId", required = false) Long kitId,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return equipmentService.page(keyword, parkId, kitId, status, pageNum, pageSize);
    }

    @Operation(summary = "查询设备详情")
    @GetMapping("/{id}")
    public Result<Equipment> getById(@PathVariable Long id) {
        return equipmentService.getById(id);
    }

    @Operation(summary = "新增设备")
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return equipmentService.create(params);
    }

    @Operation(summary = "更新设备")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return equipmentService.update(id, params);
    }

    @Operation(summary = "根据配套ID查询设备列表")
    @GetMapping("/listByKit/{kitId}")
    public Result<List<Equipment>> listByKitId(@PathVariable Long kitId) {
        return equipmentService.listByKitId(kitId);
    }

    @Operation(summary = "批量保存配套下的设备列表")
    @PostMapping("/batchSave/{kitId}")
    public Result<Void> batchSave(@PathVariable Long kitId, @RequestBody List<Map<String, Object>> equipmentList) {
        return equipmentService.batchSave(kitId, equipmentList);
    }

    @Operation(summary = "删除设备 (软删除)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return equipmentService.delete(id);
    }
}