-- V15: 添加用户类型字段 + 用户-菜单直接授权表

-- 1. sys_user 添加 user_type 字段
ALTER TABLE `sys_user`
    ADD COLUMN `user_type` TINYINT(1) DEFAULT 0 COMMENT '用户类型: 0=普通用户 1=租户管理员 2=运营管理员' AFTER `tenant_id`;

-- 2. 更新现有 admin 用户为运营管理员
UPDATE `sys_user` SET `user_type` = 2 WHERE `id` = 1;

-- 3. 用户-菜单直接授权表（用于运营管理员菜单授权）
CREATE TABLE IF NOT EXISTS `sys_user_menu` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`user_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-菜单直接授权表';

-- 4. 给 admin 用户授权所有菜单
INSERT IGNORE INTO `sys_user_menu` (`user_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu`;
