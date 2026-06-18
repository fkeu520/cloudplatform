-- ================================================================
-- V28: park-space 第一张业务表 (csyh 业务融合 W3)
--
-- 背景: 5 个业务 app (空间/招商/合同/财务/物业) 是 csyh 融合入口.
--       V11 seed 了 app 6 (空间中心 park-space) 但无业务表, W4 业务模块上线时再填充.
--       本 V28 提前落地 sys_room 表, 作为空间中心第一批业务数据.
--
-- 设计:
--   - sys_room 表 (园区房屋/工位), 字段精简, 后续按需扩展
--   - 雪花 ID (BaseEntity 已用 IdType.ASSIGN_ID)
--   - 逻辑删除 (BaseEntity.deleted, MP @TableLogic)
--   - 多租户 (tenant_id, 来自 P0-1 多租户拦截器)
--
-- 路由: GET /room/page (v1 阶段 1 个端点, 后续按需补)
-- 菜单: 在 app 6 (空间中心) 下加 1 个根菜单"房源列表" -> /room/page
--       同时绑给 role 1 (SUPER_ADMIN) 让所有管理员可见
-- ================================================================

-- 1) sys_room 表
CREATE TABLE IF NOT EXISTS `sys_room` (
    `id`          bigint       NOT NULL              COMMENT '主键 (雪花)',
    `park_id`     bigint       DEFAULT NULL          COMMENT '园区 ID (csyh 强依赖)',
    `building_id` bigint       DEFAULT NULL          COMMENT '楼宇 ID',
    `floor`       int          DEFAULT NULL          COMMENT '楼层',
    `room_no`     varchar(50)  NOT NULL              COMMENT '房号 (e.g. A-101)',
    `room_type`   varchar(20)  DEFAULT 'OFFICE'      COMMENT '类型: OFFICE/MEETING/STORAGE/PARKING',
    `area`        decimal(10,2) DEFAULT NULL          COMMENT '面积 (m²)',
    `monthly_rent` decimal(10,2) DEFAULT NULL         COMMENT '月租金 (元)',
    `status`      tinyint      NOT NULL DEFAULT '1'  COMMENT '状态: 0=空置 1=已租 2=装修中 3=停用',
    `remark`      varchar(500) DEFAULT NULL          COMMENT '备注',
    `tenant_id`   bigint       DEFAULT NULL          COMMENT '租户 ID (P0-1 多租户)',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   bigint       DEFAULT NULL          COMMENT '创建人 ID',
    `update_by`   bigint       DEFAULT NULL          COMMENT '更新人 ID',
    `deleted`     tinyint      NOT NULL DEFAULT '0'  COMMENT '逻辑删除 (0=未删 1=已删)',
    PRIMARY KEY (`id`),
    KEY `idx_park_id` (`park_id`),
    KEY `idx_building_id` (`building_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='园区房屋表 (park-space 业务)';

-- 2) 空间中心菜单 (app id=6, park-space)
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `perms`, `icon`, `sort`, `status`, `app_id`, `type`)
VALUES
    (100, 0,  '空间中心',   'space',          'Layout',            NULL,                'Building', 10, 1, 6, 0),
    (101, 100, '房源列表',  '/room/page',     'room/Index',        'room:view',         'Home',     1,  1, 6, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path` = VALUES(`path`);

-- 3) 绑给 SUPER_ADMIN 角色 (id=1), 让所有租户管理员可见
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
    (1, 100),
    (1, 101);

-- 4) 种子数据: 3 个示例房源 (方便端到端验证)
INSERT IGNORE INTO `sys_room` (`id`, `park_id`, `building_id`, `floor`, `room_no`, `room_type`, `area`, `monthly_rent`, `status`, `tenant_id`, `create_by`)
VALUES
    (1900000000000000001, 1, 1, 1, 'A-101', 'OFFICE',  120.50, 8000.00, 1, 1, 1),
    (1900000000000000002, 1, 1, 1, 'A-102', 'OFFICE',   85.00, 5500.00, 0, 1, 1),
    (1900000000000000003, 1, 1, 2, 'B-201', 'MEETING',  35.00, 2000.00, 1, 1, 1);

-- 5) 验证
SELECT 'sys_room_count' AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0;
SELECT 'park_space_menu' AS k, GROUP_CONCAT(CONCAT(id, '/', name)) AS v FROM sys_menu WHERE app_id = 6 AND deleted = 0;
