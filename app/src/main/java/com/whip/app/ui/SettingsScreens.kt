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
import androidx.health.connect.client.PermissionController
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.whip.app.core.HealthDataType
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
import com.whip.app.health.HealthConnectAvailability
import com.whip.app.reminders.ReminderNotifications
import com.whip.app.reminders.HabitReminderNotifications
import com.whip.app.reminders.GoalReminderNotifications
import com.whip.app.reminders.RestTimerNotifications
import com.whip.app.reminders.FocusTimerNotifications
import com.whip.app.BuildConfig
import com.whip.app.data.BackupPreview
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whip.app.core.AppSettings
import com.whip.app.core.PersistenceRequestState
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

internal enum class SettingsSection(val label: String, val supportingText: String) {
    Appearance("Appearance & Home", "Theme, presentation, home sections, and keyboard shortcuts"),
    Planning("Planning & Units", "Dates, units, numbers, effort, recurring tasks, and review defaults"),
    Organization("Organization", "Areas, tags, and naming systems"),
    Reminders("Reminders", "Notification access, delivery status, testing, and quiet hours"),
    DataPrivacy("Data & Privacy", "Local data, backups, restore, export, health access, and deletion"),
    AboutDiagnostics("About Whip", "App identity, version, package, and data-handling summary"),
}

internal enum class DataPrivacyGroup { Health, Backup, Reset }

internal val DataPrivacyGroupOrder = listOf(
    DataPrivacyGroup.Health,
    DataPrivacyGroup.Backup,
    DataPrivacyGroup.Reset,
)

internal fun supportsAndroidDynamicColor(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.S

internal fun healthConnectOrphanedMessage(pendingAction: String?): String = when {
    pendingAction == "sync" ->
        "The previous Health Connect sync was interrupted. Review Last Sync and the copied-record count. Sync Now is idempotent and safe to repeat."
    pendingAction == "delete" ->
        "The previous local-copy deletion was interrupted. Review the local-copy count and pending-recovery message, then retry deletion; the cleanup is idempotent."
    pendingAction != null ->
        "The previous Health Connect policy change was interrupted. Review the current master toggle and category choices. If they already match your intent, do not repeat the change."
    else ->
        "The previous Health Connect action was interrupted. Review the current Health controls before trying again."
}

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
) {
    val context = LocalContext.current
    val currentLocale = LocalConfiguration.current.locales[0]
    var pendingExport by rememberSaveable { mutableStateOf(ExportKind.Backup) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmHealthDelete by rememberSaveable { mutableStateOf(false) }
    var resetSubmitted by rememberSaveable { mutableStateOf(false) }
    var createUnit by rememberSaveable { mutableStateOf(false) }
    var createUnitTargetId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var renameUnitBoundary by rememberSaveable { mutableStateOf<CustomUnitBoundary?>(null) }
    var versionUnitBoundary by rememberSaveable { mutableStateOf<CustomUnitBoundary?>(null) }
    var versionUnitTargetId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var customUnitPendingAction by rememberSaveable { mutableStateOf<String?>(null) }
    var healthPendingAction by rememberSaveable { mutableStateOf<String?>(null) }
    var healthOutcome by rememberSaveable { mutableStateOf<String?>(null) }
    var healthWarning by rememberSaveable { mutableStateOf<String?>(null) }
    var typedSettingWarning by rememberSaveable { mutableStateOf<String?>(null) }
    var customEmojiEditorOpen by rememberSaveable { mutableStateOf(false) }
    var customEmojiEditorOriginal by rememberSaveable { mutableStateOf<String?>(null) }
    var diagnosticRefresh by rememberSaveable { mutableIntStateOf(0) }
    var notificationTestMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var deliveryDetailsExpanded by rememberSaveable { mutableStateOf(false) }
    var notificationTroubleshootingExpanded by rememberSaveable { mutableStateOf(false) }
    var showEncryptedExport by rememberSaveable { mutableStateOf(false) }
    var exportPassphrase by remember { mutableStateOf("") }
    var exportPassphraseConfirmation by remember { mutableStateOf("") }
    var pendingExportPassphrase by remember { mutableStateOf<String?>(null) }
    var restorePassphrase by remember { mutableStateOf("") }
    var localSection by rememberSaveable { mutableStateOf(SettingsSection.Appearance) }
    var activeTypedSettingTag by rememberSaveable { mutableStateOf<String?>(null) }
    val section = selectedSection ?: localSection
    val externalSectionNavigation = selectedSection != null
    fun selectSection(next: SettingsSection) {
        if (activeTypedSettingTag != null) return
        if (externalSectionNavigation) onSectionChange(next) else localSection = next
    }
    var compactSectionOpen by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        diagnosticRefresh++
    }
    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { viewModel.export(it, pendingExport, pendingExportPassphrase) }
        pendingExportPassphrase = null
    }
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::previewRestore)
    }
    val backupFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(viewModel::configurePortableBackupFolder)
    }
    val healthPermissions = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
        viewModel::onHealthPermissionsResult,
    )
    val settings = state.settings
    val typedSettingMutationState by viewModel.typedSettingMutationState.collectAsStateWithLifecycle()
    val customUnitMutationState by viewModel.customUnitMutationState.collectAsStateWithLifecycle()
    val healthMutationState by viewModel.healthMutationState.collectAsStateWithLifecycle()
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
    val healthCoordinator = rememberPersistenceRequestCoordinator(
        state = healthMutationState,
        consume = viewModel::consumeHealthMutation,
        key = "settings-health-connect",
        requestNamespace = "settings-health",
        onPersisted = { receipt ->
            val completedAction = healthPendingAction
            healthOutcome = when (receipt.kind) {
                HealthMutationKind.Policy -> null
                HealthMutationKind.Sync -> "Synchronized ${receipt.affectedEntries} Health Connect records this run."
                HealthMutationKind.DeleteLocalCopies -> "Deleted ${receipt.affectedEntries} Health Connect copies from Whip."
            }
            healthWarning = receipt.warnings.joinToString(" ").takeIf(String::isNotBlank)
            if (receipt.kind == HealthMutationKind.Policy && completedAction == "enable") {
                healthPermissions.launch(viewModel.requiredHealthPermissions())
            }
            if (receipt.kind == HealthMutationKind.DeleteLocalCopies) confirmHealthDelete = false
            healthPendingAction = null
        },
        orphanedMessage = healthConnectOrphanedMessage(healthPendingAction),
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
    fun submitHealthAction(action: String, submit: (String) -> Boolean) {
        val requestId = healthCoordinator.begin() ?: return
        healthPendingAction = action
        healthOutcome = null
        healthWarning = null
        if (!submit(requestId)) {
            healthPendingAction = null
            healthCoordinator.finishFailure("Another Health Connect action is still finishing. Review it and try again.")
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WhipPageHeader(
                        title = "Settings",
                        supportingText = "Choices save immediately. Typed values open in an editor and change only after Save is confirmed.",
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("settings-category-list"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
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
                )
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
            contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
        if (state.busy) item {
            WhipStatusCard(
                kind = WhipStatusKind.Loading,
                title = "Updating Settings",
                message = "Your changes are being applied on this device.",
                modifier = Modifier.testTag("settings-loading-status"),
            )
        }
        state.message?.let { message -> item {
            val messageKind = if (message.contains(
                    Regex("failed|error|could not|unable|denied|unavailable", RegexOption.IGNORE_CASE),
                )
            ) {
                WhipStatusKind.Error
            } else {
                WhipStatusKind.Success
            }
            WhipStatusCard(
                kind = messageKind,
                title = if (messageKind == WhipStatusKind.Error) "Action Not Completed" else "Settings Updated",
                message = message,
                actionLabel = "Dismiss",
                onAction = viewModel::consumeMessage,
                modifier = Modifier.testTag("settings-result-status"),
            )
        } }
        typedSettingWarning?.let { warning -> item {
            WhipStatusCard(
                kind = WhipStatusKind.Status,
                title = "Setting Saved with Warnings",
                message = warning,
                actionLabel = "Dismiss",
                onAction = { typedSettingWarning = null },
                modifier = Modifier.testTag("settings-completed-warning"),
            )
        } }

        if (section == SettingsSection.Appearance) {
        item {
            SettingsToggle(
                "Show advanced controls by default",
                settings.powerMode,
                supportingText = "Opens optional planning and configuration groups automatically. It does not add or remove capabilities.",
            ) { selected -> viewModel.update { it.copy(powerMode = selected) } }
        }
        item {
            SettingsToggle(
                "Low-pressure Habit presentation",
                settings.lowPressureMode,
                supportingText = "De-emphasizes streaks and success/failure language in Habit views without changing history.",
            ) { selected -> viewModel.update { it.copy(lowPressureMode = selected) } }
        }
        item {
            WhipSettingsSectionCard {
                Text("Hardware Keyboard", fontWeight = FontWeight.Bold)
                Text("Ctrl+H Home · Ctrl+K Search · Ctrl+N contextual add · Ctrl+1–5 switch Tasks, Habits, Goals, Tracks, Gym", style = MaterialTheme.typography.bodySmall)
            }
        }
        item { SettingsDropdown("Theme", AppThemeMode.entries, settings.themeMode, AppThemeMode::label) { selected -> viewModel.update { it.copy(themeMode = selected) } } }
        item {
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
        item {
            SettingsToggle(
                "Use compact item rows",
                settings.compactItemLayout,
                supportingText = "Shows list-sized Tasks, Habits, Goals, and Tracks with their primary action. Expand a row for its complete information and controls.",
                modifier = Modifier.testTag("settings-compact-item-layout"),
            ) { selected -> viewModel.update { it.copy(compactItemLayout = selected) } }
        }
        item {
            SettingsHeading("Opening Area")
            Text(
                "Choose whether a new Whip session returns to the Area you used last or always starts from one chosen view. Widget shortcuts switch the current Area immediately.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
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
        if (settings.areaOpeningMode == AreaOpeningMode.Chosen) item {
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
        item {
            SettingsHeading("Home Overview")
            Text(
                "Choose which sections and empty-day shortcuts appear on Whip Home. Main navigation and saved data remain unchanged. Pinning an item reveals and expands its section so the action always has a visible destination. Visible sections can start expanded or collapsed.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        val visibleHomeSectionCount = settings.homeSections.count { it !in settings.hiddenHomeSections }
        settings.homeSections.forEachIndexed { index, section ->
            item(key = "home-${section.name}") {
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
                        WhipSettingsRow(
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

        }

        if (section == SettingsSection.Planning) {
        item { SettingsHeading("Date and Number Defaults") }
        item {
            SettingsDropdown("First day of week", DayOfWeek.entries, settings.firstDayOfWeek, { it.getDisplayName(TextStyle.FULL, currentLocale) }) { selected -> viewModel.update { it.copy(firstDayOfWeek = selected) } }
            Text("Sets weekday order in calendars and editors, and groups weekly Review and Gym analytics. Existing Habit schedules keep their own week start.", style = MaterialTheme.typography.bodySmall)
        }
        item {
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
        item {
            ClockSetting(
                label = "Late-night day cutoff",
                currentMinutes = settings.dayCutoffMinutes,
                mutation = appSettingMutation { current, minutes -> current.copy(dayCutoffMinutes = minutes) },
            )
            Text("Before this time, Today still uses the previous calendar date. Use 00:00 for the standard midnight boundary.", style = MaterialTheme.typography.bodySmall)
        }
        item {
            NumberSetting(
                label = "Default decimal precision",
                current = settings.numberPrecision,
                mutation = appSettingMutation { current, value -> current.copy(numberPrecision = value) },
                validRange = 0..6,
            )
        }
        item { SettingsHeading("Unit Defaults") }
        item { UnitSetting("Mass", listOf("kilogram", "pound", "gram"), settings.massUnitId) { value -> viewModel.update { it.copy(massUnitId = value) } } }
        item { UnitSetting("Distance", listOf("kilometre", "mile", "distance_m"), settings.distanceUnitId) { value -> viewModel.update { it.copy(distanceUnitId = value) } } }
        item { UnitSetting("Volume", listOf("litre", "millilitre", "cup", "fluid_ounce"), settings.volumeUnitId) { value -> viewModel.update { it.copy(volumeUnitId = value) } } }
        item {
            UnitSetting("Gym summaries and new exercises", listOf("kilogram", "pound"), settings.gymWeightUnitId) { value ->
                viewModel.update { it.copy(gymWeightUnitId = value) }
            }
            Text(
                "This changes aggregate gym displays and the default for new exercises. Existing exercises and machine profiles keep their own equipment unit.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            Text("Custom Units", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Create reusable units for Habit entries, Goal progress, and number fields in Tracks. Units can also be created beside the Unit control while editing a supported item.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (state.customUnits.isEmpty()) Text("No custom conversion units yet.", style = MaterialTheme.typography.bodySmall)
            state.customUnits.forEach { unit ->
                var unitMenuOpen by rememberSaveable(unit.id) { mutableStateOf(false) }
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "${unit.name}${unit.symbol.takeIf(String::isNotBlank)?.let { " ($it)" }.orEmpty()}${if (unit.archived) " · Archived" else ""}",
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "1 ${unit.symbol.ifBlank { unit.name }} = ${unit.toCanonicalFactor} ${canonicalUnitLabel(unit.dimension)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        WhipOverflowMenu(
                            label = "Options for ${unit.name}",
                            expanded = unitMenuOpen,
                            onExpandedChange = { unitMenuOpen = it },
                            modifier = Modifier.testTag("custom-unit-menu-${unit.id}"),
                            enabled = !customUnitCoordinator.saving,
                        ) {
                            WhipMenuItem("Rename", onClick = {
                                unitMenuOpen = false
                                renameUnitBoundary = unit.customUnitBoundary()
                            })
                            if (!unit.archived) {
                                WhipMenuItem("Create New Version", onClick = {
                                    unitMenuOpen = false
                                    versionUnitBoundary = unit.customUnitBoundary()
                                    versionUnitTargetId = UUID.randomUUID().toString()
                                })
                            }
                            WhipMenuItem(
                                if (unit.archived) "Restore" else "Archive",
                                onClick = {
                                    unitMenuOpen = false
                                    val boundary = unit.customUnitBoundary()
                                    submitCustomUnitAction("archive") { requestId ->
                                        viewModel.setCustomUnitArchivedMutation(
                                            requestId = requestId,
                                            boundary = boundary,
                                            archived = !boundary.archived,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
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
        item { SettingsHeading("Review Defaults") }
        item { SettingsDropdown("Default review period", ReviewPeriod.entries, settings.reviewPeriod, ReviewPeriod::label) { value -> viewModel.update { it.copy(reviewPeriod = value) } } }

        }

        if (section == SettingsSection.Planning) {
        item { SettingsHeading("Task Defaults") }
        item {
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
        item {
            SettingsToggle(
                "Show habits in Task Agenda and Calendar",
                settings.showHabitsInTaskPlanning,
            ) { value -> viewModel.update { it.copy(showHabitsInTaskPlanning = value) } }
            Text(
                "Habits remain separate records; this only projects scheduled habits into planning views.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item { SettingsDropdown("Repeating-task subtask default", RepeatStepPolicy.entries, settings.defaultTaskStepPolicy, RepeatStepPolicy::uiLabel) { value -> viewModel.update { it.copy(defaultTaskStepPolicy = value) } } }
        item {
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
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("smart-task-capture-examples"),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
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
        }
        item { SettingsHeading("Habit Defaults") }
        item {
            SettingsDropdown("Default week start for new Habits", DayOfWeek.entries, settings.defaultHabitWeekStart, { it.getDisplayName(TextStyle.FULL, currentLocale) }) { value -> viewModel.update { it.copy(defaultHabitWeekStart = value) } }
            Text("Existing Habits keep their own saved week start.", style = MaterialTheme.typography.bodySmall)
        }
        }

        if (section == SettingsSection.Planning) {
        item { SettingsHeading("Gym Defaults") }
        item { SettingsDropdown("Estimated 1RM formula", listOf("Epley", "Brzycki"), settings.oneRepMaxFormula, { it }) { value -> viewModel.update { it.copy(oneRepMaxFormula = value) } } }
        item {
            NumberSetting(
                label = "Estimated 1RM rep cutoff",
                current = settings.oneRepMaxRepCutoff,
                mutation = appSettingMutation { current, value -> current.copy(oneRepMaxRepCutoff = value) },
                validRange = 1..36,
                supportingText = "Sets above this repetition count are excluded from estimated 1RM records.",
            )
        }
        item {
            NumberSetting(
                label = "Default rest time (seconds)",
                current = settings.defaultRestSeconds,
                mutation = appSettingMutation { current, value -> current.copy(defaultRestSeconds = value) },
                validRange = 15..3_600,
                supportingText = "Used when a workout has no temporary rest-time override.",
            )
        }
        item {
            Text(
                "Rest presets · ${settings.restTimerPresetSeconds.joinToString(" · ") { seconds -> "%d:%02d".format(seconds / 60, seconds % 60) }}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "To edit these shortcuts, open an active workout and choose Rest → Adjust → Manage Presets.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { SettingsToggle("Rest timer sound", settings.timerSound) { value -> viewModel.update { it.copy(timerSound = value) } }; SettingsToggle("Rest timer vibration", settings.timerVibration) { value -> viewModel.update { it.copy(timerVibration = value) } }; SettingsToggle("Keep workout screen awake by default", settings.keepScreenAwake) { value -> viewModel.update { it.copy(keepScreenAwake = value) } } }
        item { SettingsToggle("Use compact workout set rows", settings.gymCompactSetRows) { value -> viewModel.update { it.copy(gymCompactSetRows = value) } } }
        item {
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
            val effortField = when {
                settings.showGymRpe -> "RPE"
                settings.showGymRir -> "RIR"
                else -> "None"
            }
            SettingsDropdown("Default workout effort field", listOf("None", "RPE", "RIR"), effortField, { it }) { value ->
                viewModel.update { it.copy(showGymRpe = value == "RPE", showGymRir = value == "RIR") }
            }
            Text("Individual exercises can override this default. RPE and RIR are alternative scales, so only one is shown at a time.", style = MaterialTheme.typography.bodySmall)
            SettingsToggle("Show tempo fields", settings.showGymTempo) { value -> viewModel.update { it.copy(showGymTempo = value) } }
            SettingsToggle("Include warm-ups in volume and PRs", settings.includeWarmupsInGymStats) { value -> viewModel.update { it.copy(includeWarmupsInGymStats = value) } }
        }
        item {
            Text("Hard-Set Classifications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
        item {
            SettingsDropdown(
                "Overlapping category allocation",
                listOf("Full", "Fractional", "PrimaryOnly"),
                settings.categoryAllocationMode,
                ::categoryAllocationModeLabel,
            ) { selected -> viewModel.update { it.copy(categoryAllocationMode = selected) } }
            Text(
                if (settings.categoryAllocationMode == "PrimaryOnly") {
                    "Counts the set only in the first linked category in your Gym Categories order. Reorder categories to control which one wins; this is unrelated to 5/3/1 Push/Pull assistance roles."
                } else {
                    "Controls how one hard set contributes when an exercise belongs to multiple Exercise Library categories. These categories do not assign 5/3/1 assistance roles."
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item { SettingsToggle("Adjust estimated 1RM using RPE/RIR", settings.adjustE1rmForEffort) { selected -> viewModel.update { it.copy(adjustE1rmForEffort = selected) } } }
        item { SettingsToggle("Allow assisted exercises in personal records", settings.includeAssistedInPersonalRecords) { selected -> viewModel.update { it.copy(includeAssistedInPersonalRecords = selected) } } }
        }

        if (section == SettingsSection.Organization) {
        item { SettingsHeading("Organization") }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Areas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Create named areas to group related tasks, habits, goals, and tracks across search and review.")
                    Text("${state.areas.count { !it.archived }} active · ${state.areas.count { it.archived }} archived · ${state.areaUsage.values.sumOf(AreaUsageCounts::total) + state.unassignedAreaUsage.total} items", style = MaterialTheme.typography.bodySmall)
                    WhipButton(onClick = onEditAreas, modifier = Modifier.fillMaxWidth()) { Text("Manage Areas") }
                }
            }
        }
        item {
            SettingsHeading("Custom Emojis")
            Text(
                "Name reusable emoji choices for your own organization. Whip's ${IDENTITY_EMOJI_PRESETS.size} common emojis are always available and cannot be renamed, replaced, or deleted.",
            )
            Text(
                "Removing a custom choice does not change Habits, Goals, or Tracks that already use its emoji.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        item {
            Card(Modifier.fillMaxWidth().testTag("settings-tags-summary")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
        }
        }

        if (section == SettingsSection.Reminders) {
        item { SettingsHeading("Notifications") }
        item(key = "notification-diagnostics-$diagnosticRefresh") {
            Card(Modifier.fillMaxWidth().testTag("notification-diagnostics")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reminder Delivery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(label, modifier = Modifier.weight(1f))
                                Text(state.label, color = if (state == NotificationDeliveryState.Deliverable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    DisclosureButton("Troubleshooting", notificationTroubleshootingExpanded, { notificationTroubleshootingExpanded = !notificationTroubleshootingExpanded }, Modifier.fillMaxWidth())
                    if (notificationTroubleshootingExpanded) {
                        Text(
                            if (batteryUnrestricted) "Battery optimization is unrestricted for Whip."
                            else "Android battery optimization may delay reminders while Whip is idle.",
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
                    notificationTestMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        item {
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
                    notificationTestMessage = if (ReminderNotifications.showTest(context)) {
                        "Test sent. Check the notification shade."
                    } else {
                        "Android blocked the test notification."
                    }
                    diagnosticRefresh++
                },
                modifier = Modifier.fillMaxWidth().testTag("send-test-notification"),
            ) { Text("Send Test Notification") }
            if (taskNotificationChannel == null && testNotificationAvailability.enabled) {
                Text("Sending a test creates the Task reminders channel if it does not exist yet.", style = MaterialTheme.typography.bodySmall)
            }
            AvailabilityNotice("Send test notification", testNotificationAvailability)
        }
        item { WhipTextButton(onClick = { diagnosticRefresh++ }) { Text("Refresh Notification Status") } }
        item {
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
        item { SettingsHeading("Backup & Export") }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Portable Backup Folder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Choose a folder in Files, Drive, or removable storage. Automatic portable backups are plain JSON, not encrypted. Whip keeps access after restart, verifies every backup after writing it, and never deletes unrelated files.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (state.portableBackup.configured) {
                        Text("Folder: ${state.portableBackup.folderLabel ?: "Selected folder"}")
                        state.portableBackup.lastBackupAtMillis?.let { millis ->
                            val whenSaved = formatSettingsTimestamp(
                                instant = Instant.ofEpochMilli(millis),
                                zoneId = settings.zoneId(),
                                locale = LocalConfiguration.current.locales[0],
                            )
                            Text(
                                state.portableBackup.lastBackupFileName?.let { fileName ->
                                    stringResource(R.string.settings_backup_last_verified_file, whenSaved, fileName)
                                } ?: stringResource(R.string.settings_backup_last_verified, whenSaved),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        state.portableBackup.lastError?.let { error ->
                            Text("Last backup warning or error: $error", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        WhipButton(
                            onClick = viewModel::createPortableBackup,
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Back Up Now") }
                        SettingsToggle(
                            "Automatic daily backup",
                            state.portableBackup.automaticEnabled,
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
                                ) { Text("Change Folder") }
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
        }
        item {
            WhipOutlinedButton(
                onClick = { pendingExport = ExportKind.Backup; createDocument.launch("whip-${LocalDate.now(settings.zoneId())}.whip.json") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save Plain JSON Backup") }
        }
        item {
            WhipOutlinedButton(
                onClick = { showEncryptedExport = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save Passphrase-Encrypted Backup") }
            Text(
                "Encrypted backups are authenticated and safer for health history and other sensitive data. The passphrase is never saved and cannot be recovered. Plain JSON remains available for interoperability.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item { WhipOutlinedButton(onClick = { openDocument.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Preview and Restore Backup") } }
        item {
            Text("Export CSV", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                maxItemsInEachRow = 3,
            ) {
                listOf(ExportKind.TasksCsv to "Tasks", ExportKind.HabitsCsv to "Habits", ExportKind.GoalsCsv to "Goals", ExportKind.GymCsv to "Gym", ExportKind.TracksCsv to "Tracks").forEach { (kind, label) ->
                    WhipTextButton(
                        onClick = { pendingExport = kind; createDocument.launch("whip-${label.lowercase()}-${LocalDate.now(settings.zoneId())}.csv") },
                        modifier = Modifier.width(96.dp),
                    ) { Text(label) }
                }
            }
        }
        }
        if (dataPrivacyPass == DataPrivacyGroup.Reset) {
        item {
            val destructiveActionDescription = stringResource(R.string.state_destructive_action)
            WhipDangerZone {
                Text(
                    stringResource(R.string.settings_reset_explanation),
                    style = MaterialTheme.typography.bodySmall,
                )
                WhipActionRow(
                    title = stringResource(R.string.settings_reset_entry_title),
                    onClick = {
                        resetSubmitted = false
                        confirmDelete = true
                    },
                    modifier = Modifier.testTag("reset-whip-action").semantics {
                        stateDescription = destructiveActionDescription
                    },
                    navigates = false,
                    danger = true,
                )
            }
        }
        }

        if (dataPrivacyPass == DataPrivacyGroup.Health) {
        item { SettingsHeading("Health & Privacy") }
        item {
            Text("Whip stores data locally and does not require an account. Estimated 1RM and correlations are informational, not medical or safety advice.")
            Text(
                when (state.healthConnect.availability) {
                    HealthConnectAvailability.Available -> "Health Connect is available. Whip only requests read access for the categories below."
                    HealthConnectAvailability.InstallOrUpdate -> "Install or update Health Connect to enable health data sync."
                    HealthConnectAvailability.Unsupported -> "Health Connect is not supported on this device."
                },
                style = MaterialTheme.typography.bodySmall,
            )
            if (state.healthConnect.availability == HealthConnectAvailability.InstallOrUpdate) {
                WhipOutlinedButton(
                    onClick = {
                        val packageId = "com.google.android.apps.healthdata"
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageId".toUri()))
                        }.onFailure {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://play.google.com/store/apps/details?id=$packageId".toUri(),
                                ),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Install or Update Health Connect") }
            }
        }
        item {
            val healthAvailable = state.healthConnect.availability == HealthConnectAvailability.Available
            SettingsToggle(
                label = "Sync with Health Connect",
                checked = settings.healthConnectEnabled,
                supportingText = when {
                    settings.healthConnectDeletionPending -> "Local Health Connect deletion is pending recovery. Finish Delete Health Connect Copies below before turning sync on again."
                    settings.healthConnectEnabled -> "Whip reads only the selected categories. Turn this off to stop future reads; existing local copies remain until you delete them below."
                    settings.healthDataTypes.isEmpty() -> stringResource(R.string.settings_health_sync_paused_empty)
                    else -> stringResource(
                        R.string.settings_health_sync_paused_saved,
                        settings.healthDataTypes.joinToString { it.label },
                    )
                },
                enabled = !settings.healthConnectDeletionPending && !healthCoordinator.saving &&
                    (settings.healthConnectEnabled || (healthAvailable && settings.healthDataTypes.isNotEmpty())),
                modifier = Modifier.testTag("settings-health-enabled"),
            ) { enabled ->
                submitHealthAction(if (enabled) "enable" else "disable") { requestId ->
                    viewModel.setHealthConnectEnabled(requestId, enabled)
                }
            }
            if (!healthAvailable && !settings.healthConnectEnabled) {
                Text(
                    "Sync cannot be enabled on this device, but you can still review saved scope and delete Whip's local copies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HealthDataType.entries.forEach { type ->
            item(key = "health-${type.name}") {
                val selected = type in settings.healthDataTypes
                val permission = viewModel.requiredHealthPermissionsFor(type)
                val granted = permission in state.healthConnect.grantedPermissions
                HealthDataTypeSetting(
                    type = type,
                    syncEnabled = settings.healthConnectEnabled,
                    selected = selected,
                    accessGranted = granted,
                    controlsEnabled = !healthCoordinator.saving,
                    onChange = { enabled ->
                        submitHealthAction("category-${type.name}") { requestId ->
                            viewModel.setHealthDataType(requestId, type, enabled)
                        }
                    },
                )
            }
        }
        item {
            NumberSetting(
                label = "Health read window (days)",
                current = settings.healthSyncDays,
                mutation = TypedSettingMutation(
                    state = typedSettingMutationState,
                    consume = viewModel::consumeTypedSettingMutation,
                    submit = viewModel::setHealthSyncDays,
                ),
                validRange = 1..365,
            )
            Text(
                "Each sync reconciles the selected categories inside this recent window. Older Health Connect copies already in Whip are preserved.",
                style = MaterialTheme.typography.bodySmall,
            )
            val requiredPermissions = viewModel.requiredHealthPermissions()
            val hasAllAccess = requiredPermissions.isNotEmpty() &&
                state.healthConnect.grantedPermissions.containsAll(requiredPermissions)
            val healthAvailable = state.healthConnect.availability == HealthConnectAvailability.Available
            val accessAvailability = ControlAvailability(
                enabled = healthAvailable && settings.healthDataTypes.isNotEmpty() && !healthCoordinator.saving,
                unavailableExplanation = when {
                    !healthAvailable -> "Health Connect is unavailable on this device."
                    settings.healthDataTypes.isEmpty() -> "Select at least one Health Connect category."
                    healthCoordinator.saving -> "Wait for the current Health Connect action to finish."
                    else -> null
                },
            )
            val syncAvailability = ControlAvailability(
                enabled = healthAvailable && settings.healthConnectEnabled &&
                    settings.healthDataTypes.isNotEmpty() && hasAllAccess && !healthCoordinator.saving,
                unavailableExplanation = when {
                    !healthAvailable -> "Health Connect is unavailable on this device."
                    !settings.healthConnectEnabled -> "Turn on Health Connect sync."
                    settings.healthDataTypes.isEmpty() -> "Select at least one Health Connect category."
                    !hasAllAccess -> "Review Android access for every selected category first."
                    healthCoordinator.saving -> "Wait for the current Health Connect action to finish."
                    else -> null
                },
            )
            ResponsiveSettingsActions(
                first = { buttonModifier ->
                    WhipOutlinedButton(
                        onClick = { healthPermissions.launch(requiredPermissions) },
                        enabled = accessAvailability.enabled,
                        modifier = buttonModifier.testTag("health-review-access"),
                    ) { Text("Review Access") }
                },
                second = { buttonModifier ->
                    WhipButton(
                        onClick = {
                            submitHealthAction("sync") { requestId ->
                                viewModel.syncHealthConnect(requestId)
                            }
                        },
                        enabled = syncAvailability.enabled,
                        modifier = buttonModifier.testTag("health-sync-now"),
                    ) { Text("Sync Now") }
                },
            )
            AvailabilityNotice("Review access", accessAvailability)
            AvailabilityNotice("Sync now", syncAvailability)
            healthOutcome?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            healthWarning?.let { warning ->
                WhipStatusCard(
                    kind = WhipStatusKind.Status,
                    title = "Completed with Warnings",
                    message = warning,
                    modifier = Modifier.testTag("health-connect-warning"),
                )
            }
            healthCoordinator.errorMessage?.let { message ->
                WhipStatusCard(
                    kind = WhipStatusKind.Error,
                    title = "Health Connect Action Not Completed",
                    message = message,
                    modifier = Modifier.testTag("health-connect-error"),
                )
            }
            state.healthConnect.lastSync?.let { lastSync ->
                val formatted = formatSettingsTimestamp(
                    instant = lastSync,
                    zoneId = settings.zoneId(),
                    locale = LocalConfiguration.current.locales[0],
                )
                val synchronizedEntries = pluralStringResource(
                    R.plurals.settings_health_imported_entries,
                    state.healthConnect.importedEntries,
                    state.healthConnect.importedEntries,
                )
                Text(
                    stringResource(
                        R.string.settings_health_last_sync,
                        formatted,
                        "$synchronizedEntries this run",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            WhipSettingsSectionCard {
                Text("Local Health Connect Copies", fontWeight = FontWeight.Bold)
                Text(
                    "Whip currently stores ${state.healthImportedEntryCount} Health Connect ${if (state.healthImportedEntryCount == 1) "record" else "records"}.${if (settings.healthConnectDeletionPending) " A previous deletion is pending safe recovery." else ""} Deleting them keeps your measurement definitions, selected categories, Android permissions, provider records, other Whip data, and existing backup files. Linked Habits, goals, and trends may change. Re-enabling sync can copy provider data again.",
                    style = MaterialTheme.typography.bodySmall,
                )
                WhipOutlinedButton(
                    onClick = { confirmHealthDelete = true },
                    enabled = (state.healthImportedEntryCount > 0 || settings.healthConnectDeletionPending) &&
                        !healthCoordinator.saving,
                    modifier = Modifier.fillMaxWidth().testTag("delete-health-connect-copies"),
                ) { Text("Delete Health Connect Copies from Whip") }
            }
        }
        }
        }
        if (section == SettingsSection.AboutDiagnostics) {
        item { SettingsHeading("About Whip") }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Whip", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${if (BuildConfig.DEBUG) "Development" else "Release"} · ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        modifier = Modifier.testTag("about-build-identity"),
                    )
                    Text(context.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Your data stays on this device unless you explicitly export or sync it.")
                }
            }
        }
        }
        }
        }
    }
    }

    state.backupPreview?.let { preview ->
        BackupRestorePreviewDialogs(
            preview = preview,
            busy = state.busy,
            zoneId = settings.zoneId(),
            locale = LocalConfiguration.current.locales[0],
            onCancel = viewModel::cancelRestore,
            onMerge = viewModel::confirmMerge,
            onReplace = viewModel::confirmRestore,
        )
    }
    if (showEncryptedExport) {
        PaneAwareAlertDialog(
            onDismissRequest = {
                showEncryptedExport = false
                exportPassphrase = ""
                exportPassphraseConfirmation = ""
            },
            title = { Text("Encrypt This Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Use at least 8 characters. Whip cannot recover this passphrase.")
                    OutlinedTextField(
                        value = exportPassphrase,
                        onValueChange = { exportPassphrase = it },
                        label = { Text("Passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = exportPassphraseConfirmation,
                        onValueChange = { exportPassphraseConfirmation = it },
                        label = { Text("Confirm passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = exportPassphraseConfirmation.isNotEmpty() && exportPassphraseConfirmation != exportPassphrase,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                WhipTextButton(
                    enabled = exportPassphrase.length >= 8 && exportPassphrase == exportPassphraseConfirmation,
                    onClick = {
                        pendingExport = ExportKind.EncryptedBackup
                        pendingExportPassphrase = exportPassphrase
                        exportPassphrase = ""
                        exportPassphraseConfirmation = ""
                        showEncryptedExport = false
                        createDocument.launch("whip-${LocalDate.now(settings.zoneId())}.whip.enc.json")
                    },
                ) { Text("Choose Location") }
            },
            dismissButton = { WhipTextButton(onClick = { showEncryptedExport = false; exportPassphrase = ""; exportPassphraseConfirmation = "" }) { Text("Cancel") } },
        )
    }
    if (state.encryptedRestorePending) {
        PaneAwareAlertDialog(
            onDismissRequest = { restorePassphrase = ""; viewModel.cancelEncryptedRestore() },
            title = { Text("Unlock Encrypted Backup") },
            text = {
                OutlinedTextField(
                    value = restorePassphrase,
                    onValueChange = { restorePassphrase = it },
                    label = { Text("Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                WhipTextButton(
                    enabled = restorePassphrase.isNotEmpty() && !state.busy,
                    onClick = {
                        viewModel.unlockEncryptedRestore(restorePassphrase)
                        restorePassphrase = ""
                    },
                ) { Text("Unlock and Preview") }
            },
            dismissButton = { WhipTextButton(onClick = { restorePassphrase = ""; viewModel.cancelEncryptedRestore() }) { Text("Cancel") } },
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
    if (confirmHealthDelete) {
        PermanentDeleteDialog(
            title = "Delete Health Connect Copies from Whip?",
            message = "This turns off future Health Connect sync and deletes ${state.healthImportedEntryCount} local ${if (state.healthImportedEntryCount == 1) "record" else "records"} from Whip.",
            impacts = listOf(
                "Health Connect provider records and Android permissions are not changed.",
                "Measurement definitions, selected categories, other Whip data, and existing backup files are kept.",
                "Linked Habits, goals, and trends may change; re-enabling sync can copy provider data again.",
            ),
            confirmLabel = "Turn Off Sync and Delete Copies",
            busy = healthCoordinator.saving,
            error = healthCoordinator.errorMessage,
            confirmModifier = Modifier.testTag("confirm-delete-health-connect-copies"),
            onDismiss = { if (!healthCoordinator.saving) confirmHealthDelete = false },
            onConfirm = {
                submitHealthAction("delete") { requestId ->
                    viewModel.deleteHealthConnectCopies(requestId)
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
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(SettingsSection.entries, key = SettingsSection::name) { choice ->
            NavigationDrawerItem(
                label = { Text(choice.label) },
                selected = selectedSection == choice,
                onClick = { onSectionSelected(choice) },
                modifier = Modifier
                    .testTag("settings-section-${choice.label}")
                    .focusable(),
            )
        }
    }
}

@Composable
internal fun BackupRestorePreviewDialogs(
    preview: BackupPreview,
    busy: Boolean,
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
            onDismissRequest = { if (!busy) onCancel() },
            title = { Text("Import This Whip Backup?") },
            text = {
                val exportedAt = formatSettingsTimestamp(preview.exportedAt, zoneId, locale)
                Text(
                    "Exported $exportedAt\n${preview.totalRecords} records in ${preview.tableCounts.count { it.value > 0 }} tables\n" +
                        "Preferences: ${if (preview.settingsIncluded) "included" else "not included"}\n" +
                        "${preview.duplicateStableIds} stable IDs already exist.\n\n" +
                        (preview.compatibilityMessage ?: "MERGE adds records that are not already present, remaps their relationships, keeps current settings, and commits atomically. Re-importing the same file is safe.\n\nREPLACE snapshots the current database and preferences first, then replaces all local data, settings, and scheduled work; interruption rolls back to that snapshot."),
                )
            },
            confirmButton = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    WhipTextButton(
                        enabled = preview.restoreCompatible && !busy,
                        onClick = onMerge,
                    ) { Text(if (preview.restoreCompatible) "Merge New Data" else "Update Required") }
                    WhipTextButton(
                        enabled = preview.restoreCompatible && !busy,
                        onClick = { confirmReplacement = true },
                        modifier = Modifier.testTag("request-replace-everything").semantics {
                            stateDescription = destructiveActionDescription
                        },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) { Text(replaceEverythingLabel) }
                }
            },
            dismissButton = { WhipTextButton(onClick = onCancel, enabled = !busy) { Text(cancelLabel) } },
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    EditorSectionHeader(text)
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

@Composable
internal fun HealthDataTypeSetting(
    type: HealthDataType,
    syncEnabled: Boolean,
    selected: Boolean,
    accessGranted: Boolean,
    controlsEnabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    WhipSettingsRow(
        title = if (!syncEnabled) {
            "${type.label} · sync paused"
        } else {
            "${type.label} · ${if (accessGranted) "Android access allowed" else "Android access not allowed"}"
        },
        modifier = Modifier.testTag("health-type-${type.name}"),
        checked = selected,
        enabled = controlsEnabled,
        onCheckedChange = onChange,
    )
}

@Composable private fun SettingsToggle(
    label: String,
    checked: Boolean,
    supportingText: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onChange: (Boolean) -> Unit,
) {
    WhipSettingsRow(
        title = label,
        supportingText = supportingText,
        checked = checked,
        onCheckedChange = onChange,
        enabled = enabled,
        modifier = modifier,
    )
}

@Composable private fun <T> SettingsDropdown(label: String, values: List<T>, selected: T, text: (T) -> String, onChange: (T) -> Unit) {
    SelectionField(label = label, values = values, selected = selected, valueText = text, onSelect = onChange)
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

    WhipActionRow(
        title = label,
        supportingText = buildString {
            append("Current: ")
            append(currentText)
            supportingText?.let { append(". ").append(it) }
        },
        onClick = {
            baselineText = currentText
            baselineIdentity = sourceIdentity
            draftText = currentText
            validationRequested = false
            inputTooLong = false
            coordinator.clear()
            externalConflict = null
            editorOpen = true
        },
        modifier = Modifier
            .focusRequester(actionFocusRequester)
            .semantics { stateDescription = "Saved value $currentText. Activate to edit." }
            .testTag(testTag),
    )

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
