package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.Park;
import org.apache.ibatis.annotations.Mapper;

/**
 * 园区 Mapper
 */
@Mapper
public interface ParkMapper extends BaseMapper<Park> {
}
