package com.gramayatri.app.util

import android.content.Context

/**
 * Lightweight wrapper around SharedPreferences for user settings.
 * Stores the user's display name used in pings.
 */
class UserPrefs(context: Context) {

    companion object {
        private const val PREFS_NAME = "grama_yatri_prefs"
        private const val KEY_NAME   = "user_display_name"
        private const val KEY_SETUP  = "setup_complete"
    }

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDisplayName(): String = prefs.getString(KEY_NAME, "Anonymous") ?: "Anonymous"

    fun setDisplayName(name: String) {
        prefs.edit().putString(KEY_NAME, name).apply()
    }

    fun isSetupComplete(): Boolean = prefs.getBoolean(KEY_SETUP, false)

    fun markSetupComplete() {
        prefs.edit().putBoolean(KEY_SETUP, true).apply()
    }
}
