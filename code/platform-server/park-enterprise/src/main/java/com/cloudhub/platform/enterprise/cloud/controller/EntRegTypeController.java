package com.cloudhub.platform.enterprise.cloud.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.cloud.model.EntRegType;
import com.cloudhub.platform.enterprise.cloud.service.EntRegTypeService;
import com.cloudhub.platform.park.common.security.context.LoginContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 企业注册类型 Controller (EntRegType)
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Tag(name = "企业注册类型 (EntRegType)")
@RestController
@RequestMapping("/enterprise/reg-type")
@RequiredArgsConstructor
public class EntRegTypeController {

    private final EntRegTypeService entRegTypeService;

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public Result<Page<EntRegType>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String parentCode,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return entRegTypeService.page(keyword, parentCode, pageNum, pageSize);
    }

    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public Result<EntRegType> detail(@PathVariable Long id) {
        return entRegTypeService.detail(id);
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result<EntRegType> create(@RequestBody EntRegType e) {
        return entRegTypeService.create(e, LoginContextHolder.getUsername());
    }

    @Operation(summary = "更新")
    @PutMapping("/{id}")
    public Result<EntRegType> update(@PathVariable Long id, @RequestBody EntRegType e) {
        e.setId(id);
        return entRegTypeService.update(e, LoginContextHolder.getUsername());
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return entRegTypeService.delete(id);
    }
}