package com.cloudhub.platform.common.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 灰度开关 Prometheus 指标 (gray-release-infrastructure PR6)
 * <p>
 * 把 platform.* 灰度开关当前值暴露为 Prometheus gauge, 接入现有
 * /actuator/prometheus 端点 (已被 Prometheus 抓取).
 * <p>
 * 指标命名:
 * <ul>
 *   <li>gray_switch_state{key="..."} 1.0 = enabled, 0.0 = disabled</li>
 *   <li>gray_switch_percent{key="..."} 维度比例 0-100 (PR5)</li>
 * </ul>
 * <p>
 * 抓取示例 (PromQL):
 * <pre>
 *   gray_switch_state{key="platform.tenant.interceptor.enabled"}
 *   gray_switch_state{key="platform.data-scope.upgrade.enabled"} == 1
 * </pre>
 *
 * @author cloudhub
 * @since 2026-06-27 (gray-release-infrastructure PR6)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GrayMetrics {

    private final MeterRegistry meterRegistry;
    private final PlatformToggleProperties toggleProperties;

    @PostConstruct
    public void registerGauges() {
        // ============ M4 P0-1 多租户拦截器 ============
        Gauge.builder("gray_switch_state", toggleProperties,
                        props -> props.getTenant().getInterceptor().isEnabled() ? 1.0 : 0.0)
                .description("Gray release switch state (1=enabled, 0=disabled)")
                .tag("key", "platform.tenant.interceptor.enabled")
                .register(meterRegistry);

        // ============ M5 P0-2 数据权限升级 ============
        Gauge.builder("gray_switch_state", toggleProperties,
                        props -> props.getDataScope().getUpgrade().isEnabled() ? 1.0 : 0.0)
                .description("Gray release switch state (1=enabled, 0=disabled)")
                .tag("key", "platform.data-scope.upgrade.enabled")
                .register(meterRegistry);

        Gauge.builder("gray_switch_state", toggleProperties,
                        props -> props.getDataScope().getUpgrade().isWriteStrict() ? 1.0 : 0.0)
                .description("Gray release switch state (1=enabled, 0=disabled)")
                .tag("key", "platform.data-scope.upgrade.write-strict")
                .register(meterRegistry);

        // ============ gray-release PR5 维度灰度 ============
        Gauge.builder("gray_switch_percent", toggleProperties,
                        props -> (double) (props.getDataScope().getUpgrade().getPercent() == null
                                ? 100 : props.getDataScope().getUpgrade().getPercent()))
                .description("Gray release percent (0-100, PR5 dimension=percent)")
                .register(meterRegistry);

        log.info("GrayMetrics: 已注册 4 个 gauge (gray_switch_state × 3 + gray_switch_percent × 1)");
    }
}