package com.cloudhub.platform.park.common.mybatis.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ParkBaseEntity 字段继承测试 (W2.2 验证)
 *
 * <p>验证: 业务实体继承 ParkBaseEntity 后, 同时拥有云枢 BaseEntity 字段 (id/createTime/updateTime/deleted)
 * + csyh 风格字段 (createBy/updateBy).</p>
 */
class ParkBaseEntityTest {

    static class TestEntity extends ParkBaseEntity {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @Test
    void testInheritanceFields() {
        TestEntity e = new TestEntity();
        e.setId(100L);
        e.setCreateBy(1L);
        e.setUpdateBy(2L);
        e.setName("test");

        // 云枢 BaseEntity 字段
        assertEquals(100L, e.getId());
        // ParkBaseEntity 扩展字段
        assertEquals(1L, e.getCreateBy());
        assertEquals(2L, e.getUpdateBy());
        // 业务字段
        assertEquals("test", e.getName());
    }

    @Test
    void testEqualsAndHashCode() {
        TestEntity e1 = new TestEntity();
        e1.setId(1L);
        e1.setName("a");

        TestEntity e2 = new TestEntity();
        e2.setId(1L);
        e2.setName("a");

        // 来自 @EqualsAndHashCode(callSuper = true)
        assertEquals(e1, e2);
    }
}
