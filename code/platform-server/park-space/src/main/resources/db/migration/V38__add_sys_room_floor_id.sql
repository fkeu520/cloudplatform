-- ================================================================
-- V38: park-space sys_room 新增 floor_id (Phase 6 csyh 业务融合)
--
-- 背景: csyh std 中 Room 与 Floor 是 N:1 关系 (Room.floorId → Floor.id),
--   左侧导航 (园区 → 楼栋 → 楼层) 选中楼层后, 需要按 floor_id 过滤 Room 列表.
--   当前 sys_room 只有 floor (Integer 楼层号), 不足以支持按楼层实体过滤.
--
--   故新增 floor_id BIGINT 列 + 索引 (不强约束, 兼容历史 floor Integer 数据).
-- ================================================================

-- 1) 新增 floor_id 列 (BIGINT, NULL 允许)
ALTER TABLE `sys_room`
    ADD COLUMN `floor_id` BIGINT DEFAULT NULL COMMENT '楼层 ID (csyh floorId, sys_floor.id)' AFTER `building_id`;

-- 2) 索引 (非外键, 仅查询加速)
ALTER TABLE `sys_room`
    ADD KEY `idx_floor_id` (`floor_id`);

-- 3) 验证
SELECT 'room_total' AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0;
SELECT 'room_with_floor_id' AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0 AND floor_id IS NOT NULL;