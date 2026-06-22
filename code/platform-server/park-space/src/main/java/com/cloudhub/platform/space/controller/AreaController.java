package com.cloudhub.platform.space.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Area;
import com.cloudhub.platform.space.service.AreaService;
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
 * 区域 Controller (park-space 业务)
 * <p>W3.3 阶段: 区域 (Area) 简单 CRUD 端点.</p>
 * <ul>
 *   <li>{@code GET    /area/page}         分页查询</li>
 *   <li>{@code GET    /area/{id}}         详情</li>
 *   <li>{@code POST   /area}              新增</li>
 *   <li>{@code PUT    /area/{id}}         更新</li>
 *   <li>{@code DELETE /area/{id}}         软删除</li>
 * </ul>
 */
@Tag(name = "区域管理", description = "park-space 业务 - 区域")
@RequiredArgsConstructor
@RestController
@RequestMapping("/area")
public class AreaController {

    private final AreaService areaService;

    @Operation(summary = "分页查询区域列表")
    @GetMapping("/page")
    public Result<PageResult<Area>> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "parkId", required = false) Long parkId,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return areaService.page(keyword, parkId, status, pageNum, pageSize);
    }

    @Operation(summary = "查询区域详情")
    @GetMapping("/{id}")
    public Result<Area> getById(@PathVariable Long id) {
        return areaService.getById(id);
    }

    @Operation(summary = "新增区域")
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return areaService.create(params);
    }

    @Operation(summary = "更新区域")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return areaService.update(id, params);
    }

    @Operation(summary = "删除区域 (软删除)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return areaService.delete(id);
    }
}