package com.cloudhub.platform.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Set;

@Configuration
public class MybatisPlusConfig implements MetaObjectHandler {

    private static final Set<String> IGNORE_TABLES = Set.of(
        "flyway_schema_history", "flyway_schema_history_message",
        "flyway_schema_history_ops", "flyway_schema_history_workflow",
        "sys_tenant", "sys_message", "sys_message_channel",
        "sys_message_template", "sys_message_record",
        "sys_menu", "sys_role_menu", "sys_user_role", "sys_user_menu",
        "sys_dept", "sys_post", "sys_storage_config",
        "sys_gateway_route", "sys_app",
        "sys_oper_log", "sys_login_log"
    );

    /**
     * 多租户拦截器 Bean · 启用版本（默认行为）<br>
     * 通过 {@code platform.tenant.interceptor.enabled=true} 控制，缺失时默认 {@code true}。<br>
     * 关闭时回落到 {@link #mybatisPlusInterceptorDisabled()}（仅保留分页拦截器）。<br>
     * 决策依据: 见 {@code doc/P0-1-回滚开关设计.md} 方案 A。
     *
     * @since 2026-06-04
     */
    @Bean
    @ConditionalOnProperty(name = "platform.tenant.interceptor.enabled", havingValue = "true", matchIfMissing = true)
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContextHolder.getTenantId();
                // 决策 2026-06-04: 无租户上下文时使用 9999 (主规划 §5.1.4 思路)
                //   优点: 后台任务/消息消费者无需显式设租户, SQL 不会"裸奔"
                //   约束: sys_user 等业务表不应有 tenant_id=9999 的数据 (见 TC-08)
                //   配套: TC-08 启动期自检: SELECT 1 FROM sys_user WHERE tenant_id = 9999 → 0 行
                return tenantId != null ? new LongValue(tenantId) : new LongValue(9999L);
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 决策 2026-06-04 (方案 C): 不再区分 tenantId 是否为 null
                //   原因: 旧逻辑 null 时 return true 会旁路整个拦截器 (业务表 9999 也不过滤), 与主规划 §5.1.4 冲突
                //   新逻辑: 一律按 IGNORE_TABLES 判断
                //     - 业务表 (不在 IGNORE_TABLES): 永远被过滤 (有上下文用 tenantId, 无上下文用 9999)
                //     - 平台/审计表 (在 IGNORE_TABLES): 永远不过滤
                return IGNORE_TABLES.contains(tableName);
            }
        }));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 多租户拦截器 Bean · 禁用版本（紧急回滚）<br>
     * 当 {@code platform.tenant.interceptor.enabled=false} 时激活。<br>
     * 仅保留分页拦截器，多租户过滤被旁路，业务回到 v3.1 之前行为。<br>
     * <b>警告</b>: 此 Bean 激活时无任何租户隔离，存在越权风险，仅用于紧急止血。
     *
     * @since 2026-06-04
     */
    @Bean
    @ConditionalOnProperty(name = "platform.tenant.interceptor.enabled", havingValue = "false")
    public MybatisPlusInterceptor mybatisPlusInterceptorDisabled() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 仅保留分页拦截器，避免关闭后分页功能异常
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 自定义雪花 ID 标识符生成器。
     * <p>
     * 显式注入 workerId / datacenterId, 避免 MP 3.5.5 默认 {@link DefaultIdentifierGenerator}
     * 使用固定 (0, 0) 导致多服务同集群生成 ID 时仅依赖时间戳+sequence 区分 (4096/ms 上限)。
     * <p>
     * 优先级: 环境变量 &gt; yml &gt; 默认 0/0 (兼容旧行为)。
     * <p>
     * 多机部署时务必通过环境变量为每台机器/每个服务分配唯一 (workerId, datacenterId):
     * <ul>
     *   <li>MYBATIS_PLUS_SNOWFLAKE_WORKER_ID: 0-31, 每服务/每实例不同</li>
     *   <li>MYBATIS_PLUS_SNOWFLAKE_DATACENTER_ID: 0-31, 同集群相同</li>
     * </ul>
     */
    @Bean
    public IdentifierGenerator identifierGenerator(
            @Value("${mybatis-plus.snowflake.worker-id:0}") long workerId,
            @Value("${mybatis-plus.snowflake.datacenter-id:0}") long datacenterId) {
        if (workerId < 0 || workerId > 31) {
            throw new IllegalArgumentException(
                "mybatis-plus.snowflake.worker-id must be 0-31, got: " + workerId);
        }
        if (datacenterId < 0 || datacenterId > 31) {
            throw new IllegalArgumentException(
                "mybatis-plus.snowflake.datacenter-id must be 0-31, got: " + datacenterId);
        }
        return new DefaultIdentifierGenerator(workerId, datacenterId);
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
