-- ================================================================
-- V46: 防御性补齐 sys_room 缺失字段 (V36 + V40 全集)
-- ================================================================
-- 背景:
--   - V36 将 area → area_covered, 并新增 10 字段
--   - V40 新增 renting_selling / lease_price / sale_price / is_lock / is_order
--   - 217 上 Flyway 全局禁用, 所有未 apply
--   - 后果: MyBatis-Plus SELECT * 时报 "Unknown column" 错误
--
-- 本次修复:
--   - 用 INFORMATION_SCHEMA 检查每列是否存在, 不存在则 ADD
--   - area → area_covered 需特殊处理: 旧列存在则 RENAME, 都不存在则 ADD
--   - 完全幂等, 可重复执行
-- ================================================================

-- ==================== 1) area → area_covered ====================
SET @col_ac = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND COLUMN_NAME = 'area_covered');
SET @col_old = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND COLUMN_NAME = 'area');

SET @ddl = IF(@col_ac = 0 AND @col_old > 0,
    'ALTER TABLE `sys_room` CHANGE COLUMN `area` `area_covered` decimal(15,2) DEFAULT NULL COMMENT ''建筑面积 (m², csyh 字段)''',
    IF(@col_ac = 0 AND @col_old = 0,
        'ALTER TABLE `sys_room` ADD COLUMN `area_covered` decimal(15,2) DEFAULT NULL COMMENT ''建筑面积 (m², csyh 字段)''',
        'SELECT ''area_covered already exists'' AS msg'
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==================== 2-11) V36 新增列 ====================
-- room_name
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND COLUMN_NAME = 'room_name');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `room_name` varchar(64) DEFAULT NULL COMMENT ''房间名称 (csyh roomName)''',
    'SELECT ''room_name already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- build_area
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND COLUMN_NAME = 'build_area');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `build_area` decimal(15,2) DEFAULT NULL COMMENT ''套内面积 (m²)''',
    'SELECT ''build_area already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- billable_area
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND COLUMN_NAME = 'billable_area');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `billable_area` decimal(15,2) DEFAULT NULL COMMENT ''计费面积 (m²)''',
    'SELECT ''billable_area already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- unit_price
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND COLUMN_NAME = 'unit_price');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `unit_price` decimal(15,2) DEFAULT NULL COMMENT ''单价 (元/m²/月)''',
    'SELECT ''unit_price already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- total_price
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND COLUMN_NAME = 'total_price');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `total_price` decimal(15,2) DEFAULT NULL COMMENT ''总价 (元/月)''',
    'SELECT ''total_price already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- kit_id
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND COLUMN_NAME = 'kit_id');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `kit_id` bigint DEFAULT NULL COMMENT ''关联 sys_kit.id (房间配套)''',
    'SELECT ''kit_id already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- purpose_id
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND COLUMN_NAME = 'purpose_id');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `purpose_id` bigint DEFAULT NULL COMMENT ''关联 sys_room_purpose.id (房间用途)''',
    'SELECT ''purpose_id already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- image
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND COLUMN_NAME = 'image');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `image` varchar(1000) DEFAULT NULL COMMENT ''房间图片 (JSON 数组)''',
    'SELECT ''image already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sorting
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND COLUMN_NAME = 'sorting');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `sorting` int NOT NULL DEFAULT ''0'' COMMENT ''排序''',
    'SELECT ''sorting already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- introduce
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND COLUMN_NAME = 'introduce');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `introduce` text DEFAULT NULL COMMENT ''房间介绍''',
    'SELECT ''introduce already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ==================== 12-16) V40 新增列 ====================
-- renting_selling
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND COLUMN_NAME = 'renting_selling');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `renting_selling` tinyint(1) DEFAULT NULL COMMENT ''租售状态 (0=出租, 1=出售, 2=租售均可)''',
    'SELECT ''renting_selling already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- lease_price
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND COLUMN_NAME = 'lease_price');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `lease_price` decimal(15,2) DEFAULT NULL COMMENT ''出租底价 (元/m²/月)''',
    'SELECT ''lease_price already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sale_price
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND COLUMN_NAME = 'sale_price');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `sale_price` decimal(15,2) DEFAULT NULL COMMENT ''出售总价 (元)''',
    'SELECT ''sale_price already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- is_lock
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND COLUMN_NAME = 'is_lock');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `is_lock` tinyint(1) DEFAULT ''0'' COMMENT ''是否锁定 (0=正常, 1=锁定)''',
    'SELECT ''is_lock already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- is_order
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND COLUMN_NAME = 'is_order');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `sys_room` ADD COLUMN `is_order` tinyint(1) DEFAULT ''0'' COMMENT ''是否已定 (0=未定, 1=已定)''',
    'SELECT ''is_order already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ==================== 17-20) 索引 ====================
-- uk_park_building_room_no
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND INDEX_NAME = 'uk_park_building_room_no');
SET @ddl = IF(@idx_exists = 0,
    'ALTER TABLE `sys_room` ADD UNIQUE KEY `uk_park_building_room_no` (`park_id`, `building_id`, `room_no`, `deleted`)',
    'SELECT ''uk_park_building_room_no already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- idx_kit_id
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND INDEX_NAME = 'idx_kit_id');
SET @ddl = IF(@idx_exists = 0,
    'ALTER TABLE `sys_room` ADD KEY `idx_kit_id` (`kit_id`)',
    'SELECT ''idx_kit_id already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- idx_purpose_id
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND INDEX_NAME = 'idx_purpose_id');
SET @ddl = IF(@idx_exists = 0,
    'ALTER TABLE `sys_room` ADD KEY `idx_purpose_id` (`purpose_id`)',
    'SELECT ''idx_purpose_id already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- idx_status (V36 加的, 但可能已存在)
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_room' AND INDEX_NAME = 'idx_status');
SET @ddl = IF(@idx_exists = 0,
    'ALTER TABLE `sys_room` ADD KEY `idx_status` (`status`)',
    'SELECT ''idx_status already exists'' AS msg');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ==================== 21) 标记 Flyway 成功 ====================
INSERT INTO flyway_schema_history
    (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (
    (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM (SELECT installed_rank FROM flyway_schema_history) AS t),
    '46', 'ensure room V36+V40 columns (area_covered, renting_selling, etc.)', 'SQL',
    'V46__ensure_room_v36_columns.sql',
    NULL, 'hugh', NOW(), 100, 1
)
ON DUPLICATE KEY UPDATE success = 1, execution_time = 100;

-- ==================== 22) 验证 ====================
SELECT 'room_count' AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0;
SELECT 'room_with_area_covered' AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0 AND area_covered IS NOT NULL;
SELECT 'room_with_build_area' AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0 AND build_area IS NOT NULL;
