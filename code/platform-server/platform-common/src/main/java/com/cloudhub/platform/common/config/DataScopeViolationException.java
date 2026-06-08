package com.cloudhub.platform.common.config;

/**
 * 数据权限违规异常 (M5 P0-2 PR4 D+2.5)
 *
 * <p>当 write-strict=true 时, DataScopeInnerInterceptor 解析失败 / 检测到禁止语法时抛出。
 * 触发此异常会阻止 SQL 执行, 防止越权写操作 (fail-closed)。</p>
 *
 * <p>使用场景:</p>
 * <ul>
 *   <li>业务代码使用 MySQL FORCE/USE/IGNORE INDEX 提示 (jsqlparser 4.6 不支持)</li>
 *   <li>SQL 解析失败 (语法不兼容)</li>
 *   <li>其他未预期的语句类型</li>
 * </ul>
 *
 * <p>处理建议:</p>
 * <ul>
 *   <li>开发环境: 修复代码, 移除禁用语法</li>
 *   <li>生产环境紧急: 设置 write-strict=false, 记 WARN 放行 (安全降级)</li>
 * </ul>
 *
 * @since 2026-06-08 (M5 PR4 D+2.5)
 */
public class DataScopeViolationException extends RuntimeException {

    public DataScopeViolationException(String message) {
        super(message);
    }

    public DataScopeViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
