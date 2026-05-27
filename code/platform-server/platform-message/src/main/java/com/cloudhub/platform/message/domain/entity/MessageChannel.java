package com.cloudhub.platform.message.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message_channel")
public class MessageChannel extends BaseEntity {
    private String channelCode;
    private String channelName;
    private String configJson;
    private Integer status;
    private String remark;
}
