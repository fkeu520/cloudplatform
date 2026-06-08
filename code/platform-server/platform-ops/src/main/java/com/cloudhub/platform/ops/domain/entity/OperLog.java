package com.cloudhub.platform.ops.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_oper_log")
public class OperLog {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String title;
    private Integer businessType;
    private String method;
    private String requestMethod;
    private Integer operatorType;
    private String operName;
    private String deptName;
    /** M5 P0-2 PR4: 部门ID (操作发生时所在部门, 用于数据权限审计) */
    private Long deptId;
    private String operUrl;
    private String operIp;
    private String operLocation;
    private String operParam;
    private String jsonResult;
    private Integer status;
    private String errorMsg;
    private LocalDateTime operTime;
    private Long costTime;
}
