package com.cloudhub.platform.space.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 拆分后的单个房间
 */
@Data
public class SplitRoomItem {

    @NotBlank(message = "房号不能为空")
    @Size(max = 64, message = "房号长度不能超过 64")
    private String roomNo;

    @NotBlank(message = "房间名称不能为空")
    @Size(max = 64, message = "房间名称长度不能超过 64")
    private String roomName;

    @NotNull(message = "楼层号不能为空")
    private Integer floor;

    @NotNull(message = "楼层 ID 不能为空")
    private Long floorId;

    @NotBlank(message = "房间类型不能为空")
    private String roomType;

    @NotNull(message = "建筑面积不能为空")
    @DecimalMin(value = "0.01", message = "建筑面积必须大于 0")
    private BigDecimal areaCovered;

    @DecimalMin(value = "0", message = "套内面积不能为负")
    private BigDecimal buildArea;

    @DecimalMin(value = "0", message = "计费面积不能为负")
    private BigDecimal billableArea;

    @DecimalMin(value = "0", message = "单价不能为负")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0", message = "月租金不能为负")
    private BigDecimal monthlyRent;
}