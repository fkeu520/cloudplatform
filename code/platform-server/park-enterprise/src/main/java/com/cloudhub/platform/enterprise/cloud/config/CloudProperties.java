package com.cloudhub.platform.enterprise.cloud.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 云企库第三方数据集成配置 (Phase 3)
 *
 * <p>对应 csyh CloudProperties, 简化实现 — 去掉 jasypt 加密, 改用 Spring {@code @ConfigurationProperties}.
 * <p>默认禁用 ({@code platform.cloud.enterprise.enabled=false}), 需部署方设置 API Key 后启用.
 * <p>命名空间选 {@code platform.cloud.*} 而非 {@code cloud.*} — 后者会触发 Spring Cloud Common
 * 的 {@code CloudEnvironmentPostProcessor}, 217 启动时与 MyBatis-Plus 初始化竞争导致
 * {@code SqlSessionFactory} 缺失 (2026-07-01 排错).
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Data
@Component
@ConfigurationProperties("platform.cloud.enterprise")
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
