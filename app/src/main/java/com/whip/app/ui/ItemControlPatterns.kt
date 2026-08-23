package com.whip.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

/**
 * Shared Whip interaction grammar:
 *
 *  * tabs switch peer pages and never wrap;
 *  * down arrows open a choice menu;
 *  * up/down chevrons reveal content in place;
 *  * right chevrons navigate to a child page;
 *  * vertical ellipses contain infrequent item actions;
 *  * trailing pencils always edit the exact item.
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
    WhipTextButton(onClick = onEdit) { Text(label.uiTitleCase()) }
}

/** A stable, single-row destination control for peer pages. */
@Composable
internal fun <T> DestinationTabBar(
    selected: T,
    destinations: List<T>,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    testTagPrefix: String? = null,
) {
    if (destinations.isEmpty()) return
    val destinationLabels = destinations.map { label(it).uiTitleCase() }
    key(destinationLabels) {
        val scrollState = rememberScrollState()
        Column(modifier = modifier.fillMaxWidth()) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val longestLabel = destinationLabels.maxOf(String::length)
                val shouldScroll = destinations.size > 4 || longestLabel > 8 || maxWidth < 92.dp * destinations.size
                val scrollingTabWidth = when {
                    longestLabel >= 10 -> 112.dp
                    longestLabel == 9 -> 104.dp
                    else -> 84.dp
                }
                Box(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .then(if (shouldScroll) Modifier.horizontalScroll(scrollState) else Modifier)
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        destinations.forEachIndexed { index, destination ->
                            val destinationLabel = destinationLabels[index]
                            val isSelected = selected == destination
                            val bringIntoViewRequester = remember(destinationLabel) { BringIntoViewRequester() }
                            LaunchedEffect(isSelected, shouldScroll) {
                                if (isSelected && shouldScroll) bringIntoViewRequester.bringIntoView()
                            }
                            Column(
                                modifier = Modifier
                                    .then(if (shouldScroll) Modifier.widthIn(min = scrollingTabWidth) else Modifier.weight(1f))
                                    .heightIn(min = 48.dp)
                                    .bringIntoViewRequester(bringIntoViewRequester)
                                    .selectable(
                                        selected = isSelected,
                                        onClick = { onSelect(destination) },
                                        role = Role.Tab,
                                    )
                                    .then(testTagPrefix?.let { Modifier.testTag("$it-$destinationLabel") } ?: Modifier),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier.height(45.dp).padding(
                                        horizontal = if (shouldScroll) 10.dp else 2.dp,
                                    ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        destinationLabel,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    )
                                }
                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                    thickness = 3.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                )
                            }
                        }
                    }
                    if (shouldScroll && scrollState.canScrollBackward) {
                        // Keep the fade constrained to the tab row without letting its
                        // fillMaxHeight request participate in the parent's measurement.
                        // Otherwise a scrollable tab bar can consume the whole content
                        // pane and push the destination page below the viewport.
                        Box(Modifier.matchParentSize()) {
                            Spacer(
                                Modifier.align(Alignment.CenterStart).width(20.dp).fillMaxHeight().background(
                                    Brush.horizontalGradient(
                                        listOf(MaterialTheme.colorScheme.surface, Color.Transparent),
                                    ),
                                ),
                            )
                        }
                    }
                    if (shouldScroll && scrollState.canScrollForward) {
                        Box(Modifier.matchParentSize()) {
                            Spacer(
                                Modifier.align(Alignment.CenterEnd).width(20.dp).fillMaxHeight().background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, MaterialTheme.colorScheme.surface),
                                    ),
                                ),
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** A compact one-of-many switch for alternate views of the same destination. */
@Composable
internal fun <T> SegmentedChoiceBar(
    selected: T,
    choices: List<T>,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        choices.forEachIndexed { index, choice ->
            SegmentedButton(
                selected = selected == choice,
                onClick = { onSelect(choice) },
                shape = RoundedCornerShape(
                    topStart = if (index == 0) 6.dp else 0.dp,
                    bottomStart = if (index == 0) 6.dp else 0.dp,
                    topEnd = if (index == choices.lastIndex) 6.dp else 0.dp,
                    bottomEnd = if (index == choices.lastIndex) 6.dp else 0.dp,
                ),
                modifier = Modifier.heightIn(min = 48.dp),
                label = { Text(label(choice).uiTitleCase(), maxLines = 1) },
            )
        }
    }
}

/** A disclosure reveals controls in place; its stable label does not change with state. */
@Composable
internal fun DisclosureButton(
    label: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    WhipOutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp).semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
        enabled = enabled,
    ) {
        Text(label.uiTitleCase())
        Spacer(Modifier.width(6.dp))
        Icon(
            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** A compact card that reveals optional content in place without looking like a child-page link. */
@Composable
internal fun DisclosureRow(
    title: String,
    supportingText: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = if (expanded) "Collapse $title" else "Expand $title", onClick = onClick)
            .semantics {
                role = Role.Button
                stateDescription = if (expanded) "Expanded" else "Collapsed"
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title.uiTitleCase(), fontWeight = FontWeight.SemiBold)
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
            )
        }
    }
}

/** A one-of-many value selector. Down arrows are reserved for this menu behavior. */
@Composable
internal fun <T> SelectionField(
    label: String,
    values: List<T>,
    selected: T,
    valueText: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box(Modifier.fillMaxWidth()) {
            WhipOutlinedButton(
                enabled = enabled,
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = "$label: ${valueText(selected)}"
                        stateDescription = if (expanded) "Menu open" else "Menu closed"
                    },
            ) {
                Text(
                    valueText(selected),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                values.forEach { value ->
                    val isSelected = value == selected
                    DropdownMenuItem(
                        text = { Text((if (isSelected) "✓  " else "") + valueText(value)) },
                        onClick = {
                            onSelect(value)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

/** A row/card that visibly navigates into a child page. */
@Composable
internal fun NavigationRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
) {
    val displayTitle = title.uiTitleCase()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClickLabel = "Open $displayTitle", onClick = onClick)
            .semantics { role = Role.Button },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(displayTitle, fontWeight = FontWeight.SemiBold)
                supportingText?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}

/**
 * A centered action used in peer toolbars. Every toolbar item uses the same
 * internal layout so text-only and icon-plus-text actions share a visual axis.
 */
@Composable
internal fun ToolbarActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    stateful: Boolean = false,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val displayLabel = label.uiTitleCase()
    val colors = ButtonDefaults.outlinedButtonColors(
        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    )
    WhipOutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics {
                if (stateful) {
                    selected = active
                    stateDescription = if (active) "Active" else "Inactive"
                }
            },
        colors = colors,
        border = if (active) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            ButtonDefaults.outlinedButtonBorder(enabled)
        },
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Row(
            modifier = Modifier.testTag("toolbar-action-content-$displayLabel"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(displayLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A temporary mode action with an explicit active state. */
@Composable
internal fun ModeButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ToolbarActionButton(
        label = if (active) "Done" else label,
        onClick = onClick,
        modifier = modifier,
        active = active,
        stateful = true,
    )
}

@Composable
internal fun DetailSectionBar(
    labels: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    testTagPrefix: String? = null,
) {
    DestinationTabBar(
        selected = selected,
        destinations = labels,
        onSelect = onSelect,
        label = { it },
        testTagPrefix = testTagPrefix,
    )
}
