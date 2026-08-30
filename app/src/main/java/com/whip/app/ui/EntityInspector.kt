package com.whip.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whip.app.ui.theme.whipColors

internal enum class WhipStatusTone {
    Neutral,
    Info,
    Success,
    Warning,
    Destructive,
}

internal enum class EntityInspectorSectionPlacement { Direct, Overflow }

internal data class EntityInspectorSection(
    val id: String,
    val label: String,
    val legacyLabel: String = label,
    val placement: EntityInspectorSectionPlacement = EntityInspectorSectionPlacement.Direct,
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
    statusTone: WhipStatusTone = WhipStatusTone.Neutral,
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
    val sectionStateHolder = rememberSaveableStateHolder()
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
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        statusTone = statusTone,
                        onDismiss = onDismiss,
                        onEdit = onEdit,
                        editLabel = editLabel,
                    )
                    if (sections.size > 1) {
                        EntityInspectorSectionSelector(
                            entityType = entityType,
                            sections = sections,
                            selectedSectionId = selectedSectionId,
                            onSelect = onSelectSection,
                            legacySectionTagPrefix = legacySectionTagPrefix,
                        )
                    } else HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("entity-inspector-content-$selectedSectionId")
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        sectionStateHolder.SaveableStateProvider("$paneDescription::$selectedSectionId") {
                            content()
                        }
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
    statusTone: WhipStatusTone,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)?,
    editLabel: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("entity-inspector-header")
            .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WhipIdentityEmoji(
            emoji = emoji,
            modifier = Modifier.padding(top = 3.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                title,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("entity-inspector-title")
                    .semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
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
            WhipStatusBadge(
                label = status,
                tone = statusTone,
                modifier = Modifier.testTag("entity-inspector-status"),
            )
        }
        if (onEdit != null) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("entity-inspector-edit")
                    .semantics { contentDescription = editLabel },
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(48.dp)
                .testTag("entity-inspector-close")
                .semantics { contentDescription = "Close $entityType details" },
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun WhipStatusBadge(
    label: String,
    tone: WhipStatusTone,
    modifier: Modifier = Modifier,
) {
    val semanticColors = MaterialTheme.whipColors
    val (containerColor, contentColor) = when (tone) {
        WhipStatusTone.Neutral ->
            MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
        WhipStatusTone.Info -> semanticColors.action.copy(alpha = 0.14f) to semanticColors.action
        WhipStatusTone.Success -> semanticColors.success.copy(alpha = 0.16f) to semanticColors.success
        WhipStatusTone.Warning -> semanticColors.warning.copy(alpha = 0.18f) to semanticColors.warning
        WhipStatusTone.Destructive -> semanticColors.destructive.copy(alpha = 0.14f) to semanticColors.destructive
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
        )
    }
}

@Composable
private fun EntityInspectorSectionSelector(
    entityType: String,
    sections: List<EntityInspectorSection>,
    selectedSectionId: String,
    onSelect: (String) -> Unit,
    legacySectionTagPrefix: String?,
) {
    DestinationTabBar(
        selected = sections.first { it.id == selectedSectionId },
        destinations = sections,
        primaryDestinations = sections.filter { it.placement == EntityInspectorSectionPlacement.Direct },
        onSelect = { onSelect(it.id) },
        label = EntityInspectorSection::label,
        testTagPrefix = legacySectionTagPrefix ?: "entity-inspector-section",
        testTagValue = if (legacySectionTagPrefix == null) {
            EntityInspectorSection::id
        } else {
            EntityInspectorSection::legacyLabel
        },
        secondaryTestTagPrefix = "entity-inspector-section".takeIf { legacySectionTagPrefix != null },
        secondaryTestTagValue = EntityInspectorSection::id,
        barTestTag = "entity-inspector-section-selector",
        overflowLabel = "More $entityType options",
        resetCompactItemExpansionOnChange = false,
    )
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
            fontWeight = FontWeight.SemiBold,
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
    supportingText: String? = null,
) {
    if (danger) {
        WhipActionRow(
            title = label,
            onClick = onClick,
            modifier = modifier.testTag("entity-inspector-action-$id"),
            supportingText = supportingText,
            icon = Icons.Outlined.DeleteForever,
            enabled = enabled,
            navigates = false,
            danger = true,
        )
        return
    }
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
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            supportingText?.let { supporting ->
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (danger) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun EntityInspectorDangerZone(content: @Composable ColumnScope.() -> Unit) {
    WhipDangerZone(
        modifier = Modifier.testTag("entity-inspector-danger-zone"),
        content = { Column(content = content) },
    )
}

@Composable
internal fun EntityInspectorFact(label: String, value: String, modifier: Modifier = Modifier) {
    if (value.isBlank()) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
