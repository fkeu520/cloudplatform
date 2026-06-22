package com.cloudhub.platform.space.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.RoomSplitMerge;
import com.cloudhub.platform.space.service.RoomSplitMergeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 房间拆分合并记录 Controller (park-space 业务)
 * <p>W3.6 阶段: 拆分合并记录 (RoomSplitMerge) append-only.</p>
 */
@Tag(name = "房间拆分合并", description = "park-space 业务 - 拆分合并操作历史")
@RequiredArgsConstructor
@RestController
@RequestMapping("/room-split-merge")
public class RoomSplitMergeController {

    private final RoomSplitMergeService roomSplitMergeService;

    @Operation(summary = "分页查询拆分合并记录")
    @GetMapping("/page")
    public Result<PageResult<RoomSplitMerge>> page(
            @RequestParam(name = "parkId", required = false) Long parkId,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return roomSplitMergeService.page(parkId, status, type, pageNum, pageSize);
    }

    @Operation(summary = "查询拆分合并记录详情")
    @GetMapping("/{id}")
    public Result<RoomSplitMerge> getById(@PathVariable Long id) {
        return roomSplitMergeService.getById(id);
    }

    @Operation(summary = "新增拆分合并记录")
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return roomSplitMergeService.create(params);
    }
}