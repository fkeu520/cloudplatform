-- ================================================================
-- V33: park-space 关联实体 (csyh 业务融合 W3.4)
--
-- 背景: W3.4 阶段 3 个关联实体迁移: Covenant, Energy, Equipment.
--       都是 sys_room / sys_park / sys_kit 的关联子表.
--
-- 设计:
--   - 全部使用雪花 ID (BaseEntity 已用 IdType.ASSIGN_ID)
--   - 逻辑删除 (BaseEntity.deleted, MP @TableLogic)
--   - 多租户 (tenant_id, 来自 P0-1 多租户拦截器)
--   - 状态字段: 0=停用 1=启用
--   - 表名前缀 sys_ (与 platform 命名规范一致)
--
-- 注意: W3 阶段不做跨模块外键强约束 (park-contract / park-energy 模块未上线)
-- ================================================================

-- 1) sys_covenant (合同-房间关联)
CREATE TABLE IF NOT EXISTS `sys_covenant` (
    `id`             bigint     NOT NULL              COMMENT '主键 (雪花)',
    `covenant_id`    bigint     DEFAULT NULL          COMMENT '合同 ID (跨模块引用 park-contract)',
    `covenant_type`  tinyint    DEFAULT 0             COMMENT '合同类型 (0=租赁 1=销售 2=其他)',
    `customer_id`    bigint     DEFAULT NULL          COMMENT '客户 ID',
    `room_id`        bigint     DEFAULT NULL          COMMENT '房间 ID (关联 sys_room.id)',
    `status`         tinyint    NOT NULL DEFAULT '1'  COMMENT '状态 (0=停用 1=启用)',
    `park_id`        bigint     DEFAULT NULL          COMMENT '园区ID',
    `tenant_id`      bigint     DEFAULT NULL          COMMENT '租户ID (P0-1 多租户)',
    `create_time`    datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime   DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      bigint     DEFAULT NULL          COMMENT '创建人',
    `update_by`      bigint     DEFAULT NULL          COMMENT '更新人',
    `deleted`        tinyint    NOT NULL DEFAULT '0'  COMMENT '逻辑删除 (0=未删 1=已删)',
    PRIMARY KEY (`id`),
    KEY `idx_park_id` (`park_id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_covenant_id` (`covenant_id`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同与房间关联表';

-- 2) sys_energy (能耗与房间关联)
CREATE TABLE IF NOT EXISTS `sys_energy` (
    `id`             bigint     NOT NULL              COMMENT '主键 (雪花)',
    `meter_id`       bigint     DEFAULT NULL          COMMENT '能源表 ID (跨模块)',
    `meter_class_id` bigint     DEFAULT NULL          COMMENT '能源表种类 (电表/水表/燃气表)',
    `room_id`        bigint     DEFAULT NULL          COMMENT '房间 ID (关联 sys_room.id)',
    `status`         tinyint    NOT NULL DEFAULT '1'  COMMENT '状态 (0=停用 1=启用)',
    `park_id`        bigint     DEFAULT NULL          COMMENT '园区ID',
    `tenant_id`      bigint     DEFAULT NULL          COMMENT '租户ID (P0-1 多租户)',
    `create_time`    datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime   DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      bigint     DEFAULT NULL          COMMENT '创建人',
    `update_by`      bigint     DEFAULT NULL          COMMENT '更新人',
    `deleted`        tinyint    NOT NULL DEFAULT '0'  COMMENT '逻辑删除 (0=未删 1=已删)',
    PRIMARY KEY (`id`),
    KEY `idx_park_id` (`park_id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_meter_id` (`meter_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='能耗与房间关联表';

-- 3) sys_equipment (设备设施)
CREATE TABLE IF NOT EXISTS `sys_equipment` (
    `id`              bigint       NOT NULL              COMMENT '主键 (雪花)',
    `equipment_name`  varchar(64)  NOT NULL              COMMENT '设备名称',
    `model`           varchar(64)  DEFAULT NULL          COMMENT '型号',
    `amount`          int          NOT NULL DEFAULT '1'  COMMENT '数量',
    `kit_id`          bigint       DEFAULT NULL          COMMENT '配套 ID (关联 sys_kit.id)',
    `status`          tinyint      NOT NULL DEFAULT '1'  COMMENT '状态 (0=停用 1=启用)',
    `park_id`         bigint       DEFAULT NULL          COMMENT '园区ID',
    `tenant_id`       bigint       DEFAULT NULL          COMMENT '租户ID (P0-1 多租户)',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       bigint       DEFAULT NULL          COMMENT '创建人',
    `update_by`       bigint       DEFAULT NULL          COMMENT '更新人',
    `deleted`         tinyint      NOT NULL DEFAULT '0'  COMMENT '逻辑删除 (0=未删 1=已删)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_park_kit_equipment_name` (`park_id`, `kit_id`, `equipment_name`, `deleted`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_kit_id` (`kit_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备设施表';

-- 4) 空间中心菜单 (app_id=6 park-space)
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `perms`, `icon`, `sort`, `status`, `app_id`, `type`)
VALUES
    (107, 100, '合同房间',   '/covenant/page',   'covenant/Index',   'covenant:view',   'Document',    7, 1, 6, 1),
    (108, 100, '能耗管理',   '/energy/page',     'energy/Index',     'energy:view',     'Lightning',   8, 1, 6, 1),
    (109, 100, '设备设施',   '/equipment/page',  'equipment/Index',  'equipment:view',  'Tools',       9, 1, 6, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path` = VALUES(`path`);

-- 5) 绑给 SUPER_ADMIN 角色 (id=1)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
    (1, 107), (1, 108), (1, 109);

-- 6) 种子数据 (供端到端验证)
-- 注意: 引用 sys_room (1900000000000000001=A-101), sys_kit (1900000000030000001=标准装修)
INSERT IGNORE INTO `sys_covenant` (`id`, `park_id`, `covenant_id`, `covenant_type`, `customer_id`, `room_id`, `status`, `tenant_id`, `create_by`) VALUES
    (1900000000060000001, 1, 1, 0, 100, 1900000000000000001, 1, 1, 1);

INSERT IGNORE INTO `sys_energy` (`id`, `park_id`, `meter_id`, `meter_class_id`, `room_id`, `status`, `tenant_id`, `create_by`) VALUES
    (1900000000070000001, 1, 1, 1, 1900000000000000001, 1, 1, 1);

INSERT IGNORE INTO `sys_equipment` (`id`, `park_id`, `equipment_name`, `model`, `amount`, `kit_id`, `status`, `tenant_id`, `create_by`) VALUES
    (1900000000080000001, 1, '空调', 'KFR-35', 2, 1900000000030000001, 1, 1, 1),
    (1900000000080000002, 1, '投影仪', 'PT-X400', 1, 1900000000030000002, 1, 1, 1);

-- 7) 验证
SELECT 'sys_covenant_count'  AS k, COUNT(*) AS v FROM sys_covenant  WHERE deleted = 0;
SELECT 'sys_energy_count'    AS k, COUNT(*) AS v FROM sys_energy    WHERE deleted = 0;
SELECT 'sys_equipment_count' AS k, COUNT(*) AS v FROM sys_equipment WHERE deleted = 0;
SELECT 'park_space_w34_menu' AS k, GROUP_CONCAT(CONCAT(id, '/', name)) AS v FROM sys_menu WHERE app_id = 6 AND deleted = 0;