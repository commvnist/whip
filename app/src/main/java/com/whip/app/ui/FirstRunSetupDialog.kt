package com.whip.app.ui

import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.whip.app.core.HomeSection

internal val DEFAULT_FIRST_RUN_HOME_SECTIONS: Set<HomeSection> =
    setOf(HomeSection.Tasks, HomeSection.Habits)

internal data class FirstRunSetupDraft(
    val selectedSections: Set<HomeSection> = DEFAULT_FIRST_RUN_HOME_SECTIONS,
    val powerMode: Boolean = false,
    val usePounds: Boolean = false,
    val lowPressureMode: Boolean = false,
)

@Composable
internal fun FirstRunSetupHost(
    setupCompleted: Boolean,
    mutation: TypedSettingMutation<FirstRunSetupDraft>,
    onRequestNotificationPermission: () -> Unit,
) {
    var ownsSetup by rememberSaveable { mutableStateOf(false) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var requestNotifications by rememberSaveable { mutableStateOf(false) }
    // Preferences may publish their in-memory value before commit succeeds.
    // Once submitted, only this request's receipt can dismiss its retained draft.
    if (finished || (setupCompleted && !ownsSetup)) return

    val coordinator = rememberPersistenceRequestCoordinator(
        state = mutation.state,
        consume = mutation.consume,
        requestNamespace = "first-run",
        orphanedMessage = "Setup was interrupted. Your choices are still here. Save again to finish.",
        onPersisted = { receipt ->
            finished = true
            ownsSetup = false
            mutation.onCompletedWarnings(receipt.warnings)
            if (requestNotifications) {
                requestNotifications = false
                onRequestNotificationPermission()
            }
        },
    )
    fun submit(draft: FirstRunSetupDraft, notifications: Boolean) {
        val requestId = coordinator.begin() ?: return
        ownsSetup = true
        requestNotifications = notifications
        if (!mutation.submit(requestId, draft)) {
            coordinator.finishFailure("Whip is busy saving another setting. Your choices are still here; try again.")
        }
    }
    FirstRunSetupDialog(
        onComplete = { sections, power, pounds, lowPressure, notifications ->
            submit(FirstRunSetupDraft(sections, power, pounds, lowPressure), notifications)
        },
        onUseDefaults = { submit(FirstRunSetupDraft(), false) },
        saving = coordinator.saving,
        errorMessage = coordinator.errorMessage,
    )
}

@Composable
fun FirstRunSetupDialog(
    onComplete: (Set<HomeSection>, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onUseDefaults: () -> Unit,
    saving: Boolean = false,
    errorMessage: String? = null,
) {
    var selectedSections by rememberSaveable { mutableStateOf(DEFAULT_FIRST_RUN_HOME_SECTIONS) }
    var powerMode by rememberSaveable { mutableStateOf(false) }
    var usePounds by rememberSaveable { mutableStateOf(false) }
    var lowPressureMode by rememberSaveable { mutableStateOf(false) }
    var notifications by rememberSaveable { mutableStateOf(false) }
    var showOptionalPreferences by rememberSaveable { mutableStateOf(false) }
    var customizing by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) scroll.scrollTo(0)
    }
    PaneAwareAlertDialog(
        onDismissRequest = {},
        paneTitle = if (customizing) "Customize Whip" else "Welcome to Whip",
        inputBlocked = saving,
        inputBlockedLabel = "Saving setup",
        title = { Text(if (customizing) "Customize Whip" else "Welcome to Whip") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                errorMessage?.let { message ->
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                if (!customizing) {
                    Text(
                        "Turn plans into action without giving up ownership of your data.",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            SetupValue(
                                Icons.Outlined.CheckCircle,
                                "Plan What Matters",
                                "Capture Tasks and keep today's next actions clear.",
                            )
                            SetupValue(
                                Icons.Outlined.Autorenew,
                                "Build Momentum",
                                "Practice Habits, pursue Goals, record evidence, and train.",
                            )
                            SetupValue(
                                Icons.Outlined.Lock,
                                "Private by Default",
                                "Your data stays on this device. No account is required.",
                            )
                        }
                    }
                    Text(
                        "The recommended setup puts Tasks and Habits on Home and keeps advanced controls folded. Every feature remains one tap away.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text("Choose what appears on Home. Every feature remains available from main navigation.")
                    Text("Home Overview", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HomeSection.entries.forEach { section ->
                            WhipFilterChip(
                                selected = section in selectedSections,
                                onClick = {
                                    selectedSections = if (section in selectedSections) {
                                        selectedSections - section
                                    } else {
                                        selectedSections + section
                                    }
                                },
                                label = { Text(section.label) },
                            )
                        }
                    }
                    if (selectedSections.isEmpty()) {
                        Text("Choose at least one Home section.", color = MaterialTheme.colorScheme.error)
                    }
                    SetupToggle("Show advanced controls by default", powerMode) { powerMode = it }
                    Text(
                        if (powerMode) "Advanced choices open automatically where useful." else "Advanced choices stay folded until requested.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text("Weight Units", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WhipFilterChip(!usePounds, { usePounds = false }, { Text("kg") })
                        WhipFilterChip(usePounds, { usePounds = true }, { Text("lb") })
                    }
                    WhipTextButton(
                        onClick = { showOptionalPreferences = !showOptionalPreferences },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (showOptionalPreferences) "Hide Optional Preferences" else "Optional Preferences")
                    }
                    if (showOptionalPreferences) {
                        SetupToggle("Use low-pressure presentation", lowPressureMode) { lowPressureMode = it }
                        Text("Reduces streak emphasis without changing your data.", style = MaterialTheme.typography.bodySmall)
                        SetupToggle("Ask for reminder notifications", notifications) { notifications = it }
                    } else {
                        Text(
                            "You can change reminders in Settings → Reminders and presentation in Settings → Appearance & Home.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        "Whip stores your data locally. Configure backups anytime in Settings → Data & Privacy.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            WhipButton(
                enabled = !saving && (!customizing || selectedSections.isNotEmpty()),
                onClick = {
                    if (customizing) {
                        onComplete(selectedSections, powerMode, usePounds, lowPressureMode, notifications)
                    } else {
                        onUseDefaults()
                    }
                },
            ) { Text(if (customizing) "Save and Start" else "Use Recommended") }
        },
        dismissButton = {
            WhipTextButton(enabled = !saving, onClick = { customizing = !customizing }) {
                Text(if (customizing) "Back" else "Customize")
            }
        },
    )
}

@Composable
private fun SetupValue(icon: ImageVector, title: String, supportingText: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(supportingText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SetupToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .semantics(mergeDescendants = true) { contentDescription = label }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onCheckedChange = null)
    }
}
