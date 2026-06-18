package com.cloudhub.platform.common.config;

import com.cloudhub.platform.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * HttpDataScopeProvider 单元测试 (M5+ Provider 真实现, 2026-06-18)
 *
 * <h2>覆盖</h2>
 * <ul>
 *   <li>TC-DS-H01: HTTP 成功 + 解析正确 → 写缓存 + 返回正确 context</li>
 *   <li>TC-DS-H02: 缓存命中 → 第 2 次调用不发起 HTTP</li>
 *   <li>TC-DS-H03: HTTP 失败 (ResourceAccessException) → fallback 到 none() (无 WARN 升级到 ERROR)</li>
 *   <li>TC-DS-H04: HTTP 返回 null → fallback 到 none()</li>
 *   <li>TC-DS-H05: userId=null → 直接返回 none(), 不发起 HTTP</li>
 *   <li>TC-DS-H06: invalidateCache(userId) 清指定 + invalidateAll 清全部</li>
 * </ul>
 */
@DisplayName("M5+ HttpDataScopeProvider 单元测试 (6 TC)")
class HttpDataScopeProviderTest {

    /** 测试用匿名子类, 暴露父类方法 */
    private static class TestableProvider extends HttpDataScopeProvider {
        TestableProvider(RestTemplate restTemplate) {
            super(restTemplate);
        }
    }

    private RestTemplate restTemplate;
    private TestableProvider provider;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        provider = new TestableProvider(restTemplate);
        // 默认 cacheTtlMs 是 0 (long default, @Value 不在 Spring 上下文生效)
        // 测试中显式设为 60s
        ReflectionTestUtils.setField(provider, "cacheTtlMs", 60_000L);
    }

    @Test
    @DisplayName("TC-DS-H01: HTTP 成功 → 写缓存 + 返回正确 context")
    void h01_httpSuccess() {
        // 模拟 user 端点返回
        Map<String, Object> data = new HashMap<>();
        data.put("maxDataScope", 3);
        data.put("userDeptId", 100);
        data.put("customDeptIds", null);
        data.put("childDeptIds", "100,101,102");
        Result<?> result = Result.ok(data);
        when(restTemplate.getForObject(anyString(), any())).thenReturn(result);

        DataScopeContext ctx = provider.getContext(101L);

        assertNotNull(ctx);
        assertEquals(3, ctx.getMaxDataScope());
        assertEquals(100L, ctx.getUserDeptId());
        assertEquals("100,101,102", ctx.getChildDeptIds());
        verify(restTemplate, times(1)).getForObject(anyString(), any());
    }

    @Test
    @DisplayName("TC-DS-H02: 缓存命中 → 第 2 次调用不发起 HTTP")
    void h02_cacheHit() {
        Map<String, Object> data = new HashMap<>();
        data.put("maxDataScope", 2);
        data.put("userDeptId", 200);
        Result<?> result = Result.ok(data);
        when(restTemplate.getForObject(anyString(), any())).thenReturn(result);

        // 第 1 次: HTTP 调用
        DataScopeContext ctx1 = provider.getContext(101L);
        // 第 2 次: 缓存命中, 不调用 HTTP
        DataScopeContext ctx2 = provider.getContext(101L);

        assertEquals(2, ctx1.getMaxDataScope());
        assertEquals(2, ctx2.getMaxDataScope());
        verify(restTemplate, times(1)).getForObject(anyString(), any());
    }

    @Test
    @DisplayName("TC-DS-H03: HTTP 失败 (ResourceAccessException) → fallback 到 none()")
    void h03_httpFailure_fallback() {
        when(restTemplate.getForObject(anyString(), any()))
                .thenThrow(new ResourceAccessException("platform-user 不可达"));

        DataScopeContext ctx = provider.getContext(101L);

        assertNotNull(ctx, "fallback 也应返回非 null context");
        assertEquals(1, ctx.getMaxDataScope(), "fallback 应是 maxDataScope=1 (无限制)");
        assertNull(ctx.getUserDeptId());
        verify(restTemplate, times(1)).getForObject(anyString(), any());
    }

    @Test
    @DisplayName("TC-DS-H04: HTTP 返回 null → fallback 到 none()")
    void h04_httpNull_fallback() {
        when(restTemplate.getForObject(anyString(), any())).thenReturn(null);

        DataScopeContext ctx = provider.getContext(101L);

        assertEquals(1, ctx.getMaxDataScope());
        verify(restTemplate, times(1)).getForObject(anyString(), any());
    }

    @Test
    @DisplayName("TC-DS-H05: userId=null → 直接返回 none(), 不发起 HTTP")
    void h05_userIdNull() {
        DataScopeContext ctx = provider.getContext(null);

        assertNotNull(ctx);
        assertEquals(1, ctx.getMaxDataScope());
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("TC-DS-H06: invalidateCache / invalidateAll")
    void h06_invalidate() {
        // 先填缓存
        Map<String, Object> data = new HashMap<>();
        data.put("maxDataScope", 1);
        Result<?> result = Result.ok(data);
        when(restTemplate.getForObject(anyString(), any())).thenReturn(result);

        provider.getContext(101L);
        provider.getContext(102L);
        // 验证缓存生效
        provider.getContext(101L);
        provider.getContext(102L);
        verify(restTemplate, times(2)).getForObject(anyString(), any());

        // 清指定 user
        provider.invalidateCache(101L);
        provider.getContext(101L);
        verify(restTemplate, times(3)).getForObject(anyString(), any());

        // 清全部
        provider.invalidateAll();
        provider.getContext(101L);
        provider.getContext(102L);
        verify(restTemplate, times(5)).getForObject(anyString(), any());
    }
}
