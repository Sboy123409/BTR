package com.example.btrapp.core.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.example.btrapp.core.DaysMask
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Current time for "locked until" style labels. Refreshes every [periodMs] and whenever any of
 * [invalidateOn] changes, so a rule that just started isn't judged against a stale clock.
 */
@Composable
fun rememberNow(vararg invalidateOn: Any?, periodMs: Long = 15_000): Long {
    val tick by produceState(0) {
        while (true) {
            delay(periodMs)
            value++
        }
    }
    return remember(tick, *invalidateOn) { System.currentTimeMillis() }
}

private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
private val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFormatter)

fun formatDateTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

fun formatMinuteOfDay(minute: Int): String =
    LocalTime.of(minute / 60, minute % 60).format(timeFormatter)

fun formatMinutes(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

private val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

fun dayLabel(isoDay: Int): String = dayLabels[isoDay - 1]

fun formatDays(mask: Int): String = when (mask) {
    DaysMask.EVERY_DAY -> "Every day"
    DaysMask.WEEKDAYS -> "Weekdays"
    DaysMask.WEEKENDS -> "Weekends"
    else -> (1..7).filter { DaysMask.isDayOn(mask, it) }.joinToString(", ") { dayLabel(it) }
}

/** Debug builds get extra 1-minute options so locks and grants can be tested quickly. */
fun Context.isDebuggable(): Boolean =
    applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
