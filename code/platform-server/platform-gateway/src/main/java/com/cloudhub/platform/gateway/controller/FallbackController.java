package com.cloudhub.platform.gateway.controller;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 网关熔断降级回调 (M6 P3-6)
 *
 * <p>配合 CircuitBreaker filter 的 fallbackUri 使用。
 * 当后端服务熔断/超时时, 网关转发到此控制器返回降级响应。</p>
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/{service}")
    public Mono<ResponseEntity<Map<String, Object>>> fallback(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        String serviceName = path.replaceFirst("/fallback/", "");

        // 取原始异常信息
        Throwable cause = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        String reason = cause != null ? cause.getMessage() : "服务暂时不可用";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 503);
        body.put("message", "服务 [" + serviceName + "] 暂不可用: " + reason);
        body.put("data", null);
        body.put("timestamp", System.currentTimeMillis());

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
