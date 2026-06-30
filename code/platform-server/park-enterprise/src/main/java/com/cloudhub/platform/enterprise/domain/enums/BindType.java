package com.cloudhub.platform.enterprise.domain.enums;

import lombok.Getter;

/**
 * 企业绑定类型 (park-enterprise)
 *
 * <p>csyh EnterpriseBindingStatus enum 重写 — 平台用字符串 (MySQL VARCHAR(32)),不用 enum 类型
 *    是因为需要扩展性 (后续可能加 office/custom 等),且 VARCHAR 索引更灵活。
 *
 * <p>注意:此 enum 在 service 层做边界校验;DB 列约束不在此保证。
 */
@Getter
public enum BindType {

    PARK("park", "园区"),
    BUILDING("building", "楼栋"),
    TENANT("tenant", "租户"),
    ROOM("room", "房间");

    private final String code;
    private final String description;

    BindType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static BindType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (BindType t : values()) {
            if (t.code.equalsIgnoreCase(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown BindType code: " + code);
    }
}
