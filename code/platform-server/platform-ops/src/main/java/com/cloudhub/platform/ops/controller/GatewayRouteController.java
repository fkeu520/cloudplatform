package com.cloudhub.platform.ops.controller;

import com.cloudhub.platform.common.result.Result;
import com.cloudhub.platform.ops.service.GatewayRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "服务网关管理", description = "动态路由管理")
@RequiredArgsConstructor
@RestController
@RequestMapping("/gateway-route")
public class GatewayRouteController {

    private final GatewayRouteService gatewayRouteService;

    @Operation(summary = "分页查询路由")
    @GetMapping("/page")
    public Result<?> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(gatewayRouteService.page(keyword, status, pageNum, pageSize));
    }

    @Operation(summary = "查询所有路由")
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) Integer status) {
        return Result.ok(gatewayRouteService.list(status));
    }

    @Operation(summary = "根据ID查询")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.ok(gatewayRouteService.getById(id));
    }

    @Operation(summary = "新增路由")
    @PostMapping
    public Result<Void> create(@RequestBody Map<String, Object> params) {
        gatewayRouteService.create(params);
        return Result.ok();
    }

    @Operation(summary = "更新路由")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        gatewayRouteService.update(id, params);
        return Result.ok();
    }

    @Operation(summary = "删除路由")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        gatewayRouteService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "切换路由状态")
    @PostMapping("/{id}/toggle-status")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        gatewayRouteService.toggleStatus(id);
        return Result.ok();
    }
}
