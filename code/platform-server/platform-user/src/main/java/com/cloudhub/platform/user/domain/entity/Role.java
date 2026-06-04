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

    @TableId(type = IdType.ASSIGN_ID)
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

    /**
     * 数据范围 (2026-06-04 新增, M5 P0-2)
     * 1=全部 2=本部门 3=本部门及下级 4=本人 5=自定义
     */
    private Integer dataScope;

    /**
     * 自定义部门ID列表 (data_scope=5 时使用, 逗号分隔)
     */
    private String customDeptIds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 删除标记：0未删 1已删 */
    @TableLogic
    private Integer deleted;
}