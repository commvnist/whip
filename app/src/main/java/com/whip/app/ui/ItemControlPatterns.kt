package com.whip.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
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
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.whip.app.R
import com.whip.app.ui.theme.whipColors
import kotlinx.coroutines.delay

/** User-selected collection density; unrelated to window-size or fold posture. */
internal val LocalCompactItemLayout = staticCompositionLocalOf { false }

/**
 * Completion is a semantic success state throughout Whip. Keep this separate
 * from selection checkboxes, which continue to use the active selection color.
 */
@Composable
internal fun WhipCompletionCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = CheckboxDefaults.colors(
            checkedColor = MaterialTheme.whipColors.success,
            checkmarkColor = MaterialTheme.whipColors.onSuccess,
            disabledCheckedColor = MaterialTheme.whipColors.success,
            disabledIndeterminateColor = MaterialTheme.whipColors.success,
        ),
    )
}

internal fun completionTextDecoration(completed: Boolean): TextDecoration? =
    TextDecoration.LineThrough.takeIf { completed }

@Composable
internal fun completionTextColor(completed: Boolean): Color =
    if (completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface

internal enum class WhipMenuItemRole { Normal, Destructive }

internal enum class WhipReorderAxis { Vertical, Horizontal }

/** Test-visible rendered displacement for verifying the insertion preview itself. */
internal val WhipReorderPreviewOffsetKey = SemanticsPropertyKey<Float>("WhipReorderPreviewOffset")
private var SemanticsPropertyReceiver.whipReorderPreviewOffset by WhipReorderPreviewOffsetKey

/**
 * Visual state shared by a reorder grip and the item/block it owns.
 *
 * Keeping this outside the grip is what lets direct manipulation lift and move
 * the authored object instead of animating a detached icon.
 */
@Stable
internal class WhipReorderInteractionState {
    var dragging by mutableStateOf(false)
        private set
    var offsetPx by mutableFloatStateOf(0f)
        private set
    var announcedPosition by mutableStateOf<Int?>(null)
        private set
    var totalPositions by mutableStateOf<Int?>(null)
        private set
    var previewMovement by mutableStateOf(0)
        private set
    var itemExtentPx by mutableFloatStateOf(0f)
        private set
    var movedAnnouncementPosition by mutableStateOf<Int?>(null)
        private set

    internal fun begin(position: Int?, total: Int?) {
        dragging = true
        offsetPx = 0f
        announcedPosition = position
        totalPositions = total
        previewMovement = 0
    }

    internal fun dragTo(offset: Float) {
        offsetPx = offset
    }

    internal fun updateItemExtent(extentPx: Float) {
        if (extentPx > 0f) itemExtentPx = extentPx
    }

    internal fun announceMoved(delta: Int, position: Int?, total: Int?) {
        announcedPosition = position?.plus(delta)?.coerceIn(1, total ?: Int.MAX_VALUE)
        totalPositions = total
        movedAnnouncementPosition = announcedPosition
    }

    internal fun clearMovedAnnouncement() {
        movedAnnouncementPosition = null
    }

    internal fun crossed(delta: Int, position: Int?, total: Int?) {
        previewMovement += delta
        announcedPosition = position?.plus(delta)?.coerceIn(1, total ?: Int.MAX_VALUE)
        totalPositions = total
    }

    internal fun previewTo(movement: Int, position: Int?, total: Int?) {
        previewMovement = movement
        announcedPosition = position?.plus(movement)?.coerceIn(1, total ?: Int.MAX_VALUE)
        totalPositions = total
    }

    internal fun finish() {
        dragging = false
        offsetPx = 0f
        previewMovement = 0
    }
}

@Composable
internal fun rememberWhipReorderInteractionState(): WhipReorderInteractionState =
    remember { WhipReorderInteractionState() }

/** Visual collection state for an insertion gap that never writes during drag. */
@Stable
private class WhipReorderLayoutState(private val itemSpacingPx: Float = 0f) {
    private val itemExtentsPx = mutableStateMapOf<Pair<String, Int>, Float>()
    var activeInteraction by mutableStateOf<WhipReorderInteractionState?>(null)
        private set
    var activeScope by mutableStateOf<String?>(null)
        private set
    var originPosition by mutableStateOf<Int?>(null)
        private set
    var targetPosition by mutableStateOf<Int?>(null)
        private set
    var released by mutableStateOf(false)
        private set
    var lastPosition by mutableStateOf<Int?>(null)
        private set

    fun begin(interaction: WhipReorderInteractionState, scope: String?, position: Int?, total: Int?) {
        if (scope == null || position == null || total == null) return
        activeInteraction = interaction
        activeScope = scope
        originPosition = position
        targetPosition = position
        lastPosition = total
        released = false
    }

    fun register(scope: String?, position: Int?, extentPx: Float) {
        if (scope != null && position != null && extentPx > 0f) itemExtentsPx[scope to position] = extentPx
    }

    fun movementForOffset(interaction: WhipReorderInteractionState, offsetPx: Float, total: Int?): Int? {
        if (interaction !== activeInteraction) return null
        val scope = activeScope ?: return null
        val origin = originPosition ?: return null
        val last = total ?: return null
        if (offsetPx == 0f) return 0
        val fallback = interaction.itemExtentPx.takeIf { it > 0f } ?: return null
        var distance = 0f
        var previousExtent = itemExtentsPx[scope to origin] ?: fallback
        var steps = 0
        if (offsetPx > 0f) {
            for (position in (origin + 1)..last) {
                val nextExtent = itemExtentsPx[scope to position] ?: fallback
                distance += (previousExtent + nextExtent) / 2f + itemSpacingPx
                if (offsetPx < distance) break
                steps += 1
                previousExtent = nextExtent
            }
        } else {
            for (position in (origin - 1) downTo 1) {
                val nextExtent = itemExtentsPx[scope to position] ?: fallback
                distance += (previousExtent + nextExtent) / 2f + itemSpacingPx
                if (-offsetPx < distance) break
                steps -= 1
                previousExtent = nextExtent
            }
        }
        return steps
    }

    private fun extentAt(scope: String, position: Int, fallback: Float): Float =
        itemExtentsPx[scope to position] ?: fallback

    /**
     * The furthest the owned item can visibly travel without leaving its collection.
     * This uses the crossed siblings' measured extents, rather than assuming uniform rows.
     */
    fun clampOffset(interaction: WhipReorderInteractionState, offsetPx: Float): Float? {
        if (interaction !== activeInteraction) return null
        val scope = activeScope ?: return null
        val origin = originPosition ?: return null
        val last = lastPosition ?: return null
        val fallback = interaction.itemExtentPx.takeIf { it > 0f } ?: return null
        val minimum = if (origin <= 1) 0f else {
            -(1 until origin).sumOf { position -> extentAt(scope, position, fallback).toDouble() }.toFloat() -
                itemSpacingPx * (origin - 1)
        }
        val maximum = if (origin >= last) 0f else {
            ((origin + 1)..last).sumOf { position -> extentAt(scope, position, fallback).toDouble() }.toFloat() +
                itemSpacingPx * (last - origin)
        }
        return offsetPx.coerceIn(minimum, maximum)
    }

    private fun destinationOffset(interaction: WhipReorderInteractionState, target: Int): Float {
        val scope = activeScope ?: return 0f
        val origin = originPosition ?: return 0f
        val fallback = interaction.itemExtentPx.takeIf { it > 0f } ?: return 0f
        return when {
            target > origin -> ((origin + 1)..target)
                .sumOf { position -> extentAt(scope, position, fallback).toDouble() }.toFloat() +
                itemSpacingPx * (target - origin)
            target < origin -> -(target until origin)
                .sumOf { position -> extentAt(scope, position, fallback).toDouble() }.toFloat() -
                itemSpacingPx * (origin - target)
            else -> 0f
        }
    }

    fun preview(interaction: WhipReorderInteractionState, movement: Int, total: Int?) {
        if (interaction !== activeInteraction) return
        val origin = originPosition ?: return
        targetPosition = origin.plus(movement).coerceIn(1, total ?: Int.MAX_VALUE)
    }

    fun release(interaction: WhipReorderInteractionState) {
        if (interaction === activeInteraction) released = true
    }

    fun clear(interaction: WhipReorderInteractionState) {
        if (interaction !== activeInteraction) return
        activeInteraction = null
        activeScope = null
        originPosition = null
        targetPosition = null
        lastPosition = null
        released = false
    }

    fun offsetFor(interaction: WhipReorderInteractionState, scope: String?, position: Int?): Float {
        val active = activeInteraction ?: return 0f
        if (scope == null || scope != activeScope || position == null) return 0f
        val origin = originPosition ?: return 0f
        val target = targetPosition ?: origin
        val extent = active.itemExtentPx
        if (interaction === active) {
            return if (released) {
                if (position == target) 0f else destinationOffset(active, target)
            } else {
                clampOffset(active, active.offsetPx) ?: active.offsetPx
            }
        }
        return when {
            origin < target && position in (origin + 1)..target -> -extent - itemSpacingPx
            origin > target && position in target until origin -> extent + itemSpacingPx
            else -> 0f
        }
    }
}

private val LocalWhipReorderLayoutState = staticCompositionLocalOf<WhipReorderLayoutState?> { null }

/** Shares measured insertion geometry across a non-lazy authored collection. */
@Composable
internal fun WhipReorderLayout(itemSpacing: Dp = 0.dp, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val itemSpacingPx = with(density) { itemSpacing.toPx() }
    val reorderLayoutState = remember(itemSpacingPx) { WhipReorderLayoutState(itemSpacingPx) }
    CompositionLocalProvider(LocalWhipReorderLayoutState provides reorderLayoutState, content = content)
}

/** Applies the drag layer and insertion preview to the actual owned item. */
@Composable
internal fun Modifier.whipReorderItem(
    state: WhipReorderInteractionState,
    axis: WhipReorderAxis = WhipReorderAxis.Vertical,
    layoutPosition: Int? = null,
    layoutScope: String? = null,
): Modifier {
    val layoutState = LocalWhipReorderLayoutState.current
    val targetOffset = layoutState?.offsetFor(state, layoutScope, layoutPosition)
        ?: if (state.dragging) state.offsetPx else 0f
    val animatedOffset by animateFloatAsState(targetValue = targetOffset, label = "reorder-item-offset")
    // The owned item and the gap it opens must respond as one direct-manipulation
    // gesture. Animating displaced siblings during the drag makes the insertion
    // target lag behind the pointer, especially for mixed-height rows. Keep the
    // live preview synchronous and reserve animation for settling after release.
    val livePreview = layoutState?.activeInteraction?.dragging == true
    val renderedOffset = if (state.dragging || livePreview) targetOffset else animatedOffset
    val released = layoutState?.released == true && layoutState.activeInteraction === state
    LaunchedEffect(layoutState, state, layoutPosition, released) {
        if (!released) return@LaunchedEffect
        val settled = layoutPosition != null && layoutPosition == layoutState.targetPosition
        delay(if (settled) 80L else 750L)
        layoutState.clear(state)
    }
    return this
        .onGloballyPositioned { coordinates ->
            val extent = if (axis == WhipReorderAxis.Vertical) coordinates.size.height.toFloat()
            else coordinates.size.width.toFloat()
            state.updateItemExtent(extent)
            layoutState?.register(layoutScope, layoutPosition, extent)
        }
        .zIndex(if (state.dragging || released) 1f else 0f)
        .graphicsLayer {
            if (axis == WhipReorderAxis.Vertical) translationY = renderedOffset else translationX = renderedOffset
            val lift = if (state.dragging) 1.015f else 1f
            scaleX = lift
            scaleY = lift
            shadowElevation = if (state.dragging) 10.dp.toPx() else 0f
        }
        .semantics { whipReorderPreviewOffset = renderedOffset }
}

internal fun <T> moveListItem(items: List<T>, fromIndex: Int, delta: Int): List<T> {
    if (fromIndex !in items.indices || delta == 0) return items
    val target = (fromIndex + delta).coerceIn(0, items.lastIndex)
    if (target == fromIndex) return items
    return items.toMutableList().also { reordered ->
        reordered.add(target, reordered.removeAt(fromIndex))
    }
}

/**
 * Geometry shared by a lazy collection and the reorder grip currently being
 * dragged inside it. Holding a grip at an edge moves one adjacent item at a
 * time while the collection scrolls, so long collections do not become a
 * dead end at the viewport boundary.
 */
@Stable
internal class WhipReorderAutoScrollState internal constructor(
    internal val axis: WhipReorderAxis,
    private val scrollByStep: suspend (Float) -> Float,
    private val dispatchRawStep: (Float) -> Float,
) {
    internal constructor(listState: LazyListState) : this(
        axis = WhipReorderAxis.Vertical,
        scrollByStep = listState::scrollBy,
        dispatchRawStep = listState::dispatchRawDelta,
    )

    internal constructor(scrollState: ScrollState) : this(
        axis = WhipReorderAxis.Horizontal,
        scrollByStep = scrollState::scrollBy,
        dispatchRawStep = scrollState::dispatchRawDelta,
    )

    private var viewportStartPx by mutableFloatStateOf(Float.NaN)
    private var viewportEndPx by mutableFloatStateOf(Float.NaN)
    private var dragStartPx by mutableFloatStateOf(Float.NaN)
    private var pointerPx by mutableFloatStateOf(Float.NaN)

    internal fun updateViewport(startPx: Float, endPx: Float) {
        viewportStartPx = startPx
        viewportEndPx = endPx
    }

    internal fun begin(pointerInWindowPx: Float) {
        dragStartPx = pointerInWindowPx
        pointerPx = pointerInWindowPx
    }

    internal fun updatePointer(pointerInWindowPx: Float) {
        pointerPx = pointerInWindowPx
    }

    internal fun finish() {
        dragStartPx = Float.NaN
        pointerPx = Float.NaN
    }

    internal fun requestedDirection(edgeThresholdPx: Float): Int {
        if (
            viewportStartPx.isNaN() || viewportEndPx.isNaN() ||
            dragStartPx.isNaN() || pointerPx.isNaN()
        ) return 0
        return when {
            pointerPx <= viewportStartPx + edgeThresholdPx -> -1
            pointerPx >= viewportEndPx - edgeThresholdPx -> 1
            else -> 0
        }
    }

    internal suspend fun scrollBy(delta: Float): Float = scrollByStep(delta)

    internal fun dispatchRawDelta(delta: Float): Float = dispatchRawStep(delta)
}

private val LocalWhipReorderAutoScrollState =
    staticCompositionLocalOf<WhipReorderAutoScrollState?> { null }

/** A LazyColumn with the shared edge-autoscroll contract enabled for nested grips. */
@Composable
internal fun WhipReorderLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    val autoScrollState = remember(state) { WhipReorderAutoScrollState(state) }
    val density = LocalDensity.current
    val itemSpacingPx = with(density) { verticalArrangement.spacing.toPx() }
    val reorderLayoutState = remember(itemSpacingPx) { WhipReorderLayoutState(itemSpacingPx) }
    CompositionLocalProvider(
        LocalWhipReorderAutoScrollState provides autoScrollState,
        LocalWhipReorderLayoutState provides reorderLayoutState,
    ) {
        LazyColumn(
            modifier = modifier.onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                autoScrollState.updateViewport(bounds.top, bounds.bottom)
            },
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            userScrollEnabled = userScrollEnabled,
            content = content,
        )
    }
}

/** A horizontally scrolling authored collection with the same edge-scroll and insertion contract. */
@Composable
internal fun WhipReorderHorizontalRow(
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit,
) {
    val autoScrollState = remember(state) { WhipReorderAutoScrollState(state) }
    val density = LocalDensity.current
    val itemSpacingPx = with(density) { horizontalArrangement.spacing.toPx() }
    val reorderLayoutState = remember(itemSpacingPx) { WhipReorderLayoutState(itemSpacingPx) }
    CompositionLocalProvider(
        LocalWhipReorderAutoScrollState provides autoScrollState,
        LocalWhipReorderLayoutState provides reorderLayoutState,
    ) {
        Row(
            modifier = modifier
                .horizontalScroll(state)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    autoScrollState.updateViewport(bounds.left, bounds.right)
                },
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            content = content,
        )
    }
}

/**
 * The one direct-manipulation affordance for authored order throughout Whip.
 * Dragging the dedicated handle crosses adjacent positions; accessibility actions and the
 * existing overflow commands provide equivalent non-gesture movement.
 */
@Composable
internal fun WhipReorderHandle(
    label: String,
    canMovePrevious: Boolean,
    canMoveNext: Boolean,
    onMove: (Int) -> Unit,
    modifier: Modifier = Modifier,
    axis: WhipReorderAxis = WhipReorderAxis.Vertical,
    position: Int? = null,
    total: Int? = null,
    interactionState: WhipReorderInteractionState? = null,
    moveWholeItem: Boolean = false,
    layoutScope: String? = null,
    reserveWhenUnavailable: Boolean = false,
    onPreviewMove: ((Int) -> Unit)? = null,
    onCommitPreview: (() -> Unit)? = null,
    onCancelPreview: (() -> Unit)? = null,
) {
    val localInteraction = rememberWhipReorderInteractionState()
    val interaction = interactionState ?: localInteraction
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    var autoMovement by remember { mutableIntStateOf(0) }
    var lastPreviewSteps by remember { mutableIntStateOf(0) }
    var lastEdgeDirection by remember { mutableIntStateOf(0) }
    var edgeEntryNeedsCooldown by remember { mutableStateOf(false) }
    val thresholdPx = with(LocalDensity.current) { 48.dp.toPx() }
    val haptics = LocalHapticFeedback.current
    val autoScroll = LocalWhipReorderAutoScrollState.current?.takeIf { it.axis == axis }
    val reorderLayout = LocalWhipReorderLayoutState.current
    var handleStartInWindowPx by remember { mutableFloatStateOf(0f) }
    val edgeThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }
    val edgeScrollStepPx = with(LocalDensity.current) { 56.dp.toPx() }
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnPreviewMove by rememberUpdatedState(onPreviewMove)
    val currentOnCommitPreview by rememberUpdatedState(onCommitPreview)
    val currentOnCancelPreview by rememberUpdatedState(onCancelPreview)
    val currentCanMovePrevious by rememberUpdatedState(canMovePrevious)
    val currentCanMoveNext by rememberUpdatedState(canMoveNext)
    val currentPosition by rememberUpdatedState(position)
    val currentTotal by rememberUpdatedState(total)
    val previousDirection = if (axis == WhipReorderAxis.Vertical) "up" else "earlier"
    val nextDirection = if (axis == WhipReorderAxis.Vertical) "down" else "later"
    val enabled = canMovePrevious || canMoveNext
    if (!enabled && !reserveWhenUnavailable) return
    val availableDirections = listOfNotNull(
        previousDirection.takeIf { canMovePrevious },
        nextDirection.takeIf { canMoveNext },
    ).joinToString(" or ")
    LaunchedEffect(interaction.movedAnnouncementPosition) {
        if (interaction.movedAnnouncementPosition != null) {
            delay(1_500L)
            interaction.clearMovedAnnouncement()
        }
    }
    fun movementThresholdPx(): Float = interaction.itemExtentPx
        .takeIf { moveWholeItem && it > 0f }
        ?: thresholdPx
    fun directMovement(): Int = reorderLayout?.movementForOffset(interaction, accumulatedDrag, currentTotal)
        ?: (accumulatedDrag / movementThresholdPx()).toInt()
    fun directionAvailable(direction: Int): Boolean {
        val previewPosition = if (interaction.dragging) {
            reorderLayout?.targetPosition ?: interaction.announcedPosition ?: currentPosition
        } else currentPosition
        val previewTotal = currentTotal
        return if (interaction.dragging && previewPosition != null && previewTotal != null) {
            when (direction) {
                -1 -> previewPosition > 1
                1 -> previewPosition < previewTotal
                else -> false
            }
        } else {
            when (direction) {
                -1 -> currentCanMovePrevious
                1 -> currentCanMoveNext
                else -> false
            }
        }
    }
    LaunchedEffect(interaction.dragging, autoScroll) {
        val scrollState = autoScroll ?: return@LaunchedEffect
        var lastStepNanos = Long.MIN_VALUE
        while (interaction.dragging) {
            val frameNanos = withFrameNanos { it }
            if (edgeEntryNeedsCooldown) {
                lastStepNanos = frameNanos
                edgeEntryNeedsCooldown = false
            }
            val direction = scrollState.requestedDirection(edgeThresholdPx)
            val directionAllowed = directionAvailable(direction)
            val stepReady = lastStepNanos == Long.MIN_VALUE || frameNanos - lastStepNanos >= 150_000_000L
            if (directionAllowed && stepReady && (moveWholeItem || currentOnPreviewMove != null)) {
                if (currentOnPreviewMove != null) {
                    currentOnPreviewMove?.invoke(direction)
                    interaction.crossed(direction, currentPosition, currentTotal)
                } else {
                    autoMovement += direction
                    lastPreviewSteps = directMovement() + autoMovement
                    interaction.previewTo(lastPreviewSteps, currentPosition, currentTotal)
                }
                scrollState.scrollBy(direction * edgeScrollStepPx)
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                lastStepNanos = frameNanos
            }
        }
    }
    Surface(
        modifier = modifier
            .size(48.dp)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                handleStartInWindowPx = if (axis == WhipReorderAxis.Vertical) bounds.top else bounds.left
            }
            // Own short taps so a handle nested inside an open/edit/check row
            // never activates that parent action.
            .pointerInput(Unit) { detectTapGestures(onTap = {}) }
            .onPreviewKeyEvent { event ->
                if (!enabled) return@onPreviewKeyEvent false
                if (event.type != KeyEventType.KeyDown || !event.isAltPressed) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp, Key.DirectionLeft -> if (canMovePrevious) {
                        interaction.announceMoved(-1, currentPosition, currentTotal)
                        currentOnMove(-1)
                        true
                    } else false
                    Key.DirectionDown, Key.DirectionRight -> if (canMoveNext) {
                        interaction.announceMoved(1, currentPosition, currentTotal)
                        currentOnMove(1)
                        true
                    } else false
                    else -> false
                }
            }
            .focusable(enabled)
            .graphicsLayer {
                if (!moveWholeItem) {
                    if (axis == WhipReorderAxis.Vertical) translationY = interaction.offsetPx
                    else translationX = interaction.offsetPx
                }
            }
            .pointerInput(axis, enabled, moveWholeItem, layoutScope) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { start ->
                        accumulatedDrag = 0f
                        autoMovement = 0
                        lastPreviewSteps = 0
                        lastEdgeDirection = 0
                        edgeEntryNeedsCooldown = false
                        interaction.begin(currentPosition, currentTotal)
                        reorderLayout?.begin(interaction, layoutScope, currentPosition, currentTotal)
                        autoScroll?.begin(handleStartInWindowPx + if (axis == WhipReorderAxis.Vertical) start.y else start.x)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragCancel = {
                        accumulatedDrag = 0f
                        if (interaction.previewMovement != 0) {
                            currentOnCancelPreview?.invoke()
                        }
                        autoMovement = 0
                        lastPreviewSteps = 0
                        lastEdgeDirection = 0
                        edgeEntryNeedsCooldown = false
                        autoScroll?.finish()
                        reorderLayout?.clear(interaction)
                        interaction.finish()
                    },
                    onDragEnd = {
                        if (currentOnPreviewMove != null) {
                            if (interaction.previewMovement != 0) {
                                currentOnCommitPreview?.invoke()
                            }
                        } else {
                            val requestedSteps = directMovement() + autoMovement
                            val minimum = currentPosition?.let { 1 - it } ?: Int.MIN_VALUE
                            val maximum = if (currentPosition != null && currentTotal != null) {
                                currentTotal!! - currentPosition!!
                            } else Int.MAX_VALUE
                            val allowedSteps = requestedSteps.coerceIn(minimum, maximum).let { movement ->
                                when {
                                    movement < 0 && currentCanMovePrevious -> movement
                                    movement > 0 && currentCanMoveNext -> movement
                                    else -> 0
                                }
                            }
                            if (allowedSteps != 0) {
                                reorderLayout?.preview(interaction, allowedSteps, currentTotal)
                                reorderLayout?.release(interaction)
                                currentOnMove(allowedSteps)
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else {
                                reorderLayout?.clear(interaction)
                            }
                        }
                        accumulatedDrag = 0f
                        autoMovement = 0
                        lastPreviewSteps = 0
                        lastEdgeDirection = 0
                        edgeEntryNeedsCooldown = false
                        autoScroll?.finish()
                        interaction.finish()
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        autoScroll?.updatePointer(
                            handleStartInWindowPx + if (axis == WhipReorderAxis.Vertical) change.position.y else change.position.x,
                        )
                        accumulatedDrag += if (axis == WhipReorderAxis.Vertical) amount.y else amount.x
                        if (moveWholeItem && currentOnPreviewMove == null && autoScroll != null) {
                            val edgeDirection = autoScroll.requestedDirection(edgeThresholdPx)
                            val edgeAllowed = directionAvailable(edgeDirection)
                            if (edgeAllowed && edgeDirection != lastEdgeDirection) {
                                autoMovement += edgeDirection
                                autoScroll.dispatchRawDelta(edgeDirection * edgeScrollStepPx)
                                val previewSteps = directMovement() + autoMovement
                                interaction.previewTo(previewSteps, currentPosition, currentTotal)
                                reorderLayout?.preview(interaction, previewSteps, currentTotal)
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                edgeEntryNeedsCooldown = true
                            }
                            lastEdgeDirection = if (edgeAllowed) edgeDirection else 0
                        }
                        val preview = currentOnPreviewMove
                        if (preview != null) {
                            val previewThreshold = movementThresholdPx()
                            val requestedSteps = (accumulatedDrag / previewThreshold).toInt()
                            val allowedSteps = when {
                                requestedSteps < 0 && currentCanMovePrevious -> requestedSteps
                                requestedSteps > 0 && currentCanMoveNext -> requestedSteps
                                else -> 0
                            }
                            if (allowedSteps != 0) {
                                preview(allowedSteps)
                                accumulatedDrag -= allowedSteps * previewThreshold
                                interaction.crossed(allowedSteps, currentPosition, currentTotal)
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        } else if (moveWholeItem) {
                            val previewSteps = directMovement() + autoMovement
                            if (previewSteps != lastPreviewSteps) {
                                lastPreviewSteps = previewSteps
                                interaction.previewTo(previewSteps, currentPosition, currentTotal)
                                reorderLayout?.preview(interaction, previewSteps, currentTotal)
                                if (previewSteps != 0) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                        val directionAllowed = when {
                            accumulatedDrag < 0f -> currentCanMovePrevious
                            accumulatedDrag > 0f -> currentCanMoveNext
                            else -> true
                        }
                        val visible = if (directionAllowed || moveWholeItem) {
                            if (moveWholeItem) {
                                reorderLayout?.clampOffset(interaction, accumulatedDrag)
                                    ?: accumulatedDrag.coerceIn(
                                        ((currentPosition ?: 1) - 1) * -movementThresholdPx(),
                                        ((currentTotal ?: currentPosition ?: 1) - (currentPosition ?: 1)) * movementThresholdPx(),
                                    )
                            } else accumulatedDrag.coerceIn(-thresholdPx, thresholdPx) * 0.82f
                        } else 0f
                        interaction.dragTo(visible)
                    },
                )
            }
            .clearAndSetSemantics {
                contentDescription = "Reorder $label"
                stateDescription = buildString {
                    val describedPosition = if (interaction.dragging || interaction.movedAnnouncementPosition != null) {
                        interaction.announcedPosition
                    } else position
                    if (describedPosition != null && total != null) append("Position $describedPosition of $total. ")
                    append(
                        when {
                            interaction.dragging -> "Moving. Release to place"
                            interaction.movedAnnouncementPosition != null -> "Moved to position ${interaction.movedAnnouncementPosition}"
                            enabled -> "Drag $availableDirections"
                            else -> "Only item in this section"
                        },
                    )
                }
                if (interaction.dragging || interaction.movedAnnouncementPosition != null) liveRegion = LiveRegionMode.Polite
                customActions = buildList {
                    if (canMovePrevious) add(
                        CustomAccessibilityAction("Move $label $previousDirection") {
                            interaction.announceMoved(-1, currentPosition, currentTotal)
                            currentOnMove(-1)
                            true
                        },
                    )
                    if (canMoveNext) add(
                        CustomAccessibilityAction("Move $label $nextDirection") {
                            interaction.announceMoved(1, currentPosition, currentTotal)
                            currentOnMove(1)
                            true
                        },
                    )
                }
            },
        shape = MaterialTheme.shapes.small,
        color = when {
            interaction.dragging -> MaterialTheme.colorScheme.primaryContainer
            enabled -> Color.Transparent
            else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.38f)
        },
        shadowElevation = if (interaction.dragging && !moveWholeItem) 4.dp else 0.dp,
    ) {
        Icon(
            imageVector = Icons.Outlined.DragHandle,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .padding(10.dp)
                .graphicsLayer { rotationZ = if (axis == WhipReorderAxis.Horizontal) 90f else 0f },
        )
    }
}

/** Low-frequency commands use one menu grammar; irreversible commands are red and remain last. */
@Composable
internal fun WhipMenuItem(
    label: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    selected: Boolean? = null,
    role: WhipMenuItemRole = WhipMenuItemRole.Normal,
    modifier: Modifier = Modifier,
) {
    val destructive = role == WhipMenuItemRole.Destructive
    DropdownMenuItem(
        modifier = selected?.let { isSelected ->
            modifier.semantics { this.selected = isSelected }
        } ?: modifier,
        text = { Text(label) },
        onClick = onClick,
        enabled = enabled,
        leadingIcon = icon?.let { image -> { Icon(image, contentDescription = null) } },
        trailingIcon = if (selected == true) {{ Icon(Icons.Outlined.Check, contentDescription = null) }} else null,
        colors = if (destructive) {
            MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.error,
                leadingIconColor = MaterialTheme.colorScheme.error,
            )
        } else MenuDefaults.itemColors(),
    )
}

/**
 * Canonical anchor for contextual destinations and low-frequency commands.
 * The vertical ellipsis is intentionally distinct from tabs, disclosures, and
 * pagination, while retaining a full-size one-handed target.
 */
@Composable
internal fun WhipOverflowMenu(
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        ) {
            IconButton(
                onClick = { onExpandedChange(true) },
                enabled = enabled,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = label
                        stateDescription = when {
                            expanded -> "Menu open"
                            active -> "Selected page is in this menu"
                            else -> "Menu closed"
                        }
                    },
            ) {
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = null,
                    tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            content = content,
        )
    }
}

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
    titleCompleted: Boolean = false,
    headlineAccessory: (@Composable RowScope.() -> Unit)? = null,
    supportingContent: @Composable ColumnScope.() -> Unit = {},
    compactSummaryContent: @Composable ColumnScope.() -> Unit = {},
    compactExpanded: Boolean = false,
    onCompactExpansionToggle: (() -> Unit)? = null,
    compactExpansionTag: String? = null,
    compactPrimaryActionWidth: Dp = 64.dp,
    primaryActionWidth: Dp = 72.dp,
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
                        color = completionTextColor(titleCompleted),
                        textDecoration = completionTextDecoration(titleCompleted),
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
                    color = completionTextColor(titleCompleted),
                    textDecoration = completionTextDecoration(titleCompleted),
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
                    modifier = primaryActionModifier.width(primaryActionWidth).heightIn(min = 48.dp),
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

/**
 * A stable, single-row destination control for peer pages.
 *
 * Destinations are navigation, never commands, so every peer remains visible
 * and keeps its position. Low-frequency actions belong to contextual menus at
 * the owning screen or item instead of a kebab inside navigation.
 */
@Composable
internal fun <T> DestinationTabBar(
    selected: T,
    destinations: List<T>,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    compactLabel: (T) -> String = label,
    testTagPrefix: String? = null,
    testTagValue: (T) -> String = label,
    secondaryTestTagPrefix: String? = null,
    secondaryTestTagValue: (T) -> String = label,
    barTestTag: String? = null,
    resetCompactItemExpansionOnChange: Boolean = true,
) {
    if (destinations.isEmpty()) return
    val compactItemExpansionState = LocalCompactItemExpansionState.current
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f).selectableGroup()) {
                destinations.forEach { destination ->
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
                                .then(
                                    testTagPrefix?.let {
                                        Modifier.testTag("$it-${testTagValue(destination)}")
                                    } ?: Modifier,
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier
                                    .height(45.dp)
                                    .padding(horizontal = 2.dp)
                                    .then(
                                        secondaryTestTagPrefix?.let {
                                            Modifier.testTag("$it-${secondaryTestTagValue(destination)}")
                                        } ?: Modifier,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
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
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(modifier) {
        val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 1.5f)
        // Keep peer choices visible whenever the available row can actually
        // accommodate them. Large text alone is not a reason to hide controls.
        val useMenu = maxWidth < 96.dp * choices.size * fontScale
        if (useMenu) {
            Box(Modifier.fillMaxWidth()) {
                WhipOutlinedButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = "Choose view. Selected ${label(selected)}" }
                        .then(testTagPrefix?.let { Modifier.testTag("$it-${label(selected)}") } ?: Modifier),
                ) {
                    Text(label(selected).uiTitleCase(), modifier = Modifier.weight(1f), maxLines = 2)
                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    choices.forEach { choice ->
                        DropdownMenuItem(
                            text = { Text(label(choice).uiTitleCase()) },
                            leadingIcon = if (choice == selected) {{ Icon(Icons.Outlined.Check, contentDescription = "Selected") }} else null,
                            modifier = testTagPrefix?.let { Modifier.testTag("$it-${label(choice)}") } ?: Modifier,
                            onClick = {
                                if (resetCompactItemExpansionOnChange && choice != selected) compactItemExpansionState?.collapseAll()
                                menuExpanded = false
                                onSelect(choice)
                            },
                        )
                    }
                }
            }
        } else SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
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
            Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null)
        }
    }
}

/** A visually grouped list of related navigation or low-frequency commands. */
@Composable
internal fun WhipActionList(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(content = content)
    }
}

@Composable
internal fun WhipActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    navigates: Boolean = true,
    danger: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClickLabel = title, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title.uiTitleCase(), color = if (danger) MaterialTheme.colorScheme.error else Color.Unspecified)
            supportingText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (navigates) Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null)
    }
}

@Composable
internal fun WhipActionDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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

/** One visible, explanatory grammar for temporary collection reordering. */
@Composable
internal fun WhipReorderModeBar(
    itemLabel: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    boundaryNote: String? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag("reorder-mode-${itemLabel.lowercase().replace(' ', '-') }"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.DragHandle, contentDescription = null, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Reordering $itemLabel", fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull("Drag a handle, then release to place and save it.", boundaryNote).joinToString(" "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            WhipTextButton(
                onClick = onDone,
                modifier = Modifier.heightIn(min = 48.dp).testTag("reorder-mode-done"),
            ) { Text("Done") }
        }
    }
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
