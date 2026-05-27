package com.cloudhub.platform.gateway.controller;

import com.cloudhub.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Tag(name = "网关路由管理", description = "动态路由刷新")
@RequiredArgsConstructor
@RestController
@RequestMapping("/gateway/route")
public class RouteController {

    private final RouteDefinitionWriter routeDefinitionWriter;

    @Operation(summary = "批量刷新路由")
    @PostMapping("/sync")
    public Mono<Result<Void>> syncRoutes(@RequestBody List<RouteDefinition> routes) {
        return Mono.defer(() -> {
            int count = 0;
            for (RouteDefinition def : routes) {
                try {
                    routeDefinitionWriter.save(Mono.just(def)).subscribe();
                    count++;
                } catch (Exception e) {
                    log.warn("保存路由失败: routeId={}, error={}", def.getId(), e.getMessage());
                }
            }
            log.info("网关路由同步完成: {}条", count);
            return Mono.just(Result.<Void>ok());
        });
    }

    @Operation(summary = "删除路由")
    @DeleteMapping("/{routeId}")
    public Mono<Result<Void>> deleteRoute(@PathVariable String routeId) {
        return routeDefinitionWriter.delete(Mono.just(routeId))
                .then(Mono.just(Result.<Void>ok()))
                .onErrorResume(NotFoundException.class, e -> Mono.just(Result.<Void>ok()));
    }
}
