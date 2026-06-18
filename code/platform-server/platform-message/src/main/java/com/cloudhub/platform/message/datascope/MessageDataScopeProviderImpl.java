package com.cloudhub.platform.message.datascope;

import com.cloudhub.platform.common.config.DataScopeContext;
import com.cloudhub.platform.common.config.DataScopeProvider;
import com.cloudhub.platform.common.config.HttpDataScopeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * platform-message 模块 DataScopeProvider (M5+ Provider 真实现, 2026-06-18)
 *
 * <p>配套: doc/M5-P0-2-决策记录.md + doc/M5-P0-2-实施子任务.md</p>
 *
 * <h2>本实现</h2>
 * <p>继承 {@link HttpDataScopeProvider}, 通过 RestTemplate + 负载均衡
 * 调用 platform-user 的 {@code GET /user/internal/data-scope/{userId}}.
 * 内置 60s 内存缓存, HTTP 失败降级到 none().</p>
 */
@Slf4j
@Service
public class MessageDataScopeProviderImpl extends HttpDataScopeProvider {

    public MessageDataScopeProviderImpl(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate) {
        super(restTemplate);
    }
}
