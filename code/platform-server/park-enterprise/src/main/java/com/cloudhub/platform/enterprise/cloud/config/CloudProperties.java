package com.cloudhub.platform.enterprise.cloud.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 云企库第三方数据集成配置 (Phase 3)
 *
 * <p>对应 csyh CloudProperties, 简化实现 — 去掉 jasypt 加密, 改用 Spring {@code @ConfigurationProperties}.
 * <p>默认禁用 ({@code cloud.enterprise.enabled=false}), 需部署方设置 API Key 后启用.
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Data
@Component
@ConfigurationProperties("cloud.enterprise")
public class CloudProperties {

    /** 全局启用开关 (默认关闭, 灰度切换) */
    private boolean enabled = false;

    /** 云企库 API base URI */
    private String baseUri = "http://link.flyrise.cn/cloud-enterprisedb-api/v2";

    /** OAuth2 token URI */
    private String authUri = "http://link.flyrise.cn/auth-api/oauth/token";

    /** OAuth2 client id */
    private String clientId = "";

    /** OAuth2 client secret */
    private String clientSecret = "";

    /** HTTP read timeout (ms) */
    private int readTimeout = 10000;
}
