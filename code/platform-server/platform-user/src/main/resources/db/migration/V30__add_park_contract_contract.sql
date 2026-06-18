-- ================================================================
-- V30: park-contract 第一张业务表 (csyh 业务融合 W3.2b)
--
-- 背景: W3.1+W3.2 落地 park-space (Room) + park-property (Building)
--       W3.2b 落地 park-contract (Contract), 第三张业务表
--       sys_app id=8 (合同中心), 类型=1 (业务应用)
--
-- 设计:
--   - sys_contract 表 (园区合同), 与 sys_room 1:1 关联 (一个房源一份合同)
--   - 雪花 ID (BaseEntity) + 逻辑删除 + 多租户
--   - 状态机: DRAFT(0) → ACTIVE(1) → EXPIRED(2) / TERMINATED(3)
--
-- 路由: GET /contract/page
-- 菜单: app 8 (合同中心) 下加 2 个菜单
-- ================================================================

-- 1) sys_contract 表
CREATE TABLE IF NOT EXISTS `sys_contract` (
    `id`              bigint        NOT NULL              COMMENT '主键 (雪花)',
    `room_id`         bigint        DEFAULT NULL          COMMENT '关联房屋 ID (sys_room)',
    `park_id`         bigint        DEFAULT NULL          COMMENT '园区 ID',
    `contract_no`     varchar(50)   NOT NULL              COMMENT '合同编号 (e.g. CT-2026-0001)',
    `tenant_name`     varchar(100)  DEFAULT NULL          COMMENT '承租方名称 (企业/个人)',
    `tenant_phone`    varchar(20)   DEFAULT NULL          COMMENT '承租方电话',
    `start_date`      datetime      DEFAULT NULL          COMMENT '合同开始日期',
    `end_date`        datetime      DEFAULT NULL          COMMENT '合同结束日期',
    `monthly_rent`    decimal(12,2) DEFAULT NULL          COMMENT '月租金 (元)',
    `deposit`         decimal(12,2) DEFAULT NULL          COMMENT '押金 (元)',
    `payment_type`    varchar(20)   DEFAULT 'MONTHLY'     COMMENT '付款方式: MONTHLY/QUARTERLY/YEARLY',
    `status`          tinyint       NOT NULL DEFAULT '0'  COMMENT '状态: 0=草稿 1=已签约 2=已到期 3=已终止',
    `remark`          varchar(500)  DEFAULT NULL          COMMENT '备注',
    `tenant_id`       bigint        DEFAULT NULL          COMMENT '租户 ID',
    `create_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     datetime      DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `create_by`       bigint        DEFAULT NULL,
    `update_by`       bigint        DEFAULT NULL,
    `deleted`         tinyint       NOT NULL DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_park_id` (`park_id`),
    KEY `idx_status` (`status`),
    KEY `idx_contract_no` (`contract_no`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='园区合同表 (park-contract 业务)';

-- 2) 合同中心菜单 (app id=8, park-contract)
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `perms`, `icon`, `sort`, `status`, `app_id`, `type`)
VALUES
    (300, 0,  '合同中心',    'contract',          'Layout',            NULL,                   'Document',    20, 1, 8, 0),
    (301, 300, '合同列表',   '/contract/page',    'contract/Index',    'contract:view',        'Documents',    1, 1, 8, 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `path` = VALUES(`path`);

-- 3) 绑给 SUPER_ADMIN 角色 (id=1)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
    (1, 300),
    (1, 301);

-- 4) 种子: 2 份示例合同
INSERT IGNORE INTO `sys_contract` (`id`, `room_id`, `park_id`, `contract_no`, `tenant_name`, `tenant_phone`, `start_date`, `end_date`, `monthly_rent`, `deposit`, `payment_type`, `status`, `tenant_id`, `create_by`)
VALUES
    (1900000000000002001, 1900000000000000001, 1, 'CT-2026-0001', '云枢科技',  '13800002001', '2026-01-01 00:00:00', '2028-12-31 00:00:00', 8000.00, 16000.00, 'MONTHLY', 1, 1, 1),
    (1900000000000002002, 1900000000000000003, 1, 'CT-2026-0002', '智汇园区运营', '13800002002', '2026-03-01 00:00:00', '2027-02-28 00:00:00', 2000.00, 4000.00,  'MONTHLY', 0, 1, 1);