package com.cloudhub.platform.space.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 合并房源请求
 */
@Data
public class RoomMergeDTO {

    private Long parkId;
    private Long buildingId;
    private Long floorId;
    private Integer floor;

    private String roomNo;
    private String roomName;
    private String roomType;

    private BigDecimal areaCovered;
    private BigDecimal buildArea;
    private BigDecimal billableArea;
    private BigDecimal unitPrice;
    private BigDecimal monthlyRent;

    private String reasons;

    /** 被合并的旧房间 ID 列表 */
    private List<Long> oldRoomIds;
}
