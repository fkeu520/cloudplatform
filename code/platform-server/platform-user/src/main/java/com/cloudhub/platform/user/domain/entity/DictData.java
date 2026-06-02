package com.cloudhub.platform.user.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class DictData extends BaseEntity {
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer dictSort;
    private Integer status;
    private String cssClass;
    private String remark;
    private Integer tenantId;
}
