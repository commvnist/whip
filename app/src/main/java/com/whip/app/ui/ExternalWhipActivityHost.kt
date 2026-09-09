package com.whip.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whip.app.WhipApplication
import com.whip.app.core.AppThemeMode
import com.whip.app.startup.StartupRecoveryState
import com.whip.app.ui.theme.WhipActivityTheme

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
        WhipActivityTheme(activity = activity) {
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
    WhipActivityTheme(activity = activity, darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
        WhipFullScreenSurface(title = title) { content() }
    }
}
