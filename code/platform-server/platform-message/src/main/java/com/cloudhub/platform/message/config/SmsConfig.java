package com.cloudhub.platform.message.config;

import lombok.Data;

@Data
public class SmsConfig {
    private String accessKeyId;
    private String accessKeySecret;
    private String signName;
}
