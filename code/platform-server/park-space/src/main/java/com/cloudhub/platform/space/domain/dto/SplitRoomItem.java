package com.cloudhub.platform.space.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 拆分后的单个房间
 */
@Data
public class SplitRoomItem {

    private String roomNo;
    private String roomName;
    private Integer floor;
    private Long floorId;
    private String roomType;

    private BigDecimal areaCovered;
    private BigDecimal buildArea;
    private BigDecimal billableArea;
    private BigDecimal unitPrice;
    private BigDecimal monthlyRent;
}
