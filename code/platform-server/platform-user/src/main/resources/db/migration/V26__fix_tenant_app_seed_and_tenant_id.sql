-- V26: 修复 sys_tenant_app V8 seed bug + 兜底租户管理员的 tenant_id
--
-- 背景: V8 用了 `SELECT 1, 1, id` (id 列写死成 1), PK 冲突导致只有 1 行插入
--   (id=1, app_id=1=系统管理), 其他 4 个 system app (用户中心/流程中心/消息中心/运营管理)
--   都没被授权到租户 1。结果: 租户管理员 (userType=1) 登录主管理后台
--   只能看到 "系统管理" 1 个 tab, 看不到其他 4 个 app。
--
-- 修复策略:
--   1) 清掉 V8 漏的脏数据 (DELETE 不影响其他租户)
--   2) 重新 INSERT 全部 system app (用 subquery 计算新 id, 避免 PK 冲突)
--   3) 兜底: 任何 userType=1 但 tenant_id=NULL 的用户, 设 tenant_id=1
--      (V1 把 admin tenant_id 设成 NULL, 业务上 admin 不可能是租户管理员, 但用户可能改了 userType)
--
-- 验证: 重启 platform-user 后, 租户管理员登录主管理后台 (8080)
--   顶部 tabs 应该看到 5 个: 系统管理 / 用户中心 / 流程中心 / 消息中心 / 运营管理

-- 1) 清掉 tenant 1 的脏数据
DELETE FROM `sys_tenant_app` WHERE `tenant_id` = 1;

-- 2) 重新插入所有 system app (app_type=0)
--    sys_tenant_app.id 是 PK 但没有 AUTO_INCREMENT, 用子查询计算下一个 id
--    sys_app.id 1-5 是 system app: 系统管理/用户中心/流程中心/消息中心/运营管理
INSERT INTO `sys_tenant_app` (`id`, `tenant_id`, `app_id`, `status`) VALUES
    ((SELECT IFNULL(MAX(`id`), 0) FROM `sys_tenant_app`) + 1, 1, 1, 1),
    ((SELECT IFNULL(MAX(`id`), 0) FROM `sys_tenant_app`) + 1, 1, 2, 1),
    ((SELECT IFNULL(MAX(`id`), 0) FROM `sys_tenant_app`) + 1, 1, 3, 1),
    ((SELECT IFNULL(MAX(`id`), 0) FROM `sys_tenant_app`) + 1, 1, 4, 1),
    ((SELECT IFNULL(MAX(`id`), 0) FROM `sys_tenant_app`) + 1, 1, 5, 1);

-- 3) 兜底: 任何 userType=1 但 tenant_id=NULL 的用户, 设 tenant_id=1
UPDATE `sys_user` SET `tenant_id` = 1 WHERE `user_type` = 1 AND `tenant_id` IS NULL;
