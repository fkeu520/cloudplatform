package com.cloudhub.platform.space.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 地块 (park-space 业务)
 * <p>csyh 业务融合 W3.5 阶段: 地块 (Massif) 简单 CRUD.</p>
 * <p>关联关系:
 * <ul>
 *   <li>park_id → sys_park.id (园区)</li>
 *   <li>land_nature_id → sys_land_nature.id (土地性质, W3.3 已迁移)</li>
 *   <li>plan_use_id → sys_plan_use.id (规划用途, W3.3 已迁移)</li>
 * </ul>
 * <p>W3 阶段仅基础 CRUD, 不做反范式冗余字段 (landNatureName/planUseName), 关联查询通过 ID 联表.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_massif")
public class Massif extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 地块编号 */
    private String massifCode;

    /** 地块名称 */
    private String massifName;

    /** 地块面积 (m²) */
    private BigDecimal massifArea;

    /** 使用年限 */
    private Integer useYear;

    /** 土地性质 ID (关联 sys_land_nature.id) */
    private Long landNatureId;

    /** 规划用途 ID (关联 sys_plan_use.id) */
    private Long planUseId;

    /** 资产类型 */
    private String assetType;

    /** 地块描述 */
    private String massifDesc;

    /** 地块地址 */
    private String address;

    /** 状态: 0=已卖 1=可用 */
    private Integer status;

    /** 园区 ID */
    private Long parkId;

    /** 园区名称 (非数据库字段, 前端展示用) */
    @TableField(exist = false)
    private String parkName;

    /** 租户 ID (P0-1 多租户拦截器) */
    private Long tenantId;
}