package com.cloudhub.platform.property.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 园区楼宇 (park-property 第一张业务表)
 *
 * <p>csyh 业务融合 W3.2 阶段 - 物业管理 park-property 的核心实体.
 * 与 park-space (Room) 是 1:N 关系 (1 栋楼有 N 个房间), 后续 W3+ 阶段会通过 sys_room.building_id 外键关联.</p>
 *
 * @author csyh fusion W3.2
 * @since 2026-06-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_building")
public class Building extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 园区 ID */
    private Long parkId;

    /** 楼宇编号 (e.g. A/B/C) */
    private String buildingNo;

    /** 楼宇名称 (e.g. A 座) */
    private String buildingName;

    /** 总楼层数 */
    private Integer floors;

    /** 总建筑面积 (m²) */
    private BigDecimal totalArea;

    /** 建成年份 */
    private Integer buildYear;

    /** 楼宇负责人 */
    private String manager;

    /** 负责人电话 */
    private String managerPhone;

    /** 状态: 0=停用 1=正常 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 租户 ID (P0-1 多租户拦截器) */
    private Long tenantId;
}
