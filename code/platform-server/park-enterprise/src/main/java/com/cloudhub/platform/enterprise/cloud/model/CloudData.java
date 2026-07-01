package com.cloudhub.platform.enterprise.cloud.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 云企库数据统一实体 (Phase 3, 手动维护)
 *
 * <p>对应 csyh 11 个 cloud 模块的业务数据, 通过 {@code category} 分类 +
 * {@code dataContent} JSON 字段存储差异化数据结构.
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_enterprise_cloud_data")
public class CloudData extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户 ID */
    private Long tenantId;

    /** 关联企业 ID */
    private Long enterpriseId;

    /** 企业名称 (冗余) */
    private String enterpriseName;

    /** 数据分类 (见 CloudDataCategoryEnum) */
    private String category;

    /** 业务数据 (JSON 字符串, 结构因 category 而异) */
    private String dataContent;

    /** 数据年份 */
    private String dataYear;

    /** 数据日期 */
    private LocalDate dataDate;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 (0=禁用 1=启用) */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createBy;

    /** 更新人 */
    private String updateBy;
}
