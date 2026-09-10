package com.whip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal enum class WhipExecutionEmphasis { Passive, Active, Omitted }

internal class WhipExecutionItemScope internal constructor() {
    internal data class Line(val text: String, val modifier: Modifier)
    internal var leadingSlot: (@Composable () -> Unit)? = null
    internal var actionSlot: (@Composable RowScope.() -> Unit)? = null
    internal var inputSlot: (@Composable () -> Unit)? = null
    internal var valueLine: Line? = null
    internal var statusLine: Line? = null
    internal var targetLine: Line? = null
    internal val supportingLines = mutableListOf<Line>()

    fun leading(content: @Composable () -> Unit) { leadingSlot = content }
    fun actions(content: @Composable RowScope.() -> Unit) { actionSlot = content }
    fun inputs(content: @Composable () -> Unit) { inputSlot = content }
    fun values(text: String, modifier: Modifier = Modifier) { valueLine = Line(text, modifier) }
    fun status(text: String, modifier: Modifier = Modifier) { statusLine = Line(text, modifier) }
    fun target(text: String?, modifier: Modifier = Modifier) { targetLine = text?.let { Line(it, modifier) } }
    fun detail(text: String?, modifier: Modifier = Modifier) {
        text?.takeIf(String::isNotBlank)?.let { supportingLines += Line(it, modifier) }
    }
}

/** Owns the Set reading order; callers retain workout meaning, exact actions and active input state. */
@Composable
internal fun WhipExecutionItem(
    identity: String,
    modifier: Modifier = Modifier,
    identityModifier: Modifier = Modifier,
    emphasis: WhipExecutionEmphasis = WhipExecutionEmphasis.Passive,
    content: WhipExecutionItemScope.() -> Unit,
) {
    val item = WhipExecutionItemScope().apply(content)
    val active = emphasis == WhipExecutionEmphasis.Active
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(
                horizontal = WhipCardGeometry.horizontalInset,
                vertical = if (active) 12.dp else WhipCardGeometry.verticalInset,
            ),
            verticalArrangement = Arrangement.spacedBy(if (active) 10.dp else WhipCardGeometry.contentGap),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                item.leadingSlot?.invoke()
                Text(
                    identity, modifier = identityModifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (emphasis == WhipExecutionEmphasis.Omitted) FontWeight.SemiBold else FontWeight.Bold,
                    color = if (emphasis == WhipExecutionEmphasis.Omitted) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.primary,
                )
                item.actionSlot?.invoke(this)
            }
            item.valueLine?.let {
                Text(it.text, it.modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            item.statusLine?.let {
                Text(it.text, it.modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item.targetLine?.let {
                Text("Target · ${it.text}", it.modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
            item.supportingLines.forEach {
                Text(it.text, it.modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item.inputSlot?.invoke()
        }
    }
}
