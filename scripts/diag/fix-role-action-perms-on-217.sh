#!/bin/bash
# 立即修复 217 上 admin 角色缺失的操作权限 (2026-06-29 发现)
# 关联: KNOWN_ISSUES #37
# 根因: docker/fix_perms.sql 未在 217 执行, 导致 sys_menu 缺 38-45 (操作权限),
#       sys_role_menu role=1 不含这些菜单 → admin 用户 hasPermission('system:role:edit') 返 false
#       → 角色管理页 TableActions 过滤掉所有按钮 → 操作列空白

set -e

docker exec platform-mysql mysql -uplatform -pplatform123 platform -e "
-- 1. 插入缺失的 sys_menu (8 个操作权限)
INSERT IGNORE INTO sys_menu (id, parent_id, name, type, perms, sort, status, deleted, create_time, update_time) VALUES
(38, 3, '查看角色', 2, 'system:role:view', 0, 1, 0, NOW(), NOW()),
(39, 3, '新增角色', 2, 'system:role:add', 0, 1, 0, NOW(), NOW()),
(40, 3, '编辑角色', 2, 'system:role:edit', 0, 1, 0, NOW(), NOW()),
(41, 3, '删除角色', 2, 'system:role:del', 0, 1, 0, NOW(), NOW()),
(42, 4, '查看菜单', 2, 'system:menu:view', 0, 1, 0, NOW(), NOW()),
(43, 4, '新增菜单', 2, 'system:menu:add', 0, 1, 0, NOW(), NOW()),
(44, 4, '编辑菜单', 2, 'system:menu:edit', 0, 1, 0, NOW(), NOW()),
(45, 4, '删除菜单', 2, 'system:menu:del', 0, 1, 0, NOW(), NOW());

-- 2. 把这些菜单分配给 SUPER_ADMIN 角色 (id=1)
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 1, id FROM sys_menu WHERE id BETWEEN 38 AND 45;

-- 3. 验证
SELECT 'fix_perms applied' as status, COUNT(*) as missing_menus
FROM sys_menu m
WHERE m.id BETWEEN 38 AND 45 AND m.deleted = 0;

SELECT 'role 1 missing perms after fix' as section, COUNT(*) as count
FROM sys_menu m
WHERE m.id BETWEEN 38 AND 45
AND m.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id);
"