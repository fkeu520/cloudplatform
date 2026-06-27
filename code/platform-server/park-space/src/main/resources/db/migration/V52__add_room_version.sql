-- V52: Room 添加乐观锁 version 字段 (S2-1)
-- 防止并发 update 后到覆盖先到, 数据静默丢失

ALTER TABLE sys_room
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号 (MyBatis-Plus @Version)';