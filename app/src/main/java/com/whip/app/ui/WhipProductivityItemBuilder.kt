package com.whip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Text roles carry content and semantics, with typography and spacing owned by the renderer. */
internal class WhipItemInformationScope internal constructor() {
    internal val lines = mutableListOf<WhipItemInformationLine>()
    fun text(text: String, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
        if (text.isNotBlank()) lines += WhipItemInformationLine(text, modifier, maxLines)
    }
}

internal data class WhipItemInformationLine(val text: String, val modifier: Modifier, val maxLines: Int)
internal data class WhipItemDisclosure(val expanded: Boolean, val tag: String?, val toggle: () -> Unit)
internal data class WhipItemPrimaryAction(val width: Dp, val content: @Composable () -> Unit)

internal class WhipProductivityItemScope internal constructor() {
    internal var area: Pair<String, String>? = null
    internal var edit: (() -> Unit)? = null
    internal var summary = emptyList<WhipItemInformationLine>()
    internal var details = emptyList<WhipItemInformationLine>()
    internal var notice: (@Composable () -> Unit)? = null
    internal var disclosure: WhipItemDisclosure? = null
    internal var primary: WhipItemPrimaryAction? = null
    internal var expandedBody: (@Composable ColumnScope.() -> Unit)? = null

    fun area(id: String?, name: String) { area = id?.let { it to name } }
    fun edit(onClick: (() -> Unit)?) { edit = onClick }
    fun summary(content: WhipItemInformationScope.() -> Unit) { summary = WhipItemInformationScope().apply(content).lines }
    fun details(content: WhipItemInformationScope.() -> Unit) { details = WhipItemInformationScope().apply(content).lines }
    fun notice(content: @Composable () -> Unit) { notice = content }
    fun disclosure(expanded: Boolean, tag: String? = null, onToggle: (() -> Unit)?) {
        disclosure = onToggle?.let { WhipItemDisclosure(expanded, tag, it) }
    }
    fun primaryAction(width: Dp = 64.dp, content: (@Composable () -> Unit)?) {
        primary = content?.let { WhipItemPrimaryAction(width, it) }
    }
    /** Domain inputs/metrics retain their behavior inside a single ordered expanded body. */
    fun expandedContent(content: @Composable ColumnScope.() -> Unit) { expandedBody = content }
}

/**
 * Productivity content inside WhipItemCard. The caller owns its selection/drag gestures and
 * state color; this builder owns header, information, specialized body and secondary edit order.
 */
@Composable
internal fun WhipProductivityItemContent(
    itemType: String,
    itemName: String,
    emoji: String,
    modifier: Modifier = Modifier,
    identityModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    primaryActionModifier: Modifier = Modifier,
    editModifier: Modifier = Modifier,
    titleCompleted: Boolean = false,
    content: WhipProductivityItemScope.() -> Unit,
) {
    val item = WhipProductivityItemScope().apply(content)
    val disclosure = item.disclosure
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(identityModifier) { WhipIdentityEmoji(emoji) }
            Spacer(Modifier.width(8.dp))
            Text(
                itemName, modifier = titleModifier.weight(1f),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                color = completionTextColor(titleCompleted), textDecoration = completionTextDecoration(titleCompleted),
                textAlign = TextAlign.Start, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (disclosure != null) {
                IconButton(
                    onClick = disclosure.toggle,
                    modifier = Modifier.size(48.dp)
                        .then(disclosure.tag?.let(Modifier::testTag) ?: Modifier)
                        .semantics {
                            contentDescription = "${if (disclosure.expanded) "Collapse" else "Expand"} $itemType $itemName"
                            stateDescription = if (disclosure.expanded) "Expanded" else "Collapsed"
                        },
                ) {
                    Icon(if (disclosure.expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
                }
            } else item.edit?.let { ItemEditButton(itemType, itemName, it, editModifier) }
            item.primary?.let { action ->
                Box(primaryActionModifier.width(action.width).heightIn(min = 48.dp), contentAlignment = Alignment.Center) { action.content() }
            }
        }
        if (disclosure != null && !disclosure.expanded) {
            InformationLines(item.summary)
        } else {
            if (disclosure != null) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(WhipCardGeometry.contentGap)) {
                ProductivityItemInformationColumn {
                    item.notice?.invoke()
                    item.area?.let { (id, name) -> AreaBadge(id, name) }
                    InformationLines(item.details)
                }
                if (disclosure?.expanded == true) {
                    item.expandedBody?.invoke(this)
                    item.edit?.let { onEdit ->
                        WhipTextButton(
                            onClick = onEdit,
                            modifier = editModifier.fillMaxWidth().heightIn(min = 48.dp)
                                .semantics { contentDescription = "Edit $itemType $itemName" },
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit")
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InformationLines(lines: List<WhipItemInformationLine>) {
    if (lines.isNotEmpty()) ProductivityItemInformationColumn {
        lines.forEach { line -> ProductivityItemSupportingText(line.text, line.modifier.fillMaxWidth(), line.maxLines) }
    }
}
