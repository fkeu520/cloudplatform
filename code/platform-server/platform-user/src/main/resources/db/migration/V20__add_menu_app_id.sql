-- ================================================================
-- V20: 为 sys_menu 添加 app_id 字段，关联所属应用
-- 实现多租户场景下：管理平台只加载该租户已授权应用的菜单
-- ================================================================

-- 1. 添加 app_id 列（关联 sys_app.id）
ALTER TABLE sys_menu
    ADD COLUMN app_id bigint NULL COMMENT '所属应用ID' AFTER status;

-- 2. 为根级菜单设置 app_id
UPDATE sys_menu SET app_id = 1 WHERE id = 1;  -- 系统管理 → 系统管理(system)
UPDATE sys_menu SET app_id = 2 WHERE id = 14; -- 基础配置 → 用户中心(user-center)
UPDATE sys_menu SET app_id = 3 WHERE id = 26; -- 流程中心 → 流程中心(workflow)
UPDATE sys_menu SET app_id = 4 WHERE id = 48; -- 消息中心 → 消息中心(notification)

-- 3. 为第1级子菜单设置 app_id（根据父菜单继承）
UPDATE sys_menu SET app_id = 1 WHERE parent_id = 1;  -- 系统管理下所有子菜单
UPDATE sys_menu SET app_id = 2 WHERE parent_id = 14; -- 基础配置下所有子菜单
UPDATE sys_menu SET app_id = 3 WHERE parent_id = 26; -- 流程中心下所有子菜单
UPDATE sys_menu SET app_id = 4 WHERE parent_id = 48; -- 消息中心下所有子菜单

-- 4. 为第2级子菜单设置 app_id（根据父菜单继承）
-- 系统应用(1) 下子菜单的子菜单：用户管理(2)的按钮、组织管理(9)的按钮
UPDATE sys_menu SET app_id = 1 WHERE parent_id IN (2, 3, 4, 9, 25);
-- 用户中心(2) 下子菜单的子菜单：字典管理(15)的按钮、参数配置(20)的按钮
UPDATE sys_menu SET app_id = 2 WHERE parent_id IN (15, 20);
-- 流程中心(3) 下子菜单的子菜单：流程定义(27)的按钮、我的待办(32)的按钮、流程监控(35)的按钮
UPDATE sys_menu SET app_id = 3 WHERE parent_id IN (27, 32, 35);
-- 消息中心(4) 下子菜单的子菜单：消息列表(49)的按钮、渠道配置(52)的按钮、短信模板(56)的按钮、消息记录(60)的按钮
UPDATE sys_menu SET app_id = 4 WHERE parent_id IN (49, 52, 56, 60);

-- 5. 验证：检查还有没有 app_id 为空的菜单
-- SELECT id, name, parent_id FROM sys_menu WHERE app_id IS NULL AND deleted = 0;
