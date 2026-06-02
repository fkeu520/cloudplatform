package com.cloudhub.platform.gateway.filter;

import com.cloudhub.platform.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * JWT 鉴权过滤器
 */
@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    /** 无需鉴权的路径 */
    private static final List<String> WHITE_LIST = List.of(
            "/auth/login",
            "/auth/sms/send",
            "/auth/sms/login",
            "/auth/refresh",
            "/auth/public-key",
            "/message/sse",
            "/user/login",
            "/user/register",
            "/user/internal",
            "/actuator/health"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        log.info("[JwtAuth] path={}, method={}", path, exchange.getRequest().getMethod());

        // 白名单直接放行
        if (isWhitePath(path)) {
            log.info("[JwtAuth] 白名单放行: {}", path);
            return chain.filter(exchange);
        }

        log.info("[JwtAuth] 需要鉴权: {}", path);
        // 获取 Token
        String token = getToken(exchange.getRequest());
        if (token == null) {
            log.info("[JwtAuth] 无Token, 返回未登录");
            return unauthorized(exchange, "未登录，请先登录");
        }

        // 验证 Token
        try {
            if (!JwtUtil.validate(token)) {
                log.info("[JwtAuth] Token无效");
                return unauthorized(exchange, "Token无效或已过期");
            }
            String userId = JwtUtil.getUserId(token);
            log.info("[JwtAuth] Token有效, userId={}", userId);
            // 将用户ID传递到后续服务（通过 Header）
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception e) {
            log.warn("JWT 验证失败: {}", e.getMessage());
            return unauthorized(exchange, "Token验证失败");
        }
    }

    @Override
    public int getOrder() {
        return -100; // 优先级最高
    }

    private boolean isWhitePath(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private String getToken(org.springframework.http.server.reactive.ServerHttpRequest request) {
        List<String> headers = request.getHeaders().get("Authorization");
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        String auth = headers.get(0);
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().setComplete();
        return Mono.empty();
    }
}
