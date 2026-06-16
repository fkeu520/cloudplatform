package com.cloudhub.platform.park.common.base.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CastUtils 单元测试 (W2.1 验证)
 */
class CastUtilsTest {

    @Test
    void testToString() {
        assertNull(CastUtils.toString(null));
        assertEquals("hello", CastUtils.toString("hello"));
        assertEquals("123", CastUtils.toString(123));
        assertEquals("default", CastUtils.toString(null, "default"));
        assertEquals("default", CastUtils.toString("", "default"));
    }

    @Test
    void testToLong() {
        assertNull(CastUtils.toLong(null));
        assertEquals(123L, CastUtils.toLong(123));
        assertEquals(123L, CastUtils.toLong("123"));
        assertEquals(0L, CastUtils.toLong(null, 0L));
        assertEquals(123L, CastUtils.toLong("abc", 123L));
    }

    @Test
    void testToInt() {
        assertNull(CastUtils.toInt(null));
        assertEquals(42, CastUtils.toInt(42));
        assertEquals(42, CastUtils.toInt("42"));
        assertEquals(-1, CastUtils.toInt(null, -1));
    }

    @Test
    void testToDouble() {
        assertNull(CastUtils.toDouble(null));
        assertEquals(3.14, CastUtils.toDouble(3.14), 0.001);
        assertEquals(0.0, CastUtils.toDouble(null, 0.0));
    }

    @Test
    void testToBigDecimal() {
        assertNull(CastUtils.toBigDecimal(null));
        BigDecimal bd = CastUtils.toBigDecimal("123.45");
        assertNotNull(bd);
        assertEquals(0, new BigDecimal("123.45").compareTo(bd));
    }

    @Test
    void testToBoolean() {
        assertNull(CastUtils.toBoolean(null));
        assertTrue(CastUtils.toBoolean(true));
        assertTrue(CastUtils.toBoolean("true"));
        assertTrue(CastUtils.toBoolean("1"));
        assertTrue(CastUtils.toBoolean("yes"));
        assertFalse(CastUtils.toBoolean(false));
        assertFalse(CastUtils.toBoolean("false"));
        assertTrue(CastUtils.toBoolean(null, true));
        assertFalse(CastUtils.toBoolean(null, false));
    }

    @Test
    void testIsEmpty() {
        assertTrue(CastUtils.isEmpty(null));
        assertTrue(CastUtils.isEmpty(""));
        assertTrue(CastUtils.isEmpty("  "));
        assertTrue(CastUtils.isEmpty(Collections.emptyList()));
        assertTrue(CastUtils.isEmpty(new int[0]));

        assertFalse(CastUtils.isEmpty("hello"));
        assertFalse(CastUtils.isEmpty(0));
        assertFalse(CastUtils.isEmpty(List.of(1)));
    }

    @Test
    void testIsNotEmpty() {
        assertFalse(CastUtils.isNotEmpty(null));
        assertTrue(CastUtils.isNotEmpty("x"));
    }
}
