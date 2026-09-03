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
import com.whip.app.core.SharedTaskCapturePolicy
import com.whip.app.core.WhipLaunchActions
import com.whip.app.ui.SettingsViewModel
import com.whip.app.ui.WhipApp
import com.whip.app.ui.WhipFoldInfo
import com.whip.app.ui.WhipFoldOrientation
import com.whip.app.ui.StartupRecoveryScreen
import com.whip.app.ui.theme.WhipTheme
import java.util.ArrayDeque
import com.whip.app.startup.StartupRecoveryState
import kotlinx.coroutines.flow.map

internal data class LaunchRequest(
    val action: String?,
    val entityId: Long?,
    val occurrenceEpochDay: Long?,
    val sharedText: String?,
    val sharedTextShortened: Boolean,
    val areaScopeStorageKey: String?,
    val deliveryId: Long,
)

internal class LaunchRequestQueue(
    private val maxPendingSharedTaskRequests: Int,
) {
    private val requests = ArrayDeque<LaunchRequest>()

    fun enqueue(request: LaunchRequest, overflowRequest: () -> LaunchRequest) {
        val pendingSharedTaskCount = requests.count {
            it.action == WhipLaunchActions.ACTION_CAPTURE_SHARED_TASK
        }
        if (
            request.action != WhipLaunchActions.ACTION_CAPTURE_SHARED_TASK ||
            pendingSharedTaskCount < maxPendingSharedTaskRequests
        ) {
            requests.addLast(request)
            return
        }

        val existingOverflow = requests.firstOrNull {
            it.action == WhipLaunchActions.ACTION_SHARED_TASK_QUEUE_OVERFLOW
        }
        if (existingOverflow == null) {
            requests.addLast(overflowRequest())
            return
        }

        val updatedCount = existingOverflow.sharedText?.toIntOrNull()?.plus(1) ?: 1
        val replacement = existingOverflow.copy(sharedText = updatedCount.toString())
        val retained = requests.map { queued ->
            if (queued.deliveryId == existingOverflow.deliveryId) replacement else queued
        }
        requests.clear()
        requests.addAll(retained)
    }

    fun enqueueUnlessDataEpochGated(
        state: StartupRecoveryState,
        request: LaunchRequest,
        overflowRequest: () -> LaunchRequest,
    ): Boolean {
        if (state.blocksLaunchRequestsForDataEpoch()) {
            requests.clear()
            return false
        }
        enqueue(request, overflowRequest)
        return true
    }

    fun addAll(restored: Iterable<LaunchRequest>) {
        restored.forEach(requests::addLast)
    }

    fun consumeHead(deliveryId: Long): Boolean {
        if (requests.firstOrNull()?.deliveryId != deliveryId) return false
        requests.removeFirst()
        return true
    }

    fun firstOrNull(): LaunchRequest? = requests.firstOrNull()

    fun snapshot(): List<LaunchRequest> = requests.toList()

    fun clear() = requests.clear()
}

internal fun StartupRecoveryState.blocksLaunchRequestsForDataEpoch(): Boolean = when (this) {
    StartupRecoveryState.FreshStartChecking,
    is StartupRecoveryState.FreshStartCheckBlocked,
    StartupRecoveryState.FreshStartRequired,
    StartupRecoveryState.FreshStartResetting,
    is StartupRecoveryState.FreshStartBlocked,
    -> true
    else -> false
}

class MainActivity : ComponentActivity() {
    private var deliveryCounter = 0L
    private val pendingLaunchRequests = LaunchRequestQueue(MAX_PENDING_SHARED_TASK_REQUESTS)
    private val launchRequest = mutableStateOf(LaunchRequest(null, null, null, null, false, null, 0L))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deliveryCounter = savedInstanceState?.getLong(STATE_DELIVERY_COUNTER) ?: 0L
        if (savedInstanceState == null) {
            enqueueLaunchRequest(intent.toWhipLaunchRequest())
        } else {
            pendingLaunchRequests.addAll(savedInstanceState.restoredLaunchRequests())
            publishNextLaunchRequest()
        }
        if ((application as WhipApplication).startupRecoveryState.value.blocksLaunchRequestsForDataEpoch()) {
            pendingLaunchRequests.clear()
            publishNextLaunchRequest()
        }
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
            StartupRecoveryState.FreshStartChecking,
            is StartupRecoveryState.FreshStartCheckBlocked,
            StartupRecoveryState.FreshStartRequired,
            StartupRecoveryState.FreshStartResetting,
            is StartupRecoveryState.FreshStartBlocked,
            is StartupRecoveryState.Blocked -> WhipTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) {
                StartupRecoveryScreen(
                    state = startupState,
                    onRetry = app::retryStartupRecovery,
                    onEraseAllData = {
                        pendingLaunchRequests.clear()
                        publishNextLaunchRequest()
                        app.beginFreshStartReset()
                    },
                    onKeepDataAndClose = ::finishAffinity,
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
                initialSharedTextShortened = request.sharedTextShortened,
                initialAreaScopeStorageKey = request.areaScopeStorageKey,
                initialDeliveryId = request.deliveryId,
                onLaunchDeliveryConsumed = ::consumeLaunchRequest,
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
        outState.putParcelableArrayList(
            STATE_PENDING_LAUNCH_REQUESTS,
            ArrayList(
                pendingLaunchRequests.snapshot().map { request ->
                    Bundle().also { request.saveTo(it) }
                },
            ),
        )
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        acceptLaunchIntent(intent)
    }

    internal fun acceptLaunchIntent(intent: android.content.Intent) {
        setIntent(intent)
        enqueueLaunchRequest(intent.toWhipLaunchRequest())
    }

    private fun enqueueLaunchRequest(request: LaunchRequest) {
        pendingLaunchRequests.enqueueUnlessDataEpochGated(
            state = (application as WhipApplication).startupRecoveryState.value,
            request = request,
        ) {
            LaunchRequest(
                action = WhipLaunchActions.ACTION_SHARED_TASK_QUEUE_OVERFLOW,
                entityId = null,
                occurrenceEpochDay = null,
                sharedText = "1",
                sharedTextShortened = false,
                areaScopeStorageKey = null,
                deliveryId = ++deliveryCounter,
            )
        }
        publishNextLaunchRequest()
    }

    private fun consumeLaunchRequest(deliveryId: Long) {
        if (!pendingLaunchRequests.consumeHead(deliveryId)) return
        publishNextLaunchRequest()
    }

    private fun publishNextLaunchRequest() {
        launchRequest.value = pendingLaunchRequests.firstOrNull()
            ?: LaunchRequest(null, null, null, null, false, null, 0L)
    }

    private fun android.content.Intent?.toWhipLaunchRequest(): LaunchRequest {
        val sourceAction = this?.action
        val requestedAction = if (sourceAction == android.content.Intent.ACTION_SEND && type == "text/plain") {
            WhipLaunchActions.ACTION_CAPTURE_SHARED_TASK
        } else sourceAction
        val sharedCapture = if (requestedAction == WhipLaunchActions.ACTION_CAPTURE_SHARED_TASK) {
            SharedTaskCapturePolicy.bound(
                this?.getCharSequenceExtra(android.content.Intent.EXTRA_TEXT)?.toString(),
            )
        } else null
        val action = requestedAction.takeUnless {
            it == WhipLaunchActions.ACTION_CAPTURE_SHARED_TASK && sharedCapture == null
        }
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
        val areaScopeStorageKey = this?.getStringExtra(
            com.whip.app.widget.WhipWidgetProvider.EXTRA_AREA_SCOPE,
        )
        return LaunchRequest(
            action = action,
            entityId = id,
            occurrenceEpochDay = occurrence,
            sharedText = sharedCapture?.text,
            sharedTextShortened = sharedCapture?.wasShortened == true,
            areaScopeStorageKey = areaScopeStorageKey,
            deliveryId = ++deliveryCounter,
        )
    }

    private fun LaunchRequest.saveTo(outState: Bundle) {
        outState.putLong(STATE_ACTIVE_DELIVERY_ID, deliveryId)
        outState.putString(STATE_ACTIVE_ACTION, action)
        entityId?.let { outState.putLong(STATE_ACTIVE_ENTITY_ID, it) }
        occurrenceEpochDay?.let { outState.putLong(STATE_ACTIVE_OCCURRENCE_DAY, it) }
        outState.putString(STATE_ACTIVE_SHARED_TEXT, sharedText)
        outState.putBoolean(STATE_ACTIVE_SHARED_TEXT_SHORTENED, sharedTextShortened)
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
            sharedTextShortened = getBoolean(STATE_ACTIVE_SHARED_TEXT_SHORTENED),
            areaScopeStorageKey = getString(STATE_ACTIVE_AREA_SCOPE),
            deliveryId = deliveryId,
        )
    }

    @Suppress("DEPRECATION")
    private fun Bundle.restoredLaunchRequests(): List<LaunchRequest> {
        val restored = getParcelableArrayList<Bundle>(STATE_PENDING_LAUNCH_REQUESTS)
            ?.mapNotNull { savedRequest -> savedRequest.restoredLaunchRequest() }
            .orEmpty()
        return restored.ifEmpty { listOfNotNull(restoredLaunchRequest()) }
    }

    private companion object {
        const val DEBUG_SHOW_WHEN_LOCKED = "commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED"
        const val STATE_DELIVERY_COUNTER = "whip.launch.delivery_counter"
        const val STATE_PENDING_LAUNCH_REQUESTS = "whip.launch.pending_requests"
        const val STATE_ACTIVE_DELIVERY_ID = "whip.launch.active_delivery_id"
        const val STATE_ACTIVE_ACTION = "whip.launch.active_action"
        const val STATE_ACTIVE_ENTITY_ID = "whip.launch.active_entity_id"
        const val STATE_ACTIVE_OCCURRENCE_DAY = "whip.launch.active_occurrence_day"
        const val STATE_ACTIVE_SHARED_TEXT = "whip.launch.active_shared_text"
        const val STATE_ACTIVE_SHARED_TEXT_SHORTENED = "whip.launch.active_shared_text_shortened"
        const val STATE_ACTIVE_AREA_SCOPE = "whip.launch.active_area_scope"
        const val MAX_PENDING_SHARED_TASK_REQUESTS = 4
    }
}
