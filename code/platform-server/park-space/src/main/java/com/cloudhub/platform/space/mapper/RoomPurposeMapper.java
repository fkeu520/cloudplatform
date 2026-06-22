package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.RoomPurpose;
import org.apache.ibatis.annotations.Mapper;

/**
 * 房源用途 Mapper (park-space 业务)
 */
@Mapper
public interface RoomPurposeMapper extends BaseMapper<RoomPurpose> {
}