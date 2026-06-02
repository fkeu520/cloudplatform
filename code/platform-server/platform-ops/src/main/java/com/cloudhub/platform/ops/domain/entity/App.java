package com.cloudhub.platform.ops.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_app")
public class App {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String appName;

    private String appCode;

    private String appIcon;

    private Integer appType;

    private Integer status;

    private Integer sort;

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
