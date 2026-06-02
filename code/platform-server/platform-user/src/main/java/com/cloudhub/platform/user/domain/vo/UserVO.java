package com.cloudhub.platform.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String mobile;
    private String email;
    private Integer gender;
    private String genderDesc;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long orgId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long deptId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long postId;
    private String orgName;
    private String deptName;
    private String postName;
    private Integer status;
    private String statusDesc;
    private Integer userType;
    private String userTypeDesc;
    private Long tenantId;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private List<String> roles;    // 角色名称列表
    private List<String> perms;    // 权限标识列表
    private List<Long> roleIds;    // 角色ID列表（供前端编辑使用）
}