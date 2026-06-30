-- =============================================================================
-- V56: 注册 kefu 智能问答菜单 (parent_id=510)
-- 旧 210-216 (V41 用了被 park-space 占用的 ID) 改用 510-516 (500 段空闲)
-- V55 已成功跑过 (checksum -27312989) 注册了 sys_app id=7, 但 sys_menu 210-216 被 IGNORE 跳过
-- 此次 V56 用全新 ID 重新注册 sys_menu
-- =============================================================================

-- 智能问答根目录
INSERT IGNORE INTO `sys_menu`
    (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`, `status`,
     `app_id`, `menu_category`, `create_time`, `update_time`)
VALUES
    (510, 0, '智能问答', '/kefu', 'Layout', 1, 'fas fa-comment-dots', 180, 'kefu:menu', 1,
     7, 'admin', NOW(), NOW());

-- kefu 子菜单: 对话 / 知识库 / 仪表盘 (parent_id=510, type=2)
-- 使用完整路径 (与现有菜单约定一致, 如 /system/user)
INSERT IGNORE INTO `sys_menu`
    (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`, `status`,
     `app_id`, `menu_category`, `create_time`, `update_time`)
VALUES
    (511, 510, '对话',    '/kefu/chat',      'kefu/Chat',      2, 'fas fa-comments',  1, 'kefu:chat',      1, 7, 'admin', NOW(), NOW()),
    (512, 510, '知识库',  '/kefu/knowledge', 'kefu/Knowledge', 2, 'fas fa-book',      2, 'kefu:knowledge', 1, 7, 'admin', NOW(), NOW()),
    (513, 510, '仪表盘',  '/kefu/dashboard', 'kefu/Dashboard', 2, 'fas fa-chart-bar', 3, 'kefu:dashboard', 1, 7, 'admin', NOW(), NOW());

-- kefu 知识库按钮权限 (parent_id=512, type=3)
INSERT IGNORE INTO `sys_menu`
    (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`, `status`,
     `app_id`, `menu_category`, `create_time`, `update_time`)
VALUES
    (514, 512, '新增文档', '', '', 3, '', 1, 'kefu:knowledge:add',    1, 7, 'admin', NOW(), NOW()),
    (515, 512, '编辑文档', '', '', 3, '', 2, 'kefu:knowledge:edit',   1, 7, 'admin', NOW(), NOW()),
    (516, 512, '删除文档', '', '', 3, '', 3, 'kefu:knowledge:delete', 1, 7, 'admin', NOW(), NOW());

-- 授权 admin 角色 (role_id=1) 获得 510-516 共 7 个菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
    SELECT 1, m.id FROM sys_menu m
    WHERE m.id BETWEEN 510 AND 516 AND m.deleted = 0
    ON DUPLICATE KEY UPDATE role_id = role_id;
