package com.cloudhub.platform.space.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 合并房源请求
 */
@Data
public class RoomMergeDTO {

    @NotNull(message = "园区 ID 不能为空")
    private Long parkId;

    @NotNull(message = "楼栋 ID 不能为空")
    private Long buildingId;

    private Long floorId;

    private Integer floor;

    @NotBlank(message = "合并后房号不能为空")
    @Size(max = 64, message = "房号长度不能超过 64")
    private String roomNo;

    @NotBlank(message = "合并后房间名称不能为空")
    @Size(max = 64, message = "房间名称长度不能超过 64")
    private String roomName;

    private String roomType;

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

    private String reasons;

    /** 被合并的旧房间 ID 列表 (至少 2 个) */
    @NotEmpty(message = "被合并的旧房间列表不能为空")
    @Size(min = 2, message = "合并至少需要 2 个房间")
    private List<Long> oldRoomIds;
}