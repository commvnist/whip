package com.whip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Values are already formatted domain evidence. This builder only owns their presentation. */
internal class WhipSummaryScope internal constructor() {
    internal val metrics = mutableListOf<Pair<String, String>>()
    internal val facts = mutableListOf<Pair<String, String>>()

    fun metric(label: String, value: String) { metrics += label to value }
    fun fact(label: String, value: String) { facts += label to value }
}

@Composable
internal fun WhipSummaryCard(
    title: String,
    modifier: Modifier = Modifier,
    content: WhipSummaryScope.() -> Unit,
) {
    val summary = WhipSummaryScope().apply(content)
    WhipGroupedInformationCard(modifier) {
        Text(title, Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (summary.metrics.isNotEmpty()) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = ((maxWidth + WhipSpacing.standard) /
                    (144.dp * LocalDensity.current.fontScale + WhipSpacing.standard)).toInt()
                    .coerceIn(1, minOf(2, summary.metrics.size))
                Column(verticalArrangement = Arrangement.spacedBy(WhipSpacing.standard)) {
                    summary.metrics.chunked(columns).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(WhipSpacing.standard)) {
                            row.forEach { (label, value) ->
                                Column(Modifier.weight(1f).semantics(mergeDescendants = true) {},
                                    verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro)) {
                                    Text(label, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(value, Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        if (summary.facts.isNotEmpty()) {
            // Keep related labels and readings together even inside an overview-width surface.
            Column(Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling)) {
                summary.facts.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                        horizontalArrangement = Arrangement.spacedBy(WhipSpacing.standard)) {
                        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value, Modifier.widthIn(max = 240.dp).weight(1f, fill = false),
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

internal data class WhipSummaryPoint(
    val label: String,
    val fullLabel: String,
    val value: String,
    val magnitude: Double,
)

/** A short ordered series: every bar keeps its exact reading and full accessible label. */
@Composable
internal fun WhipSummarySeries(
    title: String,
    supportingText: String,
    points: List<WhipSummaryPoint>,
    modifier: Modifier = Modifier,
) {
    WhipGroupedInformationCard(modifier) {
        Text(title, Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(supportingText, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = ((maxWidth + WhipSpacing.sibling) /
                (32.dp * LocalDensity.current.fontScale + WhipSpacing.sibling)).toInt()
                .coerceIn(1, maxOf(1, points.size))
            val maximum = points.maxOfOrNull { it.magnitude.takeIf(Double::isFinite) ?: 0.0 }
                ?.coerceAtLeast(1.0) ?: 1.0
            Column(verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling)) {
                points.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling)) {
                        row.forEach { point ->
                            Column(Modifier.weight(1f).clearAndSetSemantics {
                                contentDescription = "${point.fullLabel}: ${point.value}"
                            }, horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro)) {
                                Text(point.value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.BottomCenter) {
                                    val fraction = (point.magnitude / maximum).takeIf(Double::isFinite)?.coerceIn(0.0, 1.0) ?: 0.0
                                    Box(Modifier.widthIn(max = 32.dp).fillMaxWidth().height((40 * fraction).dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(WhipSpacing.micro)))
                                }
                                Text(point.label, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
