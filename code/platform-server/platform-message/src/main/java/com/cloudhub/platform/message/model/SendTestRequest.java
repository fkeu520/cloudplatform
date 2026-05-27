package com.cloudhub.platform.message.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendTestRequest {
    private String channelCode;
    private String receiverAddress;
    private String content;
}
