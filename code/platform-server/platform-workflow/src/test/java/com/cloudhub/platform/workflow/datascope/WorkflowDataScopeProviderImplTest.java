package com.cloudhub.platform.workflow.datascope;

import com.cloudhub.platform.common.config.DataScopeContext;
import com.cloudhub.platform.common.config.HttpDataScopeProvider;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * M5+ WorkflowDataScopeProvider 单元测试 (M5+ Provider 真实现, 2026-06-18)
 *
 * <h2>覆盖</h2>
 * <ul>
 *   <li>TC-DS-WH-01: 通过 HTTP 调用 platform-user, 拿到正确 context</li>
 *   <li>TC-DS-WH-02: HTTP 失败 (workflow 部署时 platform-user 不可达) → fallback 到 none()</li>
 * </ul>
 */
@DisplayName("M5+ WorkflowDataScopeProvider 单元测试 (2 TC)")
class WorkflowDataScopeProviderImplTest {

    @Test
    @DisplayName("TC-DS-WH-01: HTTP 调用成功, 拿到正确的 data_scope context")
    void wh01_httpSuccess() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        Map<String, Object> data = new HashMap<>();
        data.put("maxDataScope", 3);
        data.put("userDeptId", 500);
        data.put("customDeptIds", null);
        data.put("childDeptIds", "500,501,502");
        when(restTemplate.getForObject(anyString(), any())).thenReturn(com.cloudhub.platform.common.result.Result.ok(data));

        WorkflowDataScopeProviderImpl provider = new WorkflowDataScopeProviderImpl(restTemplate);

        DataScopeContext ctx = provider.getContext(202L);

        assertNotNull(ctx);
        assertEquals(3, ctx.getMaxDataScope());
        assertEquals(500L, ctx.getUserDeptId());
    }

    @Test
    @DisplayName("TC-DS-WH-02: HTTP 失败 → fallback 到 none() (业务可用性优先)")
    void wh02_httpFailure_fallback() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject(anyString(), any()))
                .thenThrow(new ResourceAccessException("platform-user 不可达"));

        WorkflowDataScopeProviderImpl provider = new WorkflowDataScopeProviderImpl(restTemplate);

        DataScopeContext ctx = provider.getContext(202L);

        assertNotNull(ctx, "降级场景应返回非 null");
        assertEquals(1, ctx.getMaxDataScope(), "降级到 maxDataScope=1 (无限制)");
    }
}
