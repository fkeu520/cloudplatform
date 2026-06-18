package com.cloudhub.platform.space.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 园区主表
 * <p>系统管理下的「园区管理」功能实体。与 space-std 各表的 park_id 字段关联，是空间/合同/财务等
 * 业务模块的顶层数据归属。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_park")
public class Park extends BaseEntity {

    /** 园区名称 */
    private String parkName;

    /** 省 */
    private String province;

    /** 市 */
    private String city;

    /** 区 */
    private String district;

    /** 详细地址 */
    private String address;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;

    /** 园区简介 */
    private String description;

    /** 占地面积（平方米） */
    private BigDecimal landArea;

    /** 建筑面积（平方米） */
    private BigDecimal buildingArea;

    /** 状态：0停用 1启用 */
    private Integer status;

    /** 租户ID */
    private Long tenantId;
}
