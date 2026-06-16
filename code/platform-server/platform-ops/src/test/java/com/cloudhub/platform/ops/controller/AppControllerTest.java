package com.cloudhub.platform.ops.controller;

import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.util.JwtUtil;
import com.cloudhub.platform.ops.domain.entity.App;
import com.cloudhub.platform.ops.service.AppService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * AppController /app/user 单元测试 (W3 P0 验证)
 *
 * <p>覆盖 4 个场景:
 * <ol>
 *   <li>正常 token + tenantId → 返回 app 列表</li>
 *   <li>token 无 Bearer 前缀 → 返回空列表</li>
 *   <li>JWT 解析失败 → 返回空列表</li>
 *   <li>userId 为空 → 返回空列表</li>
 * </ol>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppControllerTest {

    @Mock
    private AppService appService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AppController appController;

    private MockedStatic<JwtUtil> jwtUtilMock;
    private MockedStatic<TenantContextHolder> tenantContextMock;

    @AfterEach
    void tearDown() {
        if (jwtUtilMock != null) jwtUtilMock.close();
        if (tenantContextMock != null) tenantContextMock.close();
    }

    @Test
    void testGetUserApps_validToken_returnsList() {
        App app = new App();
        app.setId(1L);
        app.setAppCode("system");

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        jwtUtilMock = mockStatic(JwtUtil.class);
        jwtUtilMock.when(() -> JwtUtil.getUserId("valid-token")).thenReturn("10");
        tenantContextMock = mockStatic(TenantContextHolder.class);
        tenantContextMock.when(TenantContextHolder::getTenantId).thenReturn(2L);
        when(appService.userApps(eq(10L), eq(2L))).thenReturn(Collections.singletonList(app));

        var result = appController.getUserApps(request);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals(1, result.getData().size());
        assertEquals("system", result.getData().get(0).getAppCode());
        verify(appService).userApps(10L, 2L);
    }

    @Test
    void testGetUserApps_noBearerPrefix_returnsEmpty() {
        when(request.getHeader("Authorization")).thenReturn("invalid-no-prefix");

        var result = appController.getUserApps(request);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
        verifyNoInteractions(appService);
    }

    @Test
    void testGetUserApps_jwtParseFails_returnsEmpty() {
        when(request.getHeader("Authorization")).thenReturn("Bearer broken-token");
        jwtUtilMock = mockStatic(JwtUtil.class);
        jwtUtilMock.when(() -> JwtUtil.getUserId("broken-token"))
                .thenThrow(new RuntimeException("JWT parse error"));

        var result = appController.getUserApps(request);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
        verifyNoInteractions(appService);
    }

    @Test
    void testGetUserApps_userIdBlank_returnsEmpty() {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        jwtUtilMock = mockStatic(JwtUtil.class);
        jwtUtilMock.when(() -> JwtUtil.getUserId("valid-token")).thenReturn("");

        var result = appController.getUserApps(request);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
        verifyNoInteractions(appService);
    }

    @Test
    void testGetUserApps_nullTenantId_stillReturnsList() {
        App app1 = new App();
        app1.setId(1L);
        app1.setAppCode("system");
        App app2 = new App();
        app2.setId(3L);
        app2.setAppCode("workflow");

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        jwtUtilMock = mockStatic(JwtUtil.class);
        jwtUtilMock.when(() -> JwtUtil.getUserId("valid-token")).thenReturn("10");
        tenantContextMock = mockStatic(TenantContextHolder.class);
        tenantContextMock.when(TenantContextHolder::getTenantId).thenReturn(null);
        when(appService.userApps(eq(10L), isNull())).thenReturn(Arrays.asList(app1, app2));

        var result = appController.getUserApps(request);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().size());
    }
}