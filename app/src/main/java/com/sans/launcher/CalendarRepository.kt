package com.sans.launcher

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CalendarEventModel(
    val title: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val timeDisplay: String,
    val relativeDisplay: String
)

class CalendarRepository(private val context: Context) {

    private val timeFormatter24 = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val timeFormatter12 = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getNextUpcomingEventFlow(): Flow<CalendarEventModel?> = callbackFlow {
        fun queryAndEmit() {
            trySend(getNextUpcomingEvent())
        }

        queryAndEmit()

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                queryAndEmit()
            }
        }

        try {
            context.contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI,
                true,
                observer
            )
        } catch (_: Exception) { }

        // Periodic ticker to ensure relative time strings (e.g. "in 5m") update every minute
        val tickerJob = launch(Dispatchers.IO) {
            while (isActive) {
                delay(60_000L)
                queryAndEmit()
            }
        }

        awaitClose {
            try {
                context.contentResolver.unregisterContentObserver(observer)
            } catch (_: Exception) { }
            tickerJob.cancel()
        }
    }.flowOn(Dispatchers.IO)

    fun getNextUpcomingEvent(): CalendarEventModel? {
        if (!hasCalendarPermission()) return null

        val now = System.currentTimeMillis()
        val oneDayLater = now + (24 * 60 * 60 * 1000L)

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, oneDayLater)

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )

        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(
                builder.build(),
                projection,
                "${CalendarContract.Instances.END} > ?",
                arrayOf(now.toString()),
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            if (cursor != null && cursor.moveToFirst()) {
                val title = cursor.getString(0) ?: "Event"
                val begin = cursor.getLong(1)
                val end = cursor.getLong(2)
                val isAllDay = cursor.getInt(3) == 1

                val is24 = DateFormat.is24HourFormat(context)
                val formatter = if (is24) timeFormatter24 else timeFormatter12
                val startLdt = LocalDateTime.ofInstant(Instant.ofEpochMilli(begin), ZoneId.systemDefault())
                val timeFormatted = if (isAllDay) "All Day" else startLdt.format(formatter)
                val relative = formatRelativeTime(begin, now, isAllDay)

                CalendarEventModel(
                    title = title,
                    startTimeMillis = begin,
                    endTimeMillis = end,
                    timeDisplay = timeFormatted,
                    relativeDisplay = relative
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    fun openCalendarApp() {
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
        fun formatRelativeTime(begin: Long, now: Long, isAllDay: Boolean): String {
            if (isAllDay) return "Today"
            val minutesUntil = ((begin - now) / 60_000L).coerceAtLeast(0)
            return when {
                minutesUntil <= 1 -> "now"
                minutesUntil < 60 -> "in ${minutesUntil}m"
                else -> "in ${minutesUntil / 60}h ${minutesUntil % 60}m"
            }
        }
    }
}
