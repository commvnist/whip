package com.whip.app.data

import android.content.SharedPreferences

private const val SMART_TASK_CAPTURE_DEFAULT_MIGRATION = "smartTaskCaptureDefaultEnabledV2"

/** Enables the now-standard capture behavior once, then preserves later user choices. */
internal fun migrateSmartTaskCaptureDefault(preferences: SharedPreferences) {
    if (preferences.getBoolean(SMART_TASK_CAPTURE_DEFAULT_MIGRATION, false)) return

    preferences.edit()
        .putBoolean("naturalLanguageTaskCapture", true)
        .putBoolean(SMART_TASK_CAPTURE_DEFAULT_MIGRATION, true)
        .apply()
}
