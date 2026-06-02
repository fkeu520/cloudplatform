package com.cloudhub.platform.message.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageSendRequest implements Serializable {
    private Long recordId;
    private String channelCode;
    private String title;
    private String content;
    private String receiverAddress;
    private Long templateId;
}
