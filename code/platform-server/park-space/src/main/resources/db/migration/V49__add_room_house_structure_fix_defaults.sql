-- ================================================================
-- V49: 房间表补充 house_structure 字段 + 调整默认值
--
-- 背景:
--   - 前端表单已有 houseStructure 控件（字典 room_structure），
--     但缺少对应 DB 列，数据被静默丢弃
--   - status DB 默认值为 1（已租），业务要求默认 0（空置）
--   - renting_selling 没有 DB 默认值，业务要求默认 2（可租售）
-- ================================================================

-- 1) 新增 house_structure 列（字典 room_structure）
ALTER TABLE `sys_room`
    ADD COLUMN `house_structure` tinyint DEFAULT NULL COMMENT '房屋结构 (字典 room_structure, 0=钢筋混凝土 1=钢结构 2=砖混 3=木结构 4=混合)' AFTER `room_type`;

-- 2) status 默认值改为 0（空置）
ALTER TABLE `sys_room`
    MODIFY COLUMN `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态: 0=空置 1=已租 2=已售 3=锁定 4=预订';

-- 3) renting_selling 默认值设为 2（可租售）
ALTER TABLE `sys_room`
    MODIFY COLUMN `renting_selling` tinyint DEFAULT '2' COMMENT '租售状态: 0=可租 1=可售 2=可租售 3=自用';

-- 4) 回填现有 NULL 记录
UPDATE `sys_room` SET `status` = 0 WHERE `status` IS NULL;
UPDATE `sys_room` SET `renting_selling` = 2 WHERE `renting_selling` IS NULL;

-- 5) 标记 Flyway
INSERT INTO flyway_schema_history
    (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (
    (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM (SELECT installed_rank FROM flyway_schema_history) AS t),
    '49', 'add room house_structure column, fix status/renting_selling defaults', 'SQL',
    'V49__add_room_house_structure_fix_defaults.sql',
    NULL, 'hugh', NOW(), 100, 1
)
ON DUPLICATE KEY UPDATE success = 1, execution_time = 100;

-- 6) 验证
SELECT 'room_total' AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0;
SELECT 'room_status_zero_count' AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0 AND status = 0;
SELECT 'room_renting_selling_two_count' AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0 AND renting_selling = 2;
