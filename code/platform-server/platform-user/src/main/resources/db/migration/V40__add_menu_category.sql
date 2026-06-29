-- ============================================
-- V40: sys_menu 加 menu_category 列, 8 个 ops-admin 菜单入库
--
-- 背景:
--   2026-06-27 commit 751a20b 把 platform-ops-admin Layout 的硬编码菜单改用后端
--   拉取, 但 OpsUserController.getMenuTree() 选了无 appId 过滤的 /menu/tree 作为
--   代理路径, 导致 admin-platform 全量 sys_menu 串到 ops-admin (8090) 左侧菜单。
--
-- 设计原则:
--   - "几个平台, 跟代码强绑定": 平台枚举写在 Constants.MenuCategory.java (admin
--     | ops-admin | common), 不放 Nacos, 不放网关 header, 不增加新 sys_app。
--   - 新列 sys_menu.menu_category VARCHAR(32) 单一来源, deploy-time fix。
--   - 后端 /menu/by-category?category=X 物理隔离端点, 每个平台自己的 endpoint
--     硬编码 category 常量, 不走动态配置。
--
-- 关联: KNOWN_ISSUES #36.2 (短期方案占位, 本迁移为长期方案落地)
-- ============================================

ALTER TABLE sys_menu
    ADD COLUMN menu_category VARCHAR(32) NOT NULL DEFAULT 'admin'
    COMMENT 'admin | ops-admin | common — 前台枚举, deploy-time fix (Constants.MenuCategory)';

-- 兜底: 默认值已 = 'admin', 此 UPDATE 仅用于源历史行 menu_category 为空字符串的极端情况
UPDATE sys_menu SET menu_category = 'admin'
    WHERE menu_category IS NULL OR menu_category = '';

-- 8 个 ops-admin 菜单入库 (parent_id=0 顶层, app_id NULL — 不依赖 sys_app)
-- ID 选择 70-77: 历史最高约 64 (V24 数据范围) / 200 系列 (V36 空间菜单), 70-77 安全.
INSERT INTO `sys_menu`
    (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`,
     `status`, `deleted`, `app_id`, `create_time`, `update_time`, `menu_category`)
VALUES
    (70, 0, '租户管理',   '/tenant',         'tenant/Index',         1, 'fas fa-building',         1, 'ops:tenant:list',     1, 0, NULL, NOW(), NOW(), 'ops-admin'),
    (71, 0, '对象存储',   '/storage',        'storage/Index',        1, 'fas fa-database',         2, 'ops:storage:list',    1, 0, NULL, NOW(), NOW(), 'ops-admin'),
    (72, 0, '服务网关',   '/gateway',        'gateway/Index',        1, 'fas fa-plug',             3, 'ops:gateway:list',    1, 0, NULL, NOW(), NOW(), 'ops-admin'),
    (73, 0, '日志审计',   '/audit',          'audit/Index',          1, 'fas fa-clipboard-list',   4, 'ops:audit:list',      1, 0, NULL, NOW(), NOW(), 'ops-admin'),
    (74, 0, '用户管理',   '/ops-user',       'ops-user/Index',       1, 'fas fa-users-cog',        5, 'ops:user:list',       1, 0, NULL, NOW(), NOW(), 'ops-admin'),
    (75, 0, '消息记录',   '/message/record', 'message/record/Index', 1, 'fas fa-bell',             6, 'ops:message:list',    1, 0, NULL, NOW(), NOW(), 'ops-admin'),
    (76, 0, '系统监控',   '/monitor',        'monitor/Index',        1, 'fas fa-heartbeat',        7, 'ops:monitor:list',    1, 0, NULL, NOW(), NOW(), 'ops-admin'),
    (77, 0, '运维管理',   '/ops-entry',      'ops-entry/Index',      1, 'fas fa-tools',            8, 'ops:ops-entry:list',  1, 0, NULL, NOW(), NOW(), 'ops-admin');

-- sanity: 验证 8 行已入库
SELECT 'sanity: ops-admin menu count', COUNT(*) FROM sys_menu
    WHERE menu_category = 'ops-admin' AND deleted = 0;
-- 期望: 8
