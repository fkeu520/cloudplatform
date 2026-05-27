package com.cloudhub.platform.user.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体
 */
@Data
@TableName("sys_role")
public class Role {

    @TableId
    private Long id;

    /** 角色编码 */
    private String code;

    /** 角色名称 */
    private String name;

    /** 状态：0禁用 1启用 */
    private Integer status;

    /** 排序 */
    private Integer sort;

    /** 备注 */
    private String remark;

    /** 租户ID */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 删除标记：0未删 1已删 */
    @TableLogic
    private Integer deleted;
}