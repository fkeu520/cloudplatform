-- V36: 房源管理菜单重组
-- 将原「空间中心」菜单拆分为三大组：房源管理、空间设置、地块管理
-- 原空间中心下所有菜单移动到对应新组下
-- 依赖: V1 (sys_menu 表), V32 (园区管理 69-73)

-- ========== 1. 房源管理（原空间中心改名）==========
-- 先更新原空间中心菜单名称为「房源管理」
-- 注意：原 space 相关菜单 id 从 44-63 由 workflow 使用，W3 菜单 id 从 80 开始

-- 找出当前已有的 W3 子菜单 parent_id
-- 先创建三个父级菜单
-- 注意: 200/201 已被 V29 (物业/楼宇) 占用, 先用 UPDATE 改名/挪 path, 再 INSERT 新菜单
UPDATE `sys_menu` SET `name`='房源管理', `path`='/property', `icon`='fas fa-door-open', `sort`=3
    WHERE `id`=200 AND `name`='物业管理';
UPDATE `sys_menu` SET `name`='园区管理', `path`='/system/park', `component`='system/park/index', `icon`='fas fa-building', `sort`=1
    WHERE `id`=201 AND `name`='楼宇列表';
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`, `status`) VALUES
-- 空间设置
(210, 0, '空间设置', '/space-setting', NULL, 0, 'fas fa-compass', 4, NULL, 1),
-- 地块管理
(220, 0, '地块管理', '/land', NULL, 0, 'fas fa-map', 5, NULL, 1);

-- ========== 2. 房源管理子菜单 ==========
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`, `status`) VALUES
-- 园区管理子权限
(227, 201, '查看', NULL, NULL, 2, NULL, 0, 'system:park:view', 1),
(228, 201, '新增', NULL, NULL, 2, NULL, 0, 'system:park:add', 1),
(229, 201, '编辑', NULL, NULL, 2, NULL, 0, 'system:park:edit', 1),
(230, 201, '删除', NULL, NULL, 2, NULL, 0, 'system:park:del', 1),

-- 分区管理 (原名区域管理)
(202, 200, '分区管理', '/area/page', NULL, 1, 'fas fa-location', 2, 'system:area:list', 1),
-- 楼栋管理
(203, 200, '楼栋管理', '/building/page', NULL, 1, 'fas fa-warehouse', 3, 'system:building:list', 1),
-- 楼层管理
(204, 200, '楼层管理', '/floor/page', NULL, 1, 'fas fa-layer-group', 4, 'system:floor:list', 1),
-- 房间管理 (原名房源管理)
(205, 200, '房间管理', '/room/page', NULL, 1, 'fas fa-door-closed', 5, 'system:room:list', 1),
-- 合同房间
(206, 200, '合同房间', '/covenant/page', NULL, 1, 'fas fa-file-contract', 6, 'system:covenant:list', 1),
-- 锁定记录
(207, 200, '锁定记录', '/room-lock-record/page', NULL, 1, 'fas fa-lock', 7, 'system:room-lock-record:list', 1),
-- 绑定记录
(208, 200, '绑定记录', '/room-record/page', NULL, 1, 'fas fa-plug', 8, 'system:room-record:list', 1),
-- 拆分合并
(209, 200, '拆分合并', '/room-split-merge/page', NULL, 1, 'fas fa-files', 9, 'system:room-split-merge:list', 1);

-- ========== 3. 空间设置子菜单 ==========
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`, `status`) VALUES
-- 房源用途
(211, 210, '房源用途', '/room-purpose/page', NULL, 1, 'fas fa-notebook', 1, 'system:room-purpose:list', 1),
-- 空间类别
(212, 210, '空间类别', '/space-category/page', NULL, 1, 'fas fa-th-large', 2, 'system:space-category:list', 1),
-- 公共空间 (原名空间管理)
(213, 210, '公共空间', '/space/page', NULL, 1, 'fas fa-globe', 3, 'system:space:list', 1),
-- 配套设施 (原名配套管理)
(214, 210, '配套设施', '/kit/page', NULL, 1, 'fas fa-box', 4, 'system:kit:list', 1),
-- 能耗管理
(215, 210, '能耗管理', '/energy/page', NULL, 1, 'fas fa-bolt', 5, 'system:energy:list', 1),
-- 设备设施
(216, 210, '设备设施', '/equipment/page', NULL, 1, 'fas fa-tools', 6, 'system:equipment:list', 1);

-- ========== 4. 地块管理子菜单 ==========
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`, `status`) VALUES
-- 地块管理
(221, 220, '地块管理', '/massif/page', NULL, 1, 'fas fa-mountain', 1, 'system:massif:list', 1),
-- 规划用途
(222, 220, '规划用途', '/plan-use/page', NULL, 1, 'fas fa-aim', 2, 'system:plan-use:list', 1),
-- 土地性质
(223, 220, '土地性质', '/land-nature/page', NULL, 1, 'fas fa-leaf', 3, 'system:land-nature:list', 1);

-- ========== 5. 删除旧的系统管理下的园区管理菜单 (69-73) ==========
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 69 AND 73;
DELETE FROM `sys_menu` WHERE `id` BETWEEN 69 AND 73;

-- ========== 6. 删除旧的空间中心菜单及子菜单（W3 阶段注册的旧菜单）==========
-- 旧菜单 parent 为 space 的路由：先查旧 parent ID 再删
-- 旧的 W3 space 菜单 id 范围：根据之前的路由注册，parent 为 'space' 的菜单
-- 注意: MySQL 不允许在 DELETE/UPDATE 的子查询中直接引用同表, 必须用派生表包一层
-- 删除旧的角色菜单关联
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (
    SELECT `id` FROM (
        SELECT `id` FROM `sys_menu` WHERE `parent_id` IN (
            SELECT `id` FROM (SELECT `id` FROM `sys_menu` WHERE `name` = '空间中心') AS _space_parent
        )
    ) AS _space_children
);
DELETE FROM `sys_menu` WHERE `parent_id` IN (
    SELECT `id` FROM (SELECT `id` FROM `sys_menu` WHERE `name` = '空间中心') AS _space_parent
);
DELETE FROM `sys_menu` WHERE `name` = '空间中心';

-- ========== 7. 授权新菜单给超级管理员 ==========
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `id` BETWEEN 200 AND 230;
