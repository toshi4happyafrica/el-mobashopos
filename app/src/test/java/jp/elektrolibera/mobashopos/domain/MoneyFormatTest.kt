package jp.elektrolibera.mobashopos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MoneyFormatTest {

    @Test
    fun parse_jpy() {
        assertEquals(1000L, MoneyFormat.parse("1,000", 0))
        assertEquals(1620L, MoneyFormat.parse(" 1620 ", 0))
    }

    @Test
    fun parse_usd_minor_units() {
        assertEquals(1250L, MoneyFormat.parse("12.5", 2))
        assertEquals(200L, MoneyFormat.parse("2", 2))
    }

    @Test
    fun parse_rejects_invalid() {
        assertNull(MoneyFormat.parse("", 0))
        assertNull(MoneyFormat.parse("abc", 0))
        assertNull(MoneyFormat.parse("-1", 0))
        assertNull(MoneyFormat.parse("1.5", 0)) // JPY に小数は不可
        assertNull(MoneyFormat.parse("1.234", 2))
    }

    @Test
    fun format_jpy_and_usd() {
        // 円記号は JDK/Android の版で全角・半角が異なるため数値部分で確認
        assertTrue(MoneyFormat.format(1240, "JPY", 0, Locale.JAPAN).endsWith("1,240"))
        assertEquals("$123.45", MoneyFormat.format(12345, "USD", 2, Locale.US))
    }
}
