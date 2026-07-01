package com.cloudhub.platform.enterprise.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.domain.entity.CustomerInformation;
import com.cloudhub.platform.enterprise.service.CustomerInformationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 客户信息 Controller (V58)
 *
 * <p>50+ 字段, 前端 6 Tab 表单 (基本/业务/IP/财务/需求/物理)。
 *
 * @author Sisyphus (csyh 迁移)
 */
@Tag(name = "客户信息")
@RestController
@RequestMapping("/enterprise/customer")
@RequiredArgsConstructor
public class CustomerInformationController {

    private final CustomerInformationService service;

    @Operation(summary = "分页查询客户信息")
    @GetMapping("/page")
    public Result<IPage<CustomerInformation>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) Integer customerType,
            @RequestParam(required = false) Integer status) {
        IPage<CustomerInformation> page = service.pageList(
                new Page<>(current, size), enterpriseId, customerType, status);
        return Result.ok(page);
    }

    @Operation(summary = "按 ID 查询")
    @GetMapping("/{id}")
    public Result<CustomerInformation> getById(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result<CustomerInformation> save(@RequestBody CustomerInformation entity) {
        return Result.ok(service.save(entity));
    }

    @Operation(summary = "更新")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody CustomerInformation entity) {
        entity.setId(id);
        return Result.ok(service.update(entity));
    }

    @Operation(summary = "软删")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(service.deleteById(id));
    }
}
