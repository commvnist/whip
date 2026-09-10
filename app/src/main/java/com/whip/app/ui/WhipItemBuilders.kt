package com.whip.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Feature code declares record content and commands; it does not choose their geometry. */
internal class WhipRecordItemScope internal constructor() {
    internal val context = mutableListOf<String>()
    internal val facts = mutableListOf<String>()
    internal var onEdit: (() -> Unit)? = null
    internal val commands = mutableListOf<WhipRecordCommand>()

    fun context(text: String) { if (text.isNotBlank()) context += text }
    fun fact(label: String, value: String) {
        if (value.isNotBlank()) facts += if (label.isBlank()) value else "$label: $value"
    }
    fun edit(onClick: () -> Unit) { onEdit = onClick }
    fun command(
        label: String,
        icon: ImageVector? = null,
        role: WhipMenuItemRole = WhipMenuItemRole.Normal,
        onClick: () -> Unit,
    ) { commands += WhipRecordCommand(label, icon, role, onClick) }
}

internal data class WhipRecordCommand(
    val label: String,
    val icon: ImageVector?,
    val role: WhipMenuItemRole,
    val onClick: () -> Unit,
)

/**
 * Record family: title and direct actions, then full-width context and fact preview.
 * Optional roles collapse; parent lists own scrolling. Domain state stays with the caller.
 */
@Composable
internal fun WhipRecordItem(
    itemKey: Any,
    itemType: String,
    title: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    identityEmoji: String? = null,
    content: WhipRecordItemScope.() -> Unit,
) {
    val item = WhipRecordItemScope().apply(content)
    var menuOpen by rememberSaveable(itemKey) { mutableStateOf(false) }
    WhipItemCard(modifier.clickable(onClickLabel = "Open $itemType $title", onClick = onOpen)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WhipCardGeometry.contentGap),
        ) {
            identityEmoji?.takeIf(String::isNotBlank)?.let { WhipIdentityEmoji(it) }
            Text(
                title, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            item.onEdit?.let { ItemEditButton(itemType, title, it) }
            if (item.commands.isNotEmpty()) {
                WhipOverflowMenu("More Actions for $title", menuOpen, { menuOpen = it }) {
                    val commands = item.commands.sortedBy { it.role == WhipMenuItemRole.Destructive }
                    commands.forEachIndexed { index, command ->
                        if (index > 0 && command.role != commands[index - 1].role) HorizontalDivider()
                        WhipMenuItem(
                            label = command.label, icon = command.icon, role = command.role,
                            onClick = { menuOpen = false; command.onClick() },
                        )
                    }
                }
            }
        }
        if (item.context.isNotEmpty() || item.facts.isNotEmpty()) {
            ProductivityItemInformationColumn {
                if (item.context.isNotEmpty()) ProductivityItemSupportingText(item.context.joinToString(" · "))
                if (item.facts.isNotEmpty()) ProductivityItemSupportingText(item.facts.joinToString(" · "), maxLines = 3)
            }
        }
    }
}
