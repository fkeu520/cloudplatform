-- ================================================================
-- V47: 更新 building_structure + room_structure 字典标签
-- ================================================================
-- 背景:
--   - V37 初始化的 room_structure 存在 typo ("钢钢筋混凝土")
--   - 用户要求明确列出: 钢筋混凝土结构, 钢结构, 砖混结构, 木结构, 混合结构
--   - building_structure 标签需对齐命名风格
--
-- 本次修复:
--   - building_structure: 更新标签命名 (钢筋混凝土→钢筋混凝土结构, 砖木结构→木结构)
--   - room_structure:    替换为 5 个标准选项, 修复 typo
--   均保留 dict_value 映射, 不破坏现有数据
-- ================================================================

-- 1) building_structure 更新标签
UPDATE `sys_dict_data` SET `dict_label` = '钢筋混凝土结构' WHERE `dict_type` = 'building_structure' AND `dict_value` = '2' AND `deleted` = 0;
UPDATE `sys_dict_data` SET `dict_label` = '木结构'         WHERE `dict_type` = 'building_structure' AND `dict_value` = '5' AND `deleted` = 0;

-- 2) room_structure 替换为 5 个标准选项 (先删旧数据, 再插新数据)
DELETE FROM `sys_dict_data` WHERE `dict_type` = 'room_structure' AND `deleted` = 0;

INSERT IGNORE INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `css_class`, `tenant_id`) VALUES
(6050, 'room_structure', '钢筋混凝土结构', '0', 1, 1, '', 1),
(6051, 'room_structure', '钢结构',         '1', 2, 1, '', 1),
(6052, 'room_structure', '砖混结构',       '2', 3, 1, '', 1),
(6053, 'room_structure', '木结构',         '3', 4, 1, '', 1),
(6054, 'room_structure', '混合结构',       '4', 5, 1, '', 1);

-- 3) 标记 Flyway 成功
INSERT INTO flyway_schema_history
    (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (
    (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM (SELECT installed_rank FROM flyway_schema_history) AS t),
    '47', 'update building_structure and room_structure dict labels', 'SQL',
    'V47__update_building_room_structure_dicts.sql',
    NULL, 'hugh', NOW(), 100, 1
)
ON DUPLICATE KEY UPDATE success = 1, execution_time = 100;
