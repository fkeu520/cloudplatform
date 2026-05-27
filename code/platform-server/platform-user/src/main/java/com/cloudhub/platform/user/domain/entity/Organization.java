package com.cloudhub.platform.user.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_organization")
public class Organization extends BaseEntity {
    
    private Long parentId;         // 父组织ID
    private String name;           // 组织名称
    private String code;           // 组织编码
    private Integer type;          // 类型 1集团 2总部 3子公司 4分公司
    private String shortName;      // 简称
    private String fullName;       // 全称
    private String legalPerson;    // 法定代表人
    private String registeredAddress; // 注册地址
    private String businessScope;  // 经营范围
    private String taxNumber;      // 税号
    private String phone;          // 联系电话
    private Integer sort;          // 排序
    private Integer status;        // 状态
    private Long tenantId;         // 租户ID
}