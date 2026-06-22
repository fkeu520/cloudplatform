package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.Floor;
import org.apache.ibatis.annotations.Mapper;

/**
 * 楼层 Mapper (park-space 业务)
 */
@Mapper
public interface FloorMapper extends BaseMapper<Floor> {
}