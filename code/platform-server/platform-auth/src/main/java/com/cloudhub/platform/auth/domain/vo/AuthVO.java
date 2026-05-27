package com.cloudhub.platform.auth.domain.vo;

import lombok.Data;

@Data
public class AuthVO {
    private String token;       // JWT Token
    private Long expireTime;   // 过期时间戳(ms)
    private Long userId;       // 用户ID
}