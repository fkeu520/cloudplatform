-- ================================================================
-- V44: 防御性补齐 sys_room 的 floor_id + area_id 列
-- ================================================================
-- 背景:
--   - V38 (park-space) 加 floor_id BIGINT
--   - V41 (park-space) 加 area_id  BIGINT
--   - 两者都是房间 4 级树 (园区→分区→楼栋→楼层) 的关键字段
--
-- 现状问题:
--   - 217 上 Flyway 全局禁用, 手动 apply 容易遗漏
--   - scripts/diag/apply-v38-on-217.sh 只 apply 了 platform-user 的 V38
--     (fix_space_menu_visibility), 没 apply park-space 的 V38/V41
--   - 后果: RoomService.page() 生成 floor_id/area_id 过滤时, MySQL
--     报 "Unknown column 'floor_id' in 'where clause'"
--
-- 本次修复:
--   - 用 INFORMATION_SCHEMA 检查列是否存在, 不存在则 ADD COLUMN
--   - 用同样的方式检查索引
--   - 完全幂等, 可重复执行, 不会破坏现有数据
--   - 同步写入 flyway_schema_history, 防止后续启用 Flyway 后重跑
-- ================================================================

-- 1) 确保 floor_id 列存在
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_room'
      AND COLUMN_NAME = 'floor_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `floor_id` BIGINT DEFAULT NULL COMMENT ''楼层 ID (V38, csyh floorId 关联 sys_floor.id)'' AFTER `building_id`',
    'SELECT ''floor_id already exists'' AS msg'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 确保 idx_floor_id 索引存在
SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_room'
      AND INDEX_NAME = 'idx_floor_id'
);
SET @ddl = IF(@idx_exists = 0,
    'ALTER TABLE `sys_room` ADD KEY `idx_floor_id` (`floor_id`)',
    'SELECT ''idx_floor_id already exists'' AS msg'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) 确保 area_id 列存在
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_room'
      AND COLUMN_NAME = 'area_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `area_id` BIGINT DEFAULT NULL COMMENT ''分区 ID (V41, 来源 building.area_id, 支持 4 级树过滤)'' AFTER `building_id`',
    'SELECT ''area_id already exists'' AS msg'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) 确保 idx_room_area 索引存在
SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_room'
      AND INDEX_NAME = 'idx_room_area'
);
SET @ddl = IF(@idx_exists = 0,
    'ALTER TABLE `sys_room` ADD KEY `idx_room_area` (`area_id`)',
    'SELECT ''idx_room_area already exists'' AS msg'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5) 数据回填: building 有 area_id 的, 同步到 room.area_id (幂等)
UPDATE `sys_room` r
INNER JOIN `sys_building` b ON r.building_id = b.id AND b.deleted = 0
SET r.area_id = b.area_id
WHERE r.area_id IS NULL AND r.building_id IS NOT NULL;

-- 6) 标记 Flyway 成功 (防止后续启用 Flyway 后重跑)
INSERT INTO flyway_schema_history
    (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (
    (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history),
    '44', 'ensure room tree columns (floor_id, area_id)', 'SQL', 'V44__ensure_room_tree_columns.sql',
    NULL, 'hugh', NOW(), 100, 1
)
ON DUPLICATE KEY UPDATE success = 1, execution_time = 100;

-- 7) 验证
SELECT 'room_total' AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0;
SELECT 'room_with_floor_id' AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0 AND floor_id IS NOT NULL;
SELECT 'room_with_area_id'  AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0 AND area_id  IS NOT NULL;
