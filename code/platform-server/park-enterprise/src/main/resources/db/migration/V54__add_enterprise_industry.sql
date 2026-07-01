-- =============================================================================
-- V54__add_enterprise_industry.sql
-- park-enterprise Phase 1 扩展 (csyh 业务融合)
-- 来源: csyh pai-enterprise-csyh-2.x V2021111001__init.sql
-- 平台改造:
--   - String id (csyh) -> BIGINT 雪花 ID
--   - 加 tenant_id (P0-1 拦截器)
--   - 加 BaseEntity 审计字段
--   - 加 UNIQUE KEY (tenant_id, code)
-- =============================================================================

CREATE TABLE sys_enterprise_industry (
    id              BIGINT       NOT NULL                                            COMMENT '雪花 ID',
    tenant_id       BIGINT       DEFAULT NULL                                        COMMENT '租户 ID',

    -- 行业代码 (GB/T 4754-2017 门类字母 A-T)
    code            VARCHAR(16)  NOT NULL                                            COMMENT '行业代码 (门类字母+大中小类数字)',
    category        VARCHAR(64)  DEFAULT NULL                                        COMMENT '门类 (e.g. 信息传输/软件和信息技术服务业)',
    category_big    VARCHAR(64)  DEFAULT NULL                                        COMMENT '大类',
    category_middle VARCHAR(64)  DEFAULT NULL                                        COMMENT '中类',
    category_small  VARCHAR(64)  DEFAULT NULL                                        COMMENT '小类',

    -- 启用状态
    status          INT          NOT NULL DEFAULT 1                                   COMMENT '启用状态 (1=启用 0=停用)',

    -- BaseEntity 审计字段
    create_by       VARCHAR(64)  DEFAULT NULL                                        COMMENT '创建人',
    create_time     DATETIME     DEFAULT NULL                                        COMMENT '创建时间',
    update_by       VARCHAR(64)  DEFAULT NULL                                        COMMENT '更新人',
    update_time     DATETIME     DEFAULT NULL                                        COMMENT '更新时间',
    deleted         INT          NOT NULL DEFAULT 0                                   COMMENT '逻辑删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_industry_tenant_code (tenant_id, code, deleted),
    KEY idx_industry_category (category),
    KEY idx_industry_status (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业行业类型 (park-enterprise V54)';
