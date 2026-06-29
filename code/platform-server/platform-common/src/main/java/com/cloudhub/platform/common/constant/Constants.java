package com.cloudhub.platform.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

/**
 * 通用常量
 */
public class Constants {

    // ========== 通用状态 ==========

    @Getter
    @AllArgsConstructor
    public enum Status {
        DISABLE(0, "禁用"),
        ENABLE(1, "启用");

        private final int code;
        private final String desc;
    }

    @Getter
    @AllArgsConstructor
    public enum YesNo {
        NO(0, "否"),
        YES(1, "是");

        private final int code;
        private final String desc;
    }

    // ========== 删除标记 ==========

    @Getter
    @AllArgsConstructor
    public enum Deleted {
        NORMAL(0, "未删除"),
        DELETED(1, "已删除");

        private final int code;
        private final String desc;
    }

    // ========== Redis Key 前缀 ==========

    @Getter
    @AllArgsConstructor
    public enum RedisKey {
        /** 用户 token */
        USER_TOKEN("user:token:", "用户Token"),
        /** 用户缓存 */
        USER_INFO("user:info:", "用户信息缓存"),
        /** 权限缓存 */
        PERMS("user:perms:", "用户权限缓存"),
        /** 验证码 */
        CAPTCHA("captcha:", "验证码"),
        /** 限流 */
        RATE_LIMIT("rate:limit:", "限流计数器");

        private final String prefix;
        private final String desc;
    }

    // ========== 租户类型 ==========

    @Getter
    @AllArgsConstructor
    public enum TenantType {
        PLATFORM(1, "平台"),
        ENTERPRISE(2, "企业"),
        PERSONAL(3, "个人");

        private final int code;
        private final String desc;
    }

    // ========== 性别 ==========

    @Getter
    @AllArgsConstructor
    public enum Gender {
        UNKNOWN(0, "未知"),
        MALE(1, "男"),
        FEMALE(2, "女");

        private final int code;
        private final String desc;
    }

    // ========== 菜单所属平台 (前台枚举, deploy-time fix) ==========
    //
    // 设计:
    // - 几个平台与代码强绑定 (admin-platform / ops-admin 是两套独立前端 + 后端 endpoint),
    //   平台枚举写在代码 (java enum) 而非 Nacos 配置。
    // - sys_menu.menu_category 列承载实际分类, 此 enum 与 DB 列值一一对应。
    // - 加新平台 (如 'tenant-portal'): 1) 加 enum entry, 2) sys_menu.menu_category = 'tenant-portal'
    //   行 INSERT, 3) 新后端 endpoint 硬编码该 enum.code()。0 个 Nacos config。
    //
    // 关联: KNOWN_ISSUES #36.2 (长期方案), 2026-06-29

    @Getter
    @AllArgsConstructor
    public enum MenuCategory {
        ADMIN("admin", "管理后台 (platform-admin)"),
        OPS_ADMIN("ops-admin", "运营后台 (platform-ops-admin)"),
        COMMON("common", "跨平台通用菜单");

        private final String code;
        private final String desc;
    }
}