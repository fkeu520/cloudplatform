package com.cloudhub.platform.space.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 园区房屋 (park-space 核心实体)
 *
 * <p>csyh 业务融合 Phase 1 扩展字段 (V37 SQL):
 * <ul>
 *   <li>重命名 area → areaCovered (对齐 csyh 命名规范)</li>
 *   <li>新增字段 (对齐 csyh std): roomName, buildArea, billableArea, unitPrice,
 *       totalPrice, kitId, purposeId, image, sorting, introduce</li>
 * </ul>
 *
 * <p>关联关系:
 * <ul>
 *   <li>parkId → sys_park.id</li>
 *   <li>buildingId → park-property.sys_building.id (跨模块)</li>
 *   <li>kitId → sys_kit.id (房间配套, W3.3 已有)</li>
 *   <li>purposeId → sys_room_purpose.id (房间用途, W3.5 已有)</li>
 * </ul>
 *
 * <p>W3 阶段: 仅基础 CRUD, W4+ 阶段: 关联 park-contract (合同计费) / park-finance (账单).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_room")
public class Room extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 园区 ID (csyh 强依赖字段) */
    private Long parkId;

    /** 楼宇 ID (跨模块: park-property.sys_building.id) */
    private Long buildingId;

    /** 楼层 ID (V38, csyh floorId → sys_floor.id, NULL 兼容旧数据) */
    private Long floorId;

    /** 楼层 */
    private Integer floor;

    /** 房号 (e.g. A-101, 园区+楼栋内唯一, V37 唯一索引 uk_park_building_room_no) */
    private String roomNo;

    // ===== V37 Phase 1 新增字段 (对齐 csyh std) =====

    /** 房间名称 (csyh roomName, max=64) */
    private String roomName;

    /** 建筑面积 (m², csyh areaCovered, V37 由 area 重命名) */
    private BigDecimal areaCovered;

    /** 套内面积 (m², must <= areaCovered, csyh buildArea) */
    private BigDecimal buildArea;

    /** 计费面积 (m², csyh billableArea) */
    private BigDecimal billableArea;

    /** 单价 (元/m²/月, csyh unitPrice) */
    private BigDecimal unitPrice;

    /** 总价 (元/月, csyh totalPrice, 衍生自 unit_price * billable_area) */
    private BigDecimal totalPrice;

    /** 月租金 (元, 旧字段保留, 与 total_price 区别是固定值 vs 衍生值) */
    private BigDecimal monthlyRent;

    /** 关联 sys_kit.id (房间配套, csyh kitId) */
    private Long kitId;

    /** 关联 sys_room_purpose.id (房间用途, csyh purposeId) */
    private Long purposeId;

    /** 房间图片 (JSON 数组, csyh image, max=1000) */
    private String image;

    /** 排序 (csyh sorting, 默认 0) */
    private Integer sorting;

    /** 房间介绍 (csyh introduce) */
    private String introduce;

    // ===== 旧字段 =====

    /** 类型: OFFICE / MEETING / STORAGE / PARKING (csyh roomType) */
    private String roomType;

    /** 状态: 0=空置 1=已租 2=装修中 3=停用 4=自用 5=已预订 (字典 room_status) */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 租户 ID (P0-1 多租户拦截器) */
    private Long tenantId;
}
