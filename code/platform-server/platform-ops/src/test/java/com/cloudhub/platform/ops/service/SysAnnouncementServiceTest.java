package com.cloudhub.platform.ops.service;

import com.cloudhub.platform.ops.domain.entity.SysAnnouncement;
import com.cloudhub.platform.ops.domain.mapper.SysAnnouncementMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SysAnnouncementService 单元测试 (W3 P0-3 验证)
 *
 * <p>覆盖 5 个场景:
 * <ol>
 *   <li>正常查询: appCode='system', tenantId=1, limit=3</li>
 *   <li>无 tenantId (NULL): 全租户公告</li>
 *   <li>limit 越界: 自动 clamp 到 3</li>
 *   <li>limit 非法 (&lt;=0): 自动 clamp 到 3</li>
 *   <li>DB 异常: 返回空列表 (不抛异常给前端)</li>
 * </ol>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class SysAnnouncementServiceTest {

    @Mock
    private SysAnnouncementMapper mapper;

    @InjectMocks
    private SysAnnouncementService service;

    private SysAnnouncement a1;
    private SysAnnouncement a2;
    private SysAnnouncement a3;

    @BeforeEach
    void setUp() {
        a1 = new SysAnnouncement();
        a1.setId(1900000000000000101L);
        a1.setTitle("【v7.5】平台升级公告");
        a1.setAppCode(null);
        a1.setPriority(100);

        a2 = new SysAnnouncement();
        a2.setId(1900000000000000102L);
        a2.setTitle("【系统管理】菜单结构调整");
        a2.setAppCode("system");
        a2.setPriority(50);

        a3 = new SysAnnouncement();
        a3.setId(1900000000000000103L);
        a3.setTitle("【空间中心】Phase 0 即将启动");
        a3.setAppCode("park-space");
        a3.setPriority(30);
    }

    @Test
    void testRecent_normalQuery() {
        when(mapper.selectRecent(eq("system"), eq(1L), eq(3)))
                .thenReturn(Arrays.asList(a1, a2));

        List<SysAnnouncement> result = service.recent("system", 1L, 3);

        assertEquals(2, result.size());
        assertEquals(100, result.get(0).getPriority()); // priority DESC 验证
    }

    @Test
    void testRecent_nullTenantId() {
        when(mapper.selectRecent(eq(null), eq(null), eq(3)))
                .thenReturn(Collections.singletonList(a1));

        List<SysAnnouncement> result = service.recent(null, null, 3);

        assertEquals(1, result.size());
        assertEquals("【v7.5】平台升级公告", result.get(0).getTitle());
    }

    @Test
    void testRecent_limitTooLarge_clampedToThree() {
        // limit=100 应该被 clamp 到 3
        when(mapper.selectRecent(eq(null), eq(null), eq(3)))
                .thenReturn(Collections.emptyList());

        List<SysAnnouncement> result = service.recent(null, null, 100);

        assertEquals(0, result.size());
        // mapper 收到的 limit 应该是 3 (不是 100)
        verify(mapper).selectRecent(null, null, 3);
    }

    @Test
    void testRecent_limitZeroOrNegative_clampedToThree() {
        when(mapper.selectRecent(eq(null), eq(null), eq(3)))
                .thenReturn(Collections.emptyList());

        List<SysAnnouncement> result = service.recent(null, null, 0);

        verify(mapper).selectRecent(null, null, 3);
    }

    @Test
    void testRecent_dbException_returnsEmpty() {
        when(mapper.selectRecent(anyString(), any(), anyInt()))
                .thenThrow(new RuntimeException("DB connection failed"));

        // 异常不应抛出, 应返回空列表 (工作台容错)
        List<SysAnnouncement> result = service.recent("system", 1L, 3);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}