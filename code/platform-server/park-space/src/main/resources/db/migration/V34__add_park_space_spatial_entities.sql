-- ================================================================
-- V34: park-space 空间实体 (csyh 业务融合 W3.5)
--
-- 背景: W3.5 阶段 3 个空间实体迁移: Space, SpaceCategory, Massif.
--       Space 关联 sys_area (W3.3), Massif 关联 sys_land_nature/sys_plan_use (W3.3).
--
-- 设计:
--   - 全部使用雪花 ID (BaseEntity 已用 IdType.ASSIGN_ID)
--   - 逻辑删除 (BaseEntity.deleted, MP @TableLogic)
--   - 多租户 (tenant_id, 来自 P0-1 多租户拦截器)
--   - 状态字段: 0=停用 1=启用
--   - 表名前缀 sys_ (与 platform 命名规范一致)
--
-- 简化: 跳过 csyh 反范式冗余字段 (categoryName/pathNames/landNatureName/planUseName),
--       关联查询通过 ID 联表, 避免数据不一致.
-- ================================================================

-- 1) sys_space_category (空间类别)
CREATE TABLE IF NOT EXISTS `sys_space_category` (
    `id`             bigint       NOT NULL              COMMENT '主键 (雪花)',
    `type_name`      varchar(255) NOT NULL              COMMENT '类型名称 (e.g. 研发中心)',
    `type_describe`  varchar(500) DEFAULT NULL          COMMENT '类型描述',
    `status`         tinyint      NOT NULL DEFAULT '1'  COMMENT '状态 (0=停用 1=启用)',
    `park_id`        bigint       DEFAULT NULL          COMMENT '园区ID',
    `tenant_id`      bigint       DEFAULT NULL          COMMENT '租户ID (P0-1 多租户)',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      bigint       DEFAULT NULL          COMMENT '创建人',
    `update_by`      bigint       DEFAULT NULL          COMMENT '更新人',
    `deleted`        tinyint      NOT NULL DEFAULT '0'  COMMENT '逻辑删除 (0=未删 1=已删)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_park_type_name` (`park_id`, `type_name`, `deleted`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='空间类别表';

-- 2) sys_space (空间)
CREATE TABLE IF NOT EXISTS `sys_space` (
    `id`              bigint       NOT NULL              COMMENT '主键 (雪花)',
    `space_name`      varchar(100) NOT NULL              COMMENT '空间名称',
    `space_describe`  varchar(500) DEFAULT NULL          COMMENT '位置描述',
    `park_id`         bigint       DEFAULT NULL          COMMENT '所属园区ID',
    `area_id`         bigint       DEFAULT NULL          COMMENT '所属区域ID (关联 sys_area.id)',
    `category_id`     bigint       DEFAULT NULL          COMMENT '空间类别ID (关联 sys_space_category.id)',
    `status`          tinyint      NOT NULL DEFAULT '1'  COMMENT '状态 (0=停用 1=启用)',
    `tenant_id`       bigint       DEFAULT NULL          COMMENT '租户ID (P0-1 多租户)',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       bigint       DEFAULT NULL          COMMENT '创建人',
    `update_by`       bigint       DEFAULT NULL          COMMENT '更新人',
    `deleted`         tinyint      NOT NULL DEFAULT '0'  COMMENT '逻辑删除 (0=未删 1=已删)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_park_space_name` (`park_id`, `space_name`, `deleted`),
    KEY `idx_park_id` (`park_id`),
    KEY `idx_area_id` (`area_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='空间信息表';

-- 3) sys_massif (地块)
CREATE TABLE IF NOT EXISTS `sys_massif` (
    `id`              bigint         NOT NULL              COMMENT '主键 (雪花)',
    `massif_code`     varchar(32)    NOT NULL              COMMENT '地块编号',
    `massif_name`     varchar(256)   DEFAULT NULL          COMMENT '地块名称',
    `massif_area`     decimal(16,2)  DEFAULT NULL          COMMENT '地块面积 (m²)',
    `use_year`        int            DEFAULT NULL          COMMENT '使用年限',
    `land_nature_id`  bigint         DEFAULT NULL          COMMENT '土地性质ID (关联 sys_land_nature.id)',
    `plan_use_id`     bigint         DEFAULT NULL          COMMENT '规划用途ID (关联 sys_plan_use.id)',
    `asset_type`      varchar(64)    DEFAULT '国土资源'   COMMENT '资产类型',
    `massif_desc`     varchar(256)   DEFAULT NULL          COMMENT '地块描述',
    `address`         varchar(255)   DEFAULT NULL          COMMENT '地块地址',
    `status`          tinyint        NOT NULL DEFAULT '1'  COMMENT '状态 (0=已卖 1=可用)',
    `park_id`         bigint         DEFAULT NULL          COMMENT '园区ID',
    `tenant_id`       bigint         DEFAULT NULL          COMMENT '租户ID (P0-1 多租户)',
    `create_time`     datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       bigint         DEFAULT NULL          COMMENT '创建人',
    `update_by`       bigint         DEFAULT NULL          COMMENT '更新人',
    `deleted`         tinyint        NOT NULL DEFAULT '0'  COMMENT '逻辑删除 (0=未删 1=已删)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_park_massif_code` (`park_id`, `massif_code`, `deleted`),
    KEY `idx_park_id` (`park_id`),
    KEY `idx_land_nature_id` (`land_nature_id`),
    KEY `idx_plan_use_id` (`plan_use_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地块信息表';

-- 4) 空间中心菜单 (app_id=6 park-space)
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `perms`, `icon`, `sort`, `status`, `app_id`, `type`)
VALUES
    (110, 100, '空间类别',  '/space-category/page', 'space-category/Index', 'space-category:view', 'Grid',     10, 1, 6, 1),
    (111, 100, '空间管理',  '/space/page',         'space/Index',         'space:view',         'Compass',  11, 1, 6, 1),
    (112, 100, '地块管理',  '/massif/page',        'massif/Index',        'massif:view',        'Map',      12, 1, 6, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path` = VALUES(`path`);

-- 5) 绑给 SUPER_ADMIN 角色 (id=1)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
    (1, 110), (1, 111), (1, 112);

-- 6) 种子数据 (供端到端验证)
-- 注意: 引用 sys_area (1900000000010000001=A 区), sys_land_nature (1900000000050000001=工业用地),
--       sys_plan_use (1900000000040000001=办公)
INSERT IGNORE INTO `sys_space_category` (`id`, `park_id`, `type_name`, `type_describe`, `status`, `tenant_id`, `create_by`) VALUES
    (1900000000100000001, 1, '研发中心', '研发空间', 1, 1, 1),
    (1900000000100000002, 1, '营销中心', '营销空间', 1, 1, 1);

INSERT IGNORE INTO `sys_space` (`id`, `park_id`, `area_id`, `category_id`, `space_name`, `space_describe`, `status`, `tenant_id`, `create_by`) VALUES
    (1900000000110000001, 1, 1900000000010000001, 1900000000100000001, '研发一室', 'A 区 1 楼', 1, 1, 1),
    (1900000000110000002, 1, 1900000000010000001, 1900000000100000002, '营销一室', 'A 区 2 楼', 1, 1, 1);

INSERT IGNORE INTO `sys_massif` (`id`, `park_id`, `massif_code`, `massif_name`, `massif_area`, `use_year`, `land_nature_id`, `plan_use_id`, `asset_type`, `massif_desc`, `address`, `status`, `tenant_id`, `create_by`) VALUES
    (1900000000120000001, 1, 'M001', '地块一', 5000.00, 40, 1900000000050000001, 1900000000040000001, '国土资源', '主地块', '深圳市南山区', 1, 1, 1);

-- 7) 验证
SELECT 'sys_space_category_count' AS k, COUNT(*) AS v FROM sys_space_category WHERE deleted = 0;
SELECT 'sys_space_count'          AS k, COUNT(*) AS v FROM sys_space          WHERE deleted = 0;
SELECT 'sys_massif_count'         AS k, COUNT(*) AS v FROM sys_massif         WHERE deleted = 0;
SELECT 'park_space_w35_menu'      AS k, GROUP_CONCAT(CONCAT(id, '/', name)) AS v FROM sys_menu WHERE app_id = 6 AND deleted = 0;