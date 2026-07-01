package com.cloudhub.platform.enterprise.cloud.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 国民经济行业分类参考实体 (NationalEconomy)
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_enterprise_national_economy")
public class NationalEconomy extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;

    /** 分类编码 */
    private String code;

    /** 分类名称 */
    private String name;

    /** 父级编码 */
    private String parentCode;

    /** 层级 (1=门类 2=大类 3=中类 4=小类) */
    private Integer level;

    private Integer sortOrder;

    private Integer status;

    private String remark;

    private String createBy;

    private String updateBy;
}
