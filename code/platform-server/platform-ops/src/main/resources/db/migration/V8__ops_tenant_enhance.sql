-- =============================================
-- 租户管理增强：类型/应用授权
-- =============================================

-- 租户表新增字段
ALTER TABLE `sys_tenant`
    ADD COLUMN `tenant_type` tinyint NOT NULL DEFAULT '0' COMMENT '租户类型（0集团型 1单组织）' AFTER `tenant_code`;

-- 更新默认租户类型
UPDATE `sys_tenant` SET `tenant_type` = 1 WHERE `id` = 1;

-- 应用定义表
CREATE TABLE IF NOT EXISTS `sys_app` (
    `id` bigint NOT NULL COMMENT '应用ID',
    `app_name` varchar(100) NOT NULL COMMENT '应用名称',
    `app_code` varchar(50) NOT NULL COMMENT '应用编码',
    `app_icon` varchar(100) DEFAULT NULL COMMENT '应用图标',
    `app_type` tinyint NOT NULL DEFAULT '0' COMMENT '类型（0系统内置 1业务应用）',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（0停用 1启用）',
    `sort` int DEFAULT '0' COMMENT '排序',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_code` (`app_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用定义表';

-- 租户应用授权表
CREATE TABLE IF NOT EXISTS `sys_tenant_app` (
    `id` bigint NOT NULL COMMENT 'ID',
    `tenant_id` bigint NOT NULL COMMENT '租户ID',
    `app_id` bigint NOT NULL COMMENT '应用ID',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（0停用 1启用）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_app` (`tenant_id`, `app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户应用授权表';

-- 初始化系统应用
INSERT INTO `sys_app` (`id`, `app_name`, `app_code`, `app_icon`, `app_type`, `sort`, `status`) VALUES
(1, '系统管理', 'system', 'Setting', 0, 1, 1),
(2, '用户中心', 'user-center', 'User', 0, 2, 1),
(3, '流程中心', 'workflow', 'Connection', 0, 3, 0),
(4, '消息中心', 'notification', 'Message', 0, 4, 0),
(5, '运营管理', 'ops', 'Operation', 0, 5, 0)
ON DUPLICATE KEY UPDATE `app_name` = VALUES(`app_name`);

-- 默认租户授权系统应用
INSERT INTO `sys_tenant_app` (`id`, `tenant_id`, `app_id`)
SELECT 1, 1, `id` FROM `sys_app` WHERE `app_type` = 0
ON DUPLICATE KEY UPDATE `status` = 1;
