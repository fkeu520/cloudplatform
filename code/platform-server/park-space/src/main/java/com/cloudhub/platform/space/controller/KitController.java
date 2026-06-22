package com.cloudhub.platform.space.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Kit;
import com.cloudhub.platform.space.service.KitService;
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
 * 装修配套 Controller (park-space 业务)
 * <p>W3.3 阶段: 装修配套 (Kit) 简单 CRUD 端点.</p>
 * <ul>
 *   <li>{@code GET    /kit/page}          分页查询</li>
 *   <li>{@code GET    /kit/{id}}          详情</li>
 *   <li>{@code POST   /kit}               新增</li>
 *   <li>{@code PUT    /kit/{id}}          更新</li>
 *   <li>{@code DELETE /kit/{id}}          软删除</li>
 * </ul>
 */
@Tag(name = "配套管理", description = "park-space 业务 - 装修配套")
@RequiredArgsConstructor
@RestController
@RequestMapping("/kit")
public class KitController {

    private final KitService kitService;

    @Operation(summary = "分页查询配套列表")
    @GetMapping("/page")
    public Result<PageResult<Kit>> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "parkId", required = false) Long parkId,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return kitService.page(keyword, parkId, status, pageNum, pageSize);
    }

    @Operation(summary = "查询配套详情")
    @GetMapping("/{id}")
    public Result<Kit> getById(@PathVariable Long id) {
        return kitService.getById(id);
    }

    @Operation(summary = "新增配套")
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return kitService.create(params);
    }

    @Operation(summary = "更新配套")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return kitService.update(id, params);
    }

    @Operation(summary = "删除配套 (软删除)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return kitService.delete(id);
    }
}