package com.whip.app.ui

import com.whip.app.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints

internal object WhipSpacing {
    val micro = 4.dp
    val sibling = 8.dp
    val compact = 12.dp
    val standard = 16.dp
    val screenCompact = 20.dp
    val screenExpanded = 24.dp
    val major = 32.dp
}

internal object WhipContentWidth {
    val compactDialog = 560.dp
    val authoredForm = 720.dp
}

/**
 * Shared chrome for full-screen authored editors.
 *
 * The title and exit action keep a stable first row. At narrow widths or large
 * text, authored actions move to their own trailing row instead of squeezing,
 * truncating, or hiding the editor identity.
 */
@Composable
internal fun WhipEditorHeader(
    navigationAction: @Composable () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    hasActions: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Layout(
        modifier = modifier.fillMaxWidth().testTag("editor-header")
            .padding(horizontal = WhipSpacing.sibling, vertical = WhipSpacing.micro),
        content = {
            Box { navigationAction() }
            ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                Box(Modifier.semantics { heading() }.testTag("editor-title")) { title() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
                verticalAlignment = Alignment.CenterVertically) { if (hasActions) actions() }
        },
    ) { children, constraints ->
        val gap = WhipSpacing.micro.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val navigation = children[0].measure(loose)
        val actionsPlaceable = children[2].measure(loose)
        val titleMinimum = children[1].minIntrinsicWidth(Constraints.Infinity)
        val stack = hasActions && navigation.width + actionsPlaceable.width + titleMinimum + gap * 2 > constraints.maxWidth
        val titleWidth = (constraints.maxWidth - navigation.width - gap -
            if (stack) 0 else actionsPlaceable.width + gap).coerceAtLeast(0)
        val titlePlaceable = children[1].measure(loose.copy(maxWidth = titleWidth))
        val firstHeight = maxOf(48.dp.roundToPx(), navigation.height, titlePlaceable.height,
            if (stack) 0 else actionsPlaceable.height)
        val height = firstHeight + if (stack) gap + actionsPlaceable.height else 0
        layout(constraints.maxWidth, height) {
            navigation.placeRelative(0, (firstHeight - navigation.height) / 2)
            titlePlaceable.placeRelative(navigation.width + gap, (firstHeight - titlePlaceable.height) / 2)
            actionsPlaceable.placeRelative(constraints.maxWidth - actionsPlaceable.width,
                if (stack) firstHeight + gap else (firstHeight - actionsPlaceable.height) / 2)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** A leading Up action for hierarchical child pages. */
@Composable
internal fun WhipBackAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp).semantics { contentDescription = label },
    ) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
    }
}

/**
 * Dismisses an app-level destination from the trailing edge of its header.
 *
 * Back remains a leading navigation action on hierarchical child pages; an X is
 * always an exit, so keeping it trailing matches Settings and keeps app-level
 * workspaces reachable with the same thumb movement.
 */
@Composable
internal fun WhipTrailingCloseAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(48.dp).semantics { contentDescription = label },
    ) {
        Icon(Icons.Outlined.Close, contentDescription = null)
    }
}

/**
 * A destination-sized overlay owns the complete edge-to-edge window surface,
 * while its interactive content stays inside the safe drawing insets.
 *
 * Keeping the inset on the child is intentional: putting it on [Surface]
 * exposes the split layout underneath the transparent status bar on a Fold,
 * even though the overlay itself is visually full width below that bar.
 */
@Composable
internal fun WhipFullScreenSurface(
    title: String,
    modifier: Modifier = Modifier,
    contentInsets: WindowInsets = WindowInsets.safeDrawing,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .semantics { paneTitle = title }
            .testTag("full-screen-destination-surface"),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(contentInsets)
                .testTag("full-screen-destination-content"),
            content = content,
        )
    }
}

/** Measures header content only; the containing list/column owns the gap to its next item. */
@Composable
internal fun WhipPageHeader(
    title: String,
    supportingText: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) = WhipPageHeader(title, Modifier, supportingText, actions)

@Composable
internal fun WhipPageHeader(
    title: String,
    modifier: Modifier,
    supportingText: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
    ) {
        val titleContent: @Composable () -> Unit = {
            Text(
                title,
                modifier = Modifier.semantics { heading() }.testTag("page-title"),
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Layout(
            modifier = Modifier.fillMaxWidth(),
            content = {
                Box { titleContent() }
                Row(verticalAlignment = Alignment.CenterVertically, content = actions ?: {})
            },
        ) { measurables, constraints ->
            val availableWidth = constraints.maxWidth
            val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            val titlePlaceable = measurables[0].measure(looseConstraints)
            val actionsPlaceable = measurables[1].measure(looseConstraints)
            val minimumRowHeight = 48.dp.roundToPx()
            val titleRowHeight = maxOf(minimumRowHeight, titlePlaceable.height)
            val stackActions = actionsPlaceable.width > 0 && (
                availableWidth < 300.dp.roundToPx() ||
                    titlePlaceable.width + WhipSpacing.sibling.roundToPx() + actionsPlaceable.width > availableWidth
                )
            if (stackActions) {
                val actionsTop = titleRowHeight + WhipSpacing.micro.roundToPx()
                layout(availableWidth, actionsTop + actionsPlaceable.height) {
                    titlePlaceable.placeRelative(0, (titleRowHeight - titlePlaceable.height) / 2)
                    actionsPlaceable.placeRelative(availableWidth - actionsPlaceable.width, actionsTop)
                }
            } else {
                val rowHeight = maxOf(titleRowHeight, actionsPlaceable.height)
                layout(availableWidth, rowHeight) {
                    titlePlaceable.placeRelative(0, (rowHeight - titlePlaceable.height) / 2)
                    actionsPlaceable.placeRelative(availableWidth - actionsPlaceable.width, (rowHeight - actionsPlaceable.height) / 2)
                }
            }
        }
        supportingText?.takeIf(String::isNotBlank)?.let { supporting ->
            WhipPageSupportingText(
                supporting,
                modifier = Modifier.testTag("page-supporting-text"),
            )
        }
    }
}

/** Page introductions and empty explanations use the same supporting-text role. */
@Composable
private fun WhipPageSupportingText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
    )
}

@Composable
internal fun WhipPageIconAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    active: Boolean = false,
    enabled: Boolean = true,
) {
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        active -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(48.dp)
            .semantics {
                contentDescription = if (badgeCount > 0) "$label, $badgeCount active" else label
            },
    ) {
        BadgedBox(
            badge = {
                if (badgeCount > 0) {
                    if (LocalDensity.current.fontScale > 1.3f) {
                        Badge(Modifier.testTag("page-action-badge"))
                    } else {
                        Badge(Modifier.testTag("page-action-badge")) { Text(badgeCount.toString()) }
                    }
                }
            },
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
        }
    }
}

@Composable
internal fun <T> WhipViewAndFilterRow(
    selectedView: T,
    views: List<T>,
    viewLabel: (T) -> String,
    onSelectView: (T) -> Unit,
    filterCount: Int,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
    trailingActions: @Composable RowScope.() -> Unit = {},
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val showFilterLabel = maxWidth >= 440.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (views.size > 1) {
                SegmentedChoiceBar(
                    selected = selectedView,
                    choices = views,
                    onSelect = onSelectView,
                    label = viewLabel,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            if (showFilterLabel) {
                WhipTonalButton(
                    onClick = onOpenFilters,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Outlined.FilterAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(WhipSpacing.sibling))
                    Text(
                        if (filterCount > 0) stringResource(R.string.filter_count, filterCount)
                        else stringResource(R.string.action_filter),
                    )
                }
            } else {
                WhipPageIconAction(
                    icon = Icons.Outlined.FilterAlt,
                    label = stringResource(R.string.action_filter),
                    onClick = onOpenFilters,
                    badgeCount = filterCount,
                    active = filterCount > 0,
                )
            }
            trailingActions()
        }
    }
}

internal data class WhipActiveFilter(
    val key: String,
    val label: String,
    val onRemove: () -> Unit,
)

@Composable
internal fun WhipActiveFilterRow(
    filters: List<WhipActiveFilter>,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (filters.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth().testTag("active-filter-row"),
        horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
    ) {
        filters.forEach { filter ->
            WhipFilterChip(
                selected = true,
                onClick = filter.onRemove,
                label = { Text(filter.label) },
                trailingIcon = {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.action_remove_filter, filter.label),
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
        WhipTextButton(onClick = onClearAll) { Text(stringResource(R.string.action_clear_all)) }
    }
}

@Composable
internal fun WhipEmptyState(
    title: String,
    supportingText: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = WhipSpacing.standard)
            .testTag("empty-state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
    ) {
        icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(32.dp)) }
        Text(
            title,
            modifier = Modifier.widthIn(max = 520.dp).semantics { heading() }.testTag("empty-state-title"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        WhipPageSupportingText(
            supportingText,
            modifier = Modifier.widthIn(max = 520.dp).padding(horizontal = WhipSpacing.standard)
                .testTag("empty-state-supporting-text"),
            textAlign = TextAlign.Center,
        )
        if (primaryActionLabel != null && onPrimaryAction != null) {
            WhipButton(onClick = onPrimaryAction) { Text(primaryActionLabel.uiTitleCase()) }
        }
        if (secondaryActionLabel != null && onSecondaryAction != null) {
            WhipTextButton(onClick = onSecondaryAction) { Text(secondaryActionLabel.uiTitleCase()) }
        }
    }
}

/** Shared outer presentation for collection selection; callers own actions and errors. */
@Composable
internal fun WhipSelectionActionPanel(
    selectionSummary: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = WhipSpacing.standard,
                vertical = WhipSpacing.sibling,
            ),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    selectionSummary,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                WhipTextButton(onClick = onDone) { Text("Done") }
            }
            content()
        }
    }
}

@Composable
internal fun WhipSection(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro)) {
            WhipSectionHeading(title)
            supportingText?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content()
    }
}

@Composable
internal fun WhipSectionHeading(title: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    Text(
        title.uiTitleCase(),
        modifier = modifier.semantics { heading() },
        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
}

/** Keeps authored labels intact while exposing list partitions as headings. */
@Composable
internal fun WhipGroupHeading(title: String, compact: Boolean = false) {
    Text(
        title,
        modifier = Modifier
            .then(if (compact) Modifier.padding(vertical = WhipSpacing.micro) else Modifier)
            .semantics { heading() },
        style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
        color = if (compact) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        fontWeight = if (compact) null else FontWeight.Bold,
    )
}

@Composable
internal fun SupportPaneSelectionCard(
    title: String,
    supportingText: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingMaxLines: Int = 2,
    leading: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth().selectable(selected = selected, role = Role.Tab, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(WhipSpacing.compact),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            leading?.invoke()
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = supportingMaxLines,
                )
            }
            Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null)
        }
    }
}

/** Canonical low-emphasis surface for a tappable or display-only collection item. */
@Composable
internal fun WhipCollectionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    val elevation = CardDefaults.cardElevation()
    val shape = MaterialTheme.shapes.medium
    if (onClick == null) {
        Card(modifier = modifier.fillMaxWidth(), colors = colors, elevation = elevation, shape = shape, content = content)
    } else {
        Card(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .fillMaxWidth()
                .semantics { onClickLabel?.let { contentDescription = it } },
            colors = colors,
            elevation = elevation,
            shape = shape,
            content = content,
        )
    }
}

/** A compact, consistent dashboard measurement with a predictable trailing affordance. */
@Composable
internal fun WhipMetricTile(
    label: String,
    value: String,
    onClickLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val unavailableDescription = stringResource(
        R.string.navigation_unavailable_while_editing,
        label,
        value,
    )
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .widthIn(min = 150.dp)
            .semantics {
                contentDescription = if (enabled) "$label: $value. $onClickLabel"
                else unavailableDescription
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            Modifier.padding(horizontal = WhipSpacing.compact, vertical = WhipSpacing.compact),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro)) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null)
        }
    }
}

internal enum class WhipNoticeTone { Neutral, Informative, Success, Warning, Error }

internal enum class WhipStatusKind { Loading, Status, Success, Warning, Error }

/**
 * A named, announced status for work that changes after the surrounding page is
 * already visible. The visual progress bar is decorative because the complete
 * status is exposed by the card's single live-region node.
 */
@Composable
internal fun WhipStatusCard(
    kind: WhipStatusKind,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val tone = when (kind) {
        WhipStatusKind.Loading, WhipStatusKind.Status -> WhipNoticeTone.Informative
        WhipStatusKind.Success -> WhipNoticeTone.Success
        WhipStatusKind.Warning -> WhipNoticeTone.Warning
        WhipStatusKind.Error -> WhipNoticeTone.Error
    }
    val stateLabel = when (kind) {
        WhipStatusKind.Loading -> "Loading"
        WhipStatusKind.Status -> "Status"
        WhipStatusKind.Success -> "Success"
        WhipStatusKind.Warning -> "Warning"
        WhipStatusKind.Error -> "Error"
    }
    WhipNoticeCard(
        title = title,
        message = message,
        tone = tone,
        actionLabel = actionLabel,
        onAction = onAction,
        showProgress = kind == WhipStatusKind.Loading,
        semanticStateLabel = stateLabel,
        modifier = modifier,
    )
}

/** One semantic grammar for inline dependency, partial-data, warning, and error notices. */
@Composable
internal fun WhipNoticeCard(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    tone: WhipNoticeTone = WhipNoticeTone.Neutral,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    showProgress: Boolean = false,
    semanticStateLabel: String? = null,
) {
    val (containerColor, contentColor) = when (tone) {
        WhipNoticeTone.Neutral -> MaterialTheme.colorScheme.surfaceContainerLow to MaterialTheme.colorScheme.onSurface
        WhipNoticeTone.Informative -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        WhipNoticeTone.Success -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        WhipNoticeTone.Warning -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        WhipNoticeTone.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    val effectiveStateLabel = semanticStateLabel ?: when (tone) {
        WhipNoticeTone.Neutral -> null
        WhipNoticeTone.Informative -> "Information"
        WhipNoticeTone.Success -> "Success"
        WhipNoticeTone.Warning -> "Warning"
        WhipNoticeTone.Error -> "Error"
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                effectiveStateLabel?.let {
                    liveRegion = LiveRegionMode.Polite
                    stateDescription = it
                }
                if (tone == WhipNoticeTone.Error) error(message)
            },
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = WhipSpacing.compact, vertical = WhipSpacing.sibling),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
        ) {
            title?.let {
                Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            if (showProgress) {
                LinearProgressIndicator(
                    Modifier
                        .fillMaxWidth()
                        .clearAndSetSemantics {},
                )
            }
            Text(message, style = MaterialTheme.typography.bodySmall)
            if (actionLabel != null && onAction != null) {
                WhipTextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) { Text(actionLabel) }
            }
        }
    }
}

/**
 * Canonical low-emphasis surface for ordinary grouped information and controls.
 *
 * Collection, selection, reorder, chart/calendar, notice, provenance, and
 * execution surfaces deliberately retain their own semantic components.
 */
@Composable
internal fun WhipGroupedInformationCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(WhipSpacing.standard),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
            content = content,
        )
    }
}

@Composable
internal fun WhipSettingsRow(
    title: String,
    supportingText: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) = WhipToggleRow(title, checked, onCheckedChange, supportingText = supportingText, enabled = enabled)

@Composable
internal fun WhipSettingsRow(
    title: String,
    modifier: Modifier,
    supportingText: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) = WhipToggleRow(title, checked, onCheckedChange, modifier, supportingText, enabled)

/** One whole-row switch role for preferences and authored editor choices. */
@Composable
internal fun WhipToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
) = WhipSettingItem(title, modifier, enabled) {
    description(supportingText)
    toggle(checked, onCheckedChange)
}

/**
 * A canonical single-choice row. Selection, role, label, and click handling live
 * on one parent semantics node; the radio indicator is visual-only.
 */
@Composable
internal fun WhipSingleChoiceRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
    accessibilityLabel: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .semantics(mergeDescendants = true) {
                accessibilityLabel?.let { contentDescription = it }
            }
            .heightIn(min = 48.dp)
            .padding(vertical = WhipSpacing.micro),
        horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            supportingText?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * A canonical multi-choice row. Checked state and checkbox role are exposed by
 * the complete row, leaving the control itself decorative for accessibility.
 */
@Composable
internal fun WhipMultiChoiceRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
    accessibilityLabel: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .semantics(mergeDescendants = true) {
                accessibilityLabel?.let { contentDescription = it }
            }
            .heightIn(min = 48.dp)
            .padding(vertical = WhipSpacing.micro),
        horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            supportingText?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Keeps arbitrary option sets scrollable without moving a dialog's title or actions off-screen. */
@Composable
internal fun WhipChoiceList(
    modifier: Modifier = Modifier,
    maxHeight: Dp = 200.dp,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth().heightIn(max = maxHeight * LocalDensity.current.fontScale.coerceAtLeast(1f)),
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
        content = content,
    )
}

@Composable
internal fun WhipDangerZone(
    title: String? = null,
    content: @Composable () -> Unit,
) = WhipDangerZone(Modifier, title, content)

@Composable
internal fun WhipDangerZone(
    modifier: Modifier,
    title: String? = null,
    content: @Composable () -> Unit,
) {
    val resolvedTitle = title ?: stringResource(R.string.danger_zone)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(WhipSpacing.standard),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
        ) {
            Text(
                resolvedTitle,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
            content()
        }
    }
}

/** Shared page edges, including fixed leading content and narrower browser panes. */
internal fun whipPagePadding(
    horizontal: Dp = WhipSpacing.screenCompact,
    top: Dp = WhipSpacing.compact,
    bottom: Dp = WhipSpacing.screenExpanded,
) = PaddingValues(
    start = horizontal,
    top = top,
    end = horizontal,
    bottom = bottom,
)

internal val WhipPageContentPadding = whipPagePadding()
