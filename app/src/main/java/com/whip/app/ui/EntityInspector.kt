package com.whip.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

internal data class EntityInspectorSection(
    val id: String,
    val label: String,
    val legacyLabel: String = label,
)

internal data class EntityInspectorPrimaryAction(
    val id: String,
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

/**
 * Whip's shared read-first entity command center. It deliberately differs from
 * an editor: identity and navigation stay put while evidence scrolls, and the
 * single most useful existing action occupies a stable bottom dock.
 */
@Composable
internal fun EntityInspector(
    entityType: String,
    title: String,
    emoji: String,
    context: String,
    status: String,
    sections: List<EntityInspectorSection>,
    selectedSectionId: String,
    onSelectSection: (String) -> Unit,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)?,
    editLabel: String = "Edit",
    modifier: Modifier = Modifier,
    legacySurfaceTag: String? = null,
    legacySectionTagPrefix: String? = null,
    primaryAction: EntityInspectorPrimaryAction? = null,
    content: @Composable () -> Unit,
) {
    val placement = LocalWhipDialogPlacement.current
    val resolvedModifier = if (modifier == Modifier) {
        Modifier.absoluteOffset(x = placement.offsetX).width(placement.maxWidth)
    } else modifier
    val paneDescription = "$entityType details for $title"
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            // The inspector is a stable command center, not an intrinsic-height
            // alert. Keep its frame fixed while tabs swap differently sized content.
            val inspectorHeight = minOf(maxHeight * 0.94f, 720.dp)
            Surface(
                modifier = resolvedModifier
                    .widthIn(min = 280.dp, max = 560.dp)
                    .height(inspectorHeight)
                    .then(legacySurfaceTag?.let(Modifier::testTag) ?: Modifier)
                    .semantics { paneTitle = paneDescription },
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("entity-inspector"),
                ) {
                    EntityInspectorHeader(
                        entityType = entityType,
                        title = title,
                        emoji = emoji,
                        context = context,
                        status = status,
                        onDismiss = onDismiss,
                        onEdit = onEdit,
                        editLabel = editLabel,
                    )
                    if (sections.size > 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        EntityInspectorSectionSelector(
                            sections = sections,
                            selectedSectionId = selectedSectionId,
                            onSelect = onSelectSection,
                            legacySectionTagPrefix = legacySectionTagPrefix,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("entity-inspector-content-$selectedSectionId")
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        content()
                    }
                    primaryAction?.let { action ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            WhipButton(
                                onClick = action.onClick,
                                enabled = action.enabled,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                                    .heightIn(min = 48.dp)
                                    .testTag("entity-inspector-primary-${action.id}"),
                            ) {
                                Text(action.label, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntityInspectorHeader(
    entityType: String,
    title: String,
    emoji: String,
    context: String,
    status: String,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)?,
    editLabel: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("entity-inspector-header")
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(48.dp)
                .testTag("entity-inspector-close")
                .semantics { contentDescription = "Close $entityType details" },
        ) {
            Icon(Icons.Outlined.Close, contentDescription = null)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("entity-inspector-title")
                        .semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOf(entityType, context).filter(String::isNotBlank).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Surface(
                    modifier = Modifier.testTag("entity-inspector-status"),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
        if (onEdit != null) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("entity-inspector-edit")
                    .semantics { contentDescription = editLabel },
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }
    }
}

@Composable
private fun EntityInspectorSectionSelector(
    sections: List<EntityInspectorSection>,
    selectedSectionId: String,
    onSelect: (String) -> Unit,
    legacySectionTagPrefix: String?,
) {
    var overflowExpanded by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("entity-inspector-section-selector"),
    ) {
        val fontScale = LocalDensity.current.fontScale
        val visibleCapacity = when {
            // A two-page inspector is the common Overview / Options contract.
            // Both controls fit even in the compact dialog lane, so changing
            // posture or opening the same inspector on the cover display must
            // never turn one of them into a hidden "More" destination.
            sections.size <= 2 -> sections.size
            fontScale >= 2f || maxWidth < 320.dp -> 1
            sections.size <= 3 && fontScale < 1.3f -> sections.size
            fontScale >= 1.3f || maxWidth < 480.dp -> 2
            else -> 3
        }.coerceAtMost(sections.size)
        val initiallyVisible = sections.take(visibleCapacity).toMutableList()
        val selectedSection = sections.firstOrNull { it.id == selectedSectionId }
        if (selectedSection != null && selectedSection !in initiallyVisible) {
            initiallyVisible[initiallyVisible.lastIndex] = selectedSection
        }
        val visible = initiallyVisible.distinct()
        val hidden = sections.filterNot(visible::contains)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                visible.forEach { section ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("entity-inspector-section-${section.id}"),
                    ) {
                        EntityInspectorSectionTab(
                            section = section,
                            selected = section.id == selectedSectionId,
                            onSelect = onSelect,
                            modifier = Modifier.then(
                                legacySectionTagPrefix?.let { prefix ->
                                    Modifier.testTag("$prefix-${section.legacyLabel}")
                                } ?: Modifier,
                            ),
                        )
                    }
                }
            }
            if (hidden.isNotEmpty()) {
                Box {
                    WhipTextButton(
                        onClick = { overflowExpanded = true },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .semantics { contentDescription = "Open Pages" },
                    ) {
                        Text("More", maxLines = 1)
                        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = overflowExpanded,
                        onDismissRequest = { overflowExpanded = false },
                    ) {
                        hidden.forEach { section ->
                            DropdownMenuItem(
                                text = { Text(section.label) },
                                trailingIcon = if (section.id == selectedSectionId) {
                                    { Icon(Icons.Outlined.Check, contentDescription = "Selected") }
                                } else null,
                                onClick = {
                                    overflowExpanded = false
                                    onSelect(section.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntityInspectorSectionTab(
    section: EntityInspectorSection,
    selected: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                onClick = { onSelect(section.id) },
                role = Role.Tab,
            ),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(section.label, maxLines = 1)
        }
    }
}

@Composable
internal fun EntityInspectorGroup(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        supportingText?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

@Composable
internal fun EntityInspectorAction(
    id: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("entity-inspector-action-$id"),
        shape = MaterialTheme.shapes.small,
        color = if (danger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (danger) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
internal fun EntityInspectorDangerZone(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("entity-inspector-danger-zone"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.34f),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "Danger Zone",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            content()
        }
    }
}

@Composable
internal fun EntityInspectorFact(label: String, value: String, modifier: Modifier = Modifier) {
    if (value.isBlank()) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
