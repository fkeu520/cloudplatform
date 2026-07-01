package com.cloudhub.platform.enterprise.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 企业评分规则 (park-enterprise V56)
 *
 * <p>对应 csyh Rating (V2021111001 + V2022061001)。
 * 评级: 1=优 2=良 3=中 4=差, 触发条件: 逾期次数/欠费金额/日期数。
 * 业务方通过 {@code RatingController.save(ValidList<RatingVO>)} 批量配置 4 行。
 * AOP {@code RatingInitAspect} 在企业更新/支付时自动按规则重算 percentile_score。
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_rating")
public class Rating extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户 ID */
    private Long tenantId;

    /** 园区 ID (多园区时区分, V56 暂未强制) */
    private Long parkId;

    /** 评级 1=优 2=良 3=中 4=差 */
    private Integer level;

    /** 逾期次数起 */
    private Integer overdueMin;

    /** 逾期次数止 */
    private Integer overdueMax;

    /** 欠费金额起 (元) */
    private BigDecimal debtsMin;

    /** 欠费金额止 (元) */
    private BigDecimal debtsMax;

    /** 日期数 (e.g. 30=30天) */
    private Integer dateNum;

    /** 日期单位 (day/month/year) */
    private String dateUnit;

    /** 启用状态 (1=启用 0=停用) */
    private Integer status;
}
