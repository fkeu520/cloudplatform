package com.cloudhub.platform.gateway.filter;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 网关限流过滤器 (V2 - Redis 滑动窗口)
 * <p>每客户端 IP 独立计数, 基于 Redis Lua 原子操作, 多实例部署时计数一致.</p>
 * <p>V1 (单节点内存版) 保留: 当 Redis 不可达或 {@code gateway.rate-limit.enabled=false} 时
 * 自动降级到 V1, 保证高可用.</p>
 * <p>配置项 (Nacos common.yml):
 * <ul>
 *   <li>{@code gateway.rate-limit.enabled} (默认 true): 启用 Redis 限流; false 走老逻辑</li>
 *   <li>{@code gateway.rate-limit.max-per-minute} (默认 200): 每分钟最大请求数</li>
 *   <li>{@code gateway.rate-limit.window-ms} (默认 60000): 窗口大小 ms</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    /** 默认配置 (Nacos 缺失时降级) */
    private static final int DEFAULT_MAX_PER_MINUTE = 200;
    private static final long DEFAULT_WINDOW_MS = 60_000L;
    private static final boolean DEFAULT_ENABLED = true;

    @Value("${gateway.rate-limit.enabled:" + DEFAULT_ENABLED + "}")
    private boolean enabled;

    @Value("${gateway.rate-limit.max-per-minute:" + DEFAULT_MAX_PER_MINUTE + "}")
    private int maxPerMinute;

    @Value("${gateway.rate-limit.window-ms:" + DEFAULT_WINDOW_MS + "}")
    private long windowMs;

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private RedisScript<Long> rateLimitScript;

    /** V1 单节点内存降级 (Redis 不可达或 enabled=false 时使用) */
    private final Map<String, IpCounter> ipCounters = new ConcurrentHashMap<>();

    public RateLimitFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        // 加载 Lua 脚本
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/rate_limit.lua")));
        script.setResultType(Long.class);
        this.rateLimitScript = script;
        log.info("[RateLimit] 已加载 Lua 脚本, enabled={}, maxPerMinute={}, windowMs={}",
                enabled, maxPerMinute, windowMs);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = getClientIp(exchange.getRequest().getRemoteAddress());
        if (clientIp == null) {
            return chain.filter(exchange);
        }

        if (!enabled) {
            // 灰度: 走老逻辑 (单节点内存版)
            return doMemoryLimit(exchange, clientIp, chain);
        }

        // Redis 滑动窗口
        String key = "rate:ip:" + clientIp;
        long now = System.currentTimeMillis();
        String reqId = UUID.randomUUID().toString();

        List<String> keys = Collections.singletonList(key);
        List<String> args = java.util.Arrays.asList(
                String.valueOf(now),
                String.valueOf(windowMs),
                String.valueOf(maxPerMinute),
                reqId
        );

        return redisTemplate.execute(rateLimitScript, keys, args)
                .next()  // Flux<Long> → Mono<Long>
                .defaultIfEmpty(1L)
                .flatMap(allowed -> {
                    if (allowed != null && allowed == 1L) {
                        return chain.filter(exchange);
                    }
                    log.warn("[RateLimit] IP: {} 请求超过限制 ({}r/{}ms), 拒绝",
                            clientIp, maxPerMinute, windowMs);
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    return exchange.getResponse().setComplete();
                })
                .onErrorResume(err -> {
                    // Redis 不可达 → 自动降级到内存版 + 告警
                    log.error("[RateLimit] Redis 异常, 降级到内存版: {}", err.getMessage());
                    return doMemoryLimit(exchange, clientIp, chain);
                });
    }

    /**
     * V1 单节点内存版限流 (降级路径 / 灰度开关关闭)
     */
    private Mono<Void> doMemoryLimit(ServerWebExchange exchange, String clientIp, GatewayFilterChain chain) {
        IpCounter c = ipCounters.computeIfAbsent(clientIp, k -> new IpCounter());
        long now = System.currentTimeMillis();

        long currentStart = c.windowStart.get();
        if (now - currentStart > windowMs) {
            if (c.windowStart.compareAndSet(currentStart, now)) {
                c.counter.reset();
            }
        }
        c.counter.increment();
        if (c.counter.sum() > maxPerMinute) {
            log.warn("[RateLimit-内存] IP: {} 请求超过限制 ({}r/{}ms)", clientIp, maxPerMinute, windowMs);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(InetSocketAddress remoteAddress) {
        if (remoteAddress == null) return null;
        return remoteAddress.getAddress().getHostAddress();
    }

    @Override
    public int getOrder() {
        return -90;
    }

    /** 单 IP 限流状态 (V1 内存版) */
    private static class IpCounter {
        final LongAdder counter = new LongAdder();
        final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
    }
}
