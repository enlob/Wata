package app.wata.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Single source of truth for today's intake and settings. Every change is persisted
 * immediately and re-arms the next reminder.
 */
class WaterRepository(
    private val store: KeyValueStore,
    private val reminders: ReminderScheduler,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    private val _state = MutableStateFlow(read())
    val state: StateFlow<WaterState> = _state.asStateFlow()

    fun addDrink(ml: Int) {
        writeDrinks(readDrinks() + Drink(clock.now(), ml))
        reminders.dismissShown()
        publish()
    }

    fun undoLastDrink() {
        val drinks = readDrinks()
        if (drinks.isEmpty()) return
        writeDrinks(drinks.dropLast(1))
        publish()
    }

    fun updateSettings(transform: (WaterSettings) -> WaterSettings) {
        val s = transform(readSettings())
        store.putInt(KEY_GOAL, s.dailyGoalMl)
        store.putBoolean(KEY_REMINDERS_ON, s.remindersOn)
        store.putInt(KEY_INTERVAL, s.intervalMinutes)
        store.putInt(KEY_START, s.startHour)
        store.putInt(KEY_END, s.endHour)
        publish()
    }

    /** Re-reads storage (handles the day rolling over) and re-arms the next reminder. */
    fun refresh() {
        publish()
    }

    /**
     * Called when a reminder alarm fires. Returns the state to show in the notification,
     * or `null` if this reminder should be skipped (goal reached, outside active hours, stale alarm).
     */
    fun onReminderAlarm(): WaterState? {
        val now = clock.now()
        val current = read()
        val s = current.settings
        val dueAt = lastActivity(current)?.plus(s.intervalMinutes.minutes)
        val due = s.remindersOn &&
            !current.goalReached &&
            ReminderPlanner.isActive(now, s, timeZone()) &&
            (dueAt == null || dueAt <= now + 1.minutes)
        if (due) store.putLong(KEY_LAST_REMINDER, now.toEpochMilliseconds())
        val published = publish()
        return if (due) published else null
    }

    private fun publish(): WaterState {
        val base = read()
        val next = ReminderPlanner.next(clock.now(), lastActivity(base), base.settings, base.goalReached, timeZone())
        reminders.schedule(next)
        return base.copy(nextReminder = next).also { _state.value = it }
    }

    private fun read() = WaterState(readDrinks(), readSettings(), nextReminder = null)

    private fun lastActivity(state: WaterState): Instant? {
        val lastReminder = store.getLong(KEY_LAST_REMINDER, 0L)
            .takeIf { it > 0 }
            ?.let(Instant::fromEpochMilliseconds)
        return listOfNotNull(state.lastDrink?.at, lastReminder).maxOrNull()
    }

    private fun readSettings(): WaterSettings {
        val d = WaterSettings()
        return WaterSettings(
            dailyGoalMl = store.getInt(KEY_GOAL, d.dailyGoalMl),
            remindersOn = store.getBoolean(KEY_REMINDERS_ON, d.remindersOn),
            intervalMinutes = store.getInt(KEY_INTERVAL, d.intervalMinutes),
            startHour = store.getInt(KEY_START, d.startHour),
            endHour = store.getInt(KEY_END, d.endHour),
        )
    }

    /** Only today's drinks; older entries are dropped on the next write. */
    private fun readDrinks(): List<Drink> {
        val tz = timeZone()
        val today = clock.now().toLocalDateTime(tz).date
        return store.getString(KEY_DRINKS).orEmpty()
            .split(",")
            .mapNotNull { entry ->
                val parts = entry.split(":")
                val at = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
                val ml = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                Drink(Instant.fromEpochMilliseconds(at), ml)
            }
            .filter { it.at.toLocalDateTime(tz).date == today }
    }

    private fun writeDrinks(drinks: List<Drink>) {
        store.putString(KEY_DRINKS, drinks.joinToString(",") { "${it.at.toEpochMilliseconds()}:${it.ml}" })
    }

    private companion object {
        const val KEY_DRINKS = "drinks"
        const val KEY_GOAL = "goal_ml"
        const val KEY_REMINDERS_ON = "reminders_on"
        const val KEY_INTERVAL = "interval_min"
        const val KEY_START = "start_hour"
        const val KEY_END = "end_hour"
        const val KEY_LAST_REMINDER = "last_reminder"
    }
}
