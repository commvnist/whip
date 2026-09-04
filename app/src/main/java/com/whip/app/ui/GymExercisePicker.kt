package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.whip.app.domain.Exercise

/** Shared exercise search/create body for single and multi-selection pickers. */
@Composable
internal fun GymExercisePickerBody(
    exercises: List<Exercise>,
    modifier: Modifier = Modifier,
    itemLabel: String = "exercise",
    saving: Boolean = false,
    queryKey: Any,
    listTag: String,
    searchTag: String = "exercise-picker-search",
    createTag: String = "exercise-picker-create",
    emptyTag: String = "exercise-picker-empty",
    priorityIds: List<Long> = emptyList(),
    priorityGroupLabel: String? = null,
    supportingText: String? = null,
    errorMessage: String? = null,
    matches: (Exercise, String) -> Boolean = { exercise, search -> exerciseMatchesQuery(exercise, search) },
    sort: (List<Exercise>) -> List<Exercise> = { visible ->
        visible.sortedWith(compareByDescending<Exercise> { it.id in priorityIds }.thenBy { it.name.lowercase() })
    },
    onCreate: ((String) -> Unit)?,
    header: @Composable (() -> Unit)? = null,
    filters: @Composable (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null,
    row: @Composable (Exercise) -> Unit,
) {
    var query by rememberSaveable(queryKey) { mutableStateOf("") }
    val normalizedQuery = query.trim()
    val visible = sort(exercises.filter { matches(it, query) })
    val displayLabel = itemLabel.uiTitleCase()

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
    ) {
        header?.invoke()
        supportingText?.let { message ->
            Text(
                message,
                Modifier.testTag("workout-exercise-picker-scope"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        errorMessage?.let { message ->
            Text(
                message,
                Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            enabled = !saving,
            label = { Text("Search ${displayLabel.lowercase()}s") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag(searchTag),
        )
        filters?.invoke()
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                quantityLabel(visible.size, itemLabel),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onCreate != null) {
                WhipOutlinedButton(
                    onClick = { onCreate(normalizedQuery) },
                    enabled = !saving,
                    modifier = Modifier.testTag(createTag),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(WhipSpacing.sibling))
                    Text("Create $displayLabel")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).testTag(listTag),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
            contentPadding = PaddingValues(bottom = WhipSpacing.compact),
        ) {
            if (priorityGroupLabel != null && visible.any { it.id in priorityIds }) item {
                Text(
                    priorityGroupLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(visible, key = Exercise::id) { row(it) }
            if (visible.isEmpty()) item {
                OutlinedCard(Modifier.fillMaxWidth().testTag(emptyTag)) {
                    Column(
                        Modifier.fillMaxWidth().padding(WhipSpacing.standard),
                        verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
                    ) {
                        Text(
                            if (normalizedQuery.isBlank() && exercises.isEmpty()) {
                                "No ${displayLabel.lowercase()}s yet"
                            } else {
                                "No matching ${displayLabel.lowercase()}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (onCreate == null) {
                                "Try a different search or close this picker."
                            } else if (normalizedQuery.isBlank()) {
                                "Create a $itemLabel without leaving this screen. It will be saved to your Exercise Library."
                            } else {
                                "Nothing matches “$normalizedQuery”. Create it as a new $itemLabel without leaving this screen."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (onCreate != null) {
                            WhipButton(
                                onClick = { onCreate(normalizedQuery) },
                                enabled = !saving,
                                modifier = Modifier.fillMaxWidth().testTag("$createTag-empty"),
                            ) {
                                Text(
                                    if (normalizedQuery.isBlank()) "Create $displayLabel"
                                    else "Create “$normalizedQuery”",
                                )
                            }
                        }
                    }
                }
            }
        }
        footer?.invoke()
    }
}
