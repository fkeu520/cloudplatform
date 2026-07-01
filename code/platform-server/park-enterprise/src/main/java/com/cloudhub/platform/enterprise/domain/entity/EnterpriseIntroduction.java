package com.cloudhub.platform.enterprise.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 企业介绍 (park-enterprise V55)
 *
 * <p>对应 csyh EnterpriseIntroduction (V2021111001)。
 * 存储经营范围/人员规模/标签/评分等介绍信息。
 * 与 sys_enterprise 1:1 关系 (UNIQUE KEY uk_intro_enterprise)。
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_enterprise_introduction")
public class EnterpriseIntroduction extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户 ID (P0-1 拦截器自动填充) */
    private Long tenantId;

    /** 企业 ID (sys_enterprise.id, 1:1) */
    private Long enterpriseId;

    /** 历史曾用名列表 (逗号分隔) */
    private String historyNameList;

    /** 人员规模 (e.g. 100-499) */
    private String staffNumRange;

    /** 参保人数 */
    private Integer socialStaffNum;

    /** 经营范围 */
    private String businessScope;

    /** 法人类型 1=人 2=公司 */
    private Integer type;

    /** 企业类型 (e.g. 有限责任公司) */
    private String companyOrgType;

    /** 标签列表 (逗号分隔) */
    private String tags;

    /** 企业评分 (1-100, V56 Rating AOP 自动计算) */
    private Integer percentileScore;
}
