-- =============================================
-- v8 P0-3 sys_oper_log 增 step-up 关联字段
-- 配套: doc/plan/v8-P0-tenant-protection-plan.md ADR-008
-- =============================================

ALTER TABLE `sys_oper_log`
    ADD COLUMN `step_up_token_id` BIGINT DEFAULT NULL COMMENT '本次操作使用的 step-up token ID' AFTER `oper_param`,
    ADD COLUMN `requires_step_up` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '本操作是否需要 step-up' AFTER `step_up_token_id`,
    ADD KEY `idx_step_up_token` (`step_up_token_id`);
