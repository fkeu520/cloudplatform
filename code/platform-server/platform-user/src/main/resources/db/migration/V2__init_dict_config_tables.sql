-- ================================================
-- V2: 字典管理 + 参数配置表结构 + 初始数据
-- ================================================

CREATE TABLE IF NOT EXISTS `sys_dict_type` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `dict_name` VARCHAR(100) NOT NULL,
    `dict_type` VARCHAR(100) NOT NULL,
    `status` TINYINT DEFAULT 1,
    `remark` VARCHAR(255),
    `tenant_id` BIGINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_dict_type` (`dict_type`, `tenant_id`, `deleted`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_dict_data` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `dict_type` VARCHAR(100) NOT NULL,
    `dict_label` VARCHAR(100) NOT NULL,
    `dict_value` VARCHAR(100) NOT NULL,
    `dict_sort` INT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `css_class` VARCHAR(255),
    `remark` VARCHAR(255),
    `tenant_id` BIGINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    KEY `idx_dict_type` (`dict_type`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_config` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `config_name` VARCHAR(100) NOT NULL,
    `config_key` VARCHAR(100) NOT NULL,
    `config_value` VARCHAR(500) NOT NULL,
    `config_type` TINYINT DEFAULT 0,
    `remark` VARCHAR(255),
    `tenant_id` BIGINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    UNIQUE KEY `uk_config_key` (`config_key`, `tenant_id`, `deleted`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 字典初始数据
INSERT IGNORE INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `remark`, `tenant_id`) VALUES
(1, 'User Gender', 'sys_user_gender', 1, 'user gender dict', 1),
(2, 'Common Status', 'sys_normal_status', 1, 'enable/disable status', 1);

INSERT IGNORE INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `css_class`) VALUES
(1, 'sys_user_gender', 'Unknown', '0', 1, 1, ''),
(2, 'sys_user_gender', 'Male', '1', 2, 1, ''),
(3, 'sys_user_gender', 'Female', '2', 3, 1, ''),
(4, 'sys_normal_status', 'Enabled', '1', 1, 1, 'success'),
(5, 'sys_normal_status', 'Disabled', '0', 2, 1, 'danger');

-- 参数配置初始数据
INSERT IGNORE INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`) VALUES
(1, 'Default Password', 'sys.user.initPassword', '123456', 1, 'default password for new users'),
(2, 'Token Expire', 'sys.auth.tokenExpire', '604800', 1, 'JWT token expiry in seconds'),
(3, 'Site Name', 'sys.site.name', 'CloudHub', 0, 'system site title');