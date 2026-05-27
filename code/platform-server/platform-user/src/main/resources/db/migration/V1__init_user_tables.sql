-- ================================================
-- V1: 用户中心表结构 + 初始数据
-- 表: sys_organization, sys_user, sys_role, sys_user_role, sys_menu, sys_role_menu
-- ================================================

-- ----------------------------
-- 1. 组织机构表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_organization` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父组织ID',
    `name` VARCHAR(100) NOT NULL COMMENT '组织名称',
    `code` VARCHAR(50) NOT NULL COMMENT '组织编码',
    `type` TINYINT DEFAULT 1 COMMENT '类型：1公司 2部门',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记：0未删 1已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`, `tenant_id`, `deleted`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织机构表';

-- ----------------------------
-- 2. 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码（MD5）',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `mobile` VARCHAR(20) COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `gender` TINYINT DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    `org_id` BIGINT COMMENT '组织ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID',
    `last_login_ip` VARCHAR(50) COMMENT '最后登录IP',
    `last_login_time` DATETIME COMMENT '最后登录时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记：0未删 1已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`, `deleted`),
    UNIQUE KEY `uk_mobile` (`mobile`, `deleted`),
    KEY `idx_org_id` (`org_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 3. 角色表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `code` VARCHAR(50) NOT NULL COMMENT '角色编码',
    `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `remark` VARCHAR(255) COMMENT '备注',
    `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记：0未删 1已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`, `tenant_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ----------------------------
-- 4. 用户-角色关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ----------------------------
-- 5. 菜单权限表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_menu` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父菜单ID',
    `name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
    `path` VARCHAR(200) COMMENT '路由路径',
    `component` VARCHAR(255) COMMENT '组件路径',
    `type` TINYINT NOT NULL DEFAULT 1 COMMENT '类型：1菜单 2按钮',
    `icon` VARCHAR(50) COMMENT '图标',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `perms` VARCHAR(100) COMMENT '权限标识',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记：0未删 1已删',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_perms` (`perms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';

-- ----------------------------
-- 6. 角色-菜单关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- ----------------------------
-- 初始数据
-- ----------------------------

-- 根组织
INSERT IGNORE INTO `sys_organization` (`id`, `parent_id`, `name`, `code`, `type`, `sort`, `status`, `tenant_id`) VALUES
(1, 0, '云枢科技', 'CLOUDHUB', 1, 0, 1, 1);

-- 初始管理员
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `nickname`, `mobile`, `email`, `status`, `tenant_id`) VALUES
(1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', '13800138000', 'admin@cloudhub.com', 1, 1);

-- 基础角色
INSERT IGNORE INTO `sys_role` (`id`, `code`, `name`, `sort`, `remark`, `tenant_id`) VALUES
(1, 'SUPER_ADMIN', '超级管理员', 1, '拥有所有权限', 1),
(2, 'USER', '普通用户', 2, '基础用户角色', 1);

-- 管理员分配超级管理员角色
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- 基础菜单
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`) VALUES
(1, 0, '系统管理', '/system', 'Layout', 1, 'fas fa-cog', 100, ''),
(2, 1, '用户管理', '/system/user', 'system/user/index', 1, 'fas fa-users', 1, 'system:user:list'),
(3, 1, '角色管理', '/system/role', 'system/role/index', 1, 'fas fa-user-tag', 2, 'system:role:list'),
(4, 1, '菜单管理', '/system/menu', 'system/menu/index', 1, 'fas fa-bars', 3, 'system:menu:list'),
(5, 2, '查看用户', NULL, NULL, 2, NULL, 0, 'system:user:view'),
(6, 2, '新增用户', NULL, NULL, 2, NULL, 0, 'system:user:add'),
(7, 2, '编辑用户', NULL, NULL, 2, NULL, 0, 'system:user:edit'),
(8, 2, '删除用户', NULL, NULL, 2, NULL, 0, 'system:user:del'),
(9, 1, '组织管理', '/system/org', 'system/org/index', 1, 'fas fa-building', 4, 'system:org:list'),
(10, 9, '查看组织', NULL, NULL, 2, NULL, 0, 'system:org:view'),
(11, 9, '新增组织', NULL, NULL, 2, NULL, 0, 'system:org:add'),
(12, 9, '编辑组织', NULL, NULL, 2, NULL, 0, 'system:org:edit'),
(13, 9, '删除组织', NULL, NULL, 2, NULL, 0, 'system:org:del'),
(14, 0, '基础配置', '/base', 'Layout', 1, 'fas fa-th-large', 200, ''),
(15, 14, '字典管理', '/system/dict', 'system/dict/index', 1, 'fas fa-book', 1, 'system:dict:list'),
(16, 15, '查看字典', NULL, NULL, 2, NULL, 0, 'system:dict:view'),
(17, 15, '新增字典', NULL, NULL, 2, NULL, 0, 'system:dict:add'),
(18, 15, '编辑字典', NULL, NULL, 2, NULL, 0, 'system:dict:edit'),
(19, 15, '删除字典', NULL, NULL, 2, NULL, 0, 'system:dict:del'),
(20, 14, '参数配置', '/system/config', 'system/config/index', 1, 'fas fa-cog', 2, 'system:config:list'),
(21, 20, '查看参数', NULL, NULL, 2, NULL, 0, 'system:config:view'),
(22, 20, '新增参数', NULL, NULL, 2, NULL, 0, 'system:config:add'),
(23, 20, '编辑参数', NULL, NULL, 2, NULL, 0, 'system:config:edit'),
(24, 20, '删除参数', NULL, NULL, 2, NULL, 0, 'system:config:del'),
(25, 1, '操作日志', '/system/log', 'system/log/index', 1, 'fas fa-file-alt', 5, 'system:log:list');

-- 超级管理员拥有所有菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 1, `id` FROM `sys_menu`;