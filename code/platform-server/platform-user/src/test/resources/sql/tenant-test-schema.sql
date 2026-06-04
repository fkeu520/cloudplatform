-- ============================================
-- P0-1 多租户集成测试 Schema
-- 配套: doc/P0-1-集成测试checklist.md
-- 数据库: H2 MySQL 模式 (MODE=MySQL)
-- ============================================

-- ----------------------------
-- 1. 用户表 (与 V1 一致, 含 tenant_id)
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user (
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
-- 2. 角色表 (与 V1 一致)
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    status TINYINT DEFAULT 1,
    sort INT DEFAULT 0,
    remark VARCHAR(255),
    tenant_id BIGINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id)
);

-- ----------------------------
-- 3. 组织表 (与 V1+V4+V6 一致, 含全部字段)
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_organization (
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
CREATE TABLE IF NOT EXISTS sys_dict_type (
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
CREATE TABLE IF NOT EXISTS sys_config (
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
CREATE TABLE IF NOT EXISTS sys_menu (
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
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id)
);
