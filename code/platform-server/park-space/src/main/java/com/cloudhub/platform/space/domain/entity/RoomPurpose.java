package com.cloudhub.platform.space.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 房源用途 (park-space 业务)
 * <p>csyh 业务融合 W3.6 阶段: 房源用途 (RoomPurpose) 简单 CRUD.</p>
 * <p>sys_room 的扩展用途字典表. P0 #2: 改为全局配置, 不绑 parkId (2026-06-23).</p>
 * <p>V43: 新增 builtIn 固化标识. 0=系统固化不可变 1=用户新增可编辑删除 (2026-06-25).</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_room_purpose")
public class RoomPurpose extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 用途名称 (e.g. 自用, 出租, 出售) */
    private String purposeName;

    /** 固化标识: 0=系统固化不可变 1=用户新增可编辑删除 */
    @TableField("is_built_in")
    private Integer builtIn;

    /** 状态: 0=停用 1=启用 */
    private Integer status;

    /** 租户 ID (P0-1 多租户拦截器) */
    private Long tenantId;
}