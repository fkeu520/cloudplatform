package com.cloudhub.platform.park.common.base.util;

import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;

import java.util.Map;

/**
 * Service 层通用工具方法 (消除 requiredLong/requiredString/currentTenantId 跨 Service 重复)
 * <p>park-space 各 Service 广泛使用同样的参数校验 + 租户获取代码段,
 * 统一抽取到此类, 各 Service 直接调用静态方法.</p>
 * <p>使用示例:
 * <pre>{@code
 * Long parkId = ServiceUtils.requiredLong(params, "parkId");
 * String roomNo = ServiceUtils.requiredString(params, "roomNo");
 * Long tenantId = ServiceUtils.currentTenantId();
 * }</pre>
 * </p>
 */
public final class ServiceUtils {

    private ServiceUtils() {}

    /**
     * 从 Map 中取必填 Long 类型参数, 缺失/格式错误抛 BizException
     * @param params 参数 Map
     * @param key    参数名
     * @return 非 null Long 值
     */
    public static Long requiredLong(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) throw new BizException("缺少必填字段: " + key);
        try {
            return Long.valueOf(v.toString().trim());
        } catch (NumberFormatException e) {
            throw new BizException("字段类型错误: " + key);
        }
    }

    /**
     * 从 Map 中取必填 String 类型参数, 缺失/空串抛 BizException
     * @param params 参数 Map
     * @param key    参数名
     * @return 非空 String 值
     */
    public static String requiredString(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null || v.toString().isBlank()) throw new BizException("缺少必填字段: " + key);
        return v.toString().trim();
    }

    /**
     * 获取当前租户 ID (从 platform-common TenantContextHolder 取, 未设置时默认 1L)
     */
    public static Long currentTenantId() {
        Long tid = TenantContextHolder.getTenantId();
        return tid != null ? tid : 1L;
    }
}
