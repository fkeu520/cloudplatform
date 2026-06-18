package com.cloudhub.platform.park.common.base.util;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import java.util.UUID;

/**
 * ID 生成工具 (csyh cn.flyrise.common.core.utils.IdUtils 翻译)
 * <p>csyh 出现 114 次, 用于业务主键 / 单据号 / 业务流水号. 翻译策略:
 * 雪花 ID 委托 MyBatis-Plus {@link IdWorker} (云枢一致), UUID 用 JDK 原生.</p>
 * <p>使用示例 (csyh 风格兼容):
 * <pre>{@code
 * Long id = IdUtils.snowflakeId();                    // 雪花 ID (19 位)
 * String orderNo = IdUtils.snowflakeIdStr();          // 字符串形式
 * String uuid = IdUtils.uuid();                       // 标准 UUID
 * String shortUuid = IdUtils.simpleUUID();            // 去横线 UUID (32 位)
 * }</pre>
 * </p>
 * <p><b>W3 阶段</b>: 业务单据号 (如 YW + 20260618 + 8 位流水) 需扩展, 暂时用雪花 ID 顶替.</p>
 */
public final class IdUtils {

    private IdUtils() {}

    /**
     * 雪花 ID (Long)
     */
    public static Long snowflakeId() {
        return IdWorker.getId();
    }

    /**
     * 雪花 ID (String, 19 位)
     */
    public static String snowflakeIdStr() {
        return String.valueOf(IdWorker.getId());
    }

    /**
     * 标准 UUID (含横线, 36 位)
     */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * 紧凑 UUID (去横线, 32 位)
     */
    public static String simpleUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
