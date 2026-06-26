package com.cloudhub.platform.space.service;

import java.util.EnumSet;
import java.util.Set;

/**
 * 房源状态枚举 (park-space 业务)
 * <p>5 状态机, 有限转换.</p>
 * <p>设计原则:
 * <ul>
 *   <li>代码与 sys_room.status 字段一一对应 (0/1/2/3/4)</li>
 *   <li>状态转换由 {@link #canTransitionTo(RoomStatus)} 严格校验, 防止非法跳转</li>
 *   <li>{@link #SOLD} 是终态, 不可逆转</li>
 * </ul>
 */
public enum RoomStatus {

    /** 空置 (默认) */
    VACANT(0, "空置"),

    /** 已租 */
    RENTED(1, "已租"),

    /** 已售 (终态) */
    SOLD(2, "已售"),

    /** 锁定 */
    LOCKED(3, "锁定"),

    /** 预订 */
    BOOKED(4, "预订");

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
     * <p>状态转换图:
     * <pre>
     *   VACANT ─┬─→ RENTED      (租)
     *           ├─→ SOLD        (售, 终态)
     *           ├─→ LOCKED      (锁)
     *           └─→ BOOKED      (预订)
     *   RENTED ────→ VACANT      (退)
     *   LOCKED ────→ VACANT      (解锁)
     *   BOOKED ─┬─→ RENTED      (预订转租)
     *           └─→ VACANT      (取消)
     * </pre>
     */
    public boolean canTransitionTo(RoomStatus target) {
        if (this == target) return true;
        if (this == SOLD) return false;
        if (target == SOLD) return true;
        return allowedTransitions().contains(target);
    }

    /**
     * 当前状态允许的下一个状态集合 (用于错误提示)
     */
    public Set<RoomStatus> allowedTransitions() {
        switch (this) {
            case VACANT:
                return EnumSet.of(RENTED, SOLD, LOCKED, BOOKED);
            case RENTED:
                return EnumSet.of(VACANT);
            case SOLD:
                return EnumSet.noneOf(RoomStatus.class);
            case LOCKED:
                return EnumSet.of(VACANT);
            case BOOKED:
                return EnumSet.of(RENTED, VACANT);
            default:
                return EnumSet.noneOf(RoomStatus.class);
        }
    }

    @Override
    public String toString() {
        return code + "=" + desc;
    }
}