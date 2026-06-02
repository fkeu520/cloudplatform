package com.cloudhub.platform.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.user.domain.entity.UserMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMenuMapper extends BaseMapper<UserMenu> {

    @Select("SELECT menu_id FROM sys_user_menu WHERE user_id = #{userId}")
    List<Long> selectMenuIdsByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_menu WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);
}
