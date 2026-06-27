package com.cloudhub.platform.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 平台灰度开关统一持有类 (gray-release-infrastructure PR2/4)
 * <p>
 * 所有 platform.* 灰度开关集中此处, 通过 {@link RefreshScope} 支持 Nacos 推送热生效.
 * 切换开关后调用 {@code POST /actuator/refresh} (或 Nacos 自动推送 + spring.cloud.nacos.config.refresh-enabled=true) 即可触发 Bean 重建.
 * <p>
 * 关联:
 * <ul>
 *   <li>doc/log/项目进度.md §灰度基础设施</li>
 *   <li>config/common.yml (单一真源)</li>
 *   <li>MybatisPlusConfig / UserDataScopeProviderImpl (消费者)</li>
 * </ul>
 * <p>
 * 新增灰度开关步骤:
 * <ol>
 *   <li>本类加字段 + getter/setter (Lombok @Data 自动)</li>
 *   <li>config/common.yml 加 yml 段</li>
 *   <li>消费者注入 PlatformToggleProperties 用 getter 取值</li>
 *   <li>(可选) PR3 GrayController 列出, PR4 写审计</li>
 * </ol>
 *
 * @author cloudhub
 * @since 2026-06-27 (gray-release-infrastructure)
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "platform")
public class PlatformToggleProperties {

    /** M4 P0-1 多租户拦截器总开关 */
    private Tenant tenant = new Tenant();

    /** M5 P0-2 数据权限升级开关 */
    private DataScope dataScope = new DataScope();

    @Data
    public static class Tenant {
        /** TenantLineInnerInterceptor 总开关 */
        private Interceptor interceptor = new Interceptor();

        @Data
        public static class Interceptor {
            /**
             * 总开关: 默认 true (拦截器启用).
             * <ul>
             *   <li>true: 装配 TenantLineInnerInterceptor, SQL 自动拼 tenant_id</li>
             *   <li>false: 走 mybatisPlusInterceptorDisabled(), 仅保留分页 (紧急止血)</li>
             * </ul>
             */
            private boolean enabled = true;
        }
    }

    @Data
    public static class DataScope {
        /** M5 P0-2 升级版数据权限 (PR1-4 共用开关) */
        private Upgrade upgrade = new Upgrade();

        @Data
        public static class Upgrade {
            /**
             * 总开关: 默认 false (v7.0 行为).
             * <ul>
             *   <li>false: 走老 DFS (v7.0 行为, 无 CTE, 解析失败放行, Provider=null 退化为无限制)</li>
             *   <li>true: 走新 CTE (v7.1 增强, 跨租户隔离 + 8 个 @DataScope + 3 个 Provider 桩)</li>
             * </ul>
             * 灰度策略: 单服务开启 (docker-compose env) → 监控 → 全量.
             */
            private boolean enabled = false;

            /**
             * 写严格模式 (PR4, 默认 true 安全降级).
             * <ul>
             *   <li>false: SQL 解析失败/禁用语法 (FORCE INDEX) 记 WARN 放行, 业务可用但无 data scope</li>
             *   <li>true:  解析失败抛 DataScopeViolationException, 阻止执行 (写操作零越权)</li>
             * </ul>
             * 紧急回滚: 设环境变量 PLATFORM_DATA_SCOPE_UPGRADE_WRITE_STRICT=false 重启.
             */
            private boolean writeStrict = true;

            // ============ gray-release-infrastructure PR5: 维度灰度 ============

            /**
             * 灰度维度策略. 控制 upgrade.enabled=true 时, 谁真正走新逻辑.
             * <ul>
             *   <li>all: 所有用户/租户 (默认, 与 enabled=true 一致)</li>
             *   <li>tenant: 仅 tenants 白名单内的租户走新逻辑</li>
             *   <li>user: 仅 users 白名单内的用户走新逻辑</li>
             *   <li>percent: 按 userId hash 取模, 落在 [0, percent) 内的走新逻辑 (灰度比例)</li>
             * </ul>
             * 注意: 不在白名单内的仍走老逻辑 (DFS), 保证回退路径不变.
             */
            private String dimension = "all";

            /**
             * 租户白名单 (dimension=tenant 时生效).
             * CSV 格式: "1,2,3", 留空表示不限 (等价 all).
             */
            private String tenants = "";

            /**
             * 用户白名单 (dimension=user 时生效).
             * CSV 格式: "100,101,102", 留空表示不限 (等价 all).
             */
            private String users = "";

            /**
             * 灰度比例 0-100 (dimension=percent 时生效).
             * 例如 percent=10 表示 10% 用户走新逻辑 (基于 userId hash 一致性, 同 user 始终命中同一分支).
             */
            private Integer percent = 100;
        }
    }
}