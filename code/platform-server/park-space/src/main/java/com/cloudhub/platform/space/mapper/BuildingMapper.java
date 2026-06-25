package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.Building;
import org.apache.ibatis.annotations.Mapper;

/**
 * 园区楼宇 Mapper (park-space 业务)
 */
@Mapper
public interface BuildingMapper extends BaseMapper<Building> {
}