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
}