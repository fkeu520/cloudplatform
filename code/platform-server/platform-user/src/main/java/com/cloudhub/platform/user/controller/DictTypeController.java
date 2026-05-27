package com.cloudhub.platform.user.controller;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.user.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "字典类型管理", description = "字典类型CRUD")
@RequiredArgsConstructor
@RestController
@RequestMapping("/dict/type")
public class DictTypeController {

    private final DictService dictService;

    @Operation(summary = "分页查询字典类型")
    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return Result.ok(dictService.typePage(keyword, status, pageNum, pageSize));
    }

    @Operation(summary = "查询所有字典类型")
    @GetMapping("/list")
    public Result<?> list() {
        return Result.ok(dictService.typeList());
    }

    @Operation(summary = "根据ID查询字典类型")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.ok(dictService.getTypeById(id));
    }

    @Operation(summary = "新增字典类型")
    @PostMapping
    public Result<Void> create(@RequestBody Map<String, Object> params) {
        dictService.createType(params);
        return Result.ok();
    }

    @Operation(summary = "更新字典类型")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        params.put("id", id);
        dictService.updateType(params);
        return Result.ok();
    }

    @Operation(summary = "删除字典类型")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dictService.deleteType(id);
        return Result.ok();
    }
}
