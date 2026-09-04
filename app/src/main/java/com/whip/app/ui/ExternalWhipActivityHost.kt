package com.whip.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import com.whip.app.WhipApplication
import com.whip.app.core.AppThemeMode
import com.whip.app.startup.StartupRecoveryState
import com.whip.app.ui.theme.WhipTheme

/** Common visual host for external Android entry activities; feature behavior stays with each activity. */
@Composable
internal fun ExternalWhipActivityHost(
    activity: ComponentActivity,
    app: WhipApplication,
    title: String,
    content: @Composable () -> Unit,
) {
    val startupState = app.startupRecoveryState.collectAsStateWithLifecycle().value
    if (startupState != StartupRecoveryState.Ready) {
        WhipTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) {
            StartupRecoveryScreen(
                state = startupState,
                onRetry = app::retryStartupRecovery,
                onEraseAllData = app::beginFreshStartReset,
                onKeepDataAndClose = activity::finishAffinity,
            )
        }
        return
    }
    val settings = app.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = app.settingsRepository.current(),
    ).value
    val darkTheme = when (settings.themeMode) {
        AppThemeMode.System -> isSystemInDarkTheme()
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
    }
    SideEffect {
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
    WhipTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
        WhipFullScreenSurface(title = title) { content() }
    }
}
