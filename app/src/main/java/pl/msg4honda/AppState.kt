package pl.msg4honda

import android.content.Context

object AppState {
    private const val PREFS = "msg4honda"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LAST_EVENT = "last_event"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun lastEvent(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_EVENT, "")
            .orEmpty()

    fun setLastEvent(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_EVENT, value)
            .apply()
    }

    fun isSourceEnabled(context: Context, source: MessageSource): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(source.preferenceKey, true)

    fun setSourceEnabled(context: Context, source: MessageSource, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(source.preferenceKey, enabled)
            .apply()
    }
}
