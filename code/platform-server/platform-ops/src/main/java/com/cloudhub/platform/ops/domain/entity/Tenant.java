package com.cloudhub.platform.ops.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_tenant")
public class Tenant {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String tenantName;

    private String tenantCode;

    private Integer tenantType;

    private String contactPerson;

    private String contactMobile;

    private String contactEmail;

    private String address;

    private String domain;

    private Integer status;

    private LocalDateTime expireTime;

    private Integer maxUserCount;

    private String remark;

    private Integer sort;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    private String createBy;

    private String updateBy;
}
