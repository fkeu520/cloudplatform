package com.cloudhub.platform.user.controller;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.user.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "参数配置管理", description = "系统参数配置CRUD")
@RequiredArgsConstructor
@RestController
@RequestMapping("/config")
public class ConfigController {

    private final ConfigService configService;

    @Operation(summary = "分页查询参数配置")
    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "configType", required = false) Integer configType,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return Result.ok(configService.page(keyword, configType, pageNum, pageSize));
    }

    @Operation(summary = "根据ID查询参数")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.ok(configService.getById(id));
    }

    @Operation(summary = "根据配置键查询参数")
    @GetMapping("/key/{configKey}")
    public Result<?> getByKey(@PathVariable String configKey) {
        return Result.ok(configService.getByKey(configKey));
    }

    @Operation(summary = "新增参数配置")
    @PostMapping
    public Result<Void> create(@RequestBody Map<String, Object> params) {
        configService.create(params);
        return Result.ok();
    }

    @Operation(summary = "更新参数配置")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        params.put("id", id);
        configService.update(params);
        return Result.ok();
    }

    @Operation(summary = "删除参数配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return Result.ok();
    }
}
