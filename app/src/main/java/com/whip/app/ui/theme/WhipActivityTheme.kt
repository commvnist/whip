package com.whip.app.ui.theme

import android.graphics.Color
import android.os.Build
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat

/** Keeps Android window chrome in the same theme as the content, including recovery screens. */
@Composable
internal fun WhipActivityTheme(
    activity: ComponentActivity,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    SideEffect {
        applyWhipWindowAppearance(activity.window, darkTheme)
    }
    WhipTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

internal fun applyWhipWindowAppearance(window: Window, darkTheme: Boolean) {
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = !darkTheme
        isAppearanceLightNavigationBars = !darkTheme
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        // Match AndroidX edge-to-edge scrims on Android 8–9. Newer Android
        // versions retain their automatic navigation contrast protection.
        @Suppress("DEPRECATION")
        window.navigationBarColor = if (darkTheme) {
            Color.argb(128, 27, 27, 27)
        } else {
            Color.argb(230, 255, 255, 255)
        }
    }
}
