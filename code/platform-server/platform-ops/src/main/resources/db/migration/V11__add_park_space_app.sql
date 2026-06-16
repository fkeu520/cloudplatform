-- ================================================================
-- V11: 新增 park-space 应用 (csyh 融合 W3 占位)
--
-- W3 阶段范围: 仅新增 park-space 一个 app 的 seed, 其他 5 个
-- (park-contract/park-property/park-business/park-finance/park-service)
-- 留待 W3+ 业务模块翻译时增量添加。
--
-- sys_menu.app_id 字段已存在 (V20), W3 阶段不引入菜单数据,
-- park-space tab 暂为空, W4 业务模块上线时再批量 UPDATE app_id=6。
--
-- W3 工作台 /app/user 端点将返回:
-- - admin (运营管理员 userType=2): 不依赖 sys_user_role, 返回空, 不显示 park-space tab
-- - 业务用户 (userType=0/1): 通过角色授权的 menu.app_id 找, W3 阶段 menu 还没挂, 所以也不显示
-- - 效果: park-space app seed 后, 当前阶段不影响任何现有用户, 仅作为后续 park-* 业务挂载点
-- ================================================================

INSERT INTO `sys_app` (`id`, `app_name`, `app_code`, `app_icon`, `app_type`, `sort`, `status`, `remark`)
VALUES (6, '空间中心', 'park-space', 'Building', 1, 10, 1, 'csyh 园区业务融合 - 房源/楼宇/楼层/工位 (W3 占位)')
ON DUPLICATE KEY UPDATE `app_name` = VALUES(`app_name`), `sort` = VALUES(`sort`);