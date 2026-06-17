-- V25: 添加 opsadmin 租户管理员 (默认租户 1)
-- 背景: V15 把 admin (id=1) 设成 userType=2 (运营管理员), admin 保持不变
--       新增 opsadmin 作为默认租户的租户管理员, 用于访问主管理后台 (8080)
-- 用户:
--   admin (id=1)     userType=2 运营管理员  (V15 已设, 不变)   -> 登录 8090
--   opsadmin (id=2)  userType=1 租户管理员  (新增)            -> 登录 8080
-- 密码: 都是 123456 (MD5: e10adc3949ba59abbe56e057f20f883e)
--
-- 注意: 这是"revert"后的 V25, 之前的 V25 错误地修改了 admin.user_type,
--       现在 V25 不动 admin, 只新增 opsadmin 用户

-- 1) 添加 opsadmin 租户管理员 (id=2, userType=1, tenant_id=1)
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `nickname`, `mobile`, `email`, `status`, `user_type`, `tenant_id`)
VALUES (2, 'opsadmin', 'e10adc3949ba59abbe56e057f20f883e', '默认租户管理员', '13800138001', 'opsadmin@cloudhub.com', 1, 1, 1);

-- 2) opsadmin 授权角色 1 (SUPER_ADMIN) - 拿全部菜单
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES (2, 1);

-- 3) 防御: sys_tenant_app 已有全 system 应用 (V8 seed, 兜底)
--    防止 V8 没跑导致 opsadmin 拿不到菜单
INSERT IGNORE INTO `sys_tenant_app` (`tenant_id`, `app_id`)
SELECT 1, `id` FROM `sys_app` WHERE `app_type` = 0;
