package com.cloudhub.platform.park.common.base.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IdUtils / DateUtils / ConvertUtil / I18nUtil 单元测试
 */
class UtilTest {

    // ========== IdUtils 测试 ==========

    @Test
    void snowflakeId_shouldReturn19DigitLong() {
        Long id = IdUtils.snowflakeId();
        assertNotNull(id);
        assertTrue(id > 0);
        assertTrue(String.valueOf(id).length() >= 18, "雪花 ID 应该是 19 位");
    }

    @Test
    void snowflakeId_shouldBeUnique() {
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(IdUtils.snowflakeId());
        }
        assertEquals(1000, ids.size(), "1000 个雪花 ID 应该全部唯一");
    }

    @Test
    void snowflakeIdStr_shouldReturnString() {
        String id = IdUtils.snowflakeIdStr();
        assertNotNull(id);
        assertTrue(id.length() >= 18);
    }

    @Test
    void uuid_shouldReturnStandardFormat() {
        String u = IdUtils.uuid();
        assertNotNull(u);
        assertEquals(36, u.length());
        assertTrue(u.contains("-"));
    }

    @Test
    void simpleUUID_shouldReturnCompactFormat() {
        String u = IdUtils.simpleUUID();
        assertNotNull(u);
        assertEquals(32, u.length());
        assertFalse(u.contains("-"));
    }

    // ========== DateUtils 测试 ==========

    @Test
    void now_shouldReturnCurrentTime() {
        LocalDateTime n = DateUtils.now();
        assertNotNull(n);
        assertTrue(n.getYear() >= 2026);
    }

    @Test
    void format_datetime_shouldUseStandardPattern() {
        LocalDateTime dt = LocalDateTime.of(2026, 6, 18, 10, 30, 45);
        assertEquals("2026-06-18 10:30:45", DateUtils.format(dt));
    }

    @Test
    void format_date_shouldUseDatePattern() {
        LocalDate d = LocalDate.of(2026, 6, 18);
        assertEquals("2026-06-18", DateUtils.format(d));
    }

    @Test
    void format_withCustomPattern() {
        LocalDateTime dt = LocalDateTime.of(2026, 6, 18, 10, 30, 45);
        assertEquals("2026/06/18", DateUtils.format(dt, "yyyy/MM/dd"));
    }

    @Test
    void parse_validString_shouldReturnLocalDateTime() {
        LocalDateTime dt = DateUtils.parse("2026-06-18 10:30:45");
        assertNotNull(dt);
        assertEquals(2026, dt.getYear());
        assertEquals(6, dt.getMonthValue());
        assertEquals(18, dt.getDayOfMonth());
    }

    @Test
    void parse_invalidString_shouldReturnNull() {
        assertNull(DateUtils.parse("not a date"));
        assertNull(DateUtils.parse(""));
        assertNull(DateUtils.parse(null));
    }

    @Test
    void formatCompact_shouldReturnCompactString() {
        LocalDateTime dt = LocalDateTime.of(2026, 6, 18, 10, 30, 45);
        assertEquals("20260618103045", DateUtils.formatCompact(dt));
    }

    // ========== ConvertUtil 测试 ==========

    @Test
    void toStr_withNull_shouldReturnDefault() {
        assertEquals("default", ConvertUtil.toStr(null, "default"));
    }

    @Test
    void toStr_withValue_shouldReturnToString() {
        assertEquals("42", ConvertUtil.toStr(42, "default"));
    }

    @Test
    void toLong_withValidString() {
        assertEquals(123L, ConvertUtil.toLong("123", 0L));
    }

    @Test
    void toLong_withInvalidString_shouldReturnDefault() {
        assertEquals(-1L, ConvertUtil.toLong("not a number", -1L));
    }

    @Test
    void toLong_withNumber() {
        assertEquals(42L, ConvertUtil.toLong(42, 0L));
        assertEquals(42L, ConvertUtil.toLong(42L, 0L));
    }

    @Test
    void toInt_withValidString() {
        assertEquals(100, ConvertUtil.toInt("100", 0));
    }

    @Test
    void toBool_truthy() {
        assertTrue(ConvertUtil.toBool("true", false));
        assertTrue(ConvertUtil.toBool("yes", false));
        assertTrue(ConvertUtil.toBool("1", false));
    }

    @Test
    void toBool_falsy() {
        assertFalse(ConvertUtil.toBool("false", true));
        assertFalse(ConvertUtil.toBool("no", true));
        assertFalse(ConvertUtil.toBool("0", true));
    }

    @Test
    void toBool_withNull_shouldReturnDefault() {
        assertTrue(ConvertUtil.toBool(null, true));
        assertFalse(ConvertUtil.toBool(null, false));
    }

    @Test
    void isEmpty_various() {
        assertTrue(ConvertUtil.isEmpty(null));
        assertTrue(ConvertUtil.isEmpty(""));
        assertTrue(ConvertUtil.isEmpty("   "));
        assertTrue(ConvertUtil.isEmpty(java.util.Collections.emptyList()));
        assertTrue(ConvertUtil.isEmpty(java.util.Collections.emptyMap()));
        assertTrue(ConvertUtil.isEmpty(new String[0]));

        assertFalse(ConvertUtil.isEmpty("x"));
        assertFalse(ConvertUtil.isEmpty(java.util.Arrays.asList(1, 2)));
    }

    // ========== I18nUtil 测试 ==========

    @Test
    void i18n_getMessage_withoutArgs() {
        String msg = I18nUtil.getMessage("room.not.found");
        assertEquals("room.not.found", msg);
    }

    @Test
    void i18n_getMessage_withArgs_shouldFormat() {
        String msg = I18nUtil.getMessage("room.not.found.id={}", new Object[]{42});
        assertTrue(msg.contains("42"), "格式化后应包含参数 42");
    }
}
