-- =============================================================================
-- V56__add_rating_table.sql
-- park-enterprise Phase 1 扩展
-- 来源: csyh pai-enterprise-csyh-2.x V2021111001 rating 表 + V2022061001 完整字段
-- 企业评级规则 (1=优 2=良 3=中 4=差)
-- 触发: RatingInitAspect AOP 在企业更新/支付时自动重算
-- =============================================================================

CREATE TABLE sys_rating (
    id                  BIGINT         NOT NULL                                      COMMENT '雪花 ID',
    tenant_id           BIGINT         DEFAULT NULL                                  COMMENT '租户 ID',
    park_id             BIGINT         DEFAULT NULL                                  COMMENT '园区 ID (多园区时区分)',

    -- 评级字段
    level               INT            NOT NULL                                      COMMENT '评级 1=优 2=良 3=中 4=差',
    overdue_min         INT            NOT NULL DEFAULT 0                            COMMENT '逾期次数起',
    overdue_max         INT            NOT NULL DEFAULT 0                            COMMENT '逾期次数止',
    debts_min           DECIMAL(16, 2) NOT NULL DEFAULT 0.00                         COMMENT '欠费金额起 (元)',
    debts_max           DECIMAL(16, 2) NOT NULL DEFAULT 0.00                         COMMENT '欠费金额止 (元)',
    date_num            INT            DEFAULT NULL                                  COMMENT '日期数',
    date_unit           VARCHAR(16)    DEFAULT NULL                                  COMMENT '日期单位 (day/month/year)',

    -- 自身状态
    status              INT            NOT NULL DEFAULT 1                             COMMENT '启用状态 (1=启用 0=停用)',

    -- BaseEntity
    create_by           VARCHAR(64)    DEFAULT NULL                                  COMMENT '创建人',
    create_time         DATETIME       DEFAULT NULL                                  COMMENT '创建时间',
    update_by           VARCHAR(64)    DEFAULT NULL                                  COMMENT '更新人',
    update_time         DATETIME       DEFAULT NULL                                  COMMENT '更新时间',
    deleted             INT            NOT NULL DEFAULT 0                             COMMENT '逻辑删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_rating_tenant_level (tenant_id, level, deleted),
    KEY idx_rating_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业评分规则 (park-enterprise V56)';

-- 默认 4 等级规则 seed (Phase 1 内置, 实际生产按园区自定义)
INSERT INTO sys_rating (id, tenant_id, level, overdue_min, overdue_max, debts_min, debts_max, date_num, date_unit, status) VALUES
(1900000000000000101, NULL, 1, 0, 0,   0.00,    0.00,  30, 'day', 1),    -- 优: 无逾期无欠费
(1900000000000000102, NULL, 2, 1, 2,   0.00,  5000.00, 30, 'day', 1),    -- 良: 1-2 次逾期 或 ≤5000 欠费
(1900000000000000103, NULL, 3, 3, 5, 5000.01, 30000.00, 30, 'day', 1),   -- 中: 3-5 次逾期 或 5K-30K 欠费
(1900000000000000104, NULL, 4, 6, 999, 30000.01, 999999999.99, 30, 'day', 1); -- 差: ≥6 次逾期 或 ≥30K 欠费
