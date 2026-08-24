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

internal val DEFAULT_FIRST_RUN_HOME_SECTIONS: Set<HomeSection> =
    setOf(HomeSection.Tasks, HomeSection.Habits)

@Composable
fun FirstRunSetupDialog(
    onComplete: (Set<HomeSection>, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onUseDefaults: () -> Unit,
) {
    var selectedSections by rememberSaveable { mutableStateOf(DEFAULT_FIRST_RUN_HOME_SECTIONS) }
    var powerMode by rememberSaveable { mutableStateOf(false) }
    var usePounds by rememberSaveable { mutableStateOf(false) }
    var lowPressureMode by rememberSaveable { mutableStateOf(false) }
    var notifications by rememberSaveable { mutableStateOf(false) }
    var showOptionalPreferences by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Set Up Whip") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Choose what belongs at Home. Tasks, Habits, Goals, Tracks, and Gym always remain available from main navigation; this only changes the Home overview.")
                Text("Home Sections", style = MaterialTheme.typography.titleSmall)
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
                            label = { Text(section.name) },
                        )
                    }
                }
                Text("Experience", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WhipFilterChip(!powerMode, { powerMode = false }, { Text("Keep Advanced Controls Folded") })
                    WhipFilterChip(powerMode, { powerMode = true }, { Text("Show Advanced Controls by Default") })
                }
                Text(
                    if (powerMode) "Show advanced choices by default where useful." else "Keep advanced choices folded until requested.",
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
                    Text(if (showOptionalPreferences) "Hide Optional Preferences" else "More Preferences")
                }
                if (showOptionalPreferences) {
                    SetupToggle("Low-pressure presentation (less streak emphasis)", lowPressureMode) { lowPressureMode = it }
                    SetupToggle("I want reminder notifications", notifications) { notifications = it }
                } else {
                    Text(
                        "Configure reminders later in Settings → Reminders and low-pressure presentation in Settings → Appearance & Home.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    "Whip stores your data locally and does not require an account. Configure backups anytime in Settings → Data & Privacy.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            WhipButton(
                enabled = selectedSections.isNotEmpty(),
                onClick = { onComplete(selectedSections, powerMode, usePounds, lowPressureMode, notifications) },
            ) { Text("Start Using Whip") }
        },
        dismissButton = { WhipTextButton(onClick = onUseDefaults) { Text("Use Defaults") } },
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
