package com.cloudhub.platform.enterprise.cloud.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.cloud.model.EnterpriseOverview;
import com.cloudhub.platform.enterprise.cloud.service.EnterpriseOverviewService;
import com.cloudhub.platform.park.common.security.context.LoginContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 企业概览 Controller (Overview)
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Tag(name = "企业概览 (Overview)")
@RestController
@RequestMapping("/enterprise/overview")
@RequiredArgsConstructor
public class EnterpriseOverviewController {

    private final EnterpriseOverviewService overviewService;

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public Result<Page<EnterpriseOverview>> page(
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return overviewService.page(enterpriseId, keyword, pageNum, pageSize);
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public Result<EnterpriseOverview> detail(@PathVariable Long id) {
        return overviewService.detail(id);
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result<EnterpriseOverview> create(@RequestBody EnterpriseOverview e) {
        return overviewService.create(e, LoginContextHolder.getUsername());
    }

    @Operation(summary = "更新")
    @PutMapping("/{id}")
    public Result<EnterpriseOverview> update(@PathVariable Long id, @RequestBody EnterpriseOverview e) {
        e.setId(id);
        return overviewService.update(e, LoginContextHolder.getUsername());
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return overviewService.delete(id);
    }
}