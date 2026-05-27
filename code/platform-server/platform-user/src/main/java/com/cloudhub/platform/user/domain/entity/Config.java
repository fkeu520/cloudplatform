package com.cloudhub.platform.user.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class Config extends BaseEntity {
    private String configName;
    private String configKey;
    private String configValue;
    private Integer configType;
    private String remark;
    private Integer tenantId;
}
