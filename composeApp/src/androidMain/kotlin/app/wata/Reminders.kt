package app.wata

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.wata.data.ReminderScheduler
import app.wata.data.WaterState
import kotlin.time.Instant

/**
 * Exact, Doze-proof alarms so reminders arrive at the time shown in the app. Inexact alarms
 * may be delivered up to 75% of the delay late (45 min for an hourly reminder), so they're
 * only a fallback if exact alarms get revoked.
 */
class AlarmReminderScheduler(private val context: Context) : ReminderScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(at: Instant?) {
        val intent = ReminderReceiver.pendingIntent(context, ReminderReceiver.ACTION_REMIND)
        when {
            at == null -> alarmManager.cancel(intent)
            Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms() ->
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilliseconds(), intent)
            else -> alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilliseconds(), intent)
        }
    }

    override fun dismissShown() = ReminderNotifications.dismiss(context)
}

/** Handles our own alarms and the notification's "Drank a glass" action. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REMIND -> context.repository.onReminderAlarm()?.let { ReminderNotifications.show(context, it) }
            ACTION_DRINK -> context.repository.addDrink(GLASS_ML)
        }
    }

    companion object {
        const val ACTION_REMIND = "app.wata.action.REMIND"
        const val ACTION_DRINK = "app.wata.action.DRINK"
        const val GLASS_ML = 250

        fun pendingIntent(context: Context, action: String): PendingIntent = PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(context, ReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}

/** Re-arms the reminder after reboot, app update, or clock / time zone changes. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.repository.refresh()
    }
}

object ReminderNotifications {
    private const val CHANNEL_ID = "reminders"
    private const val NOTIFICATION_ID = 1

    private val titles = listOf(
        "Time for some water 💧",
        "Hydration break 💧",
        "A glass of water? 💧",
        "Your body will thank you 💧",
    )

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Drink reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Gentle reminders to drink water during the day"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean {
        val manager = NotificationManagerCompat.from(context)
        return manager.areNotificationsEnabled() &&
            manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    @SuppressLint("MissingPermission") // Checked by canNotify(): areNotificationsEnabled() is false without POST_NOTIFICATIONS.
    fun show(context: Context, state: WaterState) {
        if (!canNotify(context)) return
        val goal = state.settings.dailyGoalMl
        val text = if (state.totalMl == 0) {
            "Start with a glass. Today's goal: $goal ml."
        } else {
            "${state.totalMl} of $goal ml so far, ${goal - state.totalMl} ml to go."
        }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setColor(0xFF2F7FEA.toInt())
            .setContentTitle(titles.random())
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_stat_drop,
                "Drank a glass (${ReminderReceiver.GLASS_ML} ml)",
                ReminderReceiver.pendingIntent(context, ReminderReceiver.ACTION_DRINK),
            )
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }

    fun dismiss(context: Context) = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)

    const val PERMISSION = Manifest.permission.POST_NOTIFICATIONS
}
