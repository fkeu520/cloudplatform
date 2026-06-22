package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.RoomLockRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 房间锁定记录 Mapper (park-space 业务)
 */
@Mapper
public interface RoomLockRecordMapper extends BaseMapper<RoomLockRecord> {
}