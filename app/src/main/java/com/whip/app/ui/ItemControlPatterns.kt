package com.whip.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.whip.app.R

/** User-selected collection density; unrelated to window-size or fold posture. */
internal val LocalCompactItemLayout = staticCompositionLocalOf { false }

/** Coordinates independently expanded compact collection rows. */
@Stable
internal class CompactItemExpansionState(initialExpandedItemKeys: Set<String> = emptySet()) {
    var expandedItemKeys by mutableStateOf(initialExpandedItemKeys.toSet())
        private set

    fun toggle(itemKey: String) {
        expandedItemKeys = if (itemKey in expandedItemKeys) {
            expandedItemKeys - itemKey
        } else {
            expandedItemKeys + itemKey
        }
    }

    fun collapseAll() {
        expandedItemKeys = emptySet()
    }
}

private val CompactItemExpansionStateSaver = Saver<CompactItemExpansionState, ArrayList<String>>(
    save = { ArrayList(it.expandedItemKeys) },
    restore = { CompactItemExpansionState(it.toSet()) },
)

@Composable
internal fun rememberCompactItemExpansionState(): CompactItemExpansionState = rememberSaveable(
    saver = CompactItemExpansionStateSaver,
) { CompactItemExpansionState() }

internal val LocalCompactItemExpansionState = staticCompositionLocalOf<CompactItemExpansionState?> { null }

internal data class CompactItemDisclosure(
    val expanded: Boolean,
    val toggle: () -> Unit,
)

@Composable
internal fun rememberCompactItemDisclosure(
    itemKey: String,
): CompactItemDisclosure {
    val fallback = rememberCompactItemExpansionState()
    val expansionState = LocalCompactItemExpansionState.current ?: fallback
    return CompactItemDisclosure(
        expanded = itemKey in expansionState.expandedItemKeys,
        toggle = { expansionState.toggle(itemKey) },
    )
}

/** A one-line text action sized for the trailing lane of a compact item row. */
@Composable
internal fun ItemPrimaryTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    WhipTextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        Text(
            text = label,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

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

/**
 * Shared collection-card surface for Tasks, Habits, and Goals.
 *
 * Keeping the surface, corner treatment, inset, and vertical rhythm here stops
 * each productivity area from gradually developing its own visual grammar.
 */
@Composable
internal fun ProductivityItemCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit,
) {
    val compact = LocalCompactItemLayout.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = if (compact) MaterialTheme.shapes.small else MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 6.dp else 14.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
            content = content,
        )
    }
}

/**
 * Shared collection-card header hierarchy:
 *
 *     identity emoji -> title/context -> primary action -> edit
 *
 * The primary action uses a stable, label-aware trailing lane when present. This
 * keeps completion, logging, rating, timer, and reset controls predictable without
 * pretending those domain actions all mean the same thing.
 */
@Composable
internal fun ProductivityItemHeader(
    itemType: String,
    itemName: String,
    emoji: String,
    areaId: String?,
    areaName: String,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier,
    identityModifier: Modifier = Modifier,
    primaryActionModifier: Modifier = Modifier,
    editModifier: Modifier = Modifier,
    titleTextDecoration: TextDecoration? = null,
    headlineAccessory: (@Composable RowScope.() -> Unit)? = null,
    supportingContent: @Composable ColumnScope.() -> Unit = {},
    compactSummaryContent: @Composable ColumnScope.() -> Unit = {},
    compactExpanded: Boolean = false,
    onCompactExpansionToggle: (() -> Unit)? = null,
    compactExpansionTag: String? = null,
    compactPrimaryActionWidth: Dp = 64.dp,
    primaryAction: (@Composable () -> Unit)? = null,
) {
    val compact = LocalCompactItemLayout.current
    if (compact) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(identityModifier) { WhipIdentityEmoji(emoji) }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = itemName,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = titleTextDecoration,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (onCompactExpansionToggle != null && !compactExpanded) compactSummaryContent()
                }
                primaryAction?.let { action ->
                    Box(
                        modifier = primaryActionModifier.width(compactPrimaryActionWidth).heightIn(min = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) { action() }
                }
                if (onCompactExpansionToggle != null) {
                    IconButton(
                        onClick = onCompactExpansionToggle,
                        modifier = Modifier
                            .size(48.dp)
                            .then(compactExpansionTag?.let(Modifier::testTag) ?: Modifier)
                            .semantics {
                                contentDescription = "${if (compactExpanded) "Collapse" else "Expand"} $itemType $itemName"
                                stateDescription = if (compactExpanded) "Expanded" else "Collapsed"
                            },
                    ) {
                        Icon(
                            if (compactExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null,
                        )
                    }
                } else if (onEdit != null) {
                    ItemEditButton(itemType, itemName, onEdit, editModifier)
                }
            }
            if (onCompactExpansionToggle == null || compactExpanded) {
                if (onCompactExpansionToggle != null) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val stacked = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.5f
                    if (stacked) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            headlineAccessory?.let { accessory ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { accessory() }
                            }
                            if (areaId != null) AreaBadge(areaId, areaName)
                            supportingContent()
                            if (onCompactExpansionToggle != null && onEdit != null) {
                                WhipTextButton(
                                    onClick = onEdit,
                                    modifier = editModifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .semantics { contentDescription = "Edit $itemType $itemName" },
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Edit")
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                headlineAccessory?.let { accessory ->
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { accessory() }
                                }
                                if (areaId != null) AreaBadge(areaId, areaName)
                                supportingContent()
                            }
                            if (onCompactExpansionToggle != null && onEdit != null) {
                                ItemEditButton(itemType, itemName, onEdit, editModifier)
                            }
                        }
                    }
                }
            }
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(identityModifier) { WhipIdentityEmoji(emoji) }
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = itemName,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = titleTextDecoration,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                // Status badges must never compete with the item name for horizontal
                // space. Narrow split panes still reserve the shared action lanes, so
                // an inline badge could collapse even a short title to a few glyphs.
                headlineAccessory?.let { accessory ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) { accessory() }
                }
                if (areaId != null) AreaBadge(areaId, areaName)
                supportingContent()
            }
            primaryAction?.let { action ->
                Box(
                    modifier = primaryActionModifier.width(72.dp).heightIn(min = 48.dp),
                    contentAlignment = Alignment.Center,
                ) { action() }
            }
            if (onEdit != null) ItemEditButton(itemType, itemName, onEdit, editModifier)
        }
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
    modifier: Modifier = Modifier,
    primaryDestinations: List<T> = destinations.take(4),
    onSelect: (T) -> Unit,
    label: (T) -> String,
    compactLabel: (T) -> String = label,
    testTagPrefix: String? = null,
    barTestTag: String? = null,
    resetCompactItemExpansionOnChange: Boolean = true,
) {
    if (destinations.isEmpty()) return
    val fontScale = LocalDensity.current.fontScale
    val compactItemExpansionState = LocalCompactItemExpansionState.current
    var pagesExpanded by rememberSaveable { mutableStateOf(false) }
    fun selectDestination(destination: T) {
        if (resetCompactItemExpansionOnChange && destination != selected) {
            compactItemExpansionState?.collapseAll()
        }
        onSelect(destination)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(barTestTag?.let(Modifier::testTag) ?: Modifier),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val visibleCapacity = when {
                fontScale >= 2f -> 2
                fontScale >= 1.5f || maxWidth < 340.dp -> 2
                maxWidth < 380.dp && destinations.any { compactLabel(it).length > 10 } -> 3
                destinations.size <= 4 -> 4
                maxWidth < 520.dp -> 3
                else -> 4
            }
            val preferred = primaryDestinations.filter { it in destinations }
            val orderedCandidates = (preferred + destinations).distinct()
            val visible = orderedCandidates.take(visibleCapacity).toMutableList().also { shown ->
                if (selected !in shown) {
                    if (shown.isEmpty()) shown += selected
                    else shown[shown.lastIndex] = selected
                }
            }.distinct()
            val hidden = destinations.filterNot(visible::contains)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(Modifier.weight(1f).selectableGroup()) {
                    visible.forEach { destination ->
                        val destinationLabel = label(destination).uiTitleCase()
                        val visibleLabel = compactLabel(destination).uiTitleCase()
                        val isSelected = selected == destination
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .selectable(
                                    selected = isSelected,
                                    onClick = { selectDestination(destination) },
                                    role = Role.Tab,
                                )
                                .semantics { contentDescription = destinationLabel }
                                .then(testTagPrefix?.let { Modifier.testTag("$it-$destinationLabel") } ?: Modifier),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(Modifier.height(45.dp).padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    visibleLabel,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                                thickness = 3.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            )
                        }
                    }
                }
                if (hidden.isNotEmpty()) Box {
                    WhipTextButton(
                        onClick = { pagesExpanded = true },
                        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Open Pages" },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    ) {
                        Text(stringResource(R.string.action_more), maxLines = 1)
                        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = pagesExpanded, onDismissRequest = { pagesExpanded = false }) {
                        hidden.forEach { destination ->
                            val destinationLabel = label(destination).uiTitleCase()
                            DropdownMenuItem(
                                text = { Text(destinationLabel) },
                                trailingIcon = if (destination == selected) {{ Icon(Icons.Outlined.Check, contentDescription = "Selected") }} else null,
                                onClick = { pagesExpanded = false; selectDestination(destination) },
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
    testTagPrefix: String? = null,
    resetCompactItemExpansionOnChange: Boolean = false,
) {
    val compactItemExpansionState = LocalCompactItemExpansionState.current
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        choices.forEachIndexed { index, choice ->
            SegmentedButton(
                selected = selected == choice,
                onClick = {
                    if (resetCompactItemExpansionOnChange && choice != selected) {
                        compactItemExpansionState?.collapseAll()
                    }
                    onSelect(choice)
                },
                shape = RoundedCornerShape(
                    topStart = if (index == 0) 6.dp else 0.dp,
                    bottomStart = if (index == 0) 6.dp else 0.dp,
                    topEnd = if (index == choices.lastIndex) 6.dp else 0.dp,
                    bottomEnd = if (index == choices.lastIndex) 6.dp else 0.dp,
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .then(testTagPrefix?.let { Modifier.testTag("$it-${label(choice)}") } ?: Modifier),
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
                        text = { Text(valueText(value)) },
                        leadingIcon = if (isSelected) {{ Icon(Icons.Outlined.Check, contentDescription = "Selected") }} else null,
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
        resetCompactItemExpansionOnChange = false,
    )
}
