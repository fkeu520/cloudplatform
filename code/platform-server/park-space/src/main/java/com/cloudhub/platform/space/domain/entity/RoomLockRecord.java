package com.cloudhub.platform.space.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 房间锁定操作记录 (park-space 业务)
 * <p>csyh 业务融合 W3.6 阶段: 房间锁定记录 (RoomLockRecord) 简单 CRUD.</p>
 * <p>记录 sys_room 锁定/解锁操作历史. 关联 room_id → sys_room.id.</p>
 * <p>is_lock: 0=解锁 1=锁定.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_room_lock_record")
public class RoomLockRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 房间 ID (关联 sys_room.id) */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long roomId;

    /** 是否锁定: 0=解锁 1=锁定 */
    private Integer isLock;

    /** 企业/客户 ID (跨模块引用) */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long enterpriseId;

    /** 企业/客户名称 */
    private String enterpriseName;

    /** 操作人 (BaseEntity.createBy 提供 userId, 此处冗余存姓名) */
    private String operator;

    /** 操作原因 */
    private String reason;

    /** 锁定天数 */
    private Integer days;

    /** 园区 ID */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long parkId;

    /** 园区名称 (非数据库字段, 前端展示用) */
    @TableField(exist = false)
    private String parkName;

    /** 租户 ID (P0-1 多租户拦截器) */
    private Long tenantId;
}