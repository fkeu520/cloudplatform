-- =============================================================================
-- V53__init_enterprise.sql
-- park-enterprise Phase 1 (csyh 业务融合)
--
-- 迁移来源:
--   csyh `pai-enterprise-csyh-2.x/enterprise-std`
--   EnterprisePO + EnterpriseTagPO + EnterpriseEntBindPO
--
-- 平台改造:
--   - String id (csyh UUID) -> BIGINT 雪花 ID (平台 P0-1)
--   - 加 tenant_id (平台 P0-1 多租户拦截器)
--   - 加 BaseEntity 审计字段 (create_by, create_time, update_by, update_time, deleted)
--   - 加逻辑删除字段 (deleted TINYINT 0/1) 替代 csyh is_deleted (Integer 0/1)
--
-- 决策 (2026-06-30 默认):
--   - Q1: CustomerInformation 表进 Phase 2,本迁移不包含
--   - Q2: cloud/* 70 文件全 defer,is_sync/is_fill 字段保留占位 (不写入逻辑)
--   - Q3: Maven park-enterprise,package com.cloudhub.platform.enterprise
--   - Q4: 表放 platform 库
-- =============================================================================

-- =============================================================================
-- sys_enterprise: 企业档案主表
-- =============================================================================
CREATE TABLE sys_enterprise (
    id                       BIGINT       NOT NULL                                          COMMENT '雪花 ID (主键)',
    tenant_id                BIGINT       DEFAULT NULL                                      COMMENT '租户 ID (P0-1 拦截器注入)',

    -- 基本信息 (来自 csyh EnterprisePO)
    name                     VARCHAR(255) NOT NULL                                          COMMENT '企业名称 (必填)',
    alias                    VARCHAR(255) DEFAULT NULL                                      COMMENT '简称',
    history_names            VARCHAR(500) DEFAULT NULL                                      COMMENT '曾用名',
    history_name_list        VARCHAR(2000) DEFAULT NULL                                     COMMENT '历史曾用名列表',
    eng_name                 VARCHAR(255) DEFAULT NULL                                      COMMENT '英文名',
    tax_number               VARCHAR(64)  DEFAULT NULL                                      COMMENT '纳税人识别号',
    credit_code              VARCHAR(64)  DEFAULT NULL                                      COMMENT '统一社会信用代码',
    industry                 VARCHAR(64)  DEFAULT NULL                                      COMMENT '行业',
    category                 VARCHAR(64)  DEFAULT NULL                                      COMMENT '国民经济行业分类门类',
    category_big             VARCHAR(64)  DEFAULT NULL                                      COMMENT '大类',
    category_middle          VARCHAR(64)  DEFAULT NULL                                      COMMENT '中类',
    category_small           VARCHAR(64)  DEFAULT NULL                                      COMMENT '小类',

    -- 法人 + 注册资本
    legal_person_name        VARCHAR(64)  DEFAULT NULL                                      COMMENT '法人姓名',
    type                     INT          DEFAULT NULL                                      COMMENT '法人类型 (1=人,2=公司)',
    company_org_type         VARCHAR(64)  DEFAULT NULL                                      COMMENT '企业类型',
    reg_capital              VARCHAR(64)  DEFAULT NULL                                      COMMENT '注册资本',
    reg_capital_currency     VARCHAR(16)  DEFAULT NULL                                      COMMENT '注册资本币种',
    actual_capital           VARCHAR(64)  DEFAULT NULL                                      COMMENT '实收资本',
    actual_capital_currency  VARCHAR(16)  DEFAULT NULL                                      COMMENT '实收币种',
    reg_number               VARCHAR(64)  DEFAULT NULL                                      COMMENT '注册号',
    org_number               VARCHAR(64)  DEFAULT NULL                                      COMMENT '组织机构代码',

    -- 日期
    estiblish_time           DATETIME     DEFAULT NULL                                      COMMENT '成立日期',
    from_time                DATETIME     DEFAULT NULL                                      COMMENT '经营开始日期',
    to_time                  DATETIME     DEFAULT NULL                                      COMMENT '经营结束日期',
    approved_time            DATETIME     DEFAULT NULL                                      COMMENT '核准时间',
    revoke_date              DATETIME     DEFAULT NULL                                      COMMENT '吊销日期',
    cancel_date              DATETIME     DEFAULT NULL                                      COMMENT '注销日期',

    -- 注册地址 + 登记机关
    base                     VARCHAR(32)  DEFAULT NULL                                      COMMENT '省份',
    city                     VARCHAR(64)  DEFAULT NULL                                      COMMENT '市',
    district                 VARCHAR(64)  DEFAULT NULL                                      COMMENT '区',
    reg_location             VARCHAR(500) DEFAULT NULL                                      COMMENT '注册地址',
    reg_institute            VARCHAR(255) DEFAULT NULL                                      COMMENT '登记机关',

    -- 经营状态
    reg_status               VARCHAR(32)  DEFAULT NULL                                      COMMENT '企业状态',
    is_micro_ent             INT          DEFAULT NULL                                      COMMENT '是否小微企业 (0=否 1=是)',
    staff_num_range          VARCHAR(32)  DEFAULT NULL                                      COMMENT '人员规模',
    social_staff_num         INT          DEFAULT NULL                                      COMMENT '参保人数',
    business_scope           TEXT                                                          COMMENT '经营范围',

    -- 联系信息
    phone_number             VARCHAR(32)  DEFAULT NULL                                      COMMENT '联系电话',
    email                    VARCHAR(128) DEFAULT NULL                                      COMMENT '邮箱',
    website_list             VARCHAR(1000) DEFAULT NULL                                     COMMENT '网站列表',

    -- 标签 + 评分
    tags                     VARCHAR(500) DEFAULT NULL                                      COMMENT '标签列表',
    percentile_score         INT          DEFAULT NULL                                      COMMENT '评分',

    -- 股票相关 (来自 csyh EnterprisePO)
    bond_num                 VARCHAR(64)  DEFAULT NULL                                      COMMENT '股票号',
    bond_name                VARCHAR(64)  DEFAULT NULL                                      COMMENT '股票名',
    used_bond_name           VARCHAR(64)  DEFAULT NULL                                      COMMENT '股票曾用名',
    bond_type                VARCHAR(32)  DEFAULT NULL                                      COMMENT '股票类型',

    -- 自身状态
    status                   INT          NOT NULL DEFAULT 1                               COMMENT '启用状态 (1=启用 0=停用)',

    -- 第三方关联 (Phase 3 用,字段保留)
    logo                     VARCHAR(500) DEFAULT NULL                                      COMMENT '企业 logo URL',
    origin_id                VARCHAR(64)  DEFAULT NULL                                      COMMENT '天眼查 origin ID (csyh 保留)',
    is_sync                  INT          NOT NULL DEFAULT 0                               COMMENT '是否已同步 ES (Phase 3 占位)',
    is_fill                  INT          NOT NULL DEFAULT 0                               COMMENT '云企库同步状态 (Phase 3 占位)',

    -- 附件引用 (MinIO object key)
    accessory                VARCHAR(500) DEFAULT NULL                                      COMMENT '上传附件 key',
    org_file_id              VARCHAR(64)  DEFAULT NULL                                      COMMENT '组织架构附件 key',
    executive_file_id        VARCHAR(64)  DEFAULT NULL                                      COMMENT '公司高管附件 key',
    investor_file_id         VARCHAR(64)  DEFAULT NULL                                      COMMENT '投资方附件 key',

    -- BaseEntity 审计字段
    create_by                VARCHAR(64)  DEFAULT NULL                                      COMMENT '创建人 loginName',
    create_time              DATETIME     DEFAULT NULL                                      COMMENT '创建时间 (auto-fill)',
    update_by                VARCHAR(64)  DEFAULT NULL                                      COMMENT '更新人 loginName',
    update_time              DATETIME     DEFAULT NULL                                      COMMENT '更新时间 (auto-fill)',
    deleted                  INT          NOT NULL DEFAULT 0                               COMMENT '逻辑删除 (0=否 1=是, MP 自动填充)',

    PRIMARY KEY (id),
    KEY idx_enterprise_tenant (tenant_id),
    KEY idx_enterprise_name   (name),
    KEY idx_enterprise_credit (credit_code),
    KEY idx_enterprise_origin (origin_id),
    KEY idx_enterprise_status (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业档案主表 (park-enterprise V53)';

-- =============================================================================
-- sys_enterprise_tag: 企业标签关联表
-- =============================================================================
CREATE TABLE sys_enterprise_tag (
    id            BIGINT      NOT NULL                                                COMMENT '雪花 ID',
    tenant_id     BIGINT      DEFAULT NULL                                            COMMENT '租户 ID',
    enterprise_id BIGINT      NOT NULL                                                COMMENT '企业 ID (sys_enterprise.id)',
    tag_name      VARCHAR(64) NOT NULL                                                COMMENT '标签名 (必填)',
    tag_color     VARCHAR(16) DEFAULT NULL                                            COMMENT '颜色 hex (e.g. #1890ff)',
    sort_order    INT         NOT NULL DEFAULT 0                                       COMMENT '排序 (小的在前)',
    remark        VARCHAR(255) DEFAULT NULL                                            COMMENT '备注',

    -- BaseEntity
    create_by     VARCHAR(64) DEFAULT NULL                                            COMMENT '创建人',
    create_time   DATETIME    DEFAULT NULL                                            COMMENT '创建时间',
    update_by     VARCHAR(64) DEFAULT NULL                                            COMMENT '更新人',
    update_time   DATETIME    DEFAULT NULL                                            COMMENT '更新时间',
    deleted       INT         NOT NULL DEFAULT 0                                       COMMENT '逻辑删除',

    PRIMARY KEY (id),
    KEY idx_ent_tag_tenant (tenant_id),
    KEY idx_ent_tag_ent   (enterprise_id),
    KEY idx_ent_tag_name  (tag_name),
    UNIQUE KEY uk_ent_tag_unique (enterprise_id, tag_name, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业标签关联 (park-enterprise V53)';

-- =============================================================================
-- sys_enterprise_ent_bind: 企业绑定关系
-- =============================================================================
CREATE TABLE sys_enterprise_ent_bind (
    id            BIGINT       NOT NULL                                               COMMENT '雪花 ID',
    tenant_id     BIGINT       DEFAULT NULL                                           COMMENT '租户 ID',
    enterprise_id BIGINT       NOT NULL                                               COMMENT '企业 ID (sys_enterprise.id)',

    -- 绑定目标 (csyh 用 entType enum,我们用字符串扩展)
    bind_type     VARCHAR(32)  NOT NULL                                               COMMENT '绑定类型: park / building / tenant / room',
    bind_id       BIGINT       NOT NULL                                               COMMENT '绑定对象 ID (按 bind_type 解释)',
    bind_status   INT          NOT NULL DEFAULT 1                                      COMMENT '绑定状态 (1=有效 0=失效)',

    binding_at    DATETIME     DEFAULT NULL                                           COMMENT '绑定时间 (Phase 1 不强制,前端展示用)',
    unbound_at    DATETIME     DEFAULT NULL                                           COMMENT '解绑时间 (bind_status=0 时填)',
    remark        VARCHAR(500) DEFAULT NULL                                           COMMENT '备注',

    -- BaseEntity
    create_by     VARCHAR(64)  DEFAULT NULL                                           COMMENT '创建人',
    create_time   DATETIME     DEFAULT NULL                                           COMMENT '创建时间',
    update_by     VARCHAR(64)  DEFAULT NULL                                           COMMENT '更新人',
    update_time   DATETIME     DEFAULT NULL                                           COMMENT '更新时间',
    deleted       INT          NOT NULL DEFAULT 0                                      COMMENT '逻辑删除',

    PRIMARY KEY (id),
    KEY idx_bind_tenant    (tenant_id),
    KEY idx_bind_enterprise (enterprise_id),
    KEY idx_bind_target    (bind_type, bind_id),
    KEY idx_bind_status    (bind_status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业绑定关系 (park-enterprise V53)';

-- =============================================================================
-- 同步字典 (park-space V37 模式参考,但本批不强制 seed)
-- 后续 Phase 2 可在 V54 加企业类型/经营状态字典
-- =============================================================================
