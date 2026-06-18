package com.cloudhub.platform.park.common.base.exception;

import com.cloudhub.platform.common.exception.BizException;

/**
 * 通用业务异常 (csyh cn.flyrise.common.core.exception.CommonException 翻译)
 * <p>csyh 出现 84 次, 用于业务校验失败 / 状态非法 / 数据不存在等可恢复业务错误.
 * 翻译策略: 继承 {@link BizException} (云枢), 默认 code=400.</p>
 * <p>使用示例 (csyh 风格兼容):
 * <pre>{@code
 * if (room == null) {
 *     throw new CommonException("房间不存在");
 * }
 * if (room.getStatus() == 2) {
 *     throw new CommonException(409, "房间已出租");
 * }
 * }</pre>
 * </p>
 * <p>全局异常处理: 由 platform-common 的 {@code GlobalExceptionHandler} 统一捕获,
 * 返回 {@code Result.error(code, message)} 给前端.</p>
 * @see com.cloudhub.platform.common.exception.BizException 云枢业务异常 (父类)
 */
public class CommonException extends BizException {

    private static final long serialVersionUID = 1L;

    public CommonException(String message) {
        super(message);
    }

    public CommonException(int code, String message) {
        super(code, message);
    }

    public CommonException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
