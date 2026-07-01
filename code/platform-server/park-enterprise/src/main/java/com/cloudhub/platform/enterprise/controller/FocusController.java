package com.cloudhub.platform.enterprise.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.domain.entity.Focus;
import com.cloudhub.platform.enterprise.service.FocusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 关注标签 Controller (V57)
 *
 * @author Sisyphus (csyh 迁移)
 */
@Tag(name = "关注标签")
@RestController
@RequestMapping("/enterprise/focus")
@RequiredArgsConstructor
public class FocusController {

    private final FocusService service;

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public Result<IPage<Focus>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        IPage<Focus> page = service.pageList(new Page<>(current, size), keyword, status);
        return Result.ok(page);
    }

    @Operation(summary = "按 ID 查询")
    @GetMapping("/{id}")
    public Result<Focus> getById(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @Operation(summary = "获取全部启用标签 (供前端下拉)")
    @GetMapping("/enabled")
    public Result<List<Focus>> listEnabled() {
        return Result.ok(service.listEnabled());
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result<Focus> save(@RequestBody Focus entity) {
        return Result.ok(service.save(entity));
    }

    @Operation(summary = "更新")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Focus entity) {
        entity.setId(id);
        return Result.ok(service.update(entity));
    }

    @Operation(summary = "软删")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(service.deleteById(id));
    }
}
