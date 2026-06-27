package com.cloudhub.platform.message.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    @NotBlank(message = "渠道编码不能为空")
    private String channelCode;

    @Size(max = 200, message = "标题长度不能超过200")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    @NotBlank(message = "接收地址不能为空")
    private String receiverAddress;

    private Long templateId;
    private Integer tenantId;
    private String tenantName;
}
