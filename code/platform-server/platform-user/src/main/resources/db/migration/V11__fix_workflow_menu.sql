-- 修复菜单数据：删除冲突数据，重新插入正确的流程中心菜单
-- 场景：V10 运行时部分菜单 ID 已被占用或数据不完整

-- 先清理占用了菜单 ID 26-37 的错误数据
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 26 AND 37;
DELETE FROM `sys_menu` WHERE `id` BETWEEN 26 AND 37;

-- 重新插入流程中心菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`) VALUES
(26, 0, '流程中心', '/workflow', 'Layout', 1, 'Collection', 150, ''),
(27, 26, '流程定义', '/workflow/definition', 'workflow/definition/index', 1, 'Files', 1, 'workflow:definition:list'),
(28, 27, '查看定义', NULL, NULL, 2, NULL, 0, 'workflow:definition:view'),
(29, 27, '部署流程', NULL, NULL, 2, NULL, 0, 'workflow:definition:deploy'),
(30, 27, '删除定义', NULL, NULL, 2, NULL, 0, 'workflow:definition:del'),
(31, 27, '发起流程', NULL, NULL, 2, NULL, 0, 'workflow:definition:start'),
(32, 26, '我的待办', '/workflow/task', 'workflow/task/index', 1, 'Edit', 2, 'workflow:task:list'),
(33, 32, '审批任务', NULL, NULL, 2, NULL, 0, 'workflow:task:approve'),
(34, 32, '转办任务', NULL, NULL, 2, NULL, 0, 'workflow:task:transfer'),
(35, 26, '流程监控', '/workflow/monitor', 'workflow/monitor/index', 1, 'Monitor', 3, 'workflow:monitor:list'),
(36, 35, '查看详情', NULL, NULL, 2, NULL, 0, 'workflow:monitor:view'),
(37, 35, '删除实例', NULL, NULL, 2, NULL, 0, 'workflow:monitor:del');

-- 超级管理员角色关联流程中心菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `id` BETWEEN 26 AND 37;
