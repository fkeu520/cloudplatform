package com.cloudhub.platform.park.common.base.response;

import com.cloudhub.platform.common.result.Result;

/**
 * 园区统一响应工厂 (csyh cn.flyrise.common.core.domain.Reply 翻译)
 * <p>csyh 出现 583 次, 是最常用的响应包装类. 翻译策略:
 * 提供静态方法委托 {@link Result} (云枢), 业务代码改 import 即可.
 * 为什么不继承 Result? —— Java 静态方法不参与多态, 继承的 static 方法返回父类实例而非子类,
 * 强制继承会破坏调用方预期 ({@code R.ok()} 必须返回 R 实例). 改为 final + 静态方法转发,
 * 是 csyh 业务代码迁移代价最小且类型安全的方案.</p>
 * <p>csyh 风格兼容示例:
 * <pre>{@code
 * Result<UserVO> r = R.ok(userVo);
 * Result<List<RoomVO>> r = R.ok(rooms);
 * Result<Void> r = R.error("操作失败");
 * }</pre>
 * </p>
 * @see com.cloudhub.platform.common.result.Result 云枢统一响应
 */
public final class R {

    private R() {}

    // ========== 成功 ==========

    public static <T> Result<T> ok() {
        return Result.ok();
    }

    public static <T> Result<T> ok(T data) {
        return Result.ok(data);
    }

    public static <T> Result<T> ok(T data, String message) {
        return Result.ok(data, message);
    }

    // ========== 失败 ==========

    public static <T> Result<T> error(String message) {
        return Result.error(message);
    }

    public static <T> Result<T> error(int code, String message) {
        return Result.error(code, message);
    }

    // ========== 业务异常快捷方法 ==========

    public static <T> Result<T> badRequest(String message) {
        return Result.badRequest(message);
    }

    public static <T> Result<T> unauthorized(String message) {
        return Result.unauthorized(message);
    }

    public static <T> Result<T> forbidden(String message) {
        return Result.forbidden(message);
    }

    public static <T> Result<T> notFound(String message) {
        return Result.notFound(message);
    }

    public static <T> Result<T> serverError(String message) {
        return Result.serverError(message);
    }
}
