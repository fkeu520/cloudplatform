package com.cloudhub.platform.enterprise.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 企业注销信息 (park-enterprise V55)
 *
 * <p>对应 csyh EnterpriseUnregister (V2021111001)。
 * 与 sys_enterprise 1:1 关系 (UNIQUE KEY uk_unregister_enterprise)。
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_enterprise_unregister")
public class EnterpriseUnregister extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户 ID */
    private Long tenantId;

    /** 企业 ID (sys_enterprise.id, 1:1) */
    private Long enterpriseId;

    /** 吊销日期 */
    private LocalDateTime revokeDate;

    /** 吊销原因 */
    private String revokeReason;

    /** 注销日期 */
    private LocalDateTime cancelDate;

    /** 注销原因 */
    private String cancelReason;
}
