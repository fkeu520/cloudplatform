package com.cloudhub.platform.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Slf4j
@Configuration
public class SwaggerConfig {

    @Bean
    @Primary
    public SwaggerUiConfigProperties.SwaggerUrl[] swaggerUrls() {
        log.info("[Swagger] 配置聚合文档: auth, user, workflow, message, ops, gateway, park-space, park-property, park-contract");
        return List.of(
                url("认证中心", "/auth/v3/api-docs"),
                url("用户中心", "/user/v3/api-docs"),
                url("流程引擎", "/workflow/v3/api-docs"),
                url("消息中心", "/message/v3/api-docs"),
                url("运营中心", "/ops/v3/api-docs"),
                url("网关管理", "/gateway/v3/api-docs"),
                url("园区空间", "/park-space/v3/api-docs"),
                url("园区资产", "/park-property/v3/api-docs"),
                url("园区合同", "/park-contract/v3/api-docs")
        ).toArray(new SwaggerUiConfigProperties.SwaggerUrl[0]);
    }

    private static SwaggerUiConfigProperties.SwaggerUrl url(String name, String url) {
        SwaggerUiConfigProperties.SwaggerUrl swaggerUrl = new SwaggerUiConfigProperties.SwaggerUrl();
        swaggerUrl.setName(name);
        swaggerUrl.setUrl(url);
        return swaggerUrl;
    }
}
