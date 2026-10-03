package app.wata.data

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Pure scheduling rules: when should the next reminder fire? */
object ReminderPlanner {

    /**
     * One interval after the last drink (or reminder), kept inside active hours.
     * Once the daily goal is reached, the next reminder is tomorrow morning.
     */
    fun next(
        now: Instant,
        lastActivity: Instant?,
        settings: WaterSettings,
        goalReached: Boolean,
        timeZone: TimeZone,
    ): Instant? {
        if (!settings.remindersOn) return null
        if (goalReached) {
            val tomorrow = now.toLocalDateTime(timeZone).date.plus(1, DateTimeUnit.DAY)
            return windowStart(tomorrow, settings, timeZone)
        }
        val interval = settings.intervalMinutes.minutes
        var candidate = (lastActivity ?: now) + interval
        if (candidate <= now) {
            // Nothing pending (missed alarm, reboot, new day): start a fresh cycle,
            // or wait for active hours to begin.
            candidate = if (isActive(now, settings, timeZone)) now + interval else now
        }
        return fitIntoWindow(candidate, settings, timeZone)
    }

    fun isActive(at: Instant, settings: WaterSettings, timeZone: TimeZone): Boolean =
        fitIntoWindow(at, settings, timeZone) == at

    private fun fitIntoWindow(at: Instant, settings: WaterSettings, timeZone: TimeZone): Instant {
        val day = at.toLocalDateTime(timeZone).date
        val start = windowStart(day, settings, timeZone)
        val end = if (settings.endHour >= 24) {
            day.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
        } else {
            day.atTime(settings.endHour, 0).toInstant(timeZone)
        }
        return when {
            at < start -> start
            at >= end -> windowStart(day.plus(1, DateTimeUnit.DAY), settings, timeZone)
            else -> at
        }
    }

    private fun windowStart(day: LocalDate, settings: WaterSettings, timeZone: TimeZone): Instant =
        day.atTime(settings.startHour, 0).toInstant(timeZone)
}
