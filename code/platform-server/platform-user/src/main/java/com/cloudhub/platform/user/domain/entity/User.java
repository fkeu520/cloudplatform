package com.cloudhub.platform.user.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {
    
    private String username;        // 用户名
    private String password;        // 密码（加密存储）
    private String nickname;       // 昵称
    private String avatar;          // 头像URL
    private String mobile;          // 手机号
    private String email;          // 邮箱
    private Integer gender;        // 性别 0未知 1男 2女
    private Long orgId;            // 组织ID
    private Long deptId;           // 部门ID
    private Long postId;           // 岗位ID
    private Integer status;        // 状态 0禁用 1启用
    private Integer tenantId;     // 租户ID
    private Integer userType;    // 用户类型: 0=普通用户 1=租户管理员 2=运营管理员
    private String lastLoginIp;   // 最后登录IP
    private LocalDateTime lastLoginTime; // 最后登录时间
}
