-- V41: sys_room 加 area_id 字段 (支持房间左侧树 4 层过滤筛选: 园区→分区→楼栋→楼层)
-- 2026-06-25: 房间左侧树扩展 Area 节点, 需要 areaId 直接过滤 (避免跨表 JOIN 性能损耗)
-- areaId 来源: buildingId -> sys_building.area_id (RoomService 自动同步)

ALTER TABLE sys_room
    ADD COLUMN `area_id` BIGINT NULL COMMENT '分区 ID (关联 sys_area.id, 来源 building.area_id)' AFTER `building_id`;

CREATE INDEX `idx_room_area` ON sys_room(`area_id`);

-- 数据回填: 从 sys_building.area_id 同步到 sys_room.area_id
UPDATE sys_room r
INNER JOIN sys_building b ON r.building_id = b.id AND b.deleted = 0
SET r.area_id = b.area_id
WHERE r.area_id IS NULL AND r.building_id IS NOT NULL;

-- 数据回填: 若 building 没有 areaId 但有 parkId, 用 null (前端按 park 过滤)
-- 不强制 areaId NOT NULL, 兼容旧数据 NULL 情况
