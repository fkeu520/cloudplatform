package com.cloudhub.platform.message.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message")
public class SysMessage extends BaseEntity {
    private String title;
    private String content;
    private String type;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private Integer readStatus;
    private String businessType;
    private String businessId;
}
