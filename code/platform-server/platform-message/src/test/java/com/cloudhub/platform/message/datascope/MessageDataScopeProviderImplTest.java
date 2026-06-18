package com.cloudhub.platform.message.datascope;

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
 * M5+ MessageDataScopeProvider 单元测试 (M5+ Provider 真实现, 2026-06-18)
 *
 * <h2>覆盖</h2>
 * <ul>
 *   <li>TC-DS-MH-01: HTTP 成功</li>
 *   <li>TC-DS-MH-02: HTTP 失败 → fallback</li>
 * </ul>
 */
@DisplayName("M5+ MessageDataScopeProvider 单元测试 (2 TC)")
class MessageDataScopeProviderImplTest {

    @Test
    @DisplayName("TC-DS-MH-01: HTTP 成功")
    void mh01_httpSuccess() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        Map<String, Object> data = new HashMap<>();
        data.put("maxDataScope", 4);
        data.put("userDeptId", 700);
        when(restTemplate.getForObject(anyString(), any())).thenReturn(Result.ok(data));

        MessageDataScopeProviderImpl provider = new MessageDataScopeProviderImpl(restTemplate);

        DataScopeContext ctx = provider.getContext(303L);

        assertEquals(4, ctx.getMaxDataScope());
    }

    @Test
    @DisplayName("TC-DS-MH-02: HTTP 失败 → fallback")
    void mh02_httpFailure_fallback() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject(anyString(), any()))
                .thenThrow(new ResourceAccessException("platform-user 不可达"));

        MessageDataScopeProviderImpl provider = new MessageDataScopeProviderImpl(restTemplate);

        DataScopeContext ctx = provider.getContext(303L);

        assertEquals(1, ctx.getMaxDataScope());
    }
}
