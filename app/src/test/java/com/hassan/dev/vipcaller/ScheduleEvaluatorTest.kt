package com.hassan.dev.vipcaller

import com.hassan.dev.vipcaller.core.Schedule
import com.hassan.dev.vipcaller.core.ScheduleEvaluator
import com.hassan.dev.vipcaller.core.ScheduleMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScheduleEvaluatorTest {

    private fun at(day: Int, hour: Int, minute: Int): Calendar =
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }

    private val allDays = setOf(1, 2, 3, 4, 5, 6, 7)

    @Test
    fun `always mode ignores time and days`() {
        val schedule = Schedule(mode = ScheduleMode.ALWAYS, days = emptySet())
        assertTrue(ScheduleEvaluator.isWithin(schedule, at(Calendar.FRIDAY, 3, 0)))
    }

    @Test
    fun `window includes start and excludes end`() {
        val schedule = Schedule(ScheduleMode.WINDOW, 8 * 60, 22 * 60, allDays)
        assertTrue(ScheduleEvaluator.isWithin(schedule, at(Calendar.MONDAY, 8, 0)))
        assertTrue(ScheduleEvaluator.isWithin(schedule, at(Calendar.MONDAY, 21, 59)))
        assertFalse(ScheduleEvaluator.isWithin(schedule, at(Calendar.MONDAY, 22, 0)))
        assertFalse(ScheduleEvaluator.isWithin(schedule, at(Calendar.MONDAY, 7, 59)))
    }

    @Test
    fun `window respects selected days`() {
        val schedule = Schedule(ScheduleMode.WINDOW, 8 * 60, 22 * 60, setOf(Calendar.MONDAY))
        assertTrue(ScheduleEvaluator.isWithin(schedule, at(Calendar.MONDAY, 12, 0)))
        assertFalse(ScheduleEvaluator.isWithin(schedule, at(Calendar.TUESDAY, 12, 0)))
    }

    @Test
    fun `overnight window carries into the next calendar day`() {
        // 22:00 ليلة الاثنين حتى 06:00 صباح الثلاثاء، واليوم المختار هو الاثنين.
        val schedule = Schedule(ScheduleMode.WINDOW, 22 * 60, 6 * 60, setOf(Calendar.MONDAY))

        assertTrue(ScheduleEvaluator.isWithin(schedule, at(Calendar.MONDAY, 23, 30)))
        assertTrue(ScheduleEvaluator.isWithin(schedule, at(Calendar.TUESDAY, 2, 0)))
        assertFalse(ScheduleEvaluator.isWithin(schedule, at(Calendar.TUESDAY, 7, 0)))
        assertFalse(ScheduleEvaluator.isWithin(schedule, at(Calendar.MONDAY, 12, 0)))
    }

    @Test
    fun `overnight window wraps from sunday back to saturday`() {
        val schedule = Schedule(ScheduleMode.WINDOW, 23 * 60, 5 * 60, setOf(Calendar.SATURDAY))
        assertTrue(ScheduleEvaluator.isWithin(schedule, at(Calendar.SUNDAY, 1, 0)))
        assertTrue(ScheduleEvaluator.isWithin(schedule, at(Calendar.SATURDAY, 23, 10)))
    }

    @Test
    fun `empty day selection disables the window`() {
        val schedule = Schedule(ScheduleMode.WINDOW, 0, 23 * 60 + 59, emptySet())
        assertFalse(ScheduleEvaluator.isWithin(schedule, at(Calendar.WEDNESDAY, 10, 0)))
    }

    @Test
    fun `formatMinutes pads to two digits`() {
        assertEquals("08:05", ScheduleEvaluator.formatMinutes(8 * 60 + 5))
        assertEquals("00:00", ScheduleEvaluator.formatMinutes(0))
        assertEquals("23:59", ScheduleEvaluator.formatMinutes(23 * 60 + 59))
    }
}
