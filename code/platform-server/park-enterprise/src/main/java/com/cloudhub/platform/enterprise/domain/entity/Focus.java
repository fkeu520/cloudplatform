package com.cloudhub.platform.enterprise.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 关注标签主表 (park-enterprise V57)
 *
 * <p>对应 csyh {@code FocusPO} (V2022032201)。
 * 一个关注标签下挂多个 FocusItem 内容选项, 企业通过 EnterpriseFocus 关联。
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_focus")
public class Focus extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户 ID (P0-1 拦截器) */
    private Long tenantId;

    /** 园区 ID */
    private Long parkId;

    /** 标签名称 (e.g. 高新技术/规模以上/科技型) */
    private String name;

    /** 排序 (小的在前) */
    private Integer sorting;

    /** 启用状态 (1=启用 0=停用) */
    private Integer status;
}
