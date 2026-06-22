package com.cloudhub.platform.space.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 房源拆分合并记录 (park-space 业务)
 * <p>csyh 业务融合 W3.6 阶段: 房间拆分合并记录 (RoomSplitMerge) 简单 CRUD.</p>
 * <p>记录 sys_room 拆分/合并操作历史. status: 0=合并 1=拆分.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_room_split_merge")
public class RoomSplitMerge extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 操作人 ID (BaseEntity.createBy 也存, 此处保留) */
    private Long userId;

    /** 操作人姓名 */
    private String userName;

    /** 操作原因 */
    private String reasons;

    /** 类型 (具体枚举 W3 不实现, 用 0/1) */
    private Integer type;

    /** 原房源 ID (关联 sys_room.id) */
    private Long oldRoomId;

    /** 原房源名称 */
    private String oldRoomName;

    /** 新房源 ID (关联 sys_room.id) */
    private Long newRoomId;

    /** 新房源名称 */
    private String newRoomName;

    /** 拆分数量 */
    private Integer num;

    /** 是否继承能源表: 0=否 1=是 */
    private Integer isExtend;

    /** 状态: 0=合并 1=拆分 */
    private Integer status;

    /** 园区 ID */
    private Long parkId;

    /** 租户 ID (P0-1 多租户拦截器) */
    private Long tenantId;
}