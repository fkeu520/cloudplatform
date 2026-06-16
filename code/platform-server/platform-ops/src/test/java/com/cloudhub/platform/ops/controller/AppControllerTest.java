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
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AppController /app/user 单元测试 (W3 P0-4 验证)
 *
 * <p>W3+ 重构 (2026-06-16): 不再用 MockedStatic&lt;JwtUtil&gt;,
 * 改用真实 {@link JwtUtil#generate} 生成 token 绕过 Java 17 +
 * Mockito 5.x byte-buddy inline 模式 class redefinition 限制
 * (KNOWN_ISSUES #28). 测试更接近生产行为.</p>
 *
 * <p>注意: Controller 从 {@link TenantContextHolder} 获取 tenantId,
 * 测试前需要 setTenantId() 初始化.</p>
 *
 * <p>5 场景:
 * <ol>
 *   <li>正常 token + tenantId → 返回 app 列表</li>
 *   <li>token 无 Bearer 前缀 → 返回空列表</li>
 *   <li>JWT 解析失败 (无效 token) → 返回空列表</li>
 *   <li>userId 为空 (空 subject) → 返回空列表</li>
 *   <li>null tenantId → 仍返回列表</li>
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

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private static String token(String userId, Long tenantId, Integer userType) {
        return JwtUtil.generate(userId, "admin", tenantId, userType, 3600);
    }

    private static App app(Long id, String code, String name) {
        App a = new App();
        a.setId(id);
        a.setAppCode(code);
        a.setAppName(name);
        a.setSort(id.intValue());
        return a;
    }

    @Test
    void testGetUserApps_validToken_returnsList() {
        TenantContextHolder.setTenantId(1L);
        String t = token("10", 1L, 0);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + t);
        when(appService.userApps(eq(10L), eq(1L))).thenReturn(Collections.singletonList(app(1L, "system", "系统管理")));

        var r = appController.getUserApps(request);

        assertNotNull(r);
        assertEquals(200, r.getCode());
        assertEquals(1, r.getData().size());
        assertEquals("system", r.getData().get(0).getAppCode());
        verify(appService).userApps(10L, 1L);
    }

    @Test
    void testGetUserApps_noBearerPrefix_returnsEmpty() {
        when(request.getHeader("Authorization")).thenReturn("no-bearer");

        var r = appController.getUserApps(request);

        assertEquals(200, r.getCode());
        assertTrue(r.getData().isEmpty());
        verifyNoInteractions(appService);
    }

    @Test
    void testGetUserApps_jwtParseFails_returnsEmpty() {
        when(request.getHeader("Authorization")).thenReturn("Bearer broken.token.format");

        var r = appController.getUserApps(request);

        assertEquals(200, r.getCode());
        assertTrue(r.getData().isEmpty());
        verifyNoInteractions(appService);
    }

    @Test
    void testGetUserApps_userIdBlank_returnsEmpty() {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token("", null, null));

        var r = appController.getUserApps(request);

        assertEquals(200, r.getCode());
        assertTrue(r.getData().isEmpty());
        verifyNoInteractions(appService);
    }

    @Test
    void testGetUserApps_nullTenantId_stillReturnsList() {
        TenantContextHolder.setTenantId(null);
        String t = token("10", null, 0);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + t);
        when(appService.userApps(eq(10L), isNull())).thenReturn(List.of(app(1L, "system", "系统管理"), app(3L, "workflow", "流程中心")));

        var r = appController.getUserApps(request);

        assertEquals(200, r.getCode());
        assertEquals(2, r.getData().size());
    }
}