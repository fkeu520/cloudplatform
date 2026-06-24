package com.cloudhub.platform.park.common.base.util;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * 类型转换工具 (csyh cn.flyrise.common.core.utils.ConvertUtil 翻译)
 * <p>csyh 出现 277 次, 提供 object → 指定类型 的安全转换, 失败返回默认值.
 * 翻译策略: 用 commons-lang3 (云枢已依赖) + Java 反射, 不引入 hutool 减体积.
 * 与 {@link CastUtils} 的差异: ConvertUtil 偏"字符串/Map"场景, CastUtils 偏"基本类型"场景.</p>
 * <p><b>已废弃</b>: 功能已被 {@link CastUtils} 覆盖, 新代码请直接使用 CastUtils.
 * 为兼容 csyh 迁移代码保留本类, 后续将移除.</p>
 * @deprecated 使用 {@link CastUtils} 替代
 * @see CastUtils 基础类型转换 (本类超集)
 */
@Deprecated
public final class ConvertUtil {

    private ConvertUtil() {}

    /**
     * 转 String, null → defaultValue
     */
    public static String toStr(Object obj, String defaultValue) {
        if (obj == null) return defaultValue;
        return obj.toString();
    }

    /**
     * 转 String, null/空 → defaultValue
     */
    public static String toStr(Object obj, String defaultValue, boolean trim) {
        String s = toStr(obj, null);
        if (StringUtils.isBlank(s)) return defaultValue;
        return trim ? s.trim() : s;
    }

    /**
     * 转 long, null/格式错 → defaultValue
     */
    public static long toLong(Object obj, long defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(obj.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 转 Long (装箱, null 友好)
     */
    public static Long toLongObj(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long l) return l;
        if (obj instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(obj.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 转 int, null/格式错 → defaultValue
     */
    public static int toInt(Object obj, int defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 转 Integer (装箱, null 友好)
     */
    public static Integer toIntObj(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer i) return i;
        if (obj instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 转 BigDecimal, null/格式错 → defaultValue
     */
    public static BigDecimal toBigDecimal(Object obj, BigDecimal defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof BigDecimal bd) return bd;
        if (obj instanceof Number n) return new BigDecimal(n.toString());
        try {
            return new BigDecimal(obj.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 转 boolean
     */
    public static boolean toBool(Object obj, boolean defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Boolean b) return b;
        return BooleanUtils.toBoolean(obj.toString());
    }

    /**
     * 判断是否为空 (null / 空字符串 / 空集合 / 空数组 / 空 Map)
     */
    public static boolean isEmpty(Object obj) {
        if (obj == null) return true;
        if (obj instanceof CharSequence cs) return StringUtils.isBlank(cs);
        if (obj instanceof Collection<?> col) return col.isEmpty();
        if (obj instanceof Map<?, ?> map) return map.isEmpty();
        if (obj.getClass().isArray()) return java.lang.reflect.Array.getLength(obj) == 0;
        return false;
    }

    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }
}
