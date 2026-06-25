package com.cloudhub.platform.space.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 能源与房间关联 (park-space 业务)
 * <p>csyh 业务融合 W3.4 阶段: 能耗 (Energy) 简单 CRUD.</p>
 * <p>关联关系:
 * <ul>
 *   <li>room_id → sys_room.id (房间)</li>
 *   <li>meter_id / meter_class_id → 能源表 ID (后续接入 park-energy 模块)</li>
 * </ul>
 * <p>W3 阶段仅基础 CRUD, W4+ 阶段联动能耗读表/账单.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_energy")
public class Energy extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 能源表 ID (外键, 暂不强约束) */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long meterId;

    /** 能源表种类 (电表/水表/燃气表) */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long meterClassId;

    /** 房间 ID (关联 sys_room.id) */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long roomId;

    /** 状态: 0=停用 1=启用 */
    private Integer status;

    /** 园区 ID */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long parkId;

    /** 租户 ID (P0-1 多租户拦截器) */
    private Long tenantId;
}