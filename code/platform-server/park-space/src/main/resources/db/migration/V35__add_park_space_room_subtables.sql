-- ================================================================
-- V35: park-space Room 子表 (csyh 业务融合 W3.6)
--
-- 背景: W3.6 阶段 4 个 Room 子表迁移: RoomLockRecord, RoomPurpose, RoomRecord, RoomSplitMerge.
--       全部是 sys_room 的关联日志/字典表, append-only (W3 阶段不实现 update/delete 业务).
--
-- 设计:
--   - 全部使用雪花 ID (BaseEntity 已用 IdType.ASSIGN_ID)
--   - 逻辑删除 (BaseEntity.deleted, MP @TableLogic)
--   - 多租户 (tenant_id, 来自 P0-1 多租户拦截器)
--   - 表名前缀 sys_ (与 platform 命名规范一致)
-- ================================================================

-- 1) sys_room_purpose (房源用途字典)
CREATE TABLE IF NOT EXISTS `sys_room_purpose` (
    `id`            bigint       NOT NULL              COMMENT '主键 (雪花)',
    `purpose_name`  varchar(64)  NOT NULL              COMMENT '用途名称',
    `status`        tinyint      NOT NULL DEFAULT '1'  COMMENT '状态 (0=停用 1=启用)',
    `park_id`       bigint       DEFAULT NULL          COMMENT '园区ID',
    `tenant_id`     bigint       DEFAULT NULL          COMMENT '租户ID (P0-1 多租户)',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     bigint       DEFAULT NULL          COMMENT '创建人',
    `update_by`     bigint       DEFAULT NULL          COMMENT '更新人',
    `deleted`       tinyint      NOT NULL DEFAULT '0'  COMMENT '逻辑删除 (0=未删 1=已删)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_park_purpose_name` (`park_id`, `purpose_name`, `deleted`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房源用途字典表';

-- 2) sys_room_lock_record (房间锁定记录 - append-only)
CREATE TABLE IF NOT EXISTS `sys_room_lock_record` (
    `id`              bigint       NOT NULL              COMMENT '主键 (雪花)',
    `room_id`         bigint       DEFAULT NULL          COMMENT '房间 ID (关联 sys_room.id)',
    `is_lock`         tinyint      NOT NULL DEFAULT '0'  COMMENT '是否锁定 (0=解锁 1=锁定)',
    `enterprise_id`   bigint       DEFAULT NULL          COMMENT '企业/客户 ID (跨模块)',
    `enterprise_name` varchar(128) DEFAULT NULL          COMMENT '企业/客户名称',
    `operator`        varchar(64)  DEFAULT NULL          COMMENT '操作人',
    `reason`          varchar(500) DEFAULT NULL          COMMENT '操作原因',
    `days`            int          DEFAULT NULL          COMMENT '锁定天数',
    `park_id`         bigint       DEFAULT NULL          COMMENT '园区ID',
    `tenant_id`       bigint       DEFAULT NULL          COMMENT '租户ID (P0-1 多租户)',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       bigint       DEFAULT NULL          COMMENT '创建人',
    `update_by`       bigint       DEFAULT NULL          COMMENT '更新人',
    `deleted`         tinyint      NOT NULL DEFAULT '0'  COMMENT '逻辑删除 (0=未删 1=已删)',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_park_id` (`park_id`),
    KEY `idx_is_lock` (`is_lock`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间锁定记录表';

-- 3) sys_room_record (房间绑定/解绑记录 - append-only)
CREATE TABLE IF NOT EXISTS `sys_room_record` (
    `id`             bigint     NOT NULL              COMMENT '主键 (雪花)',
    `room_id`        bigint     DEFAULT NULL          COMMENT '房间 ID (关联 sys_room.id)',
    `customer_id`    bigint     DEFAULT NULL          COMMENT '客户 ID (跨模块)',
    `covenant_id`    bigint     DEFAULT NULL          COMMENT '合同 ID (跨模块引用 park-contract)',
    `covenant_type`  tinyint    DEFAULT 0             COMMENT '合同类型 (0=租赁 1=销售 2=其他)',
    `status`         tinyint    NOT NULL DEFAULT '0'  COMMENT '状态 (0=添加 1=解除)',
    `park_id`        bigint     DEFAULT NULL          COMMENT '园区ID',
    `tenant_id`      bigint     DEFAULT NULL          COMMENT '租户ID (P0-1 多租户)',
    `create_time`    datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime   DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      bigint     DEFAULT NULL          COMMENT '创建人',
    `update_by`      bigint     DEFAULT NULL          COMMENT '更新人',
    `deleted`        tinyint    NOT NULL DEFAULT '0'  COMMENT '逻辑删除 (0=未删 1=已删)',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_park_id` (`park_id`),
    KEY `idx_status` (`status`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间绑定/解绑记录表';

-- 4) sys_room_split_merge (房间拆分/合并记录 - append-only)
CREATE TABLE IF NOT EXISTS `sys_room_split_merge` (
    `id`            bigint       NOT NULL              COMMENT '主键 (雪花)',
    `user_id`       bigint       DEFAULT NULL          COMMENT '操作人 ID',
    `user_name`     varchar(64)  DEFAULT NULL          COMMENT '操作人姓名',
    `reasons`       varchar(500) DEFAULT NULL          COMMENT '操作原因',
    `type`          int          DEFAULT 0             COMMENT '操作类型 (具体语义 W3 不实现)',
    `old_room_id`   bigint       DEFAULT NULL          COMMENT '原房源 ID',
    `old_room_name` varchar(64)  DEFAULT NULL          COMMENT '原房源名称',
    `new_room_id`   bigint       DEFAULT NULL          COMMENT '新房源 ID',
    `new_room_name` varchar(64)  DEFAULT NULL          COMMENT '新房源名称',
    `num`           int          DEFAULT NULL          COMMENT '拆分数量',
    `is_extend`     tinyint      NOT NULL DEFAULT '0'  COMMENT '是否继承能源表 (0=否 1=是)',
    `status`        tinyint      NOT NULL DEFAULT '0'  COMMENT '状态 (0=合并 1=拆分)',
    `park_id`       bigint       DEFAULT NULL          COMMENT '园区ID',
    `tenant_id`     bigint       DEFAULT NULL          COMMENT '租户ID (P0-1 多租户)',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     bigint       DEFAULT NULL          COMMENT '创建人',
    `update_by`     bigint       DEFAULT NULL          COMMENT '更新人',
    `deleted`       tinyint      NOT NULL DEFAULT '0'  COMMENT '逻辑删除 (0=未删 1=已删)',
    PRIMARY KEY (`id`),
    KEY `idx_park_id` (`park_id`),
    KEY `idx_status` (`status`),
    KEY `idx_type` (`type`),
    KEY `idx_old_room_id` (`old_room_id`),
    KEY `idx_new_room_id` (`new_room_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间拆分/合并记录表';

-- 5) 空间中心菜单 (app_id=6 park-space)
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `perms`, `icon`, `sort`, `status`, `app_id`, `type`)
VALUES
    (113, 100, '房源用途',     '/room-purpose/page',    'room-purpose/Index',    'room-purpose:view',    'Notebook',     13, 1, 6, 1),
    (114, 100, '锁定记录',     '/room-lock-record/page','room-lock-record/Index','room-lock-record:view','Lock',         14, 1, 6, 1),
    (115, 100, '绑定记录',     '/room-record/page',     'room-record/Index',     'room-record:view',     'Connection',   15, 1, 6, 1),
    (116, 100, '拆分合并',     '/room-split-merge/page','room-split-merge/Index','room-split-merge:view','Files',        16, 1, 6, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path` = VALUES(`path`);

-- 6) 绑给 SUPER_ADMIN 角色 (id=1)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
    (1, 113), (1, 114), (1, 115), (1, 116);

-- 7) 种子数据 (供端到端验证)
-- 注意: 引用 sys_room (1900000000000000001=A-101)
INSERT IGNORE INTO `sys_room_purpose` (`id`, `park_id`, `purpose_name`, `status`, `tenant_id`, `create_by`) VALUES
    (1900000000200000001, 1, '自用', 1, 1, 1),
    (1900000000200000002, 1, '出租', 1, 1, 1),
    (1900000000200000003, 1, '出售', 1, 1, 1);

INSERT IGNORE INTO `sys_room_lock_record` (`id`, `room_id`, `is_lock`, `enterprise_id`, `enterprise_name`, `operator`, `reason`, `days`, `park_id`, `tenant_id`, `create_by`) VALUES
    (1900000000210000001, 1900000000000000001, 1, 100, '客户A', 'admin', '客户预定', 7, 1, 1, 1);

INSERT IGNORE INTO `sys_room_record` (`id`, `room_id`, `customer_id`, `covenant_id`, `covenant_type`, `status`, `park_id`, `tenant_id`, `create_by`) VALUES
    (1900000000220000001, 1900000000000000001, 100, 1, 0, 0, 1, 1, 1);

INSERT IGNORE INTO `sys_room_split_merge` (`id`, `user_id`, `user_name`, `reasons`, `type`, `old_room_id`, `old_room_name`, `new_room_id`, `new_room_name`, `num`, `is_extend`, `status`, `park_id`, `tenant_id`, `create_by`) VALUES
    (1900000000230000001, 1, 'admin', '客户要求拆分', 1, 1900000000000000001, 'A-101', 1900000000000000002, 'A-101-a', 2, 0, 1, 1, 1, 1);

-- 8) 验证
SELECT 'sys_room_purpose_count'     AS k, COUNT(*) AS v FROM sys_room_purpose      WHERE deleted = 0;
SELECT 'sys_room_lock_record_count' AS k, COUNT(*) AS v FROM sys_room_lock_record  WHERE deleted = 0;
SELECT 'sys_room_record_count'      AS k, COUNT(*) AS v FROM sys_room_record       WHERE deleted = 0;
SELECT 'sys_room_split_merge_count' AS k, COUNT(*) AS v FROM sys_room_split_merge  WHERE deleted = 0;
SELECT 'park_space_w36_menu'         AS k, GROUP_CONCAT(CONCAT(id, '/', name)) AS v FROM sys_menu WHERE app_id = 6 AND deleted = 0;