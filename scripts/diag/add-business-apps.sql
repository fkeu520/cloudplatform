-- 一站式数据脚本: 添加业务模块应用到 sys_app + 授权给租户 1
-- 用法: docker exec -i mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} cloudhub_user < add-business-apps.sql
-- 幂等: 重复跑无副作用 (ON DUPLICATE KEY / INSERT IGNORE)

-- ============ 1) 插入新业务应用 (id 从 6 开始) ============
INSERT INTO `sys_app` (`id`, `app_name`, `app_code`, `app_icon`, `app_type`, `sort`, `status`) VALUES
(6,  '空间中心', 'space-center', 'Grid', 1, 10, 1),
(7,  '招商中心', 'investment-center', 'TrendCharts', 1, 20, 1),
(8,  '合同中心', 'contract-center', 'Document', 1, 30, 1),
(9,  '财务中心', 'finance-center', 'Money', 1, 40, 1),
(10, '物业管理', 'property-management', 'House', 1, 50, 1)
ON DUPLICATE KEY UPDATE `app_name` = VALUES(`app_name`), `status` = 1;

-- ============ 2) 授权给租户 1 ============
INSERT IGNORE INTO `sys_tenant_app` (`id`, `tenant_id`, `app_id`, `status`)
SELECT
    COALESCE((SELECT MAX(`id`) FROM `sys_tenant_app`), 0) + ROW_NUMBER() OVER (ORDER BY `id`),
    1, `id`, 1
FROM `sys_app`
WHERE `app_type` = 1;

-- ============ 3) 验证 ============
SELECT 'sys_app_count' AS k, COUNT(*) AS v FROM sys_app WHERE deleted = 0
UNION ALL
SELECT 'new_apps', GROUP_CONCAT(app_name) FROM sys_app WHERE id >= 6 AND deleted = 0
UNION ALL
SELECT 'tenant_1_apps', COUNT(*) FROM sys_tenant_app WHERE tenant_id = 1 AND status = 1;
