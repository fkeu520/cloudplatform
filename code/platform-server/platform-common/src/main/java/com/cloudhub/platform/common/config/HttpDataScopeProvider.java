package com.cloudhub.platform.common.config;

import com.cloudhub.platform.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP DataScopeProvider (M5+ Provider 真实现, 2026-06-18)
 *
 * <p>配套: doc/M5-P0-2-决策记录.md + doc/M5-P0-2-实施子任务.md</p>
 *
 * <h2>背景</h2>
 * <p>platform-workflow / platform-message / platform-ops 不依赖 platform-user,
 * 之前用 {@code @ConditionalOnMissingBean} 桩 (M5 PR3), 返回 none() ——
 * 实际等于不限制 data_scope, 是个安全洞.</p>
 *
 * <h2>方案</h2>
 * <p>本类通过 RestTemplate + 负载均衡调用
 * {@code GET lb://platform-user/user/internal/data-scope/{userId}},
 * 由 platform-user 的 UserDataScopeProviderImpl 真实查询 user/role,
 * 返回 {@link DataScopeContext}.</p>
 *
 * <h2>缓存</h2>
 * <p>用 {@link ConcurrentHashMap} 60s TTL 内存缓存, 避免每次 @DataScope 方法调用
 * 都触发 1 次 HTTP. role 更新时最多 60s 后生效 (业务可接受).</p>
 *
 * <h2>降级</h2>
 * <p>HTTP 调用失败 (user 模块不可用 / 5xx / timeout) → 返回
 * {@link DataScopeContext#none()}, 降级到无 data_scope 限制, 同时记 WARN 日志.
 * 业务可用性优先 (决策原则: 可用性 > 可扩展性 > 性能).</p>
 *
 * <h2>子类用法</h2>
 * <p>非 user 模块的 Provider 改为继承本类, 业务模块只需要注册成 Spring bean:</p>
 * <pre>{@code
 * @Service
 * public class WorkflowDataScopeProviderImpl extends HttpDataScopeProvider {
 *     public WorkflowDataScopeProviderImpl(@Qualifier("loadBalancedRestTemplate") RestTemplate rt) {
 *         super(rt);
 *     }
 * }
 * }</pre>
 */
@Slf4j
public abstract class HttpDataScopeProvider implements DataScopeProvider {

    /** 缓存 key: userId → CacheEntry (DataScopeContext + 创建时间) */
    private static final class CacheEntry {
        final DataScopeContext context;
        final long createdAtMs;
        CacheEntry(DataScopeContext context, long createdAtMs) {
            this.context = context;
            this.createdAtMs = createdAtMs;
        }
    }

    /** 缓存: 60s TTL, ConcurrentHashMap 保证线程安全 (C3: 有界，超限时淘汰过期条目) */
    private static final int MAX_CACHE_SIZE = 10_000;
    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();
    private final RestTemplate restTemplate;

    /** 缓存 TTL, 默认 60s, 可通过 yml platform.data-scope.http.cache-ttl-ms 覆盖 */
    @Value("${platform.data-scope.http.cache-ttl-ms:60000}")
    private long cacheTtlMs;

    public HttpDataScopeProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public DataScopeContext getContext(Long userId) {
        if (userId == null) {
            return DataScopeContext.none();
        }

        // 1. 查缓存
        long now = System.currentTimeMillis();
        CacheEntry entry = cache.get(userId);
        if (entry != null && (now - entry.createdAtMs) < cacheTtlMs) {
            log.debug("DataScope cache HIT: userId={}, maxScope={}", userId, entry.context.getMaxDataScope());
            return entry.context;
        }

        // 2. HTTP 调用 platform-user
        String url = "lb://platform-user/user/internal/data-scope/" + userId;
        try {
            @SuppressWarnings("rawtypes")
            Result result = restTemplate.getForObject(url, Result.class);
            if (result == null || result.getData() == null) {
                log.warn("DataScope HTTP 返回为空, fallback to none. userId={}", userId);
                return DataScopeContext.none();
            }
            // 3. 反序列化 (Result.data 是 LinkedHashMap, 转 DataScopeContext)
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.getData();
            DataScopeContext ctx = DataScopeContext.builder()
                    .maxDataScope(((Number) data.getOrDefault("maxDataScope", 1)).intValue())
                    .userDeptId(data.get("userDeptId") == null ? null : ((Number) data.get("userDeptId")).longValue())
                    .customDeptIds((String) data.get("customDeptIds"))
                    .childDeptIds((String) data.get("childDeptIds"))
                    .build();

            // 4. 写缓存 (C3: 容量超限时先淘汰过期条目)
            putWithEviction(userId, ctx, now);
            log.debug("DataScope cache MISS → HTTP 调用 + 写缓存: userId={}, maxScope={}", userId, ctx.getMaxDataScope());
            return ctx;
        } catch (RestClientException e) {
            log.warn("DataScope HTTP 调用失败 (降级到无限制). userId={}, err={}", userId, e.getMessage());
            return DataScopeContext.none();
        } catch (RuntimeException e) {
            log.warn("DataScope HTTP 反序列化失败 (降级). userId={}, err={}", userId, e.getMessage());
            return DataScopeContext.none();
        }
    }

    /**
     * C3: 有界缓存写入，超限时淘汰过期条目
     */
    private void putWithEviction(Long userId, DataScopeContext ctx, long now) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            long ttl = cacheTtlMs > 0 ? cacheTtlMs : 60_000L;
            cache.values().removeIf(e -> (now - e.createdAtMs) > ttl);
        }
        cache.put(userId, new CacheEntry(ctx, now));
    }

    /**
     * 清除缓存 (运维用, role 变更时可调)
     */
    public void invalidateCache(Long userId) {
        if (userId == null) {
            cache.clear();
        } else {
            cache.remove(userId);
        }
    }

    /**
     * 清空所有缓存
     */
    public void invalidateAll() {
        cache.clear();
    }
}
