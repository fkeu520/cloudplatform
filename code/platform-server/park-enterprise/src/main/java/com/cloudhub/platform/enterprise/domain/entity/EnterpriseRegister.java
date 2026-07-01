package com.cloudhub.platform.enterprise.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 企业注册信息 (park-enterprise V55)
 *
 * <p>对应 csyh EnterpriseRegister (V2021111001)。
 * 与 sys_enterprise 1:1 关系 (UNIQUE KEY uk_register_enterprise)。
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_enterprise_register")
public class EnterpriseRegister extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户 ID */
    private Long tenantId;

    /** 企业 ID (sys_enterprise.id, 1:1) */
    private Long enterpriseId;

    /** 注册号 */
    private String regNumber;

    /** 注册资本币种 (CNY/USD/EUR) */
    private String regCapitalCurrency;

    /** 实收注册资金 */
    private String actualCapital;

    /** 实收币种 */
    private String actualCapitalCurrency;

    /** 登记机关 */
    private String regInstitute;

    /** 核准时间 */
    private LocalDateTime approvedTime;
}
