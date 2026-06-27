-- ================================================================
-- V51: park-space sys_room_split_merge ID 列类型修正 (bug 修复)
--
-- 背景:
--   sys_room_split_merge.old_room_id / new_room_id 在 V35 DDL (line 85-88)
--   被定义为 bigint, 但 RoomSplitMerge.java 实体 (line 30, 35) 改为 String
--   (用于存储逗号分隔的多房间 ID, 支持拆分场景).
--
--   拆分操作 (RoomSplitMergeService.split 第 220 行) 实际传 "200,201"
--   这种逗号分隔字符串到 MyBatis, MySQL 试图写入 bigint 列时失败:
--     DataIntegrityViolationException: Data truncated for column 'new_room_id'
--
--   合并操作 (第 131 行) 单 ID 通过字符串隐式转换暂未触发,
--   但仍是隐式转换, 不规范.
--
-- 修复:
--   列类型从 bigint 改为 varchar(500), 与实体 String 字段对齐,
--   支持逗号分隔的多 ID. mapper 已有 FIND_IN_SET 查询无需改动.
--
-- 范围: 仅 park-space 模块的 sys_room_split_merge 表.
-- V36-V50 期间任何迁移都没改过这个表, 所以列类型与 V35 一致.
-- ================================================================

-- 1) old_room_id: bigint -> varchar(500)
ALTER TABLE `sys_room_split_merge`
    MODIFY COLUMN `old_room_id` varchar(500) DEFAULT NULL
    COMMENT '原房源 ID (逗号分隔, 支持多个)';

-- 2) new_room_id: bigint -> varchar(500)
ALTER TABLE `sys_room_split_merge`
    MODIFY COLUMN `new_room_id` varchar(500) DEFAULT NULL
    COMMENT '新房源 ID (逗号分隔, 支持多个)';

-- 3) 验证列类型已变更
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'sys_room_split_merge'
  AND column_name IN ('old_room_id', 'new_room_id')
ORDER BY column_name;

-- 4) 验证现有数据无截断 (历史数据若是大数字 ID 也兼容 varchar 存储)
SELECT 'row_count' AS k, COUNT(*) AS v FROM sys_room_split_merge WHERE deleted = 0;
