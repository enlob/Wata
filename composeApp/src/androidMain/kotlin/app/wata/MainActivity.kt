package app.wata

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import app.wata.ui.AppPlatform

class MainActivity : ComponentActivity() {

    private val platform = AndroidPlatform()

    private var permissionRequestedAt = 0L

    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        platform.update()
        // An instant denial means the system didn't even show the dialog (permanently denied):
        // send the user to the app's notification settings instead.
        if (!granted && SystemClock.elapsedRealtime() - permissionRequestedAt < 400) openNotificationSettings()
    }

    private fun openNotificationSettings() {
        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        platform.update()
        setContent { App(repository, platform) }

        // Reminders are on by default, so ask once on first launch.
        val prefs = getSharedPreferences("wata", MODE_PRIVATE)
        if (savedInstanceState == null && !prefs.getBoolean(KEY_ASKED, false) && !platform.notificationsAllowed) {
            prefs.edit { putBoolean(KEY_ASKED, true) }
            platform.requestNotificationAccess()
        }
    }

    override fun onResume() {
        super.onResume()
        platform.update()
        repository.refresh()
    }

    private inner class AndroidPlatform : AppPlatform {
        override var notificationsAllowed by mutableStateOf(true)
            private set
        override var use24HourClock by mutableStateOf(true)
            private set

        fun update() {
            notificationsAllowed = ReminderNotifications.canNotify(this@MainActivity)
            use24HourClock = DateFormat.is24HourFormat(this@MainActivity)
        }

        override fun requestNotificationAccess() {
            val needsRuntimePermission = Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(ReminderNotifications.PERMISSION) != PackageManager.PERMISSION_GRANTED
            if (needsRuntimePermission) {
                permissionRequestedAt = SystemClock.elapsedRealtime()
                permissionRequest.launch(ReminderNotifications.PERMISSION)
            } else {
                // Notifications or the reminder channel were switched off in system settings.
                openNotificationSettings()
            }
        }
    }

    private companion object {
        const val KEY_ASKED = "asked_notifications"
    }
}
