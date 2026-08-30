package com.sans.launcher

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class TimeAnchorHelperTest {

    @Test
    fun testMidnightProgressIsZero() {
        val midnight = LocalDateTime.of(2026, 8, 30, 0, 0, 0)
        val state = TimeAnchorHelper.calculateTimeState(midnight)
        assertEquals(0, state.dayProgressPercent)
        assertEquals(0.0f, state.dayProgressFraction, 0.001f)
        assertEquals("00:00", state.timeFormatted)
    }

    @Test
    fun testNoonProgressIsFiftyPercent() {
        val noon = LocalDateTime.of(2026, 8, 30, 12, 0, 0)
        val state = TimeAnchorHelper.calculateTimeState(noon)
        assertEquals(50, state.dayProgressPercent)
        assertEquals(0.5f, state.dayProgressFraction, 0.001f)
        assertEquals("12:00", state.timeFormatted)
    }

    @Test
    fun testEndOfDayProgressNearHundredPercent() {
        val endOfDay = LocalDateTime.of(2026, 8, 30, 23, 59, 59)
        val state = TimeAnchorHelper.calculateTimeState(endOfDay)
        assertEquals(99, state.dayProgressPercent)
        assertEquals("23:59", state.timeFormatted)
    }
}
