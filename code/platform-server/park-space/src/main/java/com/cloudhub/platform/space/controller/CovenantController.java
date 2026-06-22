package com.cloudhub.platform.space.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.Covenant;
import com.cloudhub.platform.space.service.CovenantService;
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
 * 合同房间关联 Controller (park-space 业务)
 * <p>W3.4 阶段: 合同-房间关联 (Covenant) 简单 CRUD 端点.</p>
 */
@Tag(name = "合同房间关联", description = "park-space 业务 - 合同与房间关联")
@RequiredArgsConstructor
@RestController
@RequestMapping("/covenant")
public class CovenantController {

    private final CovenantService covenantService;

    @Operation(summary = "分页查询合同关联列表")
    @GetMapping("/page")
    public Result<PageResult<Covenant>> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "parkId", required = false) Long parkId,
            @RequestParam(name = "roomId", required = false) Long roomId,
            @RequestParam(name = "covenantType", required = false) Integer covenantType,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return covenantService.page(keyword, parkId, roomId, covenantType, pageNum, pageSize);
    }

    @Operation(summary = "查询合同关联详情")
    @GetMapping("/{id}")
    public Result<Covenant> getById(@PathVariable Long id) {
        return covenantService.getById(id);
    }

    @Operation(summary = "新增合同关联")
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return covenantService.create(params);
    }

    @Operation(summary = "更新合同关联")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return covenantService.update(id, params);
    }

    @Operation(summary = "删除合同关联 (软删除)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return covenantService.delete(id);
    }
}