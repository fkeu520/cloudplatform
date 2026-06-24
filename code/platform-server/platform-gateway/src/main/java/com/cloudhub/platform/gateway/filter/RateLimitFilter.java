package com.cloudhub.platform.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 简单限流过滤器（单节点内存版，按 IP 独立计数）
 * <p>每个客户端 IP 独立计数，单 IP 恶意请求不会影响其他用户.
 * 注意：多实例部署时各节点独立计数，生产环境请使用 Redis 版滑动窗口限流.</p>
 */
@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    /** 每 IP 每分钟最大请求数 */
    private static final int MAX_REQUESTS_PER_MINUTE = 200;

    /** 每 IP 独立计数器 (IP → Counter) */
    private final Map<String, IpCounter> ipCounters = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = getClientIp(exchange.getRequest().getRemoteAddress());
        if (clientIp == null) {
            return chain.filter(exchange);
        }

        IpCounter c = ipCounters.computeIfAbsent(clientIp, k -> new IpCounter());
        long now = System.currentTimeMillis();
        long window = 60_000L;

        // 原子性重置窗口
        long currentStart = c.windowStart.get();
        if (now - currentStart > window) {
            if (c.windowStart.compareAndSet(currentStart, now)) {
                c.counter.reset();
            }
        }

        c.counter.increment();
        if (c.counter.sum() > MAX_REQUESTS_PER_MINUTE) {
            log.warn("[RateLimit] IP: {} 请求超过限制 ({}r/min)", clientIp, MAX_REQUESTS_PER_MINUTE);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    /**
     * 获取客户端真实 IP (优先取 X-Forwarded-For)
     */
    private String getClientIp(InetSocketAddress remoteAddress) {
        if (remoteAddress == null) return null;
        return remoteAddress.getAddress().getHostAddress();
    }

    @Override
    public int getOrder() {
        return -90;
    }

    /** 单 IP 限流状态 */
    private static class IpCounter {
        final LongAdder counter = new LongAdder();
        final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
    }
}