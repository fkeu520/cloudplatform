package com.cloudhub.platform.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.common.result.PageResult;
import com.cloudhub.platform.user.domain.entity.Role;
import com.cloudhub.platform.user.domain.entity.RoleMenu;
import com.cloudhub.platform.user.mapper.RoleMapper;
import com.cloudhub.platform.user.mapper.RoleMenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 角色管理 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;

    /**
     * 分页查询角色
     */
    public PageResult<Role> page(String keyword, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Role::getName, keyword).or().like(Role::getCode, keyword);
        }
        if (status != null) {
            wrapper.eq(Role::getStatus, status);
        }
        wrapper.eq(Role::getDeleted, 0).orderByAsc(Role::getSort);

        Page<Role> page = new Page<>(pageNum, pageSize);
        Page<Role> result = roleMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 查询所有角色
     */
    public List<Role> list(Integer status) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getDeleted, 0);
        if (status != null) {
            wrapper.eq(Role::getStatus, status);
        }
        wrapper.orderByAsc(Role::getSort);
        return roleMapper.selectList(wrapper);
    }

    /**
     * 根据ID查询角色
     */
    public Role getById(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException("角色不存在");
        }
        return role;
    }

    /**
     * 新增角色
     */
    @Transactional
    public void create(Map<String, Object> params) {
        String code = (String) params.get("code");
        String name = (String) params.get("name");
        if (!StringUtils.hasText(code) || !StringUtils.hasText(name)) {
            throw new BizException("角色编码和名称不能为空");
        }

        // 校验唯一性
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getCode, code).eq(Role::getDeleted, 0);
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new BizException("角色编码已存在");
        }

        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setStatus(1);
        role.setSort((Integer) params.getOrDefault("sort", 0));
        role.setRemark((String) params.get("remark"));
        role.setTenantId(1L);
        roleMapper.insert(role);
    }

    /**
     * 更新角色
     */
    @Transactional
    public void update(Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        Role exist = roleMapper.selectById(id);
        if (exist == null) {
            throw new BizException("角色不存在");
        }

        if (params.containsKey("name")) {
            exist.setName((String) params.get("name"));
        }
        if (params.containsKey("sort")) {
            exist.setSort((Integer) params.get("sort"));
        }
        if (params.containsKey("remark")) {
            exist.setRemark((String) params.get("remark"));
        }
        if (params.containsKey("status")) {
            exist.setStatus((Integer) params.get("status"));
        }
        roleMapper.updateById(exist);
    }

    /**
     * 删除角色
     */
    @Transactional
    public void delete(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException("角色不存在");
        }
        roleMapper.deleteById(id);
        // 删除角色菜单关联
        roleMenuMapper.deleteByRoleId(id);
    }

    /**
     * 查询角色拥有的菜单ID列表
     */
    public List<Long> getMenuIds(Long roleId) {
        return roleMapper.selectMenuIdsByRoleId(roleId);
    }

    /**
     * 给角色分配菜单权限
     */
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException("角色不存在");
        }
        // 删除旧关联
        roleMenuMapper.deleteByRoleId(roleId);
        // 插入新关联
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                RoleMenu rm = new RoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }
    }
}