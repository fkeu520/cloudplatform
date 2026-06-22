package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.Energy;
import org.apache.ibatis.annotations.Mapper;

/**
 * 能源房间关联 Mapper (park-space 业务)
 */
@Mapper
public interface EnergyMapper extends BaseMapper<Energy> {
}