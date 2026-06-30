package com.cloudhub.platform.enterprise.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 企业绑定关系 (park-enterprise 业务)
 *
 * <p>csyh EnterpriseEntBindPO 改造 — bind_type 用字符串扩展 (park/building/tenant/room).
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-06-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_enterprise_ent_bind")
public class EnterpriseEntBind extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 租户 ID */
    private Long tenantId;

    /** 企业 ID (sys_enterprise.id) */
    private Long enterpriseId;

    /** 绑定类型: park / building / tenant / room */
    private String bindType;

    /** 绑定对象 ID (按 bind_type 解释) */
    private Long bindId;

    /** 绑定状态 (1=有效 0=失效) */
    private Integer bindStatus;

    /** 绑定时间 */
    private LocalDateTime bindingAt;

    /** 解绑时间 (bind_status=0 时填) */
    private LocalDateTime unboundAt;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createBy;

    /** 更新人 */
    private String updateBy;
}
