package com.cloudhub.platform.enterprise.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.domain.entity.EnterpriseIndustry;
import com.cloudhub.platform.enterprise.service.EnterpriseIndustryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 企业行业类型 Controller (V54)
 *
 * @author Sisyphus (csyh 迁移)
 */
@Tag(name = "企业行业类型")
@RestController
@RequestMapping("/enterprise/industry")
@RequiredArgsConstructor
public class EnterpriseIndustryController {

    private final EnterpriseIndustryService service;

    @Operation(summary = "分页查询行业类型")
    @GetMapping("/page")
    public Result<IPage<EnterpriseIndustry>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        IPage<EnterpriseIndustry> page = service.pageList(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size),
                keyword, category, status);
        return Result.ok(page);
    }

    @Operation(summary = "按 ID 查询")
    @GetMapping("/{id}")
    public Result<EnterpriseIndustry> getById(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @Operation(summary = "按 code 查询")
    @GetMapping("/code/{code}")
    public Result<EnterpriseIndustry> getByCode(@PathVariable String code) {
        return Result.ok(service.getByCode(code));
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result<EnterpriseIndustry> save(@RequestBody EnterpriseIndustry entity) {
        return Result.ok(service.save(entity));
    }

    @Operation(summary = "更新")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody EnterpriseIndustry entity) {
        entity.setId(id);
        return Result.ok(service.update(entity));
    }

    @Operation(summary = "软删")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(service.deleteById(id));
    }
}
