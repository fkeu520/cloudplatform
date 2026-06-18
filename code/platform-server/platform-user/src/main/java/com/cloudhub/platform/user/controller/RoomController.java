package com.cloudhub.platform.user.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.user.domain.entity.Room;
import com.cloudhub.platform.user.service.RoomService;
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

import java.util.Map;

/**
 * 园区房屋 Controller (park-space 业务)
 *
 * <p>W3.1 阶段: 完整 CRUD + 状态机端点.
 * <ul>
 *   <li>{@code GET    /room/page}        分页查询</li>
 *   <li>{@code GET    /room/{id}}        详情</li>
 *   <li>{@code POST   /room}             新增</li>
 *   <li>{@code PUT    /room/{id}}        更新字段 (不含状态)</li>
 *   <li>{@code DELETE /room/{id}}        软删除</li>
 *   <li>{@code PATCH  /room/{id}/status} 状态变更 (状态机校验)</li>
 * </ul>
 *
 * <p>路由: 走 platform-gateway /room/** 规则.</p>
 *
 * @author csyh fusion W3.1
 * @since 2026-06-18
 */
@Tag(name = "园区房屋", description = "park-space 业务 - 房源管理")
@RequiredArgsConstructor
@RestController
@RequestMapping("/room")
public class RoomController {

    private final RoomService roomService;

    @Operation(summary = "分页查询房源列表")
    @GetMapping("/page")
    public Result<PageResult<Room>> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "roomType", required = false) String roomType,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return roomService.page(keyword, roomType, status, pageNum, pageSize);
    }

    @Operation(summary = "查询房源详情")
    @GetMapping("/{id}")
    public Result<Room> getById(@PathVariable Long id) {
        return roomService.getById(id);
    }

    @Operation(summary = "新增房源")
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return roomService.create(params);
    }

    @Operation(summary = "更新房源字段 (不含状态)")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return roomService.update(id, params);
    }

    @Operation(summary = "删除房源 (软删除)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return roomService.delete(id);
    }

    @Operation(summary = "状态变更 (状态机校验)")
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return roomService.updateStatus(id, status);
    }
}
