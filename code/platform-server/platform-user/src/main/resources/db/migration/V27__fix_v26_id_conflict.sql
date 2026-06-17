-- V27: 修复 V26 的 INSERT id 冲突 + 兜底
--
-- 背景: V26 用 (SELECT MAX(id)+1 FROM sys_tenant_app) 计算 id, MySQL 在
--   INSERT...VALUES 多行时 subquery 行为是 undefined (可能 5 行拿到同一个 id 冲突)
--   V27 用 ROW_NUMBER() 保证每个 id 唯一
--
-- 兼容: V26 跑了 / 没跑 / 部分成功 (DELETE 跑了 INSERT 部分失败), 都能恢复
--   - V26 跑了成功: V27 的 DELETE 清掉, V27 的 INSERT 重新插
--   - V26 跑了部分成功: V27 的 DELETE 清掉部分成功的那 1 行, V27 的 INSERT 重新插
--   - V26 没跑: V26 的 V (V27) 比 V26 大, V26 仍要跑 (会失败), V27 兜底
--     为此 V27 是独立补丁, V26 失败也不影响 V27

-- 1) 清掉 tenant 1 全部 (V26 可能部分成功留 1 行)
DELETE FROM `sys_tenant_app` WHERE `tenant_id` = 1;

-- 2) 用 ROW_NUMBER() 计算唯一 id, 插入 5 个 system app
--    COALESCE 处理空表 (MAX 返回 NULL) 情况
--    兼容 MySQL 5.7/8.0 (ROW_NUMBER 是 8.0 标准, 5.7 不支持 — 平台用 8.0.46, 没问题)
INSERT INTO `sys_tenant_app` (`id`, `tenant_id`, `app_id`, `status`)
SELECT
    COALESCE((SELECT MAX(`id`) FROM `sys_tenant_app`), 0) + ROW_NUMBER() OVER (ORDER BY `id`),
    1, `id`, 1
FROM `sys_app`
WHERE `app_type` = 0;

-- 3) 兜底 tenant_id (同 V26)
UPDATE `sys_user` SET `tenant_id` = 1 WHERE `user_type` = 1 AND `tenant_id` IS NULL;
