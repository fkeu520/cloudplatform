package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.RoomSplitMerge;
import org.apache.ibatis.annotations.Mapper;

/**
 * 房间拆分合并记录 Mapper (park-space 业务)
 */
@Mapper
public interface RoomSplitMergeMapper extends BaseMapper<RoomSplitMerge> {
}