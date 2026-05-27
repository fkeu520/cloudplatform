package com.cloudhub.platform.ops.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_gateway_route")
public class GatewayRoute {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String routeId;

    private String routeName;

    private String uri;

    private String predicates;

    private String filters;

    private Integer orderNo;

    private Integer status;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    private String createBy;

    private String updateBy;
}
