-- ============================================
-- M5 P0-2 PR4 D+8 写方法 @DataScope 集成测试数据
-- 配套: DataScopeWriteMethodTest
-- 在 datascope-test.sql 基础上扩展
-- ============================================

-- sys_user_role 已经在 datascope-test.sql 中建过
-- 此处仅扩展 sys_dept (PR1 PR2 集成测试用)

-- sys_dept: 2 部门, 验证 update/delete scope=3
-- id=101 (T1_TECH) 命中 userId=101 的 childDepts
-- id=102 (T1_HR) 不在 userId=101 范围
-- 注: parent_id 字段可能 schema 中没, 这里只关注 id
DELETE FROM sys_dept;
INSERT INTO sys_dept (id, org_id, name, code, status, deleted) VALUES
    (101, 1, 'T1 Tech Dept', 'T1_TECH', 1, 0),
    (102, 1, 'T1 HR Dept', 'T1_HR', 1, 0);
