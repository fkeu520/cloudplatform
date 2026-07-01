package com.cloudhub.platform.enterprise.cloud.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 企业概览数据实体 (Overview)
 *
 * <p>独立于 CloudData 统一表, 字段结构较稳定, 便于前台直接展示.
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_enterprise_overview_data")
public class EnterpriseOverview extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;

    private Long enterpriseId;

    private String enterpriseName;

    /** 注册资本 */
    private String regCapital;

    /** 总资产 */
    private String totalAssets;

    /** 年营收 */
    private String annualRevenue;

    /** 员工数 */
    private Integer employeeCount;

    /** 专利数 */
    private Integer patentCount;

    /** 商标数 */
    private Integer trademarkCount;

    /** 著作权数 */
    private Integer copyrightCount;

    /** 风险数 */
    private Integer riskCount;

    /** 招投标数 */
    private Integer bidCount;

    /** 股权结构 (JSON) */
    private String equityStructureJson;

    /** 其他概览数据 (JSON) */
    private String overviewJson;

    private Integer sortOrder;

    private Integer status;

    private String remark;

    private String createBy;

    private String updateBy;
}
