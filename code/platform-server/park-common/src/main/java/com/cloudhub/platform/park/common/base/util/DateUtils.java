package com.cloudhub.platform.park.common.base.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 日期工具 (csyh cn.flyrise.common.utils.DateUtils 翻译)
 *
 * <p>csyh 出现 70 次, 提供 LocalDateTime / LocalDate / 时间戳 互转.
 * 翻译策略: 委托 java.time (Java 8+, 云枢默认), 常用 pattern 常量化.</p>
 *
 * <p>使用示例 (csyh 风格兼容):
 * <pre>{@code
 * LocalDateTime now = DateUtils.now();
 * String s = DateUtils.format(now);                    // 2026-06-18 10:00:00
 * String s2 = DateUtils.format(now, "yyyy-MM-dd");     // 2026-06-18
 * LocalDateTime t = DateUtils.parse("2026-06-18 10:00:00");
 * }</pre>
 * </p>
 *
 * @author csyh fusion W2.1
 * @since 2026-06-18
 */
public final class DateUtils {

    private DateUtils() {}

    /** 标准日期时间格式 */
    public static final String PATTERN_DATETIME = "yyyy-MM-dd HH:mm:ss";

    /** 标准日期格式 */
    public static final String PATTERN_DATE = "yyyy-MM-dd";

    /** 标准时间格式 */
    public static final String PATTERN_TIME = "HH:mm:ss";

    /** 紧凑格式 (无分隔符) */
    public static final String PATTERN_COMPACT = "yyyyMMddHHmmss";

    private static final DateTimeFormatter DTF_DATETIME = DateTimeFormatter.ofPattern(PATTERN_DATETIME);
    private static final DateTimeFormatter DTF_DATE = DateTimeFormatter.ofPattern(PATTERN_DATE);
    private static final DateTimeFormatter DTF_TIME = DateTimeFormatter.ofPattern(PATTERN_TIME);
    private static final DateTimeFormatter DTF_COMPACT = DateTimeFormatter.ofPattern(PATTERN_COMPACT);

    // ========== now ==========

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    public static LocalDate today() {
        return LocalDate.now();
    }

    // ========== format ==========

    public static String format(LocalDateTime dt) {
        return dt == null ? null : dt.format(DTF_DATETIME);
    }

    public static String format(LocalDateTime dt, String pattern) {
        if (dt == null) return null;
        return dt.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static String format(LocalDate d) {
        return d == null ? null : d.format(DTF_DATE);
    }

    public static String format(LocalDate d, String pattern) {
        if (d == null) return null;
        return d.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static String formatCompact(LocalDateTime dt) {
        return dt == null ? null : dt.format(DTF_COMPACT);
    }

    // ========== parse ==========

    public static LocalDateTime parse(String text) {
        if (text == null || text.isEmpty()) return null;
        try {
            return LocalDateTime.parse(text, DTF_DATETIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static LocalDateTime parse(String text, String pattern) {
        if (text == null || text.isEmpty()) return null;
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static LocalDate parseDate(String text) {
        if (text == null || text.isEmpty()) return null;
        try {
            return LocalDate.parse(text, DTF_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // ========== 组合 ==========

    public static LocalDateTime of(LocalDate d, LocalTime t) {
        return LocalDateTime.of(d, t);
    }

    public static LocalDateTime ofDateAndTime(String date, String time) {
        LocalDate d = parseDate(date);
        LocalTime t = LocalTime.parse(time, DTF_TIME);
        return of(d, t);
    }
}
