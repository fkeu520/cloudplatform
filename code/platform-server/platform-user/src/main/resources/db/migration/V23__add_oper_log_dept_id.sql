-- M5 P0-2 PR4: 补 sys_oper_log.dept_id 字段 (按部门审计场景)
-- 此前: sys_oper_log 只有 dept_name (varchar 显示用), 无 dept_id (FK) → 无法按部门 SQL JOIN 过滤
-- 现在: 新增 dept_id BIGINT NULL (可空, 兼容历史无 deptName 数据)
-- 索引: idx_oper_log_dept_id 加速审计查询 "某部门某时间段操作"
-- 后续 PR4: OperLogService.page 加 @DataScope(deptAlias = "dept_id") 实现按部门过滤

ALTER TABLE sys_oper_log
    ADD COLUMN dept_id BIGINT NULL COMMENT '部门ID (操作发生时所在部门, 用于数据权限审计)' AFTER dept_name;

-- 索引: 部门 + 时间组合查询是审计场景最常见
CREATE INDEX idx_oper_log_dept_id ON sys_oper_log (dept_id, oper_time);
CREATE INDEX idx_oper_log_tenant_dept ON sys_oper_log (tenant_id, dept_id);

-- 历史数据回填: 尝试通过 dept_name 匹配 sys_dept.name 回填 dept_id
-- 注意: 仅在 dept_name 完全匹配且 dept_name 非空时回填, 否则保持 NULL
UPDATE sys_oper_log o
INNER JOIN sys_dept d ON d.name = o.dept_name AND d.deleted = 0
SET o.dept_id = d.id
WHERE o.dept_id IS NULL
  AND o.dept_name IS NOT NULL
  AND o.dept_name != '';
