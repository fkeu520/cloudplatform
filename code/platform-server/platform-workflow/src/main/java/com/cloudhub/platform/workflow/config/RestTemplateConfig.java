package com.cloudhub.platform.workflow.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * M5+ Provider 真实现: 供 WorkflowDataScopeProvider 调用 platform-user /internal/data-scope/{userId}
 * <p>配套: M5+ Provider 真实现 (commit 2026-06-18)
 */
@Configuration
public class RestTemplateConfig {

    @Bean("loadBalancedRestTemplate")
    @LoadBalanced
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }
}
