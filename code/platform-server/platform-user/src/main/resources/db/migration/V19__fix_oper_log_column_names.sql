-- V19: 修正 sys_oper_log 列名与 BaseEntity 一致
-- BaseEntity 映射 create_time/update_time，但表使用 created_time/updated_time

ALTER TABLE `sys_oper_log` CHANGE COLUMN `created_time` `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE `sys_oper_log` CHANGE COLUMN `updated_time` `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
