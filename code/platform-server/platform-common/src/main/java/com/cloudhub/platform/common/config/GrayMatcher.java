package com.cloudhub.platform.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 灰度维度匹配器 (gray-release-infrastructure PR5)
 * <p>
 * 根据 {@link PlatformToggleProperties.DataScope.Upgrade} 中的 dimension / tenants / users / percent
 * 决定给定 userId + tenantId 是否走"新逻辑".
 * <p>
 * 4 种维度:
 * <ul>
 *   <li>all: 全部走新逻辑</li>
 *   <li>tenant: tenants 白名单内的走新逻辑</li>
 *   <li>user: users 白名单内的走新逻辑</li>
 *   <li>percent: 按 userId hash 取模, 落在 [0, percent) 内走新逻辑</li>
 * </ul>
 * <p>
 * 关键性质:
 * <ul>
 *   <li><b>确定性</b>: 同一 userId 多次调用结果一致 (基于 hash), 用户感知稳定</li>
 *   <li><b>零配置降级</b>: 白名单空 / percent=100 / dimension=all 都等价于 "all"</li>
 *   <li><b>不影响老逻辑</b>: false 返回时, 调用方继续走老 DFS (PR1 已保留)</li>
 * </ul>
 *
 * @author cloudhub
 * @since 2026-06-27 (gray-release-infrastructure PR5)
 */
@Slf4j
@Component
public class GrayMatcher {

    private final PlatformToggleProperties toggleProperties;

    public GrayMatcher(PlatformToggleProperties toggleProperties) {
        this.toggleProperties = toggleProperties;
    }

    /**
     * 是否走升级版逻辑 (CTE + Provider + @DataScope)
     * <p>
     * 决策树:
     * <pre>
     *   upgrade.enabled = false            → false (老逻辑)
     *   upgrade.enabled = true, dim=all    → true
     *   upgrade.enabled = true, dim=tenant → tenants.contains(tenantId)
     *   upgrade.enabled = true, dim=user   → users.contains(userId)
     *   upgrade.enabled = true, dim=percent → Math.abs(userId.hashCode()) % 100 < percent
     * </pre>
     *
     * @param userId 当前用户 ID (可能为 null, 表示无登录态, 默认走新逻辑 if enabled)
     * @param tenantId 当前租户 ID (可能为 null, 表示 admin/无租户场景)
     */
    public boolean shouldUseUpgrade(Long userId, Long tenantId) {
        var upgrade = toggleProperties.getDataScope().getUpgrade();
        // 总开关关闭 → 永远走老逻辑
        if (!upgrade.isEnabled()) {
            return false;
        }
        String dimension = upgrade.getDimension();
        if (dimension == null || dimension.isBlank() || "all".equalsIgnoreCase(dimension)) {
            return true;
        }
        if ("tenant".equalsIgnoreCase(dimension)) {
            Set<Long> tenantIds = parseCsvLong(upgrade.getTenants());
            if (tenantIds.isEmpty()) {
                log.warn("GrayMatcher: dimension=tenant 但 tenants 白名单为空, 降级为 all");
                return true;
            }
            return tenantId != null && tenantIds.contains(tenantId);
        }
        if ("user".equalsIgnoreCase(dimension)) {
            Set<Long> userIds = parseCsvLong(upgrade.getUsers());
            if (userIds.isEmpty()) {
                log.warn("GrayMatcher: dimension=user 但 users 白名单为空, 降级为 all");
                return true;
            }
            return userId != null && userIds.contains(userId);
        }
        if ("percent".equalsIgnoreCase(dimension)) {
            int percent = upgrade.getPercent() == null ? 100 : upgrade.getPercent();
            if (percent >= 100) return true;
            if (percent <= 0) return false;
            if (userId == null) return false;
            // 用 Long.hashCode() 保证同一 userId 多次调用结果一致
            int hash = Math.abs(Long.hashCode(userId));
            return (hash % 100) < percent;
        }
        log.warn("GrayMatcher: 未知的 dimension={}, 降级为 all", dimension);
        return true;
    }

    /**
     * 解析 CSV 字符串为 Long Set
     * 格式: "1,2,3" 或 " 1 , 2 , 3 ", 空字符串返回空 Set
     */
    private Set<Long> parseCsvLong(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        Set<Long> result = new HashSet<>();
        Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(s -> {
                    try {
                        result.add(Long.parseLong(s));
                    } catch (NumberFormatException e) {
                        log.warn("GrayMatcher: CSV 解析失败, 字段值={}", s);
                    }
                });
        return result;
    }
}