package com.cloudhub.platform.workflow.datascope;

import com.cloudhub.platform.common.config.DataScopeContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * M5 P0-2 PR3 WorkflowDataScopeProvider 桩单元测试 (2026-06-05)
 * <p>配套: doc/M5-P0-2-实施子任务.md §十 (PR3 启动准备)
 * <h2>覆盖</h2>
 * <ul>
 *   <li>TC-DS-15: WorkflowDataScopeProviderImpl.getContext(userId) 返回 DataScopeContext.none()</li>
 * </ul>
 */
@DisplayName("M5 P0-2 PR3 WorkflowDataScopeProvider 桩测试 (TC-DS-15)")
class WorkflowDataScopeProviderImplTest {

    @Test
    @DisplayName("TC-DS-15: WorkflowDataScopeProviderImpl.getContext 返回 none() (maxDataScope=1)")
    void ds15_workflowProvider_returnsNone() {
        WorkflowDataScopeProviderImpl provider = new WorkflowDataScopeProviderImpl();

        DataScopeContext ctx = provider.getContext(202L);

        assertNotNull(ctx, "getContext 应返回非 null");
        assertEquals(1, ctx.getMaxDataScope(), "桩应返回 maxDataScope=1 (全部)");
    }
}
