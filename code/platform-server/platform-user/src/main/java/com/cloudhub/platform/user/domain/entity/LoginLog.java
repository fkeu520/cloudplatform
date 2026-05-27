package com.cloudhub.platform.user.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
public class LoginLog {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String username;
    private Integer userType;
    private Long tenantId;
    private Integer loginType;
    private String ip;
    private String location;
    private String device;
    private String browser;
    private String os;
    private Integer status;
    private String message;
    private LocalDateTime loginTime;
    private LocalDateTime createTime;
}
