package com.cloudhub.platform.space.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备设施 (park-space 业务)
 * <p>csyh 业务融合 W3.4 阶段: 设备设施 (Equipment) 简单 CRUD.</p>
 * <p>关联关系:
 * <ul>
 *   <li>kit_id → sys_kit.id (装修配套, W3.3 已迁移)</li>
 *   <li>park_id → sys_park.id (园区)</li>
 * </ul>
 * <p>W3 阶段仅基础 CRUD, W4+ 阶段关联设备运维/巡检.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_equipment")
public class Equipment extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 设备名称 */
    private String equipmentName;

    /** 型号 */
    private String model;

    /** 数量 */
    private Integer amount;

    /** 配套 ID (关联 sys_kit.id) */
    private Long kitId;

    /** 状态: 0=停用 1=启用 */
    private Integer status;

    /** 园区 ID */
    private Long parkId;

    /** 租户 ID (P0-1 多租户拦截器) */
    private Long tenantId;
}