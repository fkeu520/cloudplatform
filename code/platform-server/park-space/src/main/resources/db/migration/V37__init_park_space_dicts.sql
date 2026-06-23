-- ================================================================
-- V38: park-space 字典初始化 (csyh 业务融合 Phase 1)
--
-- 背景: V36/V37 新增字段需要字典支持, 初始化 4 个字典类型:
--   - property_right (产权性质)
--   - building_structure (建筑结构, 国标 GB/T 50145)
--   - lease_method (租赁方式)
--   - floor_category (楼层类型)
--
-- 字典表: sys_dict_type (类型) + sys_dict_data (数据项)
-- 关联: BuildingController / RoomController 等通过 dict_type 查询
-- ================================================================

-- 1) 产权性质 property_right
INSERT IGNORE INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `remark`, `tenant_id`) VALUES
(601, '产权性质', 'property_right', 1, '园区楼宇产权性质 (csyh propertyRigth)', 1);

INSERT IGNORE INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `css_class`, `tenant_id`) VALUES
(6010, 'property_right', '国有产权', '0', 1, 1, '', 1),
(6011, 'property_right', '集体产权', '1', 2, 1, '', 1),
(6012, 'property_right', '共有产权', '2', 3, 1, '', 1),
(6013, 'property_right', '个人产权', '3', 4, 1, '', 1);

-- 2) 建筑结构 building_structure (国标 GB/T 50145)
INSERT IGNORE INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `remark`, `tenant_id`) VALUES
(602, '建筑结构', 'building_structure', 1, '园区建筑结构 (csyh buildingSafety, 国标 GB/T 50145)', 1);

INSERT IGNORE INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `css_class`, `tenant_id`) VALUES
(6020, 'building_structure', '框架结构',         '0', 1, 1, '', 1),
(6021, 'building_structure', '钢结构',           '1', 2, 1, '', 1),
(6022, 'building_structure', '钢筋混凝土',       '2', 3, 1, '', 1),
(6023, 'building_structure', '砖混结构',         '3', 4, 1, '', 1),
(6024, 'building_structure', '混合结构',         '4', 5, 1, '', 1),
(6025, 'building_structure', '砖木结构',         '5', 6, 1, '', 1),
(6026, 'building_structure', '其他',             '6', 7, 1, '', 1);

-- 3) 租赁方式 lease_method
INSERT IGNORE INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `remark`, `tenant_id`) VALUES
(603, '租赁方式', 'lease_method', 1, '园区租赁方式 (csyh leaseMethodList)', 1);

INSERT IGNORE INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `css_class`, `tenant_id`) VALUES
(6030, 'lease_method', '直租',     '0', 1, 1, '', 1),
(6031, 'lease_method', '转租',     '1', 2, 1, '', 1),
(6032, 'lease_method', '联租',     '2', 3, 1, '', 1),
(6033, 'lease_method', '其他',     '9', 9, 1, '', 1);

-- 4) 楼层类型 floor_category
INSERT IGNORE INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `remark`, `tenant_id`) VALUES
(604, '楼层类型', 'floor_category', 1, '楼层类型 (csyh floorCategory)', 1);

INSERT IGNORE INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `css_class`, `tenant_id`) VALUES
(6040, 'floor_category', '地上', '0', 1, 1, '', 1),
(6041, 'floor_category', '地下', '1', 2, 1, '', 1),
(6042, 'floor_category', '夹层', '2', 3, 1, '', 1);

-- 5) 房屋结构 room_structure (Room 模块用, csyh 字段)
INSERT IGNORE INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `remark`, `tenant_id`) VALUES
(605, '房屋结构', 'room_structure', 1, '园区房间房屋结构 (csyh hardcode)', 1);

INSERT IGNORE INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `css_class`, `tenant_id`) VALUES
(6050, 'room_structure', '框架',         '0', 1, 1, '', 1),
(6051, 'room_structure', '钢结构',       '1', 2, 1, '', 1),
(6052, 'room_structure', '钢钢筋混凝土', '2', 3, 1, '', 1),
(6053, 'room_structure', '钢筋混凝土',   '3', 4, 1, '', 1),
(6054, 'room_structure', '混合',         '4', 5, 1, '', 1),
(6055, 'room_structure', '砖木',         '5', 6, 1, '', 1),
(6056, 'room_structure', '其他',         '6', 7, 1, '', 1);

-- 6) 房间状态 room_status (前端 radio filter: 全部/空置/已租/已售/装修中)
INSERT IGNORE INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `remark`, `tenant_id`) VALUES
(606, '房间状态', 'room_status', 1, '园区房间使用状态 (csyh)', 1);

INSERT IGNORE INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `css_class`, `tenant_id`) VALUES
(6060, 'room_status', '空置',     '0', 1, 1, 'info',    1),
(6061, 'room_status', '已租',     '1', 2, 1, 'success', 1),
(6062, 'room_status', '装修中',   '2', 3, 1, 'warning', 1),
(6063, 'room_status', '已售',     '3', 4, 1, 'danger',  1),
(6064, 'room_status', '自用',     '4', 5, 1, '',       1),
(6065, 'room_status', '已预订',   '5', 6, 1, '',       1);

-- 7) 验证
SELECT 'dict_type_count' AS k, COUNT(*) AS v FROM sys_dict_type WHERE dict_type IN ('property_right', 'building_structure', 'lease_method', 'floor_category', 'room_structure', 'room_status') AND deleted = 0;
SELECT 'dict_data_count' AS k, COUNT(*) AS v FROM sys_dict_data WHERE dict_type IN ('property_right', 'building_structure', 'lease_method', 'floor_category', 'room_structure', 'room_status') AND deleted = 0;
