package com.cloudhub.platform.contract.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 园区合同 (park-contract 第一张业务表)
 *
 * <p>W3.2b 阶段接入, 与 sys_room 1:1 关联.
 * 状态机见 {@link com.cloudhub.platform.contract.service.ContractStatus}.</p>
 *
 * @author csyh fusion W3.2b
 * @since 2026-06-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_contract")
public class Contract extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 关联房屋 ID (sys_room) */
    private Long roomId;

    /** 园区 ID */
    private Long parkId;

    /** 合同编号 (e.g. CT-2026-0001) */
    private String contractNo;

    /** 承租方名称 */
    private String tenantName;

    /** 承租方电话 */
    private String tenantPhone;

    /** 合同开始日期 */
    private LocalDateTime startDate;

    /** 合同结束日期 */
    private LocalDateTime endDate;

    /** 月租金 (元) */
    private BigDecimal monthlyRent;

    /** 押金 (元) */
    private BigDecimal deposit;

    /** 付款方式: MONTHLY/QUARTERLY/YEARLY */
    private String paymentType;

    /** 状态: 0=草稿 1=已签约 2=已到期 3=已终止 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 租户 ID */
    private Long tenantId;
}