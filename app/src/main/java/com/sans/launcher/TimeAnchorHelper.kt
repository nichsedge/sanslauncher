package com.sans.launcher

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TimeState(
    val timeFormatted: String,
    val dateFormatted: String,
    val dayProgressPercent: Int,
    val dayProgressFraction: Float
)

object TimeAnchorHelper {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())

    fun calculateTimeState(now: LocalDateTime = LocalDateTime.now()): TimeState {
        val totalSecondsInDay = 86400
        val currentSecondOfDay = now.hour * 3600 + now.minute * 60 + now.second
        val fraction = (currentSecondOfDay.toFloat() / totalSecondsInDay.toFloat()).coerceIn(0f, 1f)
        val percent = (fraction * 100).toInt().coerceIn(0, 100)

        return TimeState(
            timeFormatted = now.format(timeFormatter),
            dateFormatted = now.format(dateFormatter),
            dayProgressPercent = percent,
            dayProgressFraction = fraction
        )
    }

    /**
     * Emits current time state immediately and then ticks periodically every minute.
     */
    fun timeStateFlow(): Flow<TimeState> = flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(calculateTimeState(now))
            // Sleep until the start of the next minute
            val secondsUntilNextMinute = 60 - now.second
            delay(secondsUntilNextMinute * 1000L)
        }
    }
}
