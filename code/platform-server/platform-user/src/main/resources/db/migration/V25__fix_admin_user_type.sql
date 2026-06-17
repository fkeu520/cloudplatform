-- V25: 修复 admin 用户为租户管理员 (W3 P0-5 修复补丁)
-- 背景: V15 把 admin (id=1) 设成 user_type=2 (运营管理员),
-- 导致 MenuService.getUserMenusInternal 走 userType==2 分支直接返回空数组
-- ("运营管理员：不能访问管理后台，返回空")
-- 实际意图: admin 应该是租户管理员, 通过 sys_tenant_app 拿到全应用菜单
-- 同时 V1 把 admin tenant_id 设为 NULL, 但租户管理员分支需要 tenant_id
-- 才会调 selectAuthorizedAppIds, 否则 authorizedAppIds 为空 → 菜单空
-- 修复: 1) admin.user_type 从 2 → 1 (租户管理员)
--      2) admin.tenant_id 从 NULL → 1
--      3) 确保 sys_tenant_app 已有租户 1 的全 system 应用 (V8 已 seed, 此处补防御)
--
-- 影响范围: 只改 id=1 的 admin 用户, 业务数据不动
-- 风险: 低 - Flyway 幂等 (UPDATE + WHERE 条件)
-- 验证: 重启 platform-user 后, admin 登录应看到完整菜单 (系统管理/用户中心/流程中心/消息中心/运营管理)

-- 1) admin 改为租户管理员 + 租户 1
UPDATE `sys_user`
SET `user_type` = 1, `tenant_id` = 1
WHERE `id` = 1 AND `user_type` = 2;

-- 2) 防御: 确保 sys_tenant_app 有租户 1 的全 system 应用授权
--    (V8 已 seed, 但 V25 是修复补丁, 显式 INSERT IGNORE 兜底)
INSERT IGNORE INTO `sys_tenant_app` (`tenant_id`, `app_id`)
SELECT 1, `id` FROM `sys_app` WHERE `app_type` = 0;
