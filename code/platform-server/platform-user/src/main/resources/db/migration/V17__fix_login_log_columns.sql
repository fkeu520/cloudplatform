-- V17: 补充 sys_login_log 缺少的字段
ALTER TABLE `sys_login_log`
    ADD COLUMN `user_type` TINYINT DEFAULT 0 COMMENT '用户类型' AFTER `username`,
    ADD COLUMN `tenant_id` BIGINT DEFAULT 0 COMMENT '租户ID' AFTER `user_type`;
