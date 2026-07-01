package com.cloudhub.platform.enterprise.cloud.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.cloud.model.NationalEconomy;
import com.cloudhub.platform.enterprise.cloud.service.NationalEconomyService;
import com.cloudhub.platform.park.common.security.context.LoginContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 国民经济行业分类 Controller (NationalEconomy)
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Tag(name = "国民经济行业分类 (NationalEconomy)")
@RestController
@RequestMapping("/enterprise/national-economy")
@RequiredArgsConstructor
public class NationalEconomyController {

    private final NationalEconomyService nationalEconomyService;

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public Result<Page<NationalEconomy>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) String parentCode,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return nationalEconomyService.page(keyword, level, parentCode, pageNum, pageSize);
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public Result<NationalEconomy> detail(@PathVariable Long id) {
        return nationalEconomyService.detail(id);
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result<NationalEconomy> create(@RequestBody NationalEconomy e) {
        return nationalEconomyService.create(e, LoginContextHolder.getUsername());
    }

    @Operation(summary = "更新")
    @PutMapping("/{id}")
    public Result<NationalEconomy> update(@PathVariable Long id, @RequestBody NationalEconomy e) {
        e.setId(id);
        return nationalEconomyService.update(e, LoginContextHolder.getUsername());
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return nationalEconomyService.delete(id);
    }
}