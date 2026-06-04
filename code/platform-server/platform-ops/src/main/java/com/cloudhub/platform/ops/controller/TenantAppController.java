package com.cloudhub.platform.ops.controller;

import com.cloudhub.platform.common.annotation.Log;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.ops.service.TenantAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "租户应用授权", description = "租户可用的应用/模块授权")
@RequiredArgsConstructor
@RestController
@RequestMapping("/tenant-app")
public class TenantAppController {

    private final TenantAppService tenantAppService;

    @Operation(summary = "获取租户已授权的应用ID列表")
    @GetMapping("/{tenantId}/appIds")
    public Result<List<Long>> getAuthorizedAppIds(@PathVariable Long tenantId) {
        return Result.ok(tenantAppService.getAuthorizedAppIds(tenantId));
    }

    @Log(title = "租户应用授权", businessType = 2)
    @Operation(summary = "为租户授权应用（全量覆盖）")
    @PostMapping("/{tenantId}/authorize")
    public Result<Void> authorizeApps(@PathVariable Long tenantId, @RequestBody List<Long> appIds) {
        tenantAppService.authorizeApps(tenantId, appIds);
        return Result.ok();
    }
}
