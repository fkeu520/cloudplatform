-- 消息中心菜单（sys_menu 表在 platform 库中，因此菜单迁移保留在此）
-- ID 从 48 开始（38-47 已被其他菜单项占用）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`) VALUES
(48, 0, '消息中心', '/message/list', 'Layout', 1, 'Bell', 160, 'message:list'),
(49, 48, '消息列表', '/message/list', 'message/list/index', 1, 'Bell', 1, 'message:list:view'),
(50, 49, '查看详情', NULL, NULL, 2, NULL, 0, 'message:list:detail'),
(51, 49, '删除消息', NULL, NULL, 2, NULL, 0, 'message:list:del'),
(52, 48, '渠道配置', '/message/channel', 'message/channel/index', 1, 'Setting', 3, 'message:channel:list'),
(53, 52, '新增', NULL, NULL, 2, NULL, 0, 'message:channel:add'),
(54, 52, '编辑', NULL, NULL, 2, NULL, 0, 'message:channel:edit'),
(55, 52, '测试发送', NULL, NULL, 2, NULL, 0, 'message:channel:test'),
(56, 48, '短信模板', '/message/template', 'message/template/index', 1, 'Notebook', 4, 'message:template:list'),
(57, 56, '新增', NULL, NULL, 2, NULL, 0, 'message:template:add'),
(58, 56, '编辑', NULL, NULL, 2, NULL, 0, 'message:template:edit'),
(59, 56, '删除', NULL, NULL, 2, NULL, 0, 'message:template:del'),
(60, 48, '消息记录', '/message/record', 'message/record/index', 1, 'List', 2, 'message:record:list'),
(61, 60, '查看详情', NULL, NULL, 2, NULL, 0, 'message:record:detail'),
(62, 60, '重发', NULL, NULL, 2, NULL, 0, 'message:record:resend'),
(63, 60, '删除', NULL, NULL, 2, NULL, 0, 'message:record:del');

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `id` >= 48;
