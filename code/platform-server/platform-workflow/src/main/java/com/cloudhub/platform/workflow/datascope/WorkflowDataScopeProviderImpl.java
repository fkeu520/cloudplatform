package com.cloudhub.platform.workflow.datascope;

import com.cloudhub.platform.common.config.DataScopeContext;
import com.cloudhub.platform.common.config.DataScopeProvider;
import com.cloudhub.platform.common.config.HttpDataScopeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * platform-workflow 模块 DataScopeProvider (M5+ Provider 真实现, 2026-06-18)
 *
 * <p>配套: doc/M5-P0-2-决策记录.md + doc/M5-P0-2-实施子任务.md</p>
 *
 * <h2>背景</h2>
 * <p>M5 PR3 时 platform-workflow 不依赖 platform-user, 注册 {@code @ConditionalOnMissingBean} 桩
 * (返回 none() = 无 data_scope 限制). 这是个真安全洞 —— workflow 中
 * @DataScope 方法实际不被限制.</p>
 *
 * <h2>本实现</h2>
 * <p>继承 {@link HttpDataScopeProvider}, 通过 RestTemplate + 负载均衡
 * 调用 platform-user 的 {@code GET /user/internal/data-scope/{userId}}.
 * 内置 60s 内存缓存, HTTP 失败降级到 none().</p>
 */
@Slf4j
@Service
public class WorkflowDataScopeProviderImpl extends HttpDataScopeProvider {

    public WorkflowDataScopeProviderImpl(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate) {
        super(restTemplate);
    }
}
