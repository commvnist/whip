package com.whip.app.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whip.app.core.AppSettings
import com.whip.app.core.PersistenceRequestState
import com.whip.app.core.zoneId
import com.whip.app.domain.ScheduledTask
import com.whip.app.reminders.FocusTimerNotifications
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class FocusTimerUiBindings(
    val busy: Boolean,
    val activeTaskTitle: String?,
    val activeDeadlineMillis: Long?,
    val content: (@Composable () -> Unit)?,
    val start: (Long, Int) -> Unit,
    private val errorTaskId: Long?,
    private val errorMessage: String?,
) {
    fun startErrorFor(taskId: Long): String? = errorMessage.takeIf { errorTaskId == taskId }
}

/** One request owner serves the Home card, Tasks card and Task inspector. */
@Composable
internal fun rememberFocusTimerUi(
    settingsViewModel: SettingsViewModel?,
    settings: AppSettings,
    allTasks: List<ScheduledTask>,
    tasksLoading: Boolean,
    onStarted: () -> Unit,
    onOpenTask: (ScheduledTask) -> Unit,
    onFeedback: (source: String, message: String, duration: SnackbarDuration) -> Unit,
): FocusTimerUiBindings {
    var requestKind by rememberSaveable { mutableStateOf<String?>(null) }
    var requestTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    val mutationState = settingsViewModel?.focusTimerMutationState?.collectAsStateWithLifecycle()?.value
        ?: PersistenceRequestState.Idle
    val coordinator = rememberPersistenceRequestCoordinator(
        state = mutationState,
        consume = { settingsViewModel?.consumeFocusTimerMutation(it) },
        key = "focus-timer",
        requestNamespace = "focus-timer",
        orphanedMessage = "The Focus timer change was interrupted. Check the active timer before retrying.",
        onPersisted = { receipt ->
            val started = receipt.started
            if (started != null) {
                onStarted()
                onFeedback(
                    "focus-timer",
                    "Focus started · ${started.minutes} min" +
                        started.schedulingWarnings.takeIf { it.isNotEmpty() }?.joinToString(" ", prefix = ". ").orEmpty(),
                    if (started.schedulingWarnings.isEmpty()) SnackbarDuration.Short else SnackbarDuration.Long,
                )
            } else {
                onFeedback(
                    "focus-timer",
                    if (receipt.stopped) "Focus timer stopped." else "That Focus timer already ended or changed.",
                    SnackbarDuration.Short,
                )
            }
        },
    )
    val latestFeedback by rememberUpdatedState(onFeedback)
    val completionScope = rememberCoroutineScope()
    LifecycleStartEffect(settingsViewModel) {
        val completionJob = completionScope.launch {
            settingsViewModel?.focusTimerCompletions?.collect { completed ->
                latestFeedback("focus-complete", "Focus complete for ${completed.taskTitle}. Review it when you're ready.", SnackbarDuration.Short)
            }
        }
        onStopOrDispose { completionJob.cancel() }
    }
    val deadline = settings.focusTimerDeadlineMillis
    val taskId = settings.focusTimerTaskId
    val nowMillis = rememberFocusTimerClock(deadline)
    val remainingSeconds = deadline?.let { focusTimerRemainingSeconds(it, nowMillis) }
    val focusedTask = allTasks.firstOrNull { it.task.id == taskId }
    val notificationState = focusTimerNotificationState()
    val context = LocalContext.current
    val content: (@Composable () -> Unit)? = if (taskId != null && deadline != null) {
        {
            FocusTimerCard(
                taskTitle = focusedTask?.task?.title ?: if (tasksLoading) "Loading Task…" else "Task unavailable",
                deadlineMillis = deadline,
                remainingSeconds = remainingSeconds ?: 0L,
                zoneId = settings.zoneId(),
                notificationState = notificationState,
                busy = coordinator.saving,
                errorMessage = coordinator.errorMessage.takeIf { requestKind == "stop" },
                onOpenTask = focusedTask?.let { item -> { onOpenTask(item) } },
                onStop = {
                    requestKind = "stop"
                    requestTaskId = taskId
                    val requestId = coordinator.begin()
                    if (requestId != null && settingsViewModel?.stopFocusTimer(requestId, taskId, deadline) != true) {
                        coordinator.finishFailure("Another Focus timer change is already finishing. Try again.")
                    }
                },
                onNotificationSettings = { openFocusTimerNotificationSettings(context) },
            )
        }
    } else null
    return FocusTimerUiBindings(
        busy = coordinator.saving,
        activeTaskTitle = focusedTask?.task?.title,
        activeDeadlineMillis = deadline,
        content = content,
        start = { requestedTaskId, minutes ->
            requestKind = "start"
            requestTaskId = requestedTaskId
            val requestId = coordinator.begin()
            if (requestId != null && settingsViewModel?.startFocusTimer(
                    requestId, requestedTaskId, minutes,
                    expectedActiveTaskId = taskId,
                    expectedActiveDeadlineMillis = deadline,
                ) != true
            ) coordinator.finishFailure("Another Focus timer change is already finishing. Try again.")
        },
        errorTaskId = requestTaskId.takeIf { requestKind == "start" },
        errorMessage = coordinator.errorMessage.takeIf { requestKind == "start" },
    )
}

/** Round upward so a newly started 30-minute timer begins at 30:00. */
internal fun focusTimerRemainingSeconds(deadlineMillis: Long, nowMillis: Long): Long {
    val remaining = (deadlineMillis - nowMillis).coerceAtLeast(0L)
    return remaining / 1_000L + if (remaining % 1_000L == 0L) 0L else 1L
}

internal fun formatFocusTimerDuration(seconds: Long): String {
    val safeSeconds = seconds.coerceAtLeast(0L)
    val hours = safeSeconds / 3_600L
    val minutes = safeSeconds % 3_600L / 60L
    val remainder = safeSeconds % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, remainder)
    else "%d:%02d".format(minutes, remainder)
}

@Composable
internal fun rememberFocusTimerClock(deadlineMillis: Long?): Long {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var resumeRevision by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumeRevision++
        onPauseOrDispose { }
    }
    LaunchedEffect(deadlineMillis, resumeRevision) {
        do {
            nowMillis = System.currentTimeMillis()
            if (deadlineMillis == null || deadlineMillis <= nowMillis) break
            delay(1_000L)
        } while (true)
    }
    return nowMillis
}

@Composable
internal fun focusTimerNotificationState(): NotificationDeliveryState {
    val context = LocalContext.current
    var resumeRevision by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumeRevision++
        onPauseOrDispose { }
    }
    return remember(context, resumeRevision) { readFocusTimerNotificationState(context) }
}

internal fun readFocusTimerNotificationState(context: Context): NotificationDeliveryState {
    val channel = context.getSystemService(NotificationManager::class.java)
        .getNotificationChannel(FocusTimerNotifications.channelId)
    return notificationDeliveryState(
        permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        configuredInWhip = channel != null,
        androidChannelEnabled = channel?.importance?.let { it != NotificationManager.IMPORTANCE_NONE } == true,
    )
}

internal fun openFocusTimerNotificationSettings(context: Context) {
    val channel = context.getSystemService(NotificationManager::class.java)
        .getNotificationChannel(FocusTimerNotifications.channelId)
    val channelBlocked = channel?.importance == NotificationManager.IMPORTANCE_NONE &&
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    context.startActivity(
        Intent(if (channelBlocked) Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS else Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .apply { if (channelBlocked) putExtra(Settings.EXTRA_CHANNEL_ID, FocusTimerNotifications.channelId) },
    )
}

@Composable
internal fun FocusTimerCard(
    taskTitle: String,
    deadlineMillis: Long,
    remainingSeconds: Long,
    zoneId: ZoneId,
    notificationState: NotificationDeliveryState,
    busy: Boolean,
    errorMessage: String?,
    onOpenTask: (() -> Unit)?,
    onStop: () -> Unit,
    onNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val finished = remainingSeconds <= 0L
    val finishLocalTime = Instant.ofEpochMilli(deadlineMillis).atZone(zoneId).toLocalTime()
    val finishTime = formatClockMinutes(LocalContext.current, finishLocalTime.hour * 60 + finishLocalTime.minute)
    Card(
        modifier = modifier.fillMaxWidth().testTag("active-focus-card").semantics {
            stateDescription = if (finished) "Focus timer complete" else "Focus timer running"
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compactMetadata = maxHeight < 400.dp || LocalConfiguration.current.screenHeightDp < 650
            val taskIdentity: @Composable (Modifier) -> Unit = { taskModifier ->
                Text(taskTitle, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = taskModifier.testTag("active-focus-task"))
            }
            val finishMetadata: @Composable () -> Unit = {
                Text(
                    if (finished) "Finished at $finishTime" else "Finishes at $finishTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("active-focus-finish-time"),
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = WhipSpacing.compact, vertical = WhipSpacing.sibling),
                verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
            ) {
                Text(
                    if (finished) "Focus complete" else "Focus · ${formatFocusTimerDuration(remainingSeconds)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("active-focus-countdown"),
                )
                if (compactMetadata) {
                    Row(
                        modifier = Modifier.fillMaxWidth().testTag("active-focus-metadata"),
                        horizontalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        taskIdentity(Modifier.weight(1f))
                        finishMetadata()
                    }
                } else {
                    taskIdentity(Modifier)
                    finishMetadata()
                }
                if (notificationState != NotificationDeliveryState.Deliverable) {
                    Text(
                        when (notificationState) {
                            NotificationDeliveryState.Blocked -> "Notification permission needed"
                            NotificationDeliveryState.OffInAndroid -> "Alerts off in Android"
                            NotificationDeliveryState.OffInWhip -> "Alerts unavailable"
                            NotificationDeliveryState.Deliverable -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("focus-alert-unavailable"),
                    )
                }
                errorMessage?.let { PersistenceFailureNotice(it, testTag = "focus-stop-error") }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(WhipSpacing.micro)) {
                    WhipTextButton(
                        enabled = onOpenTask != null && !busy,
                        onClick = { onOpenTask?.invoke() },
                        modifier = Modifier.testTag("focus-open-task").semantics { contentDescription = "Open focused Task $taskTitle" },
                    ) { Text("Open") }
                    WhipTextButton(
                        enabled = !busy,
                        onClick = onStop,
                        modifier = Modifier.testTag("focus-stop").semantics { contentDescription = if (finished) "Dismiss completed focus timer" else "Stop focus timer" },
                    ) { Text(if (busy) "Saving…" else if (finished) "Dismiss" else "Stop") }
                    if (notificationState != NotificationDeliveryState.Deliverable) {
                        WhipTextButton(onClick = onNotificationSettings, enabled = !busy,
                            modifier = Modifier.testTag("focus-notification-settings").semantics {
                                contentDescription = "Open Android notification settings for Focus alerts"
                            }) { Text("Alerts") }
                    }
                }
            }
        }
    }
}
