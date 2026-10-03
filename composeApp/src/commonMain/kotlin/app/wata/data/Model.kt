package app.wata.data

import kotlin.time.Instant

data class Drink(val at: Instant, val ml: Int)

data class WaterSettings(
    val dailyGoalMl: Int = 2000,
    val remindersOn: Boolean = true,
    val intervalMinutes: Int = 60,
    /** Reminders are only sent between these hours (local time). `endHour` may be 24 = midnight. */
    val startHour: Int = 8,
    val endHour: Int = 22,
)

data class WaterState(
    /** Today's drinks, oldest first. */
    val drinks: List<Drink>,
    val settings: WaterSettings,
    val nextReminder: Instant?,
) {
    val totalMl: Int get() = drinks.sumOf { it.ml }
    val progress: Float get() = totalMl.toFloat() / settings.dailyGoalMl
    val goalReached: Boolean get() = totalMl >= settings.dailyGoalMl
    val lastDrink: Drink? get() = drinks.lastOrNull()
}
