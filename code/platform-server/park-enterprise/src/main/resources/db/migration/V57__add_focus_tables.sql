-- =============================================================================
-- V57__add_focus_tables.sql
-- park-enterprise Phase 2 提前 - 关注体系
-- 来源: csyh pai-enterprise-csyh-2.x V2022032201 ddl
-- 3 张表: focus (标签主表) / focus_item (标签内容) / enterprise_focus (企业关联)
-- =============================================================================

-- sys_focus: 关注标签主表
CREATE TABLE sys_focus (
    id              BIGINT       NOT NULL                                            COMMENT '雪花 ID',
    tenant_id       BIGINT       DEFAULT NULL                                        COMMENT '租户 ID',
    park_id         BIGINT       DEFAULT NULL                                        COMMENT '园区 ID',

    name            VARCHAR(64)  NOT NULL                                            COMMENT '标签名称 (e.g. 高新技术/规模以上/科技型)',
    sorting         INT          NOT NULL DEFAULT 0                                   COMMENT '排序 (小的在前)',

    -- 自身状态
    status          INT          NOT NULL DEFAULT 1                                   COMMENT '启用状态 (1=启用 0=停用)',

    -- BaseEntity
    create_by       VARCHAR(64)  DEFAULT NULL                                        COMMENT '创建人',
    create_time     DATETIME     DEFAULT NULL                                        COMMENT '创建时间',
    update_by       VARCHAR(64)  DEFAULT NULL                                        COMMENT '更新人',
    update_time     DATETIME     DEFAULT NULL                                        COMMENT '更新时间',
    deleted         INT          NOT NULL DEFAULT 0                                   COMMENT '逻辑删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_focus_tenant_name (tenant_id, name, deleted),
    KEY idx_focus_tenant (tenant_id),
    KEY idx_focus_sorting (sorting)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注标签主表 (park-enterprise V57)';

-- sys_focus_item: 关注标签内容 (一个标签下多个内容选项)
CREATE TABLE sys_focus_item (
    id              BIGINT       NOT NULL                                            COMMENT '雪花 ID',
    tenant_id       BIGINT       DEFAULT NULL                                        COMMENT '租户 ID',
    park_id         BIGINT       DEFAULT NULL                                        COMMENT '园区 ID',

    focus_id        BIGINT       NOT NULL                                            COMMENT '关注标签 ID (sys_focus.id)',
    name            VARCHAR(64)  NOT NULL                                            COMMENT '内容名称',
    sorting         INT          NOT NULL DEFAULT 0                                   COMMENT '排序',

    -- 自身状态
    status          INT          NOT NULL DEFAULT 1                                   COMMENT '启用状态',

    -- BaseEntity
    create_by       VARCHAR(64)  DEFAULT NULL                                        COMMENT '创建人',
    create_time     DATETIME     DEFAULT NULL                                        COMMENT '创建时间',
    update_by       VARCHAR(64)  DEFAULT NULL                                        COMMENT '更新人',
    update_time     DATETIME     DEFAULT NULL                                        COMMENT '更新时间',
    deleted         INT          NOT NULL DEFAULT 0                                   COMMENT '逻辑删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_focus_item_focus_name (focus_id, name, deleted),
    KEY idx_focus_item_tenant (tenant_id),
    KEY idx_focus_item_focus (focus_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注标签内容 (park-enterprise V57)';

-- sys_enterprise_focus: 企业与关注标签的关联
CREATE TABLE sys_enterprise_focus (
    id                  BIGINT       NOT NULL                                        COMMENT '雪花 ID',
    tenant_id           BIGINT       DEFAULT NULL                                    COMMENT '租户 ID',
    park_id             BIGINT       DEFAULT NULL                                    COMMENT '园区 ID',

    enterprise_id       BIGINT       NOT NULL                                        COMMENT '企业 ID (sys_enterprise.id)',
    focus_id            BIGINT       NOT NULL                                        COMMENT '关注标签 ID',
    focus_name          VARCHAR(64)  DEFAULT NULL                                    COMMENT '冗余: 标签名称',
    focus_items         VARCHAR(500) DEFAULT NULL                                    COMMENT '关注内容 ID 列表 (逗号分隔)',
    focus_item_names    VARCHAR(1000) DEFAULT NULL                                   COMMENT '冗余: 内容名称列表 (逗号分隔)',

    -- 自身状态
    status              INT          NOT NULL DEFAULT 1                               COMMENT '启用状态',

    -- BaseEntity
    create_by           VARCHAR(64)  DEFAULT NULL                                    COMMENT '创建人',
    create_time         DATETIME     DEFAULT NULL                                    COMMENT '创建时间',
    update_by           VARCHAR(64)  DEFAULT NULL                                    COMMENT '更新人',
    update_time         DATETIME     DEFAULT NULL                                    COMMENT '更新时间',
    deleted             INT          NOT NULL DEFAULT 0                               COMMENT '逻辑删除',

    PRIMARY KEY (id),
    UNIQUE KEY uk_ent_focus_ent_focus (enterprise_id, focus_id, deleted),
    KEY idx_ent_focus_tenant (tenant_id),
    KEY idx_ent_focus_ent (enterprise_id),
    KEY idx_ent_focus_focus (focus_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业关注标签关联 (park-enterprise V57)';
