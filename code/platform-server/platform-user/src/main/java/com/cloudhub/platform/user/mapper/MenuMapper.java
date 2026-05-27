package com.cloudhub.platform.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.user.domain.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    @Select("SELECT * FROM sys_menu WHERE deleted = 0 AND status = 1 ORDER BY sort ASC")
    List<Menu> selectAllEnabled();

    @Select("SELECT m.* FROM sys_menu m " +
            "LEFT JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "LEFT JOIN sys_user_role ur ON ur.role_id = rm.role_id " +
            "WHERE ur.user_id = #{userId} AND m.deleted = 0 AND m.status = 1 " +
            "GROUP BY m.id ORDER BY m.sort ASC")
    List<Menu> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "WHERE rm.role_id = #{roleId} AND m.deleted = 0 AND m.status = 1 " +
            "ORDER BY m.sort ASC")
    List<Menu> selectByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT m.* FROM sys_menu m " +
            "INNER JOIN sys_user_menu um ON m.id = um.menu_id " +
            "WHERE um.user_id = #{userId} AND m.deleted = 0 AND m.status = 1 " +
            "ORDER BY m.sort ASC")
    List<Menu> selectEnabledByUserMenuIds(@Param("userId") Long userId);
}