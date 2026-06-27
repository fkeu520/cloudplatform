package com.cloudhub.platform.space.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 拆分房源请求
 */
@Data
public class RoomSplitDTO {

    @NotNull(message = "园区 ID 不能为空")
    private Long parkId;

    @NotNull(message = "楼栋 ID 不能为空")
    private Long buildingId;

    /** 被拆分的旧房间 ID */
    @NotNull(message = "被拆分的旧房间 ID 不能为空")
    private Long oldRoomId;

    private String reasons;

    @NotNull(message = "拆分数量不能为空")
    @Min(value = 2, message = "拆分数量至少为 2")
    private Integer num;

    /** 拆分后的新房间列表 */
    @NotEmpty(message = "拆分房间列表不能为空")
    @Valid
    private List<SplitRoomItem> roomList;
}