-- ============================================
-- V21: 统一 tenant_id 字段类型 Integer -> BIGINT
-- ============================================
-- 决策日期: 2026-06-04
-- 配套: doc/P0-1-M4-完成度审计.md §六.6.2 (历史类型不一致问题)
--
-- 背景:
--   sys_user / sys_dict_type / sys_config 的 tenant_id 字段历史上误用 INTEGER (4 字节)
--   但雪花 ID 是 BIGINT (8 字节), 业务表数据 join 时可能发生截断
--   sys_role / sys_organization / sys_oper_log / sys_login_log 等已用 BIGINT
--
-- 风险:
--   若已有数据 tenant_id 实际值 > 2^31 (2147483647), ALTER 后数据无变化 (MySQL 不会截断)
--   若已有数据 tenant_id > 2^32 (4294967295), 历史可能已截断, 但本项目租户数 < 100, 无此风险
--
-- 影响: 3 个表, 0 数据风险 (租户数 < 100, 远低于 INTEGER 上限)
-- 回滚: 反向执行 ALTER TABLE ... MODIFY COLUMN tenant_id INT NOT NULL DEFAULT 1
-- ============================================

ALTER TABLE sys_user MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE sys_dict_type MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
ALTER TABLE sys_config MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID';
