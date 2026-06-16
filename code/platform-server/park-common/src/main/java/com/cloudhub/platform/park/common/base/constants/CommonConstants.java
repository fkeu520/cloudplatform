package com.cloudhub.platform.park.common.base.constants;

/**
 * 通用常量 (csyh cn.flyrise.pai.fe.common.constants.CommonConstants 翻译)
 *
 * <p>W2 阶段: 仅枚举 csyh 强引用的 yes/no 常量 + 状态码. 业务模块如有更多常量,
 * 在各自模块的 {@code constants} 包下扩展.</p>
 *
 * <p>使用示例 (csyh 风格兼容):
 * <pre>{@code
 * if (CommonConstants.YES.equals(entity.getEnabled())) { ... }
 * }</pre>
 * </p>
 *
 * @author csyh fusion W2.1
 * @since 2026-06-16
 */
public final class CommonConstants {

    private CommonConstants() {}

    /** 是 */
    public static final String YES = "1";

    /** 否 */
    public static final String NO = "0";

    /** 启用 */
    public static final String ENABLED = "1";

    /** 禁用 */
    public static final String DISABLED = "0";

    /** 删除标记 (逻辑删除) */
    public static final Integer DELETED = 1;

    /** 正常 */
    public static final Integer NORMAL = 0;

    /** 默认页码 */
    public static final int DEFAULT_PAGE_NUM = 1;

    /** 默认每页大小 */
    public static final int DEFAULT_PAGE_SIZE = 10;
}
