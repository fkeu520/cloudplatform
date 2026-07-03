package com.cloudhub.platform.auth.aspect;

import com.cloudhub.platform.auth.service.StepUpTokenService;
import com.cloudhub.platform.common.annotation.RequireStepUp;
import com.cloudhub.platform.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

/**
 * P0-3 Step-up Aspect 单元测试 (5 TC)
 * <p>Day 2: 纯 Mockito 测试, 不依赖 Spring AOP 织入. 集成测试留作 Day 5.</p>
 *
 * <h2>测试策略</h2>
 * <ul>
 *   <li>Mock StepUpTokenService</li>
 *   <li>用 RequestContextHolder 设置 mock request (模拟 HTTP 上下文)</li>
 *   <li>Mock ProceedingJoinPoint + Signature</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("P0-3 Step-up Aspect 单元测试 (5 TC)")
class StepUpAspectTest {

    @Mock
    private StepUpTokenService stepUpService;

    @Mock
    private ProceedingJoinPoint pjp;

    @Mock
    private Signature signature;

    @InjectMocks
    private StepUpAspect aspect;

    private static final String TEST_TOKEN = "valid-stepup-token-abc";
    private static final String TEST_IP = "192.168.1.100";
    private static final String TEST_UA = "Mozilla/5.0 Test";
    private static final String TEST_SCOPE = "tenant:delete";

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void setUpHttpContext(MockHttpServletRequest req) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    @Test
    @Order(1)
    @DisplayName("TC-01: 带 X-Step-Up-Token 调用 — verifyAndConsume 通过, proceed 继续")
    void tc01_validToken_proceed() throws Throwable {
        // given
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Step-Up-Token", TEST_TOKEN);
        req.addHeader("User-Agent", TEST_UA);
        req.setRemoteAddr(TEST_IP);
        setUpHttpContext(req);

        when(stepUpService.verifyAndConsume(eq(TEST_TOKEN), eq(TEST_SCOPE), eq(TEST_IP),
                eq(TEST_UA), eq(false))).thenReturn(42L);
        when(pjp.proceed()).thenReturn("ok");
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("TenantController.delete()");

        // when
        RequireStepUp annotation = mockAnnotation(TEST_SCOPE, "删除租户", false);
        Object result = aspect.around(pjp, annotation);

        // then
        assertEquals("ok", result);
        verify(stepUpService, times(1)).verifyAndConsume(
                eq(TEST_TOKEN), eq(TEST_SCOPE), eq(TEST_IP), eq(TEST_UA), eq(false));
        verify(pjp, times(1)).proceed();
        // 验证 request 属性被设置
        assertEquals(42L, req.getAttribute("stepUpTokenId"));
        assertEquals(true, req.getAttribute("requiresStepUp"));
    }

    @Test
    @Order(2)
    @DisplayName("TC-02: 缺少 X-Step-Up-Token — Service 抛 '缺少' 异常, 不 proceed")
    void tc02_missingToken_throws() throws Throwable {
        // given: request 中无 X-Step-Up-Token
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("User-Agent", TEST_UA);
        req.setRemoteAddr(TEST_IP);
        setUpHttpContext(req);

        when(stepUpService.verifyAndConsume(eq(null), eq(TEST_SCOPE), eq(TEST_IP),
                eq(TEST_UA), eq(false)))
                .thenThrow(new BizException("缺少 X-Step-Up-Token 头"));
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("test");

        // when + then
        RequireStepUp annotation = mockAnnotation(TEST_SCOPE, "", false);
        BizException ex = assertThrows(BizException.class, () -> aspect.around(pjp, annotation));
        assertTrue(ex.getMessage().contains("缺少"));
        verify(pjp, never()).proceed();
    }

    @Test
    @Order(3)
    @DisplayName("TC-03: token scope 不匹配 — Service 抛 'scope' 异常")
    void tc03_scopeMismatch_throws() throws Throwable {
        // given
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Step-Up-Token", TEST_TOKEN);
        req.setRemoteAddr(TEST_IP);
        setUpHttpContext(req);

        when(stepUpService.verifyAndConsume(anyString(), eq(TEST_SCOPE), anyString(),
                any(), anyBoolean()))
                .thenThrow(new BizException("Step-up token scope 不匹配: 需要 " + TEST_SCOPE));

        // when + then
        RequireStepUp annotation = mockAnnotation(TEST_SCOPE, "", false);
        BizException ex = assertThrows(BizException.class, () -> aspect.around(pjp, annotation));
        assertTrue(ex.getMessage().contains("scope"));
    }

    @Test
    @Order(4)
    @DisplayName("TC-04: 异步/无 HTTP 上下文 — 跳过 step-up 校验, 直接 proceed")
    void tc04_noHttpContext_skipAndProceed() throws Throwable {
        // given: 不设置 RequestContextHolder
        when(pjp.proceed()).thenReturn("async-result");

        // when
        RequireStepUp annotation = mockAnnotation(TEST_SCOPE, "", false);
        Object result = aspect.around(pjp, annotation);

        // then
        assertEquals("async-result", result);
        verify(stepUpService, never()).verifyAndConsume(
                anyString(), anyString(), anyString(), any(), anyBoolean());
        verify(pjp, times(1)).proceed();
    }

    @Test
    @Order(5)
    @DisplayName("TC-05: X-Forwarded-For 多级代理 — 取第一个 IP")
    void tc05_multiProxy_extractFirstIp() throws Throwable {
        // given: 多级代理 X-Forwarded-For: client,proxy1,proxy2
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Step-Up-Token", TEST_TOKEN);
        req.addHeader("X-Forwarded-For", "10.0.0.5, 192.168.1.1, 172.16.0.1");
        req.setRemoteAddr("127.0.0.1");
        setUpHttpContext(req);

        when(stepUpService.verifyAndConsume(eq(TEST_TOKEN), eq(TEST_SCOPE), eq("10.0.0.5"),
                any(), eq(false))).thenReturn(99L);
        when(pjp.proceed()).thenReturn("ok");
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("test");

        // when
        RequireStepUp annotation = mockAnnotation(TEST_SCOPE, "", false);
        aspect.around(pjp, annotation);

        // then: 验证 service 收到的是第一个 IP (10.0.0.5)
        verify(stepUpService, times(1)).verifyAndConsume(
                eq(TEST_TOKEN), eq(TEST_SCOPE), eq("10.0.0.5"), any(), eq(false));
    }

    /**
     * 创建 RequireStepUp 注解 mock (注解是接口, Mockito 5+ 支持)
     */
    private RequireStepUp mockAnnotation(String scope, String description, boolean allowMultiUse) {
        RequireStepUp ann = org.mockito.Mockito.mock(RequireStepUp.class);
        when(ann.scope()).thenReturn(scope);
        when(ann.description()).thenReturn(description);
        when(ann.allowMultiUse()).thenReturn(allowMultiUse);
        return ann;
    }
}
