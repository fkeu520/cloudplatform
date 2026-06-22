package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.Equipment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备设施 Mapper (park-space 业务)
 */
@Mapper
public interface EquipmentMapper extends BaseMapper<Equipment> {
}