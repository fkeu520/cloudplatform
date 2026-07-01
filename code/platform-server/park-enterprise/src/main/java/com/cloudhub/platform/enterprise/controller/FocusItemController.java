package com.cloudhub.platform.enterprise.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.enterprise.domain.entity.FocusItem;
import com.cloudhub.platform.enterprise.service.FocusItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 关注标签内容 Controller (V57)
 *
 * @author Sisyphus (csyh 迁移)
 */
@Tag(name = "关注标签内容")
@RestController
@RequestMapping("/enterprise/focus-item")
@RequiredArgsConstructor
public class FocusItemController {

    private final FocusItemService service;

    @Operation(summary = "分页查询 (按 focusId 筛选)")
    @GetMapping("/page")
    public Result<IPage<FocusItem>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long focusId,
            @RequestParam(required = false) Integer status) {
        IPage<FocusItem> page = service.pageList(new Page<>(current, size), focusId, status);
        return Result.ok(page);
    }

    @Operation(summary = "按 focusId 查询全部启用内容")
    @GetMapping("/by-focus/{focusId}")
    public Result<List<FocusItem>> listByFocusId(@PathVariable Long focusId) {
        return Result.ok(service.listByFocusId(focusId));
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result<FocusItem> save(@RequestBody FocusItem entity) {
        return Result.ok(service.save(entity));
    }

    @Operation(summary = "更新")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody FocusItem entity) {
        entity.setId(id);
        return Result.ok(service.update(entity));
    }

    @Operation(summary = "软删")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(service.deleteById(id));
    }
}
