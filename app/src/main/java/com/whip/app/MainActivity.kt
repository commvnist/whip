package com.whip.app

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import com.whip.app.core.AppThemeMode
import com.whip.app.core.WhipLaunchActions
import com.whip.app.ui.SettingsViewModel
import com.whip.app.ui.WhipApp
import com.whip.app.ui.WhipFoldInfo
import com.whip.app.ui.WhipFoldOrientation
import com.whip.app.ui.StartupRecoveryScreen
import com.whip.app.ui.theme.WhipTheme
import com.whip.app.startup.StartupRecoveryState
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    private data class LaunchRequest(
        val action: String?,
        val entityId: Long?,
        val occurrenceEpochDay: Long?,
        val sharedText: String?,
        val areaScopeStorageKey: String?,
        val deliveryId: Long,
    )

    private var deliveryCounter = 0L
    private val launchRequest = mutableStateOf(LaunchRequest(null, null, null, null, null, 0L))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deliveryCounter = savedInstanceState?.getLong(STATE_DELIVERY_COUNTER) ?: 0L
        launchRequest.value = savedInstanceState?.restoredLaunchRequest()
            ?: intent.toWhipLaunchRequest()
        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebuggable && intent.getBooleanExtra(DEBUG_SHOW_WHEN_LOCKED, false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
        }
        enableEdgeToEdge()
        setContent {
            WhipActivityContent()
        }
    }

    @Composable
    private fun WhipActivityContent() {
        val app = application as WhipApplication
        val startupState by app.startupRecoveryState.collectAsStateWithLifecycle()
        when (startupState) {
            StartupRecoveryState.Ready -> NormalWhipContent()
            StartupRecoveryState.Checking,
            is StartupRecoveryState.Blocked -> WhipTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) {
                StartupRecoveryScreen(
                    state = startupState,
                    onRetry = app::retryStartupRecovery,
                )
            }
        }
    }

    @Composable
    private fun NormalWhipContent() {
        val settingsViewModel: SettingsViewModel = viewModel()
        val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
        val windowLayoutFlow = remember {
            WindowInfoTracker.getOrCreate(this)
                .windowLayoutInfo(this)
                .map<WindowLayoutInfo, WindowLayoutInfo?> { it }
        }
        val windowLayoutInfo by windowLayoutFlow.collectAsStateWithLifecycle(initialValue = null)
        val foldInfo = remember(windowLayoutInfo) {
            windowLayoutInfo?.displayFeatures?.filterIsInstance<FoldingFeature>()?.firstOrNull()?.let { fold ->
                WhipFoldInfo(
                    orientation = if (fold.orientation == FoldingFeature.Orientation.VERTICAL) WhipFoldOrientation.Vertical else WhipFoldOrientation.Horizontal,
                    leftPx = fold.bounds.left,
                    topPx = fold.bounds.top,
                    rightPx = fold.bounds.right,
                    bottomPx = fold.bounds.bottom,
                    separating = fold.isSeparating,
                    halfOpened = fold.state == FoldingFeature.State.HALF_OPENED,
                )
            }
        }
        val notificationPermission = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { }
        val darkTheme = when (settingsState.settings.themeMode) {
            AppThemeMode.System -> isSystemInDarkTheme()
            AppThemeMode.Light -> false
            AppThemeMode.Dark -> true
        }
        WhipTheme(darkTheme = darkTheme, dynamicColor = settingsState.settings.dynamicColor) {
            val request = launchRequest.value
            WhipApp(
                initialAction = request.action,
                initialEntityId = request.entityId,
                initialOccurrenceEpochDay = request.occurrenceEpochDay,
                initialSharedText = request.sharedText,
                initialAreaScopeStorageKey = request.areaScopeStorageKey,
                initialDeliveryId = request.deliveryId,
                foldInfo = foldInfo,
                settingsViewModel = settingsViewModel,
                onRequestNotificationPermission = {
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        settingsViewModel.markNotificationPermissionRequested()
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(STATE_DELIVERY_COUNTER, deliveryCounter)
        launchRequest.value.saveTo(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchRequest.value = intent.toWhipLaunchRequest()
    }

    private fun android.content.Intent?.toWhipLaunchRequest(): LaunchRequest {
        val sourceAction = this?.action
        val action = if (sourceAction == android.content.Intent.ACTION_SEND && type == "text/plain") {
            WhipLaunchActions.ACTION_CAPTURE_SHARED_TASK
        } else sourceAction
        val id = if (action in setOf(
                WhipLaunchActions.ACTION_OPEN_TASK,
                WhipLaunchActions.ACTION_OPEN_HABIT,
                WhipLaunchActions.ACTION_OPEN_GOAL,
                WhipLaunchActions.ACTION_OPEN_GYM,
                WhipLaunchActions.ACTION_OPEN_TRACK,
            )
        ) this?.getLongExtra(WhipLaunchActions.EXTRA_ENTITY_ID, -1L)?.takeIf { it >= 0L } else null
        val occurrence = this?.getLongExtra(
            WhipLaunchActions.EXTRA_OCCURRENCE_EPOCH_DAY,
            Long.MIN_VALUE,
        )?.takeUnless { it == Long.MIN_VALUE }
        val sharedText = if (action == WhipLaunchActions.ACTION_CAPTURE_SHARED_TASK) {
            this?.getStringExtra(android.content.Intent.EXTRA_TEXT)
                ?.trim()
                ?.takeIf(String::isNotBlank)
        } else null
        val areaScopeStorageKey = this?.getStringExtra(
            com.whip.app.widget.WhipWidgetProvider.EXTRA_AREA_SCOPE,
        )
        return LaunchRequest(action, id, occurrence, sharedText, areaScopeStorageKey, ++deliveryCounter)
    }

    private fun LaunchRequest.saveTo(outState: Bundle) {
        outState.putLong(STATE_ACTIVE_DELIVERY_ID, deliveryId)
        outState.putString(STATE_ACTIVE_ACTION, action)
        entityId?.let { outState.putLong(STATE_ACTIVE_ENTITY_ID, it) }
        occurrenceEpochDay?.let { outState.putLong(STATE_ACTIVE_OCCURRENCE_DAY, it) }
        outState.putString(STATE_ACTIVE_SHARED_TEXT, sharedText)
        outState.putString(STATE_ACTIVE_AREA_SCOPE, areaScopeStorageKey)
    }

    private fun Bundle.restoredLaunchRequest(): LaunchRequest? {
        val deliveryId = getLong(STATE_ACTIVE_DELIVERY_ID).takeIf { it > 0L } ?: return null
        deliveryCounter = maxOf(deliveryCounter, deliveryId)
        return LaunchRequest(
            action = getString(STATE_ACTIVE_ACTION),
            entityId = getLong(STATE_ACTIVE_ENTITY_ID).takeIf { containsKey(STATE_ACTIVE_ENTITY_ID) },
            occurrenceEpochDay = getLong(STATE_ACTIVE_OCCURRENCE_DAY)
                .takeIf { containsKey(STATE_ACTIVE_OCCURRENCE_DAY) },
            sharedText = getString(STATE_ACTIVE_SHARED_TEXT),
            areaScopeStorageKey = getString(STATE_ACTIVE_AREA_SCOPE),
            deliveryId = deliveryId,
        )
    }

    private companion object {
        const val DEBUG_SHOW_WHEN_LOCKED = "commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED"
        const val STATE_DELIVERY_COUNTER = "whip.launch.delivery_counter"
        const val STATE_ACTIVE_DELIVERY_ID = "whip.launch.active_delivery_id"
        const val STATE_ACTIVE_ACTION = "whip.launch.active_action"
        const val STATE_ACTIVE_ENTITY_ID = "whip.launch.active_entity_id"
        const val STATE_ACTIVE_OCCURRENCE_DAY = "whip.launch.active_occurrence_day"
        const val STATE_ACTIVE_SHARED_TEXT = "whip.launch.active_shared_text"
        const val STATE_ACTIVE_AREA_SCOPE = "whip.launch.active_area_scope"
    }
}
