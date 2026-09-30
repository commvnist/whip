package com.whip.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.FilterChip as MaterialFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.material3.FilledTonalButton as MaterialFilledTonalButton
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SelectableChipElevation
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val LocalWhipActivityActionWidth = staticCompositionLocalOf<Dp?> { null }

/** Activity controls share widths and their group's natural height; ordinary controls keep their defaults. */
@Composable
internal fun WhipActivityActions(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val scale = density.fontScale.coerceAtLeast(1f)
        val cellWidth = with(density) {
            // Round in pixels so two equal cells and their gap fit odd-width rows.
            val halfWidth = ((constraints.maxWidth - WhipSpacing.compact.roundToPx()).coerceAtLeast(0) / 2).toDp()
            val width = if (scale >= 1.5f && halfWidth < 140.dp * scale) maxWidth else halfWidth
            width.coerceAtLeast(48.dp).coerceAtMost(maxWidth)
        }
        CompositionLocalProvider(LocalWhipActivityActionWidth provides cellWidth) {
            Layout(content = content, modifier = Modifier.fillMaxWidth()) { children, constraints ->
                val width = cellWidth.roundToPx()
                val gap = WhipSpacing.compact.roundToPx()
                val rowGap = WhipSpacing.micro.roundToPx()
                val height = children.maxOfOrNull { it.minIntrinsicHeight(width) }
                    ?.coerceAtLeast(48.dp.roundToPx()) ?: 0
                val actions = children.map { it.measure(Constraints.fixed(width, height)) }
                val columns = if (width * 2 + gap <= constraints.maxWidth) 2 else 1
                val rows = (actions.size + columns - 1) / columns
                val totalHeight = if (rows == 0) 0 else rows * height + (rows - 1) * rowGap
                layout(constraints.maxWidth, constraints.constrainHeight(totalHeight)) {
                    actions.forEachIndexed { index, action ->
                        val row = index / columns
                        val rowCount = minOf(columns, actions.size - row * columns)
                        val rowWidth = rowCount * width + (rowCount - 1) * gap
                        val start = horizontalAlignment.align(rowWidth, constraints.maxWidth, LayoutDirection.Ltr)
                        action.placeRelative(start + index % columns * (width + gap), row * (height + rowGap))
                    }
                }
            }
        }
    }
}

@Composable
internal fun Modifier.whipActivityActionSize(): Modifier {
    val actionWidth = LocalWhipActivityActionWidth.current
    return (if (actionWidth == null) this else width(actionWidth)).heightIn(min = 48.dp)
}

@Composable
private fun activityActionPadding(default: PaddingValues): PaddingValues =
    if (LocalWhipActivityActionWidth.current == null) default else PaddingValues(horizontal = 12.dp, vertical = 8.dp)

/**
 * Whip's action controls use restrained rectangular corners. Material's
 * component defaults intentionally resolve to CornerFull (a pill), so every
 * app action routes through these wrappers instead of relying on that default.
 */
@Composable
internal fun WhipButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialButton(
        onClick = onClick,
        modifier = modifier.whipActivityActionSize(),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = activityActionPadding(contentPadding),
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
internal fun WhipOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialOutlinedButton(
        onClick = onClick,
        modifier = modifier.whipActivityActionSize(),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = activityActionPadding(contentPadding),
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
internal fun WhipTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialFilledTonalButton(
        onClick = onClick,
        modifier = modifier.whipActivityActionSize(),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = activityActionPadding(contentPadding),
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
internal fun WhipTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    MaterialTextButton(
        onClick = onClick,
        modifier = modifier.whipActivityActionSize(),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = activityActionPadding(contentPadding),
        interactionSource = interactionSource,
        content = content,
    )
}

/** Destructive action text follows the button's enabled state and theme. */
@Composable
internal fun WhipDestructiveTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    WhipTextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        content = content,
    )
}

/** A rectangular, single-accent choice control used for filters and modes. */
@Composable
internal fun WhipFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ),
    elevation: SelectableChipElevation = FilterChipDefaults.filterChipElevation(),
    border: BorderStroke = FilterChipDefaults.filterChipBorder(enabled, selected),
    interactionSource: MutableInteractionSource? = null,
) {
    MaterialFilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = MaterialTheme.shapes.small,
        colors = colors,
        elevation = elevation,
        border = border,
        interactionSource = interactionSource,
    )
}
