package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.LandNature;
import org.apache.ibatis.annotations.Mapper;

/**
 * 土地性质 Mapper (park-space 业务)
 */
@Mapper
public interface LandNatureMapper extends BaseMapper<LandNature> {
}