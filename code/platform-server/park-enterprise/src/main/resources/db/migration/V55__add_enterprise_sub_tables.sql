-- =============================================================================
-- V55__add_enterprise_sub_tables.sql
-- park-enterprise Phase 1 扩展
-- 来源: csyh pai-enterprise-csyh-2.x V2021111001 (拆分 5 张子表)
-- 5 张子表: introduction / register / stock / unregister / address
-- 主表 sys_enterprise 已有 60+ 字段, 子表存储更细分
-- =============================================================================

-- sys_enterprise_introduction: 企业介绍 (经营范围/标签/评分)
CREATE TABLE sys_enterprise_introduction (
    id                  BIGINT       NOT NULL                                        COMMENT '雪花 ID',
    tenant_id           BIGINT       DEFAULT NULL                                    COMMENT '租户 ID',
    enterprise_id       BIGINT       NOT NULL                                        COMMENT '企业 ID (sys_enterprise.id)',

    -- 介绍信息
    history_name_list   VARCHAR(2000) DEFAULT NULL                                   COMMENT '历史曾用名列表',
    staff_num_range     VARCHAR(32)  DEFAULT NULL                                    COMMENT '人员规模 (e.g. 100-499)',
    social_staff_num    INT          DEFAULT NULL                                    COMMENT '参保人数',
    business_scope      TEXT                                                        COMMENT '经营范围',
    type                INT          DEFAULT NULL                                    COMMENT '法人类型 1=人 2=公司',
    company_org_type    VARCHAR(64)  DEFAULT NULL                                    COMMENT '企业类型 (e.g. 有限责任公司)',
    tags                VARCHAR(500) DEFAULT NULL                                    COMMENT '标签列表 (逗号分隔)',
    percentile_score    INT          DEFAULT NULL                                    COMMENT '企业评分 (1-100)',

    -- BaseEntity
    create_by           VARCHAR(64)  DEFAULT NULL                                    COMMENT '创建人',
    create_time         DATETIME     DEFAULT NULL                                    COMMENT '创建时间',
    update_by           VARCHAR(64)  DEFAULT NULL                                    COMMENT '更新人',
    update_time         DATETIME     DEFAULT NULL                                    COMMENT '更新时间',
    deleted             INT          NOT NULL DEFAULT 0                               COMMENT '逻辑删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_intro_enterprise (enterprise_id, deleted),
    KEY idx_intro_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业介绍 (park-enterprise V55)';

-- sys_enterprise_register: 企业注册信息
CREATE TABLE sys_enterprise_register (
    id                       BIGINT       NOT NULL                                   COMMENT '雪花 ID',
    tenant_id                BIGINT       DEFAULT NULL                               COMMENT '租户 ID',
    enterprise_id            BIGINT       NOT NULL                                   COMMENT '企业 ID',

    -- 注册信息
    reg_number               VARCHAR(64)  DEFAULT NULL                               COMMENT '注册号',
    reg_capital_currency     VARCHAR(16)  DEFAULT NULL                               COMMENT '注册资本币种 (CNY/USD/EUR)',
    actual_capital           VARCHAR(64)  DEFAULT NULL                               COMMENT '实收注册资金',
    actual_capital_currency  VARCHAR(16)  DEFAULT NULL                               COMMENT '实收币种',
    reg_institute            VARCHAR(255) DEFAULT NULL                               COMMENT '登记机关',
    approved_time            DATETIME     DEFAULT NULL                               COMMENT '核准时间',

    -- BaseEntity
    create_by                VARCHAR(64)  DEFAULT NULL                               COMMENT '创建人',
    create_time              DATETIME     DEFAULT NULL                               COMMENT '创建时间',
    update_by                VARCHAR(64)  DEFAULT NULL                               COMMENT '更新人',
    update_time              DATETIME     DEFAULT NULL                               COMMENT '更新时间',
    deleted                  INT          NOT NULL DEFAULT 0                          COMMENT '逻辑删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_register_enterprise (enterprise_id, deleted),
    KEY idx_register_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业注册信息 (park-enterprise V55)';

-- sys_enterprise_stock: 股票信息
CREATE TABLE sys_enterprise_stock (
    id                  BIGINT       NOT NULL                                        COMMENT '雪花 ID',
    tenant_id           BIGINT       DEFAULT NULL                                    COMMENT '租户 ID',
    enterprise_id       BIGINT       NOT NULL                                        COMMENT '企业 ID',

    -- 股票信息
    bond_num            VARCHAR(64)  DEFAULT NULL                                    COMMENT '股票号',
    bond_name           VARCHAR(64)  DEFAULT NULL                                    COMMENT '股票名',
    used_bond_name      VARCHAR(500) DEFAULT NULL                                    COMMENT '股票曾用名',
    bond_type           VARCHAR(32)  DEFAULT NULL                                    COMMENT '股票类型 (沪 A/深 A/港股/...)',

    -- BaseEntity
    create_by           VARCHAR(64)  DEFAULT NULL                                    COMMENT '创建人',
    create_time         DATETIME     DEFAULT NULL                                    COMMENT '创建时间',
    update_by           VARCHAR(64)  DEFAULT NULL                                    COMMENT '更新人',
    update_time         DATETIME     DEFAULT NULL                                    COMMENT '更新时间',
    deleted             INT          NOT NULL DEFAULT 0                               COMMENT '逻辑删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_enterprise (enterprise_id, deleted),
    KEY idx_stock_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业股票信息 (park-enterprise V55)';

-- sys_enterprise_unregister: 注销信息
CREATE TABLE sys_enterprise_unregister (
    id                  BIGINT       NOT NULL                                        COMMENT '雪花 ID',
    tenant_id           BIGINT       DEFAULT NULL                                    COMMENT '租户 ID',
    enterprise_id       BIGINT       NOT NULL                                        COMMENT '企业 ID',

    -- 注销信息
    revoke_date         DATETIME     DEFAULT NULL                                    COMMENT '吊销日期',
    revoke_reason       VARCHAR(500) DEFAULT NULL                                    COMMENT '吊销原因',
    cancel_date         DATETIME     DEFAULT NULL                                    COMMENT '注销日期',
    cancel_reason       VARCHAR(500) DEFAULT NULL                                    COMMENT '注销原因',

    -- BaseEntity
    create_by           VARCHAR(64)  DEFAULT NULL                                    COMMENT '创建人',
    create_time         DATETIME     DEFAULT NULL                                    COMMENT '创建时间',
    update_by           VARCHAR(64)  DEFAULT NULL                                    COMMENT '更新人',
    update_time         DATETIME     DEFAULT NULL                                    COMMENT '更新时间',
    deleted             INT          NOT NULL DEFAULT 0                               COMMENT '逻辑删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_unregister_enterprise (enterprise_id, deleted),
    KEY idx_unregister_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业注销信息 (park-enterprise V55)';

-- sys_enterprise_address: 地址信息 (省/市/区 拆分)
CREATE TABLE sys_enterprise_address (
    id                  BIGINT       NOT NULL                                        COMMENT '雪花 ID',
    tenant_id           BIGINT       DEFAULT NULL                                    COMMENT '租户 ID',
    enterprise_id       BIGINT       NOT NULL                                        COMMENT '企业 ID',

    -- 地址拆分
    base                VARCHAR(32)  DEFAULT NULL                                    COMMENT '省份简称 (e.g. 广东)',
    city                VARCHAR(64)  DEFAULT NULL                                    COMMENT '市',
    district            VARCHAR(64)  DEFAULT NULL                                    COMMENT '区',
    reg_location        VARCHAR(500) DEFAULT NULL                                    COMMENT '完整注册地址',

    -- BaseEntity
    create_by           VARCHAR(64)  DEFAULT NULL                                    COMMENT '创建人',
    create_time         DATETIME     DEFAULT NULL                                    COMMENT '创建时间',
    update_by           VARCHAR(64)  DEFAULT NULL                                    COMMENT '更新人',
    update_time         DATETIME     DEFAULT NULL                                    COMMENT '更新时间',
    deleted             INT          NOT NULL DEFAULT 0                               COMMENT '逻辑删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_address_enterprise (enterprise_id, deleted),
    KEY idx_address_tenant (tenant_id),
    KEY idx_address_base (base),
    KEY idx_address_city (city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业地址信息 (park-enterprise V55)';
