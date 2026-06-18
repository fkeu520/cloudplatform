package com.cloudhub.platform.property.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.property.domain.entity.Building;
import org.apache.ibatis.annotations.Mapper;

/**
 * 园区楼宇 Mapper (park-property 业务)
 */
@Mapper
public interface BuildingMapper extends BaseMapper<Building> {
}
