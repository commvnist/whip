package com.whip.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp

/** Feature code declares record content and commands; it does not choose their geometry. */
internal class WhipRecordItemScope internal constructor() {
    internal val context = mutableListOf<String>()
    internal val facts = mutableListOf<String>()
    internal val details = mutableListOf<String>()
    internal var onEdit: (() -> Unit)? = null
    internal var directAction: WhipRecordCommand? = null
    internal var reorder: WhipRecordReorder? = null
    internal val commands = mutableListOf<WhipRecordCommand>()

    fun context(text: String) { if (text.isNotBlank()) context += text }
    fun detail(text: String) { if (text.isNotBlank()) details += text }
    fun fact(label: String, value: String) {
        if (value.isNotBlank()) facts += if (label.isBlank()) value else "$label: $value"
    }
    fun edit(onClick: () -> Unit) { onEdit = onClick }
    fun action(label: String, icon: ImageVector, onClick: () -> Unit) {
        directAction = WhipRecordCommand(label, icon, WhipMenuItemRole.Normal, onClick)
    }
    fun reorder(
        position: Int,
        total: Int,
        interactionState: WhipReorderInteractionState,
        layoutScope: String,
        onMove: (Int) -> Unit,
    ) { reorder = WhipRecordReorder(position, total, interactionState, layoutScope, onMove) }
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

internal data class WhipRecordReorder(
    val position: Int,
    val total: Int,
    val interactionState: WhipReorderInteractionState,
    val layoutScope: String,
    val onMove: (Int) -> Unit,
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
    onOpen: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    identityEmoji: String? = null,
    content: WhipRecordItemScope.() -> Unit,
) {
    val item = WhipRecordItemScope().apply(content)
    val reorder = item.reorder
    var menuOpen by rememberSaveable(itemKey, reorder != null) { mutableStateOf(false) }
    val recordModifier = when {
        reorder != null -> modifier.whipReorderItem(
            reorder.interactionState, layoutPosition = reorder.position, layoutScope = reorder.layoutScope,
        )
        onOpen != null -> modifier.clickable(onClickLabel = "Open $itemType $title", onClick = onOpen)
        else -> modifier
    }
    WhipItemCard(recordModifier) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WhipCardGeometry.contentGap),
        ) {
            reorder?.let {
                WhipReorderHandle(
                    label = title,
                    canMovePrevious = it.position > 1,
                    canMoveNext = it.position < it.total,
                    position = it.position, total = it.total,
                    interactionState = it.interactionState, layoutScope = it.layoutScope,
                    moveWholeItem = true, reserveWhenUnavailable = true, onMove = it.onMove,
                )
            }
            identityEmoji?.takeIf(String::isNotBlank)?.let { WhipIdentityEmoji(it) }
            Text(
                title, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            )
            if (reorder == null) item.onEdit?.let { ItemEditButton(itemType, title, it) }
            if (reorder == null) item.directAction?.let { action ->
                IconButton(onClick = action.onClick, modifier = Modifier.size(48.dp)) {
                    Icon(requireNotNull(action.icon), contentDescription = action.label)
                }
            }
            if (reorder == null && item.commands.isNotEmpty()) {
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
        if (item.context.isNotEmpty() || item.facts.isNotEmpty() || item.details.isNotEmpty()) {
            ProductivityItemInformationColumn {
                if (item.context.isNotEmpty()) ProductivityItemSupportingText(item.context.joinToString(" · "))
                if (item.facts.isNotEmpty()) ProductivityItemSupportingText(item.facts.joinToString(" · "), maxLines = 3)
                item.details.forEach { ProductivityItemSupportingText(it) }
            }
        }
    }
}
