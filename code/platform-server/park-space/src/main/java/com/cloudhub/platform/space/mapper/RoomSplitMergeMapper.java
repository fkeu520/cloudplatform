package com.cloudhub.platform.space.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudhub.platform.space.domain.entity.RoomSplitMerge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoomSplitMergeMapper extends BaseMapper<RoomSplitMerge> {

    /**
     * new_room_id/old_room_id 为逗号分隔的 ID 串, 使用 FIND_IN_SET 精确匹配
     */
    @Select("SELECT * FROM sys_room_split_merge WHERE deleted = 0 AND FIND_IN_SET(#{newRoomId}, new_room_id) > 0 ORDER BY create_time DESC")
    List<RoomSplitMerge> listByNewRoomId(String newRoomId);

    @Select("SELECT * FROM sys_room_split_merge WHERE deleted = 0 AND (FIND_IN_SET(#{roomId}, old_room_id) > 0 OR FIND_IN_SET(#{roomId}, new_room_id) > 0) ORDER BY create_time DESC")
    List<RoomSplitMerge> listByRoomId(String roomId);

    @Select("SELECT * FROM sys_room_split_merge WHERE deleted = 0 AND FIND_IN_SET(#{newRoomId}, new_room_id) > 0 LIMIT 1")
    RoomSplitMerge getByNewRoomId(String newRoomId);
}
