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
    val readable = 920.dp
    val dashboard = 1200.dp
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
    BoxWithConstraints(modifier.fillMaxWidth().testTag("editor-header")) {
        val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 2f)
        val stackActions = hasActions && maxWidth < 360.dp * fontScale
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = WhipSpacing.sibling, vertical = WhipSpacing.sibling),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navigationAction()
                ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                    Box(
                        Modifier
                            .weight(1f)
                            .semantics { heading() }
                            .testTag("editor-title"),
                    ) { title() }
                }
                if (!stackActions && hasActions) {
                    actions()
                }
            }
            if (stackActions) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
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

@Composable
internal fun WhipPageHeader(
    title: String,
    supportingText: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) = WhipPageHeader(title, Modifier, supportingText, actions)

@Composable
internal fun WhipPageHeader(
    title: String,
    modifier: Modifier,
    supportingText: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = WhipSpacing.sibling),
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
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 2f)
            if (maxWidth < 300.dp * fontScale) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
                ) {
                    titleContent()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) { titleContent() }
                    actions()
                }
            }
        }
        supportingText?.takeIf(String::isNotBlank)?.let { supporting ->
            Text(
                supporting,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                minLines = 2,
            )
        }
    }
}

@Composable
internal fun WhipPageIconAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    active: Boolean = false,
) {
    val contentColor = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .semantics {
                contentDescription = if (badgeCount > 0) "$label, $badgeCount active" else label
            },
    ) {
        BadgedBox(
            badge = {
                if (badgeCount > 0) Badge { Text(badgeCount.toString()) }
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
            .padding(vertical = WhipSpacing.major)
            .testTag("empty-state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
    ) {
        icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(32.dp)) }
        Text(
            title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            supportingText,
            modifier = Modifier.padding(horizontal = WhipSpacing.standard),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Text(
                title.uiTitleCase(),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            supportingText?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content()
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
    if (onClick == null) {
        Card(modifier = modifier.fillMaxWidth(), colors = colors, content = content)
    } else {
        Card(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
                .fillMaxWidth()
                .semantics { onClickLabel?.let { contentDescription = it } },
            colors = colors,
            content = content,
        )
    }
}

/** A compact, consistent dashboard metric with a predictable trailing affordance. */
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

internal enum class WhipStatusKind { Loading, Status, Success, Error }

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
        WhipStatusKind.Error -> WhipNoticeTone.Error
    }
    val stateLabel = when (kind) {
        WhipStatusKind.Loading -> "Loading"
        WhipStatusKind.Status -> "Status"
        WhipStatusKind.Success -> "Success"
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
        WhipNoticeTone.Informative -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
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
            .semantics {
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

/** Canonical grouped Settings block for explanatory or multi-control content. */
@Composable
internal fun WhipSettingsSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(WhipSpacing.compact),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
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
) = WhipSettingsRow(title, Modifier, supportingText, checked, onCheckedChange, enabled)

@Composable
internal fun WhipSettingsRow(
    title: String,
    modifier: Modifier,
    supportingText: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = WhipSpacing.sibling),
        horizontalArrangement = Arrangement.spacedBy(WhipSpacing.standard),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            supportingText?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
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
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth().heightIn(max = 200.dp),
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
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier.padding(WhipSpacing.standard),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.compact),
        ) {
            Text(resolvedTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

internal val WhipPageContentPadding = PaddingValues(
    start = WhipSpacing.screenCompact,
    top = WhipSpacing.compact,
    end = WhipSpacing.screenCompact,
    bottom = WhipSpacing.screenExpanded,
)
