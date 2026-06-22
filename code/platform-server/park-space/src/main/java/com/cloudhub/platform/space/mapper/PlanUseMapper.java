package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.PlanUse;
import org.apache.ibatis.annotations.Mapper;

/**
 * 规划用途 Mapper (park-space 业务)
 */
@Mapper
public interface PlanUseMapper extends BaseMapper<PlanUse> {
}