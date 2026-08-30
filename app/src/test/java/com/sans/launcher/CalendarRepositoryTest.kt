package com.sans.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarRepositoryTest {

    @Test
    fun testAllDayEventReturnsToday() {
        val now = 1000000L
        val result = CalendarRepository.formatRelativeTime(
            begin = now + 3600000L,
            now = now,
            isAllDay = true
        )
        assertEquals("Today", result)
    }

    @Test
    fun testEventStartingNowOrWithinOneMinuteReturnsNow() {
        val now = 1000000L
        val result = CalendarRepository.formatRelativeTime(
            begin = now + 30000L,
            now = now,
            isAllDay = false
        )
        assertEquals("now", result)
    }

    @Test
    fun testEventStartingInMinutesReturnsInMinutes() {
        val now = 1000000L
        val result = CalendarRepository.formatRelativeTime(
            begin = now + (15 * 60 * 1000L),
            now = now,
            isAllDay = false
        )
        assertEquals("in 15m", result)
    }

    @Test
    fun testEventStartingInHoursAndMinutesReturnsBoth() {
        val now = 1000000L
        val result = CalendarRepository.formatRelativeTime(
            begin = now + (135 * 60 * 1000L), // 2 hours 15 minutes
            now = now,
            isAllDay = false
        )
        assertEquals("in 2h 15m", result)
    }

    @Test
    fun testPastEventCoercesToNow() {
        val now = 1000000L
        val result = CalendarRepository.formatRelativeTime(
            begin = now - 5000L,
            now = now,
            isAllDay = false
        )
        assertEquals("now", result)
    }
}
