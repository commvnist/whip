package com.whip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import com.whip.app.domain.Area
import kotlinx.coroutines.launch

@Composable
internal fun AreaManagementDialog(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    adaptiveLayout: WhipAdaptiveLayout,
    paneOffsetX: Dp = 0.dp,
    paneMaxWidth: Dp = 640.dp,
    onDismiss: () -> Unit,
) {
    var createOpen by rememberSaveable { mutableStateOf(false) }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var colorId by rememberSaveable { mutableStateOf<String?>(null) }
    var mergeSourceId by rememberSaveable { mutableStateOf<String?>(null) }
    var archiveId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val active = state.areas.filterNot(Area::archived)
    val archived = state.areas.filter(Area::archived)
    val childDialogModifier = Modifier.absoluteOffset(x = paneOffsetX).width(paneMaxWidth)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val surfaceModifier = if (adaptiveLayout == WhipAdaptiveLayout.Compact) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .absoluteOffset(x = paneOffsetX)
                    .width(minOf(maxWidth * 0.94f, paneMaxWidth, 640.dp))
                    .fillMaxHeight(0.88f)
            }
        Surface(modifier = surfaceModifier, shape = MaterialTheme.shapes.extraLarge) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Areas", style = MaterialTheme.typography.headlineSmall)
                            Text("Group related tasks, habits, and goals.", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { createOpen = true }) { Text("Create") }
                        TextButton(onClick = onDismiss) { Text("Done") }
                    }
                    HorizontalDivider()
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 12.dp, 20.dp, 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.areas.size > 8) item {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it.take(40) },
                                label = { Text("Find area") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (active.isEmpty()) item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("No active areas yet", style = MaterialTheme.typography.titleMedium)
                                Text("Create an area with any name you want, then use it across Home, Tasks, Habits, Goals, Search, and Review.")
                                Button(onClick = { createOpen = true }) { Text("Create area") }
                            }
                        }
                        itemsIndexed(
                            active.filter { query.isBlank() || it.name.contains(query, true) },
                            key = { _, area -> area.id },
                        ) { index, area ->
                            AreaManagerRow(
                                area = area,
                                usage = state.areaUsage[area.id] ?: AreaUsageCounts(),
                                canMoveUp = index > 0,
                                canMoveDown = index < active.lastIndex,
                                onRename = { renameId = area.id },
                                onColor = { colorId = area.id },
                                onMove = { viewModel.moveArea(area.id, it) },
                                onMerge = { mergeSourceId = area.id },
                                onArchive = { archiveId = area.id },
                                onDelete = { deleteId = area.id },
                            )
                        }
                        if (archived.isNotEmpty()) item {
                            HorizontalDivider(Modifier.padding(top = 8.dp))
                            TextButton(onClick = { archivedExpanded = !archivedExpanded }, modifier = Modifier.fillMaxWidth()) {
                                Text("Archived · ${archived.size} ${if (archivedExpanded) "▴" else "▾"}")
                            }
                        }
                        if (archivedExpanded) itemsIndexed(
                            archived.filter { query.isBlank() || it.name.contains(query, true) },
                            key = { _, area -> "archived-${area.id}" },
                        ) { _, area ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                AreaDot(area)
                                Column(Modifier.weight(1f)) {
                                    Text(area.name)
                                    Text("Archived · ${usageText(state.areaUsage[area.id] ?: AreaUsageCounts())}", style = MaterialTheme.typography.bodySmall)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    TextButton(onClick = { viewModel.setAreaArchived(area.id, false) }) { Text("Restore") }
                                    TextButton(onClick = { deleteId = area.id }) { Text("Delete") }
                                }
                            }
                        }
                    }
                }
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
            }
        }
        }
    }

    if (createOpen) CreateAreaDialog(
        modifier = childDialogModifier,
        existingAreas = state.areas,
        onDismiss = { createOpen = false },
        onCreate = viewModel::createArea,
        onSelected = { _, _ -> createOpen = false },
    )
    renameId?.let { id -> state.areas.firstOrNull { it.id == id }?.let { area ->
        RenameAreaDialog(
            modifier = childDialogModifier,
            area = area,
            existingAreas = state.areas,
            onDismiss = { renameId = null },
            onRename = { name, result -> viewModel.renameArea(area.id, name, result) },
            onRenamed = { renameId = null },
        )
    } }
    colorId?.let { id -> state.areas.firstOrNull { it.id == id }?.let { area ->
        AreaColorDialog(childDialogModifier, area, { colorId = null }) { viewModel.setAreaColor(area.id, it); colorId = null }
    } }
    mergeSourceId?.let { id -> state.areas.firstOrNull { it.id == id }?.let { source ->
        MergeAreaDialog(childDialogModifier, source, active.filter { it.id != id }, state.areaUsage[id] ?: AreaUsageCounts(), { mergeSourceId = null }) { target ->
            viewModel.mergeAreas(source.id, target.id)
            mergeSourceId = null
        }
    } }
    archiveId?.let { id -> state.areas.firstOrNull { it.id == id }?.let { area ->
        val usage = state.areaUsage[id] ?: AreaUsageCounts()
        PaneAwareAlertDialog(
            modifier = childDialogModifier,
            onDismissRequest = { archiveId = null },
            title = { Text("Archive ${area.name}?") },
            text = { Text(if (usage.total == 0) "It will be hidden from area pickers." else "${usageText(usage)} will keep this assignment. The area will be hidden from pickers until restored.") },
            confirmButton = { TextButton(onClick = {
                viewModel.setAreaArchived(id, true)
                archiveId = null
                scope.launch {
                    if (snackbar.showSnackbar("${area.name} archived", "Undo") == SnackbarResult.ActionPerformed) {
                        viewModel.setAreaArchived(id, false)
                    }
                }
            }) { Text("Archive") } },
            dismissButton = { TextButton(onClick = { archiveId = null }) { Text("Cancel") } },
        )
    } }
    deleteId?.let { id -> state.areas.firstOrNull { it.id == id }?.let { area ->
        PermanentAreaDeleteDialog(
            modifier = childDialogModifier,
            area = area,
            usage = state.areaUsage[id] ?: AreaUsageCounts(),
            onDismiss = { deleteId = null },
            onKeepItems = {
                viewModel.deleteAreaAndKeepItems(id)
                deleteId = null
            },
            onDeleteItems = {
                viewModel.deleteAreaAndItems(id)
                deleteId = null
            },
        )
    } }
}

@Composable
private fun AreaManagerRow(
    area: Area,
    usage: AreaUsageCounts,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRename: () -> Unit,
    onColor: () -> Unit,
    onMove: (Int) -> Unit,
    onMerge: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by rememberSaveable { mutableStateOf(false) }
    Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AreaDot(area)
            Column(Modifier.weight(1f)) {
                Text(area.name, style = MaterialTheme.typography.titleMedium)
                Text(usageText(usage), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(
                    onClick = { menu = true },
                    modifier = Modifier.semantics { contentDescription = "More options for ${area.name}" },
                ) { Icon(Icons.Outlined.MoreVert, contentDescription = null) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Rename") }, { menu = false; onRename() })
                    DropdownMenuItem({ Text("Choose color") }, { menu = false; onColor() })
                    DropdownMenuItem({ Text("Move up") }, { menu = false; onMove(-1) }, enabled = canMoveUp)
                    DropdownMenuItem({ Text("Move down") }, { menu = false; onMove(1) }, enabled = canMoveDown)
                    DropdownMenuItem({ Text("Merge into…") }, { menu = false; onMerge() })
                    DropdownMenuItem({ Text("Archive") }, { menu = false; onArchive() })
                    DropdownMenuItem({ Text("Delete permanently") }, { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
internal fun PermanentAreaDeleteDialog(
    modifier: Modifier = Modifier,
    area: Area,
    usage: AreaUsageCounts,
    onDismiss: () -> Unit,
    onKeepItems: () -> Unit,
    onDeleteItems: () -> Unit,
) {
    val explanation = if (usage.total == 0) {
        "This area is empty. Deleting it cannot be undone."
    } else {
        "Choose what happens to ${usage.total} assigned items: ${usage.tasks} tasks, ${usage.habits} habits, and ${usage.goals} goals. " +
            "Moving them keeps the items and their history. Deleting the items cannot be undone. " +
            "Saved filters and widgets using this area will reset to All areas."
    }
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Delete ${area.name} permanently?") },
        text = { Text(explanation) },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (usage.total > 0) {
                    TextButton(onClick = onKeepItems) { Text("Move items to No area") }
                }
                TextButton(
                    onClick = if (usage.total == 0) onKeepItems else onDeleteItems,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(if (usage.total == 0) "Delete area" else "Delete area and ${usage.total} items")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable private fun AreaDot(area: Area) {
    Box(Modifier.size(14.dp).clip(CircleShape).background(area.colorArgb?.let(::Color) ?: MaterialTheme.colorScheme.outlineVariant))
}

private fun usageText(usage: AreaUsageCounts): String =
    "${usage.total} items · ${usage.tasks} tasks · ${usage.habits} habits · ${usage.goals} goals"

@Composable
private fun RenameAreaDialog(
    modifier: Modifier,
    area: Area,
    existingAreas: List<Area>,
    onDismiss: () -> Unit,
    onRename: (String, (Result<Unit>) -> Unit) -> Unit,
    onRenamed: () -> Unit,
) {
    var name by rememberSaveable(area.id) { mutableStateOf(area.name) }
    var saving by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val conflict = existingAreas.firstOrNull { it.id != area.id && it.name.equals(name.trim(), true) }
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Rename area") },
        text = { Column {
            OutlinedTextField(name, { name = it.take(40); error = null }, label = { Text("Area name") }, supportingText = { Text("${name.length}/40") }, singleLine = true, isError = conflict != null || error != null)
            conflict?.let { Text("${it.name} already exists. Cancel and use Merge instead.", color = MaterialTheme.colorScheme.error) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(enabled = name.isNotBlank() && conflict == null && !saving, onClick = {
            saving = true
            onRename(name.trim()) { result -> saving = false; result.onSuccess { onRenamed() }.onFailure { error = it.message } }
        }) { Text(if (saving) "Saving…" else "Rename") } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AreaColorDialog(modifier: Modifier, area: Area, onDismiss: () -> Unit, onSelect: (Long?) -> Unit) {
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Color for ${area.name}") },
        text = { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AreaColorPalette.forEach { color -> Surface(
                onClick = { onSelect(color) },
                color = color?.let(::Color) ?: MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(if (color == area.colorArgb) 3.dp else 1.dp, if (color == area.colorArgb) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
            ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { if (color == null) Text("—") } } }
        } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MergeAreaDialog(
    modifier: Modifier,
    source: Area,
    targets: List<Area>,
    usage: AreaUsageCounts,
    onDismiss: () -> Unit,
    onMerge: (Area) -> Unit,
) {
    var targetId by rememberSaveable { mutableStateOf<String?>(null) }
    val target = targets.firstOrNull { it.id == targetId }
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Merge ${source.name}") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Move ${usageText(usage)} to another area. ${source.name} will then be removed. This cannot be undone.")
            if (targets.isEmpty()) Text("Create another active area before merging.") else AreaSelectionDropdown(targets, targetId, onSelect = { id, _ -> targetId = id })
        } },
        confirmButton = { TextButton(enabled = target != null, onClick = { target?.let(onMerge) }) { Text("Merge into ${target?.name ?: "…"}") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
