-- ================================================================
-- V32: park-space 简单 CRUD 实体 (csyh 业务融合 W3.3)
--
-- 背景: W3.3 阶段 5 个简单 CRUD 实体迁移: Area, Floor, Kit, PlanUse, LandNature
--       每个都跟 sys_park / sys_building / sys_room 关联, 是空间中心第二批业务数据.
--
-- 设计:
--   - 全部使用雪花 ID (BaseEntity 已用 IdType.ASSIGN_ID)
--   - 逻辑删除 (BaseEntity.deleted, MP @TableLogic)
--   - 多租户 (tenant_id, 来自 P0-1 多租户拦截器)
--   - 状态字段: 0=停用 1=启用 (与 sys_park 一致)
--   - 表名前缀 sys_ (与 platform 命名规范一致)
--
-- 路由: GET /{area|floor|kit|plan-use|land-nature}/page (v1 阶段 5 个端点)
-- 菜单: 在 app 6 (空间中心) 下加 5 个子菜单, 绑给 role 1 (SUPER_ADMIN)
-- ================================================================

-- 1) sys_area (区域)
CREATE TABLE IF NOT EXISTS `sys_area` (
    `id`             bigint        NOT NULL              COMMENT '主键 (雪花)',
    `area_name`      varchar(64)   NOT NULL              COMMENT '区域名称',
    `area_covered`   decimal(16,2) DEFAULT NULL          COMMENT '占地面积 (m²)',
    `built_area`     decimal(16,2) DEFAULT NULL          COMMENT '建筑面积 (m²)',
    `function_area`  varchar(64)   DEFAULT NULL          COMMENT '功能区域描述',
    `building_amount` int          NOT NULL DEFAULT '0'  COMMENT '楼栋数',
    `room_amount`    int           NOT NULL DEFAULT '0'  COMMENT '房间数',
    `is_virtual`     tinyint       NOT NULL DEFAULT '0'  COMMENT '是否虚拟区域 (0=否 1=是)',
    `sorting`        int           NOT NULL DEFAULT '0'  COMMENT '排序',
    `status`         tinyint       NOT NULL DEFAULT '1'  COMMENT '状态 (0=停用 1=启用)',
    `park_id`        bigint        DEFAULT NULL          COMMENT '园区ID',
    `tenant_id`      bigint        DEFAULT NULL          COMMENT '租户ID (P0-1 多租户)',
    `create_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      bigint        DEFAULT NULL          COMMENT '创建人',
    `update_by`      bigint        DEFAULT NULL          COMMENT '更新人',
    `deleted`        tinyint       NOT NULL DEFAULT '0'  COMMENT '逻辑删除 (0=未删 1=已删)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_park_area_name` (`park_id`, `area_name`, `deleted`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域表 (park-space 业务)';

-- 2) sys_floor (楼层)
CREATE TABLE IF NOT EXISTS `sys_floor` (
    `id`             bigint         NOT NULL              COMMENT '主键 (雪花)',
    `floor_name`     varchar(64)    NOT NULL              COMMENT '楼层名称 (e.g. 1 楼)',
    `serial_code`    int            DEFAULT NULL          COMMENT '楼层序号',
    `floor_category` int            NOT NULL DEFAULT '0'  COMMENT '楼层类型 (0=地上 1=地下 2=夹层)',
    `coefficient`    decimal(16,2)  NOT NULL DEFAULT '1.00' COMMENT '楼层系数',
    `sorting`        int            NOT NULL DEFAULT '0'  COMMENT '排序',
    `status`         tinyint        NOT NULL DEFAULT '1'  COMMENT '状态 (0=停用 1=启用)',
    `park_id`        bigint         DEFAULT NULL          COMMENT '园区ID',
    `building_id`    bigint         DEFAULT NULL          COMMENT '楼栋ID',
    `tenant_id`      bigint         DEFAULT NULL          COMMENT '租户ID (P0-1 多租户)',
    `create_time`    datetime       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime       DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      bigint         DEFAULT NULL          COMMENT '创建人',
    `update_by`      bigint         DEFAULT NULL          COMMENT '更新人',
    `deleted`        tinyint        NOT NULL DEFAULT '0'  COMMENT '逻辑删除 (0=未删 1=已删)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_building_floor_name` (`building_id`, `floor_name`, `deleted`),
    KEY `idx_park_id` (`park_id`),
    KEY `idx_building_id` (`building_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼层表 (park-space 业务)';

-- 3) sys_kit (装修配套)
CREATE TABLE IF NOT EXISTS `sys_kit` (
    `id`          bigint       NOT NULL              COMMENT '主键 (雪花)',
    `kit_name`    varchar(64)  NOT NULL              COMMENT '配套名称',
    `amount`      int          NOT NULL DEFAULT '0'  COMMENT '数量',
    `status`      tinyint      NOT NULL DEFAULT '1'  COMMENT '状态 (0=停用 1=启用)',
    `park_id`     bigint       DEFAULT NULL          COMMENT '园区ID',
    `tenant_id`   bigint       DEFAULT NULL          COMMENT '租户ID (P0-1 多租户)',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint       DEFAULT NULL          COMMENT '创建人',
    `update_by`   bigint       DEFAULT NULL          COMMENT '更新人',
    `deleted`     tinyint      NOT NULL DEFAULT '0'  COMMENT '逻辑删除 (0=未删 1=已删)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_park_kit_name` (`park_id`, `kit_name`, `deleted`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='装修配套表 (park-space 业务)';

-- 4) sys_plan_use (规划用途)
CREATE TABLE IF NOT EXISTS `sys_plan_use` (
    `id`             bigint        NOT NULL              COMMENT '主键 (雪花)',
    `plan_use_code`  varchar(32)   DEFAULT NULL          COMMENT '规划用途编号 (e.g. OFFICE)',
    `plan_use_name`  varchar(256)  NOT NULL              COMMENT '规划用途名称',
    `color`          varchar(32)   DEFAULT NULL          COMMENT '标的色 (hex)',
    `status`         tinyint       NOT NULL DEFAULT '1'  COMMENT '状态 (0=停用 1=启用)',
    `park_id`        bigint        DEFAULT NULL          COMMENT '园区ID',
    `tenant_id`      bigint        DEFAULT NULL          COMMENT '租户ID (P0-1 多租户)',
    `create_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      bigint        DEFAULT NULL          COMMENT '创建人',
    `update_by`      bigint        DEFAULT NULL          COMMENT '更新人',
    `deleted`        tinyint       NOT NULL DEFAULT '0'  COMMENT '逻辑删除 (0=未删 1=已删)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_park_plan_use_name` (`park_id`, `plan_use_name`, `deleted`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规划用途表 (park-space 业务)';

-- 5) sys_land_nature (土地性质)
CREATE TABLE IF NOT EXISTS `sys_land_nature` (
    `id`                bigint        NOT NULL              COMMENT '主键 (雪花)',
    `land_nature_name`  varchar(256)  NOT NULL              COMMENT '土地性质名称',
    `land_nature_code`  varchar(32)   DEFAULT NULL          COMMENT '土地性质编号',
    `color`             varchar(32)   DEFAULT NULL          COMMENT '标的色 (hex)',
    `status`            tinyint       NOT NULL DEFAULT '1'  COMMENT '状态 (0=停用 1=启用)',
    `park_id`           bigint        DEFAULT NULL          COMMENT '园区ID',
    `tenant_id`         bigint        DEFAULT NULL          COMMENT '租户ID (P0-1 多租户)',
    `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         bigint        DEFAULT NULL          COMMENT '创建人',
    `update_by`         bigint        DEFAULT NULL          COMMENT '更新人',
    `deleted`           tinyint       NOT NULL DEFAULT '0'  COMMENT '逻辑删除 (0=未删 1=已删)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_park_land_nature_name` (`park_id`, `land_nature_name`, `deleted`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='土地性质表 (park-space 业务)';

-- 6) 空间中心菜单 (app_id=6 park-space)
-- 父菜单 sys_menu.id=100 (空间中心) 已存在 (V28 seed)
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `perms`, `icon`, `sort`, `status`, `app_id`, `type`)
VALUES
    (102, 100, '区域管理',     '/area/page',          'area/Index',          'area:view',          'Location',     2, 1, 6, 1),
    (103, 100, '楼层管理',     '/floor/page',         'floor/Index',         'floor:view',         'OfficeBuilding', 3, 1, 6, 1),
    (104, 100, '配套管理',     '/kit/page',           'kit/Index',           'kit:view',           'Box',           4, 1, 6, 1),
    (105, 100, '规划用途',     '/plan-use/page',      'plan-use/Index',      'plan-use:view',      'Aim',           5, 1, 6, 1),
    (106, 100, '土地性质',     '/land-nature/page',   'land-nature/Index',   'land-nature:view',   'Mountains',     6, 1, 6, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path` = VALUES(`path`);

-- 7) 绑给 SUPER_ADMIN 角色 (id=1), 让所有租户管理员可见
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
    (1, 102), (1, 103), (1, 104), (1, 105), (1, 106);

-- 8) 种子数据 (供端到端验证)
INSERT IGNORE INTO `sys_area` (`id`, `park_id`, `area_name`, `area_covered`, `built_area`, `function_area`, `building_amount`, `room_amount`, `is_virtual`, `sorting`, `status`, `tenant_id`, `create_by`) VALUES
    (1900000000010000001, 1, 'A 区', 10000.00, 8000.00, '研发区',  5, 50, 0, 1, 1, 1, 1),
    (1900000000010000002, 1, 'B 区',  5000.00, 4000.00, '商业区',  2, 20, 0, 2, 1, 1, 1);

INSERT IGNORE INTO `sys_floor` (`id`, `park_id`, `building_id`, `floor_name`, `serial_code`, `floor_category`, `coefficient`, `sorting`, `status`, `tenant_id`, `create_by`) VALUES
    (1900000000020000001, 1, 1, '1 楼', 1, 0, 1.00, 1, 1, 1, 1),
    (1900000000020000002, 1, 1, '2 楼', 2, 0, 1.00, 2, 1, 1, 1),
    (1900000000020000003, 1, 1, '3 楼', 3, 0, 1.00, 3, 1, 1, 1);

INSERT IGNORE INTO `sys_kit` (`id`, `park_id`, `kit_name`, `amount`, `status`, `tenant_id`, `create_by`) VALUES
    (1900000000030000001, 1, '标准装修', 1, 1, 1, 1),
    (1900000000030000002, 1, '精装修',   1, 1, 1, 1);

INSERT IGNORE INTO `sys_plan_use` (`id`, `park_id`, `plan_use_code`, `plan_use_name`, `color`, `status`, `tenant_id`, `create_by`) VALUES
    (1900000000040000001, 1, 'OFFICE',    '办公',     '#409EFF', 1, 1, 1),
    (1900000000040000002, 1, 'SHOP',      '商业',     '#67C23A', 1, 1, 1),
    (1900000000040000003, 1, 'WAREHOUSE', '仓储',     '#E6A23C', 1, 1, 1);

INSERT IGNORE INTO `sys_land_nature` (`id`, `park_id`, `land_nature_code`, `land_nature_name`, `color`, `status`, `tenant_id`, `create_by`) VALUES
    (1900000000050000001, 1, 'INDUSTRIAL', '工业用地', '#909399', 1, 1, 1),
    (1900000000050000002, 1, 'COMMERCIAL', '商业用地', '#F56C6C', 1, 1, 1);

-- 9) 验证
SELECT 'sys_area_count'         AS k, COUNT(*) AS v FROM sys_area         WHERE deleted = 0;
SELECT 'sys_floor_count'        AS k, COUNT(*) AS v FROM sys_floor        WHERE deleted = 0;
SELECT 'sys_kit_count'          AS k, COUNT(*) AS v FROM sys_kit          WHERE deleted = 0;
SELECT 'sys_plan_use_count'     AS k, COUNT(*) AS v FROM sys_plan_use     WHERE deleted = 0;
SELECT 'sys_land_nature_count'  AS k, COUNT(*) AS v FROM sys_land_nature  WHERE deleted = 0;
SELECT 'park_space_w33_menu'    AS k, GROUP_CONCAT(CONCAT(id, '/', name)) AS v FROM sys_menu WHERE app_id = 6 AND deleted = 0;