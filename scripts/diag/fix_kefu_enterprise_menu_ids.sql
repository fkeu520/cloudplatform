-- =============================================================================
-- 数据补丁: 清理 kefu 旧菜单残留
-- 时机: V56 跑完后 (510-516), 旧 ID 已被 park-space 占用
-- 217 上手动跑一次 (Flyway 不重复执行)
-- =============================================================================

-- 1. 清理 211-216 (V41 老版本残留, parent_id=210 误用 park-space 已占用 ID)
DELETE FROM sys_role_menu WHERE menu_id BETWEEN 211 AND 216;
UPDATE sys_menu SET deleted = 1 WHERE id BETWEEN 211 AND 216 AND deleted = 0;

-- 2. sanity check
SELECT 'V56 kefu menu (510-516)' AS scope, COUNT(*) AS cnt FROM sys_menu
    WHERE id BETWEEN 510 AND 516 AND deleted = 0;
SELECT 'admin role 关联的 kefu 菜单' AS scope, COUNT(*) AS cnt FROM sys_role_menu rm
    JOIN sys_menu m ON m.id = rm.menu_id
    WHERE rm.role_id = 1 AND m.id BETWEEN 510 AND 516 AND m.deleted = 0;

-- 期望: 7 / 7