-- =============================================================================
-- V55: 注册知识库(kefu)为独立应用 + 完善菜单权限
--
-- 背景:
--   智能问答(知识库)是独立前后端应用，已在 ghcr.io 构建镜像并部署
--   需要注册为 sys_app 以便在管理后台顶部 tab 显示
--   同时完善菜单权限结构(子菜单+按钮权限)
--
-- 设计:
--   1. sys_app: id=7, app_code='knowledge', app_name='知识库'
--   2. sys_menu: 在 V41(id=210) 基础上补充子菜单+按钮权限
--     - 210 已存在: 智能问答(目录), 保持不动
--     - 新增子菜单 211-213: 对话/知识库/仪表盘
--     - 新增按钮权限 214-216: 知识库CRUD
--   3. 所有菜单关联 app_id=7
--   4. INSERT IGNORE 授权 admin 角色
-- =============================================================================

-- 1. 注册知识库应用
INSERT INTO `sys_app` (`id`, `app_name`, `app_code`, `app_icon`, `app_type`, `sort`, `status`, `remark`)
VALUES (7, '知识库', 'knowledge', 'fas fa-comment-dots', 1, 20, 1, '智能问答知识库(kefu) - 独立应用')
ON DUPLICATE KEY UPDATE `app_name` = VALUES(`app_name`), `sort` = VALUES(`sort`);

-- 2. 更新 V41 已存在的顶层菜单(210)，关联 app_id=7
-- 如果 210 已存在且 app_id 为 NULL，补充 app_id
UPDATE `sys_menu` SET `app_id` = 7 WHERE `id` = 210 AND `app_id` IS NULL;

-- 3. 新增子菜单(知识库使用独立前端, admin 内通过 iframe/导航访问)
INSERT IGNORE INTO `sys_menu`
    (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`,
     `status`, `deleted`, `app_id`, `create_time`, `update_time`)
VALUES
    (211, 210, '对话',   'chat',       '', 2, 'fas fa-comments',  1, 'kefu:chat',      1, 0, 7, NOW(), NOW()),
    (212, 210, '知识库', 'knowledge',  '', 2, 'fas fa-book',      2, 'kefu:knowledge', 1, 0, 7, NOW(), NOW()),
    (213, 210, '仪表盘', 'dashboard',  '', 2, 'fas fa-chart-bar', 3, 'kefu:dashboard', 1, 0, 7, NOW(), NOW());

-- 4. 新增按钮权限
INSERT IGNORE INTO `sys_menu`
    (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`,
     `status`, `deleted`, `app_id`, `create_time`, `update_time`)
VALUES
    (214, 212, '新增文档', '', '', 3, '', 1, 'kefu:knowledge:add',    1, 0, 7, NOW(), NOW()),
    (215, 212, '编辑文档', '', '', 3, '', 2, 'kefu:knowledge:edit',   1, 0, 7, NOW(), NOW()),
    (216, 212, '删除文档', '', '', 3, '', 3, 'kefu:knowledge:delete', 1, 0, 7, NOW(), NOW());

-- 5. 授权 admin 角色(id=1) 获得 210-216 共 7 个菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
    SELECT 1, m.id FROM sys_menu m
    WHERE m.id BETWEEN 210 AND 216 AND m.deleted = 0
    ON DUPLICATE KEY UPDATE role_id = role_id;

-- sanity check
SELECT 'kefu knowledge app menu count', COUNT(*) FROM sys_menu
    WHERE id BETWEEN 210 AND 216 AND deleted = 0;
-- 期望: 7
