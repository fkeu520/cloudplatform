package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.Massif;
import org.apache.ibatis.annotations.Mapper;

/**
 * 地块 Mapper (park-space 业务)
 */
@Mapper
public interface MassifMapper extends BaseMapper<Massif> {
}