package com.cloudhub.platform.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.user.domain.entity.DictType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DictTypeMapper extends BaseMapper<DictType> {

    @Select("SELECT * FROM sys_dict_type WHERE dict_type = #{dictType} AND deleted = 0")
    DictType selectByType(@Param("dictType") String dictType);
}
