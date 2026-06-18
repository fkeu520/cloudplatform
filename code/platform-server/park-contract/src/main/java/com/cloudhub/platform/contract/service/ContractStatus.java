package com.cloudhub.platform.contract.service;

/** 合同状态枚举 */
public enum ContractStatus {
    DRAFT(0, "草稿"),
    ACTIVE(1, "已签约"),
    EXPIRED(2, "已到期"),
    TERMINATED(3, "已终止");

    public final int code;
    public final String desc;
    ContractStatus(int code, String desc) { this.code = code; this.desc = desc; }

    public boolean matches(Integer code) { return code != null && this.code == code; }

    public static ContractStatus fromCode(Integer code) {
        if (code == null) return DRAFT;
        for (ContractStatus s : values()) { if (s.code == code) return s; }
        throw new IllegalArgumentException("未知合同状态: " + code);
    }

    /** DRAFT→ACTIVE, ACTIVE→EXPIRED/TERMINATED, EXPIRED/TERMINATED 不可逆转 */
    public boolean canTransitionTo(ContractStatus target) {
        if (this == target) return true;
        if (this == EXPIRED || this == TERMINATED) return false; // 终态
        if (target == TERMINATED) return true;                   // 任意→终止
        // DRAFT→ACTIVE, ACTIVE→EXPIRED
        return this == DRAFT && target == ACTIVE
            || this == ACTIVE && target == EXPIRED;
    }
}