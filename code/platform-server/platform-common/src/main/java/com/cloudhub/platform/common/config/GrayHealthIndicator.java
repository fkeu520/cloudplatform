package com.cloudhub.platform.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 灰度开关健康端点 (gray-release-infrastructure PR6)
 * <p>
 * 暴露在 /actuator/health/gray (Spring Boot Actuator 自动路由)
 * 返回所有 platform.* 灰度开关当前值, 便于:
 * <ul>
 *   <li>Prometheus 抓取 /actuator/health 整体状态</li>
 *   <li>K8s liveness/readiness probe (UP/DOWN 取决于开关是否健康)</li>
 *   <li>curl 快速排查当前灰度状态 (vs UI 登录)</li>
 * </ul>
 * <p>
 * 健康策略 (gray-release PR6):
 * <ul>
 *   <li>所有开关 UP (灰度基础设施就绪) → status: UP</li>
 *   <li>PlatformToggleProperties 注入失败 → status: DOWN (极端情况, 启动时挂)</li>
 * </ul>
 * <p>
 * 注意: 灰度开关本身不做健康判断 (灰度是常态, 不是异常), 但暴露详情.
 *
 * @author cloudhub
 * @since 2026-06-27 (gray-release-infrastructure PR6)
 */
@Slf4j
@Component("gray")
@RequiredArgsConstructor
public class GrayHealthIndicator implements HealthIndicator {

    private final PlatformToggleProperties toggleProperties;

    @Override
    public Health health() {
        try {
            Health.Builder builder = Health.up();

            // M4 P0-1 多租户拦截器
            builder.withDetail("platform.tenant.interceptor.enabled",
                    toggleProperties.getTenant().getInterceptor().isEnabled());

            // M5 P0-2 数据权限升级
            builder.withDetail("platform.data-scope.upgrade.enabled",
                    toggleProperties.getDataScope().getUpgrade().isEnabled());
            builder.withDetail("platform.data-scope.upgrade.write-strict",
                    toggleProperties.getDataScope().getUpgrade().isWriteStrict());

            // gray-release PR5 维度灰度
            builder.withDetail("platform.data-scope.upgrade.dimension",
                    toggleProperties.getDataScope().getUpgrade().getDimension());
            builder.withDetail("platform.data-scope.upgrade.tenants",
                    toggleProperties.getDataScope().getUpgrade().getTenants());
            builder.withDetail("platform.data-scope.upgrade.users",
                    toggleProperties.getDataScope().getUpgrade().getUsers());
            builder.withDetail("platform.data-scope.upgrade.percent",
                    toggleProperties.getDataScope().getUpgrade().getPercent());

            log.debug("GrayHealthIndicator: 健康检查返回 UP, 当前开关已展示");
            return builder.build();
        } catch (Exception e) {
            log.error("GrayHealthIndicator: 健康检查失败", e);
            return Health.down(e).build();
        }
    }
}