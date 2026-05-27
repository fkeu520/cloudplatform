package com.cloudhub.platform.message.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message_record")
public class MessageRecord extends BaseEntity {
    private String title;
    private String content;
    private String channelCode;
    private Long templateId;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private String receiverAddress;
    private Integer sendStatus;
    private Integer retryCount;
    private Integer maxRetries;
    private LocalDateTime sendTime;
    private String errorMsg;
    private String businessType;
    private String businessId;
    private Integer tenantId;
    private String tenantName;
}
