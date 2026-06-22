package com.cloudhub.platform.space.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 装修配套 (park-space 业务)
 * <p>csyh 业务融合 W3.3 阶段: 装修配套 (Kit) 简单 CRUD.</p>
 * <p>关联关系: park_id → sys_park.id, 一个园区下可定义多套装修方案.</p>
 * <p>W3 阶段仅基础 CRUD, W4+ 阶段关联 sys_room 套件类型.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_kit")
public class Kit extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 配套名称 (e.g. 标准装修, 精装修) */
    private String kitName;

    /** 数量 */
    private Integer amount;

    /** 状态: 0=停用 1=启用 */
    private Integer status;

    /** 园区 ID */
    private Long parkId;

    /** 租户 ID (P0-1 多租户拦截器) */
    private Long tenantId;
}