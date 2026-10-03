package app.wata.data

import kotlin.time.Instant

/** Persistent key-value storage, implemented per platform. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
    fun getLong(key: String, default: Long): Long
    fun putLong(key: String, value: Long)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

/** Arms the platform's alarm for the next reminder and manages the reminder notification. */
interface ReminderScheduler {
    /** Replaces any pending reminder; `null` cancels it. */
    fun schedule(at: Instant?)
    fun dismissShown()
}
