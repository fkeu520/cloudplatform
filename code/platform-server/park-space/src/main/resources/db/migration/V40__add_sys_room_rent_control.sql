-- ================================================================
-- V40: park-space sys_room 新增租售控制字段 (P1#7 质量修复)
--
-- 背景: 房源需要支持"可租/可售/可租售/自用"状态及对应价格,
--   用于租售控制管理页面 (room-control).
--
-- 新增字段:
--   renting_selling  INT        0=可租 1=可售 2=可租售 3=自用
--   lease_price      DECIMAL    元/㎡/天
--   sale_price       DECIMAL    元/㎡
--   is_lock          INT        0=未锁定 1=已锁定
--   is_order         INT        0=未预定 1=已预定
-- ================================================================

-- 1) 新增租售控制字段
ALTER TABLE `sys_room`
    ADD COLUMN `renting_selling` int DEFAULT NULL COMMENT '租售状态: 0=可租 1=可售 2=可租售 3=自用' AFTER `status`,
    ADD COLUMN `lease_price` decimal(15,2) DEFAULT NULL COMMENT '租价 (元/㎡/天, csyh leasePrice)' AFTER `renting_selling`,
    ADD COLUMN `sale_price` decimal(15,2) DEFAULT NULL COMMENT '售价 (元/㎡, csyh salePrice)' AFTER `lease_price`,
    ADD COLUMN `is_lock` int NOT NULL DEFAULT '0' COMMENT '是否锁定: 0=否 1=是' AFTER `sale_price`,
    ADD COLUMN `is_order` int NOT NULL DEFAULT '0' COMMENT '是否预定: 0=否 1=是' AFTER `is_lock`;

-- 2) 索引加速租售状态筛选
ALTER TABLE `sys_room`
    ADD KEY `idx_renting_selling` (`renting_selling`),
    ADD KEY `idx_is_lock` (`is_lock`);

-- 3) 验证
SELECT 'room_total' AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0;
SELECT 'room_with_renting_selling' AS k, COUNT(*) AS v FROM sys_room WHERE deleted = 0 AND renting_selling IS NOT NULL;
