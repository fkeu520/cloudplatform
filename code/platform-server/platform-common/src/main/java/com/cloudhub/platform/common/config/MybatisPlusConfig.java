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

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Configuration
public class MybatisPlusConfig implements MetaObjectHandler {

    /** C9: 多租户忽略表 (不含 tenant_id 列) */
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
     */
    @Bean
    @ConditionalOnProperty(name = "platform.tenant.interceptor.enabled", havingValue = "true", matchIfMissing = true)
    public MybatisPlusInterceptor mybatisPlusInterceptor(
            // M5 P0-2 PR4: 写严格开关 (默认 true, fail-closed)
            //   false: SQL 解析失败时记 WARN 放行 (PR1-3 行为, 安全降级)
            //   true:  SQL 解析失败时抛 DataScopeViolationException (fail-closed)
            @Value("${platform.data-scope.upgrade.write-strict:true}") boolean writeStrict) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContextHolder.getTenantId();
                // 无租户上下文时, ignoreTable 会返回 true (跳过过滤)
                // 此处返回 0 仅作占位 (实际不会使用)
                return tenantId != null ? new LongValue(tenantId) : new LongValue(0);
            }

            @Override
            public boolean ignoreTable(String tableName) {
                Long tenantId = TenantContextHolder.getTenantId();
                // 决策 2026-06-04 (用户修正): 无租户上下文时跳过所有过滤
                //   适用于: admin(tenant_id=NULL 运营管理员)、内部接口调用、后台任务
                //   安全: 正常请求会经过 TenantFilter 设置租户上下文
                if (tenantId == null) return true;
                // C9: flyway_ 前缀自动忽略，新增 flyway 表无需手动更新 Set
                if (tableName.startsWith("flyway_")) return true;
                return IGNORE_TABLES.contains(tableName);
            }
        }));
        // M5 P0-2: 数据权限拦截器 (M5 P0-2 实施, 2026-06-04; PR4 扩展写操作 2026-06-08)
        //   顺序: TenantLine → DataScope → Pagination
        //   TenantLine 先拼 tenant_id, DataScope 后拼 data_scope 片段, Pagination 最后拼 LIMIT
        DataScopeInnerInterceptor dataScopeInterceptor = new DataScopeInnerInterceptor();
        dataScopeInterceptor.setWriteStrict(writeStrict);
        interceptor.addInnerInterceptor(dataScopeInterceptor);
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 多租户拦截器 Bean · 禁用版本（紧急回滚）<br>
     * 当 {@code platform.tenant.interceptor.enabled=false} 时激活。<br>
     * 仅保留分页拦截器，多租户过滤被旁路，业务回到 v3.1 之前行为。<br>
     * <b>警告</b>: 此 Bean 激活时无任何租户隔离，存在越权风险，仅用于紧急止血。
     */
    @Bean
    @ConditionalOnProperty(name = "platform.tenant.interceptor.enabled", havingValue = "false")
    public MybatisPlusInterceptor mybatisPlusInterceptorDisabled() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 仅保留分页拦截器，避免关闭后分页功能异常
        // 紧急关闭场景: 也跳过 DataScopeInnerInterceptor, 完全旁路 (回到 v3.1 行为)
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

    /**
     * C5+C9: 启动时打印 IGNORE_TABLES 清单，提示新增模块表需加入忽略列表或加 tenant_id 列
     */
    @PostConstruct
    public void init() {
        log.info("Multi-tenant IGNORE_TABLES ({} tables + flyway_* prefix): {}", IGNORE_TABLES.size(), IGNORE_TABLES);
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
