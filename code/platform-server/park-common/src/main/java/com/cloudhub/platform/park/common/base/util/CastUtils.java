package com.cloudhub.platform.park.common.base.util;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * 类型转换工具 (csyh cn.flyrise.pai.fe.common.util.CastUtils 翻译)
 * <p>csyh 出现 58 次, 提供 object → 指定类型 的安全转换. 翻译策略:
 * 委托 Hutool 的工具方法, 业务代码改 import 即可.</p>
 * <p>W2 阶段: 仅基础 8 种类型 + 集合判断. 复杂转换 (Map/List/Date) 留 W3 阶段补全.</p>
 * <p>使用示例 (csyh 风格兼容):
 * <pre>{@code
 * Long id = CastUtils.toLong(obj, 0L);
 * String name = CastUtils.toString(obj, "");
 * boolean flag = CastUtils.toBoolean(obj);
 * }</pre>
 * </p>
 */
public final class CastUtils {

    private CastUtils() {}

    public static String toString(Object obj) {
        return obj == null ? null : obj.toString();
    }

    public static String toString(Object obj, String defaultValue) {
        if (obj == null) return defaultValue;
        String s = obj.toString();
        return (s == null || s.isEmpty()) ? defaultValue : s;
    }

    public static Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long l) return l;
        if (obj instanceof Number n) return n.longValue();
        // PC2-5: 对齐 ConvertUtil.toLong, 先 trim 避免前后空格导致 NumberFormatException
        return NumberUtils.toLong(obj.toString().trim());
    }

    public static long toLong(Object obj, long defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Long l) return l;
        if (obj instanceof Number n) return n.longValue();
        // PC2-5: trim 对齐 ConvertUtil
        return NumberUtils.toLong(obj.toString().trim(), defaultValue);
    }

    public static Integer toInt(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer i) return i;
        if (obj instanceof Number n) return n.intValue();
        // PC2-5: trim 对齐 ConvertUtil
        return NumberUtils.toInt(obj.toString().trim());
    }

    public static int toInt(Object obj, int defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Integer i) return i;
        if (obj instanceof Number n) return n.intValue();
        // PC2-5: trim 对齐 ConvertUtil
        return NumberUtils.toInt(obj.toString().trim(), defaultValue);
    }

    public static Double toDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Double d) return d;
        if (obj instanceof Number n) return n.doubleValue();
        // PC2-5: trim 对齐 ConvertUtil
        return NumberUtils.toDouble(obj.toString().trim());
    }

    public static double toDouble(Object obj, double defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Double d) return d;
        if (obj instanceof Number n) return n.doubleValue();
        // PC2-5: trim 对齐 ConvertUtil
        return NumberUtils.toDouble(obj.toString().trim(), defaultValue);
    }

    public static BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return null;
        if (obj instanceof BigDecimal bd) return bd;
        if (obj instanceof Number n) return new BigDecimal(n.toString());
        // PC2-5: trim 对齐 ConvertUtil
        String s = obj.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Boolean toBoolean(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Boolean b) return b;
        return BooleanUtils.toBoolean(obj.toString());
    }

    public static boolean toBoolean(Object obj, boolean defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Boolean b) return b;
        Boolean b = BooleanUtils.toBooleanObject(obj.toString());
        return b != null ? b : defaultValue;
    }

    public static boolean isEmpty(Object obj) {
        if (obj == null) return true;
        if (obj instanceof CharSequence cs) return StringUtils.isBlank(cs);
        if (obj instanceof Collection<?> col) return col.isEmpty();
        if (obj.getClass().isArray()) return java.lang.reflect.Array.getLength(obj) == 0;
        return false;
    }

    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }
}
