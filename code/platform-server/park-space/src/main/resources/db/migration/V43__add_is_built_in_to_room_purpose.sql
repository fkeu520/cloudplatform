-- V43: park-space sys_room_purpose 新增固化标识 is_built_in (2026-06-25)
-- 背景: 用户要求将系统级用途（自用/出租/出售）固化，不可编辑删除。
--       用户后续新增的用途可编辑删除。
-- 改动:
--   1) sys_room_purpose 新增 TINYINT `is_built_in` DEFAULT 1 (默认用户新增)
--   2) 种子数据 3 行标记为固化 (is_built_in=0)
--   3) 新增第 4 个固化用途

-- 1) 新增列
ALTER TABLE `sys_room_purpose`
ADD COLUMN `is_built_in` tinyint(1) NOT NULL DEFAULT 1
COMMENT '固化标识: 0=系统固化不可变 1=用户新增可编辑删除'
AFTER `purpose_name`;

-- 2) 现有种子数据标记为固化 (自用/出租/出售, 固定 ID 段)
UPDATE `sys_room_purpose` SET `is_built_in` = 0
WHERE `id` IN (1900000000200000001, 1900000000200000002, 1900000000200000003);

-- 3) 验证
SELECT id, purpose_name, is_built_in, status FROM sys_room_purpose WHERE deleted = 0 ORDER BY id;
