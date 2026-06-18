-- 217 手动执行脚本: park-contract 第一张业务表 + 菜单
-- 用法: docker exec -i platform-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} cloudhub_user < add-park-contract-contract.sql
CREATE TABLE IF NOT EXISTS `sys_contract` (
    `id`              bigint        NOT NULL              COMMENT '主键 (雪花)',
    `room_id`         bigint        DEFAULT NULL          COMMENT '关联房屋 ID (sys_room)',
    `park_id`         bigint        DEFAULT NULL          COMMENT '园区 ID',
    `contract_no`     varchar(50)   NOT NULL              COMMENT '合同编号',
    `tenant_name`     varchar(100)  DEFAULT NULL          COMMENT '承租方名称',
    `tenant_phone`    varchar(20)   DEFAULT NULL          COMMENT '承租方电话',
    `start_date`      datetime      DEFAULT NULL,
    `end_date`        datetime      DEFAULT NULL,
    `monthly_rent`    decimal(12,2) DEFAULT NULL,
    `deposit`         decimal(12,2) DEFAULT NULL,
    `payment_type`    varchar(20)   DEFAULT 'MONTHLY',
    `status`          tinyint       NOT NULL DEFAULT '0',
    `remark`          varchar(500)  DEFAULT NULL,
    `tenant_id`       bigint        DEFAULT NULL,
    `create_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     datetime      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `create_by`       bigint        DEFAULT NULL,
    `update_by`       bigint        DEFAULT NULL,
    `deleted`         tinyint       NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_contract_no` (`contract_no`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `perms`, `icon`, `sort`, `status`, `app_id`, `type`)
VALUES
    (300, 0,  '合同中心',  'contract',          'Layout',            NULL,            'Document',   20, 1, 8, 0),
    (301, 300, '合同列表', '/contract/page',    'contract/Index',    'contract:view', 'Documents',   1, 1, 8, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 300), (1, 301);

INSERT IGNORE INTO `sys_contract` (`id`, `room_id`, `park_id`, `contract_no`, `tenant_name`, `tenant_phone`, `start_date`, `end_date`, `monthly_rent`, `deposit`, `payment_type`, `status`, `tenant_id`, `create_by`)
VALUES
    (1900000000000002001, 1900000000000000001, 1, 'CT-2026-0001', '云枢科技',  '13800002001', '2026-01-01 00:00:00', '2028-12-31 00:00:00', 8000.00, 16000.00, 'MONTHLY', 1, 1, 1),
    (1900000000000002002, 1900000000000000003, 1, 'CT-2026-0002', '智汇园区运营', '13800002002', '2026-03-01 00:00:00', '2027-02-28 00:00:00', 2000.00, 4000.00,  'MONTHLY', 0, 1, 1);