-- V16: 添加登录日志表 + 操作日志添加租户ID

-- 1. sys_oper_log 添加 tenant_id
ALTER TABLE `sys_oper_log`
    ADD COLUMN `tenant_id` BIGINT DEFAULT 0 COMMENT '租户ID' AFTER `deleted`;

-- 2. 登录日志表
CREATE TABLE IF NOT EXISTS `sys_login_log` (
    `id`          BIGINT NOT NULL COMMENT '日志ID',
    `user_id`     BIGINT DEFAULT NULL COMMENT '用户ID',
    `username`    VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    `user_type`   TINYINT DEFAULT 0 COMMENT '用户类型 0普通用户 1租户管理员 2运营管理员',
    `tenant_id`   BIGINT DEFAULT 0 COMMENT '租户ID',
    `login_type`  TINYINT DEFAULT '0' COMMENT '登录类型（0密码 1短信 2第三方）',
    `ip`          VARCHAR(50) DEFAULT NULL COMMENT '登录IP',
    `location`    VARCHAR(100) DEFAULT NULL COMMENT '登录地点',
    `device`      VARCHAR(100) DEFAULT NULL COMMENT '设备信息',
    `browser`     VARCHAR(100) DEFAULT NULL COMMENT '浏览器',
    `os`          VARCHAR(50) DEFAULT NULL COMMENT '操作系统',
    `status`      TINYINT DEFAULT '1' COMMENT '状态（0失败 1成功）',
    `message`     VARCHAR(200) DEFAULT NULL COMMENT '消息',
    `login_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';
