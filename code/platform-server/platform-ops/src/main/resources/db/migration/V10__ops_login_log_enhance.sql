-- V10: 登录日志添加 user_type 和 tenant_id 字段
ALTER TABLE `sys_login_log`
    ADD COLUMN `user_type` TINYINT DEFAULT 0 COMMENT '用户类型: 0=普通用户 1=租户管理员 2=运营管理员' AFTER `username`,
    ADD COLUMN `tenant_id` BIGINT DEFAULT 0 COMMENT '租户ID' AFTER `user_type`;
