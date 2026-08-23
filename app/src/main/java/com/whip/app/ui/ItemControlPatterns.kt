package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * The shared interaction grammar for first-class Whip items:
 *
 *  * tapping a card opens its details;
 *  * the trailing pencil always edits that item directly;
 *  * status-specific quick actions remain separate controls;
 *  * destructive and infrequent actions live in the details dialog's More section.
 *
 * Keeping these controls here makes it harder for Tasks, Habits, and Goals to drift
 * into separate interaction schemes again.
 */
@Composable
internal fun ItemEditButton(
    itemType: String,
    itemName: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onEdit, modifier = modifier.size(48.dp)) {
        Icon(
            Icons.Outlined.Edit,
            contentDescription = "Edit $itemType $itemName",
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
internal fun DetailEditButton(label: String, onEdit: () -> Unit) {
    TextButton(onClick = onEdit) { Text(label) }
}

@Composable
internal fun DetailSectionBar(
    labels: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    testTagPrefix: String? = null,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEach { label ->
            FilterChip(
                selected = selected == label,
                onClick = { onSelect(label) },
                label = { Text(label) },
                modifier = testTagPrefix?.let { Modifier.testTag("$it-$label") } ?: Modifier,
            )
        }
    }
}

/**
 * Consistent compact destination navigation: three common destinations stay visible,
 * while lower-frequency destinations expand from the same More chip on every domain.
 */
@Composable
internal fun <T> ProgressiveDestinationBar(
    selected: T,
    primary: List<T>,
    secondary: List<T>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    testTagPrefix: String? = null,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        primary.forEach { destination ->
            FilterChip(
                selected = selected == destination,
                onClick = { onSelect(destination) },
                label = { Text(label(destination)) },
                modifier = testTagPrefix?.let { Modifier.testTag("$it-${label(destination)}") } ?: Modifier,
            )
        }
        if (secondary.isNotEmpty()) {
            FilterChip(
                selected = expanded || selected in secondary,
                onClick = { onExpandedChange(!expanded) },
                label = { Text("More") },
                modifier = testTagPrefix?.let { Modifier.testTag("$it-more") } ?: Modifier,
            )
            if (expanded || selected in secondary) {
                secondary.forEach { destination ->
                    FilterChip(
                        selected = selected == destination,
                        onClick = { onSelect(destination) },
                        label = { Text(label(destination)) },
                        modifier = testTagPrefix?.let { Modifier.testTag("$it-${label(destination)}") } ?: Modifier,
                    )
                }
            }
        }
    }
}
