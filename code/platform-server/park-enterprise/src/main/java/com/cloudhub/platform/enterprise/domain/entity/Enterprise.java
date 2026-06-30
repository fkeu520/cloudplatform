package com.cloudhub.platform.enterprise.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cloudhub.platform.common.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 企业档案 (park-enterprise 核心实体)
 *
 * <p>csyh 业务融合 Phase 1: Enterprise 字段基于 csyh {@code pai-enterprise-csyh-2.x}
 *    EnterprisePO 重写 (60+ 字段 → 平台 Lombok {@code @Data} 风格)。
 *
 * <p>改造点 (per 业务模块迁移流程 §七 Step 7):
 * <ul>
 *   <li>String id (csyh UUID 字符串) → Long id (雪花 ID, BaseEntity 已带)</li>
 *   <li>添加 tenant_id (P0-1 多租户拦截器)</li>
 *   <li>保留所有 csyh EnterprisePO 字段便于完整业务;Phase 1 控制器只暴露核心字段</li>
 *   <li>{@code @JsonFormat(shape=STRING)} on id 继承自 BaseEntity (Snowflake 精度保护)</li>
 * </ul>
 *
 * <p>关联关系:
 * <ul>
 *   <li>sys_enterprise_tag.enterprise_id → 本表.id (1:N)</li>
 *   <li>sys_enterprise_ent_bind.enterprise_id → 本表.id (1:N)</li>
 * </ul>
 *
 * <p>Phase 3 扩展预留:
 * <ul>
 *   <li>origin_id (天眼查) / is_sync (ES) / is_fill (云企库) 已建字段,逻辑延后</li>
 * </ul>
 *
 * @author Sisyphus (csyh 迁移)
 * @since 2026-06-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_enterprise")
public class Enterprise extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 租户 ID (P0-1 多租户拦截器自动填充;@DataField 需配置 ignoreTable 或始终手动) */
    private Long tenantId;

    // ===== 基本信息 (Phase 1 核心字段) =====

    /** 企业名称 (必填) */
    private String name;

    /** 简称 */
    private String alias;

    /** 曾用名 */
    private String historyNames;

    /** 历史曾用名列表 (JSON/字符串) */
    private String historyNameList;

    /** 英文名 */
    private String engName;

    /** 纳税人识别号 */
    private String taxNumber;

    /** 统一社会信用代码 */
    private String creditCode;

    /** 行业 */
    private String industry;

    /** 国民经济行业分类门类 */
    private String category;

    /** 大类 */
    private String categoryBig;

    /** 中类 */
    private String categoryMiddle;

    /** 小类 */
    private String categorySmall;

    // ===== 法人 + 注册资本 =====

    /** 法人姓名 */
    private String legalPersonName;

    /** 法人类型 (1=人,2=公司) */
    private Integer type;

    /** 企业类型 */
    private String companyOrgType;

    /** 注册资本 */
    private String regCapital;

    /** 注册资本币种 (人民币/美元/欧元) */
    private String regCapitalCurrency;

    /** 实收资本 */
    private String actualCapital;

    /** 实收币种 */
    private String actualCapitalCurrency;

    /** 注册号 */
    private String regNumber;

    /** 组织机构代码 */
    private String orgNumber;

    // ===== 日期 =====

    /** 成立日期 */
    private LocalDateTime estiblishTime;

    /** 经营开始日期 */
    private LocalDateTime fromTime;

    /** 经营结束日期 */
    private LocalDateTime toTime;

    /** 核准时间 */
    private LocalDateTime approvedTime;

    /** 吊销日期 */
    private LocalDateTime revokeDate;

    /** 注销日期 */
    private LocalDateTime cancelDate;

    // ===== 注册地址 =====

    /** 省份 */
    private String base;

    /** 市 */
    private String city;

    /** 区 */
    private String district;

    /** 注册地址 */
    private String regLocation;

    /** 登记机关 */
    private String regInstitute;

    // ===== 经营状态 =====

    /** 企业状态 (在营/吊销/注销 等) */
    private String regStatus;

    /** 是否小微企业 (0=否 1=是) */
    private Integer isMicroEnt;

    /** 人员规模 (e.g. 100-499 人) */
    private String staffNumRange;

    /** 参保人数 */
    private Integer socialStaffNum;

    /** 经营范围 */
    private String businessScope;

    // ===== 联系信息 =====

    /** 联系电话 */
    private String phoneNumber;

    /** 邮箱 */
    private String email;

    /** 网站列表 */
    private String websiteList;

    // ===== 标签 + 评分 =====

    /** 标签列表 (冗余字段,主存 sys_enterprise_tag) */
    private String tags;

    /** 评分 */
    private Integer percentileScore;

    // ===== 股票相关 =====

    /** 股票号 */
    private String bondNum;

    /** 股票名 */
    private String bondName;

    /** 股票曾用名 */
    private String usedBondName;

    /** 股票类型 */
    private String bondType;

    // ===== 自身状态 =====

    /** 启用状态 (1=启用 0=停用) */
    private Integer status;

    // ===== 第三方关联 (Phase 3 占位) =====

    /** 企业 logo URL */
    private String logo;

    /** 天眼查 origin ID (csyh 保留字段) */
    private String originId;

    /** 是否已同步 ES (Phase 3) */
    private Integer isSync;

    /** 云企库同步状态 (0=无需 1=等待 2=完成) */
    private Integer isFill;

    // ===== BaseEntity 扩展审计字段 (Phase 1 平台约定) =====

    /** 创建人 loginName (BaseEntity 不带,需单独声明) */
    private String createBy;

    /** 更新人 loginName */
    private String updateBy;
}
