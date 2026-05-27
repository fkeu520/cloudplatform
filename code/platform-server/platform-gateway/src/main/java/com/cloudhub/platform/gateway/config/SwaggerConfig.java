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
        log.info("[Swagger] 配置聚合文档: auth, user");
        return List.of(
                url("认证中心", "/auth/v3/api-docs"),
                url("用户中心", "/user/v3/api-docs")
        ).toArray(new SwaggerUiConfigProperties.SwaggerUrl[0]);
    }

    private static SwaggerUiConfigProperties.SwaggerUrl url(String name, String url) {
        SwaggerUiConfigProperties.SwaggerUrl swaggerUrl = new SwaggerUiConfigProperties.SwaggerUrl();
        swaggerUrl.setName(name);
        swaggerUrl.setUrl(url);
        return swaggerUrl;
    }
}
