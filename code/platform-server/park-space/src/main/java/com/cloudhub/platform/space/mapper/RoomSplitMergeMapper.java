package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.RoomSplitMerge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoomSplitMergeMapper extends BaseMapper<RoomSplitMerge> {

    @Select("SELECT * FROM sys_room_split_merge WHERE deleted = 0 AND new_room_id LIKE CONCAT('%', #{newRoomId}, '%') ORDER BY create_time DESC")
    List<RoomSplitMerge> listByNewRoomId(String newRoomId);

    @Select("SELECT * FROM sys_room_split_merge WHERE deleted = 0 AND (old_room_id LIKE CONCAT('%', #{roomId}, '%') OR new_room_id LIKE CONCAT('%', #{roomId}, '%')) ORDER BY create_time DESC")
    List<RoomSplitMerge> listByRoomId(String roomId);

    @Select("SELECT * FROM sys_room_split_merge WHERE deleted = 0 AND new_room_id LIKE CONCAT('%', #{newRoomId}, '%') LIMIT 1")
    RoomSplitMerge getByNewRoomId(String newRoomId);
}
