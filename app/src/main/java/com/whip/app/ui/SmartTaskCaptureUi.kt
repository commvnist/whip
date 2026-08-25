package com.whip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.whip.app.domain.TaskCaptureAssumption

internal class SmartTaskCaptureVisualTransformation(
    private val assumptions: List<TaskCaptureAssumption>,
    private val highlightColor: Color,
    private val highlightedTextColor: Color,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = AnnotatedString.Builder(text)
        assumptions.forEach { assumption ->
            if (assumption.start >= 0 && assumption.endExclusive <= text.length) {
                highlighted.addStyle(
                    style = SpanStyle(
                        color = highlightedTextColor,
                        background = highlightColor,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    start = assumption.start,
                    end = assumption.endExclusive,
                )
            }
        }
        return TransformedText(highlighted.toAnnotatedString(), OffsetMapping.Identity)
    }
}

internal fun List<TaskCaptureAssumption>.smartCaptureStateDescription(action: String): String? =
    takeIf { it.isNotEmpty() }?.joinToString(
        prefix = "Smart Capture assumptions: ",
        postfix = ". $action",
        separator = "; ",
        transform = TaskCaptureAssumption::interpretation,
    )

@Composable
internal fun SmartTaskCapturePreview(
    assumptions: List<TaskCaptureAssumption>,
    actionText: String,
    modifier: Modifier = Modifier,
    testTag: String = "smart-task-capture-preview",
) {
    if (assumptions.isEmpty()) return
    Surface(
        modifier = modifier.testTag(testTag),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "Smart Capture preview",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                assumptions.forEach { assumption ->
                    Surface(
                        modifier = Modifier.testTag("smart-task-assumption-${assumption.kind.name}"),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Text(
                            assumption.interpretation,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
            Text(
                actionText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
