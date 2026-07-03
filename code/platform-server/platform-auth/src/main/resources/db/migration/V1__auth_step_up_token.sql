-- =============================================
-- v8 P0-3 Step-up Token 二次鉴权表
-- 配套: doc/plan/v8-P0-tenant-protection-plan.md ADR-008
-- =============================================

CREATE TABLE IF NOT EXISTS `sys_step_up_token` (
    `id`              BIGINT       NOT NULL                          COMMENT 'ID',
    `token_hash`      VARCHAR(64)  NOT NULL                          COMMENT 'Token SHA-256 哈希（不存明文）',
    `user_id`         BIGINT       NOT NULL                          COMMENT '签发给的 user_id',
    `tenant_id`       BIGINT       DEFAULT NULL                      COMMENT '租户ID（平台超管为 NULL）',
    `scope`           VARCHAR(100) NOT NULL                          COMMENT '允许的操作范围（逗号分隔, 如 "tenant:delete,refund:create"）',
    `single_use`      TINYINT(1)   NOT NULL DEFAULT 1                COMMENT '是否单次有效',
    `used`            TINYINT(1)   NOT NULL DEFAULT 0                COMMENT '是否已使用',
    `issued_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签发时间',
    `expires_at`      DATETIME     NOT NULL                          COMMENT '过期时间（issued_at + 5min）',
    `consumed_at`     DATETIME     DEFAULT NULL                      COMMENT '使用时间',
    `client_ip`       VARCHAR(45)  DEFAULT NULL                      COMMENT '签发时客户端 IP',
    `user_agent`      VARCHAR(500) DEFAULT NULL                      COMMENT '签发时 UA',
    `revoked`         TINYINT(1)   NOT NULL DEFAULT 0                COMMENT '是否被撤销',
    `revoked_at`      DATETIME     DEFAULT NULL                      COMMENT '撤销时间',
    `revoke_reason`   VARCHAR(200) DEFAULT NULL                      COMMENT '撤销原因',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token_hash` (`token_hash`),
    KEY `idx_user_expires` (`user_id`, `expires_at`),
    KEY `idx_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Step-up 二次鉴权 Token';
