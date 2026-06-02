package com.cloudhub.platform.user.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_post")
public class Post extends BaseEntity {
    
    private Long orgId;            // 所属组织ID
    private Long deptId;           // 所属部门ID
    private String name;           // 岗位名称
    private String code;           // 岗位编码
    private String level;          // 岗位级别（如P1-P10）
    private Integer sort;          // 排序
    private Integer status;        // 状态
}