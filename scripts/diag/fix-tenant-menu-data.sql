-- 一站式数据修复: 跳过 Flyway, 直接 INSERT/UPDATE 让租户管理员能拿到 5 个 tab
-- 用法: docker exec -i mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} cloudhub_user < fix-tenant-menu-data.sql
-- 幂等: 重复跑无副作用

-- ============ 1) 加 opsadmin 租户管理员 (id=2) ============
INSERT IGNORE INTO `sys_user`
  (`id`, `username`, `password`, `nickname`, `mobile`, `email`, `status`, `user_type`, `tenant_id`)
VALUES
  (2, 'opsadmin', 'e10adc3949ba59abbe56e057f20f883e', '默认租户管理员', '13800138001', 'opsadmin@cloudhub.com', 1, 1, 1);

-- ============ 2) opsadmin 授权 SUPER_ADMIN 角色 (id=1) ============
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES (2, 1);

-- ============ 3) 清掉 tenant 1 旧数据 (V8 bug + 你手动加的可能有重复) ============
DELETE FROM `sys_tenant_app` WHERE `tenant_id` = 1;

-- ============ 4) 重新插入所有 5 个 system app (ROW_NUMBER 唯一 id) ============
INSERT INTO `sys_tenant_app` (`id`, `tenant_id`, `app_id`, `status`)
SELECT
    COALESCE((SELECT MAX(`id`) FROM `sys_tenant_app`), 0) + ROW_NUMBER() OVER (ORDER BY `id`),
    1, `id`, 1
FROM `sys_app`
WHERE `app_type` = 0;

-- ============ 5) 兜底: 任何 userType=1 但 tenant_id=NULL 的用户 → tenant_id=1 ============
UPDATE `sys_user` SET `tenant_id` = 1 WHERE `user_type` = 1 AND `tenant_id` IS NULL;

-- ============ 验证 (跑完这个文件, 应该看到 opsadmin + 5 个 app + 你的租户管理员 tenant_id=1) ============
SELECT 'opsadmin user' AS k, COUNT(*) AS v FROM sys_user WHERE username = 'opsadmin'
UNION ALL
SELECT 'tenant_apps', COUNT(*) FROM sys_tenant_app WHERE tenant_id = 1
UNION ALL
SELECT 'tenant_admins', GROUP_CONCAT(CONCAT(username, '(t=', tenant_id, ')')) FROM sys_user WHERE user_type = 1;
