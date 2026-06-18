package com.cloudhub.platform.space.service;

import java.util.EnumSet;
import java.util.Set;

/**
 * 房源状态枚举 (park-space 业务)
 *
 * <p>5 状态机, 4 允许的转换 + 1 终态.</p>
 *
 * <p>设计原则:
 * <ul>
 *   <li>代码与 sys_room.status 字段一一对应 (0/1/2/3)</li>
 *   <li>状态转换由 {@link #canTransitionTo(RoomStatus)} 严格校验, 防止非法跳转</li>
 *   <li>{@link #DISABLED} 是终态, 不可逆转 (如需启用, 创建新房源)</li>
 * </ul>
 *
 * @author csyh fusion W3.1
 * @since 2026-06-18
 */
public enum RoomStatus {

    /** 空置 (默认) */
    VACANT(0, "空置"),

    /** 已租 */
    RENTED(1, "已租"),

    /** 装修中 */
    RENOVATING(2, "装修中"),

    /** 停用 (终态) */
    DISABLED(3, "停用");

    public final int code;
    public final String desc;

    RoomStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public boolean matches(Integer code) {
        return code != null && this.code == code;
    }

    public static RoomStatus fromCode(Integer code) {
        if (code == null) return VACANT;
        for (RoomStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知房源状态: " + code);
    }

    /**
     * 判断当前状态是否可转换到目标状态
     *
     * <p>状态转换图:
     * <pre>
     *   VACANT ─┬─→ RENTED      (租)
     *           └─→ RENOVATING  (装)
     *   RENTED ────→ VACANT      (退)
     *   RENOVATING → VACANT     (完)
     *   任意 ──────→ DISABLED   (管, 终态)
     * </pre>
     */
    public boolean canTransitionTo(RoomStatus target) {
        if (this == target) return true;
        // 终态: 不可离开
        if (this == DISABLED) return false;
        // 任意 → DISABLED 允许
        if (target == DISABLED) return true;
        // 其他转换: 按允许集合判断
        return allowedTransitions().contains(target);
    }

    /**
     * 当前状态允许的下一个状态集合 (用于错误提示)
     */
    public Set<RoomStatus> allowedTransitions() {
        switch (this) {
            case VACANT:
                return EnumSet.of(RENTED, RENOVATING, DISABLED);
            case RENTED:
                return EnumSet.of(VACANT, DISABLED);
            case RENOVATING:
                return EnumSet.of(VACANT, DISABLED);
            case DISABLED:
                return EnumSet.noneOf(RoomStatus.class);  // 终态
            default:
                return EnumSet.noneOf(RoomStatus.class);
        }
    }

    @Override
    public String toString() {
        return code + "=" + desc;
    }
}
