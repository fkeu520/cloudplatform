package com.cloudhub.platform.space.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 区域 (park-space 业务)
 * <p>csyh 业务融合 W3.3 阶段: 区域 (Area) 简单 CRUD.</p>
 * <p>关联关系: park_id → sys_park.id, 一个园区下可划分多个区域 (A 区/B 区等).</p>
 * <p>W3 阶段仅基础 CRUD, W4+ 阶段关联建筑数 / 房间数 统计.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_area")
public class Area extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 区域名称 */
    private String areaName;

    /** 占地面积 (m²) */
    private BigDecimal areaCovered;

    /** 建筑面积 (m²) */
    private BigDecimal builtArea;

    /** 功能区域描述 */
    private String functionArea;

    /** 楼栋数 */
    private Integer buildingAmount;

    /** 房间数 */
    private Integer roomAmount;

    /** 是否虚拟区域 (0=否 1=是) */
    private Integer isVirtual;

    /** 排序 */
    private Integer sorting;

    /** 状态: 0=停用 1=启用 */
    private Integer status;

    /** 园区 ID */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long parkId;

    /** 租户 ID (P0-1 多租户拦截器) */
    private Long tenantId;
}