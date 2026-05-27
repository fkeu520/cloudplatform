package com.cloudhub.platform.user.controller;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.user.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "字典数据管理", description = "字典数据CRUD")
@RequiredArgsConstructor
@RestController
@RequestMapping("/dict/data")
public class DictDataController {

    private final DictService dictService;

    @Operation(summary = "分页查询字典数据")
    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(name = "dictType") String dictType,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return Result.ok(dictService.dataPage(dictType, keyword, pageNum, pageSize));
    }

    @Operation(summary = "根据字典类型查询数据")
    @GetMapping("/type/{dictType}")
    public Result<?> getByType(@PathVariable(name = "dictType") String dictType) {
        return Result.ok(dictService.getDataByType(dictType));
    }

    @Operation(summary = "根据ID查询字典数据")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable(name = "id") Long id) {
        return Result.ok(dictService.getDataById(id));
    }

    @Operation(summary = "新增字典数据")
    @PostMapping
    public Result<Void> create(@RequestBody Map<String, Object> params) {
        dictService.createData(params);
        return Result.ok();
    }

    @Operation(summary = "更新字典数据")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        params.put("id", id);
        dictService.updateData(params);
        return Result.ok();
    }

    @Operation(summary = "删除字典数据")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dictService.deleteData(id);
        return Result.ok();
    }
}
