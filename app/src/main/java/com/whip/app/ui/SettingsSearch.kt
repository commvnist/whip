package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

internal data class SettingsSearchEntry(val anchor: String, val section: SettingsSection, val title: String, val purpose: String, val aliases: String)

internal val SettingsSearchEntries = listOf(
    SettingsSearchEntry("setting-theme", SettingsSection.Appearance, "Theme", "Light, dark, or system appearance", "colors display"),
    SettingsSearchEntry("setting-dynamic-color", SettingsSection.Appearance, "Use Android dynamic colors", "Use the wallpaper palette", "theme colour"),
    SettingsSearchEntry("setting-advanced", SettingsSection.Appearance, "Show advanced controls by default", "Open optional configuration groups", "power mode disclosure"),
    SettingsSearchEntry("setting-low-pressure", SettingsSection.Appearance, "Low-pressure Habit presentation", "Reduce streak and success/failure emphasis", "gentle streaks"),
    SettingsSearchEntry("setting-celebrate", SettingsSection.Appearance, "Celebrate completed Goals", "Victory Shower when a Goal completes", "confetti celebration"),
    SettingsSearchEntry("setting-opening-area", SettingsSection.Appearance, "When Whip opens", "Return to the last Area or a chosen Area", "start launch default opening"),
    SettingsSearchEntry("setting-home", SettingsSection.Appearance, "Home Overview", "Show, hide, expand, and reorder Home sections", "home order layout shortcuts"),
    SettingsSearchEntry("setting-keyboard", SettingsSection.Appearance, "Hardware Keyboard", "Keyboard shortcuts", "hotkeys ctrl"),
    SettingsSearchEntry("setting-week", SettingsSection.Planning, "First day of week", "Calendar order and weekly analytics", "monday sunday review"),
    SettingsSearchEntry("setting-timezone", SettingsSection.Planning, "Follow device time zone", "Device or fixed time zone", "timezone travel clock"),
    SettingsSearchEntry("setting-cutoff", SettingsSection.Planning, "Late-night day cutoff", "When Today changes to the next date", "midnight rollover late day"),
    SettingsSearchEntry("setting-precision", SettingsSection.Planning, "Default decimal precision", "Number display digits", "rounding decimals"),
    SettingsSearchEntry("setting-mass", SettingsSection.Planning, "Mass", "Default measurement unit for general entries", "weight kg lb pounds kilograms grams"),
    SettingsSearchEntry("setting-distance", SettingsSection.Planning, "Distance", "Default distance measurement unit", "miles kilometres km meters"),
    SettingsSearchEntry("setting-volume", SettingsSection.Planning, "Volume", "Default volume measurement unit", "litres cups fluid ounces ml"),
    SettingsSearchEntry("setting-custom-units", SettingsSection.Planning, "Custom Units", "Create and manage reusable measurement units", "glass steps pages archive rename"),
    SettingsSearchEntry("setting-review", SettingsSection.Planning, "Default review period", "Initial date window for Review", "week month year"),
    SettingsSearchEntry("setting-repeats", SettingsSection.Planning, "Show every repeating occurrence in Upcoming", "Next occurrence or all occurrences in 30 days", "tasks repeat"),
    SettingsSearchEntry("setting-habits-planning", SettingsSection.Planning, "Show habits in Task Agenda and Calendar", "Include scheduled habits in planning views", "tasks schedule"),
    SettingsSearchEntry("setting-subtasks", SettingsSection.Planning, "Repeating-task subtask default", "How steps carry into a repeating occurrence", "checklist reset copy"),
    SettingsSearchEntry("setting-capture", SettingsSection.Planning, "Smart Task Capture", "Recognize scheduling and task details while typing", "natural language parser examples"),
    SettingsSearchEntry("setting-habit-week", SettingsSection.Planning, "Default week start for new Habits", "Initial Habit week start; existing schedules keep theirs", "monday sunday"),
    SettingsSearchEntry("setting-gym-mass", SettingsSection.Gym, "Gym summaries and new exercises", "Gym display unit and new-exercise default; existing equipment keeps its unit", "weight kg lb pounds kilograms"),
    SettingsSearchEntry("setting-rest", SettingsSection.Gym, "Default rest time (seconds)", "Rest duration when no workout override is set", "timer pause seconds"),
    SettingsSearchEntry("setting-rest-presets", SettingsSection.Gym, "Rest Presets", "App-wide rest timer shortcuts", "timer durations quick"),
    SettingsSearchEntry("setting-sound", SettingsSection.Gym, "Rest timer sound", "Play background rest alerts", "audio notification bell"),
    SettingsSearchEntry("setting-vibration", SettingsSection.Gym, "Rest timer vibration", "Vibrate for rest alerts", "haptic buzz"),
    SettingsSearchEntry("setting-awake", SettingsSection.Gym, "Keep workout screen awake by default", "Keep the display on while training", "sleep lock"),
    SettingsSearchEntry("setting-auto-rest", SettingsSection.Gym, "Start rest timer when a set completes", "Automatically start rest after logging", "automatic timer"),
    SettingsSearchEntry("setting-effort", SettingsSection.Gym, "Default workout effort field", "Choose RPE, RIR, or no effort input", "rating perceived exertion reps reserve"),
    SettingsSearchEntry("setting-tempo", SettingsSection.Gym, "Show tempo fields", "Show tempo in workout entry", "cadence"),
    SettingsSearchEntry("setting-formula", SettingsSection.Gym, "Estimated 1RM formula", "Choose Epley or Brzycki", "one rep max calculation"),
    SettingsSearchEntry("setting-rep-cutoff", SettingsSection.Gym, "Estimated 1RM rep cutoff", "Exclude high-repetition sets from estimated records", "repetition maximum limit"),
    SettingsSearchEntry("setting-warmups", SettingsSection.Gym, "Include warm-ups in volume and PRs", "Choose whether warm-up sets count", "stats statistics records"),
    SettingsSearchEntry("setting-hard-sets", SettingsSection.Gym, "Hard-Set Classifications", "Which set types contribute to category totals", "working warmup drop volume"),
    SettingsSearchEntry("setting-allocation", SettingsSection.Gym, "Overlapping category allocation", "Count shared-category sets fully, fractionally, or once", "muscles volume split primary"),
    SettingsSearchEntry("setting-effort-adjustment", SettingsSection.Gym, "Adjust estimated 1RM using RPE/RIR", "Use effort in estimated strength records", "one rep max"),
    SettingsSearchEntry("setting-assisted", SettingsSection.Gym, "Allow assisted exercises in personal records", "Include assisted movements in records", "pr assistance"),
    SettingsSearchEntry("setting-areas", SettingsSection.Organization, "Manage Areas", "Group Tasks, Habits, Goals, and Tracks", "create archive rename merge move color"),
    SettingsSearchEntry("setting-tags", SettingsSection.Organization, "Manage Tags", "Labels across productivity items", "create archive rename merge"),
    SettingsSearchEntry("setting-emojis", SettingsSection.Organization, "Custom Emojis", "Name and manage reusable emoji choices", "icons identity rename"),
    SettingsSearchEntry("setting-notifications", SettingsSection.Reminders, "Notifications", "Android delivery access and channel status", "permission alerts channels battery exact alarm troubleshooting"),
    SettingsSearchEntry("setting-test-notification", SettingsSection.Reminders, "Send test notification", "Check notification delivery on this device", "alerts test"),
    SettingsSearchEntry("setting-refresh-notifications", SettingsSection.Reminders, "Refresh Notification Status", "Refresh Task, Habit, and Goal reminder schedules", "repair schedules reminders"),
    SettingsSearchEntry("setting-quiet", SettingsSection.Reminders, "Quiet Hours", "Pause reminders during chosen hours", "do not disturb silence night"),
    SettingsSearchEntry("setting-backup-status", SettingsSection.DataPrivacy, "Backup Protection", "Latest verified copy and backup errors", "recovery safety back up now"),
    SettingsSearchEntry("setting-backup", SettingsSection.DataPrivacy, "Save or Restore a Backup", "Plain JSON, passphrase-encrypted export, and restore preview", "backup file import merge replace encrypted password"),
    SettingsSearchEntry("setting-backup-folder", SettingsSection.DataPrivacy, "Portable Backup Folder", "Automatic verified copies, retention, and folder access", "daily drive automatic keep reconnect"),
    SettingsSearchEntry("setting-csv", SettingsSection.DataPrivacy, "Export CSV", "Export Tasks, Habits, Goals, Gym, or Tracks to spreadsheets", "table excel download"),
    SettingsSearchEntry("setting-reset", SettingsSection.DataPrivacy, "Reset Whip", "Review destructive removal of local data", "delete everything erase"),
    SettingsSearchEntry("setting-about", SettingsSection.AboutDiagnostics, "About Whip", "Version, package, and data-handling summary", "app build privacy identity"),
 )

internal fun searchSettings(query: String): List<SettingsSearchEntry> {
    val terms = query.trim().lowercase().split(Regex("\\s+")).filter(String::isNotBlank)
    return SettingsSearchEntries.filter { entry ->
        val text = "${entry.title} ${entry.section.label} ${entry.purpose} ${entry.aliases}".lowercase()
        terms.all(text::contains)
    }
}

/** Registers each actual lazy item position, including conditional and repeated rows. */
internal class SettingsAnchorItems(private val scope: LazyListScope, private val positions: MutableMap<String, Int>) {
    private var nextIndex = 0
    init { positions.clear() }
    fun item(key: Any? = null, content: @Composable LazyItemScope.() -> Unit) {
        if (key is String) positions[key] = nextIndex
        nextIndex++
        scope.item(key = key) {
            if (key is String && key.startsWith("setting-")) {
                val itemScope = this
                Column(Modifier.fillMaxWidth().testTag(key)) { content(itemScope) }
            } else content()
        }
    }
    fun <T> items(values: List<T>, key: (T) -> Any, content: @Composable LazyItemScope.(T) -> Unit) {
        values.forEach { value -> item(key(value)) { content(value) } }
    }
}

@Composable
internal fun SettingsSearchDialog(query: String, onQueryChange: (String) -> Unit, onDismiss: () -> Unit, onSelect: (SettingsSearchEntry) -> Unit) {
    val matches = remember(query) { searchSettings(query) }
    val keyboard = LocalSoftwareKeyboardController.current
    PaneAwareAlertDialog(
        modifier = Modifier.testTag("settings-search-dialog"),
        onDismissRequest = onDismiss,
        title = { Text("Search Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, onQueryChange, label = { Text("Setting or purpose") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings-search-query"))
                if (matches.isEmpty()) Text("No matching settings. Try a purpose such as pounds, rest sound, or backup.")
                else Text("${matches.size} settings", style = MaterialTheme.typography.bodySmall)
                LazyColumn(Modifier.weight(1f, fill = false).testTag("settings-search-results")) {
                    items(matches, key = SettingsSearchEntry::anchor) { entry ->
                        WhipActionRow(title = entry.title, supportingText = "${entry.section.label} · ${entry.purpose}",
                            onClick = { keyboard?.hide(); onSelect(entry) }, modifier = Modifier.testTag("settings-search-${entry.anchor}"))
                    }
                }
            }
        },
        confirmButton = { WhipTextButton(onClick = onDismiss) { Text("Close") } },
    )
}
