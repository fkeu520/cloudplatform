-- 请假申请测试菜单
INSERT IGNORE INTO sys_menu (id, parent_id, name, path, component, type, icon, sort, perms, status) VALUES
(47, 26, '请假申请', '/workflow/leave', 'workflow/leave/index', 1, 'fas fa-paper-plane', 4, 'workflow:leave:apply', 1);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (1, 47);