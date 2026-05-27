package com.cloudhub.platform.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简单限流过滤器（单节点内存版）
 * 注意：生产环境请使用 Redis 版滑动窗口限流
 */
@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final int MAX_REQUESTS_PER_MINUTE = 200;

    // 简单滑动窗口计数（生产环境用 Redis）
    private final AtomicInteger counter = new AtomicInteger(0);
    private volatile long windowStart = System.currentTimeMillis();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long now = System.currentTimeMillis();
        long window = 60_000L;

        // 重置窗口
        if (now - windowStart > window) {
            windowStart = now;
            counter.set(0);
        }

        int count = counter.incrementAndGet();
        if (count > MAX_REQUESTS_PER_MINUTE) {
            log.warn("[RateLimit] IP: {} 请求超过限制", exchange.getRequest().getRemoteAddress());
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -90;
    }
}