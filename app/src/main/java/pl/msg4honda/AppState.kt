package pl.msg4honda

import android.content.Context

object AppState {
    private const val PREFS = "msg4honda"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LAST_EVENT = "last_event"
    private const val KEY_MAPS_DEBUG = "maps_debug"
    private const val KEY_MAPS_ACCESSIBILITY_DEBUG = "maps_accessibility_debug"
    private const val KEY_SPEED_LIMIT = "speed_limit"
    private const val KEY_SPEED_LIMIT_TIME = "speed_limit_time"
    private const val SPEED_LIMIT_MAX_AGE_MS = 60_000L

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

    fun mapsDebug(context: Context): String {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val notification = preferences.getString(KEY_MAPS_DEBUG, "").orEmpty()
        val accessibility = preferences.getString(KEY_MAPS_ACCESSIBILITY_DEBUG, "").orEmpty()
        return listOfNotNull(
            notification.takeIf(String::isNotBlank),
            accessibility.takeIf(String::isNotBlank)?.let {
                "=== ODCZYT EKRANU MAPS ===\n$it"
            },
        ).joinToString("\n\n")
    }

    fun setMapsDebug(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MAPS_DEBUG, value)
            .apply()
    }

    fun setMapsAccessibilityDebug(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MAPS_ACCESSIBILITY_DEBUG, value)
            .apply()
    }

    fun setSpeedLimit(context: Context, speedLimit: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_SPEED_LIMIT, speedLimit)
            .putLong(KEY_SPEED_LIMIT_TIME, System.currentTimeMillis())
            .apply()
    }

    fun speedLimit(context: Context): Int? {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val timestamp = preferences.getLong(KEY_SPEED_LIMIT_TIME, 0L)
        if (System.currentTimeMillis() - timestamp > SPEED_LIMIT_MAX_AGE_MS) return null
        return preferences.getInt(KEY_SPEED_LIMIT, 0).takeIf { it in 5..160 }
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
