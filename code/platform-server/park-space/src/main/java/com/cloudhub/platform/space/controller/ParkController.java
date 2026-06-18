package com.cloudhub.platform.space.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Park;
import com.cloudhub.platform.space.service.ParkService;
import com.cloudhub.platform.space.service.ParkService.RegionNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 园区管理 Controller
 * <p>提供园区 CRUD + 省市区级联数据接口.
 * <ul>
 *   <li>{@code GET    /park/region/tree}  省市区三级树</li>
 *   <li>{@code GET    /park/page}         分页查询</li>
 *   <li>{@code GET    /park/{id}}         详情</li>
 *   <li>{@code POST   /park}              新增</li>
 *   <li>{@code PUT    /park/{id}}         更新</li>
 *   <li>{@code DELETE /park/{id}}         删除（软删除）</li>
 *   <li>{@code PATCH  /park/{id}/status}  启停</li>
 *   <li>{@code GET    /park/list}         全部启用园区（下拉选择）</li>
 * </ul>
 * <p>路由: 走 platform-gateway /park/** 规则.</p>
 */
@Tag(name = "园区管理", description = "park-space 业务 - 园区主数据")
@RequiredArgsConstructor
@RestController
@RequestMapping("/park")
public class ParkController {

    private final ParkService parkService;

    // ========== 省市区数据 ==========

    @Operation(summary = "获取省市区三级树（Cascader 数据源）")
    @GetMapping("/region/tree")
    public Result<List<RegionNode>> getRegionTree() {
        return parkService.getRegionTree();
    }

    // ========== 分页查询 ==========

    @Operation(summary = "分页查询园区列表")
    @GetMapping("/page")
    public Result<PageResult<Park>> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return parkService.page(keyword, status, pageNum, pageSize);
    }

    // ========== 详情 ==========

    @Operation(summary = "查询园区详情")
    @GetMapping("/{id}")
    public Result<Park> getById(@PathVariable Long id) {
        return parkService.getById(id);
    }

    // ========== 新增 ==========

    @Operation(summary = "新增园区")
    @PostMapping
    public Result<Long> create(@RequestBody Park park) {
        return parkService.create(park);
    }

    // ========== 更新 ==========

    @Operation(summary = "更新园区")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Park park) {
        return parkService.update(id, park);
    }

    // ========== 删除 ==========

    @Operation(summary = "删除园区（软删除）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return parkService.delete(id);
    }

    // ========== 启停 ==========

    @Operation(summary = "启停园区")
    @PatchMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        return parkService.toggleStatus(id, status);
    }

    // ========== 全部列表 ==========

    @Operation(summary = "查询全部启用园区（供其他模块下拉选择）")
    @GetMapping("/list")
    public Result<List<Park>> listAll() {
        return parkService.listAll();
    }
}