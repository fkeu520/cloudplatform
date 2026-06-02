package com.cloudhub.platform.user.domain.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;          // JWT Token
    private Long expireTime;       // 过期时间
    private UserVO user;          // 用户信息
}