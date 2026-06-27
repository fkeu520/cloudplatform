-- ============================================
-- 灰度开关审计表 (gray-release-infrastructure PR4)
-- 跟踪 platform.* 灰度开关的所有变更
-- 关联: doc/log/项目进度.md §灰度基础设施
-- ============================================

CREATE TABLE IF NOT EXISTS `sys_gray_audit` (
    `id`           BIGINT       NOT NULL                COMMENT '雪花 ID',
    `switch_key`   VARCHAR(200) NOT NULL                COMMENT '配置键 (如 platform.data-scope.upgrade.enabled)',
    `group_name`   VARCHAR(100) DEFAULT NULL             COMMENT '分组 (M4 P0-1 / M5 P0-2 等)',
    `old_value`    VARCHAR(200) DEFAULT NULL             COMMENT '旧值',
    `new_value`    VARCHAR(200) DEFAULT NULL             COMMENT '新值',
    `op_type`      VARCHAR(20)  NOT NULL                COMMENT '操作类型: NACOS_PUSH / DB_UPDATE / ENV_RESTART / BEAN_REFRESH',
    `operator_id`  BIGINT       DEFAULT NULL             COMMENT '操作人 ID (sys_user.id, DB 方式时记录; Nacos 推送时为 NULL)',
    `operator_name` VARCHAR(64) DEFAULT NULL             COMMENT '操作人姓名 (冗余便于查询)',
    `reason`       VARCHAR(500) DEFAULT NULL             COMMENT '变更原因 (业务方填写)',
    `service_name` VARCHAR(64)  DEFAULT NULL             COMMENT '变更发生服务 (如 platform-user)',
    `instance_ip`  VARCHAR(64)  DEFAULT NULL             COMMENT '变更实例 IP',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_switch_key` (`switch_key`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灰度开关审计表';

-- 初始化 3 条种子审计记录 (标记 PR4 部署初始状态)
INSERT INTO `sys_gray_audit` (`id`, `switch_key`, `group_name`, `old_value`, `new_value`, `op_type`, `operator_name`, `reason`, `service_name`, `create_time`)
VALUES
    (1900000000000001001, 'platform.tenant.interceptor.enabled', 'M4 P0-1 多租户拦截器', NULL, 'true', 'INIT', 'system', 'gray-release-infrastructure PR4 初始化', 'platform-user', NOW()),
    (1900000000000001002, 'platform.data-scope.upgrade.enabled', 'M5 P0-2 数据权限升级', NULL, 'false', 'INIT', 'system', 'gray-release-infrastructure PR4 初始化', 'platform-user', NOW()),
    (1900000000000001003, 'platform.data-scope.upgrade.write-strict', 'M5 P0-2 数据权限升级', NULL, 'true', 'INIT', 'system', 'gray-release-infrastructure PR4 初始化', 'platform-user', NOW());

-- 加入 sys_gray_audit 到 MyBatis-Plus 忽略表列表 (避免被 TenantLineInnerInterceptor 加 tenant_id 条件)
-- (注: 这个表无 tenant_id 列, 但保守起见加入 IGNORE_TABLES 由 MybatisPlusConfig.java 控制)
-- 修改 MybatisPlusConfig IGNORE_TABLES Set 同步加 "sys_gray_audit"