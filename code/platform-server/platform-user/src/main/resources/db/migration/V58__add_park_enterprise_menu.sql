-- =============================================================================
-- V58: park-enterprise 企业档案菜单登记 (parent_id=520)
-- 旧 220-229 (V54 用了被 park-space 占用的 ID) 改用 520-529 (500 段空闲)
-- V54 已成功跑过 (2026-06-30 13:05:20), 但 sys_menu 220-229 被 INSERT IGNORE 静默跳过
-- 此次 V58 用全新 ID 重新注册 sys_menu + sys_role_menu
-- =============================================================================

-- 顶层目录 (parent_id=0, type=1)
INSERT IGNORE INTO `sys_menu`
    (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`,
     `status`, `deleted`, `app_id`, `create_time`, `update_time`, `menu_category`)
VALUES
    (520, 0, '企业档案', '/enterprise/', 'Layout', 1, 'fas fa-building', 250, 'enterprise:menu',
     1, 0, 11, NOW(), NOW(), 'admin');

-- 三个子菜单 (parent_id=520, type=2)
INSERT IGNORE INTO `sys_menu`
    (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`,
     `status`, `deleted`, `app_id`, `create_time`, `update_time`, `menu_category`)
VALUES
    (521, 520, '企业列表', 'page', 'park/enterprise/Index', 2, 'fas fa-list',  1, 'enterprise:list',     1, 0, 11, NOW(), NOW(), 'admin'),
    (522, 520, '企业标签', 'tag',  'park/enterprise/Tag',   2, 'fas fa-tags',  2, 'enterprise:tag:list',  1, 0, 11, NOW(), NOW(), 'admin'),
    (523, 520, '企业绑定', 'bind', 'park/enterprise/Bind',  2, 'fas fa-link',  3, 'enterprise:bind:list', 1, 0, 11, NOW(), NOW(), 'admin');

-- 按钮级权限 (parent_id=520, type=3)
INSERT IGNORE INTO `sys_menu`
    (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`,
     `status`, `deleted`, `app_id`, `create_time`, `update_time`, `menu_category`)
VALUES
    (524, 520, '新增企业', '', '', 3, '', 1, 'enterprise:add',         1, 0, 11, NOW(), NOW(), 'admin'),
    (525, 520, '编辑企业', '', '', 3, '', 2, 'enterprise:edit',        1, 0, 11, NOW(), NOW(), 'admin'),
    (526, 520, '删除企业', '', '', 3, '', 3, 'enterprise:delete',      1, 0, 11, NOW(), NOW(), 'admin'),
    (527, 520, '启停企业', '', '', 3, '', 4, 'enterprise:status',      1, 0, 11, NOW(), NOW(), 'admin'),
    (528, 520, '新增标签', '', '', 3, '', 5, 'enterprise:tag:add',     1, 0, 11, NOW(), NOW(), 'admin'),
    (529, 520, '新增绑定', '', '', 3, '', 6, 'enterprise:bind:add',    1, 0, 11, NOW(), NOW(), 'admin');

-- 角色 menu 授权 (admin 角色 role_id=1 自动获得 520-529 共 10 个菜单)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
    SELECT 1, m.id FROM sys_menu m
    WHERE m.id BETWEEN 520 AND 529 AND m.deleted = 0
    ON DUPLICATE KEY UPDATE role_id = role_id;
