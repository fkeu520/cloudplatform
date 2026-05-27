package com.cloudhub.platform.ops.controller;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.ops.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "对象存储管理", description = "MinIO存储配置管理")
@RequiredArgsConstructor
@RestController
@RequestMapping("/storage")
public class StorageController {

    private final StorageService storageService;

    @Operation(summary = "分页查询存储配置")
    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(storageService.page(keyword, pageNum, pageSize));
    }

    @Operation(summary = "查询所有存储配置")
    @GetMapping("/list")
    public Result<?> list() {
        return Result.ok(storageService.list());
    }

    @Operation(summary = "根据ID查询")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.ok(storageService.getById(id));
    }

    @Operation(summary = "新增存储配置")
    @PostMapping
    public Result<Void> create(@RequestBody Map<String, Object> params) {
        storageService.create(params);
        return Result.ok();
    }

    @Operation(summary = "更新存储配置")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        storageService.update(id, params);
        return Result.ok();
    }

    @Operation(summary = "删除存储配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        storageService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "测试MinIO连接")
    @PostMapping("/{id}/test")
    public Result<Boolean> testConnection(@PathVariable Long id) {
        return Result.ok(storageService.testConnection(id));
    }

    @Operation(summary = "获取Bucket列表")
    @GetMapping("/{id}/buckets")
    public Result<?> listBuckets(@PathVariable Long id) {
        return Result.ok(storageService.listBuckets(id));
    }

    @Operation(summary = "创建Bucket")
    @PostMapping("/{id}/buckets")
    public Result<Void> createBucket(@PathVariable Long id, @RequestParam String bucketName) {
        storageService.createBucket(id, bucketName);
        return Result.ok();
    }
}
