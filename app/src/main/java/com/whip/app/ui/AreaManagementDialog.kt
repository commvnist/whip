package com.whip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Merge
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import com.whip.app.domain.Area
import kotlinx.coroutines.launch

@Composable
internal fun AreaManagementDialog(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    paneMaxWidth: Dp = 640.dp,
    onDismiss: () -> Unit,
) {
    var createOpen by rememberSaveable { mutableStateOf(false) }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var colorId by rememberSaveable { mutableStateOf<String?>(null) }
    var mergeSourceId by rememberSaveable { mutableStateOf<String?>(null) }
    var moveItemsSourceId by rememberSaveable { mutableStateOf<String?>(null) }
    var archiveId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val active = state.areas.filterNot(Area::archived)
    val archived = state.areas.filter(Area::archived)
    val childDialogModifier = Modifier.width(paneMaxWidth)
    val selectedArea = detailId?.let { id -> state.areas.firstOrNull { it.id == id } }
    BackHandler {
        if (detailId != null) detailId = null else onDismiss()
    }

    WhipFullScreenSurface(title = "Areas") {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (detailId != null) {
                        IconButton(
                            onClick = { detailId = null },
                            modifier = Modifier
                                .testTag("area-back-action")
                                .semantics { contentDescription = "Back to Areas" },
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                        }
                    }
                    Column(Modifier.weight(1f).testTag("area-destination-title")) {
                        Text(selectedArea?.name ?: "Areas", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            if (selectedArea == null) "Group related tasks, habits, goals, and tracks." else usageText(state.areaUsage[selectedArea.id] ?: AreaUsageCounts()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (selectedArea == null) WhipButton(onClick = { createOpen = true }) { Text("Create Area") }
                    WhipTrailingCloseAction(
                        label = "Close Areas",
                        onClick = onDismiss,
                        modifier = Modifier.testTag("area-close-action"),
                    )
                }
                HorizontalDivider()
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val masterDetail = maxWidth >= 760.dp
                    if (masterDetail) {
                        Row(Modifier.fillMaxSize()) {
                            AreaListContent(
                                modifier = Modifier.weight(0.42f).fillMaxHeight(),
                                state = state,
                                active = active,
                                archived = archived,
                                query = query,
                                onQueryChange = { query = it },
                                archivedExpanded = archivedExpanded,
                                onArchivedExpandedChange = { archivedExpanded = it },
                                selectedAreaId = detailId,
                                onOpen = { detailId = it },
                                onRename = { renameId = it },
                                onColor = { colorId = it },
                                onMove = viewModel::moveArea,
                                onMoveItems = { moveItemsSourceId = it },
                                onMerge = { mergeSourceId = it },
                                onArchive = { archiveId = it },
                                onDelete = { deleteId = it },
                                onRestore = { viewModel.setAreaArchived(it, false) },
                            )
                            VerticalDivider(Modifier.fillMaxHeight())
                            if (selectedArea != null) {
                                AreaDetailContent(
                                    modifier = Modifier.weight(0.58f).fillMaxHeight(),
                                    area = selectedArea,
                                    usage = state.areaUsage[selectedArea.id] ?: AreaUsageCounts(),
                                    onRename = { renameId = selectedArea.id },
                                    onColor = { colorId = selectedArea.id },
                                    onMoveItems = { moveItemsSourceId = selectedArea.id },
                                    onMerge = { mergeSourceId = selectedArea.id },
                                    onArchive = { archiveId = selectedArea.id },
                                    onDelete = { deleteId = selectedArea.id },
                                )
                            } else {
                                WhipEmptyState(
                                    title = "Choose an Area",
                                    supportingText = "Select an Area to review its identity, items, and lifecycle actions.",
                                    modifier = Modifier.weight(0.58f).padding(24.dp),
                                )
                            }
                        }
                    } else if (selectedArea != null) {
                        AreaDetailContent(
                            modifier = Modifier.fillMaxSize(),
                            area = selectedArea,
                            usage = state.areaUsage[selectedArea.id] ?: AreaUsageCounts(),
                            onRename = { renameId = selectedArea.id },
                            onColor = { colorId = selectedArea.id },
                            onMoveItems = { moveItemsSourceId = selectedArea.id },
                            onMerge = { mergeSourceId = selectedArea.id },
                            onArchive = { archiveId = selectedArea.id },
                            onDelete = { deleteId = selectedArea.id },
                        )
                    } else {
                        AreaListContent(
                            modifier = Modifier.fillMaxSize(),
                            state = state,
                            active = active,
                            archived = archived,
                            query = query,
                            onQueryChange = { query = it },
                            archivedExpanded = archivedExpanded,
                            onArchivedExpandedChange = { archivedExpanded = it },
                            selectedAreaId = null,
                            onOpen = { detailId = it },
                            onRename = { renameId = it },
                            onColor = { colorId = it },
                            onMove = viewModel::moveArea,
                            onMoveItems = { moveItemsSourceId = it },
                            onMerge = { mergeSourceId = it },
                            onArchive = { archiveId = it },
                            onDelete = { deleteId = it },
                            onRestore = { viewModel.setAreaArchived(it, false) },
                        )
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
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
    moveItemsSourceId?.let { sourceId ->
        val source = state.areas.firstOrNull { it.id == sourceId }
        if (source != null) {
            MoveAreaItemsDialog(
                modifier = childDialogModifier,
                sourceId = sourceId,
                sourceName = source.name,
                usage = state.areaUsage[sourceId] ?: AreaUsageCounts(),
                targets = active.filter { it.id != sourceId },
                onDismiss = { moveItemsSourceId = null },
                onMove = { targetId ->
                    viewModel.moveAllAreaItems(sourceId, targetId)
                    moveItemsSourceId = null
                },
            )
        }
    }
    archiveId?.let { id -> state.areas.firstOrNull { it.id == id }?.let { area ->
        val usage = state.areaUsage[id] ?: AreaUsageCounts()
        if (!area.archived && active.size == 1) {
            LastAreaRequiredDialog(childDialogModifier, area, "archive", { archiveId = null }) {
                archiveId = null
                createOpen = true
            }
        } else {
            PaneAwareAlertDialog(
                modifier = childDialogModifier,
                onDismissRequest = { archiveId = null },
                title = { Text("Archive ${area.name}?") },
                text = { Text(if (usage.total == 0) "It will be hidden from area pickers." else "${usageText(usage)} will keep this assignment. The area will be hidden from pickers until restored.") },
                confirmButton = { WhipTextButton(onClick = {
                    viewModel.setAreaArchived(id, true)
                    archiveId = null
                    scope.launch {
                        if (snackbar.showSnackbar("${area.name} archived", "Undo") == SnackbarResult.ActionPerformed) {
                            viewModel.setAreaArchived(id, false)
                        }
                    }
                }) { Text("Archive") } },
                dismissButton = { WhipTextButton(onClick = { archiveId = null }) { Text("Cancel") } },
            )
        }
    } }
    deleteId?.let { id -> state.areas.firstOrNull { it.id == id }?.let { area ->
        if (!area.archived && active.size == 1) {
            LastAreaRequiredDialog(childDialogModifier, area, "delete", { deleteId = null }) {
                deleteId = null
                createOpen = true
            }
        } else {
            PermanentAreaDeleteDialog(
                modifier = childDialogModifier,
                area = area,
                usage = state.areaUsage[id] ?: AreaUsageCounts(),
                replacementAreas = active.filter { it.id != id },
                onDismiss = { deleteId = null },
                onMoveItems = { targetId ->
                    viewModel.deleteAreaAndKeepItems(id, targetId)
                    deleteId = null
                },
                onDeleteItems = {
                    viewModel.deleteAreaAndItems(id)
                    deleteId = null
                },
            )
        }
    } }
}

@Composable
private fun AreaListContent(
    modifier: Modifier,
    state: SettingsUiState,
    active: List<Area>,
    archived: List<Area>,
    query: String,
    onQueryChange: (String) -> Unit,
    archivedExpanded: Boolean,
    onArchivedExpandedChange: (Boolean) -> Unit,
    selectedAreaId: String?,
    onOpen: (String) -> Unit,
    onRename: (String) -> Unit,
    onColor: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onMoveItems: (String) -> Unit,
    onMerge: (String) -> Unit,
    onArchive: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRestore: (String) -> Unit,
) {
    var reordering by rememberSaveable { mutableStateOf(false) }
    var toolsExpanded by rememberSaveable { mutableStateOf(false) }
    val visibleActive = active.filter { query.isBlank() || it.name.contains(query, true) }
    BackHandler(enabled = reordering) { reordering = false }
    WhipReorderLazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 16.dp, 20.dp, 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            WhipPageHeader(
                title = "Your Areas",
                supportingText = "Open an Area to rename it, move its items, merge it, or manage its lifecycle.",
            ) {
                if (!reordering && active.size > 1) Box {
                    WhipPageIconAction(
                        icon = Icons.Outlined.MoreVert,
                        label = "More Area Actions",
                        onClick = { toolsExpanded = true },
                    )
                    DropdownMenu(expanded = toolsExpanded, onDismissRequest = { toolsExpanded = false }) {
                        WhipMenuItem(
                            label = if (query.isBlank()) "Reorder Areas" else "Clear Search & Reorder All",
                            onClick = {
                                if (query.isNotBlank()) onQueryChange("")
                                reordering = true
                                toolsExpanded = false
                            },
                        )
                    }
                }
            }
        }
        if (!reordering && state.areas.size > 8) item {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    reordering = false
                    onQueryChange(it.take(40))
                },
                label = { Text("Find Area") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (reordering) item {
            WhipReorderModeBar(
                itemLabel = "Areas",
                onDone = { reordering = false },
                boundaryNote = "Archived Areas stay outside this order.",
            )
        }
        itemsIndexed(
            visibleActive,
            key = { _, area -> area.id },
        ) { index, area ->
            AreaManagerRow(
                area = area,
                usage = state.areaUsage[area.id] ?: AreaUsageCounts(),
                selected = selectedAreaId == area.id,
                canMoveUp = index > 0,
                canMoveDown = index < visibleActive.lastIndex,
                position = index + 1,
                total = visibleActive.size,
                reorderEnabled = reordering && query.isBlank(),
                onRename = { onRename(area.id) },
                onColor = { onColor(area.id) },
                onMove = { onMove(area.id, it) },
                onMoveItems = { onMoveItems(area.id) },
                onMerge = { onMerge(area.id) },
                onArchive = { onArchive(area.id) },
                onDelete = { onDelete(area.id) },
                onOpen = { onOpen(area.id) },
            )
        }
        if (!reordering && archived.isNotEmpty()) item {
            HorizontalDivider(Modifier.padding(top = 8.dp))
            DisclosureButton(
                label = "Archived · ${archived.size}",
                expanded = archivedExpanded,
                onClick = { onArchivedExpandedChange(!archivedExpanded) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!reordering && archivedExpanded) itemsIndexed(
            archived.filter { query.isBlank() || it.name.contains(query, true) },
            key = { _, area -> "archived-${area.id}" },
        ) { _, area ->
            Surface(
                onClick = { onOpen(area.id) },
                color = if (selectedAreaId == area.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AreaDot(area)
                    Column(Modifier.weight(1f)) {
                        Text(area.name)
                        Text("Archived · ${usageText(state.areaUsage[area.id] ?: AreaUsageCounts())}", style = MaterialTheme.typography.bodySmall)
                    }
                    WhipTextButton(onClick = { onRestore(area.id) }) { Text("Restore") }
                }
            }
        }
    }
}

@Composable
private fun AreaManagerRow(
    area: Area,
    usage: AreaUsageCounts,
    selected: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    position: Int,
    total: Int,
    reorderEnabled: Boolean,
    onRename: () -> Unit,
    onColor: () -> Unit,
    onMove: (Int) -> Unit,
    onMoveItems: () -> Unit,
    onMerge: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
) {
    var menu by rememberSaveable { mutableStateOf(false) }
    val reorderInteraction = rememberWhipReorderInteractionState()
    Surface(
        onClick = onOpen,
        enabled = !reorderEnabled,
        tonalElevation = 1.dp,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .whipReorderItem(
                reorderInteraction,
                layoutPosition = position,
                layoutScope = "area-browse",
            )
            .then(
                if (reorderEnabled) Modifier
                else Modifier.semantics { contentDescription = "Open area details for ${area.name}" },
            ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (reorderEnabled) {
                WhipReorderHandle(
                    label = "${area.name} Area",
                    canMovePrevious = canMoveUp,
                    canMoveNext = canMoveDown,
                    position = position,
                    total = total,
                    interactionState = reorderInteraction,
                    moveWholeItem = true,
                    layoutScope = "area-browse",
                    reserveWhenUnavailable = true,
                    onMove = onMove,
                )
            }
            AreaDot(area)
            Column(Modifier.weight(1f)) {
                Text(area.name, style = MaterialTheme.typography.titleMedium)
                Text(usageText(usage), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!reorderEnabled) Box {
                IconButton(
                    onClick = { menu = true },
                    modifier = Modifier.semantics { contentDescription = "More options for ${area.name}" },
                ) { Icon(Icons.Outlined.MoreVert, contentDescription = null) }
                DropdownMenu(menu, { menu = false }) {
                    WhipMenuItem("Rename", { menu = false; onRename() }, Icons.Outlined.Edit)
                    WhipMenuItem("Choose Color", { menu = false; onColor() }, Icons.Outlined.Palette)
                    HorizontalDivider()
                    WhipMenuItem("Move All Items…", { menu = false; onMoveItems() }, Icons.AutoMirrored.Outlined.DriveFileMove, enabled = usage.total > 0)
                    WhipMenuItem("Merge Into…", { menu = false; onMerge() }, Icons.Outlined.Merge)
                    HorizontalDivider()
                    WhipMenuItem("Archive", { menu = false; onArchive() }, Icons.Outlined.Archive)
                    HorizontalDivider()
                    WhipMenuItem(
                        "Delete Permanently",
                        { menu = false; onDelete() },
                        Icons.Outlined.DeleteForever,
                        role = WhipMenuItemRole.Destructive,
                    )
                }
            }
        }
    }
}

@Composable
private fun AreaDetailContent(
    modifier: Modifier = Modifier,
    area: Area,
    usage: AreaUsageCounts,
    onRename: () -> Unit,
    onColor: () -> Unit,
    onMoveItems: () -> Unit,
    onMerge: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp, 20.dp, 24.dp, 88.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            WhipPageHeader(
                title = area.name,
                supportingText = if (area.archived) "Archived Area · ${usageText(usage)}" else "Active Area · ${usageText(usage)}",
            )
        }
        item {
            WhipSection("Identity", supportingText = "These changes preserve every assigned item and its history.") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AreaDot(area)
                    Text("Area color", modifier = Modifier.weight(1f))
                }
                WhipOutlinedButton(onClick = onRename, modifier = Modifier.fillMaxWidth()) { Text("Rename Area") }
                WhipOutlinedButton(onClick = onColor, modifier = Modifier.fillMaxWidth()) { Text("Choose Color") }
            }
        }
        item {
            WhipSection("Organization", supportingText = "Move or combine every assigned task, habit, goal, Track, and Track Entry in one operation.") {
                WhipOutlinedButton(onClick = onMoveItems, enabled = usage.total > 0, modifier = Modifier.fillMaxWidth()) {
                    Text(if (usage.total > 0) "Move ${usage.total} Items" else "No Items to Move")
                }
                WhipOutlinedButton(onClick = onMerge, modifier = Modifier.fillMaxWidth()) { Text("Merge into Another Area") }
            }
        }
        item {
            WhipSection("Lifecycle") {
                WhipOutlinedButton(onClick = onArchive, modifier = Modifier.fillMaxWidth()) {
                    Text(if (area.archived) "Restore Area" else "Archive Area")
                }
            }
        }
        item {
            WhipDangerZone {
                Text("Permanently deleting an Area requires choosing whether to move its items or delete them too.")
                WhipOutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete Area Permanently") }
            }
        }
    }
}

@Composable
private fun OutlinedAction(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    WhipOutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = if (destructive) androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ) else androidx.compose.material3.ButtonDefaults.outlinedButtonColors(),
    ) { Text(label) }
}

@Composable
internal fun MoveAreaItemsDialog(
    modifier: Modifier = Modifier,
    sourceId: String,
    sourceName: String,
    usage: AreaUsageCounts,
    targets: List<Area>,
    onDismiss: () -> Unit,
    onMove: (String) -> Unit,
) {
    var targetId by rememberSaveable(sourceId) { mutableStateOf<String?>(null) }
    val options = targets.filterNot(Area::archived)
    val selected = options.firstOrNull { it.id == targetId }
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Move Everything from $sourceName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Move ${usageText(usage)} together. The source area and all item history will remain unchanged.")
                if (options.isEmpty()) {
                    Text("Create another active Area before moving these items.")
                } else {
                    Text("Destination", style = MaterialTheme.typography.labelLarge)
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { targetId = option.id }
                                .semantics { contentDescription = "Move to ${option.name}" }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = targetId == option.id, onClick = { targetId = option.id })
                            Text(option.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            WhipTextButton(enabled = selected != null, onClick = { selected?.let { onMove(it.id) } }) {
                Text("Move ${usage.total} Items")
            }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun PermanentAreaDeleteDialog(
    modifier: Modifier = Modifier,
    area: Area,
    usage: AreaUsageCounts,
    replacementAreas: List<Area>,
    onDismiss: () -> Unit,
    onMoveItems: (String) -> Unit,
    onDeleteItems: () -> Unit,
) {
    var replacementId by rememberSaveable(area.id) { mutableStateOf<String?>(null) }
    val replacement = replacementAreas.firstOrNull { it.id == replacementId }
    val explanation = if (usage.total == 0) {
        "This area is empty. Deleting it cannot be undone."
    } else {
        "Choose what happens to ${usage.total} assigned items: ${usage.tasks} tasks, ${usage.habits} habits, ${usage.goals} goals, and ${usage.tracks} Tracks containing ${usage.trackEntries} Entries. " +
            "Moving them keeps the items and their history. Deleting the items cannot be undone. " +
            "Saved filters and widgets using this area will reset to All areas."
    }
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Delete ${area.name} Permanently?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(explanation)
                if (usage.total > 0) {
                    Text("Move items to", style = MaterialTheme.typography.labelLarge)
                    replacementAreas.forEach { target ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { replacementId = target.id }
                                .semantics { contentDescription = "Move items to ${target.name}" }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(replacementId == target.id, { replacementId = target.id })
                            Text(target.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (usage.total > 0) {
                    WhipTextButton(
                        enabled = replacement != null,
                        onClick = { replacement?.let { onMoveItems(it.id) } },
                    ) { Text("Move Items and Delete Area") }
                }
                WhipTextButton(
                    onClick = onDeleteItems,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(if (usage.total == 0) "Delete Area" else "Delete Area and ${usage.total} Items")
                }
            }
        },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun LastAreaRequiredDialog(
    modifier: Modifier,
    area: Area,
    action: String,
    onDismiss: () -> Unit,
    onCreateArea: () -> Unit,
) {
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Create Another Area First") },
        text = { Text("${area.name} is your only active Area. Every item in Whip must have an Area, so create another one before you $action ${area.name}.") },
        confirmButton = { WhipTextButton(onClick = onCreateArea) { Text("Create Area") } },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable private fun AreaDot(area: Area) {
    Box(
        Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(area.colorArgb?.let(::Color) ?: MaterialTheme.colorScheme.outlineVariant)
            .semantics { contentDescription = "Area color for ${area.name}" },
    )
}

private fun usageText(usage: AreaUsageCounts): String {
    if (usage.total == 0) return "No items"
    val categories = listOf(
        usage.tasks to "task",
        usage.habits to "habit",
        usage.goals to "goal",
        usage.tracks to "Track",
        usage.trackEntries to "Track Entry",
    ).filter { it.first > 0 }.map { (count, noun) -> quantityLabel(count, noun) }
    return (listOf(quantityLabel(usage.total, "item")) + categories).joinToString(" · ")
}

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
        title = { Text("Rename Area") },
        text = { Column {
            OutlinedTextField(name, { name = it.take(40); error = null }, label = { Text("Area name") }, supportingText = { Text("${name.length}/40") }, singleLine = true, isError = conflict != null || error != null)
            conflict?.let { Text("${it.name} already exists. Cancel and use Merge instead.", color = MaterialTheme.colorScheme.error) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { WhipTextButton(enabled = name.isNotBlank() && conflict == null && !saving, onClick = {
            saving = true
            onRename(name.trim()) { result -> saving = false; result.onSuccess { onRenamed() }.onFailure { error = it.message } }
        }) { Text(if (saving) "Saving…" else "Rename") } },
        dismissButton = { WhipTextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AreaColorDialog(modifier: Modifier, area: Area, onDismiss: () -> Unit, onSelect: (Long?) -> Unit) {
    WhipColorPickerDialog(
        modifier = modifier,
        title = "Color for ${area.name}",
        initialColor = area.colorArgb,
        onDismiss = onDismiss,
        onConfirm = onSelect,
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
        confirmButton = { WhipTextButton(enabled = target != null, onClick = { target?.let(onMerge) }) { Text("Merge into ${target?.name ?: "…"}") } },
        dismissButton = { WhipTextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
