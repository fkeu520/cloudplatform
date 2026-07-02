-- =============================================================================
-- V57: 注册企业档案菜单 (parent_id=520)
--
-- 企业档案作为一级菜单, 放在「流程中心」(sort=150) 和「智能问答」(sort=180) 之间
-- ID 使用 520 段 (V56 用了 510-516)
--
-- 依赖: V1 (sys_menu 表), V56 (kefu 菜单, 已使用 510-516)
-- =============================================================================

-- 企业档案根目录
INSERT IGNORE INTO `sys_menu`
    (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`, `status`,
     `app_id`, `menu_category`, `create_time`, `update_time`)
VALUES
    (520, 0, '企业档案', '/enterprise', NULL, 1, 'fas fa-building', 165, 'enterprise:menu', 1,
     1, 'admin', NOW(), NOW());

-- 企业档案子菜单 (type=2: 有 path 的叶子菜单)
INSERT IGNORE INTO `sys_menu`
    (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`, `status`,
     `app_id`, `menu_category`, `create_time`, `update_time`)
VALUES
    (521, 520, '企业列表',       '/enterprise/page',         NULL, 2, 'fas fa-list',        1,  'enterprise:page:list',    1, 1, 'admin', NOW(), NOW()),
    (522, 520, '企业标签',       '/enterprise/tag',          NULL, 2, 'fas fa-tags',        2,  'enterprise:tag:list',     1, 1, 'admin', NOW(), NOW()),
    (523, 520, '企业绑定',       '/enterprise/bind',         NULL, 2, 'fas fa-link',        3,  'enterprise:bind:list',    1, 1, 'admin', NOW(), NOW()),
    (524, 520, '行业类型',       '/enterprise/industry',     NULL, 2, 'fas fa-list-alt',    4,  'enterprise:industry:list',1, 1, 'admin', NOW(), NOW()),
    (525, 520, '评分规则',       '/enterprise/rating',       NULL, 2, 'fas fa-star',        5,  'enterprise:rating:list',  1, 1, 'admin', NOW(), NOW()),
    (526, 520, '客户信息',       '/enterprise/customer',     NULL, 2, 'fas fa-user-tie',    6,  'enterprise:customer:list',1, 1, 'admin', NOW(), NOW()),
    (527, 520, '关注标签',       '/enterprise/focus',        NULL, 2, 'fas fa-bookmark',    7,  'enterprise:focus:list',   1, 1, 'admin', NOW(), NOW()),
    (528, 520, '云企库数据',     '/enterprise/cloud-data',   NULL, 2, 'fas fa-cloud',       8,  'enterprise:cloud:list',   1, 1, 'admin', NOW(), NOW()),
    (529, 520, '注册类型',       '/enterprise/reg-type',     NULL, 2, 'fas fa-folder-open', 9,  'enterprise:regtype:list', 1, 1, 'admin', NOW(), NOW()),
    (530, 520, '国民经济分类',   '/enterprise/national-economy', NULL, 2, 'fas fa-sitemap',  10, 'enterprise:economy:list', 1, 1, 'admin', NOW(), NOW()),
    (531, 520, '企业概览',       '/enterprise/overview',     NULL, 2, 'fas fa-chart-pie',   11, 'enterprise:overview:list',1, 1, 'admin', NOW(), NOW());

-- 企业列表按钮权限 (type=3: 按钮, 无 path)
INSERT IGNORE INTO `sys_menu`
    (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`, `status`,
     `app_id`, `menu_category`, `create_time`, `update_time`)
VALUES
    (532, 521, '查看企业', '', '', 3, '', 1, 'enterprise:page:view',   1, 1, 'admin', NOW(), NOW()),
    (533, 521, '新增企业', '', '', 3, '', 2, 'enterprise:page:add',    1, 1, 'admin', NOW(), NOW()),
    (534, 521, '编辑企业', '', '', 3, '', 3, 'enterprise:page:edit',   1, 1, 'admin', NOW(), NOW()),
    (535, 521, '删除企业', '', '', 3, '', 4, 'enterprise:page:del',    1, 1, 'admin', NOW(), NOW());

-- 授权 admin 角色 (role_id=1) 获得 520-535 共 16 个菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
    SELECT 1, m.id FROM sys_menu m
    WHERE m.id BETWEEN 520 AND 535 AND m.deleted = 0
    ON DUPLICATE KEY UPDATE role_id = role_id;