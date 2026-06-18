package com.cloudhub.platform.ops.datascope;

import com.cloudhub.platform.common.config.DataScopeContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * M5 P0-2 PR3 OpsDataScopeProvider 桩单元测试 (2026-06-05)
 * <p>配套: doc/M5-P0-2-实施子任务.md §十 (PR3 启动准备)
 * <h2>覆盖</h2>
 * <ul>
 *   <li>TC-DS-14: OpsDataScopeProviderImpl.getContext(userId) 返回 DataScopeContext.none() (maxDataScope=1)</li>
 * </ul>
 * <h2>设计</h2>
 * <p>纯单元测试 (不启动 Spring), 验证桩实现正确返回 none().
 * <p>Spring 上下文集成由 application 启动 + 业务接口验证覆盖.
 */
@DisplayName("M5 P0-2 PR3 OpsDataScopeProvider 桩测试 (TC-DS-14)")
class OpsDataScopeProviderImplTest {

    @Test
    @DisplayName("TC-DS-14: OpsDataScopeProviderImpl.getContext 返回 none() (maxDataScope=1)")
    void ds14_opsProvider_returnsNone() {
        // 纯单元测试, 不启动 Spring
        OpsDataScopeProviderImpl provider = new OpsDataScopeProviderImpl();

        // 调桩, 应返回 DataScopeContext.none() (maxDataScope=1 = 全部)
        DataScopeContext ctx = provider.getContext(101L);

        assertNotNull(ctx, "getContext 应返回非 null");
        assertEquals(1, ctx.getMaxDataScope(), "桩应返回 maxDataScope=1 (全部)");
    }
}
