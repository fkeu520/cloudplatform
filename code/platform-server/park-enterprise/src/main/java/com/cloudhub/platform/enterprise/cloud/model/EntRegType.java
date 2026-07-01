package com.cloudhub.platform.enterprise.cloud.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 企业注册类型参考实体 (EntRegType)
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_enterprise_reg_type")
public class EntRegType extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;

    /** 类型编码 */
    private String code;

    /** 类型名称 */
    private String name;

    /** 父级编码 */
    private String parentCode;

    private Integer sortOrder;

    private Integer status;

    private String remark;

    private String createBy;

    private String updateBy;
}
