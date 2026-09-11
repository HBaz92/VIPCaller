package com.hassan.dev.vipcaller

import com.hassan.dev.vipcaller.core.DeviceContact
import com.hassan.dev.vipcaller.core.SearchUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchUtilsTest {

    private fun contact(name: String, number: String) =
        DeviceContact(lookupKey = name, name = name, number = number, photoUri = null, systemRingtone = null)

    private val book = listOf(
        contact("أحمد العلي", "+966501111111"),
        contact("محمد بن سالم", "0502222222"),
        contact("فاطمة الزهراء", "0553333333"),
        contact("Sara Khan", "0544444444"),
        contact("خالد أحمد", "0565555555")
    )

    // ------------------------------------------------------------ the old bug

    @Test
    fun `name search does not match every contact`() {
        val result = SearchUtils.filter(book, "فاطمة")
        assertEquals(1, result.size)
        assertEquals("فاطمة الزهراء", result.first().name)
    }

    @Test
    fun `a query that matches nothing returns empty`() {
        assertTrue(SearchUtils.filter(book, "زززز").isEmpty())
    }

    // ------------------------------------------------------------- normalising

    @Test
    fun `alef variants are treated as the same letter`() {
        assertEquals(1, SearchUtils.filter(book, "احمد العلي").size)
        assertEquals(1, SearchUtils.filter(book, "أحمد العلي").size)
    }

    @Test
    fun `ta marbuta and ya variants are unified`() {
        assertEquals(1, SearchUtils.filter(book, "فاطمه").size)
        assertEquals(1, SearchUtils.filter(book, "الزهرا").size)
    }

    @Test
    fun `diacritics and tatweel are ignored`() {
        assertEquals(1, SearchUtils.filter(book, "مُحَمَّد").size)
        assertEquals(1, SearchUtils.filter(book, "محـــمد").size)
    }

    @Test
    fun `latin search is case insensitive`() {
        assertEquals(1, SearchUtils.filter(book, "sara").size)
        assertEquals(1, SearchUtils.filter(book, "KHAN").size)
    }

    // ------------------------------------------------------------------ number

    @Test
    fun `number search matches partial digits`() {
        val result = SearchUtils.filter(book, "3333")
        assertEquals(1, result.size)
        assertEquals("فاطمة الزهراء", result.first().name)
    }

    @Test
    fun `arabic indic digits match latin stored numbers`() {
        val result = SearchUtils.filter(book, "٢٢٢٢")
        assertEquals(1, result.size)
        assertEquals("محمد بن سالم", result.first().name)
    }

    @Test
    fun `number search does not leak unrelated names`() {
        assertNull(SearchUtils.rank("أحمد العلي", "+966501111111", "3333"))
    }

    // ----------------------------------------------------------------- ranking

    @Test
    fun `prefix matches rank above mid-name matches`() {
        // "أحمد العلي" يبدأ بالبحث، بينما "خالد أحمد" يطابق في كلمة لاحقة.
        val result = SearchUtils.filter(book, "أحمد")
        assertEquals(2, result.size)
        assertEquals("أحمد العلي", result[0].name)
        assertEquals("خالد أحمد", result[1].name)
    }

    @Test
    fun `blank query returns the full list unchanged`() {
        assertEquals(book, SearchUtils.filter(book, "   "))
    }

    @Test
    fun `whitespace inside the query is collapsed`() {
        assertEquals(1, SearchUtils.filter(book, "محمد   بن").size)
    }
}
