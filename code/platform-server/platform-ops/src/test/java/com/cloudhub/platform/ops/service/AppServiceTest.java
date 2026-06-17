package com.cloudhub.platform.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudhub.platform.ops.domain.entity.App;
import com.cloudhub.platform.ops.domain.mapper.AppMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * AppService.userApps 单元测试 (W3 P0 验证)
 *
 * <p>覆盖 5 个场景:
 * <ol>
 *   <li>userId=null → 返回空列表 (异常保护)</li>
 *   <li>普通用户 (有角色授权 menu) → 返回 app 列表</li>
 *   <li>租户管理员 (有 tenant 授权 app) → 返回 app 列表</li>
 *   <li>运营管理员 (无角色) → 返回空列表</li>
 *   <li>tenantId=null + 普通用户 → 不传 tenantId 也能查到</li>
 * </ol>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class AppServiceTest {

    @Mock
    private AppMapper appMapper;

    @InjectMocks
    private AppService appService;

    private App systemApp;
    private App workflowApp;

    @BeforeEach
    void setUp() {
        systemApp = new App();
        systemApp.setId(1L);
        systemApp.setAppName("系统管理");
        systemApp.setAppCode("system");
        systemApp.setSort(1);

        workflowApp = new App();
        workflowApp.setId(3L);
        workflowApp.setAppName("流程中心");
        workflowApp.setAppCode("workflow");
        workflowApp.setSort(3);
    }

    @Test
    void testUserApps_nullUserId_returnsEmpty() {
        List<App> result = appService.userApps(null, null, null);
        assertTrue(result.isEmpty());
        verifyNoInteractions(appMapper);
    }

    @Test
    void testUserApps_normalUser_withRoles() {
        // 普通用户 userId=10, userType=0, tenantId=null
        when(appMapper.selectUserApps(eq(10L), isNull())).thenReturn(Arrays.asList(systemApp, workflowApp));

        List<App> result = appService.userApps(10L, null, 0);

        assertEquals(2, result.size());
        assertEquals("system", result.get(0).getAppCode());
        verify(appMapper, times(1)).selectUserApps(10L, null);
    }

    @Test
    void testUserApps_tenantAdmin_withTenantGrant() {
        // 租户管理员 userId=5, userType=1, tenantId=2 → 走 selectTenantAdminApps
        when(appMapper.selectTenantAdminApps(eq(2L))).thenReturn(Collections.singletonList(systemApp));

        List<App> result = appService.userApps(5L, 2L, 1);

        assertEquals(1, result.size());
        assertEquals("system", result.get(0).getAppCode());
        verify(appMapper).selectTenantAdminApps(2L);
    }

    @Test
    void testUserApps_opsAdmin_noRoles_returnsEmpty() {
        // 运营管理员 userId=1, userType=2 → 走 selectUserApps (同普通用户逻辑)
        when(appMapper.selectUserApps(eq(1L), isNull())).thenReturn(Collections.emptyList());

        List<App> result = appService.userApps(1L, null, 2);

        assertTrue(result.isEmpty());
    }

    @Test
    void testUserApps_userIdBlankString_invalid() {
        // userId=0 + userType=null → 走 else 分支调用 mapper
        when(appMapper.selectUserApps(eq(0L), isNull())).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> appService.userApps(0L, null, null));
    }
}