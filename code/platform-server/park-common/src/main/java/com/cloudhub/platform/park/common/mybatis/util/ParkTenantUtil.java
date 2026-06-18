package com.cloudhub.platform.park.common.mybatis.util;

import com.cloudhub.platform.common.config.TenantContextHolder;

/**
 * 多租户上下文工具 (csyh cn.flyrise.mybatis.util.ThreadLocalUtil / TenantUtil 翻译)
 * <p>csyh 出现 3 次 (ThreadLocalUtil) + 1 次 (TenantUtil).
 * 翻译策略: 完全委托云枢 {@link TenantContextHolder} (M4 P0-1 已完成), 业务代码改 import 即可.</p>
 * <p>W2.2 策略: 零包装, 纯静态方法转发. W3 阶段如需扩展, 增加辅助方法.</p>
 * <p>使用示例 (csyh 风格兼容):
 * <pre>{@code
 * Long tenantId = ParkTenantUtil.getTenantId();
 * ParkTenantUtil.setTenantId(123L);
 * }</pre>
 * </p>
 * @see TenantContextHolder (云枢 M4 已就位)
 */
public final class ParkTenantUtil {

    private ParkTenantUtil() {}

    public static Long getTenantId() {
        return TenantContextHolder.getTenantId();
    }

    public static void setTenantId(Long tenantId) {
        TenantContextHolder.setTenantId(tenantId);
    }

    public static void clear() {
        TenantContextHolder.clear();
    }
}
