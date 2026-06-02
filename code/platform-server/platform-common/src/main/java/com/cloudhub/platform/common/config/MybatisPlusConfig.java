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

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContextHolder.getTenantId();
                return tenantId != null ? new LongValue(tenantId) : new LongValue(1);
            }

            @Override
            public boolean ignoreTable(String tableName) {
                Long tenantId = TenantContextHolder.getTenantId();
                if (tenantId == null) return true;
                return IGNORE_TABLES.contains(tableName);
            }
        }));
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
