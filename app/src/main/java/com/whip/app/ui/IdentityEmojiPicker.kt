package com.whip.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whip.app.domain.IDENTITY_EMOJI_PRESETS
import com.whip.app.domain.CustomIdentityEmoji
import com.whip.app.domain.IdentityEmojiPreset
import com.whip.app.domain.isDefaultIdentityEmoji
import com.whip.app.domain.isIdentityEmoji
import com.whip.app.domain.normalizeCustomIdentityEmojis
import com.whip.app.domain.normalizedIdentityEmoji
import java.util.Locale

@Composable
internal fun WhipIdentityEmoji(
    emoji: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .then(
                if (contentDescription == null) Modifier.clearAndSetSemantics { }
                else Modifier.semantics { this.contentDescription = contentDescription },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** Shared identity picker for Habits, Goals, and Tracks. */
@Composable
internal fun WhipEmojiPicker(
    value: String,
    defaultEmoji: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Emoji",
    customEmojis: List<CustomIdentityEmoji> = emptyList(),
    onSaveEmoji: (CustomIdentityEmoji) -> Unit = {},
    onRemoveSavedEmoji: (String) -> Unit = {},
) {
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var customOpen by rememberSaveable { mutableStateOf(false) }
    var customValue by rememberSaveable { mutableStateOf("") }
    var customName by rememberSaveable { mutableStateOf("") }
    var customAttempted by rememberSaveable { mutableStateOf(false) }
    var managingSaved by rememberSaveable { mutableStateOf(false) }
    val normalizedCustom = normalizeCustomIdentityEmojis(customEmojis)
    val selectedEmoji = value.normalizedIdentityEmoji(defaultEmoji)
    val selectedLabel = IDENTITY_EMOJI_PRESETS.firstOrNull { it.emoji == selectedEmoji }?.label
        ?: normalizedCustom.firstOrNull { it.emoji == selectedEmoji }?.name
        ?: "Custom Emoji"

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        WhipOutlinedButton(
            onClick = { pickerOpen = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("emoji-picker-trigger")
                .semantics { contentDescription = "$label: $selectedLabel. Change" },
        ) {
            WhipIdentityEmoji(selectedEmoji)
            Spacer(Modifier.width(8.dp))
            Text(selectedLabel, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
        }
    }

    if (pickerOpen) {
        val query = searchQuery.trim().lowercase(Locale.ROOT)
        val filteredPresets = IDENTITY_EMOJI_PRESETS.filter { preset -> preset.matches(query) }
        val filteredCustom = normalizedCustom.filter { choice ->
            query.isBlank() || query in choice.emoji.lowercase(Locale.ROOT) ||
                query in choice.name.lowercase(Locale.ROOT)
        }
        val customEmojiValid = customValue.isIdentityEmoji() && !customValue.isDefaultIdentityEmoji()
        val duplicateCustomName = normalizedCustom.any { choice ->
            choice.emoji != customValue.trim() && choice.name.equals(customName.trim(), ignoreCase = true)
        }
        val customNameValid = customName.isNotBlank() && !duplicateCustomName
        fun closePicker() {
            pickerOpen = false
            customOpen = false
            customAttempted = false
            managingSaved = false
            searchQuery = ""
        }
        fun useCustom(save: Boolean) {
            customAttempted = true
            if (customEmojiValid && (!save || customNameValid)) {
                val emoji = customValue.trim()
                if (save) onSaveEmoji(CustomIdentityEmoji(emoji = emoji, name = customName.trim()))
                onValueChange(emoji)
                closePicker()
            }
        }

        PaneAwareAlertDialog(
            modifier = Modifier.testTag("emoji-picker-dialog"),
            onDismissRequest = ::closePicker,
            title = { Text("Choose Emoji") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState())
                        .testTag("emoji-picker-presets"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it.take(80) },
                        modifier = Modifier.fillMaxWidth().testTag("emoji-picker-search"),
                        label = { Text("Search Activities") },
                        placeholder = { Text("Try reading, dentist, bills…") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                    )
                    WhipOutlinedButton(
                        onClick = {
                            customOpen = !customOpen
                            val currentCustom = normalizedCustom.firstOrNull { it.emoji == selectedEmoji }
                            customValue = currentCustom?.emoji.orEmpty()
                            customName = currentCustom?.name.orEmpty()
                            customAttempted = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("emoji-picker-custom-option"),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (customOpen) "Close Custom Emoji" else "Add Custom Emoji")
                    }
                    if (customOpen) {
                        OutlinedTextField(
                            value = customValue,
                            onValueChange = {
                                customValue = it.trim().take(32)
                                customAttempted = false
                            },
                            label = { Text("Custom Emoji") },
                            supportingText = {
                                Text(
                                    when {
                                        customAttempted && customValue.isDefaultIdentityEmoji() -> "That emoji is already in the built-in library."
                                        customAttempted && !customEmojiValid -> "Enter one emoji, not text or multiple separate emojis."
                                        else -> "Custom choices stay separate from Whip's read-only defaults."
                                    },
                                )
                            },
                            isError = customAttempted && !customEmojiValid,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("emoji-picker-custom-input"),
                        )
                        OutlinedTextField(
                            value = customName,
                            onValueChange = {
                                customName = it
                                customAttempted = false
                            },
                            label = { Text("Name") },
                            supportingText = {
                                Text(
                                    when {
                                        customAttempted && duplicateCustomName -> "That custom emoji name is already in use."
                                        customAttempted && customName.isBlank() -> "Give this emoji a name for your organization."
                                        else -> "This name appears in every Habit, Goal, and Track picker."
                                    },
                                )
                            },
                            isError = customAttempted && !customNameValid,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("emoji-picker-custom-name"),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        ) {
                            WhipTextButton(
                                onClick = { useCustom(save = false) },
                                modifier = Modifier.testTag("emoji-picker-custom-use-once"),
                            ) { Text("Use Once") }
                            WhipButton(
                                onClick = { useCustom(save = true) },
                                modifier = Modifier.testTag("emoji-picker-custom-apply"),
                            ) { Text("Save & Use", fontWeight = FontWeight.SemiBold) }
                        }
                    }

                    EmojiSectionHeading(
                        title = "Common Emojis",
                        supportingText = "100 common planning activities, ordered from broadest use to more specific use.",
                    )
                    if (filteredPresets.isEmpty() && filteredCustom.isEmpty()) {
                        Text(
                            "No emoji matches “${searchQuery.trim()}”. Add any emoji above instead.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (filteredPresets.isNotEmpty()) {
                        EmojiGrid(
                            presets = filteredPresets,
                            selectedEmoji = selectedEmoji,
                            onSelect = { emoji -> onValueChange(emoji); closePicker() },
                        )
                    }

                    if (normalizedCustom.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            EmojiSectionHeading(
                                title = "My Emojis",
                                supportingText = if (managingSaved) {
                                    "Remove a choice here, or rename and replace choices in Settings → Organization."
                                } else {
                                    "Named reusable choices you added. Manage them in Settings → Organization."
                                },
                                modifier = Modifier.weight(1f),
                            )
                            WhipTextButton(
                                onClick = { managingSaved = !managingSaved },
                                modifier = Modifier.testTag("emoji-picker-manage-saved"),
                            ) { Text(if (managingSaved) "Done" else "Manage") }
                        }
                        if (filteredCustom.isNotEmpty()) {
                            SavedEmojiList(
                                choices = filteredCustom,
                                selectedEmoji = selectedEmoji,
                                managing = managingSaved,
                                onSelect = { emoji -> onValueChange(emoji); closePicker() },
                                onRemove = onRemoveSavedEmoji,
                            )
                        } else {
                            Text("No saved emoji matches this search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = { WhipTextButton(onClick = ::closePicker) { Text("Close") } },
        )
    }
}

private fun IdentityEmojiPreset.matches(query: String): Boolean = query.isBlank() ||
    query in label.lowercase(Locale.ROOT) || query in searchTerms.lowercase(Locale.ROOT) || query in emoji

@Composable
private fun EmojiSectionHeading(
    title: String,
    supportingText: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            supportingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmojiGrid(
    presets: List<IdentityEmojiPreset>,
    selectedEmoji: String,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        presets.forEach { preset ->
            EmojiChoice(
                emoji = preset.emoji,
                label = preset.label,
                selected = selectedEmoji == preset.emoji,
                onClick = { onSelect(preset.emoji) },
                modifier = Modifier.testTag("emoji-preset-${preset.label}"),
            )
        }
    }
}

@Composable
private fun SavedEmojiList(
    choices: List<CustomIdentityEmoji>,
    selectedEmoji: String,
    managing: Boolean,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEachIndexed { index, choice ->
            val selected = !managing && selectedEmoji == choice.emoji
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("emoji-saved-$index")
                    .selectable(
                        selected = selected,
                        role = if (managing) Role.Button else Role.RadioButton,
                        onClick = { if (managing) onRemove(choice.emoji) else onSelect(choice.emoji) },
                    )
                    .semantics {
                        role = if (managing) Role.Button else Role.RadioButton
                        this.selected = selected
                        contentDescription = if (managing) {
                            "Remove ${choice.name} custom emoji"
                        } else {
                            "${choice.name} emoji"
                        }
                    },
                shape = MaterialTheme.shapes.small,
                color = when {
                    managing -> MaterialTheme.colorScheme.errorContainer
                    selected -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        managing -> MaterialTheme.colorScheme.error
                        selected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outlineVariant
                    },
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WhipIdentityEmoji(choice.emoji)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(choice.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            choice.emoji,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (managing) Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun EmojiChoice(
    emoji: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
) {
    Surface(
        modifier = modifier
            .size(52.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
                contentDescription = "$label emoji"
            },
        shape = MaterialTheme.shapes.small,
        color = when {
            destructive -> MaterialTheme.colorScheme.errorContainer
            selected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            when {
                destructive -> MaterialTheme.colorScheme.error
                selected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        WhipIdentityEmoji(emoji, Modifier.size(52.dp))
    }
}
