-- V32: 新增「园区管理」菜单项
-- 园区管理放在「系统管理」(parent_id=1) 下，排序在日志查看(5)之后
-- 依赖: V1 (sys_menu 表)
-- 注意: V24 使用了 64-68 的 ID 段

INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`, `status`) VALUES
(69, 1, '园区管理', '/system/park', 'system/park/index', 1, 'fas fa-building', 6, 'system:park:list', 1),
(70, 69, '查看', NULL, NULL, 2, NULL, 0, 'system:park:view', 1),
(71, 69, '新增', NULL, NULL, 2, NULL, 0, 'system:park:add', 1),
(72, 69, '编辑', NULL, NULL, 2, NULL, 0, 'system:park:edit', 1),
(73, 69, '删除', NULL, NULL, 2, NULL, 0, 'system:park:del', 1);

-- 超级管理员拥有新菜单权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `id` BETWEEN 69 AND 73;
