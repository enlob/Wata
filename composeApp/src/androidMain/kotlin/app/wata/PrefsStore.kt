package app.wata

import android.content.Context
import androidx.core.content.edit
import app.wata.data.KeyValueStore

/** Writes are committed synchronously: they are tiny, and receivers may be killed right after returning. */
class PrefsStore(context: Context) : KeyValueStore {
    private val prefs = context.getSharedPreferences("wata", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String) = prefs.edit(commit = true) { putString(key, value) }
    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    override fun putInt(key: String, value: Int) = prefs.edit(commit = true) { putInt(key, value) }
    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)
    override fun putLong(key: String, value: Long) = prefs.edit(commit = true) { putLong(key, value) }
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = prefs.edit(commit = true) { putBoolean(key, value) }
}
