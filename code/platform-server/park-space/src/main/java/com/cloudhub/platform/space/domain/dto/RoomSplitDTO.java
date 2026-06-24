package com.cloudhub.platform.space.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 拆分房源请求
 */
@Data
public class RoomSplitDTO {

    private Long parkId;
    private Long buildingId;

    /** 被拆分的旧房间 ID */
    private Long oldRoomId;

    private String reasons;

    private Integer num;

    /** 拆分后的新房间列表 */
    private List<SplitRoomItem> roomList;
}
