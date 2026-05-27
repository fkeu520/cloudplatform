package com.cloudhub.platform.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.user.domain.entity.LoginLog;
import com.cloudhub.platform.user.service.LoginLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "登录日志", description = "登录日志查询")
@RequiredArgsConstructor
@RestController
@RequestMapping("/login-log")
public class LoginLogController {

    private final LoginLogService loginLogService;

    @Operation(summary = "分页查询登录日志")
    @GetMapping("/page")
    public Result<Page<LoginLog>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer userType,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        return Result.ok(loginLogService.page(username, userType, tenantId, status, startTime, endTime, pageNum, pageSize));
    }
}
