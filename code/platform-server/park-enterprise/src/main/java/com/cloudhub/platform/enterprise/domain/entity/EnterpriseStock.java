package com.cloudhub.platform.enterprise.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 企业股票信息 (park-enterprise V55)
 *
 * <p>对应 csyh EnterpriseStock (V2021111001)。
 * 与 sys_enterprise 1:1 关系 (UNIQUE KEY uk_stock_enterprise)。
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_enterprise_stock")
public class EnterpriseStock extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户 ID */
    private Long tenantId;

    /** 企业 ID (sys_enterprise.id, 1:1) */
    private Long enterpriseId;

    /** 股票号 */
    private String bondNum;

    /** 股票名 */
    private String bondName;

    /** 股票曾用名 */
    private String usedBondName;

    /** 股票类型 (沪 A/深 A/港股/...) */
    private String bondType;
}
