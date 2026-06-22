package com.cloudhub.platform.space.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Massif;
import com.cloudhub.platform.space.service.MassifService;
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
 * 地块 Controller (park-space 业务)
 * <p>W3.5 阶段: 地块 (Massif) 简单 CRUD 端点.</p>
 */
@Tag(name = "地块管理", description = "park-space 业务 - 地块")
@RequiredArgsConstructor
@RestController
@RequestMapping("/massif")
public class MassifController {

    private final MassifService massifService;

    @Operation(summary = "分页查询地块列表")
    @GetMapping("/page")
    public Result<PageResult<Massif>> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "parkId", required = false) Long parkId,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return massifService.page(keyword, parkId, status, pageNum, pageSize);
    }

    @Operation(summary = "查询地块详情")
    @GetMapping("/{id}")
    public Result<Massif> getById(@PathVariable Long id) {
        return massifService.getById(id);
    }

    @Operation(summary = "新增地块")
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return massifService.create(params);
    }

    @Operation(summary = "更新地块")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return massifService.update(id, params);
    }

    @Operation(summary = "删除地块 (软删除)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return massifService.delete(id);
    }
}