package com.cloudhub.platform.enterprise.cloud.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.cloud.model.CloudData;
import com.cloudhub.platform.enterprise.cloud.service.CloudDataService;
import com.cloudhub.platform.park.common.security.context.LoginContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 云企库数据 Controller (Phase 3, 手动维护)
 *
 * <p>通用 CRUD, 通过 {@code category} 参数区分业务类型.
 * 前端按模块传入对应 category 即可 (business_risk / judicial_risk / knowledge 等).
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Tag(name = "云企库数据 (CloudData)", description = "Phase 3 手动维护 — 按 category 分类通用 CRUD")
@RestController
@RequestMapping("/enterprise/cloud-data")
@RequiredArgsConstructor
public class CloudDataController {

    private final CloudDataService cloudDataService;

    @Operation(summary = "按分类分页查询")
    @GetMapping("/page")
    public Result<Page<CloudData>> page(
            @RequestParam String category,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dataYear,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return cloudDataService.page(category, enterpriseId, keyword, dataYear, pageNum, pageSize);
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public Result<CloudData> detail(@PathVariable Long id) {
        return cloudDataService.detail(id);
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result<CloudData> create(@RequestBody CloudData d) {
        return cloudDataService.create(d, LoginContextHolder.getUsername());
    }

    @Operation(summary = "更新")
    @PutMapping("/{id}")
    public Result<CloudData> update(@PathVariable Long id, @RequestBody CloudData d) {
        d.setId(id);
        return cloudDataService.update(d, LoginContextHolder.getUsername());
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return cloudDataService.delete(id);
    }
}