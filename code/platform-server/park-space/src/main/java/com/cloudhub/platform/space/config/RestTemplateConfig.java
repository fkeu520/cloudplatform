package com.cloudhub.platform.space.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 跨服务 HTTP 调用 (调用 platform-user 取昵称)
 * <p>对应 room-split-merge 操作人昵称补全 (Bug: 操作人显示登录账号而非昵称)</p>
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