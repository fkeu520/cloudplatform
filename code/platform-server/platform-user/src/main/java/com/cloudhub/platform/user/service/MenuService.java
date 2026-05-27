package com.cloudhub.platform.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.user.domain.entity.Menu;
import com.cloudhub.platform.user.mapper.MenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
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
     * 根据用户ID获取菜单树（动态权限）
     */
    public List<Map<String, Object>> getUserMenus(Long userId) {
        List<Menu> menus = menuMapper.selectByUserId(userId);
        return buildTree(menus, 0L);
    }

    /**
     * 根据用户ID获取权限列表
     */
    public List<String> getUserPermissions(Long userId) {
        List<Menu> menus = menuMapper.selectByUserId(userId);
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
                    List<Map<String, Object>> children = buildTree(menus, m.getId());
                    if (!children.isEmpty()) {
                        node.put("children", children);
                    }
                    return node;
                })
                .collect(Collectors.toList());
    }
}