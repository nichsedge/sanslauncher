package com.sans.launcher

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

import androidx.compose.runtime.Immutable

@Immutable
data class CalendarEventModel(
    val title: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val timeDisplay: String,
    val relativeDisplay: String
)

data class CalendarInstanceData(
    val title: String,
    val begin: Long,
    val end: Long,
    val isAllDay: Boolean,
    val status: Int = CalendarContract.Instances.STATUS_CONFIRMED,
    val selfAttendeeStatus: Int = CalendarContract.Attendees.ATTENDEE_STATUS_NONE
)

class CalendarRepository(private val context: Context) {

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @Volatile
    private var cachedInstances: List<CalendarInstanceData>? = null

    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun refresh() {
        refreshTrigger.tryEmit(Unit)
    }

    fun getNextUpcomingEventFlow(): Flow<CalendarEventModel?> = callbackFlow {
        fun queryAndEmit(forceQuery: Boolean = false) {
            launch(Dispatchers.IO) {
                trySend(getNextUpcomingEvent(forceQuery = forceQuery))
            }
        }

        var observer: ContentObserver? = null

        fun tryRegisterObserver() {
            if (observer == null && hasCalendarPermission()) {
                val obs = object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        queryAndEmit(forceQuery = true)
                    }
                }
                try {
                    context.contentResolver.registerContentObserver(
                        CalendarContract.CONTENT_URI,
                        true,
                        obs
                    )
                    context.contentResolver.registerContentObserver(
                        CalendarContract.Events.CONTENT_URI,
                        true,
                        obs
                    )
                    observer = obs
                } catch (_: Exception) { }
            }
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                tryRegisterObserver()
                val isDateOrTzChanged = intent?.action == Intent.ACTION_DATE_CHANGED ||
                        intent?.action == Intent.ACTION_TIMEZONE_CHANGED
                queryAndEmit(forceQuery = isDateOrTzChanged)
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }

        try {
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Exception) {
            try {
                context.registerReceiver(receiver, filter)
            } catch (_: Exception) { }
        }

        tryRegisterObserver()
        queryAndEmit(forceQuery = true)

        val refreshJob = launch(Dispatchers.IO) {
            refreshTrigger.collect {
                tryRegisterObserver()
                queryAndEmit(forceQuery = true)
            }
        }

        awaitClose {
            observer?.let {
                try {
                    context.contentResolver.unregisterContentObserver(it)
                } catch (_: Exception) { }
            }
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) { }
            refreshJob.cancel()
        }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    fun queryCalendarInstances(): List<CalendarInstanceData> {
        if (!hasCalendarPermission()) return emptyList()

        val zoneId = ZoneId.systemDefault()
        val todayLocalDate = LocalDate.now(zoneId)

        val queryStart = todayLocalDate.minusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val queryEnd = todayLocalDate.plusDays(15).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, queryStart)
        ContentUris.appendId(builder, queryEnd)

        val instances = mutableListOf<CalendarInstanceData>()
        var cursor: Cursor? = null

        try {
            cursor = context.contentResolver.query(
                builder.build(),
                PROJECTION,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )
        } catch (_: Exception) {
            try {
                cursor = context.contentResolver.query(
                    builder.build(),
                    FALLBACK_PROJECTION,
                    null,
                    null,
                    "${CalendarContract.Instances.BEGIN} ASC"
                )
            } catch (_: Exception) {
                return emptyList()
            }
        }

        try {
            cursor?.use { c ->
                val titleIndex = c.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIndex = c.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIndex = c.getColumnIndex(CalendarContract.Instances.END)
                val allDayIndex = c.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val statusIndex = c.getColumnIndex(CalendarContract.Instances.STATUS)
                val selfAttendeeIndex = c.getColumnIndex(CalendarContract.Instances.SELF_ATTENDEE_STATUS)

                while (c.moveToNext()) {
                    val title = if (titleIndex != -1 && !c.isNull(titleIndex)) c.getString(titleIndex) else "Event"
                    val begin = if (beginIndex != -1 && !c.isNull(beginIndex)) c.getLong(beginIndex) else 0L
                    val end = if (endIndex != -1 && !c.isNull(endIndex)) c.getLong(endIndex) else begin
                    val isAllDay = if (allDayIndex != -1 && !c.isNull(allDayIndex)) c.getInt(allDayIndex) == 1 else false
                    val status = if (statusIndex != -1 && !c.isNull(statusIndex)) c.getInt(statusIndex) else CalendarContract.Instances.STATUS_CONFIRMED
                    val selfAttendeeStatus = if (selfAttendeeIndex != -1 && !c.isNull(selfAttendeeIndex)) c.getInt(selfAttendeeIndex) else CalendarContract.Attendees.ATTENDEE_STATUS_NONE

                    instances.add(
                        CalendarInstanceData(
                            title = title,
                            begin = begin,
                            end = end,
                            isAllDay = isAllDay,
                            status = status,
                            selfAttendeeStatus = selfAttendeeStatus
                        )
                    )
                }
            }
        } catch (_: Exception) {
            return emptyList()
        }

        cachedInstances = instances
        return instances
    }

    fun getNextUpcomingEvent(forceQuery: Boolean = false): CalendarEventModel? {
        if (!hasCalendarPermission()) return null

        val instances = if (forceQuery || cachedInstances == null) {
            queryCalendarInstances()
        } else {
            cachedInstances ?: queryCalendarInstances()
        }

        val now = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()
        val is24 = DateFormat.is24HourFormat(context)
        return selectNextUpcomingEvent(instances, now, zoneId, is24)
    }

    fun openCalendarApp(startTimeMillis: Long? = null) {
        if (startTimeMillis != null && startTimeMillis > 0) {
            try {
                val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
                ContentUris.appendId(builder, startTimeMillis)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = builder.build()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) { }
        }

        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_CALENDAR)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
                ContentUris.appendId(builder, System.currentTimeMillis())
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = builder.build()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) { }
        }
    }

    companion object {
        private val PROJECTION = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.STATUS,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS
        )

        private val FALLBACK_PROJECTION = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )

        private val TIME_FORMAT_24 = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        private val TIME_FORMAT_12 = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        private val DATE_TIME_FORMAT_24 = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.getDefault())
        private val DATE_TIME_FORMAT_12 = DateTimeFormatter.ofPattern("EEE h:mm a", Locale.getDefault())
        private val DATE_FORMAT_SHORT = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

        fun selectNextUpcomingEvent(
            events: List<CalendarInstanceData>,
            now: Long,
            zoneId: ZoneId = ZoneId.systemDefault(),
            is24Hour: Boolean = true
        ): CalendarEventModel? {
            val todayLocalDate = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
            val tomorrowLocalDate = todayLocalDate.plusDays(1)

            val timeFormatter = if (is24Hour) TIME_FORMAT_24 else TIME_FORMAT_12
            val dateTimeFormatter = if (is24Hour) DATE_TIME_FORMAT_24 else DATE_TIME_FORMAT_12

            var bestTodayTimed: CalendarEventModel? = null
            var bestTodayAllDay: CalendarEventModel? = null
            var bestTomorrowTimed: CalendarEventModel? = null
            var bestTomorrowAllDay: CalendarEventModel? = null
            var bestFutureTimed: CalendarEventModel? = null
            var bestFutureAllDay: CalendarEventModel? = null

            for (event in events) {
                if (event.status == CalendarContract.Instances.STATUS_CANCELED) continue
                if (event.selfAttendeeStatus == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED) continue

                if (event.isAllDay) {
                    val allDayStartDate = Instant.ofEpochMilli(event.begin).atZone(ZoneOffset.UTC).toLocalDate()
                    val allDayEndDate = Instant.ofEpochMilli(event.end).atZone(ZoneOffset.UTC).toLocalDate()

                    val isToday = (allDayStartDate == todayLocalDate) ||
                            (!todayLocalDate.isBefore(allDayStartDate) && todayLocalDate.isBefore(allDayEndDate))
                    val isTomorrow = (allDayStartDate == tomorrowLocalDate) ||
                            (!tomorrowLocalDate.isBefore(allDayStartDate) && tomorrowLocalDate.isBefore(allDayEndDate))
                    val isFuture = allDayStartDate.isAfter(tomorrowLocalDate)

                    if (isToday) {
                        if (bestTodayAllDay == null) {
                            bestTodayAllDay = CalendarEventModel(
                                title = event.title,
                                startTimeMillis = event.begin,
                                endTimeMillis = event.end,
                                timeDisplay = "All Day",
                                relativeDisplay = "Today"
                            )
                        }
                    } else if (isTomorrow) {
                        if (bestTomorrowAllDay == null) {
                            bestTomorrowAllDay = CalendarEventModel(
                                title = event.title,
                                startTimeMillis = event.begin,
                                endTimeMillis = event.end,
                                timeDisplay = "All Day",
                                relativeDisplay = "Tomorrow"
                            )
                        }
                    } else if (isFuture) {
                        if (bestFutureAllDay == null || event.begin < bestFutureAllDay.startTimeMillis) {
                            val daysUntil = allDayStartDate.toEpochDay() - todayLocalDate.toEpochDay()
                            bestFutureAllDay = CalendarEventModel(
                                title = event.title,
                                startTimeMillis = event.begin,
                                endTimeMillis = event.end,
                                timeDisplay = "All Day",
                                relativeDisplay = if (daysUntil <= 6) allDayStartDate.format(DATE_FORMAT_SHORT) else "in ${daysUntil}d"
                            )
                        }
                    }
                } else {
                    if (event.end > now) {
                        val startLdt = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.begin), zoneId)
                        val startLocalDate = startLdt.toLocalDate()
                        val isOngoing = event.begin <= now && event.end > now
                        val isToday = (startLocalDate == todayLocalDate) || isOngoing
                        val isTomorrow = startLocalDate == tomorrowLocalDate
                        val isFuture = startLocalDate.isAfter(tomorrowLocalDate)

                        val relative = formatRelativeTime(event.begin, now, false, zoneId)
                        val timeFormatted = if (isToday || isTomorrow) {
                            startLdt.format(timeFormatter)
                        } else {
                            startLdt.format(dateTimeFormatter)
                        }

                        val model = CalendarEventModel(
                            title = event.title,
                            startTimeMillis = event.begin,
                            endTimeMillis = event.end,
                            timeDisplay = timeFormatted,
                            relativeDisplay = relative
                        )

                        if (isToday) {
                            if (bestTodayTimed == null || event.begin < bestTodayTimed.startTimeMillis) {
                                bestTodayTimed = model
                            }
                        } else if (isTomorrow) {
                            if (bestTomorrowTimed == null || event.begin < bestTomorrowTimed.startTimeMillis) {
                                bestTomorrowTimed = model
                            }
                        } else if (isFuture) {
                            if (bestFutureTimed == null || event.begin < bestFutureTimed.startTimeMillis) {
                                bestFutureTimed = model
                            }
                        }
                    }
                }
            }

            return bestTodayTimed
                ?: bestTodayAllDay
                ?: bestTomorrowTimed
                ?: bestTomorrowAllDay
                ?: bestFutureTimed
                ?: bestFutureAllDay
        }

        fun formatRelativeTime(
            begin: Long,
            now: Long,
            isAllDay: Boolean,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): String {
            if (isAllDay) {
                val eventDate = Instant.ofEpochMilli(begin).atZone(ZoneOffset.UTC).toLocalDate()
                val todayDate = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
                return when {
                    eventDate == todayDate -> "Today"
                    eventDate == todayDate.plusDays(1) -> "Tomorrow"
                    eventDate.isAfter(todayDate) -> {
                        val days = eventDate.toEpochDay() - todayDate.toEpochDay()
                        "in ${days}d"
                    }
                    else -> "Today"
                }
            }

            if (begin <= now) {
                return "now"
            }

            val minutesUntil = (begin - now) / 60_000L
            if (minutesUntil <= 1) return "now"
            if (minutesUntil < 60) return "in ${minutesUntil}m"

            val startLdt = LocalDateTime.ofInstant(Instant.ofEpochMilli(begin), zoneId)
            val todayDate = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
            val eventDate = startLdt.toLocalDate()

            return if (eventDate == todayDate) {
                val hours = minutesUntil / 60
                val mins = minutesUntil % 60
                if (mins == 0L) "in ${hours}h" else "in ${hours}h ${mins}m"
            } else if (eventDate == todayDate.plusDays(1)) {
                val hours = minutesUntil / 60
                val mins = minutesUntil % 60
                if (mins == 0L) "tomorrow in ${hours}h" else "tomorrow in ${hours}h ${mins}m"
            } else {
                val days = eventDate.toEpochDay() - todayDate.toEpochDay()
                "in ${days}d"
            }
        }
    }
}
