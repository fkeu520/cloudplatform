package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.Area;
import org.apache.ibatis.annotations.Mapper;

/**
 * 区域 Mapper (park-space 业务)
 */
@Mapper
public interface AreaMapper extends BaseMapper<Area> {
}