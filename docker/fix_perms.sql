INSERT IGNORE INTO sys_menu (id, parent_id, name, type, perms, sort) VALUES
(38, 3, '查看角色', 2, 'system:role:view', 0),
(39, 3, '新增角色', 2, 'system:role:add', 0),
(40, 3, '编辑角色', 2, 'system:role:edit', 0),
(41, 3, '删除角色', 2, 'system:role:del', 0),
(42, 4, '查看菜单', 2, 'system:menu:view', 0),
(43, 4, '新增菜单', 2, 'system:menu:add', 0),
(44, 4, '编辑菜单', 2, 'system:menu:edit', 0),
(45, 4, '删除菜单', 2, 'system:menu:del', 0);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 1, id FROM sys_menu WHERE id BETWEEN 38 AND 45;
