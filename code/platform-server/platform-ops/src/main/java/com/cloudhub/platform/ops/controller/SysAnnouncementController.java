package com.cloudhub.platform.ops.controller;

import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.ops.domain.entity.SysAnnouncement;
import com.cloudhub.platform.ops.service.SysAnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统公告 Controller (W3 首页工作台公告卡片)
 *
 * <p>W3 阶段: 仅 /announcement/recent 端点.
 * W4 阶段: 加 CRUD + App.vue 管理页.</p>
 */
@Tag(name = "系统公告", description = "首页工作台公告卡片数据源")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/announcement")
public class SysAnnouncementController {

    private final SysAnnouncementService announcementService;

    /**
     * 查询最近公告 (工作台公告卡片)
     *
     * <p>逻辑:
     * <ul>
     *   <li>appCode 不传 → 返回全局公告 (app_code IS NULL)</li>
     *   <li>appCode='system' → 返回全局 + system 应用公告</li>
     *   <li>tenantId 自动从 TenantContextHolder 取 (TenantFilter 已注入)</li>
     * </ul>
     * </p>
     *
     * @param appCode 可选, 当前选中的应用编码
     * @param limit   可选, 默认 3, 最大 50 (服务端自动 clamp)
     */
    @Operation(summary = "查询最近公告 (工作台公告卡片)")
    @GetMapping("/recent")
    public Result<List<SysAnnouncement>> recent(
            @RequestParam(required = false) String appCode,
            @RequestParam(required = false, defaultValue = "3") int limit) {
        Long tenantId = TenantContextHolder.getTenantId();
        return Result.ok(announcementService.recent(appCode, tenantId, limit));
    }
}