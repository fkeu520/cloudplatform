package com.cloudhub.platform.common.config;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;

/**
 * 数据权限 MyBatis-Plus 拦截器 (M5 P0-2 实施)
 *
 * 配套: doc/M5-P0-2-实施子任务.md
 *
 * <h2>职责</h2>
 * 读取 {@link DataScopeContextHolder} 中的 SQL 片段, 拼接到原 SQL 的 WHERE 子句末尾
 *
 * <h2>实现方式</h2>
 * 实现 MyBatis-Plus 的 {@link InnerInterceptor} 接口, 在 beforePrepare 阶段改写 BoundSql.sql
 *
 * <h2>示例</h2>
 * <pre>{@code
 *   原 SQL: SELECT * FROM sys_user WHERE deleted = 0
 *   片段:   " AND u.id = 5"
 *   改写:  SELECT * FROM sys_user WHERE deleted = 0 AND u.id = 5
 * }</pre>
 *
 * <h2>限制 (M5 起步版)</h2>
 * <ul>
 *   <li>只处理简单 SELECT (PlainSelect), 不处理 UNION / 子查询</li>
 *   <li>假设 SQL 必有 WHERE 子句 (MyBatis-Plus selectList 默认会加 deleted = 0)</li>
 *   <li>SQL 解析失败时**不抛异常**, 仅记 WARN 日志, 保留原 SQL (避免破坏业务)</li>
 * </ul>
 *
 * <h2>拦截器顺序 (重要!)</h2>
 * <pre>
 *   1. TenantLineInnerInterceptor  (P0-1: 拼 tenant_id)
 *   2. DataScopeInnerInterceptor   (M5:   拼 data_scope 片段)  ← 本类
 *   3. PaginationInnerInterceptor  (拼 LIMIT/OFFSET)
 * }</pre>
 *
 * @since 2026-06-04
 */
@Slf4j
public class DataScopeInnerInterceptor implements InnerInterceptor {

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        // 1. 取 SQL 片段 (从 DataScopeAspect 写入)
        String fragment = DataScopeContextHolder.get();
        if (fragment == null || fragment.isEmpty()) {
            // 无 data_scope 限制, 放行
            return;
        }

        // 2. 取原 SQL
        BoundSql boundSql = sh.getBoundSql();
        String originalSql = boundSql.getSql();

        // 3. 解析 SQL + 拼片段
        String newSql;
        try {
            newSql = injectFragment(originalSql, fragment);
        } catch (JSQLParserException e) {
            // 解析失败: 记 WARN, 放行原 SQL (安全降级)
            log.warn("DataScope SQL parse failed, original SQL kept. fragment=[{}] sql=[{}]",
                    fragment, originalSql, e);
            // 仍 clear, 防止同一 fragment 被反复用于其他 SQL
            DataScopeContextHolder.clear();
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("DataScope SQL rewrite: {} -> {}", originalSql, newSql);
        }

        // 4. 反射设置新 SQL 到 BoundSql
        MetaObject metaObject = SystemMetaObject.forObject(boundSql);
        metaObject.setValue("sql", newSql);

        // 5. 关键: 改写后立即 clear, 防止业务方法内的"子查询"被错误改写
        // 业务方法 (如 UserService.list) 可能先调 userMapper.selectList, 然后 toUserVO 内部
        // 再调 menuMapper.selectByUserId。后者表无 id 列, 改写会失败。
        // 策略: Aspect 设 fragment, 第一个 mapper 改写后立即清空, 后续 mapper 不再被改写。
        // 局限: 业务方法只对**第一个** mapper 生效 (主业务 mapper 通常就是第一个)。
        DataScopeContextHolder.clear();
    }

    /**
     * 将 fragment 拼接到 SQL 的 WHERE 子句末尾
     * 策略: 找最后一个 WHERE, 后面加 AND fragment
     * 简化: 假设 SQL 必有 WHERE (MyBatis-Plus 默认)
     */
    private String injectFragment(String originalSql, String fragment) throws JSQLParserException {
        Statement stmt = CCJSqlParserUtil.parse(originalSql);
        if (!(stmt instanceof Select)) {
            return originalSql;
        }
        Select select = (Select) stmt;
        if (!(select.getSelectBody() instanceof PlainSelect)) {
            return originalSql;
        }
        PlainSelect plain = (PlainSelect) select.getSelectBody();

        // 解析 fragment 为 Expression
        Expression fragmentExpr = CCJSqlParserUtil.parseExpression(fragment.trim());

        Expression where = plain.getWhere();
        if (where == null) {
            plain.setWhere(fragmentExpr);
        } else {
            // WHERE (where AND fragment) - 改写为 and(where, fragment)
            plain.setWhere(new AndExpression(where, fragmentExpr));
        }

        return select.toString();
    }
}
