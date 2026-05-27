CREATE TABLE IF NOT EXISTS `sys_message` (
    `id`              BIGINT       NOT NULL COMMENT '消息ID',
    `title`           VARCHAR(200) NOT NULL COMMENT '消息标题',
    `content`         TEXT         DEFAULT NULL COMMENT '消息内容',
    `type`            VARCHAR(32)  DEFAULT 'system' COMMENT '消息类型: system-系统消息, notice-通知公告, interaction-互动消息',
    `sender_id`       BIGINT       DEFAULT NULL COMMENT '发送人ID',
    `sender_name`     VARCHAR(64)  DEFAULT NULL COMMENT '发送人名称',
    `receiver_id`     BIGINT       DEFAULT NULL COMMENT '接收人ID',
    `receiver_name`   VARCHAR(64)  DEFAULT NULL COMMENT '接收人名称',
    `read_status`     TINYINT      DEFAULT 0 COMMENT '阅读状态 0未读 1已读',
    `business_type`   VARCHAR(64)  DEFAULT NULL COMMENT '关联业务类型',
    `business_id`     VARCHAR(64)  DEFAULT NULL COMMENT '关联业务ID',
    `create_time`     DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time`     DATETIME     DEFAULT NULL COMMENT '更新时间',
    `deleted`         TINYINT      DEFAULT 0 COMMENT '逻辑删除 0正常 1删除',
    PRIMARY KEY (`id`),
    INDEX `idx_receiver` (`receiver_id`, `receiver_name`),
    INDEX `idx_read_status` (`read_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

CREATE TABLE IF NOT EXISTS `sys_message_channel` (
    `id`              BIGINT       NOT NULL COMMENT '渠道ID',
    `channel_code`    VARCHAR(32)  NOT NULL COMMENT '渠道编码: sms/email/app_push/site',
    `channel_name`    VARCHAR(64)  NOT NULL COMMENT '渠道名称',
    `config_json`     TEXT         DEFAULT NULL COMMENT '渠道配置JSON',
    `status`          TINYINT      DEFAULT 1 COMMENT '状态 0禁用 1启用',
    `remark`          VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time`     DATETIME     DEFAULT NULL COMMENT '更新时间',
    `deleted`         TINYINT      DEFAULT 0 COMMENT '逻辑删除 0正常 1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_channel_code` (`channel_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息渠道配置表';

CREATE TABLE IF NOT EXISTS `sys_message_template` (
    `id`              BIGINT       NOT NULL COMMENT '模板ID',
    `template_code`   VARCHAR(64)  NOT NULL COMMENT '模板编码',
    `template_name`   VARCHAR(128) NOT NULL COMMENT '模板名称',
    `channel_code`    VARCHAR(32)  NOT NULL COMMENT '所属渠道编码',
    `sign_name`       VARCHAR(32)  DEFAULT NULL COMMENT '短信签名(仅限短信渠道)',
    `template_id`     VARCHAR(64)  DEFAULT NULL COMMENT '第三方模板ID(阿里云模板CODE)',
    `template_content` TEXT        NOT NULL COMMENT '模板内容(含变量占位符 {name})',
    `params_json`     TEXT         DEFAULT NULL COMMENT '变量定义JSON',
    `status`          TINYINT      DEFAULT 1 COMMENT '状态 0禁用 1启用',
    `create_time`     DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time`     DATETIME     DEFAULT NULL COMMENT '更新时间',
    `deleted`         TINYINT      DEFAULT 0 COMMENT '逻辑删除 0正常 1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`, `deleted`),
    INDEX `idx_channel` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息模板表';

CREATE TABLE IF NOT EXISTS `sys_message_record` (
    `id`                BIGINT       NOT NULL COMMENT '记录ID',
    `title`             VARCHAR(200) DEFAULT NULL COMMENT '消息标题',
    `content`           TEXT         DEFAULT NULL COMMENT '发送内容(渲染后)',
    `channel_code`      VARCHAR(32)  NOT NULL COMMENT '发送渠道',
    `template_id`       BIGINT       DEFAULT NULL COMMENT '关联模板ID',
    `sender_id`         BIGINT       DEFAULT NULL COMMENT '发送人ID',
    `sender_name`       VARCHAR(64)  DEFAULT NULL COMMENT '发送人名称',
    `receiver_id`       BIGINT       DEFAULT NULL COMMENT '接收人ID',
    `receiver_name`     VARCHAR(64)  DEFAULT NULL COMMENT '接收人名称',
    `receiver_address`  VARCHAR(128) DEFAULT NULL COMMENT '接收地址(手机号/邮箱/用户ID)',
    `send_status`       TINYINT      DEFAULT 0 COMMENT '发送状态: 0待发送 1发送中 2成功 3失败',
    `retry_count`       INT          DEFAULT 0 COMMENT '已重试次数',
    `max_retries`       INT          DEFAULT 3 COMMENT '最大重试次数',
    `send_time`         DATETIME     DEFAULT NULL COMMENT '发送时间',
    `error_msg`         VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    `business_type`     VARCHAR(64)  DEFAULT NULL COMMENT '关联业务类型',
    `business_id`       VARCHAR(64)  DEFAULT NULL COMMENT '关联业务ID',
    `create_time`       DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_time`       DATETIME     DEFAULT NULL COMMENT '更新时间',
    `deleted`           TINYINT      DEFAULT 0 COMMENT '逻辑删除 0正常 1删除',
    PRIMARY KEY (`id`),
    INDEX `idx_channel` (`channel_code`),
    INDEX `idx_send_status` (`send_status`),
    INDEX `idx_receiver_id` (`receiver_id`),
    INDEX `idx_business` (`business_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息发送记录表';

INSERT IGNORE INTO `sys_message` (`id`, `title`, `content`, `type`, `sender_id`, `sender_name`, `receiver_id`, `receiver_name`, `read_status`, `create_time`) VALUES
(1, '系统上线通知', '云枢中台V1.0版本已正式上线，欢迎使用！如需帮助请联系管理员。', 'notice', 1, '系统管理员', NULL, NULL, 0, NOW()),
(2, '安全提醒', '请定期修改您的登录密码，确保账户安全。密码长度不少于8位，需包含字母和数字。', 'system', 1, '系统管理员', NULL, NULL, 0, NOW()),
(3, '功能更新公告', '流程引擎已升级至最新版本，新增催办、转办、加签等功能，请在流程管理中体验新特性。', 'notice', 1, '系统管理员', NULL, NULL, 0, NOW()),
(4, '欢迎使用云枢中台', '欢迎加入云枢中台！您可以在数据看板查看系统运行概况，通过菜单栏快速访问各个功能模块。', 'system', 1, '系统管理员', NULL, NULL, 0, NOW()),
(5, '审批任务待处理', '您有一条新的请假审批任务需要处理，请及时登录系统查看。', 'interaction', 2, '张三', NULL, NULL, 1, NOW()),
(6, '任务已完成', '您提交的请假申请已审批通过。', 'interaction', 1, '系统管理员', NULL, NULL, 1, NOW()),
(7, '系统维护通知', '系统将于本周六凌晨2:00-4:00进行例行维护，届时系统将暂停服务，请提前保存工作。', 'notice', 1, '系统管理员', NULL, NULL, 0, NOW()),
(8, '操作日志提醒', '检测到异常登录尝试，请确认是否为本人操作。登录IP: 192.168.1.100', 'system', 1, '系统管理员', NULL, NULL, 0, NOW()),
(9, '版本更新说明', 'V1.1版本更新内容：新增消息中心模块、优化工作流引擎、修复已知问题。', 'notice', 1, '系统管理员', NULL, NULL, 0, NOW()),
(10, '新功能推荐', '推荐使用消息中心功能，您可以通过右上角铃铛图标查看最新消息通知。', 'system', 1, '系统管理员', NULL, NULL, 0, NOW());

INSERT IGNORE INTO `sys_message_channel` (`id`, `channel_code`, `channel_name`, `config_json`, `status`, `remark`) VALUES
(1, 'site', '站内信', '{}', 1, '系统内置站内信渠道'),
(2, 'sms', '阿里云短信', '{"accessKeyId":"","accessKeySecret":"","signName":"云枢中台","regionId":"cn-hangzhou"}', 1, '阿里云短信渠道(占位配置)');

INSERT IGNORE INTO `sys_message_template` (`id`, `template_code`, `template_name`, `channel_code`, `sign_name`, `template_id`, `template_content`, `params_json`) VALUES
(1, 'SITE_NOTICE_DEFAULT', '站内信-通知公告默认模板', 'site', NULL, NULL, '{content}', '[{"name":"content","desc":"消息内容"}]'),
(2, 'SITE_SYSTEM_DEFAULT', '站内信-系统消息默认模板', 'site', NULL, NULL, '{content}', '[{"name":"content","desc":"消息内容"}]'),
(3, 'SMS_VERIFY_CODE', '短信-验证码模板', 'sms', '云枢中台', 'SMS_123456', '您的验证码为{code}，有效期5分钟。', '[{"name":"code","desc":"验证码"}]'),
(4, 'SMS_NOTICE', '短信-通知模板', 'sms', '云枢中台', 'SMS_234567', '{content}', '[{"name":"content","desc":"通知内容"}]');
