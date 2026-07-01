package com.cloudhub.platform.enterprise.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 企业地址信息 (park-enterprise V55)
 *
 * <p>对应 csyh EnterpriseAddress (V2021111001)。
 * 与 sys_enterprise 1:1 关系 (UNIQUE KEY uk_address_enterprise)。
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_enterprise_address")
public class EnterpriseAddress extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户 ID */
    private Long tenantId;

    /** 企业 ID (sys_enterprise.id, 1:1) */
    private Long enterpriseId;

    /** 省份简称 (e.g. 广东) */
    private String base;

    /** 市 */
    private String city;

    /** 区 */
    private String district;

    /** 完整注册地址 */
    private String regLocation;
}
