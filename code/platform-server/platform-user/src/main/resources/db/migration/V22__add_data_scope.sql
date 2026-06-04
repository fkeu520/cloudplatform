-- ============================================
-- V22: sys_role 加 data_scope + custom_dept_ids 字段
-- ============================================
-- 决策日期: 2026-06-04
-- 配套: doc/M5-P0-2-实施子任务.md
--
-- 背景:
--   sys_role 之前无 data_scope 字段, 所有角色看到的数据范围相同
--   M5 P0-2 引入"数据权限"概念, 同角色不同数据范围
--   e.g. "销售"角色, 经理看本部门, 员工看本人
--
-- 字段语义:
--   data_scope TINYINT DEFAULT 1
--     1 = 全部 (默认, 兼容现有数据)
--     2 = 本部门
--     3 = 本部门及下级
--     4 = 本人
--     5 = 自定义 (custom_dept_ids 决定)
--
-- 兼容性:
--   - DEFAULT 1 兼容老数据 (全部权限, 等同于无 data_scope)
--   - 不影响现有 user_role 关联逻辑
--   - DataScopeAspect 不强制注解, 老代码不受影响
--
-- 回滚:
--   ALTER TABLE sys_role DROP COLUMN data_scope;
--   ALTER TABLE sys_role DROP COLUMN custom_dept_ids;
-- ============================================

ALTER TABLE sys_role ADD COLUMN data_scope TINYINT DEFAULT 1
    COMMENT '数据范围 1=全部 2=本部门 3=本部门及下级 4=本人 5=自定义';

ALTER TABLE sys_role ADD COLUMN custom_dept_ids VARCHAR(1000) DEFAULT NULL
    COMMENT '自定义部门ID列表 (data_scope=5 时使用, 逗号分隔)';
