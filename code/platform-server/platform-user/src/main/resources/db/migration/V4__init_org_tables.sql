CREATE TABLE IF NOT EXISTS sys_dept
(
    id              BIGINT NOT NULL PRIMARY KEY COMMENT '主键ID',
    org_id          BIGINT NOT NULL COMMENT '所属组织ID',
    parent_id       BIGINT DEFAULT 0 COMMENT '父部门ID',
    name            VARCHAR(100) NOT NULL COMMENT '部门名称',
    code            VARCHAR(50) COMMENT '部门编码',
    manager         VARCHAR(50) COMMENT '部门负责人',
    phone           VARCHAR(20) COMMENT '联系电话',
    sort            INT DEFAULT 0 COMMENT '排序',
    status          TINYINT DEFAULT 1 COMMENT '状态 1启用 0禁用',
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT DEFAULT 0 COMMENT '是否删除 1是 0否',
    INDEX idx_org_id (org_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

CREATE TABLE IF NOT EXISTS sys_post
(
    id              BIGINT NOT NULL PRIMARY KEY COMMENT '主键ID',
    org_id          BIGINT NOT NULL COMMENT '所属组织ID',
    dept_id         BIGINT COMMENT '所属部门ID',
    name            VARCHAR(100) NOT NULL COMMENT '岗位名称',
    code            VARCHAR(50) COMMENT '岗位编码',
    level           VARCHAR(20) COMMENT '岗位级别',
    sort            INT DEFAULT 0 COMMENT '排序',
    status          TINYINT DEFAULT 1 COMMENT '状态 1启用 0禁用',
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT DEFAULT 0 COMMENT '是否删除 1是 0否',
    INDEX idx_org_id (org_id),
    INDEX idx_dept_id (dept_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位表';

ALTER TABLE sys_organization MODIFY COLUMN type TINYINT DEFAULT 1 COMMENT '类型 1集团 2总部 3子公司 4分公司';
ALTER TABLE sys_organization ADD COLUMN short_name VARCHAR(100) COMMENT '简称';
ALTER TABLE sys_organization ADD COLUMN full_name VARCHAR(500) COMMENT '全称';
ALTER TABLE sys_organization ADD COLUMN legal_person VARCHAR(50) COMMENT '法定代表人';
ALTER TABLE sys_organization ADD COLUMN registered_address VARCHAR(500) COMMENT '注册地址';
ALTER TABLE sys_organization ADD COLUMN business_scope VARCHAR(1000) COMMENT '经营范围';
ALTER TABLE sys_organization ADD COLUMN tax_number VARCHAR(50) COMMENT '税号';
ALTER TABLE sys_organization ADD COLUMN phone VARCHAR(20) COMMENT '联系电话';
ALTER TABLE sys_organization MODIFY COLUMN tenant_id BIGINT COMMENT '租户ID';

INSERT IGNORE INTO sys_organization (id, parent_id, name, code, type, short_name, full_name, status) VALUES
(1, 0, '云枢集团', 'YUNSHU', 1, '云枢集团', '云枢科技集团有限公司', 1),
(2, 1, '总部', 'HEAD', 2, '总部', '云枢科技集团有限公司总部', 1),
(3, 1, '华东分公司', 'EAST', 4, '华东分公司', '云枢科技集团有限公司华东分公司', 1);

-- sys_dept 种子: 显式 id (雪花风格 19 位, 范围 1900000000000000001-1900000000000000006)
INSERT IGNORE INTO sys_dept (id, org_id, parent_id, name, code, sort, status, create_time, update_time, deleted) VALUES
(1900000000000000001, 2, 0, '总经办', 'GM', 1, 1, NOW(), NOW(), 0),
(1900000000000000002, 2, 0, '人力资源部', 'HR', 2, 1, NOW(), NOW(), 0),
(1900000000000000003, 2, 0, '财务部', 'FIN', 3, 1, NOW(), NOW(), 0),
(1900000000000000004, 2, 0, '技术部', 'TECH', 4, 1, NOW(), NOW(), 0),
(1900000000000000005, 3, 0, '华东销售部', 'EAST_SALES', 1, 1, NOW(), NOW(), 0),
(1900000000000000006, 3, 0, '华东客服部', 'EAST_CS', 2, 1, NOW(), NOW(), 0);

-- sys_post 种子: 显式 id (雪花风格 19 位, 范围 1900000000000001001-1900000000000001010)
INSERT IGNORE INTO sys_post (id, org_id, dept_id, name, code, level, sort, status, create_time, update_time, deleted) VALUES
(1900000000000001001, 2, 1900000000000000001, '总经理', 'GM', 'P10', 1, 1, NOW(), NOW(), 0),
(1900000000000001002, 2, 1900000000000000001, '副总经理', 'DGM', 'P9', 2, 1, NOW(), NOW(), 0),
(1900000000000001003, 2, 1900000000000000002, 'HR总监', 'HR_DIR', 'P8', 1, 1, NOW(), NOW(), 0),
(1900000000000001004, 2, 1900000000000000002, 'HR经理', 'HR_MGR', 'P6', 2, 1, NOW(), NOW(), 0),
(1900000000000001005, 2, 1900000000000000003, '财务总监', 'FIN_DIR', 'P8', 1, 1, NOW(), NOW(), 0),
(1900000000000001006, 2, 1900000000000000004, '技术总监', 'TECH_DIR', 'P8', 1, 1, NOW(), NOW(), 0),
(1900000000000001007, 2, 1900000000000000004, '架构师', 'ARCH', 'P7', 2, 1, NOW(), NOW(), 0),
(1900000000000001008, 2, 1900000000000000004, '开发工程师', 'DEV', 'P5', 3, 1, NOW(), NOW(), 0),
(1900000000000001009, 3, 1900000000000000005, '区域销售经理', 'SALES_MGR', 'P6', 1, 1, NOW(), NOW(), 0),
(1900000000000001010, 3, 1900000000000000006, '客服代表', 'CS', 'P3', 1, 1, NOW(), NOW(), 0);