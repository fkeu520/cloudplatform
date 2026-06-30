-- ============================================
-- V41: 补全系统管理操作权限菜单 + 分配给 SUPER_ADMIN
--
-- 背景 (KNOWN_ISSUES #37):
--   2026-06-29 admin 用户登录后, 角色管理页操作列空白.
--   根因: docker/fix_perms.sql 是手动脚本, 未在 217 部署链路中自动执行.
--   sys_menu 缺 id 38-45 (system:role/menu 操作权限),
--   sys_role_menu role=1 也未关联 → getUserPermissions 返的 list 不含
--   system:role:edit/add/del 和 system:menu:edit/add/del
--   → TableActions.hasPermission 返 false → 所有按钮过滤 → 操作列空.
--
-- 修复:
--   1. INSERT IGNORE 8 个 sys_menu 操作权限 (与 fix_perms.sql 对齐)
--   2. INSERT IGNORE sys_role_menu 把这 8 个菜单分给 SUPER_ADMIN (id=1)
--   3. 兜底: 给其他 admin-like 角色 (id=1 + code 包含 ADMIN) 也补一份
--
-- 设计原则:
--   - idempotent (INSERT IGNORE): 重复执行不会报错
--   - 不引入新角色: 复用现有 SUPER_ADMIN 角色
--   - 不动菜单结构: 与 docker/fix_perms.sql 内容对齐, 后续可删
--   - 部署策略: 217 Flyway disabled → 同时提供 scripts/diag 手动脚本
--     + 此 migration 作为新环境的 ground truth
--
-- 关联: KNOWN_ISSUES #37, fix_perms.sql
-- ============================================

-- 1. 插入缺失的 sys_menu (8 个)
INSERT IGNORE INTO `sys_menu`
    (`id`, `parent_id`, `name`, `type`, `perms`, `sort`, `status`, `deleted`, `create_time`, `update_time`)
VALUES
    (38, 3, '查看角色', 2, 'system:role:view', 0, 1, 0, NOW(), NOW()),
    (39, 3, '新增角色', 2, 'system:role:add',  0, 1, 0, NOW(), NOW()),
    (40, 3, '编辑角色', 2, 'system:role:edit', 0, 1, 0, NOW(), NOW()),
    (41, 3, '删除角色', 2, 'system:role:del',  0, 1, 0, NOW(), NOW()),
    (42, 4, '查看菜单', 2, 'system:menu:view', 0, 1, 0, NOW(), NOW()),
    (43, 4, '新增菜单', 2, 'system:menu:add',  0, 1, 0, NOW(), NOW()),
    (44, 4, '编辑菜单', 2, 'system:menu:edit', 0, 1, 0, NOW(), NOW()),
    (45, 4, '删除菜单', 2, 'system:menu:del',  0, 1, 0, NOW(), NOW());

-- 2. 把这 8 个菜单分给所有 code 含 ADMIN 的角色 (兜底 SUPER_ADMIN + 未来运维角色)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, m.id
FROM `sys_role` r, `sys_menu` m
WHERE r.code LIKE '%ADMIN%'
  AND r.deleted = 0
  AND m.id BETWEEN 38 AND 45
  AND m.deleted = 0;

-- sanity check: 验证 SUPER_ADMIN 角色至少拿到 8 个新菜单
SELECT 'sanity: SUPER_ADMIN role missing new menus', COUNT(*) AS missing_count
FROM `sys_menu` m
WHERE m.id BETWEEN 38 AND 45
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm
    JOIN `sys_role` r ON r.id = rm.role_id
    WHERE rm.menu_id = m.id AND r.code = 'SUPER_ADMIN' AND r.deleted = 0
  );
-- 期望: 0