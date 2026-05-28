package com.cloudhub.platform.ops.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Tag(name = "系统监控", description = "各服务健康状态聚合")
@RequiredArgsConstructor
@RestController
@RequestMapping("/ops/monitor")
public class MonitorController {

    private static final Map<String, String[]> HEALTH_ENDPOINTS = new LinkedHashMap<>();

    static {
        HEALTH_ENDPOINTS.put("platform-user", new String[]{"http://platform-user:8081/user/v3/api-docs"});
        HEALTH_ENDPOINTS.put("platform-auth", new String[]{"http://platform-auth:8082/auth/v3/api-docs"});
        HEALTH_ENDPOINTS.put("platform-workflow", new String[]{"http://platform-workflow:8084/workflow/v3/api-docs"});
        HEALTH_ENDPOINTS.put("platform-message", new String[]{"http://platform-message:8085/message/site/unread-count"});
        HEALTH_ENDPOINTS.put("platform-ops", new String[]{"http://localhost:8087/ops/v3/api-docs"});
    }

    private final RestTemplate restTemplate;

    @Operation(summary = "获取所有服务健康状态")
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", System.currentTimeMillis());

        Map<String, CompletableFuture<Map<String, Object>>> futures = new LinkedHashMap<>();
        for (Entry<String, String[]> entry : HEALTH_ENDPOINTS.entrySet()) {
            String name = entry.getKey();
            String[] urls = entry.getValue();
            futures.put(name, CompletableFuture.supplyAsync(() -> checkService(name, urls)));
        }

        Map<String, Object> details = new LinkedHashMap<>();
        boolean allUp = true;
        for (Entry<String, CompletableFuture<Map<String, Object>>> entry : futures.entrySet()) {
            Map<String, Object> info = entry.getValue().join();
            details.put(entry.getKey(), info);
            if (!"UP".equals(info.get("status"))) allUp = false;
        }
        result.put("details", details);
        result.put("status", allUp ? "UP" : "DEGRADED");
        return result;
    }

    private Map<String, Object> checkService(String name, String[] urls) {
        for (String url : urls) {
            try {
                restTemplate.getForObject(url, String.class);
                return Map.of("status", "UP", "url", url);
            } catch (Exception ignored) {}
        }
        return Map.of("status", "DOWN", "url", String.join(", ", urls), "error", "所有端点不可达");
    }
}
