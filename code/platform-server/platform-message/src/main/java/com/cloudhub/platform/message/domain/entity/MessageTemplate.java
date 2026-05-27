package com.cloudhub.platform.message.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message_template")
public class MessageTemplate extends BaseEntity {
    private String templateCode;
    private String templateName;
    private String channelCode;
    private String signName;
    private String templateId;
    private String templateContent;
    private String paramsJson;
    private Integer status;
}
