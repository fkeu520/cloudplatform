-- ================================================================
-- [MIGRATION NOTICE 2026-06-26] building 业务已迁至 park-space 模块
-- ================================================================
-- 本 V29 是历史迁移文件, 仅保留 sys_building 表的 DDL 与种子数据
-- 当前 building 业务由 park-space 模块承载:
--   - 后端: park-space/.../controller/BuildingController.java
--   - 网关: platform-gateway application.yml park-space 路由 (/building/**)
--   - 菜单: V36/V38 重组至 app_id=1 (system)
-- park-property 模块暂时弃用, 不再新增 building 相关业务
-- Flyway validate-on-migrate: false (所有模块), 本注释变更不影响 checksum 校验
-- ================================================================

-- ================================================================
-- V29: park-property 第一张业务表 (csyh 业务融合 W3.2)
--
-- 背景: W3.1 (commit 4c08183) 落地 park-space (Room), 现接入 park-property (Building).
--       复用 sys_app seed (add-business-apps.sql) 中 id=10 (物业管理).
--       与 park-space 一样, 暂不新建 Maven module, 业务代码挂到 platform-user.
--
-- 设计:
--   - sys_building 表 (楼宇), 包含 parkId/buildingNo/floors/area/manager 等字段
--   - 雪花 ID (BaseEntity 已用 IdType.ASSIGN_ID)
--   - 逻辑删除 (BaseEntity.deleted, MP @TableLogic)
--   - 多租户 (tenant_id, 来自 P0-1 多租户拦截器)
--
-- 路由: GET /building/page (v1 阶段 1 个端点, 后续按需补)
-- 菜单: 在 app 10 (物业管理) 下加 1 个根菜单"楼宇列表" -> /building/page
--       绑给 role 1 (SUPER_ADMIN)
-- ================================================================

-- 1) sys_building 表
CREATE TABLE IF NOT EXISTS `sys_building` (
    `id`            bigint        NOT NULL              COMMENT '主键 (雪花)',
    `park_id`       bigint        DEFAULT NULL          COMMENT '园区 ID',
    `building_no`   varchar(50)   NOT NULL              COMMENT '楼宇编号 (e.g. A/B/C)',
    `building_name` varchar(100)  DEFAULT NULL          COMMENT '楼宇名称 (e.g. A 座)',
    `floors`        int           DEFAULT 1             COMMENT '总楼层数',
    `total_area`    decimal(12,2) DEFAULT NULL          COMMENT '总建筑面积 (m²)',
    `build_year`    int           DEFAULT NULL          COMMENT '建成年份',
    `manager`       varchar(50)   DEFAULT NULL          COMMENT '楼宇负责人',
    `manager_phone` varchar(20)   DEFAULT NULL          COMMENT '负责人电话',
    `status`        tinyint       NOT NULL DEFAULT '1'  COMMENT '状态: 0=停用 1=正常',
    `remark`        varchar(500)  DEFAULT NULL          COMMENT '备注',
    `tenant_id`     bigint        DEFAULT NULL          COMMENT '租户 ID (P0-1 多租户)',
    `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     bigint        DEFAULT NULL          COMMENT '创建人 ID',
    `update_by`     bigint        DEFAULT NULL          COMMENT '更新人 ID',
    `deleted`       tinyint       NOT NULL DEFAULT '0'  COMMENT '逻辑删除 (0=未删 1=已删)',
    PRIMARY KEY (`id`),
    KEY `idx_park_id` (`park_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='园区楼宇表 (park-property 业务)';

-- 2) 物业管理菜单 (app id=10, park-property)
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `perms`, `icon`, `sort`, `status`, `app_id`, `type`)
VALUES
    (200, 0,  '物业管理',    'property',       'Layout',             NULL,                  'House',     10, 1, 10, 0),
    (201, 200, '楼宇列表',   '/building/page', 'building/Index',     'building:view',       'OfficeBuilding', 1, 1, 10, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path` = VALUES(`path`);

-- 3) 绑给 SUPER_ADMIN 角色 (id=1)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
    (1, 200),
    (1, 201);

-- 4) 种子数据: 2 个示例楼宇
INSERT IGNORE INTO `sys_building` (`id`, `park_id`, `building_no`, `building_name`, `floors`, `total_area`, `build_year`, `manager`, `manager_phone`, `status`, `tenant_id`, `create_by`)
VALUES
    (1900000000000001001, 1, 'A', 'A 座 写字楼',  12, 15000.00, 2020, '张经理', '13800001001', 1, 1, 1),
    (1900000000000001002, 1, 'B', 'B 座 综合楼',   8, 10000.00, 2018, '李经理', '13800001002', 1, 1, 1);

-- 5) 验证
SELECT 'sys_building_count' AS k, COUNT(*) AS v FROM sys_building WHERE deleted = 0;
SELECT 'park_property_menu' AS k, GROUP_CONCAT(CONCAT(id, '/', name)) AS v FROM sys_menu WHERE app_id = 10 AND deleted = 0;
