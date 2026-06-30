package com.cloudhub.platform.enterprise.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企业标签关联 (park-enterprise 业务)
 *
 * <p>csyh EnterpriseTagPO 改造 — 多租户 + BaseEntity 风格.
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-06-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_enterprise_tag")
public class EnterpriseTag extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 租户 ID */
    private Long tenantId;

    /** 企业 ID (sys_enterprise.id) */
    private Long enterpriseId;

    /** 标签名 */
    private String tagName;

    /** 颜色 hex (e.g. #1890ff) */
    private String tagColor;

    /** 排序 (小的在前) */
    private Integer sortOrder;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createBy;

    /** 更新人 */
    private String updateBy;
}
