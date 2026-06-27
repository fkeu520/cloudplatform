package com.cloudhub.platform.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.common.util.JwtUtil;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.park.common.security.context.LoginContextHolder;
import com.cloudhub.platform.user.domain.entity.OperLog;
import com.cloudhub.platform.user.service.OperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Long tenantId,
            HttpServletRequest request
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
        // 租户过滤：如果传了tenantId则使用，否则从token自动获取
        if (tenantId != null) {
            query.eq("tenant_id", tenantId);
        } else {
            try {
                String auth = request.getHeader("Authorization");
                if (auth != null && auth.startsWith("Bearer ")) {
                    Long tid = JwtUtil.getTenantId(auth.substring(7));
                    if (tid != null && tid > 0) {
                        query.eq("tenant_id", tid);
                    }
                }
            } catch (Exception ignored) {}
        }
        query.orderByDesc("oper_time");

        Page<OperLog> page = new Page<>(pageNum, pageSize);
        // M5 P0-2 PR4: 走 pageList (有 @DataScope 注解) 而非直接 page() 触发按部门过滤
        IPage<OperLog> result = operLogService.pageList(page, query);
        return Result.ok(result);
    }

    @Operation(summary = "根据ID查询操作日志")
    @GetMapping("/{id}")
    public Result<OperLog> getById(@PathVariable Long id) {
        return Result.ok(operLogService.getById(id));
    }

    @Operation(summary = "删除操作日志")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        // U2-7: 租户隔离 + ADMIN 权限校验
        requireAdmin();
        Long tenantId = resolveTenantId(request);
        if (tenantId != null) {
            OperLog log = operLogService.getById(id);
            if (log != null && log.getTenantId() != null && !log.getTenantId().equals(tenantId)) {
                throw new BizException("无权删除其他租户的操作日志");
            }
        }
        operLogService.removeById(id);
        return Result.ok();
    }

    @Operation(summary = "批量删除操作日志")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids, HttpServletRequest request) {
        // U2-7: 租户隔离 + ADMIN 权限校验
        requireAdmin();
        Long tenantId = resolveTenantId(request);
        if (tenantId != null && ids != null && !ids.isEmpty()) {
            List<OperLog> logs = operLogService.listByIds(ids);
            for (OperLog log : logs) {
                if (log.getTenantId() != null && !log.getTenantId().equals(tenantId)) {
                    throw new BizException("无权删除其他租户的操作日志: id=" + log.getId());
                }
            }
        }
        operLogService.removeByIds(ids);
        return Result.ok();
    }

    @Operation(summary = "清空操作日志")
    @DeleteMapping("/clear")
    public Result<Void> clear(HttpServletRequest request) {
        // U2-3: 限制仅 ADMIN + 仅清空当前租户, 不能直接清空全表
        requireAdmin();
        Long tenantId = resolveTenantId(request);
        if (tenantId == null) {
            throw new BizException("清空操作日志必须指定租户上下文");
        }
        operLogService.remove(new QueryWrapper<OperLog>().eq("tenant_id", tenantId));
        return Result.ok();
    }

    /**
     * 要求当前用户是 ADMIN 角色
     */
    private void requireAdmin() {
        if (!LoginContextHolder.getRoles().contains("admin")) {
            throw new BizException("无权操作, 仅管理员可执行");
        }
    }

    /**
     * 从 JWT 解析当前租户 ID
     */
    private Long resolveTenantId(HttpServletRequest request) {
        Long tid = LoginContextHolder.getTenantId();
        if (tid != null && tid > 0) return tid;
        try {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                Long parsed = JwtUtil.getTenantId(auth.substring(7));
                if (parsed != null && parsed > 0) return parsed;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
