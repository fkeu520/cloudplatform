package com.cloudhub.platform.space.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 楼层 (park-space 业务)
 * <p>csyh 业务融合 W3.3 阶段: 楼层 (Floor) 简单 CRUD.</p>
 * <p>关联关系: park_id → sys_park.id, building_id → sys_building.id.</p>
 * <p>W3 阶段仅基础 CRUD, 不做楼层类型联动 (地上/地下更新楼栋计数) 等复杂逻辑.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_floor")
public class Floor extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 楼层名称 (e.g. 1 楼) */
    private String floorName;

    /** 楼层序号 (e.g. 1, 2, 3) */
    private Integer serialCode;

    /** 楼层类型: 0=地上 1=地下 2=夹层 */
    private Integer floorCategory;

    /** 楼层系数 (默认 1.00) */
    private BigDecimal coefficient;

    /** 排序 */
    private Integer sorting;

    /** 状态: 0=停用 1=启用 */
    private Integer status;

    /** 园区 ID */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long parkId;

    /** 楼栋 ID */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long buildingId;

    /** 租户 ID (P0-1 多租户拦截器) */
    private Long tenantId;
}