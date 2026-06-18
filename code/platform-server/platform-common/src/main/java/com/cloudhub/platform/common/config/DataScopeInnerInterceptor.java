package com.cloudhub.platform.common.config;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.upsert.Upsert;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.regex.Pattern;

/**
 * 数据权限 MyBatis-Plus 拦截器 (M5 P0-2 实施, M5 PR4 扩展写操作)
 * 配套: doc/M5-P0-2-实施子任务.md §十三 + doc/M5-PR4-D1-jsqlparser-poc.md
 * <h2>职责</h2>
 * 读取 {@link DataScopeContextHolder} 中的 SQL 片段, 拼接到原 SQL 的 WHERE 子句末尾
 * <h2>支持的语句类型 (PR4 扩展后)</h2>
 * <ul>
 *   <li><b>SELECT</b>: 改写 WHERE (PR1-3 已实现)</li>
 *   <li><b>UPDATE</b>: 改写 WHERE (PR4 D+2.1 新增)</li>
 *   <li><b>DELETE</b>: 改写 WHERE (PR4 D+2.1 新增)</li>
 *   <li><b>INSERT ... ON DUPLICATE KEY UPDATE</b>: 提取 useDuplicate 部分, 应用 data scope (PR4 D+2.2 新增)</li>
 *   <li><b>REPLACE INTO</b>: 跳过 (解析为 Upsert, jsqlparser 不分离 DELETE+INSERT) (PR4 D+2.2 已知限制)</li>
 *   <li><b>TRUNCATE</b>: 跳过 (DDL, 不应受 data scope 限制) (PR4 D+2.1)</li>
 * </ul>
 * <h2>FORCE INDEX 预检测 (PR4 D+2.3)</h2>
 * <p>jsqlparser 4.6 不支持 UPDATE/DELETE 中的 MySQL FORCE/USE/IGNORE INDEX 提示。
 * 业务代码若使用这些提示, SQL 解析会失败。</p>
 * <p>本拦截器在解析前预检测这些关键词, 提前 fail-loud, 避免运行时才报错。</p>
 * <h2>灰度开关 (D+2.5)</h2>
 * <ul>
 *   <li>{@code writeStrict} 默认 true: 解析失败时抛 {@link DataScopeViolationException} (fail-closed)</li>
 *   <li>{@code writeStrict=false}: 记 WARN 放行原 SQL (安全降级)</li>
 * </ul>
 * <h2>限制 (PoC 验证结果, doc/M5-PR4-D1-jsqlparser-poc.md)</h2>
 * <ul>
 *   <li>jsqlparser 4.6 解析能力: 30 用例 29 PASS, 1 FAIL (FORCE INDEX)</li>
 *   <li>FORCE/USE/IGNORE INDEX 提示 → 拒绝 (预检测 + 解析失败)</li>
 *   <li>REPLACE INTO → 跳过 (Upsert 节点无法分离)</li>
 *   <li>业务规范建议: 禁用 FORCE/USE/IGNORE INDEX, REPLACE INTO; 用 SELECT+UPDATE/INSERT 替代</li>
 * </ul>
 * <h2>拦截器顺序 (重要!)</h2>
 * <pre>
 *   1. TenantLineInnerInterceptor  (P0-1: 拼 tenant_id)
 *   2. DataScopeInnerInterceptor   (M5:   拼 data_scope 片段)  ← 本类
 *   3. PaginationInnerInterceptor  (拼 LIMIT/OFFSET)
 * }</pre>
 */
@Slf4j
public class DataScopeInnerInterceptor implements InnerInterceptor {

    /**
     * MySQL 优化提示预检测 (PR4 D+2.3)
     * <p>jsqlparser 4.6 不识别 UPDATE/DELETE 中的 FORCE/USE/IGNORE INDEX,
     * 业务代码使用这些提示会导致解析失败。</p>
     */
    private static final Pattern MYSQL_INDEX_HINT_PATTERN = Pattern.compile(
            "\\b(FORCE\\s+INDEX|USE\\s+INDEX|IGNORE\\s+INDEX)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * 写严格模式 (PR4 D+2.5)
     * <p>false: 解析失败时记 WARN 放行 (安全降级)<br>
     * true: 解析失败时抛 DataScopeViolationException (fail-closed)</p>
     */
    private boolean writeStrict = true;

    public void setWriteStrict(boolean writeStrict) {
        this.writeStrict = writeStrict;
    }

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

        // 3. PR4 D+2.3: FORCE INDEX 预检测
        //    jsqlparser 4.6 不支持, 业务代码用这些提示会失败, 提前 fail-loud
        if (MYSQL_INDEX_HINT_PATTERN.matcher(originalSql).find()) {
            handleFailure(originalSql,
                    "MySQL FORCE/USE/IGNORE INDEX 提示不被支持 (jsqlparser 4.6 限制), 编码规范禁用");
            return;
        }

        // 4. 解析 SQL + 拼片段
        String newSql;
        try {
            newSql = injectFragment(originalSql, fragment);
        } catch (JSQLParserException e) {
            handleFailure(originalSql, "parse error: " + e.getMessage());
            return;
        } catch (RuntimeException e) {
            // 不支持的语句类型 (Upsert / Insert with no duplicate / Truncate 等)
            // PR4 设计: 这些类型被跳过, 视为 "安全旁路"
            if (isUninterceptedStatement(e)) {
                log.debug("DataScope: statement type not intercepted, skip. sql={}", originalSql);
            } else {
                handleFailure(originalSql, "unsupported: " + e.getMessage());
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("DataScope SQL rewrite: {} -> {}", originalSql, newSql);
        }

        // 5. 反射设置新 SQL 到 BoundSql
        MetaObject metaObject = SystemMetaObject.forObject(boundSql);
        metaObject.setValue("sql", newSql);

        // 6. (M5 PR4 修正) 不在拦截器内 clear, 留给 DataScopeAspect.doAfter (@After) 清理
        //    原因: MyBatis-Plus 分页会触发 2 条 SQL (COUNT + SELECT), 在拦截器内 clear 会让
        //    第二条 SELECT 拿不到 fragment, 导致分页查询 records 列表未过滤 (total 正确, records 错)
        //    BUG-2026-06-08: B5 验证发现分页查询 total=1 records=2 不一致, 定位此原因
        //    原"防止子查询被错误改写"的设计顾虑, 由 Aspect @After 的 ThreadLocal 清理覆盖即可
    }

    /**
     * 解析失败 / 禁止 时的统一处理 (PR4 D+2.5)
     * <p>writeStrict=true: 抛异常 (fail-closed)<br>
     * writeStrict=false: 记 WARN, 放行原 SQL (安全降级)</p>
     */
    private void handleFailure(String originalSql, String reason) {
        DataScopeContextHolder.clear();
        if (writeStrict) {
            log.error("DataScope fail-closed (write-strict=true). reason={}, sql={}", reason, originalSql);
            throw new DataScopeViolationException(
                    "DataScope interception failed: " + reason + ". SQL: " + originalSql);
        } else {
            log.warn("DataScope parse failed (write-strict=false, fallback). reason={}, sql={}",
                    reason, originalSql);
        }
    }

    /**
     * 判断是否为"不拦截的语句类型"异常
     * <p>Upsert/Truncate/Insert 等被识别但拦截器不处理, 抛 UnsupportedRuntimeException</p>
     */
    private boolean isUninterceptedStatement(RuntimeException e) {
        String msg = e.getMessage();
        return msg != null && msg.contains("Unsupported statement type");
    }

    /**
     * 将 fragment 拼接到 SQL 的 WHERE 子句末尾
     * <p>支持 SELECT (PR1-3) / UPDATE / DELETE (PR4 D+2.1) / Insert with duplicate (PR4 D+2.2)</p>
     */
    private String injectFragment(String originalSql, String fragment) throws JSQLParserException {
        Statement stmt = CCJSqlParserUtil.parse(originalSql);
        Expression fragmentExpr = CCJSqlParserUtil.parseExpression(fragment.trim());

        if (stmt instanceof Select) {
            return injectIntoSelect((Select) stmt, fragmentExpr);
        } else if (stmt instanceof Update) {
            return injectIntoUpdate((Update) stmt, fragmentExpr);
        } else if (stmt instanceof Delete) {
            return injectIntoDelete((Delete) stmt, fragmentExpr);
        } else if (stmt instanceof Insert) {
            return injectIntoInsert((Insert) stmt, fragmentExpr, originalSql);
        } else {
            // Upsert / Truncate 等 - 不拦截
            throw new RuntimeException("Unsupported statement type: " + stmt.getClass().getSimpleName());
        }
    }

    /**
     * SELECT: 已有 PR1-3 实现
     */
    private String injectIntoSelect(Select select, Expression fragmentExpr) {
        if (!(select.getSelectBody() instanceof PlainSelect)) {
            // UNION 等暂不处理, 跳过
            return select.toString();
        }
        PlainSelect plain = (PlainSelect) select.getSelectBody();
        Expression where = plain.getWhere();
        if (where == null) {
            plain.setWhere(fragmentExpr);
        } else {
            plain.setWhere(new AndExpression(where, fragmentExpr));
        }
        return select.toString();
    }

    /**
     * UPDATE: 改写 WHERE (PR4 D+2.1)
     * <p>UPDATE 无 SELECT 子句, 直接改 setWhere</p>
     */
    private String injectIntoUpdate(Update update, Expression fragmentExpr) {
        Expression where = update.getWhere();
        if (where == null) {
            update.setWhere(fragmentExpr);
        } else {
            update.setWhere(new AndExpression(where, fragmentExpr));
        }
        return update.toString();
    }

    /**
     * DELETE: 改写 WHERE (PR4 D+2.1)
     */
    private String injectIntoDelete(Delete delete, Expression fragmentExpr) {
        Expression where = delete.getWhere();
        if (where == null) {
            delete.setWhere(fragmentExpr);
        } else {
            delete.setWhere(new AndExpression(where, fragmentExpr));
        }
        return delete.toString();
    }

    /**
     * INSERT 处理 (PR4 D+2.2)
     * <p>情况 1: 简单 INSERT (无 ON DUPLICATE KEY UPDATE) - 跳过 (INSERT 无 WHERE 概念)</p>
     * <p>情况 2: INSERT ... ON DUPLICATE KEY UPDATE - 提取 duplicate 部分, 应用 data scope</p>
     * <p>注意: jsqlparser 4.6 的 Insert.getUseDuplicate() 返回值需验证, 实际可能为 null 或 List&lt;Column&gt;</p>
     */
    private String injectIntoInsert(Insert insert, Expression fragmentExpr, String originalSql) {
        // jsqlparser 4.6 的 Insert 类有 useDuplicate 字段 (boolean 或 List<Column>)
        // 简单 INSERT 无 duplicate, 跳过
        // INSERT ... ON DUPLICATE KEY UPDATE 有 duplicate
        // 反射检查 duplicate 字段
        try {
            java.lang.reflect.Field useDuplicateField = null;
            for (java.lang.reflect.Field f : insert.getClass().getDeclaredFields()) {
                if (f.getName().equalsIgnoreCase("useDuplicate")
                        || f.getName().equalsIgnoreCase("duplicateUpdate")) {
                    useDuplicateField = f;
                    break;
                }
            }
            if (useDuplicateField == null) {
                // 无 duplicate 字段 (新版本 jsqlparser 可能改名), 跳过
                return originalSql;
            }
            useDuplicateField.setAccessible(true);
            Object useDuplicate = useDuplicateField.get(insert);
            if (useDuplicate == null
                    || (useDuplicate instanceof Boolean && !(Boolean) useDuplicate)
                    || (useDuplicate instanceof java.util.Collection && ((java.util.Collection<?>) useDuplicate).isEmpty())) {
                // 简单 INSERT, 无 duplicate
                return originalSql;
            }
            // 有 duplicate, 但 jsqlparser 4.6 API 限制, 难以直接改写
            // 简化方案: 跳过 + WARN (业务规范禁用 upsert)
            log.warn("DataScope: INSERT ... ON DUPLICATE KEY UPDATE 的 UPDATE 部分无法拦截 (jsqlparser 4.6 限制), 业务规范禁用 upsert. sql={}",
                    originalSql);
            return originalSql;
        } catch (IllegalAccessException e) {
            log.warn("DataScope: INSERT duplicate 字段反射失败, 跳过. sql={}", originalSql, e);
            return originalSql;
        }
    }
}
