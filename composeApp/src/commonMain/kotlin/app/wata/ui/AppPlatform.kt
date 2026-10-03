package app.wata.ui

import androidx.compose.runtime.Stable

/** Platform capabilities the UI needs. Implementations expose Compose-observable state. */
@Stable
interface AppPlatform {
    val notificationsAllowed: Boolean
    val use24HourClock: Boolean
    fun requestNotificationAccess()
}
