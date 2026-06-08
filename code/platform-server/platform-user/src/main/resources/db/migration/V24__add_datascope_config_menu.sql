-- V24: 新增「数据权限配置」菜单项（sys_menu + sys_role_menu）
-- 依赖: V1(基础表) / V14(消息菜单, id 用到 63) / V20(app_id)

INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`, `status`) VALUES
(64, 20, '数据权限配置', '/system/config/data-scope', 'system/config/dataScopeUpgrade', 1, 'fas fa-shield-alt', 3, 'system:config:datascope:list', 1),
(65, 64, '查看', NULL, NULL, 2, NULL, 0, 'system:config:datascope:view', 1),
(66, 64, '新增', NULL, NULL, 2, NULL, 0, 'system:config:datascope:add', 1),
(67, 64, '编辑', NULL, NULL, 2, NULL, 0, 'system:config:datascope:edit', 1),
(68, 64, '删除', NULL, NULL, 2, NULL, 0, 'system:config:datascope:del', 1);

-- 设置 app_id（跟随父菜单「参数配置」的 app_id=2）
UPDATE sys_menu SET app_id = 2 WHERE id = 64;
UPDATE sys_menu SET app_id = 2 WHERE parent_id = 64;

-- 超级管理员拥有新菜单权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `id` BETWEEN 64 AND 68;
