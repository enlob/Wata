package app.wata.ui

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private fun formatClock(hour: Int, minute: Int?, use24h: Boolean): String {
    if (use24h) return "${hour.pad()}:${(minute ?: 0).pad()}"
    val h = (hour % 12).let { if (it == 0) 12 else it }
    val suffix = if (hour % 24 < 12) "AM" else "PM"
    return if (minute == null) "$h $suffix" else "$h:${minute.pad()} $suffix"
}

/** Whole hour: "08:00" or "8 AM". */
internal fun formatHour(hour: Int, use24h: Boolean): String = formatClock(hour, null, use24h)

internal fun formatTime(at: Instant, use24h: Boolean, tz: TimeZone = TimeZone.currentSystemDefault()): String {
    val t = at.toLocalDateTime(tz)
    return formatClock(t.hour, t.minute, use24h)
}

internal fun formatInterval(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    minutes % 60 == 30 -> "${minutes / 60}½ h"
    else -> "${minutes / 60} h ${minutes % 60}"
}

private fun Int.pad() = toString().padStart(2, '0')
