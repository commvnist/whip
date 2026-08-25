package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
                    modifier = Modifier.fillMaxWidth(),
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
                    Text(if (filterCount > 0) "Filter · $filterCount" else "Filter")
                }
            } else {
                WhipPageIconAction(
                    icon = Icons.Outlined.FilterAlt,
                    label = "Filter",
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
                        contentDescription = "Remove ${filter.label}",
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
        WhipTextButton(onClick = onClearAll) { Text("Clear All") }
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
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
internal fun WhipDangerZone(
    title: String = "Danger Zone",
    content: @Composable () -> Unit,
) = WhipDangerZone(Modifier, title, content)

@Composable
internal fun WhipDangerZone(
    modifier: Modifier,
    title: String = "Danger Zone",
    content: @Composable () -> Unit,
) {
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
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

internal val WhipPageContentPadding = PaddingValues(
    start = WhipSpacing.screenCompact,
    top = WhipSpacing.compact,
    end = WhipSpacing.screenCompact,
    bottom = 112.dp,
)
