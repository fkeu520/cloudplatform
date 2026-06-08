-- ================================================-- 手动执行：注册「数据权限配置」菜单-- 用途: Flyway 当前 disabled，此脚本供手动执行到 platform 库-- 执行方式:--   docker exec -i platform-mysql mysql -uplatform -pplatform123 platform < doc/sql/V24_manual_add_datascope_menu.sql-- ================================================
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`, `status`) VALUES(64, 20, '数据权限配置', '/system/config/data-scope', 'system/config/dataScopeUpgrade', 1, 'fas fa-shield-alt', 3, 'system:config:datascope:list', 1),
(65, 64, '查看', NULL, NULL, 2, NULL, 0, 'system:config:datascope:view', 1),
(66, 64, '新增', NULL, NULL, 2, NULL, 0, 'system:config:datascope:add', 1),
(67, 64, '编辑', NULL, NULL, 2, NULL, 0, 'system:config:datascope:edit', 1),
(68, 64, '删除', NULL, NULL, 2, NULL, 0, 'system:config:datascope:del', 1);

-- 若 V20 已执行，补 app_id（跟随父菜单「参数配置」app_id=2）
UPDATE sys_menu SET app_id = 2 WHERE id = 64 AND app_id IS NULL;
UPDATE sys_menu SET app_id = 2 WHERE parent_id = 64 AND app_id IS NULL;

-- 超级管理员拥有新菜单权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `id` BETWEEN 64 AND 68;
