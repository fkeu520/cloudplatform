package com.cloudhub.platform.ops.controller;

import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.ops.domain.entity.SysAnnouncement;
import com.cloudhub.platform.ops.service.SysAnnouncementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SysAnnouncementController /announcement/recent 单元测试 (W3 P0-3 验证)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysAnnouncementControllerTest {

    @Mock
    private SysAnnouncementService service;

    @InjectMocks
    private SysAnnouncementController controller;

    private MockedStatic<TenantContextHolder> tenantMock;

    @AfterEach
    void tearDown() {
        if (tenantMock != null) tenantMock.close();
    }

    private SysAnnouncement mockAnnouncement(Long id, String title, String appCode) {
        SysAnnouncement a = new SysAnnouncement();
        a.setId(id);
        a.setTitle(title);
        a.setAppCode(appCode);
        a.setPublishTime(LocalDateTime.now());
        return a;
    }

    @Test
    void testRecent_defaultLimitAndTenantId() {
        SysAnnouncement a1 = mockAnnouncement(101L, "公告1", null);
        SysAnnouncement a2 = mockAnnouncement(102L, "公告2", "system");

        tenantMock = mockStatic(TenantContextHolder.class);
        tenantMock.when(TenantContextHolder::getTenantId).thenReturn(1L);
        when(service.recent(eq(null), eq(1L), eq(3))).thenReturn(Arrays.asList(a1, a2));

        var result = controller.recent(null, 3);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().size());
    }

    @Test
    void testRecent_withAppCode_system() {
        SysAnnouncement a = mockAnnouncement(102L, "系统管理公告", "system");

        tenantMock = mockStatic(TenantContextHolder.class);
        tenantMock.when(TenantContextHolder::getTenantId).thenReturn(2L);
        when(service.recent(eq("system"), eq(2L), eq(3)))
                .thenReturn(Collections.singletonList(a));

        var result = controller.recent("system", 3);

        assertNotNull(result);
        assertEquals("system", result.getData().get(0).getAppCode());
    }

    @Test
    void testRecent_noAnnouncement_returnsEmpty() {
        tenantMock = mockStatic(TenantContextHolder.class);
        tenantMock.when(TenantContextHolder::getTenantId).thenReturn(null);
        when(service.recent(any(), any(), anyInt())).thenReturn(Collections.emptyList());

        var result = controller.recent(null, 3);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    void testRecent_customLimit_passedThrough() {
        tenantMock = mockStatic(TenantContextHolder.class);
        tenantMock.when(TenantContextHolder::getTenantId).thenReturn(1L);
        when(service.recent(eq("park-space"), eq(1L), eq(5)))
                .thenReturn(Collections.emptyList());

        controller.recent("park-space", 5);

        verify(service).recent("park-space", 1L, 5);
    }
}