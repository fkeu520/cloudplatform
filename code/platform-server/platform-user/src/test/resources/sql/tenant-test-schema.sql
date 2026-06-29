-- ============================================
-- P0-1 多租户集成测试 Schema
-- 配套: doc/P0-1-集成测试checklist.md
-- 数据库: H2 MySQL 模式 (MODE=MySQL)
--
-- 注意: 每个 @Test 前都会跑 (BEFORE_TEST_METHOD)
--       H2 内存 DB schema 跨测试累积, 用 DROP + CREATE 强制重建
--       避免 IF NOT EXISTS 跳过导致 schema 大小写不一致
-- ============================================

-- 强制清理 (H2 CASCADE 删除外键依赖)
DROP TABLE IF EXISTS sys_user CASCADE;
DROP TABLE IF EXISTS sys_role CASCADE;
DROP TABLE IF EXISTS sys_organization CASCADE;
DROP TABLE IF EXISTS sys_dict_type CASCADE;
DROP TABLE IF EXISTS sys_config CASCADE;
DROP TABLE IF EXISTS sys_menu CASCADE;
DROP TABLE IF EXISTS sys_role_menu CASCADE;
DROP TABLE IF EXISTS sys_user_menu CASCADE;
DROP TABLE IF EXISTS sys_user_role CASCADE;
DROP TABLE IF EXISTS sys_dept CASCADE;
DROP TABLE IF EXISTS sys_post CASCADE;

-- ----------------------------
-- 1. 用户表 (与 V1 一致, 含 tenant_id)
-- ----------------------------
CREATE TABLE sys_user (
    id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100),
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    mobile VARCHAR(20),
    email VARCHAR(100),
    gender TINYINT DEFAULT 0,
    org_id BIGINT,
    dept_id BIGINT,
    post_id BIGINT,
    status TINYINT DEFAULT 1,
    tenant_id BIGINT DEFAULT 1,
    user_type TINYINT DEFAULT 0,
    last_login_ip VARCHAR(50),
    last_login_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id)
);

-- ----------------------------
-- 2. 角色表 (V1 + V22 data_scope + custom_dept_ids)
-- ----------------------------
CREATE TABLE sys_role (
    id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    status TINYINT DEFAULT 1,
    sort INT DEFAULT 0,
    remark VARCHAR(255),
    tenant_id BIGINT DEFAULT 1,
    data_scope TINYINT DEFAULT 1 COMMENT '1=全部 2=本部门 3=本部门及下级 4=本人 5=自定义',
    custom_dept_ids VARCHAR(1000) DEFAULT NULL COMMENT '自定义部门ID列表 (data_scope=5)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id)
);

-- ----------------------------
-- 3. 组织表 (与 V1+V4+V6 一致, 含全部字段)
-- ----------------------------
CREATE TABLE sys_organization (
    id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    type TINYINT DEFAULT 1,
    short_name VARCHAR(50),
    full_name VARCHAR(200),
    legal_person VARCHAR(50),
    registered_address VARCHAR(255),
    business_scope VARCHAR(500),
    tax_number VARCHAR(50),
    phone VARCHAR(20),
    sort INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    tenant_id BIGINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id)
);

-- ----------------------------
-- 4. 字典类型表 (与 V2 一致)
-- ----------------------------
CREATE TABLE sys_dict_type (
    id BIGINT NOT NULL,
    dict_name VARCHAR(100) NOT NULL,
    dict_type VARCHAR(100) NOT NULL,
    status TINYINT DEFAULT 1,
    remark VARCHAR(255),
    tenant_id BIGINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id)
);

-- ----------------------------
-- 5. 参数配置表 (与 V2 一致)
-- ----------------------------
CREATE TABLE sys_config (
    id BIGINT NOT NULL,
    config_name VARCHAR(100) NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(500) NOT NULL,
    config_type TINYINT DEFAULT 0,
    remark VARCHAR(255),
    tenant_id BIGINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id)
);

-- ----------------------------
-- 6. 菜单表 (IGNORE_TABLES 之一, 含 app_id 等 V20 字段)
-- ----------------------------
CREATE TABLE sys_menu (
    id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(50) NOT NULL,
    path VARCHAR(200),
    component VARCHAR(255),
    type TINYINT DEFAULT 1,
    icon VARCHAR(50),
    sort INT DEFAULT 0,
    perms VARCHAR(100),
    status TINYINT DEFAULT 1,
    app_id BIGINT,
    menu_category VARCHAR(32) DEFAULT 'admin',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id)
);

-- ----------------------------
-- 7. 角色-菜单关联表 (V1 schema, MenuMapper 需要)
-- ----------------------------
CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

-- ----------------------------
-- 8. 用户-菜单关联表 (V1 schema)
-- ----------------------------
CREATE TABLE sys_user_menu (
    user_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, menu_id)
);

-- ----------------------------
-- 9. 部门表 (PR2 集成测试用, 简化版)
-- ----------------------------
CREATE TABLE sys_dept (
    id BIGINT NOT NULL,
    org_id BIGINT,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(100),
    code VARCHAR(50),
    manager VARCHAR(50),
    phone VARCHAR(20),
    sort INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id)
);

-- ----------------------------
-- 10. 岗位表 (PR2 集成测试用, 简化版)
-- ----------------------------
CREATE TABLE sys_post (
    id BIGINT NOT NULL,
    org_id BIGINT,
    dept_id BIGINT,
    name VARCHAR(100),
    code VARCHAR(50),
    level VARCHAR(20),
    sort INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id)
);

-- ----------------------------
-- 11. ������־ (M5 PR4 OperLog ����Ȩ�޲���)
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_oper_log (
    id BIGINT NOT NULL,
    title VARCHAR(200),
    business_type INT,
    method VARCHAR(500),
    request_method VARCHAR(10),
    operator_type INT,
    oper_name VARCHAR(64),
    dept_name VARCHAR(100),
    dept_id BIGINT,
    oper_url VARCHAR(500),
    oper_ip VARCHAR(64),
    oper_location VARCHAR(100),
    oper_param TEXT,
    json_result TEXT,
    status INT DEFAULT 0,
    error_msg TEXT,
    oper_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    cost_time BIGINT,
    tenant_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id)
);
