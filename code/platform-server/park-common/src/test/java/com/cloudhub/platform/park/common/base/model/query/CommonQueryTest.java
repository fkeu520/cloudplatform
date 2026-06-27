package com.cloudhub.platform.park.common.base.model.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CommonQuery 单元测试 (PC2-3: orderBy 白名单)
 */
class CommonQueryTest {

    @Test
    void pc23_safeOrderBy_shouldAcceptWhitelisted() {
        CommonQuery q = new CommonQuery();
        q.setOrderBy("createTime");
        assertEquals("createTime", q.safeOrderBy());

        q.setOrderBy("updateTime");
        assertEquals("updateTime", q.safeOrderBy());

        q.setOrderBy("id");
        assertEquals("id", q.safeOrderBy());
    }

    @Test
    void pc23_safeOrderBy_shouldRejectInjection() {
        CommonQuery q = new CommonQuery();
        q.setOrderBy("createTime; DROP TABLE sys_user--");
        assertEquals("id", q.safeOrderBy(), "PC2-3: SQL 注入应被拒绝, 回退默认 id");

        q.setOrderBy("1=1 OR 1=1");
        assertEquals("id", q.safeOrderBy());

        q.setOrderBy("room_no");
        assertEquals("id", q.safeOrderBy(), "不在白名单的字段应被拒绝");
    }

    @Test
    void pc23_safeOrderBy_shouldHandleNullAndEmpty() {
        CommonQuery q = new CommonQuery();
        q.setOrderBy(null);
        assertEquals("id", q.safeOrderBy());

        q.setOrderBy("");
        assertEquals("id", q.safeOrderBy());
    }

    @Test
    void pc23_safeOrderDirection_shouldAcceptAscdesc() {
        CommonQuery q = new CommonQuery();
        q.setOrderDirection("asc");
        assertEquals("asc", q.safeOrderDirection());

        q.setOrderDirection("desc");
        assertEquals("desc", q.safeOrderDirection());
    }

    @Test
    void pc23_safeOrderDirection_shouldRejectInjection() {
        CommonQuery q = new CommonQuery();
        q.setOrderDirection("asc; DROP TABLE");
        assertEquals("desc", q.safeOrderDirection());

        q.setOrderDirection("ASC");
        assertEquals("desc", q.safeOrderDirection(), "大小写敏感, ASC 应被拒绝");

        q.setOrderDirection(null);
        assertEquals("desc", q.safeOrderDirection());
    }
}