package com.cloudhub.platform.user.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.user.domain.entity.Room;
import com.cloudhub.platform.user.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 园区房屋 Controller (park-space 业务)
 *
 * <p>W3 阶段入口: 提供 1 个分页端点 {@code GET /room/page}, 为"房源列表"页面服务.
 * 路由: 走 platform-gateway 的 /user/** 规则 (gateway 转发到 platform-user:8081).</p>
 *
 * <p>权限: 业务侧 park-common ParkAuthFilter 写 LoginContextHolder, 可加
 * {@code @RequiresPermissions("room:view")} 控制访问 (W3+ 阶段).</p>
 *
 * @author csyh fusion W3
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
}
