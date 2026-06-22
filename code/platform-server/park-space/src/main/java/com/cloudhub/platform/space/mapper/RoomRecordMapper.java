package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.RoomRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 房间记录 Mapper (park-space 业务)
 */
@Mapper
public interface RoomRecordMapper extends BaseMapper<RoomRecord> {
}