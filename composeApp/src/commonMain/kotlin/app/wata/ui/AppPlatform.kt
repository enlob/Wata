package app.wata.ui

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDate

/** Platform capabilities the UI needs. Implementations expose Compose-observable state. */
@Stable
interface AppPlatform {
    val notificationsAllowed: Boolean
    val use24HourClock: Boolean

    /** Weekday, day and month in the user's language, e.g. "Sunday, 4 October" or "domenica 4 ottobre". */
    fun formatDate(date: LocalDate): String

    fun requestNotificationAccess()
}
