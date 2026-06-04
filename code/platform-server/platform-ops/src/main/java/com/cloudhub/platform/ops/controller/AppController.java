package com.cloudhub.platform.ops.controller;

import com.cloudhub.platform.common.annotation.Log;
import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.ops.service.AppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "应用管理", description = "系统应用/模块定义")
@RequiredArgsConstructor
@RestController
@RequestMapping("/app")
public class AppController {

    private final AppService appService;

    @Operation(summary = "分页查询应用")
    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer appType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(appService.page(keyword, appType, pageNum, pageSize));
    }

    @Operation(summary = "查询所有应用")
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) Integer appType) {
        return Result.ok(appService.list(appType));
    }

    @Operation(summary = "根据ID查询")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.ok(appService.getById(id));
    }

    @Log(title = "应用管理", businessType = 1)
    @Operation(summary = "新增应用")
    @PostMapping
    public Result<Void> create(@RequestBody Map<String, Object> params) {
        appService.create(params);
        return Result.ok();
    }

    @Log(title = "应用管理", businessType = 2)
    @Operation(summary = "更新应用")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        appService.update(id, params);
        return Result.ok();
    }

    @Log(title = "应用管理", businessType = 3)
    @Operation(summary = "删除应用")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        appService.delete(id);
        return Result.ok();
    }
}
