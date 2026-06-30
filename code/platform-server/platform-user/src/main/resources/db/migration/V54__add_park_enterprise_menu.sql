-- =============================================================================
-- V54: park-enterprise 菜单登记
--
-- 背景:
--   park-enterprise Phase 1 后端完成 (commit 8a1ac34/b6e9c94/616c438/06f0786/1996615)
--   3 个 Controller 已实现 (EnterpriseController, EnterpriseTagController, EnterpriseEntBindController)
--   V53 已建好 sys_enterprise / sys_enterprise_tag / sys_enterprise_ent_bind 表
--   但 admin 后台左侧菜单还没出现 "企业档案" — 不挂菜单用户进不去
--
-- ID 范围 220-229:
--   V41 用了 210 (kefu), V40 用了 70-77 (ops-admin), V36 用了 200-209 (park-space)
--   220-229 是空闲段, 避开历史占用
-- =============================================================================

-- 顶层目录 (parent_id=0, type=1 = directory)
-- path='/enterprise/' 与 V53 设计的 /enterprise/{page,tag,bind} 路由前缀一致
-- component='Layout' 表示目录 (Layout wrapper), parent uses .vue file
-- menu_category='admin' 让 ops-admin (8090) 不显示此菜单 — V40 隔离原则
-- perms='enterprise:menu' 作为前端菜单显示和按钮级入口标记
INSERT IGNORE INTO `sys_menu`
    (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`,
     `status`, `deleted`, `app_id`, `create_time`, `update_time`, `menu_category`)
VALUES
    (220, 0, '企业档案', '/enterprise/', 'Layout', 1, 'fas fa-building', 250, 'enterprise:menu',
     1, 0, NULL, NOW(), NOW(), 'admin');

-- 三个子菜单 (type=2 = menu item, parent_id=220)
-- 注意: Vue 前端页面待 Phase 1G 完成, 菜单登记先做让 sys_menu 表可访问
INSERT IGNORE INTO `sys_menu`
    (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`,
     `status`, `deleted`, `app_id`, `create_time`, `update_time`, `menu_category`)
VALUES
    -- 企业列表 (主入口 — 默认重定向到此页面)
    (221, 220, '企业列表', 'page', 'park/enterprise/Index',     2, 'fas fa-list',     1, 'enterprise:list',     1, 0, NULL, NOW(), NOW(), 'admin'),
    -- 企业标签 (字典下拉 + 标签管理)
    (222, 220, '企业标签', 'tag',  'park/enterprise/Tag',       2, 'fas fa-tags',     2, 'enterprise:tag:list',  1, 0, NULL, NOW(), NOW(), 'admin'),
    -- 企业绑定 (企业↔园区/楼栋 关系)
    (223, 220, '企业绑定', 'bind', 'park/enterprise/Bind',      2, 'fas fa-link',     3, 'enterprise:bind:list', 1, 0, NULL, NOW(), NOW(), 'admin');

-- 按钮级权限 (用于 @RequiresPermissions / 前端 button 隐藏)
-- 与 service 层 method 名称对齐 (Phase 1 实现: page/detail/create/update/delete + tag/bind CRUD)
INSERT IGNORE INTO `sys_menu`
    (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`,
     `status`, `deleted`, `app_id`, `create_time`, `update_time`, `menu_category`)
VALUES
    (224, 220, '新增企业', '',  '', 3, '', 10, 'enterprise:add',    1, 0, NULL, NOW(), NOW(), 'admin'),
    (225, 220, '编辑企业', '',  '', 3, '', 11, 'enterprise:edit',    1, 0, NULL, NOW(), NOW(), 'admin'),
    (226, 220, '删除企业', '',  '', 3, '', 12, 'enterprise:delete',  1, 0, NULL, NOW(), NOW(), 'admin'),
    (227, 220, '启停企业', '',  '', 3, '', 13, 'enterprise:status',  1, 0, NULL, NOW(), NOW(), 'admin'),
    (228, 220, '新增标签', '',  '', 3, '', 14, 'enterprise:tag:add',    1, 0, NULL, NOW(), NOW(), 'admin'),
    (229, 220, '新增绑定', '',  '', 3, '', 15, 'enterprise:bind:add',   1, 0, NULL, NOW(), NOW(), 'admin');

-- 角色 menu 授权 (admin 角色 role_id=1 自动获得 220-229 共 10 个菜单)
-- 留 V41 同款 INSERT IGNORE 模式, 幂等可重跑
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
    SELECT 1, m.id FROM sys_menu m
    WHERE m.id BETWEEN 220 AND 229 AND m.deleted = 0
    ON DUPLICATE KEY UPDATE role_id = role_id;

-- sanity check
SELECT 'park-enterprise menu count', COUNT(*) FROM sys_menu
    WHERE id BETWEEN 220 AND 229 AND deleted = 0;
-- 期望: 10
