package com.cloudhub.platform.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.user.domain.entity.Config;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ConfigMapper extends BaseMapper<Config> {

    @Select("SELECT * FROM sys_config WHERE config_key = #{configKey} AND deleted = 0")
    Config selectByKey(@Param("configKey") String configKey);
}
