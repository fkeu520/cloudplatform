package com.cloudhub.platform.space.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 空间 (park-space 业务)
 * <p>csyh 业务融合 W3.5 阶段: 空间 (Space) 简单 CRUD.</p>
 * <p>关联关系:
 * <ul>
 *   <li>park_id → sys_park.id (园区)</li>
 *   <li>area_id → sys_area.id (区域, W3.3 已迁移)</li>
 *   <li>category_id → sys_space_category.id (空间类别, W3.5 同批迁移)</li>
 * </ul>
 * <p>W3 阶段仅基础 CRUD, 不做反范式冗余字段 (categoryName/pathNames), 关联查询通过 ID 联表.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_space")
public class Space extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 空间名称 */
    private String spaceName;

    /** 位置描述 */
    private String spaceDescribe;

    /** 所属园区 ID */
    private Long parkId;

    /** 园区名称 (非数据库字段, 前端展示用) */
    @TableField(exist = false)
    private String parkName;

    /** 所属区域 ID */
    private Long areaId;

    /** 空间类别 ID */
    private Long categoryId;

    /** 状态: 0=停用 1=启用 */
    private Integer status;

    /** 租户 ID (P0-1 多租户拦截器) */
    private Long tenantId;
}