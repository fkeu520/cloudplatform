package com.cloudhub.platform.space.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.dto.RoomMergeDTO;
import com.cloudhub.platform.space.domain.dto.RoomSplitDTO;
import com.cloudhub.platform.space.domain.entity.Room;
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

import java.util.List;

@Tag(name = "房间拆分合并", description = "park-space 业务 - 拆分合并操作及记录")
@RequiredArgsConstructor
@RestController
@RequestMapping("/room-split-merge")
public class RoomSplitMergeController {

    private final RoomSplitMergeService roomSplitMergeService;

    @Operation(summary = "分页查询拆分合并记录")
    @GetMapping("/page")
    public Result<PageResult<RoomSplitMerge>> page(
            @RequestParam(name = "parkId", required = false) Long parkId,
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return roomSplitMergeService.page(parkId, type, pageNum, pageSize);
    }

    @Operation(summary = "查询拆分合并记录详情")
    @GetMapping("/{id}")
    public Result<RoomSplitMerge> getById(@PathVariable Long id) {
        return roomSplitMergeService.getById(id);
    }

    @Operation(summary = "根据房间 ID 查询拆分合并记录")
    @GetMapping("/room/{roomId}")
    public Result<List<RoomSplitMerge>> listByRoomId(@PathVariable Long roomId) {
        return roomSplitMergeService.listByRoomId(roomId);
    }

    @Operation(summary = "合并房间")
    @PostMapping("/merge")
    public Result<Room> merge(@RequestBody RoomMergeDTO dto) {
        return roomSplitMergeService.merge(dto);
    }

    @Operation(summary = "拆分房间")
    @PostMapping("/split")
    public Result<List<Room>> split(@RequestBody RoomSplitDTO dto) {
        return roomSplitMergeService.split(dto);
    }

    @Operation(summary = "还原拆分合并")
    @PostMapping("/restore/{id}/{type}")
    public Result<RoomSplitMerge> restore(@PathVariable Long id, @PathVariable int type) {
        return roomSplitMergeService.restore(id, type);
    }
}
