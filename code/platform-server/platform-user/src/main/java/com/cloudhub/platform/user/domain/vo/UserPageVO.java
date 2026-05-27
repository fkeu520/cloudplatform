package com.cloudhub.platform.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserPageVO {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    private String username;
    private String nickname;
    private String mobile;
    private String email;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long orgId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long deptId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long postId;
    private String orgName;
    private String deptName;
    private String postName;
    private String statusDesc;
    private Integer userType;
    private String userTypeDesc;
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;
}