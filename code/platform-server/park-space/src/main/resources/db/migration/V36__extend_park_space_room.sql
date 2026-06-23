-- ================================================================
-- V37: park-space Room 字段扩展 (csyh 业务融合 Phase 1)
--
-- 背景: 按空间资产迁移方案对照当前代码差距分析, Room 实体需对齐 csyh std:
--   - 重命名 area → area_covered (建筑面积, csyh 字段命名)
--   - 新增 room_name (房间名称), build_area (套内面积), billable_area (计费面积)
--   - 新增 unit_price (单价), total_price (总价, 衍生)
--   - 新增 kit_id (关联 sys_kit.id 房间配套), purpose_id (关联 sys_room_purpose.id 房间用途)
--   - 新增 image (房间图片 JSON), sorting (排序), introduce (房间介绍)
--
-- 路由: RoomController 已存在 /room/page, 客户端需新增 /room/check-no, /room/import
-- ================================================================

-- 1) sys_room 重命名 area → area_covered
ALTER TABLE `sys_room` CHANGE COLUMN `area` `area_covered` decimal(15,2) DEFAULT NULL COMMENT '建筑面积 (m², csyh 字段)';

-- 2) sys_room 字段扩展
ALTER TABLE `sys_room`
    ADD COLUMN `room_name`       varchar(64)    DEFAULT NULL          COMMENT '房间名称 (csyh roomName)',
    ADD COLUMN `build_area`      decimal(15,2)  DEFAULT NULL          COMMENT '套内面积 (m², must <= area_covered, csyh buildArea)',
    ADD COLUMN `billable_area`   decimal(15,2)  DEFAULT NULL          COMMENT '计费面积 (m², csyh billableArea)',
    ADD COLUMN `unit_price`      decimal(15,2)  DEFAULT NULL          COMMENT '单价 (元/m²/月, csyh unitPrice)',
    ADD COLUMN `total_price`     decimal(15,2)  DEFAULT NULL          COMMENT '总价 (元/月, csyh totalPrice, 衍生自 unit_price * billable_area)',
    ADD COLUMN `kit_id`          bigint         DEFAULT NULL          COMMENT '关联 sys_kit.id (房间配套, csyh kitId)',
    ADD COLUMN `purpose_id`      bigint         DEFAULT NULL          COMMENT '关联 sys_room_purpose.id (房间用途, csyh purposeId)',
    ADD COLUMN `image`           varchar(1000)  DEFAULT NULL          COMMENT '房间图片 (JSON 数组, csyh image)',
    ADD COLUMN `sorting`         int            NOT NULL DEFAULT '0'  COMMENT '排序 (csyh sorting)',
    ADD COLUMN `introduce`       text           DEFAULT NULL          COMMENT '房间介绍 (csyh introduce)';

-- 3) 唯一性索引: 房号 园区内唯一
ALTER TABLE `sys_room`
    ADD UNIQUE KEY `uk_park_building_room_no` (`park_id`, `building_id`, `room_no`, `deleted`);

-- 4) 外键索引: 提升查询性能
ALTER TABLE `sys_room`
    ADD KEY `idx_kit_id` (`kit_id`),
    ADD KEY `idx_purpose_id` (`purpose_id`),
    ADD KEY `idx_status` (`status`);

-- 5) 验证
SELECT 'room_count' AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0;
SELECT 'room_with_area_covered' AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0 AND area_covered IS NOT NULL;
SELECT 'room_with_build_area' AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0 AND build_area IS NOT NULL;
