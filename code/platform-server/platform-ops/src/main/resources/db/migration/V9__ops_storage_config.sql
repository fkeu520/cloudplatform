-- =============================================
-- 运营管理服务：存储配置表
-- =============================================

CREATE TABLE IF NOT EXISTS `sys_storage_config` (
    `id` bigint NOT NULL COMMENT 'ID',
    `name` varchar(100) NOT NULL COMMENT '名称',
    `endpoint` varchar(500) NOT NULL COMMENT 'Endpoint地址',
    `access_key` varchar(200) DEFAULT NULL COMMENT 'Access Key',
    `secret_key` varchar(200) DEFAULT NULL COMMENT 'Secret Key',
    `region` varchar(50) DEFAULT NULL COMMENT '区域',
    `is_secure` tinyint DEFAULT '0' COMMENT '是否HTTPS',
    `default_bucket` varchar(100) DEFAULT NULL COMMENT '默认Bucket',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（0停用 1启用）',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `sort` int DEFAULT '0' COMMENT '排序',
    `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储配置表';
