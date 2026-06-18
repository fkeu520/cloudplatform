-- V31: 创建园区主表 sys_park
-- 用于系统管理下的「园区管理」功能，与 space-std 的 park_id 字段关联

CREATE TABLE IF NOT EXISTS `sys_park` (
    `id`             BIGINT         NOT NULL COMMENT '主键',
    `park_name`      VARCHAR(100)   NOT NULL COMMENT '园区名称',
    `province`       VARCHAR(32)    DEFAULT NULL COMMENT '省',
    `city`           VARCHAR(32)    DEFAULT NULL COMMENT '市',
    `district`       VARCHAR(32)    DEFAULT NULL COMMENT '区',
    `address`        VARCHAR(255)   DEFAULT NULL COMMENT '详细地址',
    `longitude`      DECIMAL(10,7)  DEFAULT NULL COMMENT '经度',
    `latitude`       DECIMAL(10,7)  DEFAULT NULL COMMENT '纬度',
    `description`    VARCHAR(500)   DEFAULT NULL COMMENT '园区简介',
    `land_area`      DECIMAL(16,2)  DEFAULT NULL COMMENT '占地面积（平方米）',
    `building_area`  DECIMAL(16,2)  DEFAULT NULL COMMENT '建筑面积（平方米）',
    `status`         TINYINT        DEFAULT 1 COMMENT '状态：0停用 1启用',
    `tenant_id`      BIGINT         DEFAULT 1 COMMENT '租户ID',
    `create_time`    DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT        DEFAULT 0 COMMENT '删除标记：0未删 1已删',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='园区主表';
