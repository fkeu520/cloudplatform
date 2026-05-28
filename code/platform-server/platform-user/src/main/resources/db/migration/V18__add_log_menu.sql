-- V18: 新增日志查看菜单（含操作日志 + 登录日志）

INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`, `status`, `create_time`, `update_time`)
VALUES (25, 1, '日志查看', '/system/log', 'system/log/index', 1, 'fas fa-file-alt', 5, 'system:log:list', 1, NOW(), NOW());
