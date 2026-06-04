package com.cloudhub.platform.common.config;

/**
 * 数据权限 SQL 片段 ThreadLocal 上下文
 *
 * 配套: doc/M5-P0-2-实施子任务.md
 *
 * 用法:
 *   - DataScopeAspect 在 @Before 时调用 set() 写入 SQL 片段
 *   - DataScopeInnerInterceptor 在 SQL 拼接时调用 get() 读取
 *   - 业务方法结束后由 Aspect/Interceptor 调 clear() 清理
 *
 * 注意: 与 TenantContextHolder 不同, 此处存的是"已拼接的 SQL 片段"而非租户 ID
 *
 * @since 2026-06-04
 */
public class DataScopeContextHolder {

    private static final ThreadLocal<String> SQL_FRAGMENT = new ThreadLocal<>();

    /**
     * 设置当前请求的数据权限 SQL 片段
     * 示例: " AND u.dept_id = 100" 或 " AND u.id = 5" 或 "" (scope=1 全部)
     */
    public static void set(String fragment) {
        SQL_FRAGMENT.set(fragment);
    }

    /**
     * 获取当前请求的数据权限 SQL 片段
     * @return SQL 片段, 无设置返回 "" (不过滤)
     */
    public static String get() {
        return SQL_FRAGMENT.get() == null ? "" : SQL_FRAGMENT.get();
    }

    /**
     * 清理 ThreadLocal (必须调用, 避免线程复用污染)
     */
    public static void clear() {
        SQL_FRAGMENT.remove();
    }
}
