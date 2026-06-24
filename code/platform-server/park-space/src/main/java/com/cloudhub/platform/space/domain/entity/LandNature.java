package com.cloudhub.platform.space.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 土地性质 (park-space 业务)
 * <p>csyh 业务融合 W3.3 阶段: 土地性质 (LandNature) 简单 CRUD.</p>
 * <p>关联关系: park_id → sys_park.id, 用于地块土地性质分类.</p>
 * <p>W3 阶段仅基础 CRUD.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_land_nature")
public class LandNature extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 土地性质名称 (e.g. 工业用地, 商业用地) */
    private String landNatureName;

    /** 土地性质编号 */
    private String landNatureCode;

    /** 标的色 (前端展示, hex) */
    private String color;

    /** 状态: 0=停用 1=启用 */
    private Integer status;

    /** 园区 ID */
    private Long parkId;

    /** 园区名称 (非数据库字段, 前端展示用) */
    @TableField(exist = false)
    private String parkName;

    /** 租户 ID (P0-1 多租户拦截器) */
    private Long tenantId;
}