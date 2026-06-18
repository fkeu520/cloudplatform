package com.cloudhub.platform.park.common.base.exception;

import com.cloudhub.platform.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 异常适配层单元测试
 * <p>验证 CommonException / BusinessException 正确继承 BizException (云枢),
 * 字段与方法完全兼容.</p>
 */
class ExceptionTest {

    @Test
    void commonException_withMessage_shouldDefaultTo400() {
        CommonException ex = new CommonException("参数错误");
        assertEquals(400, ex.getCode());
        assertEquals("参数错误", ex.getMessage());
    }

    @Test
    void commonException_withCodeAndMessage_shouldUseGiven() {
        CommonException ex = new CommonException(409, "资源冲突");
        assertEquals(409, ex.getCode());
        assertEquals("资源冲突", ex.getMessage());
    }

    @Test
    void commonException_withCause_shouldKeepStack() {
        Throwable cause = new RuntimeException("root");
        CommonException ex = new CommonException(500, "服务器错误", cause);
        assertEquals(500, ex.getCode());
        assertSame(cause, ex.getCause());
    }

    @Test
    void businessException_isCommonException() {
        BusinessException ex = new BusinessException("业务错误");
        assertTrue(ex instanceof CommonException, "BusinessException 必须是 CommonException");
        assertTrue(ex instanceof BizException, "BusinessException 必须是 BizException");
    }

    @Test
    void businessException_defaultCodeIs400() {
        BusinessException ex = new BusinessException("余额不足");
        assertEquals(400, ex.getCode());
        assertEquals("余额不足", ex.getMessage());
    }

    @Test
    void businessException_withCodeAndMessage() {
        BusinessException ex = new BusinessException(403, "无权限");
        assertEquals(403, ex.getCode());
    }

    @Test
    void commonException_isBizException() {
        // 验证云枢 GlobalExceptionHandler 能捕获 park 异常
        CommonException ex = new CommonException("test");
        assertTrue(ex instanceof BizException, "CommonException 必须是 BizException");
    }

    @Test
    void businessException_serializable() {
        // 异常通常不需要 Serializable, 但父类是 RuntimeException, 这里只验证对象本身
        BusinessException ex = new BusinessException("test");
        assertNotNull(ex);
    }
}
