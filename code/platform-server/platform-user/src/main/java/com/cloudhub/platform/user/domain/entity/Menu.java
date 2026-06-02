package com.cloudhub.platform.user.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜单权限实体
 */
@Data
@TableName("sys_menu")
public class Menu {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 父菜单ID */
    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 路由路径 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 类型：1菜单 2按钮 */
    private Integer type;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 权限标识 */
    private String perms;

    /** 状态：0禁用 1启用 */
    private Integer status;

    /** 所属应用ID（关联 sys_app.id），用于多租户应用级权限过滤 */
    private Long appId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}