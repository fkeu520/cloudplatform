-- =============================================================================
-- V63__add_enterprise_cloud_data.sql
-- park-enterprise Phase 3 (csyh cloud 模块迁移, 手动维护版)
--
-- 设计决策:
--   - 统一表 sys_enterprise_cloud_data + category 分类, 替代 csyh 74 个 VO (Q1)
--   - 独立参考表: sys_enterprise_reg_type (注册类型), sys_enterprise_national_economy (国民经济)
--   - 独立概览表: sys_enterprise_overview_data (结构较稳定)
--   - 手动维护 (用户指示: "做成手动维护即可")
-- =============================================================================

-- =============================================================================
-- sys_enterprise_cloud_data: 云企库数据统一表 (人工维护)
-- =============================================================================
CREATE TABLE sys_enterprise_cloud_data (
    id                       BIGINT       NOT NULL               COMMENT '雪花 ID',
    tenant_id                BIGINT       DEFAULT NULL           COMMENT '租户 ID',
    enterprise_id            BIGINT       DEFAULT NULL           COMMENT '关联企业 ID',
    enterprise_name          VARCHAR(255) DEFAULT NULL           COMMENT '企业名称 (冗余)',
    category                 VARCHAR(50)  NOT NULL               COMMENT '数据分类',

    data_content             TEXT         DEFAULT NULL           COMMENT '业务数据 (JSON 字符串)',
    data_year                VARCHAR(10)  DEFAULT NULL           COMMENT '数据年份',
    data_date                DATE         DEFAULT NULL           COMMENT '数据日期',

    sort_order               INT          DEFAULT 0              COMMENT '排序号',
    status                   TINYINT      DEFAULT 1              COMMENT '状态 (0=禁用 1=启用)',
    remark                   VARCHAR(500) DEFAULT NULL           COMMENT '备注',

    create_by                VARCHAR(64)  DEFAULT NULL           COMMENT '创建人',
    create_time              DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by                VARCHAR(64)  DEFAULT NULL           COMMENT '更新人',
    update_time              DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    deleted                  TINYINT      DEFAULT 0,

    PRIMARY KEY (id),
    INDEX idx_tenant (tenant_id),
    INDEX idx_enterprise (enterprise_id),
    INDEX idx_category (category),
    INDEX idx_category_year (category, data_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='云企库数据统一表 (人工维护, Phase 3)';


-- =============================================================================
-- sys_enterprise_reg_type: 企业注册类型参考表
-- =============================================================================
CREATE TABLE sys_enterprise_reg_type (
    id                       BIGINT       NOT NULL               COMMENT '雪花 ID',
    tenant_id                BIGINT       DEFAULT NULL,

    code                     VARCHAR(64)  NOT NULL               COMMENT '类型编码',
    name                     VARCHAR(255) NOT NULL               COMMENT '类型名称',
    parent_code              VARCHAR(64)  DEFAULT NULL           COMMENT '父级编码',
    sort_order               INT          DEFAULT 0,
    status                   TINYINT      DEFAULT 1,
    remark                   VARCHAR(500) DEFAULT NULL,

    create_by                VARCHAR(64)  DEFAULT NULL,
    create_time              DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by                VARCHAR(64)  DEFAULT NULL,
    update_time              DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    deleted                  TINYINT      DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    INDEX idx_parent (parent_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业注册类型参考表';


-- =============================================================================
-- sys_enterprise_national_economy: 国民经济行业分类参考表
-- =============================================================================
CREATE TABLE sys_enterprise_national_economy (
    id                       BIGINT       NOT NULL               COMMENT '雪花 ID',
    tenant_id                BIGINT       DEFAULT NULL,

    code                     VARCHAR(64)  NOT NULL               COMMENT '分类编码',
    name                     VARCHAR(255) NOT NULL               COMMENT '分类名称',
    parent_code              VARCHAR(64)  DEFAULT NULL           COMMENT '父级编码',
    level                    TINYINT      DEFAULT NULL           COMMENT '层级 (1=门类 2=大类 3=中类 4=小类)',
    sort_order               INT          DEFAULT 0,
    status                   TINYINT      DEFAULT 1,
    remark                   VARCHAR(500) DEFAULT NULL,

    create_by                VARCHAR(64)  DEFAULT NULL,
    create_time              DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by                VARCHAR(64)  DEFAULT NULL,
    update_time              DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    deleted                  TINYINT      DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    INDEX idx_parent (parent_code),
    INDEX idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国民经济行业分类参考表';


-- =============================================================================
-- sys_enterprise_overview_data: 企业概览数据
-- =============================================================================
CREATE TABLE sys_enterprise_overview_data (
    id                       BIGINT       NOT NULL               COMMENT '雪花 ID',
    tenant_id                BIGINT       DEFAULT NULL,
    enterprise_id            BIGINT       DEFAULT NULL           COMMENT '关联企业 ID',
    enterprise_name          VARCHAR(255) DEFAULT NULL           COMMENT '企业名称',

    reg_capital              VARCHAR(64)  DEFAULT NULL           COMMENT '注册资本',
    total_assets             VARCHAR(64)  DEFAULT NULL           COMMENT '总资产',
    annual_revenue           VARCHAR(64)  DEFAULT NULL           COMMENT '年营收',
    employee_count           INT          DEFAULT NULL           COMMENT '员工数',
    patent_count             INT          DEFAULT NULL           COMMENT '专利数',
    trademark_count          INT          DEFAULT NULL           COMMENT '商标数',
    copyright_count          INT          DEFAULT NULL           COMMENT '著作权数',
    risk_count               INT          DEFAULT NULL           COMMENT '风险数',
    bid_count                INT          DEFAULT NULL           COMMENT '招投标数',

    equity_structure_json    TEXT         DEFAULT NULL           COMMENT '股权结构 (JSON)',
    overview_json            TEXT         DEFAULT NULL           COMMENT '其他概览数据 (JSON)',

    sort_order               INT          DEFAULT 0,
    status                   TINYINT      DEFAULT 1,
    remark                   VARCHAR(500) DEFAULT NULL,

    create_by                VARCHAR(64)  DEFAULT NULL,
    create_time              DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_by                VARCHAR(64)  DEFAULT NULL,
    update_time              DATETIME     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    deleted                  TINYINT      DEFAULT 0,

    PRIMARY KEY (id),
    INDEX idx_enterprise (enterprise_id),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业概览数据 (Overview)';
