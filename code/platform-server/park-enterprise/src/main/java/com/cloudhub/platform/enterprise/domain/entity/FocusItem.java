package com.cloudhub.platform.enterprise.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 关注标签内容 (park-enterprise V57)
 *
 * <p>对应 csyh {@code FocusItemPO} (V2022032201)。
 * 一个 Focus 标签下挂多个 Item, e.g. 标签=企业规模, Items=[微型企业/小型企业/中型企业/大型企业]。
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_focus_item")
public class FocusItem extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户 ID */
    private Long tenantId;

    /** 园区 ID */
    private Long parkId;

    /** 关注标签 ID (sys_focus.id) */
    private Long focusId;

    /** 内容名称 */
    private String name;

    /** 排序 */
    private Integer sorting;

    /** 启用状态 */
    private Integer status;
}
