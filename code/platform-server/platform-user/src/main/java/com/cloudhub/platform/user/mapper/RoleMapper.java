package com.cloudhub.platform.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.user.domain.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT * FROM sys_role WHERE deleted = 0 ORDER BY sort ASC")
    List<Role> selectAll();

    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 查用户所有角色 (含 data_scope + custom_dept_ids)
     * 配套: M5 P0-2 data_scope 实施
     * 决策: 多角色合并策略 - 取最严格 (max data_scope)
     */
    @Select("SELECT r.* FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0")
    List<Role> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 按编码查角色 (dataScope 闭环测试用)
     */
    @Select("SELECT * FROM sys_role WHERE code = #{code} AND deleted = 0")
    Role selectByCode(@Param("code") String code);
}