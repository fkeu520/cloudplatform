package com.cloudhub.platform.park.common.base.util;

import org.slf4j.helpers.MessageFormatter;

import java.util.Locale;

/**
 * 国际化工具 (csyh cn.flyrise.common.core.utils.I18nUtil 翻译)
 *
 * <p>csyh 出现 177 次, 用于业务文案国际化. 翻译策略:
 * 委托 slf4j MessageFormatter (支持占位符) + Java ResourceBundle, 业务代码改 import 即可.</p>
 *
 * <p>使用示例 (csyh 风格兼容):
 * <pre>{@code
 * String msg = I18nUtil.getMessage("room.not.found", new Object[]{roomId}, locale);
 * throw new CommonException(I18nUtil.getMessage("room.not.found", roomId));
 * }</pre>
 * </p>
 *
 * <p><b>W2 阶段</b>: 仅占位符替换 (无 i18n 文件). 直接传中文 message.
 * <b>W3 阶段</b>: 接入 platform-common 的 i18n 配置, 按 Locale 查 i18n bundle.</p>
 *
 * @author csyh fusion W2.1
 * @since 2026-06-18
 */
public final class I18nUtil {

    private I18nUtil() {}

    /** 默认 Locale (中文) */
    public static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    /** W3 阶段 i18n bundle 路径 (暂未实现) */
    private static final String BUNDLE_BASE_NAME = "i18n.park-messages";

    /**
     * 获取本地化消息 (无占位符, 默认 Locale)
     *
     * @param key 消息 key
     * @return 消息文本
     */
    public static String getMessage(String key) {
        return getMessage(key, DEFAULT_LOCALE);
    }

    /**
     * 获取本地化消息 (无占位符, 指定 Locale)
     */
    public static String getMessage(String key, Locale locale) {
        return format(key, null, locale);
    }

    /**
     * 获取本地化消息 (带占位符, 默认 Locale)
     *
     * @param key  消息 key
     * @param args 占位符参数
     * @return 格式化后消息
     */
    public static String getMessage(String key, Object... args) {
        return getMessage(key, args, DEFAULT_LOCALE);
    }

    /**
     * 获取本地化消息 (带占位符, 指定 Locale)
     */
    public static String getMessage(String key, Object[] args, Locale locale) {
        return format(key, args, locale);
    }

    /**
     * 内部: W2 阶段用 key 作为消息, W3 阶段用 ResourceBundle 查
     */
    private static String format(String key, Object[] args, Locale locale) {
        // W3 阶段: ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale).getString(key)
        // W2 阶段: 直接把 key 当 message, 用 slf4j 格式化占位符
        if (args == null || args.length == 0) {
            return key;
        }
        return MessageFormatter.arrayFormat(key, args).getMessage();
    }
}
