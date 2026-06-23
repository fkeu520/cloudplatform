package com.cloudhub.platform.property.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 园区楼宇 (park-property 核心实体)
 *
 * <p>csyh 业务融合 Phase 1 扩展字段 (V36 SQL):
 * <ul>
 *   <li>对齐 csyh std `sys_building` 字段: buildingCode, floorNumber, underground,
 *       areaCovered, propertyRight, buildingSafety, shareArea, leaseMethod,
 *       sorting, certificate, image</li>
 *   <li>保留 W3.2 阶段字段: buildingNo (e.g. A/B/C), buildYear, manager, managerPhone, remark
 *       (用户额外需求, csyh 无但当前项目需要)</li>
 * </ul>
 *
 * <p>关联关系:
 * <ul>
 *   <li>park-space (Room) 通过 building_id 外键引用 (跨模块)</li>
 *   <li>park-space (Floor) 通过 building_id 外键引用 (跨模块)</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_building")
public class Building extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 园区 ID */
    private Long parkId;

    /** 区域 ID (关联 sys_area.id) */
    private Long areaId;

    // ===== V36 Phase 1 新增字段 (对齐 csyh std) =====

    /** 楼栋编号 (园区内唯一, csyh buildingCode, V36 唯一索引 uk_park_building_code) */
    private String buildingCode;

    /** 楼宇编号 (e.g. A/B/C, 旧字段保留兼容, V36 已同步到 buildingCode) */
    private String buildingNo;

    /** 楼宇名称 (e.g. A 座) */
    private String buildingName;

    /** 地上层数 (csyh floorNumber, 默认 1) */
    private Integer floorNumber;

    /** 地下层数 (csyh underground, 默认 0) */
    private Integer underground;

    /** 总楼层数 (旧字段保留, 等同 floorNumber + underground) */
    private Integer floors;

    /** 建筑面积 (m², csyh areaCovered, V36 由 total_area 同步) */
    private BigDecimal areaCovered;

    /** 总建筑面积 (m², 旧字段保留) */
    private BigDecimal totalArea;

    /** 产权性质 (字典 property_right: 0=国有 1=集体 2=共有 3=个人, csyh propertyRigth) */
    private Integer propertyRight;

    /** 建筑结构 (字典 building_structure: 0=框架 1=钢 2=钢混 3=砖混 4=混合 5=砖木 6=其他, csyh buildingSafety) */
    private Integer buildingSafety;

    /** 公摊面积 (m², must <= areaCovered, csyh shareArea) */
    private BigDecimal shareArea;

    /** 租赁方式 (字典 lease_method: 0=直租 1=转租 2=联租 9=其他, csyh leaseMethod) */
    private Integer leaseMethod;

    /** 排序 (csyh sorting) */
    private Integer sorting;

    /** 产权证号 (csyh certificate, max=100) */
    private String certificate;

    /** 楼栋图片 (JSON 数组, 多图, csyh image, max=1000) */
    private String image;

    // ===== 旧字段保留 =====

    /** 建成年份 */
    private Integer buildYear;

    /** 楼宇负责人 */
    private String manager;

    /** 负责人电话 */
    private String managerPhone;

    /** 备注 */
    private String remark;

    /** 状态: 0=停用 1=启用 */
    private Integer status;

    /** 租户 ID (P0-1 多租户拦截器) */
    private Long tenantId;
}
