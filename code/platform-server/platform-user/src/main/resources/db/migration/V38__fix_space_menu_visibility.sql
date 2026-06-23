-- ================================================================
-- V38: 修复空间菜单可见性 (V37 后续)
--
-- 问题:
--   1. 空间资产 (id=200) 及其子菜单在系统管理 tab 下不显示
--      原因: V37 SQL 重组菜单层级时, 未统一设置 app_id.
--      现状:
--        - 200 空间资产      app_id=10 (property-management, 错)
--        - 210 空间设置      app_id=NULL (错)
--        - 220 地块管理      app_id=NULL (错)
--        - 200 子菜单 (201 除外) app_id=NULL
--        - 201 园区管理      app_id=10 (跟 200 一致, 但也错)
--      前端 Layout.vue 调用 GET /menu/user?appId=1 (系统管理 tab),
--      后端 SQL 过滤 app_id=1 → 这些菜单被过滤掉.
--
--   2. 100 空间中心 不应显示 (legacy 旧菜单, V36 应删但漏删)
--      原因: V36 SQL 重组时, 把 200/210/220 从 100 拆出来, 但
--      没有删除 100 及其 16 个子菜单 (101-116).
--      当前: 100 status=1 + 16 个子菜单 status=1, 在 sys_role_menu 中.
--
-- 修复:
--   1. 把 200/210/220 + 所有子菜单 + 孙菜单 统一到 app_id=1 (system),
--      让它们在"系统管理"tab 下显示
--   2. 把 100 及其所有子菜单 status=0, 并从 sys_role_menu 中清除
--   3. 重新授权 role=1,确保新结构完整覆盖
--
-- 依赖: V36 (基础结构), V37 (V37 menu v2), V1 (sys_menu / sys_role_menu)
-- ================================================================

-- ========== 1. 空间菜单归到 system app (id=1) ==========
-- 1.1 三个一级菜单
UPDATE `sys_menu` SET `app_id`=1 WHERE `id` IN (200, 210, 220);

-- 1.2 二级菜单 (200/210/220 的直接子菜单)
UPDATE `sys_menu` SET `app_id`=1
 WHERE `parent_id` IN (200, 210, 220);

-- 1.3 三级菜单 (按钮权限, 例如 201 园区管理下的 227-230)
UPDATE `sys_menu` SET `app_id`=1
 WHERE `parent_id` IN (SELECT id FROM (
     SELECT id FROM `sys_menu` WHERE `parent_id` IN (200, 210, 220)
 ) AS t);

-- ========== 2. 隐藏 legacy 100 空间中心 ==========
-- 2.1 软删除: status=0 (保留记录以便审计, 后续可恢复)
UPDATE `sys_menu` SET `status`=0
 WHERE `id`=100 OR `parent_id`=100;

-- 2.2 清理角色授权: 防止前端 role_menu 关联导致意外显示
DELETE FROM `sys_role_menu`
 WHERE `menu_id`=100
    OR `menu_id` IN (SELECT id FROM (
        SELECT id FROM `sys_menu` WHERE `parent_id`=100
    ) AS t);

-- ========== 3. 重新授权 role=1 (超级管理员) ==========
-- 防御性: 清掉旧的 200-230 范围授权, 按 parent_id 重新授
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 200 AND 230;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `id` IN (200, 210, 220) AND `status`=1;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu`
 WHERE `parent_id` IN (200, 210, 220) AND `status`=1;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu`
 WHERE `parent_id` IN (SELECT id FROM (
     SELECT id FROM `sys_menu` WHERE `parent_id` IN (200, 210, 220)
 ) AS t) AND `status`=1;

-- ========== 4. 验证查询 (供 apply 脚本检查) ==========
-- 应返回 0 (200/210/220 都没有 app_id != 1 的情况)
SELECT 'sanity: 200/210/220 app_id 全部应为 1' AS check_name,
       SUM(CASE WHEN app_id = 1 THEN 0 ELSE 1 END) AS fail_count
  FROM `sys_menu`
 WHERE `id` IN (200, 210, 220)
    OR `parent_id` IN (200, 210, 220)
    OR `parent_id` IN (SELECT id FROM (
        SELECT id FROM `sys_menu` WHERE `parent_id` IN (200, 210, 220)
    ) AS t);

-- 应返回 0 (100 及其子菜单 status 全部应为 0)
SELECT 'sanity: 100 及子菜单 status 全部应为 0' AS check_name,
       SUM(CASE WHEN status = 0 THEN 0 ELSE 1 END) AS fail_count
  FROM `sys_menu`
 WHERE `id` = 100 OR `parent_id` = 100;

-- 应返回 0 (100 及其子菜单不应出现在 sys_role_menu)
SELECT 'sanity: 100 不应出现在 sys_role_menu' AS check_name,
       COUNT(*) AS fail_count
  FROM `sys_role_menu`
 WHERE `menu_id` = 100
    OR `menu_id` IN (SELECT id FROM (
        SELECT id FROM `sys_menu` WHERE `parent_id` = 100
    ) AS t);