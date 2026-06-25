package com.cloudhub.platform.space.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 空间类别 (park-space 业务)
 * <p>csyh 业务融合 W3.5 阶段: 空间类别 (SpaceCategory) 简单 CRUD.</p>
 * <p>关联关系: park_id → sys_park.id (园区).</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_space_category")
public class SpaceCategory extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 类型名称 (e.g. 研发中心, 营销中心) */
    private String typeName;

    /** 类型描述 */
    private String typeDescribe;

    /** 园区 ID */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long parkId;

    /** 园区名称 (非数据库字段, 前端展示用) */
    @TableField(exist = false)
    private String parkName;

    /** 状态: 0=停用 1=启用 */
    private Integer status;

    /** 租户 ID (P0-1 多租户拦截器) */
    private Long tenantId;
}