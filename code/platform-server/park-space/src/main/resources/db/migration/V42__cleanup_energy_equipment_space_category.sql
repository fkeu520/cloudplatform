-- ================================================================
-- V42: park-space 清理 (P0 #4+#5 后续 + 通用化)
--
-- 背景:
--   1) 能耗管理 (Energy) 功能已下线 -> 删除 sys_energy 表 + 菜单 215
--   2) 设备设施 (Equipment) 作为独立菜单下线 (但房间配套 sub-table 仍需),
--      仅移除菜单 entry 216. 后端 EquipmentController/listByKit/batchSave 保留.
--   3) 空间类别 (SpaceCategory) 通用化 (V39 Kit/RoomPurpose 同模式),
--      移除 park_id, 唯一性按 (type_name, deleted) 保证.
--
-- 影响: 房间配套 (kit) 弹窗内的设备子表功能不受影响 (EquipmentController 保留).
-- ================================================================

-- 1) 删除 sys_energy 表 (V33 创建, 现已下线)
DROP TABLE IF EXISTS `sys_energy`;

-- 2) 删除菜单: 能耗管理 (215) + 设备设施 (216) - 仅菜单, 设备 sub-table 继续可用
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (215, 216);
DELETE FROM `sys_menu`        WHERE `id`      IN (215, 216);

-- 3) 空间类别移除 park_id
--    旧唯一键 (park_id, type_name, deleted) -> 新唯一键 (type_name, deleted)
ALTER TABLE `sys_space_category`
    ADD UNIQUE KEY `uk_type_name` (`type_name`, `deleted`);

ALTER TABLE `sys_space_category`
    DROP INDEX `uk_park_type_name`;

ALTER TABLE `sys_space_category`
    DROP COLUMN `park_id`;

-- 4) 验证
SELECT 'energy_table_exists'       AS k, COUNT(*) AS v FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'sys_energy';
SELECT 'energy_menu_removed'      AS k, COUNT(*) AS v FROM sys_menu WHERE id = 215;
SELECT 'equipment_menu_removed'    AS k, COUNT(*) AS v FROM sys_menu WHERE id = 216;
SELECT 'space_category_has_park_id' AS k, COUNT(*) AS v FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_space_category' AND column_name = 'park_id';
