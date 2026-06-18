package com.cloudhub.platform.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.user.domain.entity.Room;
import org.apache.ibatis.annotations.Mapper;

/**
 * 园区房屋 Mapper (park-space 业务)
 *
 * @author csyh fusion W3
 * @since 2026-06-18
 */
@Mapper
public interface RoomMapper extends BaseMapper<Room> {
}
