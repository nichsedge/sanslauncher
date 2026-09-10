package com.sans.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.Immutable

@Immutable
data class TimeState(
    val timeFormatted: String,
    val dateFormatted: String
)

object TimeAnchorHelper {

    private val timeFormatter24 = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val timeFormatter12 = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())

    fun calculateTimeState(
        now: LocalDateTime = LocalDateTime.now(),
        is24Hour: Boolean = true
    ): TimeState {
        val formatter = if (is24Hour) timeFormatter24 else timeFormatter12

        return TimeState(
            timeFormatted = now.format(formatter),
            dateFormatted = now.format(dateFormatter)
        )
    }

    /**
     * Emits current time state immediately and updates on system time broadcasts.
     */
    fun timeStateFlow(context: Context): Flow<TimeState> = callbackFlow {
        fun emitCurrent() {
            val is24 = DateFormat.is24HourFormat(context)
            trySend(calculateTimeState(LocalDateTime.now(), is24))
        }

        emitCurrent()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                emitCurrent()
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) { }
        }
    }.distinctUntilChanged()
}
