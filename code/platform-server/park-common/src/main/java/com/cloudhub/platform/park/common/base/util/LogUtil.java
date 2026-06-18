package com.cloudhub.platform.park.common.base.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志工具 (csyh cn.flyrise.pai.fe.common.util.LogUtil 翻译)
 * <p>csyh 出现 76 次, 提供统一的 info/warn/error 接口 (内部封装 slf4j).
 * 翻译策略: 委托 slf4j, 增加"业务上下文"占位 (W3 阶段接入 LoginContextHolder 后
 * 自动附加 username/tenantId 等).</p>
 * <p>W2 阶段: 仅基础方法. W3 阶段接入 Logback MDC 自动注入 tenantId.</p>
 * <p>使用示例 (csyh 风格兼容):
 * <pre>{@code
 * LogUtil.info("业务模块", "用户 {} 创建了房间 {}", userId, roomId);
 * LogUtil.error("业务模块", "保存失败", e);
 * }</pre>
 * </p>
 */
public final class LogUtil {

    private LogUtil() {}

    /**
     * 业务日志 (info 级别, 含模块名前缀)
     */
    public static void info(String module, String message, Object... args) {
        Logger log = LoggerFactory.getLogger("park." + module);
        log.info("[{}] " + message, prepend(args, module));
    }

    public static void warn(String module, String message, Object... args) {
        Logger log = LoggerFactory.getLogger("park." + module);
        log.warn("[{}] " + message, prepend(args, module));
    }

    public static void error(String module, String message, Object... args) {
        Logger log = LoggerFactory.getLogger("park." + module);
        log.error("[{}] " + message, prepend(args, module));
    }

    public static void error(String module, String message, Throwable t) {
        Logger log = LoggerFactory.getLogger("park." + module);
        log.error("[{}] {}", module, message, t);
    }

    private static Object[] prepend(Object[] args, String module) {
        if (args == null || args.length == 0) {
            return new Object[]{module};
        }
        Object[] result = new Object[args.length + 1];
        result[0] = module;
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }
}
