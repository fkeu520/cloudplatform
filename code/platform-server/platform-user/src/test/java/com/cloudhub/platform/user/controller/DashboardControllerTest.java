package com.cloudhub.platform.user.controller;

import com.cloudhub.platform.common.util.JwtUtil;
import com.cloudhub.platform.user.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DashboardController 单元测试 (W3 P1-3)
 *
 * <p>使用 {@link MockedStatic} mock {@link JwtUtil} 静态方法,
 * 确保测试不依赖真实 JWT 解析.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardControllerTest {

    @Mock
    private DashboardService service;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private DashboardController controller;

    private MockedStatic<JwtUtil> jwtUtilMock;

    @AfterEach
    void tearDown() {
        if (jwtUtilMock != null) jwtUtilMock.close();
    }

    private void mockJwtUserId(String userIdStr) {
        jwtUtilMock = mockStatic(JwtUtil.class);
        jwtUtilMock.when(() -> JwtUtil.getUserId(anyString())).thenReturn(userIdStr);
    }

    // ============ /welcome ============

    @Test
    void testWelcome_noToken_returnsZeroWelcome() {
        when(request.getHeader("Authorization")).thenReturn(null);

        var result = controller.welcome(request);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals(0, result.getData().get("todoCount"));
        verifyNoInteractions(service);
    }

    @Test
    void testWelcome_validToken_callsService() {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        mockJwtUserId("10");
        when(service.welcome(eq(10L))).thenReturn(Map.of(
                "todoCount", 5, "msgCount", 3, "myApplyCount", 2, "sysNoticeCount", 1));

        var result = controller.welcome(request);

        assertEquals(5, result.getData().get("todoCount"));
        verify(service).welcome(10L);
    }

    @Test
    void testWelcome_invalidTokenFormat_returnsZero() {
        when(request.getHeader("Authorization")).thenReturn("Basic xxx");

        var result = controller.welcome(request);

        assertEquals(0, result.getData().get("todoCount"));
        verifyNoInteractions(service);
    }

    // ============ /todos ============

    @Test
    void testTodos_noToken_returnsEmpty() {
        when(request.getHeader("Authorization")).thenReturn(null);

        var result = controller.todos(request, 5);

        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
        verifyNoInteractions(service);
    }

    @Test
    void testTodos_limitClampedToMax() {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        mockJwtUserId("10");
        when(service.todos(eq(10L), eq(20))).thenReturn(List.of());

        controller.todos(request, 100); // 请求 100, 应该被 clamp 到 20

        verify(service).todos(10L, 20);
    }

    @Test
    void testTodos_limitZero_clampedToOne() {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        mockJwtUserId("10");
        when(service.todos(eq(10L), eq(1))).thenReturn(List.of());

        controller.todos(request, 0);

        verify(service).todos(10L, 1);
    }

    // ============ /business-stats ============

    @Test
    void testBusinessStats_nullAppCode_emptyList() {
        var result = controller.businessStats(null);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
        verifyNoInteractions(service);
    }

    @Test
    void testBusinessStats_blankAppCode_emptyList() {
        var result = controller.businessStats("");

        assertTrue(result.getData().isEmpty());
        verifyNoInteractions(service);
    }

    @Test
    void testBusinessStats_systemAppCode_callsService() {
        when(service.businessStats(eq("system"))).thenReturn(List.of());

        var result = controller.businessStats("system");

        assertNotNull(result);
        verify(service).businessStats("system");
    }

    // ============ /system-stats ============

    @Test
    void testSystemStats_noToken_returnsEmptyMap() {
        when(request.getHeader("Authorization")).thenReturn(null);

        var result = controller.systemStats(request);

        assertNotNull(result);
        assertTrue(result.getData().isEmpty());
        verifyNoInteractions(service);
    }

    @Test
    void testSystemStats_invalidAuthFormat_emptyMap() {
        when(request.getHeader("Authorization")).thenReturn("Basic xxx");

        var result = controller.systemStats(request);

        assertTrue(result.getData().isEmpty());
        verifyNoInteractions(service);
    }
}