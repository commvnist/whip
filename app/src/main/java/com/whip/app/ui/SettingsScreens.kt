package com.whip.app.ui

import android.Manifest
import android.app.NotificationManager
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.first
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import com.whip.app.R
import com.whip.app.core.AppThemeMode
import com.whip.app.core.AreaOpeningMode
import com.whip.app.core.HomeSection
import com.whip.app.core.ReviewPeriod
import com.whip.app.core.zoneId
import com.whip.app.domain.RepeatStepPolicy
import com.whip.app.domain.CustomIdentityEmoji
import com.whip.app.domain.IDENTITY_EMOJI_PRESETS
import com.whip.app.domain.isDefaultIdentityEmoji
import com.whip.app.domain.isIdentityEmoji
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.CustomUnitBoundary
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.AreaScope
import com.whip.app.domain.customUnitBoundary
import com.whip.app.domain.toUnitDefinition
import com.whip.app.domain.toWhipDoubleOrNull
import com.whip.app.reminders.ReminderNotifications
import com.whip.app.reminders.HabitReminderNotifications
import com.whip.app.reminders.GoalReminderNotifications
import com.whip.app.reminders.RestTimerNotifications
import com.whip.app.reminders.FocusTimerNotifications
import com.whip.app.reminders.canScheduleExactReminderAlarms
import com.whip.app.BuildConfig
import com.whip.app.data.BackupPreview
import com.whip.app.core.OperationStatus
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.whip.app.core.AppSettings
import com.whip.app.core.PersistenceRequestState
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID

internal enum class SettingsSection(val label: String, val supportingText: String) {
    Appearance("Appearance & Home", "Theme, presentation, home sections, and keyboard shortcuts"),
    Planning("Planning & Units", "Dates, units, numbers, tasks, habits, and review defaults"),
    Gym("Gym", "Rest timers, workout inputs, and progress calculations"),
    Organization("Organization", "Areas, tags, and naming systems"),
    Reminders("Reminders", "Notification access, delivery status, testing, and quiet hours"),
    DataPrivacy("Data & Privacy", "Local data, backups, restore, export, and deletion"),
    AboutDiagnostics("About Whip", "App identity, version, package, and data-handling summary"),
}

internal enum class DataPrivacyGroup { Backup, Reset }

internal val DataPrivacyGroupOrder = listOf(
    DataPrivacyGroup.Backup,
    DataPrivacyGroup.Reset,
)

internal fun supportsAndroidDynamicColor(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.S

private val LocalSettingsTypedEditorState = staticCompositionLocalOf<(String, Boolean) -> Unit> {
    { _, _ -> }
}

internal data class TypedSettingMutation<T>(
    val state: PersistenceRequestState<SettingsMutationReceipt>,
    val consume: (String) -> Unit,
    val submit: (requestId: String, value: T) -> Boolean,
    val onCompletedWarnings: (List<String>) -> Unit = {},
)

internal fun submitDestructiveActionOnce(
    alreadySubmitted: Boolean,
    busy: Boolean,
    markSubmitted: () -> Unit,
    action: () -> Unit,
): Boolean {
    if (alreadySubmitted || busy) return false
    markSubmitted()
    action()
    return true
}

@Composable
internal fun SettingsContent(
    state: SettingsUiState,
    innerPadding: PaddingValues,
    viewModel: SettingsViewModel,
    onEditAreas: () -> Unit = {},
    onEditTags: () -> Unit = {},
    onDataReset: () -> Unit = {},
    selectedSection: SettingsSection? = null,
    onSectionChange: (SettingsSection) -> Unit = {},
    externalSearchAction: Boolean = false,
    searchRequested: Boolean = false,
    onSearchRequestConsumed: () -> Unit = {},
    onSearchAvailabilityChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val weekdayFormatter = rememberWhipWeekdayFormatter()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var resetSubmitted by rememberSaveable { mutableStateOf(false) }
    var createUnit by rememberSaveable { mutableStateOf(false) }
    var createUnitTargetId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var renameUnitBoundary by rememberSaveable { mutableStateOf<CustomUnitBoundary?>(null) }
    var versionUnitBoundary by rememberSaveable { mutableStateOf<CustomUnitBoundary?>(null) }
    var versionUnitTargetId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var customUnitPendingAction by rememberSaveable { mutableStateOf<String?>(null) }
    var typedSettingWarning by rememberSaveable { mutableStateOf<String?>(null) }
    var customEmojiEditorOpen by rememberSaveable { mutableStateOf(false) }
    var customEmojiEditorOriginal by rememberSaveable { mutableStateOf<String?>(null) }
    var diagnosticRefresh by rememberSaveable { mutableIntStateOf(0) }
    var notificationTestMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var notificationTestSucceeded by rememberSaveable { mutableStateOf(false) }
    var notificationRefreshMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var notificationRefreshWarning by rememberSaveable { mutableStateOf(false) }
    var restPresetsOpen by rememberSaveable { mutableStateOf(false) }
    var customUnitsExpanded by rememberSaveable { mutableStateOf(false) }
    var captureExamplesExpanded by rememberSaveable { mutableStateOf(false) }
    var deliveryDetailsExpanded by rememberSaveable { mutableStateOf(false) }
    var notificationTroubleshootingExpanded by rememberSaveable { mutableStateOf(false) }
    var backupFolderExpanded by rememberSaveable { mutableStateOf(false) }
    var csvExportsExpanded by rememberSaveable { mutableStateOf(false) }
    var showEncryptedExport by rememberSaveable { mutableStateOf(false) }
    var exportPassphrase by remember { mutableStateOf("") }
    var exportPassphraseConfirmation by remember { mutableStateOf("") }
    var restorePassphrase by remember { mutableStateOf("") }
    var localSection by rememberSaveable { mutableStateOf(SettingsSection.Appearance) }
    var activeTypedSettingTag by rememberSaveable { mutableStateOf<String?>(null) }
    val section = selectedSection ?: localSection
    val settingsListState = rememberSaveable(section, saver = LazyListState.Saver) { LazyListState() }
    var observedOperation by remember { mutableStateOf(state.operation) }
    LaunchedEffect(state.operation) {
        val operationChanged = observedOperation != state.operation
        observedOperation = state.operation
        // The existing card owns general Data & Privacy feedback. Reveal new work/results,
        // including fast operations whose Running state never reached a rendered frame.
        // Opening a section or restoring an old message does not move its saved position.
        if (operationChanged && section == SettingsSection.DataPrivacy &&
            state.backupPreview == null && !state.encryptedRestorePending && !confirmDelete &&
            !showEncryptedExport && activeTypedSettingTag == null && (state.busy || state.message != null)
        ) {
            settingsListState.scrollToItem(0)
        }
    }
    LifecycleResumeEffect(Unit) {
        diagnosticRefresh++
        onPauseOrDispose { }
    }
    val externalSectionNavigation = selectedSection != null
    fun selectSection(next: SettingsSection) {
        if (activeTypedSettingTag != null) return
        if (externalSectionNavigation) onSectionChange(next) else localSection = next
    }
    var compactSectionOpen by rememberSaveable { mutableStateOf(false) }
    var settingsSearchOpen by rememberSaveable { mutableStateOf(false) }
    var settingsSearchQuery by rememberSaveable { mutableStateOf("") }
    var pendingSettingsAnchor by rememberSaveable { mutableStateOf<String?>(null) }
    val settingsAnchorPositions = remember(section) { mutableMapOf<String, Int>() }
    val canSearchSettings = activeTypedSettingTag == null && !state.busy && !confirmDelete &&
        state.backupPreview == null && !state.encryptedRestorePending && !showEncryptedExport
    SideEffect { onSearchAvailabilityChange(canSearchSettings) }
    LaunchedEffect(searchRequested) {
        if (searchRequested) {
            if (canSearchSettings) settingsSearchOpen = true
            onSearchRequestConsumed()
        }
    }
    LaunchedEffect(pendingSettingsAnchor, section, compactSectionOpen) {
        val anchor = pendingSettingsAnchor ?: return@LaunchedEffect
        if (SettingsSearchEntries.firstOrNull { it.anchor == anchor }?.section != section) return@LaunchedEffect
        snapshotFlow { settingsListState.layoutInfo.totalItemsCount }.first { it > 0 && anchor in settingsAnchorPositions }
        withFrameNanos { }
        settingsAnchorPositions[anchor]?.let { settingsListState.scrollToItem(it) }
        pendingSettingsAnchor = null
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        diagnosticRefresh++
    }
    val exactAlarmAccess = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        diagnosticRefresh++
    }
    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        viewModel.completeDocumentExport(uri)
    }
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::previewRestore)
    }
    val backupFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(viewModel::configurePortableBackupFolder)
    }
    val settings = state.settings
    val typedSettingMutationState by viewModel.typedSettingMutationState.collectAsStateWithLifecycle()
    val notificationRefreshCoordinator = rememberPersistenceRequestCoordinator(
        state = typedSettingMutationState,
        consume = viewModel::consumeTypedSettingMutation,
        key = "settings-reminder-refresh",
        requestNamespace = "settings-reminder-refresh",
        onPersisted = { receipt ->
            diagnosticRefresh++
            notificationRefreshWarning = receipt.warnings.isNotEmpty()
            notificationRefreshMessage = receipt.warnings.joinToString(" ").takeIf(String::isNotBlank)
                ?: "Task, Habit, and Goal reminder schedules refreshed. Android notification availability is shown above."
            typedSettingWarning = null
        },
        orphanedMessage = "The previous reminder refresh was interrupted. Refresh Notification Status to retry.",
    )
    val customUnitMutationState by viewModel.customUnitMutationState.collectAsStateWithLifecycle()
    val customUnitCoordinator = rememberPersistenceRequestCoordinator(
        state = customUnitMutationState,
        consume = viewModel::consumeCustomUnitMutation,
        key = "settings-custom-units",
        requestNamespace = "settings-custom-unit",
        onPersisted = {
            when (customUnitPendingAction) {
                "create" -> {
                    createUnit = false
                    createUnitTargetId = UUID.randomUUID().toString()
                }
                "rename" -> renameUnitBoundary = null
                "version" -> {
                    versionUnitBoundary = null
                    versionUnitTargetId = UUID.randomUUID().toString()
                }
            }
            customUnitPendingAction = null
        },
        orphanedMessage =
            "The previous custom-unit change was interrupted. Review the unit's current name, version, and archive state before trying again. Do not repeat Archive or Restore blindly.",
    )
    val restPresetsCoordinator = rememberPersistenceRequestCoordinator(
        state = typedSettingMutationState,
        consume = viewModel::consumeTypedSettingMutation,
        key = "settings-rest-presets",
        requestNamespace = "settings-rest-presets",
        onPersisted = { receipt ->
            restPresetsOpen = false
            typedSettingWarning = receipt.warnings.joinToString(" ").takeIf(String::isNotBlank)
        },
        orphanedMessage = "The previous preset save was interrupted. Review your shortcuts and retry.",
    )
    val quietHoursCoordinator = rememberPersistenceRequestCoordinator(
        state = typedSettingMutationState,
        consume = viewModel::consumeTypedSettingMutation,
        key = "settings-quiet-hours-toggle",
        requestNamespace = "settings-quiet-hours-toggle",
        onPersisted = { receipt ->
            typedSettingWarning = receipt.warnings.joinToString(" ").takeIf(String::isNotBlank)
        },
        orphanedMessage = "The previous quiet-hours change was interrupted. Review the current schedule and retry.",
    )
    val timeZoneModeCoordinator = rememberPersistenceRequestCoordinator(
        state = typedSettingMutationState,
        consume = viewModel::consumeTypedSettingMutation,
        key = "settings-follow-device-time-zone",
        requestNamespace = "settings-follow-device-time-zone",
        onPersisted = { receipt ->
            typedSettingWarning = receipt.warnings.joinToString(" ").takeIf(String::isNotBlank)
        },
        orphanedMessage = "The previous time-zone mode change was interrupted. Review the active time zone and retry.",
    )
    fun submitCustomUnitAction(action: String, submit: (String) -> Boolean) {
        val requestId = customUnitCoordinator.begin() ?: return
        customUnitPendingAction = action
        if (!submit(requestId)) {
            customUnitPendingAction = null
            customUnitCoordinator.finishFailure("Another custom-unit change is still finishing. Review it and try again.")
        }
    }
    fun <T> appSettingMutation(
        transform: (AppSettings, T) -> AppSettings,
    ): TypedSettingMutation<T> = TypedSettingMutation(
        state = typedSettingMutationState,
        consume = viewModel::consumeTypedSettingMutation,
        submit = { requestId, value ->
            viewModel.updateTypedSetting(requestId) { current -> transform(current, value) }
        },
        onCompletedWarnings = { warnings ->
            typedSettingWarning = warnings.joinToString(" ").takeIf(String::isNotBlank)
        },
    )
    // Observe here so resume refreshes the platform snapshot, not only the lazy item.
    val diagnosticRefreshKey = diagnosticRefresh
    val notificationPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val notificationPermissionRationale = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context.findActivity()?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
        } == true
    val notificationPermissionPermanentlyDenied = !notificationPermissionGranted &&
        settings.notificationPermissionRequested && !notificationPermissionRationale
    val appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    val reminderChannels = listOf(
        "Task reminders" to ReminderNotifications.CHANNEL_ID,
        "Habit reminders" to HabitReminderNotifications.CHANNEL_ID,
        "Goal reminders" to GoalReminderNotifications.CHANNEL_ID,
        "Rest timer" to RestTimerNotifications.channelId(settings.timerSound, settings.timerVibration),
        "Focus timer" to FocusTimerNotifications.channelId,
    )
    val channelStates = reminderChannels.associate { (label, id) ->
        val channel = notificationManager.getNotificationChannel(id)
        label to notificationDeliveryState(
            permissionGranted = notificationPermissionGranted,
            appNotificationsEnabled = appNotificationsEnabled,
            configuredInWhip = channel != null,
            androidChannelEnabled = channel?.importance?.let { it != NotificationManager.IMPORTANCE_NONE } == true,
        )
    }
    val overallNotificationState = overallNotificationDeliveryState(
        notificationPermissionGranted,
        appNotificationsEnabled,
        channelStates.values,
    )
    val taskNotificationChannel = notificationManager.getNotificationChannel(ReminderNotifications.CHANNEL_ID)
    val taskNotificationChannelBlocked = taskNotificationChannel?.importance == NotificationManager.IMPORTANCE_NONE
    val batteryUnrestricted = context.getSystemService(PowerManager::class.java)
        .isIgnoringBatteryOptimizations(context.packageName)
    val exactAlarmAccessGranted = canScheduleExactReminderAlarms(context)
    CompositionLocalProvider(
        LocalSettingsTypedEditorState provides { tag, open ->
            activeTypedSettingTag = if (open) tag else activeTypedSettingTag.takeUnless { it == tag }
        },
    ) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(innerPadding)) {
        val wideSettingsNavigation = !externalSectionNavigation && maxWidth >= 840.dp
        if (!externalSectionNavigation && !wideSettingsNavigation && !compactSectionOpen) {
            Column(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(whipPagePadding(bottom = 0.dp)),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WhipPageHeader(
                        title = "Settings",
                        supportingText = "Preferences, defaults, and app data.",
                    ) {
                        if (!externalSearchAction) WhipPageIconAction(Icons.Outlined.Search, "Search Settings", onClick = { settingsSearchOpen = true }, enabled = canSearchSettings)
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("settings-category-list"),
                    contentPadding = whipPagePadding(top = WhipSpacing.sibling),
                    verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
                ) {
                    item {
                        WhipActionList {
                            SettingsSection.entries.forEachIndexed { index, choice ->
                                WhipActionRow(
                                    title = choice.label,
                                    supportingText = choice.supportingText,
                                    onClick = { selectSection(choice); compactSectionOpen = true },
                                    modifier = Modifier.testTag("settings-section-${choice.label}"),
                                )
                                if (index < SettingsSection.entries.lastIndex) WhipActionDivider()
                            }
                        }
                    }
                }
            }
            return@BoxWithConstraints
        }
        BackHandler(
            enabled = !externalSectionNavigation && !wideSettingsNavigation && compactSectionOpen &&
                activeTypedSettingTag == null,
        ) {
            compactSectionOpen = false
        }
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(whipPagePadding(bottom = 0.dp)),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!externalSectionNavigation && !wideSettingsNavigation) {
                    WhipBackAction(
                        label = "Back to Settings",
                        onClick = {
                            if (activeTypedSettingTag == null) compactSectionOpen = false
                        },
                    )
                }
                WhipPageHeader(
                    title = if (wideSettingsNavigation) "Settings" else section.label,
                    supportingText = if (wideSettingsNavigation) {
                        "Local preferences, data controls, defaults, and export."
                    } else null,
                    modifier = Modifier.weight(1f),
                ) {
                    if (!externalSearchAction) WhipPageIconAction(
                        Icons.Outlined.Search,
                        "Search Settings",
                        onClick = { settingsSearchOpen = true },
                        enabled = canSearchSettings,
                        modifier = Modifier.testTag("settings-search-open"),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().weight(1f)) {
        if (wideSettingsNavigation) {
            WideSettingsSectionSidebar(
                selectedSection = section,
                onSectionSelected = ::selectSection,
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight(),
            )
            VerticalDivider()
        }
        WhipReorderLazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight().testTag("settings-list"),
            state = settingsListState,
            contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
        val anchored = SettingsAnchorItems(this, settingsAnchorPositions)
        if (state.busy) anchored.item {
            WhipStatusCard(
                kind = WhipStatusKind.Loading,
                title = "Working",
                message = (state.operation as? OperationStatus.Running)?.message ?: "Your request is in progress.",
                modifier = Modifier.testTag("settings-loading-status"),
            )
        }
        state.message?.takeIf { state.backupPreview == null && !state.encryptedRestorePending && !confirmDelete }?.let { message -> anchored.item {
            val messageKind = if (state.operation is OperationStatus.Failed) {
                WhipStatusKind.Error
            } else {
                WhipStatusKind.Success
            }
            WhipStatusCard(
                kind = messageKind,
                title = if (messageKind == WhipStatusKind.Error) "Action Not Completed" else "Action Completed",
                message = message,
                actionLabel = "Dismiss",
                onAction = viewModel::consumeMessage,
                modifier = Modifier.testTag("settings-result-status"),
            )
        } }
        typedSettingWarning?.let { warning -> anchored.item {
            WhipStatusCard(
                kind = WhipStatusKind.Warning,
                title = "Setting Saved with Warnings",
                message = warning,
                actionLabel = "Dismiss",
                onAction = { typedSettingWarning = null },
                modifier = Modifier.testTag("settings-completed-warning"),
            )
        } }

        if (section == SettingsSection.Appearance) {
        anchored.item(key = "setting-theme") { SettingsHeading("Theme and Colors") }
        anchored.item { SettingsDropdown("Theme", AppThemeMode.entries, settings.themeMode, AppThemeMode::label) { selected -> viewModel.update { it.copy(themeMode = selected) } } }
        anchored.item(key = "setting-dynamic-color") {
            val dynamicColorAvailable = supportsAndroidDynamicColor(Build.VERSION.SDK_INT)
            SettingsToggle(
                "Use Android dynamic colors",
                settings.dynamicColor,
                enabled = dynamicColorAvailable,
                supportingText = if (dynamicColorAvailable) {
                    "Uses the device wallpaper palette."
                } else {
                    "Requires Android 12 or newer. Whip uses its selected Theme on this device."
                },
            ) { selected -> viewModel.update { it.copy(dynamicColor = selected) } }
        }
        anchored.item { SettingsHeading("Presentation") }
        anchored.item(key = "setting-advanced") {
            SettingsToggle(
                "Show advanced controls by default",
                settings.powerMode,
                supportingText = "Opens optional planning and configuration groups automatically. It does not add or remove capabilities.",
            ) { selected -> viewModel.update { it.copy(powerMode = selected) } }
        }
        anchored.item(key = "setting-low-pressure") {
            SettingsToggle(
                "Low-pressure Habit presentation",
                settings.lowPressureMode,
                supportingText = "De-emphasizes streaks and success/failure language in Habit views without changing history.",
            ) { selected -> viewModel.update { it.copy(lowPressureMode = selected) } }
        }
        anchored.item(key = "setting-celebrate") {
            SettingsToggle(
                "Celebrate completed Goals",
                settings.goalCelebrationEnabled,
                supportingText = "Show Victory Shower for four seconds when you complete a Goal.",
            ) { selected -> viewModel.update { it.copy(goalCelebrationEnabled = selected) } }
        }
        anchored.item {
            SettingsHeading("Opening Area")
            Text(
                "Choose whether a new Whip session returns to the Area you used last or always starts from one chosen view. Widget shortcuts switch the current Area immediately.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        anchored.item(key = "setting-opening-area") {
            SettingsDropdown(
                label = "When Whip opens",
                values = AreaOpeningMode.entries,
                selected = settings.areaOpeningMode,
                text = { mode -> if (mode == AreaOpeningMode.LastUsed) "Last used area" else "Chosen area" },
            ) { selected ->
                viewModel.update { current ->
                    val activeAreas = state.areas.filterNot(com.whip.app.domain.Area::archived)
                    val lastUsed = AreaScope.fromStorageKey(current.activeAreaScope)
                    val chosen = if (
                        selected == AreaOpeningMode.Chosen &&
                        current.areaOpeningMode == AreaOpeningMode.LastUsed
                    ) {
                        if (lastUsed == AreaScope.All && activeAreas.size == 1) {
                            AreaScope.One(activeAreas.single().id).storageKey
                        } else {
                            lastUsed.storageKey
                        }
                    } else {
                        current.chosenOpeningAreaScope
                    }
                    current.copy(areaOpeningMode = selected, chosenOpeningAreaScope = chosen)
                }
            }
            Text(
                if (settings.areaOpeningMode == AreaOpeningMode.LastUsed) {
                    "Area changes, including widget switches, are saved and restored the next time Whip starts."
                } else {
                    "Widget switches affect the current session, but a new session returns to the chosen Area."
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (settings.areaOpeningMode == AreaOpeningMode.Chosen) anchored.item {
            val activeAreas = state.areas.filterNot(com.whip.app.domain.Area::archived)
            val choices = listOf(AreaScope.All) + activeAreas.map { AreaScope.One(it.id) }
            val storedChoice = AreaScope.fromStorageKey(settings.chosenOpeningAreaScope)
            val selectedChoice = storedChoice.takeIf(choices::contains) ?: AreaScope.All
            SettingsDropdown(
                label = "Opening area",
                values = choices,
                selected = selectedChoice,
                text = { scope ->
                    when (scope) {
                        AreaScope.All -> "All Areas"
                        AreaScope.Unassigned -> "Main"
                        is AreaScope.One -> activeAreas.firstOrNull { it.id == scope.areaId }?.name ?: "Unavailable Area"
                    }
                },
            ) { selected ->
                viewModel.update { it.copy(chosenOpeningAreaScope = selected.storageKey) }
            }
        }
        anchored.item(key = "setting-home") {
            SettingsHeading("Home Overview")
            Text(
                "Choose which sections and empty-day shortcuts appear on Whip Home. Main navigation and saved data remain unchanged. Pinning an item reveals and expands its section so the action always has a visible destination. Visible sections can start expanded or collapsed.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        val visibleHomeSectionCount = settings.homeSections.count { it !in settings.hiddenHomeSections }
        settings.homeSections.forEachIndexed { index, section ->
            anchored.item(key = "home-${section.name}") {
                val visible = section !in settings.hiddenHomeSections
                val expanded = section !in settings.collapsedHomeSections
                val reorderInteraction = rememberWhipReorderInteractionState()
                Card(
                    Modifier.fillMaxWidth().whipReorderItem(
                        reorderInteraction,
                        layoutPosition = index + 1,
                        layoutScope = "settings-home-sections",
                    ),
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        WhipToggleRow(
                            title = "Show ${section.label} on Home",
                            supportingText = when {
                                !visible -> "Hidden from the Home overview and its empty-day shortcuts."
                                expanded -> "Visible with its Home details expanded."
                                else -> "Visible as a collapsed Home heading."
                            },
                            checked = visible,
                            onCheckedChange = { show ->
                                viewModel.update { current ->
                                    current.copy(
                                        hiddenHomeSections = if (show) {
                                            current.hiddenHomeSections - section
                                        } else {
                                            current.hiddenHomeSections + section
                                        },
                                    )
                                }
                            },
                            enabled = !visible || visibleHomeSectionCount > 1,
                            modifier = Modifier.testTag("home-section-${section.name}"),
                        )
                        if (visible && visibleHomeSectionCount == 1) {
                            Text(
                                "At least one Home section must remain visible.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            if (visible) {
                                DisclosureButton(
                                    label = "Show Details by Default",
                                    expanded = expanded,
                                    onClick = { viewModel.update { current -> current.copy(collapsedHomeSections = if (section in current.collapsedHomeSections) current.collapsedHomeSections - section else current.collapsedHomeSections + section) } },
                                )
                            }
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                                WhipReorderHandle(
                                    label = "${section.label} Home section",
                                    canMovePrevious = index > 0,
                                    canMoveNext = index < settings.homeSections.lastIndex,
                                    position = index + 1,
                                    total = settings.homeSections.size,
                                    interactionState = reorderInteraction,
                                    moveWholeItem = true,
                                    layoutScope = "settings-home-sections",
                                    onMove = { viewModel.moveHomeSection(section, it) },
                                )
                            }
                        }
                    }
                }
            }
        }
        anchored.item(key = "setting-keyboard") {
            WhipGroupedInformationCard {
                WhipGroupHeading("Hardware Keyboard")
                Text("Ctrl+H Home · Ctrl+K Search · Ctrl+N contextual add · Ctrl+1–5 switch Tasks, Habits, Goals, Tracks, Gym", style = MaterialTheme.typography.bodySmall)
            }
        }

        }

        if (section == SettingsSection.Planning) {
        anchored.item { SettingsHeading("Date and Number Defaults") }
        anchored.item(key = "setting-week") {
            SettingsDropdown("First day of week", DayOfWeek.entries, settings.firstDayOfWeek, { weekdayFormatter.label(it, WhipWeekdayLabelWidth.Full) }) { selected -> viewModel.update { it.copy(firstDayOfWeek = selected) } }
            Text("Sets weekday order in calendars and editors, and groups weekly Review and Gym analytics. Existing Habit schedules keep their own week start.", style = MaterialTheme.typography.bodySmall)
        }
        anchored.item(key = "setting-timezone") {
            val followDevice = settings.timeZoneId == null
            val editingTimeZone = activeTypedSettingTag == "settings-field-time-zone"
            SettingsToggle(
                "Follow device time zone",
                followDevice,
                enabled = activeTypedSettingTag == null && !timeZoneModeCoordinator.saving,
                modifier = Modifier.testTag("settings-follow-device-time-zone"),
            ) { enabled ->
                val requestId = timeZoneModeCoordinator.begin() ?: return@SettingsToggle
                if (!viewModel.updateTypedSetting(requestId) {
                        it.copy(timeZoneId = if (enabled) null else ZoneId.systemDefault().id)
                    }
                ) {
                    timeZoneModeCoordinator.finishFailure("Another Settings change is still finishing. Review it and try again.")
                }
            }
            timeZoneModeCoordinator.errorMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                )
            }
            if (!followDevice || editingTimeZone) {
                TimeZoneSetting(
                    current = settings.timeZoneId,
                    mutation = appSettingMutation { current, value -> current.copy(timeZoneId = value) },
                )
            }
            Text("Active time zone: ${settings.zoneId().id}. Historical entries keep their saved local date and offset.", style = MaterialTheme.typography.bodySmall)
        }
        anchored.item(key = "setting-cutoff") {
            ClockSetting(
                label = "Late-night day cutoff",
                currentMinutes = settings.dayCutoffMinutes,
                mutation = appSettingMutation { current, minutes -> current.copy(dayCutoffMinutes = minutes) },
            )
            Text("Before this time, Today still uses the previous calendar date. Use 00:00 for the standard midnight boundary.", style = MaterialTheme.typography.bodySmall)
        }
        anchored.item(key = "setting-precision") {
            NumberSetting(
                label = "Default decimal precision",
                current = settings.numberPrecision,
                mutation = appSettingMutation { current, value -> current.copy(numberPrecision = value) },
                validRange = 0..6,
            )
        }
        anchored.item { SettingsHeading("Unit Defaults") }
        anchored.item(key = "setting-mass") { UnitSetting("Mass", listOf("kilogram", "pound", "gram"), settings.massUnitId) { value -> viewModel.update { it.copy(massUnitId = value) } } }
        anchored.item(key = "setting-distance") { UnitSetting("Distance", listOf("kilometre", "mile", "distance_m"), settings.distanceUnitId) { value -> viewModel.update { it.copy(distanceUnitId = value) } } }
        anchored.item(key = "setting-volume") { UnitSetting("Volume", listOf("litre", "millilitre", "cup", "fluid_ounce"), settings.volumeUnitId) { value -> viewModel.update { it.copy(volumeUnitId = value) } } }
        anchored.item(key = "setting-custom-units") {
            DisclosureRow("Custom Units", supportingText = "${state.customUnits.count { !it.archived }} active · ${state.customUnits.count { it.archived }} archived",
                expanded = customUnitsExpanded, onClick = { customUnitsExpanded = !customUnitsExpanded })
        }
        if (customUnitsExpanded) {
        anchored.item {
            Text(
                "Create reusable units for Habit entries, Goal progress, and number fields in Tracks. You can also create a unit beside an item's Unit control.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        anchored.items(state.customUnits, key = { "custom-unit-${it.id}" }) { unit ->
            WhipRecordItem(
                itemKey = unit.id,
                itemType = "custom unit",
                title = "${unit.name}${unit.symbol.takeIf(String::isNotBlank)?.let { " ($it)" }.orEmpty()}",
                modifier = Modifier.testTag("custom-unit-card-${unit.id}"),
            ) {
                context(listOfNotNull(unit.dimension.uiLabel(), "Archived".takeIf { unit.archived }).joinToString(" · "))
                detail("1 ${unit.symbol.ifBlank { unit.name }} = ${unit.toCanonicalFactor} ${canonicalUnitLabel(unit.dimension)}")
                command("Rename", enabled = !customUnitCoordinator.saving) {
                    renameUnitBoundary = unit.customUnitBoundary()
                }
                if (!unit.archived) {
                    command("Create New Version", enabled = !customUnitCoordinator.saving) {
                        versionUnitBoundary = unit.customUnitBoundary()
                        versionUnitTargetId = UUID.randomUUID().toString()
                    }
                }
                command(if (unit.archived) "Restore" else "Archive", enabled = !customUnitCoordinator.saving) {
                    val boundary = unit.customUnitBoundary()
                    submitCustomUnitAction("archive") { requestId ->
                        viewModel.setCustomUnitArchivedMutation(
                            requestId = requestId,
                            boundary = boundary,
                            archived = !boundary.archived,
                        )
                    }
                }
            }
        }
        anchored.item {
            customUnitCoordinator.errorMessage?.let { message ->
                WhipStatusCard(
                    kind = WhipStatusKind.Error,
                    title = "Custom Unit Not Saved",
                    message = message,
                    modifier = Modifier.testTag("custom-unit-error"),
                )
            }
            WhipOutlinedButton(
                onClick = {
                    createUnitTargetId = UUID.randomUUID().toString()
                    createUnit = true
                    customUnitCoordinator.clear()
                },
                enabled = !customUnitCoordinator.saving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Create Custom Unit") }
        }
        }
        anchored.item { SettingsHeading("Review Defaults") }
        anchored.item(key = "setting-review") { SettingsDropdown("Default review period", ReviewPeriod.entries, settings.reviewPeriod, ReviewPeriod::label) { value -> viewModel.update { it.copy(reviewPeriod = value) } } }

        }

        if (section == SettingsSection.Planning) {
        anchored.item { SettingsHeading("Task Defaults") }
        anchored.item(key = "setting-repeats") {
            SettingsToggle(
                "Show every repeating occurrence in Upcoming",
                settings.showAllUpcomingTaskOccurrences,
            ) { value -> viewModel.update { it.copy(showAllUpcomingTaskOccurrences = value) } }
            Text(
                if (settings.showAllUpcomingTaskOccurrences) {
                    "Upcoming shows every occurrence in the next 30 days."
                } else {
                    "Upcoming shows only the next occurrence of each repeating task."
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        anchored.item(key = "setting-habits-planning") {
            SettingsToggle(
                "Show habits in Task Agenda and Calendar",
                settings.showHabitsInTaskPlanning,
            ) { value -> viewModel.update { it.copy(showHabitsInTaskPlanning = value) } }
            Text(
                "Habits remain separate records; this only projects scheduled habits into planning views.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        anchored.item(key = "setting-subtasks") { SettingsDropdown("Repeating-task subtask default", RepeatStepPolicy.entries, settings.defaultTaskStepPolicy, RepeatStepPolicy::uiLabel) { value -> viewModel.update { it.copy(defaultTaskStepPolicy = value) } } }
        anchored.item(key = "setting-capture") {
            SettingsToggle(
                label = "Smart Task Capture",
                checked = settings.naturalLanguageTaskCapture,
                modifier = Modifier.testTag("settings-smart-task-capture"),
            ) { value -> viewModel.update { it.copy(naturalLanguageTaskCapture = value) } }
            Text(
                "On by default. Recognized scheduling, repeat, deadline, reminder, priority, duration, effort, and tag details are highlighted before saving. Quick Capture applies visible details automatically; the full editor lets you review them first. Parsing stays on this device.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (settings.naturalLanguageTaskCapture) {
                DisclosureRow("Smart Capture Examples", "Try: Send report tomorrow at 9am #work",
                    expanded = captureExamplesExpanded, onClick = { captureExamplesExpanded = !captureExamplesExpanded })
                if (captureExamplesExpanded) WhipGroupedInformationCard(
                    modifier = Modifier.padding(top = 8.dp).testTag("smart-task-capture-examples"),
                ) {
                        Text("Try Smart Capture", fontWeight = FontWeight.Bold)
                        Text("Send report tomorrow at 9am #work", style = MaterialTheme.typography.bodyMedium)
                        Text("Review notes every Mon & Thu for 30m", style = MaterialTheme.typography.bodyMedium)
                        Text("Submit expenses by next Friday !high", style = MaterialTheme.typography.bodyMedium)
                        Text("Join planning call at 2pm with reminder", style = MaterialTheme.typography.bodyMedium)
                        Text("Replace filter every other month after completion", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Also understands named months, in 3 days, weekdays/weekends, monthly on the 1st, until Dec 31, for 10 occurrences, reminder offsets, priority: urgent, and light effort. Only highlighted phrases are interpreted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                }
            }
        }
        anchored.item { SettingsHeading("Habit Defaults") }
        anchored.item(key = "setting-habit-week") {
            SettingsDropdown("Default week start for new Habits", DayOfWeek.entries, settings.defaultHabitWeekStart, { weekdayFormatter.label(it, WhipWeekdayLabelWidth.Full) }) { value -> viewModel.update { it.copy(defaultHabitWeekStart = value) } }
            Text("Existing Habits keep their own saved week start.", style = MaterialTheme.typography.bodySmall)
        }
        }

        if (section == SettingsSection.Gym) {
        anchored.item { SettingsHeading("Workout and Rest") }
        anchored.item(key = "setting-gym-mass") {
            UnitSetting("Gym summaries and new exercises", listOf("kilogram", "pound"), settings.gymWeightUnitId) { value ->
                viewModel.update { it.copy(gymWeightUnitId = value) }
            }
            Text(
                "This changes aggregate gym displays and the default for new exercises. Existing exercises and machine profiles keep their own equipment unit.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        anchored.item(key = "setting-rest") {
            NumberSetting(
                label = "Default rest time (seconds)",
                current = settings.defaultRestSeconds,
                mutation = appSettingMutation { current, value -> current.copy(defaultRestSeconds = value) },
                validRange = 15..3_600,
                supportingText = "Used when a workout has no temporary rest-time override.",
            )
        }
        anchored.item(key = "setting-rest-presets") {
            WhipSettingItem("Rest Presets") {
                edit(value = settings.restTimerPresetSeconds.joinToString(" · ") { seconds -> "%d:%02d".format(seconds / 60, seconds % 60) },
                    onOpen = { restPresetsCoordinator.clear(); restPresetsOpen = true })
            }
            Text("App-wide shortcuts. A workout's temporary rest time does not change these presets.", style = MaterialTheme.typography.bodySmall)
        }
        anchored.item(key = "setting-sound") { SettingsToggle("Rest timer sound", settings.timerSound) { value -> viewModel.update { it.copy(timerSound = value) } } }
        anchored.item(key = "setting-vibration") { SettingsToggle("Rest timer vibration", settings.timerVibration) { value -> viewModel.update { it.copy(timerVibration = value) } } }
        anchored.item(key = "setting-awake") { SettingsToggle("Keep workout screen awake by default", settings.keepScreenAwake) { value -> viewModel.update { it.copy(keepScreenAwake = value) } } }
        anchored.item(key = "setting-auto-rest") {
            SettingsToggle(
                "Start rest timer when a set completes",
                settings.restTimerAutoStart,
                supportingText = "Starts the in-app timer automatically. Android notification access is needed only for background alerts.",
            ) { value ->
                if (value && !settings.restTimerAutoStart && !notificationPermissionGranted) {
                    viewModel.markNotificationPermissionRequested()
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.update { it.copy(restTimerAutoStart = value) }
            }
        }
        anchored.item { SettingsHeading("Workout Inputs") }
        anchored.item(key = "setting-effort") {
            val effortField = when {
                settings.showGymRpe -> "RPE"
                settings.showGymRir -> "RIR"
                else -> "None"
            }
            SettingsDropdown("Default workout effort field", listOf("None", "RPE", "RIR"), effortField, { it }) { value ->
                viewModel.update { it.copy(showGymRpe = value == "RPE", showGymRir = value == "RIR") }
            }
            Text("Individual exercises can override this default. RPE and RIR are alternative scales, so only one is shown at a time.", style = MaterialTheme.typography.bodySmall)
        }
        anchored.item(key = "setting-tempo") {
            SettingsToggle("Show tempo fields", settings.showGymTempo) { value -> viewModel.update { it.copy(showGymTempo = value) } }

        }
        anchored.item { SettingsHeading("Progress Calculations") }
        anchored.item(key = "setting-formula") { SettingsDropdown("Estimated 1RM formula", listOf("Epley", "Brzycki"), settings.oneRepMaxFormula, { it }) { value -> viewModel.update { it.copy(oneRepMaxFormula = value) } } }
        anchored.item(key = "setting-rep-cutoff") {
            NumberSetting(
                label = "Estimated 1RM rep cutoff",
                current = settings.oneRepMaxRepCutoff,
                mutation = appSettingMutation { current, value -> current.copy(oneRepMaxRepCutoff = value) },
                validRange = 1..36,
                supportingText = "Sets above this repetition count are excluded from estimated 1RM records.",
            )
        }
        anchored.item(key = "setting-warmups") { SettingsToggle("Include warm-ups in volume and PRs", settings.includeWarmupsInGymStats) { value -> viewModel.update { it.copy(includeWarmupsInGymStats = value) } } }
        anchored.item(key = "setting-hard-sets") {
            Text("Hard-Set Classifications", modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WorkoutSetClassification.entries.forEach { classification ->
                    val value = classification.name
                    WhipFilterChip(
                        selected = value in settings.hardSetClassifications,
                        onClick = {
                            viewModel.update { current ->
                                val changed = if (value in current.hardSetClassifications) current.hardSetClassifications - value else current.hardSetClassifications + value
                                current.copy(hardSetClassifications = changed.ifEmpty { setOf("Working") })
                            }
                        },
                        label = { Text(value.workoutSetClassificationLabel()) },
                    )
                }
            }
            Text("Only these classifications count in category hard-set summaries. Volume and PR inclusion remain separately configurable.", style = MaterialTheme.typography.bodySmall)
        }
        anchored.item(key = "setting-allocation") {
            SettingsDropdown(
                "Overlapping category allocation",
                listOf("Full", "Fractional", "PrimaryOnly"),
                settings.categoryAllocationMode,
                ::categoryAllocationModeLabel,
            ) { selected -> viewModel.update { it.copy(categoryAllocationMode = selected) } }
            Text(
                if (settings.categoryAllocationMode == "PrimaryOnly") {
                    "Counts the set only in the first linked category in your Gym Categories order. Reorder categories to control which one wins."
                } else {
                    "Controls how one hard set contributes when an exercise belongs to multiple Exercise Library categories. Routine roles are configured separately."
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        anchored.item(key = "setting-effort-adjustment") { SettingsToggle("Adjust estimated 1RM using RPE/RIR", settings.adjustE1rmForEffort) { selected -> viewModel.update { it.copy(adjustE1rmForEffort = selected) } } }
        anchored.item(key = "setting-assisted") { SettingsToggle("Allow assisted exercises in personal records", settings.includeAssistedInPersonalRecords) { selected -> viewModel.update { it.copy(includeAssistedInPersonalRecords = selected) } } }
        }

        if (section == SettingsSection.Organization) {
        anchored.item(key = "setting-areas") {
            WhipGroupedInformationCard {
                    WhipGroupHeading("Areas")
                    Text("Create named areas to group related tasks, habits, goals, and tracks across search and review.")
                    Text("${state.areas.count { !it.archived }} active · ${state.areas.count { it.archived }} archived · ${state.areaUsage.values.sumOf(AreaUsageCounts::total) + state.unassignedAreaUsage.total} items", style = MaterialTheme.typography.bodySmall)
                    WhipButton(onClick = onEditAreas, modifier = Modifier.fillMaxWidth()) { Text("Manage Areas") }
            }
        }
        anchored.item(key = "setting-tags") {
            WhipGroupedInformationCard(Modifier.testTag("settings-tags-summary")) {
                    WhipGroupHeading("Tags")
                    Text("Use flexible labels across Tasks, Habits, Goals, and Tracks while each item keeps one primary Area.")
                    Text(
                        "${state.tags.count { !it.archived }} active · ${state.tags.count { it.archived }} archived · ${state.tagUsage.values.sumOf(TagUsageCounts::total)} current references",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    WhipButton(
                        onClick = onEditTags,
                        modifier = Modifier.fillMaxWidth().testTag("manage-tags-action"),
                    ) { Text("Manage Tags") }
            }
        }
        anchored.item(key = "setting-emojis") {
            SettingsHeading("Custom Emojis")
            Text(
                "Name reusable emoji choices for your own organization. Whip's ${IDENTITY_EMOJI_PRESETS.size} common emojis are always available and cannot be renamed, replaced, or deleted.",
            )
            Text(
                "Removing a custom choice does not change Habits, Goals, or Tracks that already use its emoji.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            WhipGroupedInformationCard(Modifier.padding(top = WhipSpacing.sibling)) {
                    Text(
                        "${IDENTITY_EMOJI_PRESETS.size} Common Emojis · Read-Only",
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (settings.customIdentityEmojis.isEmpty()) {
                        Text("No custom emojis yet.", style = MaterialTheme.typography.bodySmall)
                    }
                    settings.customIdentityEmojis.forEachIndexed { index, choice ->
                        val reorderInteraction = rememberWhipReorderInteractionState()
                        var emojiMenuOpen by rememberSaveable(choice.emoji) { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .whipReorderItem(
                                    reorderInteraction,
                                    layoutPosition = index + 1,
                                    layoutScope = "settings-custom-emojis",
                                )
                                .testTag("custom-emoji-${choice.emoji}"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            WhipReorderHandle(
                                label = "${choice.name} custom emoji",
                                canMovePrevious = index > 0,
                                canMoveNext = index < settings.customIdentityEmojis.lastIndex,
                                position = index + 1,
                                total = settings.customIdentityEmojis.size,
                                interactionState = reorderInteraction,
                                moveWholeItem = true,
                                layoutScope = "settings-custom-emojis",
                                onMove = { delta -> viewModel.moveCustomIdentityEmoji(choice.emoji, delta) },
                            )
                            WhipIdentityEmoji(choice.emoji, contentDescription = "${choice.name} emoji")
                            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                Text(choice.name, fontWeight = FontWeight.SemiBold)
                                Text(choice.emoji, style = MaterialTheme.typography.bodySmall)
                            }
                            WhipOverflowMenu(
                                label = "Options for ${choice.name}",
                                expanded = emojiMenuOpen,
                                onExpandedChange = { emojiMenuOpen = it },
                                modifier = Modifier.testTag("custom-emoji-menu-${choice.emoji}"),
                            ) {
                                WhipMenuItem("Edit", onClick = {
                                    emojiMenuOpen = false
                                    customEmojiEditorOriginal = choice.emoji
                                    customEmojiEditorOpen = true
                                })
                                WhipMenuItem(
                                    "Remove",
                                    onClick = {
                                        emojiMenuOpen = false
                                        viewModel.removeCustomIdentityEmoji(choice.emoji)
                                    },
                                    role = WhipMenuItemRole.Destructive,
                                )
                            }
                        }
                    }
                    WhipOutlinedButton(
                        onClick = {
                            customEmojiEditorOriginal = null
                            customEmojiEditorOpen = true
                        },
                        modifier = Modifier.fillMaxWidth().testTag("custom-emoji-add"),
                    ) { Text("Add Custom Emoji") }
            }
        }

        }

        if (section == SettingsSection.Reminders) {
        anchored.item(key = "setting-notifications") { SettingsHeading("Notifications") }
        anchored.item(key = "notification-diagnostics-$diagnosticRefreshKey") {
            WhipGroupedInformationCard(Modifier.testTag("notification-diagnostics")) {
                    WhipGroupHeading("Reminder Delivery")
                    Text(overallNotificationState.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        when (overallNotificationState) {
                            NotificationDeliveryState.Deliverable -> "Configured Whip reminders can be delivered by Android."
                            NotificationDeliveryState.Blocked -> when {
                                notificationPermissionPermanentlyDenied -> "Android notification permission is blocked. Re-enable it before relying on reminders."
                                notificationPermissionRationale -> "Notification permission was declined. Whip uses it only for reminders and timers you enable."
                                else -> "Notification permission has not been granted, so no reminder can be shown."
                            }
                            NotificationDeliveryState.OffInWhip -> "Android is ready, but no Whip reminder channel is active yet. Add a reminder or enable a timer when you want notifications."
                            NotificationDeliveryState.OffInAndroid -> "Android is blocking Whip or at least one active reminder channel."
                        },
                        color = when (overallNotificationState) {
                            NotificationDeliveryState.Deliverable -> MaterialTheme.colorScheme.primary
                            NotificationDeliveryState.OffInWhip -> MaterialTheme.colorScheme.onSurfaceVariant
                            NotificationDeliveryState.Blocked, NotificationDeliveryState.OffInAndroid -> MaterialTheme.colorScheme.error
                        },
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Precise reminder timing", modifier = Modifier.weight(1f))
                            Text(
                                if (exactAlarmAccessGranted) "Allowed" else "Needs access",
                                color = if (exactAlarmAccessGranted) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            if (exactAlarmAccessGranted) {
                                "Android can wake Whip at reminder times. Whip also keeps a background fallback."
                            } else {
                                "Android may defer reminders until it runs Whip. Allow Alarms & reminders for consistent timing."
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (!exactAlarmAccessGranted) {
                            WhipButton(
                                onClick = {
                                    exactAlarmAccess.launch(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                            .setData("package:${context.packageName}".toUri()),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("allow-precise-reminder-timing"),
                            ) { Text("Allow Precise Reminder Timing") }
                        }
                    }

                    if (!notificationPermissionGranted) {
                        WhipButton(
                            onClick = {
                                if (notificationPermissionPermanentlyDenied) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                                    )
                                } else {
                                    viewModel.markNotificationPermissionRequested()
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (notificationPermissionPermanentlyDenied) "Repair in Android Settings" else "Allow Notifications") }
                    } else if (overallNotificationState == NotificationDeliveryState.OffInAndroid) {
                        WhipButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Repair in Android Settings") }
                    }

                    DisclosureButton("Delivery Details", deliveryDetailsExpanded, { deliveryDetailsExpanded = !deliveryDetailsExpanded }, Modifier.fillMaxWidth())
                    if (deliveryDetailsExpanded) {
                        channelStates.forEach { (label, state) ->
                            EntityInspectorFact(label = label, value = state.label)
                        }
                    }

                    DisclosureButton("Troubleshooting", notificationTroubleshootingExpanded, { notificationTroubleshootingExpanded = !notificationTroubleshootingExpanded }, Modifier.fillMaxWidth())
                    if (notificationTroubleshootingExpanded) {
                        Text(
                            when {
                                batteryUnrestricted -> "Battery optimization is unrestricted for Whip."
                                exactAlarmAccessGranted -> "Precise reminder wakeups are allowed. Android may still limit very closely spaced alarms during deep idle; Whip keeps a fallback."
                                else -> "Android battery optimization and missing precise-timing access may delay reminders while Whip is idle."
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        WhipOutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Android Notification Settings") }
                        reminderChannels.filter { (label) -> channelStates[label] == NotificationDeliveryState.OffInAndroid }.forEach { (label, channelId) ->
                            WhipOutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            .putExtra(Settings.EXTRA_CHANNEL_ID, channelId),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Repair $label") }
                        }
                        WhipOutlinedButton(
                            onClick = { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Battery Optimization Settings") }
                    }
            }
        }
        anchored.item(key = "setting-test-notification") {
            val testNotificationAvailability = ControlAvailability(
                enabled = canSendNotificationTest(
                    notificationPermissionGranted,
                    appNotificationsEnabled,
                    taskNotificationChannelBlocked,
                ),
                unavailableExplanation = when {
                    !notificationPermissionGranted -> "Allow Whip notifications in Android settings."
                    !appNotificationsEnabled -> "Turn on Whip notifications in Android settings."
                    taskNotificationChannelBlocked -> "Turn on the Task reminders channel in Android settings."
                    else -> null
                },
            )
            WhipButton(
                enabled = testNotificationAvailability.enabled,
                onClick = {
                    notificationTestSucceeded = ReminderNotifications.showTest(context)
                    notificationTestMessage = if (notificationTestSucceeded) {
                        "Test sent. Check the notification shade."
                    } else {
                        "Android blocked the test notification."
                    }
                    diagnosticRefresh++
                },
                modifier = Modifier.fillMaxWidth().testTag("send-test-notification"),
            ) { Text("Send Test Notification") }
            notificationTestMessage?.let { message ->
                WhipStatusCard(kind = if (notificationTestSucceeded) WhipStatusKind.Success else WhipStatusKind.Error,
                    title = if (notificationTestSucceeded) "Test Notification Sent" else "Test Notification Blocked",
                    message = message, modifier = Modifier.testTag("notification-test-result"))
            }
            if (taskNotificationChannel == null && testNotificationAvailability.enabled) {
                Text("Sending a test creates the Task reminders channel if it does not exist yet.", style = MaterialTheme.typography.bodySmall)
            }
            AvailabilityNotice("Send test notification", testNotificationAvailability)
        }
        anchored.item(key = "setting-refresh-notifications") {
            WhipOutlinedButton(
                onClick = {
                    diagnosticRefresh++
                    notificationRefreshMessage = null
                    val requestId = notificationRefreshCoordinator.begin()
                    if (requestId != null && !viewModel.refreshReminderSchedules(requestId)) {
                        notificationRefreshCoordinator.finishFailure("Another Settings change is still finishing. Wait and try again.")
                    }
                },
                enabled = !state.busy && !notificationRefreshCoordinator.saving && activeTypedSettingTag == null,
                modifier = Modifier.fillMaxWidth().testTag("refresh-notification-status"),
            ) { Text(if (notificationRefreshCoordinator.saving) "Refreshing Reminders…" else "Refresh Notification Status") }
            val refreshError = notificationRefreshCoordinator.errorMessage
            val refreshMessage = refreshError ?: notificationRefreshMessage
            if (refreshMessage != null) {
                WhipStatusCard(
                    kind = when {
                        refreshError != null -> WhipStatusKind.Error
                        notificationRefreshWarning -> WhipStatusKind.Warning
                        else -> WhipStatusKind.Success
                    },
                    title = if (refreshError != null || notificationRefreshWarning) "Reminder Refresh Incomplete" else "Reminder Schedules Refreshed",
                    message = refreshMessage,
                    modifier = Modifier.testTag("notification-refresh-result"),
                )
            }
        }
        anchored.item(key = "setting-quiet") { SettingsHeading("Quiet Hours") }
        anchored.item {
            val enabled = settings.quietStartMinutes != null && settings.quietEndMinutes != null
            val editingQuietHours = activeTypedSettingTag in setOf(
                "settings-field-quiet-hours-start",
                "settings-field-quiet-hours-end",
            )
            SettingsToggle(
                "Enable notification quiet hours",
                enabled,
                enabled = activeTypedSettingTag == null && !quietHoursCoordinator.saving,
                modifier = Modifier.testTag("settings-enable-quiet-hours"),
            ) { selected ->
                val requestId = quietHoursCoordinator.begin() ?: return@SettingsToggle
                if (!viewModel.updateTypedSetting(requestId) { current ->
                        current.copy(
                            quietStartMinutes = if (selected) current.quietStartMinutes ?: 22 * 60 else null,
                            quietEndMinutes = if (selected) current.quietEndMinutes ?: 7 * 60 else null,
                        )
                    }
                ) {
                    quietHoursCoordinator.finishFailure("Another Settings change is still finishing. Review it and try again.")
                }
            }
            quietHoursCoordinator.errorMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                )
            }
            if (enabled) Text(
                "${formatSettingsClock(requireNotNull(settings.quietStartMinutes))}–${formatSettingsClock(requireNotNull(settings.quietEndMinutes))}" +
                    if (requireNotNull(settings.quietEndMinutes) < requireNotNull(settings.quietStartMinutes)) " · overnight" else "",
                style = MaterialTheme.typography.bodySmall)
            if (enabled || editingQuietHours) {
                ClockSetting(
                    label = "Quiet hours start",
                    currentMinutes = settings.quietStartMinutes ?: 22 * 60,
                    sourceIdentity = "${settings.quietStartMinutes}:${settings.quietEndMinutes}",
                    mutation = appSettingMutation { current, minutes ->
                        current.copy(
                            quietStartMinutes = minutes,
                            quietEndMinutes = current.quietEndMinutes ?: 7 * 60,
                        )
                    },
                )
                ClockSetting(
                    label = "Quiet hours end",
                    currentMinutes = settings.quietEndMinutes ?: 7 * 60,
                    sourceIdentity = "${settings.quietStartMinutes}:${settings.quietEndMinutes}",
                    mutation = appSettingMutation { current, minutes ->
                        current.copy(
                            quietStartMinutes = current.quietStartMinutes ?: 22 * 60,
                            quietEndMinutes = minutes,
                        )
                    },
                )
            }
        }
        }

        val dataPrivacyPasses = if (section == SettingsSection.DataPrivacy) {
            DataPrivacyGroupOrder
        } else {
            emptyList()
        }
        dataPrivacyPasses.forEach { dataPrivacyPass ->
        if (dataPrivacyPass == DataPrivacyGroup.Backup) {
        anchored.item(key = "setting-backup-status") {
            WhipGroupedInformationCard(modifier = Modifier.testTag("backup-protection-summary")) {
                WhipGroupHeading("Backup & Export")
                Text(when {
                    !state.portableBackup.configured -> "No backup folder configured"
                    state.portableBackup.lastError != null -> "Backup needs attention"
                    state.portableBackup.lastBackupAtMillis == null -> "Folder connected · No verified backup yet"
                    state.portableBackup.automaticEnabled -> "Automatic daily backup is on"
                    else -> "Automatic daily backup is off"
                }, style = MaterialTheme.typography.titleSmall)
                state.portableBackup.lastBackupAtMillis?.let { millis ->
                    val whenSaved = formatSettingsTimestamp(
                        instant = Instant.ofEpochMilli(millis),
                        zoneId = settings.zoneId(),
                        locale = LocalConfiguration.current.locales[0],
                    )
                    Text(
                        stringResource(R.string.settings_backup_last_verified, whenSaved),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                state.portableBackup.lastError?.let { error ->
                    Text(
                        "Last backup warning or error: $error. Reconnect this folder below, or forget it and choose another folder.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                    )
                }
                if (!state.portableBackup.configured) {
                    Text("Save a backup below, or choose a folder for verified daily copies.", style = MaterialTheme.typography.bodySmall)
                    WhipOutlinedButton(onClick = { backupFolder.launch(null) }, enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()) { Text("Choose Backup Folder") }
                } else {
                    if (state.portableBackup.lastError != null || state.portableBackup.lastBackupAtMillis == null) {
                        Text(if (state.portableBackup.automaticEnabled) "Automatic daily backup is on" else "Automatic daily backup is off",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    WhipButton(onClick = viewModel::createPortableBackup, enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()) { Text("Back Up Now") }
                    if (state.portableBackup.lastError != null) WhipTextButton(
                        onClick = { backupFolder.launch(state.portableBackup.folderUri?.let(android.net.Uri::parse)) },
                        enabled = !state.busy,
                    ) { Text("Reconnect or Change Folder") }
                }
            }
        }
        anchored.item(key = "setting-backup") { SettingsHeading("Save or Restore a Backup") }
        anchored.item {
            WhipActionList {
                WhipActionRow(
                    title = "Save Plain JSON Backup",
                    supportingText = "A readable copy of your records and settings. Anyone with the file can read it.",
                    enabled = !state.busy,
                    onClick = {
                        viewModel.prepareDocumentExport(ExportKind.Backup)
                        createDocument.launch("whip-${LocalDate.now(settings.zoneId())}.whip.json")
                    },
                )
                WhipActionDivider()
                WhipActionRow(
                    title = "Save Passphrase-Encrypted Backup",
                    supportingText = "Protect your records and settings with a passphrase. Whip cannot recover a forgotten passphrase.",
                    enabled = !state.busy,
                    onClick = { showEncryptedExport = true },
                )
                WhipActionDivider()
                WhipActionRow(
                    title = "Preview and Restore Backup",
                    supportingText = "Review a saved backup before choosing to merge or replace local data.",
                    enabled = !state.busy,
                    onClick = { openDocument.launch(arrayOf("application/json", "text/plain", "*/*")) },
                )
            }
        }
        anchored.item(key = "setting-backup-folder") {
            DisclosureRow(title = "Portable Backup Folder",
                supportingText = if (state.portableBackup.configured) "Automatic backups, retention, and folder access" else "Set up verified daily backups",
                expanded = backupFolderExpanded, onClick = { if (activeTypedSettingTag == null) backupFolderExpanded = !backupFolderExpanded },
                modifier = Modifier.testTag("backup-folder-disclosure"))
        }
        if (backupFolderExpanded) anchored.item {
            WhipGroupedInformationCard {
                    Text(
                        "Save verified plain-JSON backups to Files, Drive, or removable storage. Retention and cleanup act only on Whip's automatic-backup and incomplete-write filenames.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (state.portableBackup.configured) {
                        Text("Folder: ${state.portableBackup.folderLabel ?: "Selected folder"}")
                        state.portableBackup.lastBackupFileName?.let { fileName ->
                            state.portableBackup.lastBackupAtMillis?.let { millis ->
                                Text(stringResource(R.string.settings_backup_last_verified_file,
                                    formatSettingsTimestamp(Instant.ofEpochMilli(millis), settings.zoneId(), LocalConfiguration.current.locales[0]),
                                    fileName), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        SettingsToggle(
                            "Automatic daily backup",
                            state.portableBackup.automaticEnabled,
                            enabled = !state.busy,
                            onChange = viewModel::setPortableBackupAutomatic,
                        )
                        Text(
                            stringResource(R.string.settings_backup_automatic_retention),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        NumberSetting(
                            label = "Verified backups to keep (1–30)",
                            current = state.portableBackup.retentionCount,
                            mutation = TypedSettingMutation(
                                state = typedSettingMutationState,
                                consume = viewModel::consumeTypedSettingMutation,
                                submit = viewModel::setPortableBackupRetention,
                            ),
                            validRange = 1..30,
                        )
                        ResponsiveSettingsActions(
                            first = { buttonModifier ->
                                WhipOutlinedButton(
                                    onClick = { backupFolder.launch(state.portableBackup.folderUri?.let(android.net.Uri::parse)) },
                                    enabled = !state.busy,
                                    modifier = buttonModifier,
                                ) {
                                    Text(if (state.portableBackup.lastError == null) "Change Folder" else "Reconnect or Change Folder")
                                }
                            },
                            second = { buttonModifier ->
                                WhipTextButton(
                                    onClick = viewModel::clearPortableBackupFolder,
                                    enabled = !state.busy,
                                    modifier = buttonModifier,
                                ) {
                                    Text("Forget Folder")
                                }
                            },
                        )
                    } else {
                        WhipButton(
                            onClick = { backupFolder.launch(null) },
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Choose Backup Folder") }
                    }
            }
        }
        anchored.item(key = "setting-csv") {
            DisclosureRow(title = "Export CSV", supportingText = "Five spreadsheet exports · not restorable backups",
                expanded = csvExportsExpanded, onClick = { csvExportsExpanded = !csvExportsExpanded },
                modifier = Modifier.testTag("csv-export-disclosure"))
        }
        if (csvExportsExpanded) anchored.item {
            Text(
                "Export individual tables for spreadsheets. Use a backup to restore Whip.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            WhipActionList {
                listOf(ExportKind.TasksCsv to "Tasks", ExportKind.HabitsCsv to "Habits", ExportKind.GoalsCsv to "Goals", ExportKind.GymCsv to "Gym", ExportKind.TracksCsv to "Tracks").forEachIndexed { index, (kind, label) ->
                    if (index > 0) WhipActionDivider()
                    WhipActionRow(title = "Export $label CSV", enabled = !state.busy, onClick = {
                        viewModel.prepareDocumentExport(kind)
                        createDocument.launch("whip-${label.lowercase()}-${LocalDate.now(settings.zoneId())}.csv")
                    })
                }
            }
        }
        }
        if (dataPrivacyPass == DataPrivacyGroup.Reset) {
        anchored.item(key = "setting-reset") {
            val destructiveActionDescription = stringResource(R.string.state_destructive_action)
            WhipDangerZone {
                Text(
                    stringResource(R.string.settings_reset_explanation),
                    style = MaterialTheme.typography.bodySmall,
                )
                WhipActionRow(
                    title = stringResource(R.string.settings_reset_entry_title),
                    onClick = {
                        viewModel.consumeMessage()
                        resetSubmitted = false
                        confirmDelete = true
                    },
                    enabled = !state.busy,
                    modifier = Modifier.testTag("reset-whip-action").semantics {
                        stateDescription = destructiveActionDescription
                    },
                    navigates = false,
                    danger = true,
                )
            }
        }
        }

        }
        if (section == SettingsSection.AboutDiagnostics) {
        anchored.item(key = "setting-about") {
            WhipGroupedInformationCard {
                    Text("Whip", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${if (BuildConfig.DEBUG) "Development" else "Release"} · ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        modifier = Modifier.testTag("about-build-identity"),
                    )
                    Text(context.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Your data stays on this device unless you export it or save backups to a folder you choose.")
            }
        }
        }
        }
        }
    }
    }

    if (settingsSearchOpen) SettingsSearchDialog(
        query = settingsSearchQuery,
        onQueryChange = { settingsSearchQuery = it },
        onDismiss = { settingsSearchOpen = false },
        onSelect = { entry ->
            if (canSearchSettings) {
                when (entry.anchor) {
                    "setting-custom-units" -> customUnitsExpanded = true
                    "setting-capture" -> captureExamplesExpanded = true
                    "setting-notifications" -> { deliveryDetailsExpanded = true; notificationTroubleshootingExpanded = true }
                    "setting-backup-folder" -> backupFolderExpanded = true
                    "setting-csv" -> csvExportsExpanded = true
                }
                selectSection(entry.section)
                compactSectionOpen = true
                pendingSettingsAnchor = entry.anchor
                settingsSearchOpen = false
            }
        },
    )

    state.backupPreview?.let { preview ->
        BackupRestorePreviewDialogs(
            preview = preview,
            busy = state.busy,
            error = (state.operation as? OperationStatus.Failed)?.message,
            zoneId = settings.zoneId(),
            locale = LocalConfiguration.current.locales[0],
            onCancel = viewModel::cancelRestore,
            onMerge = viewModel::confirmMerge,
            onReplace = viewModel::confirmRestore,
        )
    }
    if (showEncryptedExport) {
        val confirmationFocus = remember { FocusRequester() }
        val canExport = exportPassphrase.length >= 8 && exportPassphrase == exportPassphraseConfirmation
        val launchEncryptedExport = {
            if (showEncryptedExport && exportPassphrase.length >= 8 && exportPassphrase == exportPassphraseConfirmation) {
                viewModel.prepareDocumentExport(ExportKind.EncryptedBackup, exportPassphrase)
                exportPassphrase = ""
                exportPassphraseConfirmation = ""
                showEncryptedExport = false
                createDocument.launch("whip-${LocalDate.now(settings.zoneId())}.whip.enc.json")
            }
        }
        PaneAwareAlertDialog(
            onDismissRequest = {
                showEncryptedExport = false
                exportPassphrase = ""
                exportPassphraseConfirmation = ""
            },
            title = { Text("Encrypt This Backup") },
            text = {
                WhipDialogBody(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Use at least 8 characters. Whip cannot recover this passphrase.")
                    OutlinedTextField(
                        value = exportPassphrase,
                        onValueChange = { exportPassphrase = it },
                        label = { Text("Passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { confirmationFocus.requestFocus() }),
                        singleLine = true,
                        isError = exportPassphrase.isNotEmpty() && exportPassphrase.length < 8,
                        supportingText = if (exportPassphrase.isNotEmpty() && exportPassphrase.length < 8) ({ Text("Use at least 8 characters.") }) else null,
                        modifier = Modifier.fillMaxWidth().testTag("backup-export-passphrase"),
                    )
                    OutlinedTextField(
                        value = exportPassphraseConfirmation,
                        onValueChange = { exportPassphraseConfirmation = it },
                        label = { Text("Confirm passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { launchEncryptedExport() }),
                        singleLine = true,
                        isError = exportPassphraseConfirmation.isNotEmpty() && exportPassphraseConfirmation != exportPassphrase,
                        supportingText = if (exportPassphraseConfirmation.isNotEmpty() && exportPassphraseConfirmation != exportPassphrase) ({ Text("Passphrases do not match.") }) else null,
                        modifier = Modifier.fillMaxWidth().focusRequester(confirmationFocus).testTag("backup-export-confirmation"),
                    )
                }
            },
            confirmButton = {
                WhipTextButton(
                    enabled = canExport,
                    onClick = launchEncryptedExport,
                ) { Text("Choose Location") }
            },
            dismissButton = { WhipTextButton(onClick = { showEncryptedExport = false; exportPassphrase = ""; exportPassphraseConfirmation = "" }) { Text("Cancel") } },
        )
    }
    if (state.encryptedRestorePending) {
        val unlockBackup = {
            if (restorePassphrase.isNotEmpty() && !state.busy) {
                viewModel.unlockEncryptedRestore(restorePassphrase)
                restorePassphrase = ""
            }
        }
        PaneAwareAlertDialog(
            onDismissRequest = { if (!state.busy) { restorePassphrase = ""; viewModel.cancelEncryptedRestore() } },
            title = { Text("Unlock Encrypted Backup") },
            text = {
                WhipDialogBody(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Enter the original passphrase used to encrypt this backup.")
                    (state.operation as? OperationStatus.Failed)?.let { failure ->
                        WhipStatusCard(
                            kind = WhipStatusKind.Error,
                            title = "Could Not Unlock Backup",
                            message = failure.message,
                            modifier = Modifier.testTag("backup-unlock-error"),
                        )
                    }
                    OutlinedTextField(
                        value = restorePassphrase,
                        onValueChange = { restorePassphrase = it },
                        enabled = !state.busy,
                        label = { Text("Passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { unlockBackup() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("backup-unlock-passphrase"),
                    )
                }
            },
            confirmButton = {
                WhipTextButton(
                    enabled = restorePassphrase.isNotEmpty() && !state.busy,
                    onClick = unlockBackup,
                ) { Text(if (state.busy) "Unlocking…" else "Unlock and Preview") }
            },
            dismissButton = { WhipTextButton(enabled = !state.busy, onClick = { restorePassphrase = ""; viewModel.cancelEncryptedRestore() }) { Text("Cancel") } },
        )
    }
    if (confirmDelete) {
        val destructiveActionDescription = stringResource(R.string.state_destructive_action)
        PermanentDeleteDialog(
            title = stringResource(R.string.settings_reset_confirm_title),
            message = stringResource(R.string.settings_reset_confirm_intro),
            impacts = listOf(
                stringResource(R.string.settings_reset_impact_records),
                stringResource(R.string.settings_reset_impact_preferences),
                stringResource(R.string.settings_reset_impact_backup_link),
            ),
            confirmLabel = stringResource(R.string.settings_reset_confirm_action),
            busy = state.busy || resetSubmitted,
            error = (state.operation as? OperationStatus.Failed)?.message,
            confirmModifier = Modifier.testTag("confirm-reset-whip").semantics {
                stateDescription = destructiveActionDescription
            },
            onDismiss = { confirmDelete = false },
            onConfirm = {
                submitDestructiveActionOnce(
                    alreadySubmitted = resetSubmitted,
                    busy = state.busy,
                    markSubmitted = { resetSubmitted = true },
                    action = {
                        viewModel.deleteAllData(
                            onSuccess = onDataReset,
                            onFailure = { resetSubmitted = false },
                        )
                    },
                )
            },
        )
    }
    if (restPresetsOpen) {
        RestDurationDialog(
            initialSeconds = settings.defaultRestSeconds,
            isWorkoutOverride = false,
            presetSeconds = settings.restTimerPresetSeconds,
            presetsOnly = true,
            saving = restPresetsCoordinator.saving,
            error = restPresetsCoordinator.errorMessage,
            onDismiss = { restPresetsOpen = false; restPresetsCoordinator.clear() },
            onConfirm = {},
            onPresetSecondsChange = { presets ->
                val requestId = restPresetsCoordinator.begin()
                if (requestId != null && !viewModel.updateTypedSetting(requestId) { it.copy(restTimerPresetSeconds = presets) }) {
                    restPresetsCoordinator.finishFailure("Another Settings change is still finishing. Retry saving these presets.")
                }
            },
        )
    }
    if (customEmojiEditorOpen) {
        val initial = settings.customIdentityEmojis.firstOrNull { it.emoji == customEmojiEditorOriginal }
        CustomIdentityEmojiDialog(
            initial = initial,
            existingChoices = settings.customIdentityEmojis,
            onDismiss = {
                customEmojiEditorOpen = false
                customEmojiEditorOriginal = null
            },
            onSave = { choice ->
                viewModel.upsertCustomIdentityEmoji(customEmojiEditorOriginal, choice)
                customEmojiEditorOpen = false
                customEmojiEditorOriginal = null
            },
        )
    }
    if (createUnit) {
        CustomUnitDialog(
            mode = CustomUnitEditMode.Create,
            saving = customUnitCoordinator.saving && customUnitPendingAction == "create",
            error = customUnitCoordinator.errorMessage,
            onDismiss = {
                if (!customUnitCoordinator.saving) {
                    createUnit = false
                    customUnitCoordinator.clear()
                }
            },
            onSave = { name, symbol, dimension, factor ->
                submitCustomUnitAction("create") { requestId ->
                    viewModel.createCustomUnitMutation(
                        requestId = requestId,
                        requestedUnitId = createUnitTargetId,
                        name = name,
                        symbol = symbol,
                        dimension = dimension,
                        factor = factor,
                    )
                }
            },
        )
    }
    renameUnitBoundary?.let { boundary ->
        CustomUnitDialog(
            mode = CustomUnitEditMode.Rename,
            initial = boundary.toUnitDefinition(),
            saving = customUnitCoordinator.saving && customUnitPendingAction == "rename",
            error = customUnitCoordinator.errorMessage,
            onDismiss = {
                if (!customUnitCoordinator.saving) {
                    renameUnitBoundary = null
                    customUnitCoordinator.clear()
                }
            },
            onSave = { name, symbol, _, _ ->
                submitCustomUnitAction("rename") { requestId ->
                    viewModel.renameCustomUnitMutation(requestId, boundary, name, symbol)
                }
            },
        )
    }
    versionUnitBoundary?.let { boundary ->
        CustomUnitDialog(
            mode = CustomUnitEditMode.Version,
            initial = boundary.toUnitDefinition(),
            saving = customUnitCoordinator.saving && customUnitPendingAction == "version",
            error = customUnitCoordinator.errorMessage,
            onDismiss = {
                if (!customUnitCoordinator.saving) {
                    versionUnitBoundary = null
                    customUnitCoordinator.clear()
                }
            },
            onSave = { name, symbol, _, factor ->
                submitCustomUnitAction("version") { requestId ->
                    viewModel.createCustomUnitVersionMutation(
                        requestId = requestId,
                        boundary = boundary,
                        requestedUnitId = versionUnitTargetId,
                        name = name,
                        symbol = symbol,
                        factor = factor,
                    )
                }
            },
        )
    }
    }
}

@Composable
internal fun WideSettingsSectionSidebar(
    selectedSection: SettingsSection,
    onSectionSelected: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.selectableGroup().testTag("settings-wide-section-list"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(SettingsSection.entries, key = SettingsSection::name) { choice ->
            SupportPaneSelectionCard(
                title = choice.label,
                supportingText = choice.supportingText,
                selected = selectedSection == choice,
                onClick = { onSectionSelected(choice) },
                modifier = Modifier
                    .testTag("settings-section-${choice.label}")
                    .focusable(),
                supportingMaxLines = 3,
            )
        }
    }
}

@Composable
internal fun BackupRestorePreviewDialogs(
    modifier: Modifier = Modifier,
    preview: BackupPreview,
    busy: Boolean,
    error: String? = null,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
    onCancel: () -> Unit,
    onMerge: () -> Unit,
    onReplace: () -> Unit,
) {
    val cancelLabel = stringResource(R.string.action_cancel)
    val destructiveActionDescription = stringResource(R.string.state_destructive_action)
    val replaceEverythingLabel = stringResource(R.string.action_replace_everything)
    var confirmReplacement by rememberSaveable(preview.exportedAt.toString()) { mutableStateOf(false) }
    var replacementSubmitted by rememberSaveable(preview.exportedAt.toString()) { mutableStateOf(false) }
    var replacementObservedBusy by rememberSaveable(preview.exportedAt.toString()) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(busy, replacementSubmitted) {
        when {
            replacementSubmitted && busy -> replacementObservedBusy = true
            replacementSubmitted && replacementObservedBusy && !busy -> {
                replacementSubmitted = false
                replacementObservedBusy = false
            }
        }
    }
    if (!confirmReplacement) {
        PaneAwareAlertDialog(
            modifier = modifier,
            onDismissRequest = { if (!busy) onCancel() },
            paneTitle = "Import This Whip Backup?",
            title = null,
            text = {
                val exportedAt = formatSettingsTimestamp(preview.exportedAt, zoneId, locale)
                val recordLabel = if (preview.totalRecords == 1) "record" else "records"
                val populatedTables = preview.tableCounts.count { it.value > 0 }
                val tableLabel = if (populatedTables == 1) "table" else "tables"
                WhipDialogBody(
                    modifier = Modifier
                        .heightIn(max = 520.dp)
                        .testTag("backup-preview-content")
                        .verticalScroll(rememberScrollState()),
                ) {
                    WhipDialogHeading("Import This Whip Backup?")
                    error?.let {
                        WhipStatusCard(
                            kind = WhipStatusKind.Error,
                            title = "Import Not Completed",
                            message = it,
                            modifier = Modifier.testTag("backup-preview-error"),
                        )
                    }
                    Text(
                        "${preview.totalRecords} $recordLabel · Exported $exportedAt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    preview.compatibilityMessage?.let { message ->
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    WhipGroupedInformationCard {
                        WhipButton(
                            modifier = Modifier.fillMaxWidth().testTag("merge-new-data"),
                            enabled = preview.restoreCompatible && !busy,
                            onClick = onMerge,
                        ) { Text(if (preview.restoreCompatible) "Merge New Data" else "Update Required") }
                        Text(
                            "Adds records that are not already present, remaps their relationships, and keeps current settings. Re-importing the same file is safe.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    WhipGroupedInformationCard {
                        WhipOutlinedButton(
                            enabled = preview.restoreCompatible && !busy,
                            onClick = { confirmReplacement = true },
                            modifier = Modifier.fillMaxWidth().testTag("request-replace-everything").semantics {
                                stateDescription = destructiveActionDescription
                            },
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) { Text(replaceEverythingLabel) }
                        Text(
                            "Creates a recovery snapshot first, then replaces all local data, settings, and scheduled work. An interruption rolls back to that snapshot.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        "Backup Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        "$populatedTables populated $tableLabel · " +
                            "preferences ${if (preview.settingsIncluded) "included" else "not included"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${preview.duplicateStableIds} " +
                            (if (preview.duplicateStableIds == 1) "stable ID already exists" else "stable IDs already exist") +
                            " on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                WhipTextButton(
                    enabled = !busy,
                    onClick = onCancel,
                ) { Text(cancelLabel) }
            },
        )
    } else {
        PermanentDeleteDialog(
            title = stringResource(R.string.settings_backup_replace_title),
            message = stringResource(R.string.settings_backup_replace_intro),
            impacts = listOf(
                stringResource(R.string.settings_backup_replace_impact_records),
                stringResource(R.string.settings_backup_replace_impact_preferences),
                stringResource(R.string.settings_backup_replace_impact_recovery),
            ),
            confirmLabel = replaceEverythingLabel,
            busyLabel = stringResource(R.string.settings_backup_replacing),
            busy = busy || replacementSubmitted,
            error = error,
            confirmModifier = Modifier.testTag("confirm-replace-everything").semantics {
                stateDescription = destructiveActionDescription
            },
            onDismiss = { confirmReplacement = false },
            onConfirm = {
                if (!replacementSubmitted && !busy) {
                    replacementSubmitted = true
                    onReplace()
                }
            },
        )
    }
}

@Composable
internal fun CustomIdentityEmojiDialog(
    initial: CustomIdentityEmoji? = null,
    existingChoices: List<CustomIdentityEmoji>,
    onDismiss: () -> Unit,
    onSave: (CustomIdentityEmoji) -> Unit,
) {
    val editorKey = initial?.emoji ?: "new-custom-emoji"
    var emoji by rememberSaveable(editorKey) { mutableStateOf(initial?.emoji.orEmpty()) }
    var name by rememberSaveable(editorKey) { mutableStateOf(initial?.name.orEmpty()) }
    val normalizedEmoji = emoji.trim()
    val normalizedName = name.trim()
    val isBuiltIn = normalizedEmoji.isDefaultIdentityEmoji()
    val emojiIsValid = normalizedEmoji.isIdentityEmoji() && !isBuiltIn
    val duplicateEmoji = existingChoices.any { choice ->
        choice.emoji == normalizedEmoji && choice.emoji != initial?.emoji
    }
    val duplicateName = existingChoices.any { choice ->
        choice.emoji != initial?.emoji && choice.name.equals(normalizedName, ignoreCase = true)
    }
    val canSave = emojiIsValid && normalizedName.isNotBlank() && !duplicateEmoji && !duplicateName

    PaneAwareAlertDialog(
        modifier = Modifier.testTag("custom-emoji-editor"),
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Custom Emoji" else "Edit Custom Emoji") },
        text = {
            WhipDialogBody {
                Text(
                    "Your custom choices are available in every Habit, Goal, and Track emoji picker. The common library stays read-only.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.trim().take(32) },
                    modifier = Modifier.fillMaxWidth().testTag("custom-emoji-editor-glyph"),
                    label = { Text("Emoji") },
                    isError = normalizedEmoji.isNotEmpty() && (!emojiIsValid || duplicateEmoji),
                    supportingText = {
                        Text(
                            when {
                                isBuiltIn -> "This is a built-in emoji and is already always available."
                                duplicateEmoji -> "This custom emoji already exists."
                                normalizedEmoji.isNotEmpty() && !emojiIsValid -> "Enter one emoji, not text or multiple separate emojis."
                                else -> "Choose one emoji for this custom entry."
                            },
                        )
                    },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth().testTag("custom-emoji-editor-name"),
                    label = { Text("Name") },
                    isError = duplicateName,
                    supportingText = {
                        Text(
                            when {
                                duplicateName -> "That custom emoji name is already in use."
                                normalizedName.isBlank() && name.isNotEmpty() -> "Enter a name for your organization."
                                else -> "For example: Deep Work, Family Admin, or Chess Study."
                            },
                        )
                    },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = canSave,
                onClick = { onSave(CustomIdentityEmoji(normalizedEmoji, normalizedName)) },
                modifier = Modifier.testTag("custom-emoji-editor-save"),
            ) { Text(if (initial == null) "Add" else "Save") }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SettingsHeading(text: String) {
    Column(Modifier.padding(top = WhipSpacing.standard)) { EditorSectionHeader(text) }
}

/** Action pairs stay thumb-friendly without squeezing labels at compact widths or large text. */
@Composable
internal fun ResponsiveSettingsActions(
    modifier: Modifier = Modifier,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val stacked = maxWidth < 420.dp || LocalDensity.current.fontScale >= 1.5f
        if (stacked) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        }
    }
}

internal fun formatSettingsTimestamp(
    instant: Instant,
    zoneId: ZoneId,
    locale: Locale,
): String = DateTimeFormatter
    .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    .withLocale(locale)
    .withZone(zoneId)
    .format(instant)

@Composable private fun SettingsToggle(
    label: String,
    checked: Boolean,
    supportingText: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onChange: (Boolean) -> Unit,
) {
    WhipToggleRow(
        title = label,
        supportingText = supportingText,
        checked = checked,
        onCheckedChange = onChange,
        enabled = enabled,
        modifier = modifier,
    )
}

@Composable private fun <T> SettingsDropdown(label: String, values: List<T>, selected: T, text: (T) -> String, onChange: (T) -> Unit) {
    WhipSettingItem(label) { choice(values, selected, text, onChange) }
}

@Composable internal fun NumberSetting(
    label: String,
    current: Int,
    mutation: TypedSettingMutation<Int>,
    validRange: IntRange? = null,
    supportingText: String? = null,
    testTag: String = settingsFreeFormFieldTag(label),
    sourceIdentity: String = "number",
) {
    TransactionalSettingsField(
        label = label,
        current = current,
        parse = { value ->
            value.toIntOrNull()?.takeIf { validRange == null || it in validRange }
        },
        format = Int::toString,
        mutation = mutation,
        invalidMessage = when (validRange) {
            null -> "Enter a whole number."
            else -> "Enter ${validRange.first}–${validRange.last}."
        },
        supportingText = supportingText,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        maxInputLength = 10,
        testTag = testTag,
        sourceIdentity = sourceIdentity,
    )
}

@Composable internal fun ClockSetting(
    label: String,
    currentMinutes: Int,
    mutation: TypedSettingMutation<Int>,
    testTag: String = settingsFreeFormFieldTag(label),
    sourceIdentity: String = "clock",
) {
    TransactionalSettingsField(
        label = label,
        current = currentMinutes,
        parse = ::parseSettingsClock,
        format = ::formatSettingsClock,
        mutation = mutation,
        invalidMessage = "Enter a complete 24-hour time from 00:00 to 23:59.",
        supportingText = "Use 24-hour HH:MM, from 00:00 to 23:59.",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        maxInputLength = 5,
        testTag = testTag,
        sourceIdentity = sourceIdentity,
    )
}

@Composable private fun UnitSetting(label: String, values: List<String>, current: String, onChange: (String) -> Unit) {
    SettingsDropdown(label, values, current, { id ->
        BuiltInUnits.get(id)?.let { unit -> "${unit.name} (${unit.symbol})" } ?: id
    }, onChange)
}

internal fun parseSettingsClock(value: String): Int? {
    if (!SETTINGS_CLOCK_PATTERN.matches(value)) return null
    val hour = value.substring(0, 2).toInt()
    val minute = value.substring(3, 5).toInt()
    return hour * 60 + minute
}

internal fun formatSettingsClock(minutes: Int): String =
    "%02d:%02d".format(Locale.ROOT, minutes / 60, minutes % 60)

internal fun settingsFreeFormFieldTag(label: String): String = "settings-field-" + label
    .lowercase(Locale.ROOT)
    .replace(Regex("[^a-z0-9]+"), "-")
    .trim('-')

@Composable
private fun <T> TransactionalSettingsField(
    label: String,
    current: T,
    parse: (String) -> T?,
    format: (T) -> String,
    mutation: TypedSettingMutation<T>,
    invalidMessage: String,
    supportingText: String?,
    keyboardOptions: KeyboardOptions,
    maxInputLength: Int,
    testTag: String,
    sourceIdentity: String,
) {
    val currentText = format(current)
    var editorOpen by rememberSaveable(label) { mutableStateOf(false) }
    var editorWasOpened by rememberSaveable(label) { mutableStateOf(false) }
    var baselineText by rememberSaveable(label) { mutableStateOf(currentText) }
    var baselineIdentity by rememberSaveable(label) { mutableStateOf(sourceIdentity) }
    var draftText by rememberSaveable(label) { mutableStateOf(currentText) }
    var validationRequested by rememberSaveable(label) { mutableStateOf(false) }
    var inputTooLong by rememberSaveable(label) { mutableStateOf(false) }
    var externalConflict by rememberSaveable(label) { mutableStateOf<String?>(null) }
    var durabilityRetryRequired by rememberSaveable(label) { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable(label) { mutableStateOf(false) }
    val inputFocusRequester = remember { FocusRequester() }
    val actionFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val editorScroll = rememberScrollState()
    val reportEditorState = LocalSettingsTypedEditorState.current

    DisposableEffect(testTag) {
        onDispose { reportEditorState(testTag, false) }
    }

    fun resetEditor(close: Boolean) {
        baselineText = currentText
        baselineIdentity = sourceIdentity
        draftText = currentText
        validationRequested = false
        inputTooLong = false
        externalConflict = null
        durabilityRetryRequired = false
        confirmDiscard = false
        if (close) editorOpen = false
    }

    val coordinator = rememberPersistenceRequestCoordinator(
        state = mutation.state,
        consume = mutation.consume,
        key = testTag,
        requestNamespace = testTag,
        onPersisted = { receipt ->
            mutation.onCompletedWarnings(receipt.warnings)
            resetEditor(close = true)
        },
        orphanedMessage =
            "Whip was interrupted before it could confirm this save. Your draft is still here; review the current value and try again.",
    )

    LaunchedEffect(editorOpen, currentText, sourceIdentity) {
        if (!editorOpen) {
            baselineText = currentText
            baselineIdentity = sourceIdentity
            draftText = currentText
            return@LaunchedEffect
        }

        if (currentText != baselineText || sourceIdentity != baselineIdentity) {
            // A repository may publish process-local SharedPreferences memory
            // before a durable commit result is known. Never promote that
            // observation to the editor's durable baseline while this exact
            // request is still running.
            if (coordinator.saving) return@LaunchedEffect
            val semanticContextChanged = sourceIdentity != baselineIdentity
            if (draftText == baselineText && !semanticContextChanged) {
                draftText = currentText
                inputTooLong = false
            } else {
                externalConflict =
                    "This setting or its mode changed elsewhere. Your draft is still here; review it before saving."
            }
            baselineText = currentText
            baselineIdentity = sourceIdentity
        }
    }

    LaunchedEffect(editorOpen) {
        reportEditorState(testTag, editorOpen)
        if (editorOpen) {
            editorWasOpened = true
            inputFocusRequester.requestFocus()
            keyboard?.show()
        } else if (editorWasOpened) {
            runCatching { actionFocusRequester.requestFocus() }
        }
    }

    val parsed = if (inputTooLong) null else parse(draftText)
    val dirty = inputTooLong || draftText != baselineText
    val saving = coordinator.saving
    val hasUncommittedIntent = dirty || externalConflict != null || durabilityRetryRequired
    val inputProblem = when {
        inputTooLong -> "Use at most $maxInputLength characters."
        validationRequested && parsed == null -> invalidMessage
        else -> null
    }

    fun submitDraft() {
        if (coordinator.saving) return
        validationRequested = true
        val value = parsed ?: return
        val normalized = format(value)
        draftText = normalized
        inputTooLong = false
        if (normalized == currentText && externalConflict == null && !durabilityRetryRequired) {
            resetEditor(close = true)
            return
        }
        coordinator.clear()
        val requestId = coordinator.begin() ?: return
        durabilityRetryRequired = true
        if (!mutation.submit(requestId, value)) {
            coordinator.finishFailure("Another settings change is still finishing. Your draft is still here; try again.")
        }
    }

    WhipSettingItem(
        title = label.uiTitleCase(),
        modifier = Modifier
            .focusRequester(actionFocusRequester)
            .testTag(testTag),
        itemKey = testTag,
    ) {
        description(supportingText)
        edit(currentText) {
            baselineText = currentText
            baselineIdentity = sourceIdentity
            draftText = currentText
            validationRequested = false
            inputTooLong = false
            coordinator.clear()
            externalConflict = null
            editorOpen = true
        }
    }

    if (editorOpen) {
        ProductivityEditorDialog(
            modifier = Modifier.widthIn(max = 560.dp),
            testTag = "$testTag-editor",
            paneTitle = "Edit $label",
            stableHeight = false,
            inputBlocked = saving,
            inputBlockedLabel = "Saving $label",
            onDismissRequest = {
                if (!saving) {
                    if (hasUncommittedIntent) confirmDiscard = true else resetEditor(close = true)
                }
            },
            title = { Text("Edit $label") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(editorScroll),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    externalConflict?.let { message ->
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                    coordinator.errorMessage?.let { message ->
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .semantics { liveRegion = LiveRegionMode.Polite }
                                .testTag("$testTag-save-error"),
                        )
                    }
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { value ->
                            inputTooLong = value.length > maxInputLength
                            draftText = value.take(maxInputLength)
                            validationRequested = false
                        },
                        label = { Text(label) },
                        supportingText = {
                            Text(
                                inputProblem ?: supportingText ?: "Enter the value, then choose Save.",
                                modifier = if (inputProblem == null) {
                                    Modifier
                                } else {
                                    Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                                },
                            )
                        },
                        isError = inputProblem != null,
                        enabled = !saving,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = KeyboardActions(onDone = { submitDraft() }),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(inputFocusRequester)
                            .onPreviewKeyEvent { event ->
                                if (
                                    event.type == KeyEventType.KeyDown &&
                                    event.key == Key.Escape &&
                                    !saving
                                ) {
                                    if (hasUncommittedIntent) confirmDiscard = true else resetEditor(close = true)
                                    true
                                } else {
                                    false
                                }
                            }
                            .semantics {
                                stateDescription = when {
                                    saving -> "Saving"
                                    coordinator.errorMessage != null -> "Not saved"
                                    dirty -> "Edited, not saved"
                                    else -> "Matches saved value"
                                }
                            }
                            .testTag("$testTag-input"),
                    )
                    if (saving) {
                        Text(
                            "Waiting for Whip to confirm the saved value…",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }
            },
            confirmButton = {
                WhipButton(
                    onClick = ::submitDraft,
                    enabled = !saving && parsed != null &&
                        (dirty || externalConflict != null || durabilityRetryRequired),
                    modifier = Modifier.testTag("$testTag-save"),
                ) { Text("Save") }
            },
            dismissButton = {
                WhipTextButton(
                    onClick = { resetEditor(close = true) },
                    enabled = !saving,
                    modifier = Modifier.testTag("$testTag-cancel"),
                ) { Text("Cancel") }
            },
        )
    }

    if (confirmDiscard) {
        UnsavedChangesDialog(
            subject = "setting",
            onKeepEditing = { confirmDiscard = false },
            onDiscard = { resetEditor(close = true) },
            modifier = Modifier.testTag("$testTag-discard-confirmation"),
        )
    }
}

private val SETTINGS_CLOCK_PATTERN = Regex("(?:[01]\\d|2[0-3]):[0-5]\\d")

internal enum class CustomUnitEditMode { Create, Rename, Version }

@Composable internal fun CustomUnitDialog(
    modifier: Modifier = Modifier,
    testTag: String? = "custom-unit-dialog",
    mode: CustomUnitEditMode,
    initial: com.whip.app.domain.UnitDefinition? = null,
    initialDimension: UnitDimension? = null,
    dimensionLocked: Boolean = false,
    saving: Boolean = false,
    error: String? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, UnitDimension, Double) -> Unit,
) {
    val startingName = initial?.name.orEmpty()
    val startingSymbol = initial?.symbol.orEmpty()
    val startingDimension = initial?.dimension ?: initialDimension ?: UnitDimension.Count
    val startingFactor = initial?.toCanonicalFactor?.toString() ?: "1"
    var name by rememberSaveable(initial?.id, mode) { mutableStateOf(startingName) }
    var symbol by rememberSaveable(initial?.id, mode) { mutableStateOf(startingSymbol) }
    var dimension by rememberSaveable(initial?.id, mode, initialDimension) { mutableStateOf(startingDimension) }
    var factor by rememberSaveable(initial?.id, mode) { mutableStateOf(startingFactor) }
    var confirmDiscard by rememberSaveable(initial?.id, mode) { mutableStateOf(false) }
    var validationRequested by rememberSaveable(initial?.id, mode) { mutableStateOf(false) }
    val dirty = name != startingName || symbol != startingSymbol || dimension != startingDimension ||
        (mode != CustomUnitEditMode.Rename && factor != startingFactor)
    fun requestDismiss() {
        if (saving) return
        if (dirty) confirmDiscard = true else onDismiss()
    }
    val canonicalLabel = canonicalUnitLabel(dimension)
    val nameFocus = remember { FocusRequester() }
    val symbolFocus = remember { FocusRequester() }
    val factorFocus = remember { FocusRequester() }
    val nameValidationTarget = remember { BringIntoViewRequester() }
    val factorValidationTarget = remember { BringIntoViewRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val parsedFactor = factor.toWhipDoubleOrNull()
    val nameInvalid = validationRequested && name.isBlank()
    val factorInvalid = validationRequested && mode != CustomUnitEditMode.Rename &&
        (parsedFactor == null || parsedFactor <= 0.0)
    LaunchedEffect(initial?.id, mode) { nameFocus.requestFocus() }
    LaunchedEffect(nameInvalid, factorInvalid) {
        when {
            nameInvalid -> {
                nameFocus.requestFocus()
                nameValidationTarget.bringIntoView()
            }
            factorInvalid -> {
                factorFocus.requestFocus()
                factorValidationTarget.bringIntoView()
            }
        }
    }
    PaneAwareAlertDialog(
        modifier = modifier,
        testTag = testTag,
        onDismissRequest = ::requestDismiss,
        title = {
            Text(
                when (mode) {
                    CustomUnitEditMode.Create -> "Create Custom Unit"
                    CustomUnitEditMode.Rename -> "Rename Custom Unit"
                    CustomUnitEditMode.Version -> "Create Conversion Version"
                },
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    name,
                    { name = it.take(100) },
                    enabled = !saving,
                    label = { Text("Name *, e.g. glass") },
                    singleLine = true,
                    isError = nameInvalid,
                    supportingText = if (nameInvalid) {{ Text("Name is required") }} else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { symbolFocus.requestFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(nameValidationTarget)
                        .focusRequester(nameFocus)
                        .testTag("custom-unit-name"),
                )
                OutlinedTextField(
                    symbol,
                    { symbol = it.take(20) },
                    enabled = !saving,
                    label = { Text("Symbol, e.g. gl") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (mode == CustomUnitEditMode.Rename) ImeAction.Done else ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { factorFocus.requestFocus() },
                        onDone = { keyboard?.hide() },
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(symbolFocus).testTag("custom-unit-symbol"),
                )
                if (mode == CustomUnitEditMode.Create && !dimensionLocked && !saving) {
                    SettingsDropdown("Dimension", UnitDimension.entries, dimension, UnitDimension::uiLabel) { dimension = it }
                } else Text("Dimension: ${dimension.uiLabel()}", style = MaterialTheme.typography.bodySmall)
                if (mode != CustomUnitEditMode.Rename) {
                    Text(
                        "Whip stores ${dimension.label} values in $canonicalLabel so compatible units can be compared and linked.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        factor,
                        { factor = it.take(64) },
                        enabled = !saving,
                        label = { Text("1 ${symbol.ifBlank { name.ifBlank { "custom unit" } }} equals how many $canonicalLabel?") },
                        isError = factorInvalid,
                        supportingText = {
                            Column {
                                if (factorInvalid) Text("Enter a number greater than 0")
                                Text(customUnitExample(dimension))
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(factorValidationTarget)
                            .focusRequester(factorFocus)
                            .testTag("custom-unit-factor"),
                    )
                    if (mode == CustomUnitEditMode.Version) Text(
                        "The existing unit stays attached to current definitions and history, then is archived from new pickers. This new version becomes available for future selections; Whip does not silently retarget anything.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else Text(
                    "Renaming changes this label wherever the unit appears, including history. Recorded numbers and conversion meaning stay unchanged.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (saving) {
                    Text(
                        "Waiting for Whip to confirm the saved unit…",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                    )
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = !saving,
                onClick = {
                    validationRequested = true
                    if (name.isBlank() || (mode != CustomUnitEditMode.Rename && (parsedFactor == null || parsedFactor <= 0.0))) {
                        return@WhipTextButton
                    }
                    onSave(name, symbol, dimension, parsedFactor ?: requireNotNull(initial).toCanonicalFactor)
                },
                modifier = Modifier.testTag("custom-unit-confirm"),
            ) { Text(if (saving) "Saving…" else if (mode == CustomUnitEditMode.Rename) "Save Name" else "Create") }
        },
        dismissButton = { WhipTextButton(enabled = !saving, onClick = ::requestDismiss) { Text("Cancel") } },
    )
    if (confirmDiscard && !saving) {
        UnsavedChangesDialog(
            subject = "custom unit",
            onKeepEditing = { confirmDiscard = false },
            onDiscard = {
                confirmDiscard = false
                onDismiss()
            },
            modifier = Modifier.testTag("custom-unit-discard-confirmation"),
        )
    }
}

private fun canonicalUnitLabel(dimension: UnitDimension): String = when (dimension) {
    UnitDimension.Count -> "counts"
    UnitDimension.Duration -> "seconds"
    UnitDimension.Distance -> "metres"
    UnitDimension.Volume -> "millilitres"
    UnitDimension.Mass -> "kilograms"
    UnitDimension.Length -> "metres"
    UnitDimension.Money -> "currency units"
    UnitDimension.Energy -> "kilojoules"
    UnitDimension.Temperature -> "degrees Celsius"
    UnitDimension.Speed -> "metres per second"
    UnitDimension.Pace -> "seconds per metre"
    UnitDimension.Frequency -> "hertz"
    UnitDimension.Percentage -> "percent"
    UnitDimension.Unitless -> "base numbers"
    UnitDimension.Custom -> "custom base units"
}

private fun customUnitExample(dimension: UnitDimension): String = when (dimension) {
    UnitDimension.Mass -> "Example: stone → enter 6.35029318 because 1 st = 6.35029318 kg."
    UnitDimension.Volume -> "Example: 250 mL glass → enter 250."
    UnitDimension.Distance, UnitDimension.Length -> "Example: 400 m lap → enter 400."
    UnitDimension.Duration -> "Example: 15 minute block → enter 900 seconds."
    UnitDimension.Energy -> "Example: 1 kcal → enter 4.184 kJ."
    UnitDimension.Temperature -> "Celsius, Fahrenheit, and Kelvin are built in. Custom scales must use the same zero point as Celsius."
    UnitDimension.Speed -> "Example: 1 km/h → enter 0.27777778 metres per second."
    UnitDimension.Pace -> "Example: 1 min/km → enter 0.06 seconds per metre."
    UnitDimension.Frequency -> "Example: 1 per minute → enter 0.01666667 hertz."
    else -> "Enter the amount represented by one of your custom units."
}

private fun categoryAllocationModeLabel(value: String): String = when (value) {
    "Full" -> "Full contribution"
    "Fractional" -> "Split contribution"
    "PrimaryOnly" -> "First linked category only"
    else -> value
}

@Composable internal fun TimeZoneSetting(
    current: String?,
    mutation: TypedSettingMutation<String>,
    testTag: String = "settings-field-time-zone",
) {
    val effectiveCurrent = current ?: ZoneId.systemDefault().id
    TransactionalSettingsField(
        label = "Time zone ID",
        current = effectiveCurrent,
        parse = { value -> parseSettingsTimeZone(value)?.id },
        format = String::trim,
        mutation = mutation,
        invalidMessage = "Enter a valid region or UTC-offset time zone.",
        supportingText = "Examples: America/Toronto, Europe/London, +02:00",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        maxInputLength = 128,
        testTag = testTag,
        sourceIdentity = if (current == null) "follow-device" else "explicit",
    )
}

internal fun parseSettingsTimeZone(value: String): ZoneId? {
    val normalized = value.trim()
    return runCatching { ZoneId.of(normalized) }.getOrNull()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
