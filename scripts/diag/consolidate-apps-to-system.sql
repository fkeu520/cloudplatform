-- ================================================================
-- 合并 app: 用户中心/流程中心/消息中心 → 系统管理
-- 背景:
--   1. V8 seed 创建 5 个 system app (id=1-5):
--      - 1: 系统管理 (system)
--      - 2: 用户中心 (user-center) - 含基础配置菜单 (id=14)
--      - 3: 流程中心 (workflow)
--      - 4: 消息中心 (notification)
--      - 5: 运营管理 (ops) - 8090 用
--   2. V20 给 sys_menu 加 app_id, 4 个根菜单分别挂到 4 个 app
--   3. W3 v7.6 引入租户管理员自动授权, 每个 system app 都要授权
--   4. 用户决策: 4 个独立 tab 太碎, 合并为 1 个"系统管理" tab
--
-- 行动:
--   1) sys_menu.app_id: 2/3/4 → 1 (菜单全部归到系统管理)
--   2) sys_app.status: 2/3/4 → 0 (停用, 数据保留便于回滚)
--   3) sys_tenant_app.status: 2/3/4 → 0 (停用, 与 app 同步)
--
-- 幂等: 重复跑无副作用 (已合并的状态再 SET 不变)
-- 用法: docker exec -i mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} cloudhub_user < consolidate-apps-to-system.sql
-- ================================================================

-- ============ 1) 备份现状 (供回滚参考, 不影响主流程) ============
-- 如果需要回滚, 用 sys_menu_app_id_backup 表恢复 (脚本末尾会建)

DROP TABLE IF EXISTS `sys_menu_app_id_backup`;
CREATE TABLE `sys_menu_app_id_backup` AS
    SELECT `id`, `app_id` AS `old_app_id`, NOW() AS `backup_time`
    FROM `sys_menu`
    WHERE `app_id` IN (2, 3, 4) AND `deleted` = 0;

SELECT 'backup_rows' AS k, COUNT(*) AS v FROM sys_menu_app_id_backup;

-- ============ 2) 合并 sys_menu.app_id: 2/3/4 → 1 ============
-- 把"用户中心/基础配置/流程中心/消息中心"4 个模块的菜单全部归到"系统管理"
UPDATE `sys_menu`
SET `app_id` = 1
WHERE `app_id` IN (2, 3, 4) AND `deleted` = 0;

-- ============ 3) 停用 sys_app: 2/3/4 → status=0 ============
-- 数据保留 (不 DELETE), 便于回滚
UPDATE `sys_app`
SET `status` = 0
WHERE `id` IN (2, 3, 4) AND `deleted` = 0;

-- ============ 4) 停用 sys_tenant_app: app 2/3/4 的授权 → status=0 ============
-- 租户管理员不再看到这 3 个 app (虽然已经 status=0, 这里双保险)
UPDATE `sys_tenant_app`
SET `status` = 0
WHERE `app_id` IN (2, 3, 4);

-- ============ 5) 验证 1: app 状态 ============
SELECT '=== sys_app status ===' AS section;
SELECT `id`, `app_name`, `app_code`, `app_type`, `status`
FROM `sys_app`
WHERE `deleted` = 0
ORDER BY `id`;

-- ============ 6) 验证 2: menu app_id 分布 (应该全在 app 1 + 业务 app 6-10) ============
SELECT '=== sys_menu app_id distribution ===' AS section;
SELECT `app_id`, COUNT(*) AS menu_count
FROM `sys_menu`
WHERE `deleted` = 0 AND `app_id` IS NOT NULL
GROUP BY `app_id`
ORDER BY `app_id`;

-- ============ 7) 验证 3: sys_tenant_app 状态 ============
SELECT '=== sys_tenant_app status ===' AS section;
SELECT ta.`tenant_id`, ta.`app_id`, a.`app_name`, ta.`status`
FROM `sys_tenant_app` ta
LEFT JOIN `sys_app` a ON a.`id` = ta.`app_id`
ORDER BY ta.`tenant_id`, ta.`app_id`;

-- ============ 8) 验证 4: 合并后 app 1 的菜单树 ============
SELECT '=== app 1 (系统管理) root menus ===' AS section;
SELECT `id`, `parent_id`, `name`, `path`, `app_id`
FROM `sys_menu`
WHERE `app_id` = 1 AND `parent_id` = 0 AND `deleted` = 0
ORDER BY `sort`;

-- ============ 9) 检查是否还有孤儿菜单 (app_id 为 NULL 或指向停用 app) ============
SELECT '=== orphan menu check ===' AS section;
SELECT COUNT(*) AS orphan_count
FROM `sys_menu` m
LEFT JOIN `sys_app` a ON a.`id` = m.`app_id`
WHERE m.`deleted` = 0
  AND (m.`app_id` IS NULL OR (a.`status` = 0 AND m.`app_id` != 1));

-- ============ 10) 回滚提示 ============
SELECT '=== rollback hint ===' AS section;
SELECT '回滚 SQL 模板: UPDATE sys_menu m JOIN sys_menu_app_id_backup b ON m.id=b.id SET m.app_id=b.old_app_id; UPDATE sys_app SET status=1 WHERE id IN (2,3,4); UPDATE sys_tenant_app SET status=1 WHERE app_id IN (2,3,4);' AS hint;
