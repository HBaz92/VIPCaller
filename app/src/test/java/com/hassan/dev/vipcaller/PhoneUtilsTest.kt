package com.hassan.dev.vipcaller

import com.hassan.dev.vipcaller.core.PhoneUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneUtilsTest {

    @Test
    fun `normalize strips formatting and country code`() {
        assertEquals("512345678", PhoneUtils.normalize("+966 51 234 5678"))
        assertEquals("512345678", PhoneUtils.normalize("0512345678"))
        assertEquals("512345678", PhoneUtils.normalize("(051) 234-5678"))
    }

    @Test
    fun `short numbers are kept as is`() {
        assertEquals("9200", PhoneUtils.normalize("9200"))
    }

    @Test
    fun `matches ignores country code and separators`() {
        assertTrue(PhoneUtils.matches("+966512345678", "0512345678"))
        assertTrue(PhoneUtils.matches("051-234-5678", "512345678"))
    }

    @Test
    fun `blank or different numbers do not match`() {
        assertFalse(PhoneUtils.matches(null, "0512345678"))
        assertFalse(PhoneUtils.matches("", ""))
        assertFalse(PhoneUtils.matches("0512345678", "0512345679"))
    }

    @Test
    fun `pretty collapses whitespace`() {
        assertEquals("+966 51 234 5678", PhoneUtils.pretty("  +966  51 234   5678 "))
    }

    // ------------------------------------------------- اتجاه العرض داخل واجهة عربية

    @Test
    fun `forDisplay wraps the number in directional isolates`() {
        val shown = PhoneUtils.forDisplay("+966 55 123 4567")
        assertEquals('⁦', shown.first())
        assertEquals('⁩', shown.last())
    }

    @Test
    fun `forDisplay keeps the digits in their original order`() {
        val raw = "+966 55 123 4567"
        assertEquals(raw, PhoneUtils.forDisplay(raw).trim('⁦', '⁩'))
    }

    @Test
    fun `forDisplay leaves blank input untouched`() {
        assertEquals("", PhoneUtils.forDisplay(""))
        assertEquals("   ", PhoneUtils.forDisplay("   "))
    }

    @Test
    fun `forDisplay output still normalises to the same number`() {
        // العازل يجب ألا يكسر المطابقة لو مرّ نص معروض عبر normalize.
        assertEquals(
            PhoneUtils.normalize("+966501234567"),
            PhoneUtils.normalize(PhoneUtils.forDisplay("+966501234567"))
        )
    }
}
