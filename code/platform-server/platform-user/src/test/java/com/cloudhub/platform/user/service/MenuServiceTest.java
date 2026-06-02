package com.cloudhub.platform.user.service;

import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.user.domain.entity.Menu;
import com.cloudhub.platform.user.domain.entity.User;
import com.cloudhub.platform.user.mapper.MenuMapper;
import com.cloudhub.platform.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MenuService 多租户菜单过滤 单元测试
 *
 * 核心测试点:
 * - 租户管理员(userType=1): 只能看到租户已授权应用的菜单
 * - 普通用户(userType=0): 角色菜单被租户授权应用集二次过滤
 * - 运营管理员(userType=2): 始终返回空(走运营后台)
 * - 权限列表同样按租户授权应用过滤
 */
@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuMapper menuMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private MenuService menuService;

    private final Long userId = 1L;
    private final Long tenantId = 1L;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ==================== 租户管理员 (userType=1) ====================

    @Test
    @DisplayName("租户管理员: 只返回已授权应用的菜单")
    void tenantAdmin_shouldSeeOnlyAuthorizedAppMenus() {
        // Given: userType=1 (租户管理员)
        User adminUser = new User();
        adminUser.setId(userId);
        adminUser.setUserType(1);

        when(userMapper.selectById(userId)).thenReturn(adminUser);
        // 租户只授权了 app 1 (system) 和 app 2 (user-center)
        when(menuMapper.selectAuthorizedAppIds(tenantId)).thenReturn(List.of(1L, 2L));
        // 启用菜单中只有 app 1 和 2 的菜单
        Menu sysMenu = createMenu(1L, 0L, "系统管理", "/system", 1, 1L);
        Menu userCenterMenu = createMenu(14L, 0L, "基础配置", "/base", 1, 2L);
        when(menuMapper.selectEnabledByAppIds(List.of(1L, 2L)))
                .thenReturn(List.of(sysMenu, userCenterMenu));

        // When
        List<Map<String, Object>> result = menuService.getUserMenus(userId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size(), "租户管理员应该只看到 2 个授权应用的根菜单");
        assertEquals("系统管理", result.get(0).get("name"));
        assertEquals("基础配置", result.get(1).get("name"));
        verify(menuMapper).selectEnabledByAppIds(List.of(1L, 2L));
    }

    @Test
    @DisplayName("租户管理员: 无授权应用时返回空")
    void tenantAdmin_noAuthorizedApps_shouldReturnEmpty() {
        // Given: 用户是租户管理员,但租户没有任何授权应用
        User adminUser = new User();
        adminUser.setId(userId);
        adminUser.setUserType(1);

        when(userMapper.selectById(userId)).thenReturn(adminUser);
        when(menuMapper.selectAuthorizedAppIds(tenantId)).thenReturn(List.of());

        // When
        List<Map<String, Object>> result = menuService.getUserMenus(userId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty(), "无授权应用时应返回空菜单");
        verify(menuMapper, never()).selectEnabledByAppIds(anyList());
    }

    // ==================== 运营管理员 (userType=2) ====================

    @Test
    @DisplayName("运营管理员: 始终返回空(走运营后台)")
    void opsAdmin_shouldReturnEmpty() {
        // Given: 运营管理员
        User opsUser = new User();
        opsUser.setId(userId);
        opsUser.setUserType(2);

        when(userMapper.selectById(userId)).thenReturn(opsUser);

        // When
        List<Map<String, Object>> result = menuService.getUserMenus(userId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty(), "运营管理员在管理后台应该返回空菜单");
        verify(menuMapper, never()).selectEnabledByAppIds(any());
    }

    // ==================== 普通用户 (userType=0) ====================

    @Test
    @DisplayName("普通用户: 角色菜单按租户授权应用二次过滤")
    void regularUser_shouldFilterMenusByAppIds() {
        // Given: 普通用户
        User normalUser = new User();
        normalUser.setId(userId);
        normalUser.setUserType(0);

        when(userMapper.selectById(userId)).thenReturn(normalUser);
        // 租户授权了 app 1 (system) 和 app 3 (workflow)
        when(menuMapper.selectAuthorizedAppIds(tenantId)).thenReturn(List.of(1L, 3L));
        // 角色给了所有菜单(含 app 2 user-center, app 4 notification)
        when(menuMapper.selectByUserId(userId)).thenReturn(List.of(
                createMenu(1L, 0L, "系统管理", "/system", 1, 1L),
                createMenu(14L, 0L, "基础配置", "/base", 1, 2L),
                createMenu(26L, 0L, "流程中心", "/workflow", 1, 3L),
                createMenu(48L, 0L, "消息中心", "/message", 1, 4L)
        ));

        // When
        List<Map<String, Object>> result = menuService.getUserMenus(userId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size(), "普通用户只能看到已授权应用的菜单");
        assertEquals("系统管理", result.get(0).get("name"));
        assertEquals("流程中心", result.get(1).get("name"));
    }

    @Test
    @DisplayName("普通用户: 菜单无 appId 时不过滤(兼容旧数据)")
    void regularUser_menuWithoutAppId_shouldNotBeFiltered() {
        // Given: 普通用户
        User normalUser = new User();
        normalUser.setId(userId);
        normalUser.setUserType(0);

        when(userMapper.selectById(userId)).thenReturn(normalUser);
        // 有 appId null 的旧菜单应通过(兼容性)
        when(menuMapper.selectByUserId(userId)).thenReturn(List.of(
                createMenu(1L, 0L, "旧菜单", "/legacy", 1, null)
        ));

        // When
        List<Map<String, Object>> result = menuService.getUserMenus(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size(), "无 appId 的菜单应该不被过滤");
        assertEquals("旧菜单", result.get(0).get("name"));
    }

    // ==================== getUserPermissions ====================

    @Test
    @DisplayName("权限列表: 按租户授权应用过滤")
    void permissions_shouldFilterByAppIds() {
        // Given
        User normalUser = new User();
        normalUser.setId(userId);
        normalUser.setUserType(0);

        when(menuMapper.selectByUserId(userId)).thenReturn(List.of(
                createMenuWithPerms(1L, 0L, "系统管理", null, 2, 1L, "system:user:list"),
                createMenuWithPerms(2L, 1L, "用户管理", null, 2, 1L, "system:user:add"),
                createMenuWithPerms(48L, 0L, "消息中心", null, 2, 4L, "message:list:view")
        ));
        when(menuMapper.selectAuthorizedAppIds(tenantId)).thenReturn(List.of(1L));

        // When
        List<String> perms = menuService.getUserPermissions(userId);

        // Then
        assertNotNull(perms);
        assertEquals(2, perms.size(), "只应返回授权应用(1)的权限");
        assertTrue(perms.contains("system:user:list"));
        assertTrue(perms.contains("system:user:add"));
        assertFalse(perms.contains("message:list:view"));
    }

    // ==================== 边界情况 ====================

    @Test
    @DisplayName("租户ID为null(未登录)时不过滤")
    void noTenantId_shouldNotFilter() {
        TenantContextHolder.clear();

        User normalUser = new User();
        normalUser.setId(userId);
        normalUser.setUserType(0);
        when(userMapper.selectById(userId)).thenReturn(normalUser);
        when(menuMapper.selectByUserId(userId)).thenReturn(List.of(
                createMenu(1L, 0L, "全部可见", "/all", 1, 1L)
        ));

        List<Map<String, Object>> result = menuService.getUserMenus(userId);
        assertNotNull(result);
        assertEquals(1, result.size(), "未登录时应该不过滤");
    }

    @Test
    @DisplayName("用户不存在时返回空")
    void userNotFound_shouldReturnEmpty() {
        when(userMapper.selectById(anyLong())).thenReturn(null);

        List<Map<String, Object>> result = menuService.getUserMenus(999L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== 工具方法 ====================

    private Menu createMenu(Long id, Long parentId, String name, String path, Integer type, Long appId) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setName(name);
        menu.setPath(path);
        menu.setType(type);
        menu.setStatus(1);
        menu.setAppId(appId);
        return menu;
    }

    private Menu createMenuWithPerms(Long id, Long parentId, String name, String path,
                                     Integer type, Long appId, String perms) {
        Menu menu = createMenu(id, parentId, name, path, type, appId);
        menu.setPerms(perms);
        return menu;
    }
}
