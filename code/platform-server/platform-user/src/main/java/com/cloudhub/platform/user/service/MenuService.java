package com.cloudhub.platform.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.user.domain.entity.Menu;
import com.cloudhub.platform.user.domain.entity.User;
import com.cloudhub.platform.user.mapper.MenuMapper;
import com.cloudhub.platform.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单管理 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuMapper menuMapper;
    private final UserMapper userMapper;

    /**
     * 获取所有菜单（树形结构）
     */
    public List<Map<String, Object>> tree() {
        List<Menu> all = menuMapper.selectList(
                new LambdaQueryWrapper<Menu>().eq(Menu::getDeleted, 0).orderByAsc(Menu::getSort)
        );
        return buildTree(all, 0L);
    }

    /**
     * 按 menu_category 列出菜单 (扁平, 用于 ops-admin / future 平台)
     *
     * <p>V40+#36.2 长期方案: 平台 enum 写在 {@link com.cloudhub.platform.common.constant.Constants.MenuCategory},
     * sys_menu.menu_category 列承载实际值, 此方法不做任何动态拼接,
     * 严格按 category 字符串相等过滤。</p>
     */
    public List<Map<String, Object>> listByCategory(String category) {
        List<Menu> all = menuMapper.selectByCategory(category);
        return buildTree(all, 0L);
    }

    /**
     * 获取菜单树（仅菜单类型，用于左侧导航）
     */
    public List<Map<String, Object>> navTree() {
        List<Menu> all = menuMapper.selectList(
                new LambdaQueryWrapper<Menu>()
                        .eq(Menu::getDeleted, 0)
                        .eq(Menu::getStatus, 1)
                        .eq(Menu::getType, 1)
                        .orderByAsc(Menu::getSort)
        );
        return buildTree(all, 0L);
    }

    /**
     * 根据用户ID获取菜单树（动态权限 + 多租户应用级过滤）
     * - 租户管理员(userType=1)：返回该租户已授权应用的启用菜单
     * - 普通用户(userType=0)：通过角色+直接授权获取，再过滤租户已授权应用
     * - 运营管理员(userType=2)：返回空（只能登录运营后台）
     *
     * <p>W3 阶段新增重载方法 {@link #getUserMenus(Long, Long)} 支持按 appId 过滤,
     * 用于 Layout.vue 切换顶部 tab 时加载对应左侧菜单. 本方法保留向后兼容.</p>
     */
    public List<Map<String, Object>> getUserMenus(Long userId) {
        return getUserMenus(userId, null);
    }

    /**
     * 根据用户ID + appId 获取菜单树 (W3 P0-5 新增)
     *
     * <p>逻辑:
     * <ol>
     *   <li>首先按 {@link #getUserMenus(Long)} 拿用户全量菜单</li>
     *   <li>如果传入 appId, 在结果上过滤 m.appId == appId (含 appId IS NULL 公共菜单)</li>
     *   <li>空 appId / null 与旧方法行为一致 (返回所有菜单)</li>
     * </ol>
     * </p>
     *
     * @param userId 用户ID (从 JWT 解析)
     * @param appId  应用ID (可选; null = 返回所有, 具体值 = 只返回该 app 下菜单)
     * @return 菜单树 (按 parentId=0 起始的树形结构)
     */
    public List<Map<String, Object>> getUserMenus(Long userId, Long appId) {
        // 复用旧方法拿到全量菜单 (含 userType + 租户过滤)
        List<Map<String, Object>> allMenus = getUserMenusInternal(userId);
        if (appId == null) {
            return allMenus;
        }
        // 按 appId 过滤: 包含指定 app 的菜单 + appId IS NULL 的公共菜单
        return allMenus.stream()
                .filter(node -> {
                    Object nodeAppId = node.get("appId");
                    return nodeAppId == null || appId.equals(((Number) nodeAppId).longValue());
                })
                .map(node -> filterTreeByAppId(node, appId))
                .filter(node -> node != null)
                .collect(Collectors.toList());
    }

    /**
     * 内部方法: 拿用户全量菜单树 (无 appId 过滤)
     * 拆出来为了 getUserMenus(Long, Long) 复用 + 单测可独立验证
     */
    private List<Map<String, Object>> getUserMenusInternal(Long userId) {
        // 查询用户类型
        User user = userMapper.selectById(userId);
        if (user == null) {
            return new ArrayList<>();
        }
        Integer userType = user.getUserType() != null ? user.getUserType() : 0;

        // 获取当前请求的租户ID（从JWT Token解析而来，TenantFilter 已注入）
        Long tenantId = TenantContextHolder.getTenantId();
        // 获取该租户已授权的应用ID列表（effectively final，lambda 安全）
        List<Long> authorizedAppIds = (tenantId != null)
                ? menuMapper.selectAuthorizedAppIds(tenantId)
                : Collections.emptyList();

        if (userType == 1) {
            // 租户管理员：只返回该租户已授权应用的启用菜单
            List<Menu> menus;
            if (authorizedAppIds.isEmpty()) {
                // 没有授权任何应用 → 返回空菜单
                menus = new ArrayList<>();
            } else {
                menus = menuMapper.selectEnabledByAppIds(authorizedAppIds);
            }
            return buildTree(menus, 0L);
        } else if (userType == 2) {
            // 运营管理员：不能访问管理后台，返回空
            return new ArrayList<>();
        } else {
            // 普通用户：通过角色 + 直接授权菜单，再过滤租户已授权应用
            List<Menu> menus = menuMapper.selectByUserId(userId);
            if (!authorizedAppIds.isEmpty()) {
                menus = menus.stream()
                        .filter(m -> m.getAppId() == null || authorizedAppIds.contains(m.getAppId()))
                        .collect(Collectors.toList());
            }
            return buildTree(menus, 0L);
        }
    }

    /**
     * 递归过滤子树, 仅保留 appId 匹配或 appId IS NULL 的节点
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> filterTreeByAppId(Map<String, Object> node, Long appId) {
        Object nodeAppId = node.get("appId");
        boolean selfMatch = (nodeAppId == null) || appId.equals(((Number) nodeAppId).longValue());
        List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
        if (children != null && !children.isEmpty()) {
            List<Map<String, Object>> filteredChildren = children.stream()
                    .map(c -> filterTreeByAppId(c, appId))
                    .filter(c -> c != null)
                    .collect(Collectors.toList());
            if (!filteredChildren.isEmpty()) {
                node.put("children", filteredChildren);
            } else {
                node.remove("children");
            }
        }
        // 自身匹配 OR 有任何子节点匹配 → 保留
        if (selfMatch || (node.get("children") != null)) {
            return node;
        }
        return null;
    }

    /**
     * 根据用户ID获取权限列表（已过滤租户未授权应用的权限）
     *
     * <p>用户类型处理:
     * <ul>
     *   <li>userType=0 (普通用户): 通过角色+直接授权获取, 再过滤租户已授权应用</li>
     *   <li>userType=1 (租户管理员): 跳过角色关联, 直接拿租户已授权应用的全部权限</li>
     *   <li>userType=2 (运营管理员): 无菜单权限, 返回空</li>
     * </ul>
     * </p>
     */
    public List<String> getUserPermissions(Long userId) {
        User user = userMapper.selectById(userId);
        Integer userType = user != null && user.getUserType() != null ? user.getUserType() : 0;
        Long tenantId = TenantContextHolder.getTenantId();

        List<Menu> menus;
        if (userType == 1 && tenantId != null) {
            // 租户管理员: 跳过角色关联, 拿该租户已授权应用的全部权限
            List<Long> appIds = menuMapper.selectAuthorizedAppIds(tenantId);
            if (appIds.isEmpty()) {
                return new ArrayList<>();
            }
            menus = menuMapper.selectEnabledByAppIds(appIds);
        } else {
            // 普通用户 / 运营管理员: 通过角色+直接授权获取
            menus = menuMapper.selectByUserId(userId);
            if (tenantId != null) {
                List<Long> appIds = menuMapper.selectAuthorizedAppIds(tenantId);
                if (!appIds.isEmpty()) {
                    menus = menus.stream()
                            .filter(m -> m.getAppId() == null || appIds.contains(m.getAppId()))
                            .collect(Collectors.toList());
                }
            }
        }

        return menus.stream()
                .filter(m -> m.getPerms() != null && !m.getPerms().isEmpty())
                .map(Menu::getPerms)
                .collect(Collectors.toList());
    }

    /**
     * 根据角色ID获取菜单树
     */
    public List<Map<String, Object>> getRoleMenus(Long roleId) {
        List<Menu> menus = menuMapper.selectByRoleId(roleId);
        return buildTree(menus, 0L);
    }

    /**
     * 根据ID查询菜单
     */
    public Menu getById(Long id) {
        Menu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BizException("菜单不存在");
        }
        return menu;
    }

    /**
     * 新增菜单
     */
    @Transactional
    public void create(Map<String, Object> params) {
        String name = (String) params.get("name");
        Integer type = ((Number) params.getOrDefault("type", 1)).intValue();
        if (!StringUtils.hasText(name)) {
            throw new BizException("菜单名称不能为空");
        }

        Menu menu = new Menu();
        menu.setParentId(params.get("parentId") != null ? ((Number) params.get("parentId")).longValue() : 0L);
        menu.setName(name);
        menu.setType(type);
        menu.setPath((String) params.get("path"));
        menu.setComponent((String) params.get("component"));
        menu.setIcon((String) params.get("icon"));
        menu.setSort(params.get("sort") != null ? ((Number) params.get("sort")).intValue() : 0);
        menu.setPerms((String) params.get("perms"));
        menu.setStatus(params.get("status") != null ? ((Number) params.get("status")).intValue() : 1);
        menuMapper.insert(menu);
    }

    /**
     * 更新菜单
     */
    @Transactional
    public void update(Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        Menu exist = menuMapper.selectById(id);
        if (exist == null) {
            throw new BizException("菜单不存在");
        }
        if (params.containsKey("name")) exist.setName((String) params.get("name"));
        if (params.containsKey("parentId")) exist.setParentId(((Number) params.get("parentId")).longValue());
        if (params.containsKey("path")) exist.setPath((String) params.get("path"));
        if (params.containsKey("component")) exist.setComponent((String) params.get("component"));
        if (params.containsKey("icon")) exist.setIcon((String) params.get("icon"));
        if (params.containsKey("sort")) exist.setSort(((Number) params.get("sort")).intValue());
        if (params.containsKey("perms")) exist.setPerms((String) params.get("perms"));
        if (params.containsKey("status")) exist.setStatus(((Number) params.get("status")).intValue());
        menuMapper.updateById(exist);
    }

    /**
     * 删除菜单
     */
    @Transactional
    public void delete(Long id) {
        // 检查是否有子菜单
        long childCount = menuMapper.selectCount(
                new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id).eq(Menu::getDeleted, 0)
        );
        if (childCount > 0) {
            throw new BizException("请先删除子菜单");
        }
        menuMapper.deleteById(id);
    }

    // ========== 内部工具方法 ==========

    private List<Map<String, Object>> buildTree(List<Menu> menus, Long parentId) {
        return menus.stream()
                .filter(m -> m.getParentId().equals(parentId))
                .map(m -> {
                    Map<String, Object> node = new java.util.HashMap<>();
                    node.put("id", m.getId());
                    node.put("name", m.getName());
                    node.put("path", m.getPath());
                    node.put("component", m.getComponent());
                    node.put("type", m.getType());
                    node.put("icon", m.getIcon());
                    node.put("perms", m.getPerms());
                    node.put("sort", m.getSort());
                    node.put("appId", m.getAppId()); // W3 P0-5: 把 appId 放进 node, 前端按 appId 过滤时可用
                    List<Map<String, Object>> children = buildTree(menus, m.getId());
                    if (!children.isEmpty()) {
                        node.put("children", children);
                    }
                    return node;
                })
                .collect(Collectors.toList());
    }
}