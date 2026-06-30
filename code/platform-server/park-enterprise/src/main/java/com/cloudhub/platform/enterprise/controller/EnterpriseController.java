package com.cloudhub.platform.enterprise.controller;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.domain.entity.Enterprise;
import com.cloudhub.platform.enterprise.service.EnterpriseService;
import com.cloudhub.platform.park.common.security.context.LoginContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 企业档案 Controller (park-enterprise Phase 1)
 *
 * <p>csyh EnterpriseController 改造 — 平台 {@code Result<T>} + 租户拦截器自动注入.
 * <p>Phase 1 提供列表 + 详情 + 新增 + 更新 + 删除 + 启停. 云企库对接留 Phase 3.
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-06-30
 */
@Tag(name = "企业档案", description = "park-enterprise 业务 - 企业档案 CRUD")
@RestController
@RequestMapping("/enterprise")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    @Operation(summary = "分页查询企业列表")
    @GetMapping("/page")
    public Result<?> page(@RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize) {
        return enterpriseService.page(keyword, status, pageNum, pageSize);
    }

    @Operation(summary = "企业详情")
    @GetMapping("/{id}")
    public Result<Enterprise> detail(@PathVariable Long id) {
        return enterpriseService.detail(id);
    }

    @Operation(summary = "新增企业")
    @PostMapping
    public Result<Enterprise> create(@RequestBody Enterprise e) {
        return enterpriseService.create(e, LoginContextHolder.getUsername());
    }

    @Operation(summary = "更新企业")
    @PutMapping("/{id}")
    public Result<Enterprise> update(@PathVariable Long id, @RequestBody Enterprise e) {
        e.setId(id);
        return enterpriseService.update(e, LoginContextHolder.getUsername());
    }

    @Operation(summary = "逻辑删除企业")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return enterpriseService.delete(id);
    }

    @Operation(summary = "启用/停用企业")
    @PatchMapping("/{id}/status")
    public Result<Void> setStatus(@PathVariable Long id,
                                  @RequestParam Integer status) {
        return enterpriseService.setStatus(id, status);
    }
}
