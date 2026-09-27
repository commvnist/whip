package com.whip.app.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whip.app.core.AppSettings
import com.whip.app.ui.theme.whipColors
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

internal data class GoalCelebrationEvent(
    val id: Long,
    val goalName: String,
)

internal class GoalCelebrationController {
    private var event by mutableStateOf<GoalCelebrationEvent?>(null)
    private var nextId = 0L

    fun complete(settings: AppSettings, goalName: String) {
        if (settings.goalCelebrationEnabled) event = GoalCelebrationEvent(++nextId, goalName)
    }

    @Composable
    fun Overlay() {
        val current = event ?: return
        GoalCelebrationOverlay(current) { finishedId ->
            if (event?.id == finishedId) event = null
        }
    }
}

internal val LocalGoalCelebration = staticCompositionLocalOf<GoalCelebrationController?> { null }

@Composable
internal fun GoalCelebrationHost(content: @Composable () -> Unit) {
    val controller = remember { GoalCelebrationController() }
    CompositionLocalProvider(LocalGoalCelebration provides controller) {
        Box(Modifier.fillMaxSize()) {
            content()
            controller.Overlay()
        }
    }
}

/** One presentation owner for saved Goal completions. */
@Composable
internal fun GoalCelebrationOverlay(event: GoalCelebrationEvent, onFinished: (Long) -> Unit) {
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val progress = remember(event.id) { Animatable(0f) }
    var cardVisible by remember(event.id) { mutableStateOf(true) }
    LaunchedEffect(event.id, motionEnabled) {
        if (motionEnabled) progress.animateTo(1f, tween(4_000, easing = LinearEasing)) else delay(4_000)
        onFinished(event.id)
    }

    val palette = listOf(
        MaterialTheme.whipColors.success,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
    )
    val visibility = if (motionEnabled) celebrationCardVisibility(progress.value) else 1f
    val dismissOnTap = if (cardVisible) Modifier.pointerInput(event.id) {
        detectTapGestures { cardVisible = false }
    } else Modifier
    Box(Modifier.fillMaxSize().testTag("goal-celebration").then(dismissOnTap)) {
        if (motionEnabled) ConfettiField(progress.value, palette)
        if (cardVisible) Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .widthIn(max = 360.dp)
                .graphicsLayer {
                    alpha = visibility
                    scaleX = 0.9f + visibility * 0.1f
                    scaleY = scaleX
                }
                .testTag("goal-celebration-card")
                .semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                    onClick(label = "Dismiss completion message") { cardVisible = false; true }
                },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompletionBadge()
                Text(
                    "You did it!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    event.goalName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

internal fun celebrationCardVisibility(progress: Float): Float = min(1f, min(progress, 1f - progress) * 5f)

@Composable
private fun CompletionBadge() {
    Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

private data class ConfettiPiece(
    val x: Float,
    val delay: Float,
    val fall: Float,
    val drift: Float,
    val size: Float,
    val color: Int,
    val round: Boolean,
)

@Composable
private fun ConfettiField(progress: Float, palette: List<Color>) {
    val pieces = remember {
        val random = Random(73)
        List(92) {
            ConfettiPiece(
                x = random.nextFloat(),
                delay = random.nextFloat() * 0.6f,
                fall = 1.1f + random.nextFloat() * 0.45f,
                drift = (random.nextFloat() - 0.5f) * 0.3f,
                size = 4f + random.nextFloat() * 5f,
                color = random.nextInt(palette.size),
                round = random.nextBoolean(),
            )
        }
    }
    Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
        pieces.forEach { piece ->
            val t = ((progress - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
            if (t <= 0f || t >= 1f) return@forEach
            val sway = sin((t * 8f + piece.x * 4f).toDouble()).toFloat() * 0.025f
            val x = (piece.x + piece.drift * t + sway) * size.width
            val y = (-0.12f - piece.delay * 0.8f + piece.fall * t) * size.height
            val color = palette[piece.color].copy(alpha = min(1f, (1f - t) * 4f))
            val width = piece.size.dp.toPx()
            if (piece.round) {
                drawCircle(color, radius = width * 0.58f, center = androidx.compose.ui.geometry.Offset(x, y))
            } else {
                drawRect(color, topLeft = androidx.compose.ui.geometry.Offset(x, y), size = androidx.compose.ui.geometry.Size(width, width * 1.7f))
            }
        }
    }
}
