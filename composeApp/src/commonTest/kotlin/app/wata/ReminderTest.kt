package app.wata

import app.wata.data.KeyValueStore
import app.wata.data.ReminderPlanner
import app.wata.data.ReminderScheduler
import app.wata.data.WaterRepository
import app.wata.data.WaterSettings
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

private val tz = TimeZone.UTC
private fun at(day: Int, hour: Int, minute: Int = 0): Instant = LocalDateTime(2026, 10, day, hour, minute).toInstant(tz)

class ReminderPlannerTest {
    private val settings = WaterSettings(intervalMinutes = 60, startHour = 8, endHour = 22)

    private fun next(now: Instant, last: Instant?, goalReached: Boolean = false, s: WaterSettings = settings) =
        ReminderPlanner.next(now, last, s, goalReached, tz)

    @Test fun oneIntervalAfterLastDrink() = assertEquals(at(3, 11), next(at(3, 10, 5), at(3, 10)))

    @Test fun lateDrinkRollsToNextMorning() = assertEquals(at(4, 8), next(at(3, 21, 35), at(3, 21, 30)))

    @Test fun beforeActiveHoursWaitsForStart() = assertEquals(at(3, 8), next(at(3, 6), at(2, 21)))

    @Test fun afterActiveHoursWaitsForTomorrow() = assertEquals(at(4, 8), next(at(3, 23), null))

    @Test fun overdueRestartsCycleFromNow() = assertEquals(at(3, 13), next(at(3, 12), at(3, 9)))

    @Test fun goalReachedSkipsToTomorrow() = assertEquals(at(4, 8), next(at(3, 15), at(3, 14), goalReached = true))

    @Test fun disabledMeansNoReminder() = assertNull(next(at(3, 12), null, s = settings.copy(remindersOn = false)))

    @Test fun endAtMidnightIsSupported() =
        assertEquals(at(3, 23, 30), next(at(3, 22, 45), at(3, 22, 30), s = settings.copy(endHour = 24)))
}

class WaterRepositoryTest {
    private class MemoryStore : KeyValueStore {
        val map = mutableMapOf<String, Any>()
        override fun getString(key: String) = map[key] as String?
        override fun putString(key: String, value: String) { map[key] = value }
        override fun getInt(key: String, default: Int) = map[key] as Int? ?: default
        override fun putInt(key: String, value: Int) { map[key] = value }
        override fun getLong(key: String, default: Long) = map[key] as Long? ?: default
        override fun putLong(key: String, value: Long) { map[key] = value }
        override fun getBoolean(key: String, default: Boolean) = map[key] as Boolean? ?: default
        override fun putBoolean(key: String, value: Boolean) { map[key] = value }
    }

    private class FakeScheduler : ReminderScheduler {
        var scheduled: Instant? = null
        var dismissed = 0
        override fun schedule(at: Instant?) { scheduled = at }
        override fun dismissShown() { dismissed++ }
    }

    private class FakeClock(var now: Instant) : Clock {
        override fun now() = now
    }

    private val store = MemoryStore()
    private val scheduler = FakeScheduler()
    private val clock = FakeClock(at(3, 10))
    private val repo = WaterRepository(store, scheduler, clock) { tz }

    @Test fun drinkingAddsUpAndReschedules() {
        repo.addDrink(250)
        repo.addDrink(500)
        assertEquals(750, repo.state.value.totalMl)
        assertEquals(at(3, 11), scheduler.scheduled)
        assertEquals(2, scheduler.dismissed)
    }

    @Test fun undoRemovesLastDrink() {
        repo.addDrink(250)
        repo.addDrink(500)
        repo.undoLastDrink()
        assertEquals(250, repo.state.value.totalMl)
    }

    @Test fun newDayStartsEmpty() {
        repo.addDrink(250)
        clock.now = at(4, 9)
        repo.refresh()
        assertEquals(0, repo.state.value.totalMl)
    }

    @Test fun reachingGoalPausesUntilTomorrow() {
        repo.updateSettings { it.copy(dailyGoalMl = 1000) }
        repeat(2) { repo.addDrink(500) }
        assertEquals(at(4, 8), scheduler.scheduled)
    }

    @Test fun reminderFiresWhenDue() {
        repo.addDrink(250)
        clock.now = at(3, 11, 2)
        assertNotNull(repo.onReminderAlarm())
        assertEquals(at(3, 12, 2), scheduler.scheduled)
    }

    @Test fun staleReminderIsSkipped() {
        repo.addDrink(250)
        clock.now = at(3, 10, 30)
        repo.addDrink(250)
        clock.now = at(3, 11)
        assertNull(repo.onReminderAlarm())
        assertEquals(at(3, 11, 30), scheduler.scheduled)
    }

    @Test fun settingsPersist() {
        repo.updateSettings { it.copy(intervalMinutes = 90, startHour = 9, endHour = 21) }
        val reloaded = WaterRepository(store, scheduler, clock) { tz }
        assertEquals(WaterSettings(intervalMinutes = 90, startHour = 9, endHour = 21), reloaded.state.value.settings)
    }
}
