package com.cloudhub.platform.space.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.RoomRecord;
import com.cloudhub.platform.space.service.RoomRecordService;
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
 * 房间记录 Controller (park-space 业务)
 * <p>W3.6 阶段: 房间记录 (RoomRecord) append-only (绑定/解绑历史).</p>
 */
@Tag(name = "房间记录", description = "park-space 业务 - 房间绑定/解绑历史")
@RequiredArgsConstructor
@RestController
@RequestMapping("/room-record")
public class RoomRecordController {

    private final RoomRecordService roomRecordService;

    @Operation(summary = "分页查询房间记录")
    @GetMapping("/page")
    public Result<PageResult<RoomRecord>> page(
            @RequestParam(name = "roomId", required = false) Long roomId,
            @RequestParam(name = "parkId", required = false) Long parkId,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return roomRecordService.page(roomId, parkId, status, pageNum, pageSize);
    }

    @Operation(summary = "查询房间记录详情")
    @GetMapping("/{id}")
    public Result<RoomRecord> getById(@PathVariable Long id) {
        return roomRecordService.getById(id);
    }

    @Operation(summary = "新增房间记录")
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return roomRecordService.create(params);
    }
}