-- =============================================================================
-- 数据补丁: 清理 kefu / enterprise 旧菜单残留
-- 时机: V41/V54/V55 改 ID 后 (510-516 / 520-529), 旧 ID 已被 park-space 占用
-- 217 上手动跑一次 (Flyway 不重复执行)
-- =============================================================================

-- 1. 清理 224-226 (V54 老版本残留, parent_id=220 误用 park-space 地块管理)
--    保留 424-426 (V54 改 ID 后的正确副本)
UPDATE sys_menu SET deleted = 1 WHERE id IN (224, 225, 226) AND deleted = 0;

-- 2. 清理 420-429 (旧版 V54 残留, 当时用了 420-429 ID 范围, 现在统一迁到 520-529)
--    注意: 420-429 是 V54 旧部署创建的, app_id=11 但已被新版 V54 (520-529) 取代
--    保留 520-529, 删除 420-429 (sys_role_menu 关联一并删除)
DELETE FROM sys_role_menu WHERE menu_id BETWEEN 420 AND 429;
UPDATE sys_menu SET deleted = 1 WHERE id BETWEEN 420 AND 429 AND deleted = 0;

-- 3. 清理 sys_role_menu 中可能存在的旧 211-216 关联 (虽然 kefu 菜单没插进去, 但保险起见)
DELETE FROM sys_role_menu WHERE menu_id BETWEEN 211 AND 216;

-- 4. sanity check
SELECT 'V41 kefu menu (510-516)' AS scope, COUNT(*) AS cnt FROM sys_menu
    WHERE id BETWEEN 510 AND 516 AND deleted = 0;
SELECT 'V54 enterprise menu (520-529)' AS scope, COUNT(*) AS cnt FROM sys_menu
    WHERE id BETWEEN 520 AND 529 AND deleted = 0;
SELECT 'admin role 关联的 kefu+enterprise 菜单' AS scope, COUNT(*) AS cnt FROM sys_role_menu rm
    JOIN sys_menu m ON m.id = rm.menu_id
    WHERE rm.role_id = 1 AND m.id BETWEEN 510 AND 529 AND m.deleted = 0;

-- 期望: 7 / 10 / 17
