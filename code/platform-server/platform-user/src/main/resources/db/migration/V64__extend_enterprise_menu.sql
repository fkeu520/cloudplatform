-- =============================================================================
-- V59: 企业档案菜单扩展 (补充行业/评分/客户/关注/云企库等页面)
--
-- 背景: V54/V58 已注册 420-429 企业档案菜单 (仅含企业列表/标签/绑定 3 个页面)
-- 本迁移补充其余 8 个页面 + 修复现有菜单 type 字段
--
-- 现有菜单 (V54/V58):
--   420 企业档案 (parent=0, type=1, sort=250, app_id=11, menu_category='admin')
--   421 企业列表 (parent=420, type=1 → 修复为 type=2)
--   422 企业标签 (parent=420, type=1 → 修复为 type=2)
--   423 企业绑定 (parent=420, type=1 → 修复为 type=2)
--   424 新增企业 (parent=421, type=3)  ...
--   429 新增绑定 (parent=423, type=3)
--
-- 新加菜单 (本迁移):
--   430-437 叶子菜单 (type=2)
--   438-459 按钮权限 (type=3)
--
-- 依赖: V58 (已注册 420-429), V1 (sys_menu/sys_role_menu)
-- =============================================================================

-- ========== 1. 修复现有菜单 type (type=1→2, 叶子菜单不应为目录) ==========
UPDATE `sys_menu` SET `type`=2 WHERE `id` IN (421, 422, 423) AND `type`=1;

-- ========== 2. 新增子菜单 (type=2, 叶子菜单, parent=420) ==========
INSERT IGNORE INTO `sys_menu`
    (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`,
     `status`, `deleted`, `app_id`, `create_time`, `update_time`, `menu_category`)
VALUES
    (430, 420, '行业类型',     '/enterprise/industry',        NULL, 2, 'fas fa-list-alt',   4,  'enterprise:industry:list', 1, 0, 11, NOW(), NOW(), 'admin'),
    (431, 420, '评分规则',     '/enterprise/rating',          NULL, 2, 'fas fa-star',       5,  'enterprise:rating:list',  1, 0, 11, NOW(), NOW(), 'admin'),
    (432, 420, '客户信息',     '/enterprise/customer',        NULL, 2, 'fas fa-user-tie',   6,  'enterprise:customer:list',1, 0, 11, NOW(), NOW(), 'admin'),
    (433, 420, '关注标签',     '/enterprise/focus',           NULL, 2, 'fas fa-bookmark',   7,  'enterprise:focus:list',   1, 0, 11, NOW(), NOW(), 'admin'),
    (434, 420, '云企库数据',   '/enterprise/cloud-data',      NULL, 2, 'fas fa-cloud',      8,  'enterprise:cloud:list',   1, 0, 11, NOW(), NOW(), 'admin'),
    (435, 420, '注册类型',     '/enterprise/reg-type',        NULL, 2, 'fas fa-folder-open',9,  'enterprise:regtype:list', 1, 0, 11, NOW(), NOW(), 'admin'),
    (436, 420, '国民经济分类', '/enterprise/national-economy',NULL, 2, 'fas fa-sitemap',   10,  'enterprise:economy:list', 1, 0, 11, NOW(), NOW(), 'admin'),
    (437, 420, '企业概览',     '/enterprise/overview',        NULL, 2, 'fas fa-chart-pie', 11,  'enterprise:overview:list',1, 0, 11, NOW(), NOW(), 'admin');

-- ========== 3. 按钮权限 (type=3) ==========
INSERT IGNORE INTO `sys_menu`
    (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`,
     `status`, `deleted`, `app_id`, `create_time`, `update_time`, `menu_category`)
VALUES
    -- 企业列表按钮
    (438, 421, '查看企业', '', '', 3, '', 1, 'enterprise:page:view',   1, 0, 11, NOW(), NOW(), 'admin'),
    (439, 421, '新增企业', '', '', 3, '', 2, 'enterprise:page:add',    1, 0, 11, NOW(), NOW(), 'admin'),
    (440, 421, '编辑企业', '', '', 3, '', 3, 'enterprise:page:edit',   1, 0, 11, NOW(), NOW(), 'admin'),
    (441, 421, '删除企业', '', '', 3, '', 4, 'enterprise:page:del',    1, 0, 11, NOW(), NOW(), 'admin'),
    -- 行业类型按钮
    (442, 430, '新增行业', '', '', 3, '', 1, 'enterprise:industry:add', 1, 0, 11, NOW(), NOW(), 'admin'),
    (443, 430, '编辑行业', '', '', 3, '', 2, 'enterprise:industry:edit',1, 0, 11, NOW(), NOW(), 'admin'),
    (444, 430, '删除行业', '', '', 3, '', 3, 'enterprise:industry:del', 1, 0, 11, NOW(), NOW(), 'admin'),
    -- 评分规则按钮
    (445, 431, '新增规则', '', '', 3, '', 1, 'enterprise:rating:add',  1, 0, 11, NOW(), NOW(), 'admin'),
    (446, 431, '编辑规则', '', '', 3, '', 2, 'enterprise:rating:edit', 1, 0, 11, NOW(), NOW(), 'admin'),
    (447, 431, '删除规则', '', '', 3, '', 3, 'enterprise:rating:del',  1, 0, 11, NOW(), NOW(), 'admin');

-- ========== 4. 授权 admin 角色获取新菜单 ==========
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
    SELECT 1, m.id FROM sys_menu m
    WHERE m.id BETWEEN 430 AND 447 AND m.deleted = 0
    ON DUPLICATE KEY UPDATE role_id = role_id;