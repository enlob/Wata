package app.wata

import android.app.Application
import android.content.Context
import app.wata.data.WaterRepository

class WataApp : Application() {
    lateinit var repository: WaterRepository
        private set

    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.createChannel(this)
        repository = WaterRepository(PrefsStore(this), AlarmReminderScheduler(this))
    }
}

val Context.repository: WaterRepository
    get() = (applicationContext as WataApp).repository
