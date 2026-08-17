-- =============================================================================
-- V57: 补充 kefu 智能问答子菜单 (会话管理/FAQ管理/知识导入/数据源/评估)
-- V56 已注册 510(智能问答) + 511-513(对话/知识库/仪表盘)
-- 此次新增 517-521 共 5 个子菜单, 补全与 prototype 一致的全部 8 项
-- =============================================================================

-- 1. 新增子菜单 (parent_id=510, type=2)
INSERT IGNORE INTO `sys_menu`
    (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`, `status`,
     `app_id`, `menu_category`, `create_time`, `update_time`)
VALUES
    (517, 510, '会话管理', '/kefu/sessions',    'kefu/Sessions',   2, 'fas fa-history',    4, 'kefu:sessions',   1, 7, 'admin', NOW(), NOW()),
    (518, 510, 'FAQ管理',  '/kefu/faqs',        'kefu/Faqs',       2, 'fas fa-question-circle', 5, 'kefu:faqs',    1, 7, 'admin', NOW(), NOW()),
    (519, 510, '知识导入', '/kefu/import',      'kefu/Import',     2, 'fas fa-file-import',   6, 'kefu:import',   1, 7, 'admin', NOW(), NOW()),
    (520, 510, '数据源',   '/kefu/settings',    'kefu/Settings',   2, 'fas fa-database',   7, 'kefu:settings',  1, 7, 'admin', NOW(), NOW()),
    (521, 510, '评估',     '/kefu/evaluation',  'kefu/Evaluation', 2, 'fas fa-chart-line', 8, 'kefu:evaluation', 1, 7, 'admin', NOW(), NOW());

-- 2. 授权 admin 角色 (role_id=1) 获得 517-521 共 5 个菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
    SELECT 1, m.id FROM sys_menu m
    WHERE m.id BETWEEN 517 AND 521 AND m.deleted = 0
    ON DUPLICATE KEY UPDATE role_id = role_id;

-- sanity check
SELECT 'kefu menu count after V57', COUNT(*) FROM sys_menu
    WHERE id BETWEEN 510 AND 521 AND deleted = 0;
-- 期望: 12 (510 目录 + 511-513 子菜单 + 514-516 按钮 + 517-521 子菜单)