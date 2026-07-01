package com.cloudhub.platform.enterprise.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 企业关注标签关联 (park-enterprise V57)
 *
 * <p>对应 csyh {@code EnterpriseFocusPO} (V2022032201)。
 * 企业选择关注标签, 同时选择该标签下 1-N 个内容项。
 * focus_items 存 ID 列表 (逗号分隔), focus_item_names 存名称列表 (冗余便于展示)。
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_enterprise_focus")
public class EnterpriseFocus extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户 ID */
    private Long tenantId;

    /** 园区 ID */
    private Long parkId;

    /** 企业 ID (sys_enterprise.id) */
    private Long enterpriseId;

    /** 关注标签 ID (sys_focus.id) */
    private Long focusId;

    /** 冗余: 标签名称 (便于展示) */
    private String focusName;

    /** 关注内容 ID 列表 (逗号分隔) */
    private String focusItems;

    /** 冗余: 内容名称列表 (逗号分隔) */
    private String focusItemNames;

    /** 启用状态 */
    private Integer status;
}
