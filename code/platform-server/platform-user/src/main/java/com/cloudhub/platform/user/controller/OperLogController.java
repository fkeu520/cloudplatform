package com.cloudhub.platform.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.user.domain.entity.OperLog;
import com.cloudhub.platform.user.service.OperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "操作日志", description = "操作日志查询/清理")
@RequiredArgsConstructor
@RestController
@RequestMapping("/oper-log")
public class OperLogController {

    private final OperLogService operLogService;

    @Operation(summary = "分页查询操作日志")
    @GetMapping("/page")
    public Result<IPage<OperLog>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String operName,
            @RequestParam(required = false) Integer businessType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        QueryWrapper<OperLog> query = new QueryWrapper<>();
        if (title != null && !title.isBlank()) {
            query.like("title", title);
        }
        if (operName != null && !operName.isBlank()) {
            query.like("oper_name", operName);
        }
        if (businessType != null && businessType >= 0) {
            query.eq("business_type", businessType);
        }
        if (status != null && status >= 0) {
            query.eq("status", status);
        }
        if (startTime != null && endTime != null) {
            query.between("oper_time", startTime, endTime);
        }
        query.orderByDesc("oper_time");

        Page<OperLog> page = new Page<>(pageNum, pageSize);
        IPage<OperLog> result = operLogService.page(page, query);
        return Result.ok(result);
    }

    @Operation(summary = "根据ID查询操作日志")
    @GetMapping("/{id}")
    public Result<OperLog> getById(@PathVariable Long id) {
        return Result.ok(operLogService.getById(id));
    }

    @Operation(summary = "删除操作日志")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        operLogService.removeById(id);
        return Result.ok();
    }

    @Operation(summary = "批量删除操作日志")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        operLogService.removeByIds(ids);
        return Result.ok();
    }

    @Operation(summary = "清空操作日志")
    @DeleteMapping("/clear")
    public Result<Void> clear() {
        operLogService.remove(new QueryWrapper<>());
        return Result.ok();
    }
}
