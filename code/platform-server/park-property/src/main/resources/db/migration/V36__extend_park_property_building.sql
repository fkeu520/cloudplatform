-- ================================================================
-- V36: park-property Building 字段扩展 (csyh 业务融合 Phase 1)
--
-- 背景: 按空间资产迁移方案对照当前代码差距分析, Building 实体需对齐 csyh std:
--   - 新增 building_code (楼栋编号, 园区内唯一) — 与 building_no 合并, 统一名称
--   - 新增 floor_number (地上层数), underground (地下层数)
--   - 新增 area_covered (建筑面积, 替换 total_area)
--   - 新增 property_right (产权性质: 字典), building_safety (建筑结构: 字典)
--   - 新增 share_area (公摊面积), lease_method (租赁方式: 字典)
--   - 新增 sorting (排序), certificate (产权证号), image (楼栋图片 JSON 数组)
--
-- 注意: building_no / total_area / build_year / manager / manager_phone / remark
--   保留 (用户额外需求, csyh 无但当前项目需要)
--
-- 路由: BuildingController 已存在 /building/page, 客户端需新增 /building/check-code, /building/import
-- 字典: V38 初始化 property_right / building_structure / lease_method
-- ================================================================

-- 1) sys_building 字段扩展
ALTER TABLE `sys_building`
    ADD COLUMN `building_code`    varchar(64)    DEFAULT NULL          COMMENT '楼栋编号 (园区内唯一, csyh 字段)',
    ADD COLUMN `floor_number`     int            NOT NULL DEFAULT '1'  COMMENT '地上层数 (csyh floorNumber)',
    ADD COLUMN `underground`      int            NOT NULL DEFAULT '0'  COMMENT '地下层数 (csyh underground)',
    ADD COLUMN `area_covered`     decimal(15,2)  DEFAULT NULL          COMMENT '建筑面积 (m², csyh areaCovered)',
    ADD COLUMN `property_right`   tinyint        DEFAULT NULL          COMMENT '产权性质 (字典: property_right, csyh propertyRigth)',
    ADD COLUMN `building_safety`  tinyint        DEFAULT NULL          COMMENT '建筑结构 (字典: building_structure, 国标 GB/T 50145)',
    ADD COLUMN `share_area`       decimal(15,2)  DEFAULT NULL          COMMENT '公摊面积 (m², must <= area_covered)',
    ADD COLUMN `lease_method`     tinyint        DEFAULT NULL          COMMENT '租赁方式 (字典: lease_method)',
    ADD COLUMN `sorting`          int            NOT NULL DEFAULT '0'  COMMENT '排序',
    ADD COLUMN `certificate`      varchar(100)   DEFAULT NULL          COMMENT '产权证号 (csyh certificate)',
    ADD COLUMN `image`            varchar(1000)  DEFAULT NULL          COMMENT '楼栋图片 (JSON 数组, 多图)';

-- 2) 唯一性索引: 楼栋编号 园区内唯一 (删除原 building_no 的索引如果存在)
ALTER TABLE `sys_building`
    ADD UNIQUE KEY `uk_park_building_code` (`park_id`, `building_code`, `deleted`);

-- 3) 数据迁移: 把现有 building_no 复制到 building_code (如果 building_code 为空)
UPDATE `sys_building`
SET `building_code` = `building_no`
WHERE `building_code` IS NULL AND `building_no` IS NOT NULL;

-- 4) 数据迁移: 把现有 total_area 复制到 area_covered
UPDATE `sys_building`
SET `area_covered` = `total_area`
WHERE `area_covered` IS NULL AND `total_area` IS NOT NULL;

-- 5) 验证
SELECT 'building_count' AS k, COUNT(*) AS v FROM sys_building WHERE deleted = 0;
SELECT 'building_with_code' AS k, COUNT(*) AS v FROM sys_building WHERE deleted = 0 AND building_code IS NOT NULL;
SELECT 'building_with_area' AS k, COUNT(*) AS v FROM sys_building WHERE deleted = 0 AND area_covered IS NOT NULL;
