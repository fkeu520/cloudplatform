package com.cloudhub.platform.space.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 房源拆分合并记录
 * <p>记录 sys_room 拆分/合并/还原操作历史. type: 0=合并 1=拆分 2=还原.</p>
 * <p>oldRoomId/newRoomId 为逗号分隔的 ID 串, 支持多房间场景.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_room_split_merge")
public class RoomSplitMerge extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String userName;

    private String reasons;

    /** 0=合并 1=拆分 2=还原 */
    private Integer type;

    /** 冗余状态字段, 与 type 同值, 兼容旧前端读 status (V51+) */
    private Integer status;

    /** 原房源 ID (逗号分隔, 支持多个) */
    private String oldRoomId;

    private String oldRoomName;

    /** 新房源 ID (逗号分隔, 支持多个) */
    private String newRoomId;

    private String newRoomName;

    private Integer num;

    private Integer isExtend;

    private Long parkId;

    private Long tenantId;
}