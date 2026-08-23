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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import com.whip.app.core.AppThemeMode
import com.whip.app.core.WhipLaunchActions
import com.whip.app.ui.SettingsViewModel
import com.whip.app.ui.WhipApp
import com.whip.app.ui.WhipFoldInfo
import com.whip.app.ui.WhipFoldOrientation
import com.whip.app.ui.theme.WhipTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private data class LaunchRequest(
        val action: String?,
        val entityId: Long?,
        val occurrenceEpochDay: Long?,
        val sharedText: String?,
        val deliveryId: Long,
    )

    private var deliveryCounter = 0L
    private val launchRequest = mutableStateOf(LaunchRequest(null, null, null, null, 0L))

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            runCatching { (application as WhipApplication).locationReminderScheduler.syncAll() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyWidgetAreaScope(intent)
        launchRequest.value = intent.toWhipLaunchRequest()
        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebuggable && intent.getBooleanExtra(DEBUG_SHOW_WHEN_LOCKED, false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                enableLegacyTestLockScreenFlags()
            }
        }
        enableEdgeToEdge()
        setContent {
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
            val locationPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
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
                    onRequestLocationPermission = {
                        val permissions = buildList {
                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                add(Manifest.permission.ACCESS_FINE_LOCATION)
                                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                            if (
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        if (permissions.isNotEmpty()) locationPermission.launch(permissions.toTypedArray())
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyWidgetAreaScope(intent)
        launchRequest.value = intent.toWhipLaunchRequest()
    }

    private fun applyWidgetAreaScope(intent: android.content.Intent?) {
        intent?.getStringExtra(com.whip.app.widget.WhipWidgetProvider.EXTRA_AREA_SCOPE)?.let { storageKey ->
            (application as WhipApplication).settingsRepository.update { it.copy(activeAreaScope = storageKey) }
        }
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
        return LaunchRequest(action, id, occurrence, sharedText, ++deliveryCounter)
    }

    @Suppress("DEPRECATION")
    private fun enableLegacyTestLockScreenFlags() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
        )
    }

    private companion object {
        const val DEBUG_SHOW_WHEN_LOCKED = "commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED"
    }
}
