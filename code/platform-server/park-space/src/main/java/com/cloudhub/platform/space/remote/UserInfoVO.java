package com.cloudhub.platform.space.remote;

import lombok.Data;

/**
 * platform-user UserVO 简化版 (仅取 nickname)
 * <p>避免引入 platform-user 依赖; 仅在 park-space 用于跨服务取昵称</p>
 */
@Data
public class UserInfoVO {

    /** 用户 ID */
    private Long id;

    /** 用户名 (登录账号) */
    private String username;

    /** 昵称 */
    private String nickname;
}