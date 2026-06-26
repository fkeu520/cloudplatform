-- ================================================================
-- V48: 更新 room_status 字典为 空置/已租/已售/锁定/预订
-- ================================================================
-- 背景:
--   V37 初始化的 room_status 字典含 6 项 (空置/已租/装修中/已售/自用/已预订)
--   用户要求精简为 5 项: 空置/已租/已售/锁定/预订
--   同时更新 RoomStatus 枚举与其对齐
-- ================================================================

-- 1) 更新字典类型名称
UPDATE `sys_dict_type` SET `remark` = '房间使用状态: 空置/已租/已售/锁定/预订'
WHERE `dict_type` = 'room_status' AND `deleted` = 0;

-- 2) 替换字典数据 (先删旧数据, 再插新数据)
DELETE FROM `sys_dict_data` WHERE `dict_type` = 'room_status' AND `deleted` = 0;

INSERT IGNORE INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `css_class`, `tenant_id`) VALUES
(6060, 'room_status', '空置', '0', 1, 1, 'info',    1),
(6061, 'room_status', '已租', '1', 2, 1, 'success', 1),
(6062, 'room_status', '已售', '2', 3, 1, 'danger',  1),
(6063, 'room_status', '锁定', '3', 4, 1, 'warning', 1),
(6064, 'room_status', '预订', '4', 5, 1, 'primary', 1);

-- 3) 标记 Flyway 成功 (兼容 V45 之后的手动执行模式)
INSERT INTO flyway_schema_history
    (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES (
    (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM (SELECT installed_rank FROM flyway_schema_history) AS t),
    '48', 'update room_status dict to 5 statuses', 'SQL',
    'V48__update_room_status_dict.sql',
    NULL, 'hugh', NOW(), 100, 1
)
ON DUPLICATE KEY UPDATE success = 1, execution_time = 100;
