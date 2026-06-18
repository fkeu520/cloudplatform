package com.cloudhub.platform.park.common.base.exception;

/**
 * 业务异常 (csyh cn.flyrise.common.exception.BusinessException 翻译)
 *
 * <p>csyh 出现 20 次, 是 CommonException 的同义别名, 用于业务模块的"业务级"异常.
 * 翻译策略: 继承 {@link CommonException} (避免重复), 与 csyh 业务代码 import 路径对应.</p>
 *
 * <p>使用示例 (csyh 风格兼容):
 * <pre>{@code
 * if (order.getAmount() <= 0) {
 *     throw new BusinessException("订单金额必须大于 0");
 * }
 * }</pre>
 * </p>
 *
 * <p><b>命名说明</b>:
 * <ul>
 *   <li>{@code CommonException} — 翻译自 cn.flyrise.common.core.exception (通用)</li>
 *   <li>{@code BusinessException} — 翻译自 cn.flyrise.common.exception (业务)</li>
 *   <li>两者实际等价, 但 import 路径不同, 都暴露方便业务模块按 csyh 习惯选</li>
 * </ul>
 *
 * @author csyh fusion W2.1
 * @since 2026-06-18
 * @see com.cloudhub.platform.park.common.base.exception.CommonException 同义别名
 */
public class BusinessException extends CommonException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(int code, String message) {
        super(code, message);
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
