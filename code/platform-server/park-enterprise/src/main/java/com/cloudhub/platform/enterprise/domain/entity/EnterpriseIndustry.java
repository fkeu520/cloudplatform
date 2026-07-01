package com.cloudhub.platform.enterprise.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 企业行业类型 (park-enterprise V54)
 *
 * <p>对应 csyh {@code pai-enterprise-csyh-2.x} EnterpriseIndustryVO。
 * 存储 GB/T 4754-2017 国民经济行业分类 (门类/大类/中类/小类)。
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_enterprise_industry")
public class EnterpriseIndustry extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户 ID (P0-1 拦截器自动填充) */
    private Long tenantId;

    /** 行业代码 (门类字母+大中小类数字, e.g. "A" / "A01" / "A0111") */
    private String code;

    /** 门类 (e.g. 信息传输/软件和信息技术服务业) */
    private String category;

    /** 大类 */
    private String categoryBig;

    /** 中类 */
    private String categoryMiddle;

    /** 小类 */
    private String categorySmall;

    /** 启用状态 (1=启用 0=停用) */
    private Integer status;
}
