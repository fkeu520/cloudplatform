package com.cloudhub.platform.space.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.RoomLockRecord;
import com.cloudhub.platform.space.service.RoomLockRecordService;
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
 * 房间锁定记录 Controller (park-space 业务)
 * <p>W3.6 阶段: append-only (锁定/解锁历史), 仅 page/getById/create.</p>
 */
@Tag(name = "房间锁定记录", description = "park-space 业务 - 房间锁定操作历史")
@RequiredArgsConstructor
@RestController
@RequestMapping("/room-lock-record")
public class RoomLockRecordController {

    private final RoomLockRecordService roomLockRecordService;

    @Operation(summary = "分页查询锁定记录")
    @GetMapping("/page")
    public Result<PageResult<RoomLockRecord>> page(
            @RequestParam(name = "roomId", required = false) Long roomId,
            @RequestParam(name = "parkId", required = false) Long parkId,
            @RequestParam(name = "isLock", required = false) Integer isLock,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return roomLockRecordService.page(roomId, parkId, isLock, pageNum, pageSize);
    }

    @Operation(summary = "查询锁定记录详情")
    @GetMapping("/{id}")
    public Result<RoomLockRecord> getById(@PathVariable Long id) {
        return roomLockRecordService.getById(id);
    }

    @Operation(summary = "新增锁定记录")
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return roomLockRecordService.create(params);
    }
}