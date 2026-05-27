package com.cloudhub.platform.ops.controller;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.ops.service.AuditService;
import com.cloudhub.platform.ops.service.LoginLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "日志审计", description = "操作日志/登录日志/ELK日志检索")
@RequiredArgsConstructor
@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditService auditService;
    private final LoginLogService loginLogService;

    @Operation(summary = "操作日志分页查询")
    @GetMapping("/oper-log/page")
    public Result<?> operLogPage(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String operName,
            @RequestParam(required = false) Integer businessType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(auditService.operLogPage(title, operName, businessType, status, startTime, endTime, pageNum, pageSize));
    }

    @Operation(summary = "登录日志分页查询")
    @GetMapping("/login-log/page")
    public Result<?> loginLogPage(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(auditService.loginLogPage(username, status, startTime, endTime, pageNum, pageSize));
    }

    @Operation(summary = "ELK日志检索")
    @GetMapping("/elk/search")
    public Result<?> searchElk(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(auditService.searchElk(keyword, startTime, endTime, from, size));
    }

    @Operation(summary = "登录日志统计")
    @GetMapping("/login-log/stats")
    public Result<?> loginLogStats(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.ok(loginLogService.list(null, null));
    }
}
