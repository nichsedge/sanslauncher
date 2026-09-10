package com.sans.launcher

import android.provider.CalendarContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

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

    @Test
    fun testSelectNextUpcomingEvent_PrioritizesTodayTimedOverTodayAllDay() {
        val zoneId = ZoneId.of("UTC")
        val todayLocalDate = LocalDate.of(2026, 9, 2)
        val now = todayLocalDate.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()

        val allDayBegin = todayLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val allDayEnd = todayLocalDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val timedBegin = todayLocalDate.atTime(14, 0).atZone(zoneId).toInstant().toEpochMilli()
        val timedEnd = todayLocalDate.atTime(15, 0).atZone(zoneId).toInstant().toEpochMilli()

        val events = listOf(
            CalendarInstanceData(
                title = "All Day Event",
                begin = allDayBegin,
                end = allDayEnd,
                isAllDay = true
            ),
            CalendarInstanceData(
                title = "Team Meeting",
                begin = timedBegin,
                end = timedEnd,
                isAllDay = false
            )
        )

        val result = CalendarRepository.selectNextUpcomingEvent(
            events = events,
            now = now,
            zoneId = zoneId,
            is24Hour = true
        )

        assertNotNull(result)
        assertEquals("Team Meeting", result?.title)
        assertEquals("14:00", result?.timeDisplay)
        assertEquals("in 4h", result?.relativeDisplay)
    }

    @Test
    fun testSelectNextUpcomingEvent_SelectsTodayAllDayWhenNoTimedEventsToday() {
        val zoneId = ZoneId.of("UTC")
        val todayLocalDate = LocalDate.of(2026, 9, 2)
        val now = todayLocalDate.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()

        val allDayBegin = todayLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val allDayEnd = todayLocalDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val events = listOf(
            CalendarInstanceData(
                title = "Holiday",
                begin = allDayBegin,
                end = allDayEnd,
                isAllDay = true
            )
        )

        val result = CalendarRepository.selectNextUpcomingEvent(
            events = events,
            now = now,
            zoneId = zoneId,
            is24Hour = true
        )

        assertNotNull(result)
        assertEquals("Holiday", result?.title)
        assertEquals("All Day", result?.timeDisplay)
        assertEquals("Today", result?.relativeDisplay)
    }

    @Test
    fun testSelectNextUpcomingEvent_IgnoresCanceledAndDeclinedEvents() {
        val zoneId = ZoneId.of("UTC")
        val todayLocalDate = LocalDate.of(2026, 9, 2)
        val now = todayLocalDate.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()

        val timedBegin = todayLocalDate.atTime(14, 0).atZone(zoneId).toInstant().toEpochMilli()
        val timedEnd = todayLocalDate.atTime(15, 0).atZone(zoneId).toInstant().toEpochMilli()

        val events = listOf(
            CalendarInstanceData(
                title = "Canceled Meeting",
                begin = timedBegin,
                end = timedEnd,
                isAllDay = false,
                status = CalendarContract.Instances.STATUS_CANCELED
            ),
            CalendarInstanceData(
                title = "Declined Meeting",
                begin = timedBegin,
                end = timedEnd,
                isAllDay = false,
                selfAttendeeStatus = CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED
            )
        )

        val result = CalendarRepository.selectNextUpcomingEvent(
            events = events,
            now = now,
            zoneId = zoneId,
            is24Hour = true
        )

        assertNull(result)
    }

    @Test
    fun testSelectNextUpcomingEvent_AcceptsTentativeAndNoneAttendeeStatus() {
        val zoneId = ZoneId.of("UTC")
        val todayLocalDate = LocalDate.of(2026, 9, 2)
        val now = todayLocalDate.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()

        val timedBegin = todayLocalDate.atTime(14, 0).atZone(zoneId).toInstant().toEpochMilli()
        val timedEnd = todayLocalDate.atTime(15, 0).atZone(zoneId).toInstant().toEpochMilli()

        val events = listOf(
            CalendarInstanceData(
                title = "Personal Reminder",
                begin = timedBegin,
                end = timedEnd,
                isAllDay = false,
                status = CalendarContract.Instances.STATUS_TENTATIVE,
                selfAttendeeStatus = CalendarContract.Attendees.ATTENDEE_STATUS_NONE
            )
        )

        val result = CalendarRepository.selectNextUpcomingEvent(
            events = events,
            now = now,
            zoneId = zoneId,
            is24Hour = true
        )

        assertNotNull(result)
        assertEquals("Personal Reminder", result?.title)
    }

    @Test
    fun testSelectNextUpcomingEvent_HandlesOngoingTimedEvent() {
        val zoneId = ZoneId.of("UTC")
        val todayLocalDate = LocalDate.of(2026, 9, 2)
        val now = todayLocalDate.atTime(14, 30).atZone(zoneId).toInstant().toEpochMilli()

        val timedBegin = todayLocalDate.atTime(14, 0).atZone(zoneId).toInstant().toEpochMilli()
        val timedEnd = todayLocalDate.atTime(15, 0).atZone(zoneId).toInstant().toEpochMilli()

        val events = listOf(
            CalendarInstanceData(
                title = "Ongoing Workshop",
                begin = timedBegin,
                end = timedEnd,
                isAllDay = false
            )
        )

        val result = CalendarRepository.selectNextUpcomingEvent(
            events = events,
            now = now,
            zoneId = zoneId,
            is24Hour = true
        )

        assertNotNull(result)
        assertEquals("Ongoing Workshop", result?.title)
        assertEquals("14:00", result?.timeDisplay)
        assertEquals("now", result?.relativeDisplay)
    }

    @Test
    fun testSelectNextUpcomingEvent_HandlesMultiDayAllDayEvent() {
        val zoneId = ZoneId.of("UTC")
        val todayLocalDate = LocalDate.of(2026, 9, 2)
        val now = todayLocalDate.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()

        val multiDayBegin = LocalDate.of(2026, 9, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val multiDayEnd = LocalDate.of(2026, 9, 4).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val events = listOf(
            CalendarInstanceData(
                title = "Vacation Trip",
                begin = multiDayBegin,
                end = multiDayEnd,
                isAllDay = true
            )
        )

        val result = CalendarRepository.selectNextUpcomingEvent(
            events = events,
            now = now,
            zoneId = zoneId,
            is24Hour = true
        )

        assertNotNull(result)
        assertEquals("Vacation Trip", result?.title)
        assertEquals("All Day", result?.timeDisplay)
        assertEquals("Today", result?.relativeDisplay)
    }

    @Test
    fun testSelectNextUpcomingEvent_SelectsTomorrowTimedWhenNoEventsToday() {
        val zoneId = ZoneId.of("UTC")
        val todayLocalDate = LocalDate.of(2026, 9, 2)
        val tomorrowLocalDate = todayLocalDate.plusDays(1)
        val now = todayLocalDate.atTime(20, 0).atZone(zoneId).toInstant().toEpochMilli()

        val tomorrowBegin = tomorrowLocalDate.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()
        val tomorrowEnd = tomorrowLocalDate.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()

        val events = listOf(
            CalendarInstanceData(
                title = "Tomorrow Morning Sync",
                begin = tomorrowBegin,
                end = tomorrowEnd,
                isAllDay = false
            )
        )

        val result = CalendarRepository.selectNextUpcomingEvent(
            events = events,
            now = now,
            zoneId = zoneId,
            is24Hour = true
        )

        assertNotNull(result)
        assertEquals("Tomorrow Morning Sync", result?.title)
        assertEquals("09:00", result?.timeDisplay)
        assertEquals("tomorrow in 13h", result?.relativeDisplay)
    }

    @Test
    fun testSelectNextUpcomingEvent_SelectsFutureTimedWhenNoEventsTodayOrTomorrow() {
        val zoneId = ZoneId.of("UTC")
        val todayLocalDate = LocalDate.of(2026, 9, 2) // Wednesday
        val futureLocalDate = LocalDate.of(2026, 9, 5) // Saturday (+3 days)
        val now = todayLocalDate.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()

        val futureBegin = futureLocalDate.atTime(14, 30).atZone(zoneId).toInstant().toEpochMilli()
        val futureEnd = futureLocalDate.atTime(15, 30).atZone(zoneId).toInstant().toEpochMilli()

        val events = listOf(
            CalendarInstanceData(
                title = "Weekend Conference",
                begin = futureBegin,
                end = futureEnd,
                isAllDay = false
            )
        )

        val result = CalendarRepository.selectNextUpcomingEvent(
            events = events,
            now = now,
            zoneId = zoneId,
            is24Hour = true
        )

        assertNotNull(result)
        assertEquals("Weekend Conference", result?.title)
        assertEquals("Sat 14:30", result?.timeDisplay)
        assertEquals("in 3d", result?.relativeDisplay)
    }

    @Test
    fun testSelectNextUpcomingEvent_SelectsFutureAllDayWhenNoOtherEvents() {
        val zoneId = ZoneId.of("UTC")
        val todayLocalDate = LocalDate.of(2026, 9, 2)
        val futureLocalDate = LocalDate.of(2026, 9, 6) // Sunday (+4 days)
        val now = todayLocalDate.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()

        val futureBegin = futureLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val futureEnd = futureLocalDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val events = listOf(
            CalendarInstanceData(
                title = "Family Reunion",
                begin = futureBegin,
                end = futureEnd,
                isAllDay = true
            )
        )

        val result = CalendarRepository.selectNextUpcomingEvent(
            events = events,
            now = now,
            zoneId = zoneId,
            is24Hour = true
        )

        assertNotNull(result)
        assertEquals("Family Reunion", result?.title)
        assertEquals("All Day", result?.timeDisplay)
        assertEquals("Sun, Sep 6", result?.relativeDisplay)
    }

    @Test
    fun testSelectNextUpcomingEvent_RelativeTimeUpdatesOnMinuteTickFromSameInstances() {
        val zoneId = ZoneId.of("UTC")
        val todayLocalDate = LocalDate.of(2026, 9, 2)
        val t0 = todayLocalDate.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()
        val eventStart = todayLocalDate.atTime(10, 15).atZone(zoneId).toInstant().toEpochMilli()
        val eventEnd = todayLocalDate.atTime(11, 0).atZone(zoneId).toInstant().toEpochMilli()

        val events = listOf(
            CalendarInstanceData(
                title = "Status Standup",
                begin = eventStart,
                end = eventEnd,
                isAllDay = false
            )
        )

        val at1000 = CalendarRepository.selectNextUpcomingEvent(events, t0, zoneId, true)
        assertEquals("in 15m", at1000?.relativeDisplay)

        // 10 minutes later (10 time ticks later from cached instances)
        val t10 = t0 + (10 * 60 * 1000L)
        val at1010 = CalendarRepository.selectNextUpcomingEvent(events, t10, zoneId, true)
        assertEquals("in 5m", at1010?.relativeDisplay)

        // At event start
        val atStart = CalendarRepository.selectNextUpcomingEvent(events, eventStart, zoneId, true)
        assertEquals("now", atStart?.relativeDisplay)
    }
}
