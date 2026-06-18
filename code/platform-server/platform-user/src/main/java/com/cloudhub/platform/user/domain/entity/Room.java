package com.cloudhub.platform.user.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 园区房屋 (park-space 第一张业务表)
 *
 * <p>csyh 业务融合 W3 阶段 - 空间中心 park-space 的核心实体.
 * 字段精简, 后续按需扩展 (工位/楼层/楼宇等会拆成独立表).</p>
 *
 * <p>W3 阶段: 仅基础 CRUD, 不涉及租金计算/合同关联等业务逻辑.
 * W4+ 阶段: 关联 sys_contract (park-contract) / sys_tenant (park-property) 等.</p>
 *
 * @author csyh fusion W3
 * @since 2026-06-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_room")
public class Room extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 园区 ID (csyh 强依赖字段) */
    private Long parkId;

    /** 楼宇 ID */
    private Long buildingId;

    /** 楼层 */
    private Integer floor;

    /** 房号 (e.g. A-101) */
    private String roomNo;

    /** 类型: OFFICE / MEETING / STORAGE / PARKING */
    private String roomType;

    /** 面积 (m²) */
    private BigDecimal area;

    /** 月租金 (元) */
    private BigDecimal monthlyRent;

    /** 状态: 0=空置 1=已租 2=装修中 3=停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 租户 ID (P0-1 多租户拦截器) */
    private Long tenantId;
}
