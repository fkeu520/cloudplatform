-- ================================================================
-- V45: 修复 V44 flyway_schema_history INSERT 报错 (MySQL 1093)
-- ================================================================
-- 背景:
--   V44 的第 6 步用子查询 SELECT MAX(installed_rank) FROM flyway_schema_history
--   来生成下一个 rank, 但 MySQL 不允许 INSERT 的子查询引用目标表本身
--   (ERROR 1093: You can't specify target table 'flyway_schema_history'
--                for update in FROM clause)
--
--   后果: 217 上 V44 跑完前 5 步 (列/索引已就位 + 数据回填), 第 6 步失败
--   导致 flyway_schema_history 没有 V44 记录
--
-- 修复:
--   - 用派生表 (子查询包一层 AS t) 绕开 MySQL 1093 限制
--   - 同步防御性重检 sys_room 的列/索引 (幂等, 不会破坏已就位的数据)
--   - 标记 V44 history success=1, 防止后续启用 Flyway 后重跑
-- ================================================================

-- 1) 防御性重检 floor_id 列 (幂等)
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_room'
      AND COLUMN_NAME = 'floor_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `floor_id` BIGINT DEFAULT NULL COMMENT ''楼层 ID (V38, csyh floorId 关联 sys_floor.id)'' AFTER `building_id`',
    'SELECT ''V45: floor_id exists'' AS msg'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 防御性重检 idx_floor_id
SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_room'
      AND INDEX_NAME = 'idx_floor_id'
);
SET @ddl = IF(@idx_exists = 0,
    'ALTER TABLE `sys_room` ADD KEY `idx_floor_id` (`floor_id`)',
    'SELECT ''V45: idx_floor_id exists'' AS msg'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) 防御性重检 area_id 列
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_room'
      AND COLUMN_NAME = 'area_id'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `area_id` BIGINT DEFAULT NULL COMMENT ''分区 ID (V41, 来源 building.area_id, 支持 4 级树过滤)'' AFTER `building_id`',
    'SELECT ''V45: area_id exists'' AS msg'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) 防御性重检 idx_room_area
SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_room'
      AND INDEX_NAME = 'idx_room_area'
);
SET @ddl = IF(@idx_exists = 0,
    'ALTER TABLE `sys_room` ADD KEY `idx_room_area` (`area_id`)',
    'SELECT ''V45: idx_room_area exists'' AS msg'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) 数据回填: area_id 同步 (幂等, 已回填的会被 WHERE 过滤)
UPDATE `sys_room` r
INNER JOIN `sys_building` b ON r.building_id = b.id AND b.deleted = 0
SET r.area_id = b.area_id
WHERE r.area_id IS NULL AND r.building_id IS NOT NULL;

-- 6) 标记 Flyway 历史: V44 (用派生表绕开 MySQL 1093)
-- 关键: 子查询包一层 AS t 派生表, MySQL 才允许引用
INSERT INTO flyway_schema_history
    (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT
    (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM (SELECT installed_rank FROM flyway_schema_history) AS t),
    '44', 'ensure room tree columns (floor_id, area_id)', 'SQL', 'V44__ensure_room_tree_columns.sql',
    NULL, 'hugh', NOW(), 100, 1
ON DUPLICATE KEY UPDATE success = 1, execution_time = 100;

-- 7) 标记本次 V45 历史
INSERT INTO flyway_schema_history
    (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT
    (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM (SELECT installed_rank FROM flyway_schema_history) AS t),
    '45', 'fix V44 flyway history MySQL 1093 + defensive recheck', 'SQL', 'V45__fix_v44_flyway_history.sql',
    NULL, 'hugh', NOW(), 100, 1
ON DUPLICATE KEY UPDATE success = 1, execution_time = 100;

-- 8) 验证
SELECT 'room_total' AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0;
SELECT 'room_with_floor_id' AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0 AND floor_id IS NOT NULL;
SELECT 'room_with_area_id'  AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0 AND area_id  IS NOT NULL;
SELECT 'V44 history' AS k, success FROM flyway_schema_history WHERE version = '44';
SELECT 'V45 history' AS k, success FROM flyway_schema_history WHERE version = '45';
