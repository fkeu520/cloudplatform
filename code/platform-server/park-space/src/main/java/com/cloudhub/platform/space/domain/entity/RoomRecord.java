package com.cloudhub.platform.space.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 房间记录表 (park-space 业务)
 * <p>csyh 业务融合 W3.6 阶段: 房间记录 (RoomRecord) 简单 CRUD.</p>
 * <p>记录 sys_room 绑定/解绑 客户+合同 的历史. status: 0=添加 1=解除.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_room_record")
public class RoomRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 房间 ID (关联 sys_room.id) */
    private Long roomId;

    /** 客户 ID (跨模块引用) */
    private Long customerId;

    /** 合同 ID (跨模块引用 park-contract) */
    private Long covenantId;

    /** 合同类型: 0=租赁 1=销售 2=其他 */
    private Integer covenantType;

    /** 状态: 0=添加 1=解除 */
    private Integer status;

    /** 园区 ID */
    private Long parkId;

    /** 租户 ID (P0-1 多租户拦截器) */
    private Long tenantId;
}