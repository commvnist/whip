package com.whip.app.ui

import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.whip.app.core.HomeSection

private enum class BackupChoice(val label: String) {
    Encrypted("Create encrypted backup now"),
    Plaintext("Create plain JSON backup now"),
    Later("Decide later"),
}

@Composable
fun FirstRunSetupDialog(
    onComplete: (Set<HomeSection>, Boolean, Boolean, Boolean, String, Boolean) -> Unit,
    onSkip: () -> Unit,
) {
    var selectedAreas by rememberSaveable { mutableStateOf(setOf(HomeSection.Tasks, HomeSection.Habits)) }
    var powerMode by rememberSaveable { mutableStateOf(false) }
    var usePounds by rememberSaveable { mutableStateOf(false) }
    var lowPressureMode by rememberSaveable { mutableStateOf(false) }
    var notifications by rememberSaveable { mutableStateOf(false) }
    var backupChoice by rememberSaveable { mutableStateOf(BackupChoice.Later) }
    var showOptionalPreferences by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Set Up Whip") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Choose what belongs at Home. Tasks, Habits, Goals, and Gym remain available from main navigation; this only changes the Home overview.")
                Text("Home Sections", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeSection.entries.forEach { area ->
                        WhipFilterChip(
                            selected = area in selectedAreas,
                            onClick = {
                                selectedAreas = if (area in selectedAreas) selectedAreas - area else selectedAreas + area
                            },
                            label = { Text(area.name) },
                        )
                    }
                }
                Text("Experience", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WhipFilterChip(!powerMode, { powerMode = false }, { Text("Simple Start") })
                    WhipFilterChip(powerMode, { powerMode = true }, { Text("Power Mode") })
                }
                Text(
                    if (powerMode) "Show advanced choices by default where useful." else "Keep advanced choices folded until requested.",
                    style = MaterialTheme.typography.bodySmall,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WhipFilterChip(!usePounds, { usePounds = false }, { Text("kg") })
                    WhipFilterChip(usePounds, { usePounds = true }, { Text("lb") })
                }
                WhipTextButton(
                    onClick = { showOptionalPreferences = !showOptionalPreferences },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (showOptionalPreferences) "Hide Optional Preferences" else "More Preferences")
                }
                if (showOptionalPreferences) {
                    SetupToggle("Low-pressure presentation (less streak emphasis)", lowPressureMode) { lowPressureMode = it }
                    SetupToggle("I want reminder notifications", notifications) { notifications = it }
                    Text("Portable Backup Privacy", style = MaterialTheme.typography.titleSmall)
                    BackupChoice.entries.forEach { choice ->
                        WhipFilterChip(backupChoice == choice, { backupChoice = choice }, { Text(choice.label.uiTitleCase()) })
                    }
                    Text(
                        "Encrypted backups use a passphrase Whip cannot recover. Plain JSON is readable by other tools and is not encrypted.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(
                        "Reminder, low-pressure, and backup choices can be configured later in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = selectedAreas.isNotEmpty(),
                onClick = { onComplete(selectedAreas, powerMode, usePounds, lowPressureMode, backupChoice.name, notifications) },
            ) { Text("Finish Setup") }
        },
        dismissButton = { WhipTextButton(onClick = onSkip) { Text("Skip · Simple Mode") } },
    )
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
