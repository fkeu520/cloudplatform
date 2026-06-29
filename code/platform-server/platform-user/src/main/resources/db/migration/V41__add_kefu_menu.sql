-- 智能问答菜单
-- ID 从 210 开始（200-209 已被其他菜单项占用）
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`, `status`) VALUES
(210, 0, '智能问答', '/kefu/', 'Layout', 1, 'fas fa-comment-dots', 180, 'kefu:menu', 1);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `id` >= 210 AND `id` <= 210;