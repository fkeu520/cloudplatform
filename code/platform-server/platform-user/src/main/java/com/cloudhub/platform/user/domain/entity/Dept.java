package com.cloudhub.platform.user.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class Dept extends BaseEntity {
    
    private Long orgId;            // 所属组织ID
    private Long parentId;         // 父部门ID
    private String name;           // 部门名称
    private String code;           // 部门编码
    private String manager;        // 部门负责人
    private String phone;          // 联系电话
    private Integer sort;          // 排序
    private Integer status;        // 状态
    
    @TableField(exist = false)
    private List<Dept> children = new ArrayList<>();
}