-- ================================================
-- V5: 用户表添加部门和岗位字段
-- ================================================

-- 用户表添加部门ID和岗位ID字段
ALTER TABLE `sys_user` 
    ADD COLUMN `dept_id` BIGINT COMMENT '部门ID' AFTER `org_id`,
    ADD COLUMN `post_id` BIGINT COMMENT '岗位ID' AFTER `dept_id`,
    ADD KEY `idx_dept_id` (`dept_id`),
    ADD KEY `idx_post_id` (`post_id`);