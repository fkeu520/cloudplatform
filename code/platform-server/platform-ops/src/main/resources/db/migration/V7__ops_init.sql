-- =============================================
-- 运营管理服务初始化表
-- =============================================

-- 租户表
CREATE TABLE IF NOT EXISTS `sys_tenant` (
    `id` bigint NOT NULL COMMENT '租户ID',
    `tenant_name` varchar(100) NOT NULL COMMENT '租户名称',
    `tenant_code` varchar(50) NOT NULL COMMENT '租户编码',
    `contact_person` varchar(50) DEFAULT NULL COMMENT '联系人',
    `contact_mobile` varchar(20) DEFAULT NULL COMMENT '联系电话',
    `contact_email` varchar(100) DEFAULT NULL COMMENT '联系邮箱',
    `address` varchar(200) DEFAULT NULL COMMENT '地址',
    `domain` varchar(100) DEFAULT NULL COMMENT '绑定域名',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（0停用 1启用）',
    `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
    `max_user_count` int DEFAULT '0' COMMENT '最大用户数（0不限）',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `sort` int DEFAULT '0' COMMENT '排序',
    `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

-- 初始化默认租户
INSERT INTO `sys_tenant` (`id`, `tenant_name`, `tenant_code`, `status`, `create_time`) VALUES
(1, '默认租户', 'default', 1, NOW())
ON DUPLICATE KEY UPDATE `tenant_name` = VALUES(`tenant_name`);

-- 服务网关路由表
CREATE TABLE IF NOT EXISTS `sys_gateway_route` (
    `id` bigint NOT NULL COMMENT '路由ID',
    `route_id` varchar(100) NOT NULL COMMENT '路由标识',
    `route_name` varchar(100) DEFAULT NULL COMMENT '路由名称',
    `uri` varchar(500) NOT NULL COMMENT '目标URI',
    `predicates` text COMMENT '谓词（JSON数组）',
    `filters` text COMMENT '过滤器（JSON数组）',
    `order_no` int DEFAULT '0' COMMENT '排序',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（0停用 1启用）',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_route_id` (`route_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关路由表';

-- 初始化现有路由（与 gateway application.yml 同步）
INSERT INTO `sys_gateway_route` (`id`, `route_id`, `route_name`, `uri`, `predicates`, `filters`, `order_no`, `status`) VALUES
(1, 'platform-user', '用户中心', 'http://localhost:8081', '[{\"name\":\"Path\",\"args\":{\"pattern\":\"/user/**\"}}]', NULL, 1, 1),
(2, 'platform-user-role', '角色管理', 'http://localhost:8081', '[{\"name\":\"Path\",\"args\":{\"pattern\":\"/role/**\"}}]', NULL, 2, 1),
(3, 'platform-user-menu', '菜单管理', 'http://localhost:8081', '[{\"name\":\"Path\",\"args\":{\"pattern\":\"/menu/**\"}}]', NULL, 3, 1),
(4, 'platform-user-dict-type', '字典管理', 'http://localhost:8081', '[{\"name\":\"Path\",\"args\":{\"pattern\":\"/dict/**\"}}]', NULL, 4, 1),
(5, 'platform-user-config', '参数配置', 'http://localhost:8081', '[{\"name\":\"Path\",\"args\":{\"pattern\":\"/config/**\"}}]', NULL, 5, 1),
(6, 'platform-user-org', '组织管理', 'http://localhost:8081', '[{\"name\":\"Path\",\"args\":{\"pattern\":\"/org/**\"}}]', NULL, 6, 1),
(7, 'platform-auth', '认证中心', 'http://127.0.0.1:8082', '[{\"name\":\"Path\",\"args\":{\"pattern\":\"/auth/**\"}}]', NULL, 7, 1)
ON DUPLICATE KEY UPDATE `route_name` = VALUES(`route_name`);

-- 登录日志表
CREATE TABLE IF NOT EXISTS `sys_login_log` (
    `id` bigint NOT NULL COMMENT '日志ID',
    `user_id` bigint DEFAULT NULL COMMENT '用户ID',
    `username` varchar(50) DEFAULT NULL COMMENT '用户名',
    `login_type` tinyint DEFAULT '0' COMMENT '登录类型（0密码 1短信 2第三方）',
    `ip` varchar(50) DEFAULT NULL COMMENT '登录IP',
    `location` varchar(100) DEFAULT NULL COMMENT '登录地点',
    `device` varchar(100) DEFAULT NULL COMMENT '设备信息',
    `browser` varchar(100) DEFAULT NULL COMMENT '浏览器',
    `os` varchar(50) DEFAULT NULL COMMENT '操作系统',
    `status` tinyint DEFAULT '1' COMMENT '状态（0失败 1成功）',
    `message` varchar(200) DEFAULT NULL COMMENT '消息',
    `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';
