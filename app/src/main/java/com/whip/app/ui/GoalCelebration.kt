package com.whip.app.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whip.app.core.GoalCelebrationStyle
import com.whip.app.core.AppSettings
import com.whip.app.ui.theme.whipColors
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

internal data class GoalCelebrationEvent(
    val id: Long,
    val style: GoalCelebrationStyle,
    val goalName: String,
    val preview: Boolean = false,
)

internal class GoalCelebrationController {
    private var event by mutableStateOf<GoalCelebrationEvent?>(null)
    private var nextId = 0L

    fun complete(settings: AppSettings, goalName: String) {
        if (settings.goalCelebrationEnabled) show(settings.goalCelebrationStyle, goalName, preview = false)
    }

    fun preview(style: GoalCelebrationStyle) = show(style, "Your Goal", preview = true)

    private fun show(style: GoalCelebrationStyle, goalName: String, preview: Boolean) {
        event = GoalCelebrationEvent(++nextId, style, goalName, preview)
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

/** One presentation owner for saved Goal completions and Settings previews. */
@Composable
internal fun GoalCelebrationOverlay(event: GoalCelebrationEvent, onFinished: (Long) -> Unit) {
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val progress = remember(event.id) { Animatable(0f) }
    val duration = when (event.style) {
        GoalCelebrationStyle.QuietGlow -> 850
        GoalCelebrationStyle.ConfettiMoment -> 1_300
        GoalCelebrationStyle.VictoryShower -> 2_000
    }
    LaunchedEffect(event.id, motionEnabled) {
        if (motionEnabled) progress.animateTo(1f, tween(duration)) else delay(1_600)
        onFinished(event.id)
    }

    val palette = listOf(
        MaterialTheme.whipColors.success,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
    )
    val reveal = if (motionEnabled) min(1f, progress.value * 5f) else 1f
    val fade = if (motionEnabled) min(1f, (1f - progress.value) * 6f) else 1f
    Box(Modifier.fillMaxSize().testTag("goal-celebration-${event.style.name}")) {
        if (motionEnabled && event.style != GoalCelebrationStyle.QuietGlow) {
            ConfettiField(event.style, progress.value, palette)
        }
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .widthIn(max = 360.dp)
                .graphicsLayer {
                    alpha = min(reveal, fade)
                    scaleX = 0.9f + reveal * 0.1f
                    scaleY = scaleX
                }
                .testTag("goal-celebration-card")
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (event.preview) Text("Preview · ${event.style.label}", style = MaterialTheme.typography.labelMedium)
                CompletionBadge(event.style, progress.value, motionEnabled, palette[0])
                Text(
                    if (event.style == GoalCelebrationStyle.VictoryShower) "You did it!" else "Goal completed",
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

@Composable
private fun CompletionBadge(style: GoalCelebrationStyle, progress: Float, motionEnabled: Boolean, color: Color) {
    Box(Modifier.size(if (style == GoalCelebrationStyle.VictoryShower) 88.dp else 76.dp), contentAlignment = Alignment.Center) {
        if (style == GoalCelebrationStyle.QuietGlow && motionEnabled) Canvas(
            Modifier.fillMaxSize().clearAndSetSemantics {},
        ) {
            repeat(2) { index ->
                val phase = ((progress * 1.35f) - index * 0.35f).coerceIn(0f, 1f)
                drawCircle(
                    color = color.copy(alpha = (1f - phase) * 0.55f),
                    radius = (24 + phase * 14).dp.toPx(),
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }
        Box(
            Modifier
                .size(if (style == GoalCelebrationStyle.VictoryShower) 64.dp else 56.dp)
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
private fun ConfettiField(style: GoalCelebrationStyle, progress: Float, palette: List<Color>) {
    val fullShower = style == GoalCelebrationStyle.VictoryShower
    val pieces = remember(style) {
        val random = Random(if (fullShower) 73 else 29)
        List(if (fullShower) 92 else 36) {
            ConfettiPiece(
                x = random.nextFloat(),
                delay = random.nextFloat() * if (fullShower) 0.4f else 0.25f,
                fall = (if (fullShower) 1.45f else 0.95f) + random.nextFloat() * 0.45f,
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
