-- ================================================
-- V6: 修复 sys_dept / sys_post 列名不匹配问题
-- BaseEntity 使用 create_time / update_time
-- ================================================

ALTER TABLE `sys_dept`
    CHANGE COLUMN `created_time` `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CHANGE COLUMN `updated_time` `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE `sys_post`
    CHANGE COLUMN `created_time` `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CHANGE COLUMN `updated_time` `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
