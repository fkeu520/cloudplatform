package com.cloudhub.platform.ops.datascope;

import com.cloudhub.platform.common.config.DataScopeContext;
import com.cloudhub.platform.common.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * M5+ OpsDataScopeProvider 单元测试 (M5+ Provider 真实现, 2026-06-18)
 *
 * <h2>覆盖</h2>
 * <ul>
 *   <li>TC-DS-OH-01: HTTP 成功</li>
 *   <li>TC-DS-OH-02: HTTP 失败 → fallback</li>
 * </ul>
 */
@DisplayName("M5+ OpsDataScopeProvider 单元测试 (2 TC)")
class OpsDataScopeProviderImplTest {

    @Test
    @DisplayName("TC-DS-OH-01: HTTP 成功")
    void oh01_httpSuccess() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        Map<String, Object> data = new HashMap<>();
        data.put("maxDataScope", 2);
        data.put("userDeptId", 800);
        when(restTemplate.getForObject(anyString(), any())).thenReturn(Result.ok(data));

        OpsDataScopeProviderImpl provider = new OpsDataScopeProviderImpl(restTemplate);

        DataScopeContext ctx = provider.getContext(404L);

        assertEquals(2, ctx.getMaxDataScope());
    }

    @Test
    @DisplayName("TC-DS-OH-02: HTTP 失败 → fallback")
    void oh02_httpFailure_fallback() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject(anyString(), any()))
                .thenThrow(new ResourceAccessException("platform-user 不可达"));

        OpsDataScopeProviderImpl provider = new OpsDataScopeProviderImpl(restTemplate);

        DataScopeContext ctx = provider.getContext(404L);

        assertEquals(1, ctx.getMaxDataScope());
    }
}
