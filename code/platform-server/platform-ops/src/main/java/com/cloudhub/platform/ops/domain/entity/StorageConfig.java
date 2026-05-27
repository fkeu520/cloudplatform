package com.cloudhub.platform.ops.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_storage_config")
public class StorageConfig {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private String endpoint;

    private String accessKey;

    private String secretKey;

    private String region;

    private Boolean isSecure;

    private String defaultBucket;

    private Integer status;

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
