-- ============================================
-- M5 DataScope 集成测试 SQL
-- 配套: DataScopeIntegrationTest
-- 在 tenant-test-schema.sql 基础上扩展
-- ============================================

-- sys_user_role 关联表 (V1 schema 未含, M5 需查 user 的 role)
-- W3 修复: 增加 id 列, 否则 UserRoleMapper.selectList() 查 SELECT id 报 Column "id" not found
CREATE TABLE sys_user_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
);

-- ============================================
-- 数据准备: 验证 scope=4 (本人) data_scope
-- ============================================
DELETE FROM sys_user_role;

-- 重新设置 role1 (id=101) 的 data_scope=4 (本人)
UPDATE sys_role SET data_scope = 4, custom_dept_ids = NULL WHERE id = 101;

-- user1 (id=101, 租户 1) 关联 role1 (id=101, data_scope=4 本人)
INSERT INTO sys_user_role (user_id, role_id) VALUES (101, 101);
