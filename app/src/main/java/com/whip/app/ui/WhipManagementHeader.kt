package com.whip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal data class WhipManagementAction(
    val label: String,
    val onClick: () -> Unit,
    val testTag: String? = null,
)

/** One destination identity, complete supporting text and stable navigation/action roles. */
@Composable
internal fun WhipManagementHeader(
    title: String,
    supportingText: String,
    close: WhipManagementAction,
    primary: WhipManagementAction? = null,
    back: WhipManagementAction? = null,
    enabled: Boolean = true,
    titleTag: String? = null,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stackPrimary = primary != null && maxWidth < 360.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        Column(
            Modifier.fillMaxWidth().padding(horizontal = WhipSpacing.compact, vertical = WhipSpacing.sibling),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
        ) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                back?.let { action ->
                    IconButton(onClick = action.onClick, enabled = enabled, modifier = action.modifier().size(48.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = action.label)
                    }
                }
                Column(Modifier.weight(1f).then(titleTag?.let { Modifier.testTag(it) } ?: Modifier)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                }
                if (!stackPrimary) primary?.let { ManagementPrimaryAction(it, enabled) }
                WhipTrailingCloseAction(
                    label = close.label, onClick = close.onClick, enabled = enabled, modifier = close.modifier(),
                )
            }
            Text(
                supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            if (stackPrimary) primary?.let { ManagementPrimaryAction(it, enabled, Modifier.fillMaxWidth()) }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ManagementPrimaryAction(action: WhipManagementAction, enabled: Boolean, modifier: Modifier = Modifier) {
    WhipButton(onClick = action.onClick, enabled = enabled, modifier = modifier.then(action.modifier())) {
        Text(action.label)
    }
}

private fun WhipManagementAction.modifier(): Modifier = testTag?.let { Modifier.testTag(it) } ?: Modifier
