-- ================================================================
-- V37: 空间管理菜单再调整 (空间资产 + 空间设置 二级分组)
--
-- 背景:
--   V36 把空间中心拆成 房源管理/空间设置/地块管理 三个一级, 但 210 空间设置
--   内容混杂 (房源用途/空间类别/公共空间/配套/能耗/设备), 跟 200 房源管理
--   边界不清。用户决策: 200 改名 空间资产, 收纳"强业务"二级; 210 保留
--   空间设置, 只放"配置/运维"类 (空间类别/能耗/设备)。
--
-- 200 空间资产 下 (13 项, 业务强相关):
--   sort=1  房源用途      (211 移过来, 来自 210)
--   sort=2  房间配套      (214 改名 + 移过来, 来自 210, 原名 配套设施)
--   sort=3  园区管理      (201 不动)
--   sort=4  分区管理      (202 不动)
--   sort=5  楼栋管理      (203 不动)
--   sort=6  楼层管理      (204 不动)
--   sort=7  房间管理      (205 不动)
--   sort=8  租售控制      (218 新增, 暂用 /room-control/page 占位 path, 后端待补)
--   sort=9  房源拆分合并  (209 改名, 来自 V36 的 "拆分合并")
--   sort=10 公共区域      (213 改名 + 移过来, 来自 210, 原名 公共空间)
--   sort=11 合同房间      (206 不动, V36 已删除但用户要求保留, 恢复)
--   sort=12 锁定记录      (207 恢复, V36 已删除)
--   sort=13 绑定记录      (208 恢复, V36 已删除)
--
-- 210 空间设置 下 (3 项, 配置/运维):
--   sort=1  空间类别      (212 不动)
--   sort=2  能耗管理      (215 恢复, V36 已删除)
--   sort=3  设备设施      (216 恢复, V36 已删除)
--
-- 220 地块管理 (3 项不变):
--   221 地块管理 / 222 规划用途 / 223 土地性质
--
-- 设计原则:
--   1. 用 UPDATE 替代 INSERT IGNORE, 避免同 ID 冲突被静默吞掉
--   2. 重新授权 role_id=1 收下整个新结构
--   3. 菜单元数据先就位, 后端 218 租售控制 后续补
--
-- 依赖: V36 (基础结构), V1 (sys_menu 表)
-- ================================================================

-- ========== 1. 200 改名 + 改 path/icon ==========
UPDATE `sys_menu`
   SET `name`='空间资产', `path`='/asset', `icon`='fas fa-cubes', `sort`=3
 WHERE `id`=200;

-- ========== 2. 211 房源用途 移到 200, sort=1 ==========
UPDATE `sys_menu`
   SET `parent_id`=200, `name`='房源用途', `path`='/room-purpose/page',
       `icon`='fas fa-notebook', `sort`=1, `perms`='system:room-purpose:list'
 WHERE `id`=211;

-- ========== 3. 214 改名为 房间配套, 移到 200, sort=2 ==========
UPDATE `sys_menu`
   SET `parent_id`=200, `name`='房间配套', `path`='/kit/page',
       `icon`='fas fa-box', `sort`=2, `perms`='system:kit:list'
 WHERE `id`=214;

-- ========== 4. 201-205 园区/分区/楼栋/楼层/房间管理 重排 sort ==========
UPDATE `sys_menu` SET `sort`=3  WHERE `id`=201;  -- 园区管理
UPDATE `sys_menu` SET `sort`=4  WHERE `id`=202;  -- 分区管理
UPDATE `sys_menu` SET `sort`=5  WHERE `id`=203;  -- 楼栋管理
UPDATE `sys_menu` SET `sort`=6  WHERE `id`=204;  -- 楼层管理
UPDATE `sys_menu` SET `sort`=7  WHERE `id`=205;  -- 房间管理

-- ========== 5. 218 新增 租售控制, sort=8 (path 占位, 后端待补) ==========
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`, `perms`, `status`)
VALUES (218, 200, '租售控制', '/room-control/page', NULL, 1, 'fas fa-key', 8, 'system:rent-control:list', 1)
ON DUPLICATE KEY UPDATE `name`='租售控制', `path`='/room-control/page',
                        `parent_id`=200, `icon`='fas fa-key', `sort`=8,
                        `perms`='system:rent-control:list';

-- ========== 6. 209 改名 房源拆分合并, sort=9 ==========
UPDATE `sys_menu`
   SET `name`='房源拆分合并', `sort`=9
 WHERE `id`=209;

-- ========== 7. 213 改名 公共区域, 移到 200, sort=10 ==========
UPDATE `sys_menu`
   SET `parent_id`=200, `name`='公共区域', `path`='/space/page',
       `icon`='fas fa-globe', `sort`=10, `perms`='system:space:list'
 WHERE `id`=213;

-- ========== 8. 206/207/208 合同房间/锁定记录/绑定记录 恢复在 200 下, sort=11-13 ==========
UPDATE `sys_menu` SET `parent_id`=200, `sort`=11, `status`=1
   WHERE `id`=206;  -- 合同房间
UPDATE `sys_menu` SET `parent_id`=200, `sort`=12, `status`=1
   WHERE `id`=207;  -- 锁定记录
UPDATE `sys_menu` SET `parent_id`=200, `sort`=13, `status`=1
   WHERE `id`=208;  -- 绑定记录

-- ========== 9. 210 空间设置 改回原 name, 子菜单只剩 212/215/216 ==========
UPDATE `sys_menu` SET `name`='空间设置' WHERE `id`=210;

-- 212 保持
UPDATE `sys_menu` SET `parent_id`=210, `sort`=1, `status`=1 WHERE `id`=212;
-- 215/216 恢复在 210 下
UPDATE `sys_menu` SET `parent_id`=210, `sort`=2, `status`=1 WHERE `id`=215;
UPDATE `sys_menu` SET `parent_id`=210, `sort`=3, `status`=1 WHERE `id`=216;

-- ========== 10. 220 地块管理 不动, 确保 3 个子菜单 sort 正确 ==========
-- (防御性, V36 已设, 这里再确认)
UPDATE `sys_menu` SET `parent_id`=220, `sort`=1, `status`=1 WHERE `id`=221;
UPDATE `sys_menu` SET `parent_id`=220, `sort`=2, `status`=1 WHERE `id`=222;
UPDATE `sys_menu` SET `parent_id`=220, `sort`=3, `status`=1 WHERE `id`=223;

-- ========== 11. 重新授权 role_id=1 ==========
-- 先清掉 V36 给的所有 200-230 授权, 然后按 parent_id 重新授
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 200 AND 230;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `id` IN (200, 210, 220);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `parent_id` IN (200, 210, 220) AND `status`=1;
-- 201 园区管理 下的 4 个按钮权限 (227-230)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `id` BETWEEN 227 AND 230;
