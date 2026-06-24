-- ================================================================
-- V39: park-space 移除 Kit / RoomPurpose 的 park_id 字段 (P0 #2 + #3)
--
-- 背景: 用户验收发现 Kit (装修配套) 和 RoomPurpose (房源用途) 误绑 park_id.
--       实际是全局配置字典 (不同园区共用同一套字典), 不应按园区隔离.
--       修复: 去掉 park_id 字段 (实体/VO/API/前端表单), 唯一性由 (name, deleted) 保证.
--
-- 与 csyh 参考实现差异:
--   csyh 源 (kitType.vue / purpose.vue) form 必填 parkId, 按用户当前园区过滤.
--   本项目设计: 全部字典全局可见, 一个园区下不需重复定义, 故移除 parkId.
--   如未来需 csyh 行为, 改回即可 (V39 是 ALTER 迁移, 可回滚).
--
-- 数据假设: 当前 sys_kit / sys_room_purpose 的 (name, deleted) 唯一.
--   种子数据: sys_kit 2 行 (不同名), sys_room_purpose 3 行 (不同名), 全部 park_id=1.
--   如有跨园区重名, 需先手动 dedup (保留最早 id):
--     DELETE k1 FROM sys_kit k1 INNER JOIN (
--       SELECT kit_name, MIN(id) AS min_id FROM sys_kit WHERE deleted=0 GROUP BY kit_name HAVING COUNT(*) > 1
--     ) k2 ON k1.kit_name = k2.kit_name AND k1.id > k2.min_id AND k1.deleted = 0;
--
-- 影响: equipment 子表 (sys_equipment) 的 park_id 不变 (P1 #5 后续处理).
-- ================================================================

-- 1) sys_kit: 新唯一键 → 旧唯一键 → 删列
ALTER TABLE `sys_kit`
    ADD UNIQUE KEY `uk_kit_name` (`kit_name`, `deleted`);

ALTER TABLE `sys_kit`
    DROP INDEX `uk_park_kit_name`;

ALTER TABLE `sys_kit`
    DROP COLUMN `park_id`;

-- 2) sys_room_purpose: 新唯一键 → 旧唯一键 → 删列
ALTER TABLE `sys_room_purpose`
    ADD UNIQUE KEY `uk_purpose_name` (`purpose_name`, `deleted`);

ALTER TABLE `sys_room_purpose`
    DROP INDEX `uk_park_purpose_name`;

ALTER TABLE `sys_room_purpose`
    DROP COLUMN `park_id`;

-- 3) 验证
SELECT 'kit_count'             AS k, COUNT(*) AS v FROM sys_kit         WHERE deleted = 0;
SELECT 'room_purpose_count'    AS k, COUNT(*) AS v FROM sys_room_purpose WHERE deleted = 0;
SELECT 'kit_has_park_id'       AS k, COUNT(*) AS v FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_kit' AND column_name = 'park_id';
SELECT 'room_purpose_has_park_id' AS k, COUNT(*) AS v FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_room_purpose' AND column_name = 'park_id';
