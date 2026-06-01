package com.cloudhub.platform.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class MybatisPlusConfig implements MetaObjectHandler {

    private static final Set<String> IGNORE_TABLES = Set.of(
        "flyway_schema_history", "flyway_schema_history_message",
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
