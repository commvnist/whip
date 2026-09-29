package com.whip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Root chrome has a content-independent height: counts and available tools never move the list. */
@Composable
internal fun WhipWorkspaceHeader(
    summary: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backLabel: String = "Back to previous view",
    backModifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    Row(
        modifier.fillMaxWidth().padding(horizontal = WhipSpacing.screenCompact)
            .heightIn(min = maxOf(72f, 40f * fontScale + 24f).dp).testTag("workspace-context-row"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
    ) {
        if (onBack != null) WhipBackAction(backLabel, onBack, backModifier)
        Text(summary, Modifier.weight(1f).semantics { heading() }.testTag("workspace-context-summary"),
            style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(Modifier.widthIn(min = 96.dp).heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically,
            content = actions)
    }
}

@Composable
internal fun WhipWorkspaceMore(
    label: String,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        WhipPageIconAction(Icons.Outlined.MoreVert, label, { expanded = true })
        DropdownMenu(expanded, { expanded = false }) { content { expanded = false } }
    }
}
