package com.cloudhub.platform.space.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.PlanUse;
import com.cloudhub.platform.space.service.PlanUseService;
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
 * 规划用途 Controller (park-space 业务)
 * <p>W3.3 阶段: 规划用途 (PlanUse) 简单 CRUD 端点.</p>
 * <ul>
 *   <li>{@code GET    /plan-use/page}       分页查询</li>
 *   <li>{@code GET    /plan-use/{id}}       详情</li>
 *   <li>{@code POST   /plan-use}            新增</li>
 *   <li>{@code PUT    /plan-use/{id}}       更新</li>
 *   <li>{@code DELETE /plan-use/{id}}       软删除</li>
 * </ul>
 */
@Tag(name = "规划用途", description = "park-space 业务 - 规划用途")
@RequiredArgsConstructor
@RestController
@RequestMapping("/plan-use")
public class PlanUseController {

    private final PlanUseService planUseService;

    @Operation(summary = "分页查询规划用途列表")
    @GetMapping("/page")
    public Result<PageResult<PlanUse>> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "parkId", required = false) Long parkId,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return planUseService.page(keyword, parkId, status, pageNum, pageSize);
    }

    @Operation(summary = "查询规划用途详情")
    @GetMapping("/{id}")
    public Result<PlanUse> getById(@PathVariable Long id) {
        return planUseService.getById(id);
    }

    @Operation(summary = "新增规划用途")
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return planUseService.create(params);
    }

    @Operation(summary = "更新规划用途")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return planUseService.update(id, params);
    }

    @Operation(summary = "删除规划用途 (软删除)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return planUseService.delete(id);
    }
}