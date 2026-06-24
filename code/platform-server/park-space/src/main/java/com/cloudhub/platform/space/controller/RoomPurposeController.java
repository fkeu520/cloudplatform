package com.cloudhub.platform.space.controller;

import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.space.domain.entity.RoomPurpose;
import com.cloudhub.platform.space.service.RoomPurposeService;
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
 * 房源用途 Controller (park-space 业务)
 * <p>W3.6 阶段: 房源用途 (RoomPurpose) 简单 CRUD 端点.</p>
 */
@Tag(name = "房源用途", description = "park-space 业务 - 房源用途字典")
@RequiredArgsConstructor
@RestController
@RequestMapping("/room-purpose")
public class RoomPurposeController {

    private final RoomPurposeService roomPurposeService;

    @Operation(summary = "分页查询房源用途")
    @GetMapping("/page")
    public Result<PageResult<RoomPurpose>> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return roomPurposeService.page(keyword, status, pageNum, pageSize);
    }

    @Operation(summary = "查询房源用途详情")
    @GetMapping("/{id}")
    public Result<RoomPurpose> getById(@PathVariable Long id) {
        return roomPurposeService.getById(id);
    }

    @Operation(summary = "新增房源用途")
    @PostMapping
    public Result<Long> create(@RequestBody Map<String, Object> params) {
        return roomPurposeService.create(params);
    }

    @Operation(summary = "更新房源用途")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return roomPurposeService.update(id, params);
    }

    @Operation(summary = "删除房源用途 (软删除)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return roomPurposeService.delete(id);
    }
}