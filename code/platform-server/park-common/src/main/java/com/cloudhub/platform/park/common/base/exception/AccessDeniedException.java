package com.cloudhub.platform.park.common.base.exception;

/**
 * 访问拒绝异常 (csyh Shiro UnauthorizedException 翻译)
 * <p>当用户已认证但无权限时抛出, 由 {@code RequiresPermissionsAspect} 抛.
 * 继承 {@link CommonException} (业务异常), 默认 code=403 (HTTP Forbidden).</p>
 * <p>全局异常处理: 由 platform-common 的 {@code GlobalExceptionHandler} 统一捕获,
 * 返回 {@code Result.error(403, message)} 给前端.</p>
 */
public class AccessDeniedException extends CommonException {

    private static final long serialVersionUID = 1L;

    public AccessDeniedException(String message) {
        super(403, message);
    }

    public AccessDeniedException(int code, String message) {
        super(code, message);
    }
}
