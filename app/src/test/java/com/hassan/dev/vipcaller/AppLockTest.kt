package com.hassan.dev.vipcaller

import com.hassan.dev.vipcaller.core.AppLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTest {

    @Test
    fun `accepts only four digits`() {
        assertTrue(AppLock.isValidPin("0000"))
        assertTrue(AppLock.isValidPin("9317"))
        assertFalse(AppLock.isValidPin("123"))
        assertFalse(AppLock.isValidPin("12345"))
        assertFalse(AppLock.isValidPin("12a4"))
        assertFalse(AppLock.isValidPin(""))
    }

    @Test
    fun `first four wrong attempts are not throttled`() {
        (1..4).forEach { assertEquals(0L, AppLock.lockoutMillisFor(it)) }
    }

    @Test
    fun `throttling starts at thirty seconds and doubles`() {
        assertEquals(30_000L, AppLock.lockoutMillisFor(5))
        assertEquals(60_000L, AppLock.lockoutMillisFor(6))
        assertEquals(120_000L, AppLock.lockoutMillisFor(7))
        assertEquals(240_000L, AppLock.lockoutMillisFor(8))
    }

    @Test
    fun `throttling is capped at five minutes`() {
        assertEquals(300_000L, AppLock.lockoutMillisFor(9))
        assertEquals(300_000L, AppLock.lockoutMillisFor(20))
    }

    @Test
    fun `a huge attempt count does not overflow into a short delay`() {
        // الإزاحة بعدد كبير قد تلتف وتعطي تأخيرًا صغيرًا لو لم تُقيَّد.
        (9..200).forEach { attempts ->
            assertEquals("attempts=$attempts", 300_000L, AppLock.lockoutMillisFor(attempts))
        }
    }
}
