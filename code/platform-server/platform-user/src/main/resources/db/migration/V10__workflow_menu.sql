-- 流程中心菜单
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`) VALUES
(26, 0, '流程中心', '/workflow', 'Layout', 1, 'fas fa-folder-open', 150, ''),
(27, 26, '流程定义', '/workflow/definition', 'workflow/definition/index', 1, 'fas fa-file', 1, 'workflow:definition:list'),
(28, 27, '查看定义', NULL, NULL, 2, NULL, 0, 'workflow:definition:view'),
(29, 27, '部署流程', NULL, NULL, 2, NULL, 0, 'workflow:definition:deploy'),
(30, 27, '删除定义', NULL, NULL, 2, NULL, 0, 'workflow:definition:del'),
(31, 27, '发起流程', NULL, NULL, 2, NULL, 0, 'workflow:definition:start'),
(32, 26, '我的待办', '/workflow/task', 'workflow/task/index', 1, 'fas fa-tasks', 2, 'workflow:task:list'),
(33, 32, '审批任务', NULL, NULL, 2, NULL, 0, 'workflow:task:approve'),
(34, 32, '转办任务', NULL, NULL, 2, NULL, 0, 'workflow:task:transfer'),
(35, 26, '流程监控', '/workflow/monitor', 'workflow/monitor/index', 1, 'fas fa-eye', 3, 'workflow:monitor:list'),
(36, 35, '查看详情', NULL, NULL, 2, NULL, 0, 'workflow:monitor:view'),
(37, 35, '删除实例', NULL, NULL, 2, NULL, 0, 'workflow:monitor:del');

-- 超级管理员（role_id=1）关联所有新菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `id` >= 26;
