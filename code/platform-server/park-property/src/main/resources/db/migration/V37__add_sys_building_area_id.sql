-- ================================================================
-- V37: sys_building 加 area_id 字段 (支持 4 实体联动: 园区→分区→楼栋→楼层)
--
-- 背景:
--   - BuildingService.page (park-property) 2026-06-25 commit 7190bfc 新增 areaId 过滤,
--     用于 4 级树 (园区→分区→楼栋→楼层) 级联下拉筛选
--   - Building 实体已有 areaId 字段 (commit 7190bfc)
--   - park-space V41 已加 sys_room.area_id, 此次补 park-property V37 加 sys_building.area_id
--   - 修复 park-property building/page 500: `Unknown column 'area_id' in 'field list'`
--
-- 数据回填策略:
--   - 现有 building 数据无 area 关联 (V36 仅补字段未建关联), 设为 NULL
--   - 用户后续编辑楼栋时通过前端 "所属区域" el-select 显式绑定
--   - 不强制 NOT NULL, 兼容旧数据 NULL 情况 (与 sys_room.area_id 一致)
-- ================================================================

-- 1) sys_building 加 area_id 列
ALTER TABLE `sys_building`
    ADD COLUMN `area_id` bigint DEFAULT NULL COMMENT '区域 ID (关联 sys_area.id, 用于 4 级树级联过滤)' AFTER `park_id`;

-- 2) 索引 (支持 WHERE area_id = ? 高效过滤)
CREATE INDEX `idx_building_area` ON sys_building(`area_id`);

-- 3) 验证
SELECT 'building_count'        AS k, COUNT(*)                          AS v FROM sys_building WHERE deleted = 0;
SELECT 'building_with_area'    AS k, COUNT(*)                          AS v FROM sys_building WHERE deleted = 0 AND area_id IS NOT NULL;
SELECT 'building_area_id_null' AS k, COUNT(*)                          AS v FROM sys_building WHERE deleted = 0 AND area_id IS NULL;
SELECT 'building_col_check'    AS k, IFNULL(
    (SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_building' AND COLUMN_NAME = 'area_id'),
    'MISSING'
) AS v;