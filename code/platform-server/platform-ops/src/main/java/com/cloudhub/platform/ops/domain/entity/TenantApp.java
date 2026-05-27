package com.cloudhub.platform.ops.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_tenant_app")
public class TenantApp {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long appId;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private String createBy;
}
