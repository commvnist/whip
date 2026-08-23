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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.PermissionController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import com.whip.app.core.AppThemeMode
import com.whip.app.core.HomeSection
import com.whip.app.core.HealthDataType
import com.whip.app.core.ReviewPeriod
import com.whip.app.core.zoneId
import com.whip.app.domain.RepeatStepPolicy
import com.whip.app.domain.AvoidMissingPolicy
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.toWhipDoubleOrNull
import com.whip.app.health.HealthConnectAvailability
import com.whip.app.reminders.ReminderNotifications
import com.whip.app.reminders.HabitReminderNotifications
import com.whip.app.reminders.GoalReminderNotifications
import com.whip.app.reminders.RestTimerNotifications
import com.whip.app.reminders.AutomationPromptNotifications
import com.whip.app.reminders.FocusTimerNotifications
import com.whip.app.BuildConfig
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private enum class SettingsSection(val label: String) {
    General("General"),
    Organization("Organization"),
    Reminders("Reminders"),
    Data("Data & backup"),
    Health("Health & privacy"),
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    innerPadding: PaddingValues,
    viewModel: SettingsViewModel,
    firstRunBackupRequest: String? = null,
    onFirstRunBackupRequestConsumed: () -> Unit = {},
    onEditAreas: () -> Unit = {},
) {
    val context = LocalContext.current
    var pendingExport by rememberSaveable { mutableStateOf(ExportKind.Backup) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var createUnit by rememberSaveable { mutableStateOf(false) }
    var renameUnitId by rememberSaveable { mutableStateOf<String?>(null) }
    var versionUnitId by rememberSaveable { mutableStateOf<String?>(null) }
    var taxonomyEditKind by rememberSaveable { mutableStateOf<String?>(null) }
    var taxonomyEditId by rememberSaveable { mutableStateOf<String?>(null) }
    var diagnosticRefresh by rememberSaveable { mutableIntStateOf(0) }
    var notificationTestMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showEncryptedExport by rememberSaveable { mutableStateOf(false) }
    var exportPassphrase by remember { mutableStateOf("") }
    var exportPassphraseConfirmation by remember { mutableStateOf("") }
    var pendingExportPassphrase by remember { mutableStateOf<String?>(null) }
    var restorePassphrase by remember { mutableStateOf("") }
    var section by rememberSaveable { mutableStateOf(SettingsSection.General) }
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
        "Automation prompts" to AutomationPromptNotifications.CHANNEL_ID,
        "Focus timer" to FocusTimerNotifications.channelId,
    )
    val channelHealth = reminderChannels.associate { (label, id) ->
        label to (notificationManager.getNotificationChannel(id)?.importance?.let { it != NotificationManager.IMPORTANCE_NONE } == true)
    }
    val taskNotificationChannelEnabled = channelHealth["Task reminders"] == true
    val allReminderChannelsEnabled = channelHealth.values.all { it }
    val batteryUnrestricted = context.getSystemService(PowerManager::class.java)
        .isIgnoringBatteryOptimizations(context.packageName)
    LaunchedEffect(firstRunBackupRequest) {
        when (firstRunBackupRequest) {
            "Encrypted" -> {
                section = SettingsSection.Data
                showEncryptedExport = true
                onFirstRunBackupRequestConsumed()
            }
            "Plaintext" -> {
                section = SettingsSection.Data
                pendingExport = ExportKind.Backup
                createDocument.launch("whip-${LocalDate.now(settings.zoneId())}.whip.json")
                onFirstRunBackupRequestConsumed()
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(innerPadding)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Local preferences, data controls, defaults, and export.")
            Text(
                "${if (BuildConfig.DEBUG) "Development" else "Release"} · ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · ${context.packageName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("about-build-identity"),
            )
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SettingsSection.entries.forEach { choice ->
                    androidx.compose.material3.FilterChip(
                        selected = section == choice,
                        onClick = { section = choice },
                        label = { Text(choice.label) },
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("settings-list"),
            contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
        if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.primary); TextButton(onClick = viewModel::consumeMessage) { Text("Dismiss") } } }

        if (section == SettingsSection.General) {
        item { SettingsHeading("Appearance and Home") }
        item { SettingsToggle("Power mode (surface advanced controls sooner)", settings.powerMode) { selected -> viewModel.update { it.copy(powerMode = selected) } } }
        item { SettingsToggle("Low-pressure presentation", settings.lowPressureMode) { selected -> viewModel.update { it.copy(lowPressureMode = selected) } } }
        if (settings.powerMode) item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Hardware keyboard", fontWeight = FontWeight.Bold)
                    Text("Ctrl+K search · Ctrl+N contextual add · Ctrl+1–5 switch Home, Tasks, Habits, Goals, Gym", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { SettingsDropdown("Theme", AppThemeMode.entries, settings.themeMode, { it.name }) { selected -> viewModel.update { it.copy(themeMode = selected) } } }
        item { SettingsToggle("Use Android dynamic colors", settings.dynamicColor) { selected -> viewModel.update { it.copy(dynamicColor = selected) } } }
        settings.homeSections.forEachIndexed { index, section ->
            item(key = "home-${section.name}") {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = section !in settings.hiddenHomeSections, onCheckedChange = { visible -> viewModel.update { current -> current.copy(hiddenHomeSections = if (visible) current.hiddenHomeSections - section else current.hiddenHomeSections + section) } })
                            Text(section.name, modifier = Modifier.weight(1f).padding(start = 8.dp), fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { viewModel.update { current -> current.copy(collapsedHomeSections = if (section in current.collapsedHomeSections) current.collapsedHomeSections - section else current.collapsedHomeSections + section) } }) { Text(if (section in settings.collapsedHomeSections) "Expand" else "Collapse") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(enabled = index > 0, onClick = { viewModel.moveHomeSection(section, -1) }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move ${section.name} up", modifier = Modifier.size(26.dp))
                            }
                            IconButton(enabled = index < settings.homeSections.lastIndex, onClick = { viewModel.moveHomeSection(section, 1) }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move ${section.name} down", modifier = Modifier.size(26.dp))
                            }
                        }
                    }
                }
            }
        }

        item { SettingsHeading("Date, units, and numbers") }
        item { SettingsDropdown("First day of week", DayOfWeek.entries, settings.firstDayOfWeek, { it.name.lowercase().replaceFirstChar(Char::uppercase) }) { selected -> viewModel.update { it.copy(firstDayOfWeek = selected) } } }
        item {
            val followDevice = settings.timeZoneId == null
            SettingsToggle("Follow device time zone", followDevice) { enabled ->
                viewModel.update { it.copy(timeZoneId = if (enabled) null else ZoneId.systemDefault().id) }
            }
            if (!followDevice) {
                TimeZoneSetting(settings.timeZoneId.orEmpty()) { value ->
                    viewModel.update { it.copy(timeZoneId = value) }
                }
            }
            Text("Active time zone: ${settings.zoneId().id}. Historical entries keep their saved local date and offset.", style = MaterialTheme.typography.bodySmall)
        }
        item { ClockSetting("Late-night day cutoff", settings.dayCutoffMinutes) { minutes -> viewModel.update { it.copy(dayCutoffMinutes = minutes) } } }
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
        item { NumberSetting("Default decimal precision", settings.numberPrecision) { value -> viewModel.update { it.copy(numberPrecision = value.coerceIn(0, 6)) } } }
        item {
            Text("Custom units", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Custom units are not limited to drinks. Choose a dimension first—mass units convert to kilograms, volume units to millilitres, distance units to metres, and so on.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (state.customUnits.isEmpty()) Text("No custom conversion units yet.", style = MaterialTheme.typography.bodySmall)
            state.customUnits.forEach { unit ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "${unit.name}${unit.symbol.takeIf(String::isNotBlank)?.let { " ($it)" }.orEmpty()}${if (unit.archived) " · Archived" else ""}",
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "1 ${unit.symbol.ifBlank { unit.name }} = ${unit.toCanonicalFactor} ${canonicalUnitLabel(unit.dimension)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { renameUnitId = unit.id }, modifier = Modifier.weight(1f)) { Text("Rename") }
                            TextButton(onClick = { versionUnitId = unit.id }, modifier = Modifier.weight(1f)) { Text("New version") }
                            TextButton(onClick = { viewModel.setCustomUnitArchived(unit.id, !unit.archived) }, modifier = Modifier.weight(1f)) {
                                Text(if (unit.archived) "Restore" else "Archive")
                            }
                        }
                    }
                }
            }
            OutlinedButton(onClick = { createUnit = true }, modifier = Modifier.fillMaxWidth()) { Text("Create custom unit") }
        }
        item { SettingsDropdown("Default review period", ReviewPeriod.entries, settings.reviewPeriod, { it.name }) { value -> viewModel.update { it.copy(reviewPeriod = value) } } }

        item { SettingsHeading("Tasks") }
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
        item { SettingsDropdown("Repeating-task subtask default", RepeatStepPolicy.entries, settings.defaultTaskStepPolicy, { it.name }) { value -> viewModel.update { it.copy(defaultTaskStepPolicy = value) } } }
        item {
            SettingsToggle("Enable smart task capture", settings.naturalLanguageTaskCapture) { value ->
                viewModel.update { it.copy(naturalLanguageTaskCapture = value) }
            }
            Text(
                "Adds an explicit parser for phrases such as “tomorrow”, “every 2 weeks”, and ISO dates. It never sends task text off-device.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item { SettingsDropdown("Habit week starts", DayOfWeek.entries, settings.defaultHabitWeekStart, { it.name.lowercase().replaceFirstChar(Char::uppercase) }) { value -> viewModel.update { it.copy(defaultHabitWeekStart = value) } } }
        item { SettingsDropdown("Avoid-habit missing-day default", AvoidMissingPolicy.entries, settings.defaultAvoidMissingPolicy, { it.name }) { value -> viewModel.update { it.copy(defaultAvoidMissingPolicy = value) } } }

        item { SettingsHeading("Gym defaults") }
        item { SettingsDropdown("Estimated 1RM formula", listOf("Epley", "Brzycki"), settings.oneRepMaxFormula, { it }) { value -> viewModel.update { it.copy(oneRepMaxFormula = value) } } }
        item { NumberSetting("Estimated 1RM rep cutoff", settings.oneRepMaxRepCutoff) { value -> viewModel.update { it.copy(oneRepMaxRepCutoff = value.coerceIn(1, 36)) } } }
        item { NumberSetting("Default rest seconds", settings.defaultRestSeconds) { value -> viewModel.update { it.copy(defaultRestSeconds = value.coerceAtLeast(0)) } } }
        item { SettingsToggle("Rest timer sound", settings.timerSound) { value -> viewModel.update { it.copy(timerSound = value) } }; SettingsToggle("Rest timer vibration", settings.timerVibration) { value -> viewModel.update { it.copy(timerVibration = value) } }; SettingsToggle("Keep workout screen awake by default", settings.keepScreenAwake) { value -> viewModel.update { it.copy(keepScreenAwake = value) } } }
        item { SettingsToggle("Use compact workout set rows", settings.gymCompactSetRows) { value -> viewModel.update { it.copy(gymCompactSetRows = value) } } }
        item {
            SettingsToggle("Start rest timer when a set completes", settings.restTimerAutoStart) { value ->
                if (value && !settings.restTimerAutoStart && !notificationPermissionGranted) {
                    viewModel.markNotificationPermissionRequested()
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.update { it.copy(restTimerAutoStart = value) }
            }
            SettingsToggle("Show RPE fields", settings.showGymRpe) { value -> viewModel.update { it.copy(showGymRpe = value) } }
            SettingsToggle("Show RIR fields", settings.showGymRir) { value -> viewModel.update { it.copy(showGymRir = value) } }
            SettingsToggle("Show tempo fields", settings.showGymTempo) { value -> viewModel.update { it.copy(showGymTempo = value) } }
            SettingsToggle("Include warm-ups in volume and PRs", settings.includeWarmupsInGymStats) { value -> viewModel.update { it.copy(includeWarmupsInGymStats = value) } }
        }
        item {
            Text("Hard-set classifications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Working", "BackOff", "Drop", "Amrap", "Failure", "WarmUp").forEach { value ->
                    androidx.compose.material3.FilterChip(
                        selected = value in settings.hardSetClassifications,
                        onClick = {
                            viewModel.update { current ->
                                val changed = if (value in current.hardSetClassifications) current.hardSetClassifications - value else current.hardSetClassifications + value
                                current.copy(hardSetClassifications = changed.ifEmpty { setOf("Working") })
                            }
                        },
                        label = { Text(value.replace(Regex("([a-z])([A-Z])"), "$1 $2")) },
                    )
                }
            }
            Text("Only these classifications count in category hard-set summaries. Volume and PR inclusion remain separately configurable.", style = MaterialTheme.typography.bodySmall)
        }
        item { SettingsDropdown("Overlapping category allocation", listOf("Full", "Fractional", "PrimaryOnly"), settings.categoryAllocationMode, { it }) { selected -> viewModel.update { it.copy(categoryAllocationMode = selected) } } }
        item { SettingsToggle("Adjust estimated 1RM using RPE/RIR", settings.adjustE1rmForEffort) { selected -> viewModel.update { it.copy(adjustE1rmForEffort = selected) } } }
        item { SettingsToggle("Allow assisted lifts in personal records", settings.includeAssistedInPersonalRecords) { selected -> viewModel.update { it.copy(includeAssistedInPersonalRecords = selected) } } }
        }

        if (section == SettingsSection.Organization) {
        item { SettingsHeading("Organization") }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Areas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Create your own named areas to group related items across Tasks, Habits, Goals, Search, and Review.")
                    Text("${state.areas.count { !it.archived }} active · ${state.areas.count { it.archived }} archived · ${state.areaUsage.values.sumOf(AreaUsageCounts::total) + state.unassignedAreaUsage.total} items", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onEditAreas, modifier = Modifier.fillMaxWidth()) { Text("Edit areas") }
                }
            }
        }
        item {
            SettingsHeading("Tags")
            Text("Tags can describe many facets of an item. Areas remain its single primary home.")
            if (state.tags.isEmpty()) Text("No tags yet.", style = MaterialTheme.typography.bodySmall)
            state.tags.forEach { tag ->
                Card(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("#${tag.name}${if (tag.archived) " · Archived" else ""}", modifier = Modifier.weight(1f))
                        TextButton(onClick = { taxonomyEditKind = "Tag"; taxonomyEditId = tag.id }) { Text("Rename") }
                        TextButton(onClick = { viewModel.setTagArchived(tag.id, !tag.archived) }) { Text(if (tag.archived) "Restore" else "Archive") }
                    }
                }
            }
        }
        }

        if (section == SettingsSection.Reminders) {
        item { SettingsHeading("Notifications") }
        item(key = "notification-diagnostics-$diagnosticRefresh") {
            Card(Modifier.fillMaxWidth().testTag("notification-diagnostics")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Delivery diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            notificationPermissionPermanentlyDenied -> "Notification permission is blocked. Re-enable it in Android settings before relying on reminders."
                            notificationPermissionRationale -> "Notification permission was declined. Whip only uses it for reminders and rest timers you explicitly enable."
                            !notificationPermissionGranted -> "Notification permission has not been granted. Reminders cannot be shown."
                            !appNotificationsEnabled -> "Notifications are disabled for Whip in Android settings."
                            !allReminderChannelsEnabled -> "One or more reminder channels are disabled in Android settings."
                            else -> "Notification permission and all active reminder channels are enabled."
                        },
                        color = if (notificationPermissionGranted && appNotificationsEnabled && allReminderChannelsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    channelHealth.forEach { (label, enabled) ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, modifier = Modifier.weight(1f))
                            Text(if (enabled) "Enabled" else "Blocked", color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                    }
                    Text(
                        if (batteryUnrestricted) {
                            "Battery optimization is unrestricted for Whip."
                        } else {
                            "Android battery optimization may delay reminders while Whip is idle. Exact behavior depends on your device settings."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    notificationTestMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        if (!notificationPermissionGranted) {
            item {
                OutlinedButton(
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
                ) { Text(if (notificationPermissionPermanentlyDenied) "Repair notification permission" else "Grant notification permission") }
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Open Android notification settings") }
        }
        reminderChannels.forEach { (label, channelId) ->
            if (channelHealth[label] == false) {
                item(key = "repair-channel-$channelId") {
                    OutlinedButton(
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
            }
        }
        item {
            OutlinedButton(
                onClick = { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Open battery optimization settings") }
        }
        item {
            val testNotificationAvailability = ControlAvailability(
                enabled = notificationPermissionGranted && appNotificationsEnabled && taskNotificationChannelEnabled,
                unavailableExplanation = when {
                    !notificationPermissionGranted -> "Allow Whip notifications in Android settings."
                    !appNotificationsEnabled -> "Turn on Whip notifications in Android settings."
                    !taskNotificationChannelEnabled -> "Turn on the Tasks reminder channel in Android settings."
                    else -> null
                },
            )
            Button(
                enabled = testNotificationAvailability.enabled,
                onClick = {
                    notificationTestMessage = if (ReminderNotifications.showTest(context)) {
                        "Test sent. Check the notification shade."
                    } else {
                        "Android blocked the test notification."
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("send-test-notification"),
            ) { Text("Send test notification") }
            AvailabilityNotice("Send test notification", testNotificationAvailability)
        }
        item { TextButton(onClick = { diagnosticRefresh++ }) { Text("Refresh notification status") } }
        item {
            SettingsToggle("Allow task location cues", settings.locationRemindersEnabled) { enabled ->
                viewModel.update { it.copy(locationRemindersEnabled = enabled) }
            }
            val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val backgroundGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            Text(
                when {
                    !fineGranted -> "Precise location is not allowed. Location cues are inactive."
                    !backgroundGranted -> "Foreground location is allowed, but Android must be set to “Allow all the time” for background arrival/leave cues."
                    else -> "Precise and background location are allowed. Coordinates remain local and geofencing avoids continuous tracking."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (fineGranted && backgroundGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = "package:${context.packageName}".toUri()
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Open Android location permissions") }
            Text(
                "Location cues are optional and may use additional battery. Disable this switch to unregister every Whip geofence without deleting saved places.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            val enabled = settings.quietStartMinutes != null && settings.quietEndMinutes != null
            SettingsToggle("Enable notification quiet hours", enabled) { selected ->
                viewModel.update { current ->
                    current.copy(
                        quietStartMinutes = if (selected) current.quietStartMinutes ?: 22 * 60 else null,
                        quietEndMinutes = if (selected) current.quietEndMinutes ?: 7 * 60 else null,
                    )
                }
            }
            if (enabled) {
                ClockSetting("Quiet hours start", requireNotNull(settings.quietStartMinutes)) { minutes -> viewModel.update { it.copy(quietStartMinutes = minutes) } }
                ClockSetting("Quiet hours end", requireNotNull(settings.quietEndMinutes)) { minutes -> viewModel.update { it.copy(quietEndMinutes = minutes) } }
            }
        }
        }

        if (section == SettingsSection.Data) {
        item { SettingsHeading("Backup and export") }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Portable backup folder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Choose a folder in Files, Drive, or removable storage. Automatic portable backups are plain JSON, not encrypted. Whip keeps access after restart, verifies every backup after writing it, and never deletes unrelated files.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (state.portableBackup.configured) {
                        Text("Folder: ${state.portableBackup.folderLabel ?: "Selected folder"}")
                        state.portableBackup.lastBackupAtMillis?.let { millis ->
                            val whenSaved = Instant.ofEpochMilli(millis).atZone(settings.zoneId()).toLocalDateTime()
                            Text(
                                "Last verified: $whenSaved${state.portableBackup.lastBackupFileName?.let { " · $it" }.orEmpty()}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        state.portableBackup.lastError?.let { error ->
                            Text("Last backup warning or error: $error", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = viewModel::createPortableBackup,
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Back up now") }
                        SettingsToggle(
                            "Automatic daily backup",
                            state.portableBackup.automaticEnabled,
                            onChange = viewModel::setPortableBackupAutomatic,
                        )
                        NumberSetting(
                            "Verified backups to keep (1–30)",
                            state.portableBackup.retentionCount,
                            viewModel::setPortableBackupRetention,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { backupFolder.launch(state.portableBackup.folderUri?.let(android.net.Uri::parse)) },
                                enabled = !state.busy,
                                modifier = Modifier.weight(1f),
                            ) { Text("Change folder") }
                            TextButton(onClick = viewModel::clearPortableBackupFolder, enabled = !state.busy, modifier = Modifier.weight(1f)) {
                                Text("Forget folder")
                            }
                        }
                    } else {
                        Button(
                            onClick = { backupFolder.launch(null) },
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Choose backup folder") }
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { pendingExport = ExportKind.Backup; createDocument.launch("whip-${LocalDate.now(settings.zoneId())}.whip.json") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save plain JSON backup") }
        }
        item {
            OutlinedButton(
                onClick = { showEncryptedExport = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save passphrase-encrypted backup") }
            Text(
                "Encrypted backups are authenticated and safer for health/location history. The passphrase is never saved and cannot be recovered. Plain JSON remains available for interoperability.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item { OutlinedButton(onClick = { openDocument.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Preview and restore backup") } }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(ExportKind.TasksCsv to "Tasks", ExportKind.HabitsCsv to "Habits", ExportKind.GoalsCsv to "Goals", ExportKind.GymCsv to "Gym").forEach { (kind, label) ->
                    TextButton(onClick = { pendingExport = kind; createDocument.launch("whip-${label.lowercase()}-${LocalDate.now(settings.zoneId())}.csv") }, modifier = Modifier.weight(1f)) { Text(label) }
                }
            }
        }
        item { OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete all local data") } }
        }

        if (section == SettingsSection.Health) {
        item { SettingsHeading("Health and privacy") }
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
        }
        if (state.healthConnect.availability == HealthConnectAvailability.Available) {
            item {
                SettingsToggle("Sync with Health Connect", settings.healthConnectEnabled) { enabled ->
                    viewModel.setHealthConnectEnabled(enabled)
                    if (enabled && settings.healthDataTypes.isNotEmpty()) {
                        healthPermissions.launch(viewModel.requiredHealthPermissions())
                    }
                }
                if (!settings.healthConnectEnabled) {
                    Text(
                        buildString {
                            append("Sync is paused. No health data is read. ")
                            if (settings.healthDataTypes.isEmpty()) append("No categories are selected.")
                            else append("Saved for next time: ${settings.healthDataTypes.joinToString { it.name }}.")
                        },
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
                    SettingsToggle(
                        if (!settings.healthConnectEnabled) {
                            "${type.name} · sync paused"
                        } else {
                            "${type.name} · ${if (granted) "Android access allowed" else "Android access not allowed"}"
                        },
                        selected && settings.healthConnectEnabled,
                        enabled = settings.healthConnectEnabled,
                    ) { enabled -> viewModel.setHealthDataType(type, enabled) }
                }
            }
            item {
                NumberSetting("Days to sync", settings.healthSyncDays) { value ->
                    viewModel.update { it.copy(healthSyncDays = value.coerceIn(1, 365)) }
                }
                val accessAvailability = ControlAvailability(
                    enabled = settings.healthDataTypes.isNotEmpty(),
                    unavailableExplanation = "Select at least one Health Connect category above.",
                )
                val syncAvailability = ControlAvailability(
                    enabled = settings.healthConnectEnabled && settings.healthDataTypes.isNotEmpty(),
                    unavailableExplanation = when {
                        !settings.healthConnectEnabled -> "Turn on Health Connect sync above."
                        settings.healthDataTypes.isEmpty() -> "Select at least one Health Connect category above."
                        else -> null
                    },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { healthPermissions.launch(viewModel.requiredHealthPermissions()) },
                        enabled = accessAvailability.enabled,
                        modifier = Modifier.weight(1f),
                    ) { Text("Review access") }
                    Button(
                        onClick = viewModel::syncHealthConnect,
                        enabled = syncAvailability.enabled && !state.busy,
                        modifier = Modifier.weight(1f),
                    ) { Text("Sync now") }
                }
                AvailabilityNotice("Review access", accessAvailability)
                AvailabilityNotice("Sync now", syncAvailability)
                state.healthConnect.lastSync?.let { Text("Last sync: $it · ${state.healthConnect.importedEntries} entries", style = MaterialTheme.typography.bodySmall) }
            }
        }
        }
        }
    }

    state.backupPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            title = { Text("Import this Whip backup?") },
            text = {
                Text(
                    "Exported ${preview.exportedAt}\n${preview.totalRecords} records in ${preview.tableCounts.count { it.value > 0 }} tables\n" +
                        "Preferences: ${if (preview.settingsIncluded) "included" else "not included (legacy backup)"}\n" +
                        "${preview.duplicateStableIds} stable IDs already exist.\n\n" +
                        (preview.compatibilityMessage ?: "MERGE adds records that are not already present, remaps their relationships, keeps current settings, and commits atomically. Re-importing the same file is safe.\n\nREPLACE snapshots the current database and preferences first, then replaces all local data, settings, and scheduled work; interruption rolls back to that snapshot."),
                )
            },
            confirmButton = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(enabled = preview.restoreCompatible, onClick = viewModel::confirmMerge) {
                        Text(if (preview.restoreCompatible) "Merge new data" else "Update required")
                    }
                    TextButton(enabled = preview.restoreCompatible, onClick = viewModel::confirmRestore) {
                        Text("Replace everything", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelRestore) { Text("Cancel") } },
        )
    }
    if (showEncryptedExport) {
        AlertDialog(
            onDismissRequest = {
                showEncryptedExport = false
                exportPassphrase = ""
                exportPassphraseConfirmation = ""
            },
            title = { Text("Encrypt this backup") },
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
                TextButton(
                    enabled = exportPassphrase.length >= 8 && exportPassphrase == exportPassphraseConfirmation,
                    onClick = {
                        pendingExport = ExportKind.EncryptedBackup
                        pendingExportPassphrase = exportPassphrase
                        exportPassphrase = ""
                        exportPassphraseConfirmation = ""
                        showEncryptedExport = false
                        createDocument.launch("whip-${LocalDate.now(settings.zoneId())}.whip.enc.json")
                    },
                ) { Text("Choose location") }
            },
            dismissButton = { TextButton(onClick = { showEncryptedExport = false; exportPassphrase = ""; exportPassphraseConfirmation = "" }) { Text("Cancel") } },
        )
    }
    if (state.encryptedRestorePending) {
        AlertDialog(
            onDismissRequest = { restorePassphrase = ""; viewModel.cancelEncryptedRestore() },
            title = { Text("Unlock encrypted backup") },
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
                TextButton(
                    enabled = restorePassphrase.isNotEmpty() && !state.busy,
                    onClick = {
                        viewModel.unlockEncryptedRestore(restorePassphrase)
                        restorePassphrase = ""
                    },
                ) { Text("Unlock and preview") }
            },
            dismissButton = { TextButton(onClick = { restorePassphrase = ""; viewModel.cancelEncryptedRestore() }) { Text("Cancel") } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete all local data?") },
            text = { Text("This cannot be undone. Create a full backup first if you may need this data again.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteAllData(); confirmDelete = false }) { Text("Delete everything") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
    if (createUnit) {
        CustomUnitDialog(
            mode = CustomUnitEditMode.Create,
            onDismiss = { createUnit = false },
            onSave = { name, symbol, dimension, factor ->
                viewModel.createCustomUnit(name, symbol, dimension, factor)
                createUnit = false
            },
        )
    }
    renameUnitId?.let { id ->
        state.customUnits.firstOrNull { it.id == id }?.let { unit ->
            CustomUnitDialog(
                mode = CustomUnitEditMode.Rename,
                initial = unit,
                onDismiss = { renameUnitId = null },
                onSave = { name, symbol, _, _ ->
                    viewModel.renameCustomUnit(unit.id, name, symbol)
                    renameUnitId = null
                },
            )
        }
    }
    versionUnitId?.let { id ->
        state.customUnits.firstOrNull { it.id == id }?.let { unit ->
            CustomUnitDialog(
                mode = CustomUnitEditMode.Version,
                initial = unit,
                onDismiss = { versionUnitId = null },
                onSave = { name, symbol, _, factor ->
                    viewModel.createCustomUnitVersion(unit.id, name, symbol, factor)
                    versionUnitId = null
                },
            )
        }
    }
    taxonomyEditKind?.let { kind ->
        val id = taxonomyEditId
        val initialName = if (kind == "Area") state.areas.firstOrNull { it.id == id }?.name
        else state.tags.firstOrNull { it.id == id }?.name
        if (id != null && initialName != null) {
            TaxonomyRenameDialog(
                kind = kind,
                initialName = initialName,
                onDismiss = { taxonomyEditKind = null; taxonomyEditId = null },
                onSave = { name ->
                    if (kind == "Area") viewModel.renameArea(id, name) else viewModel.renameTag(id, name)
                    taxonomyEditKind = null
                    taxonomyEditId = null
                },
            )
        }
    }
}

@Composable
private fun TaxonomyRenameDialog(
    kind: String,
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename or merge $kind") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("$kind name") }, singleLine = true)
                Text("If this name already exists, Whip merges every matching record into it.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim()) }) { Text("Apply everywhere") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable private fun SettingsHeading(text: String) { HorizontalDivider(); Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }

@Composable private fun SettingsToggle(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClickLabel = "Toggle $label", role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable private fun <T> SettingsDropdown(label: String, values: List<T>, selected: T, text: (T) -> String, onChange: (T) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column { Text(label, style = MaterialTheme.typography.labelMedium); OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(text(selected)) }; DropdownMenu(expanded, { expanded = false }) { values.forEach { value -> DropdownMenuItem({ Text(text(value)) }, { onChange(value); expanded = false }) } } }
}

@Composable private fun NumberSetting(label: String, current: Int, onChange: (Int) -> Unit) {
    var text by rememberSaveable(current) { mutableStateOf(current.toString()) }
    OutlinedTextField(text, { value -> text = value; value.toIntOrNull()?.let(onChange) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}

@Composable private fun ClockSetting(label: String, currentMinutes: Int, onChange: (Int) -> Unit) {
    var text by rememberSaveable(currentMinutes) { mutableStateOf("%02d:%02d".format(currentMinutes / 60, currentMinutes % 60)) }
    OutlinedTextField(text, { value -> text = value; parseSettingsClock(value)?.let(onChange) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}

@Composable private fun UnitSetting(label: String, values: List<String>, current: String, onChange: (String) -> Unit) {
    SettingsDropdown(label, values, current, { id ->
        BuiltInUnits.get(id)?.let { unit -> "${unit.name} (${unit.symbol})" } ?: id
    }, onChange)
}

private fun parseSettingsClock(value: String): Int? {
    val parts = value.split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
    return if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
}

private enum class CustomUnitEditMode { Create, Rename, Version }

@Composable private fun CustomUnitDialog(
    mode: CustomUnitEditMode,
    initial: com.whip.app.domain.UnitDefinition? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, UnitDimension, Double) -> Unit,
) {
    var name by rememberSaveable(initial?.id, mode) { mutableStateOf(initial?.name.orEmpty()) }
    var symbol by rememberSaveable(initial?.id, mode) { mutableStateOf(initial?.symbol.orEmpty()) }
    var dimension by rememberSaveable(initial?.id, mode) { mutableStateOf(initial?.dimension ?: UnitDimension.Count) }
    var factor by rememberSaveable(initial?.id, mode) { mutableStateOf(initial?.toCanonicalFactor?.toString() ?: "1") }
    val canonicalLabel = canonicalUnitLabel(dimension)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (mode) {
                    CustomUnitEditMode.Create -> "Create custom unit"
                    CustomUnitEditMode.Rename -> "Rename custom unit"
                    CustomUnitEditMode.Version -> "Create conversion version"
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name, e.g. glass") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(symbol, { symbol = it }, label = { Text("Symbol, e.g. gl") }, modifier = Modifier.fillMaxWidth())
                if (mode == CustomUnitEditMode.Create) {
                    SettingsDropdown("Dimension", UnitDimension.entries, dimension, UnitDimension::name) { dimension = it }
                } else Text("Dimension: ${dimension.name}", style = MaterialTheme.typography.bodySmall)
                if (mode != CustomUnitEditMode.Rename) {
                    Text(
                        "Whip stores ${dimension.name.lowercase()} values in $canonicalLabel so compatible units can be compared and linked.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        factor,
                        { factor = it },
                        label = { Text("1 ${symbol.ifBlank { name.ifBlank { "custom unit" } }} equals how many $canonicalLabel?") },
                        supportingText = { Text(customUnitExample(dimension)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (mode == CustomUnitEditMode.Version) Text(
                        "The existing conversion is archived for old records. New entries use this version.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else Text(
                    "Renaming changes only the label; recorded values and conversion meaning stay unchanged.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && (mode == CustomUnitEditMode.Rename || (factor.toWhipDoubleOrNull() ?: 0.0) > 0.0),
                onClick = { onSave(name, symbol, dimension, factor.toWhipDoubleOrNull() ?: requireNotNull(initial).toCanonicalFactor) },
            ) { Text(if (mode == CustomUnitEditMode.Rename) "Save name" else "Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
    else -> "Enter the amount represented by one of your custom units."
}

@Composable private fun TimeZoneSetting(current: String, onChange: (String) -> Unit) {
    var text by rememberSaveable(current) { mutableStateOf(current) }
    val valid = runCatching { ZoneId.of(text.trim()) }.isSuccess
    OutlinedTextField(
        value = text,
        onValueChange = { value ->
            text = value
            if (runCatching { ZoneId.of(value.trim()) }.isSuccess) onChange(value.trim())
        },
        label = { Text("IANA time zone") },
        supportingText = { Text(if (valid) "Examples: America/Toronto, Europe/London" else "Enter a valid region time zone") },
        isError = !valid,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
