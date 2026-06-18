package com.cloudhub.platform.park.common.base.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AccessDeniedException} 单元测试
 */
class AccessDeniedExceptionTest {

    @Test
    void withMessage_shouldDefaultTo403() {
        AccessDeniedException ex = new AccessDeniedException("权限不足");
        assertEquals(403, ex.getCode());
        assertEquals("权限不足", ex.getMessage());
    }

    @Test
    void withCodeAndMessage_shouldUseGiven() {
        AccessDeniedException ex = new AccessDeniedException(401, "未登录");
        assertEquals(401, ex.getCode());
        assertEquals("未登录", ex.getMessage());
    }

    @Test
    void isCommonException() {
        AccessDeniedException ex = new AccessDeniedException("test");
        assertTrue(ex instanceof CommonException, "AccessDeniedException 必须是 CommonException");
    }
}
