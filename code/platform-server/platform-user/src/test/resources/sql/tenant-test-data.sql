-- ============================================
-- P0-1 多租户集成测试 数据 (2 租户)
-- 配套: doc/P0-1-集成测试checklist.md
--
-- 注意: 每个 @Test 前都会执行本脚本 (BEFORE_TEST_METHOD)
--       所以需要先 DELETE 清空, 再 INSERT 避免主键冲突
-- ============================================

DELETE FROM sys_user;
DELETE FROM sys_role;
DELETE FROM sys_organization;
DELETE FROM sys_dict_type;
DELETE FROM sys_config;
DELETE FROM sys_menu;

-- ----------------------------
-- sys_user: 2 租户各 2 用户 (共 4 行)
-- ----------------------------
INSERT INTO sys_user (id, username, tenant_id, deleted) VALUES
    (101, 't1_user_a', 1, 0),
    (102, 't1_user_b', 1, 0),
    (201, 't2_user_a', 2, 0),
    (202, 't2_user_b', 2, 0);

-- ----------------------------
-- sys_role: 2 租户各 1 角色 (共 2 行)
-- ----------------------------
INSERT INTO sys_role (id, code, name, tenant_id, deleted) VALUES
    (101, 'T1_ADMIN', 'T1 Admin', 1, 0),
    (201, 'T2_ADMIN', 'T2 Admin', 2, 0);

-- ----------------------------
-- sys_organization: 2 租户各 1 组织 (共 2 行)
-- ----------------------------
INSERT INTO sys_organization (id, name, code, tenant_id, deleted) VALUES
    (101, 'T1 Head Office', 'T1', 1, 0),
    (201, 'T2 Head Office', 'T2', 2, 0);

-- ----------------------------
-- sys_dict_type: 2 租户各 1 字典类型 (共 2 行)
-- ----------------------------
INSERT INTO sys_dict_type (id, dict_name, dict_type, tenant_id, deleted) VALUES
    (101, 'T1 Dict', 't1_dict', 1, 0),
    (201, 'T2 Dict', 't2_dict', 2, 0);

-- ----------------------------
-- sys_config: 2 租户各 1 配置 (共 2 行)
-- ----------------------------
INSERT INTO sys_config (id, config_name, config_key, config_value, tenant_id, deleted) VALUES
    (101, 'T1 Key', 't1.key', 't1_value', 1, 0),
    (201, 'T2 Key', 't2.key', 't2_value', 2, 0);

-- ----------------------------
-- sys_menu: 平台级 (共 2 行, 用于 TC-07 系统表验证)
-- ----------------------------
INSERT INTO sys_menu (id, name, perms, deleted) VALUES
    (1, '系统管理', 'system', 0),
    (2, '用户管理', 'system:user:list', 0);
