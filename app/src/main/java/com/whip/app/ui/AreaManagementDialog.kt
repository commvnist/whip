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
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Merge
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import com.whip.app.R
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
    var pendingAction by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val areaMutationState by viewModel.areaMutationState.collectAsStateWithLifecycle()
    val active = state.areas.filterNot(Area::archived)
    val archived = state.areas.filter(Area::archived)
    val childDialogModifier = Modifier.width(paneMaxWidth)
    val selectedArea = detailId?.let { id -> state.areas.firstOrNull { it.id == id } }
    lateinit var mutationCoordinator: EntitySaveCoordinator
    mutationCoordinator = rememberPersistenceRequestCoordinator(
        state = areaMutationState,
        consume = viewModel::consumeAreaMutation,
        key = "area-management",
        requestNamespace = "area-management",
        onPersisted = { receipt ->
            when (receipt.kind) {
                AreaMutationKind.Create -> createOpen = false
                AreaMutationKind.Rename -> renameId = null
                AreaMutationKind.Color -> colorId = null
                AreaMutationKind.Reorder -> Unit
                AreaMutationKind.MoveItems -> moveItemsSourceId = null
                AreaMutationKind.Merge -> {
                    mergeSourceId = null
                    if (detailId == receipt.areaId) detailId = receipt.relatedAreaId
                }
                AreaMutationKind.Archive -> archiveId = null
                AreaMutationKind.Restore -> Unit
                AreaMutationKind.DeleteKeepingItems,
                AreaMutationKind.DeleteWithItems,
                -> {
                    deleteId = null
                    if (detailId == receipt.areaId) detailId = null
                }
            }
            pendingAction = null
            val resultMessage = when (receipt.kind) {
                AreaMutationKind.Create -> "Area created"
                AreaMutationKind.Rename -> "Area renamed"
                AreaMutationKind.Color -> "Area color updated"
                AreaMutationKind.Reorder -> "Area order updated"
                AreaMutationKind.MoveItems -> "Area items moved"
                AreaMutationKind.Merge -> "Areas merged"
                AreaMutationKind.Archive -> "Area archived"
                AreaMutationKind.Restore -> "Area restored"
                AreaMutationKind.DeleteKeepingItems -> "Area deleted; assigned items moved"
                AreaMutationKind.DeleteWithItems -> "Area and assigned items permanently deleted"
            }
            scope.launch {
                val message = listOfNotNull(
                    resultMessage,
                    receipt.warnings.joinToString(" ").takeIf(String::isNotBlank),
                ).joinToString(" · ")
                val result = snackbar.showSnackbar(
                    message = message,
                    actionLabel = "Undo".takeIf { receipt.kind == AreaMutationKind.Archive },
                    withDismissAction = receipt.warnings.isNotEmpty(),
                )
                if (result == SnackbarResult.ActionPerformed && receipt.kind == AreaMutationKind.Archive) {
                    val requestId = mutationCoordinator.begin() ?: return@launch
                    pendingAction = AreaMutationKind.Restore.name
                    if (!viewModel.setAreaArchivedMutation(requestId, receipt.areaId, false)) {
                        pendingAction = null
                        mutationCoordinator.finishFailure(
                            "Another Area change is still finishing. Review the current Areas and try Restore again.",
                        )
                    }
                }
            }
        },
        orphanedMessage =
            "The previous Area change was interrupted. Review the current Area, assignments, and archive state before retrying a destructive action.",
    )
    fun submitAreaMutation(kind: AreaMutationKind, submit: (String) -> Boolean) {
        val requestId = mutationCoordinator.begin() ?: return
        pendingAction = kind.name
        if (!submit(requestId)) {
            pendingAction = null
            mutationCoordinator.finishFailure(
                "Another Area change is still finishing. Review the current Areas and try again.",
            )
        }
    }
    fun saving(kind: AreaMutationKind): Boolean =
        mutationCoordinator.saving && pendingAction == kind.name
    fun error(kind: AreaMutationKind): String? =
        mutationCoordinator.errorMessage.takeIf { pendingAction == kind.name }

    BackHandler {
        if (!mutationCoordinator.saving) {
            if (detailId != null) detailId = null else onDismiss()
        }
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
                        enabled = !mutationCoordinator.saving,
                        modifier = Modifier.testTag("area-close-action"),
                    )
                }
                HorizontalDivider()
                if (mutationCoordinator.saving) {
                    WhipStatusCard(
                        kind = WhipStatusKind.Loading,
                        title = "Saving Area Change",
                        message = "Whip is confirming the exact Area, assignments, and resulting view before this action finishes.",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                            .testTag("area-mutation-saving"),
                    )
                } else mutationCoordinator.errorMessage?.let { message ->
                    WhipStatusCard(
                        kind = WhipStatusKind.Error,
                        title = "Area Change Not Saved",
                        message = message,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                            .testTag("area-mutation-error"),
                    )
                }
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
                                onMove = { id, direction ->
                                    submitAreaMutation(AreaMutationKind.Reorder) { requestId ->
                                        viewModel.moveAreaMutation(requestId, id, direction)
                                    }
                                },
                                onMoveItems = { moveItemsSourceId = it },
                                onMerge = { mergeSourceId = it },
                                onArchive = { archiveId = it },
                                onDelete = { deleteId = it },
                                onRestore = { id ->
                                    submitAreaMutation(AreaMutationKind.Restore) { requestId ->
                                        viewModel.setAreaArchivedMutation(requestId, id, false)
                                    }
                                },
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
                                    onRestore = { id ->
                                        submitAreaMutation(AreaMutationKind.Restore) { requestId ->
                                            viewModel.setAreaArchivedMutation(requestId, id, false)
                                        }
                                    },
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
                            onRestore = { id ->
                                submitAreaMutation(AreaMutationKind.Restore) { requestId ->
                                    viewModel.setAreaArchivedMutation(requestId, id, false)
                                }
                            },
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
                            onMove = { id, direction ->
                                submitAreaMutation(AreaMutationKind.Reorder) { requestId ->
                                    viewModel.moveAreaMutation(requestId, id, direction)
                                }
                            },
                            onMoveItems = { moveItemsSourceId = it },
                            onMerge = { mergeSourceId = it },
                            onArchive = { archiveId = it },
                            onDelete = { deleteId = it },
                            onRestore = { id ->
                                submitAreaMutation(AreaMutationKind.Restore) { requestId ->
                                    viewModel.setAreaArchivedMutation(requestId, id, false)
                                }
                            },
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
        onDismiss = { if (!saving(AreaMutationKind.Create)) createOpen = false },
        onCreate = viewModel::createArea,
        onSelected = { _, _ -> createOpen = false },
        controlledSaving = saving(AreaMutationKind.Create),
        controlledError = error(AreaMutationKind.Create),
        onCreateRequested = { name, color ->
            submitAreaMutation(AreaMutationKind.Create) { requestId ->
                viewModel.createAreaMutation(requestId, name, color)
            }
        },
    )
    renameId?.let { id -> state.areas.firstOrNull { it.id == id }?.let { area ->
        RenameAreaDialog(
            modifier = childDialogModifier,
            area = area,
            existingAreas = state.areas,
            onDismiss = { if (!saving(AreaMutationKind.Rename)) renameId = null },
            saving = saving(AreaMutationKind.Rename),
            error = error(AreaMutationKind.Rename),
            onRename = { name ->
                submitAreaMutation(AreaMutationKind.Rename) { requestId ->
                    viewModel.renameAreaMutation(requestId, area.id, name)
                }
            },
        )
    } }
    colorId?.let { id -> state.areas.firstOrNull { it.id == id }?.let { area ->
        AreaColorDialog(
            modifier = childDialogModifier,
            area = area,
            saving = saving(AreaMutationKind.Color),
            error = error(AreaMutationKind.Color),
            onDismiss = { if (!saving(AreaMutationKind.Color)) colorId = null },
        ) { color ->
            submitAreaMutation(AreaMutationKind.Color) { requestId ->
                viewModel.setAreaColorMutation(requestId, area.id, color)
            }
        }
    } }
    mergeSourceId?.let { id -> state.areas.firstOrNull { it.id == id }?.let { source ->
        MergeAreaDialog(
            childDialogModifier,
            source,
            active.filter { it.id != id },
            state.areaUsage[id] ?: AreaUsageCounts(),
            saving = saving(AreaMutationKind.Merge),
            error = error(AreaMutationKind.Merge),
            onDismiss = { if (!saving(AreaMutationKind.Merge)) mergeSourceId = null },
        ) { target ->
            submitAreaMutation(AreaMutationKind.Merge) { requestId ->
                viewModel.mergeAreasMutation(requestId, source.id, target.id)
            }
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
                saving = saving(AreaMutationKind.MoveItems),
                error = error(AreaMutationKind.MoveItems),
                onDismiss = { if (!saving(AreaMutationKind.MoveItems)) moveItemsSourceId = null },
                onMove = { targetId ->
                    submitAreaMutation(AreaMutationKind.MoveItems) { requestId ->
                        viewModel.moveAllAreaItemsMutation(requestId, sourceId, targetId)
                    }
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
                onDismissRequest = { if (!saving(AreaMutationKind.Archive)) archiveId = null },
                title = { Text("Archive ${area.name}?") },
                text = {
                    WhipDialogBody {
                        Text(if (usage.total == 0) "It will be hidden from area pickers." else "${usageText(usage)} will keep this assignment. The area will be hidden from pickers until restored.")
                        error(AreaMutationKind.Archive)?.let { message ->
                            Text(message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = { WhipTextButton(
                    enabled = !saving(AreaMutationKind.Archive),
                    onClick = {
                        submitAreaMutation(AreaMutationKind.Archive) { requestId ->
                            viewModel.setAreaArchivedMutation(requestId, id, true)
                        }
                    }
                ) { Text(if (saving(AreaMutationKind.Archive)) "Archiving…" else "Archive") } },
                dismissButton = { WhipTextButton(
                    enabled = !saving(AreaMutationKind.Archive),
                    onClick = { archiveId = null },
                ) { Text("Cancel") } },
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
                saving = mutationCoordinator.saving && pendingAction in setOf(
                    AreaMutationKind.DeleteKeepingItems.name,
                    AreaMutationKind.DeleteWithItems.name,
                ),
                error = mutationCoordinator.errorMessage.takeIf {
                    pendingAction in setOf(
                        AreaMutationKind.DeleteKeepingItems.name,
                        AreaMutationKind.DeleteWithItems.name,
                    )
                },
                onDismiss = { if (!mutationCoordinator.saving) deleteId = null },
                onMoveItems = { targetId ->
                    submitAreaMutation(AreaMutationKind.DeleteKeepingItems) { requestId ->
                        viewModel.deleteAreaKeepingItemsMutation(requestId, id, targetId)
                    }
                },
                onDeleteItems = {
                    submitAreaMutation(AreaMutationKind.DeleteWithItems) { requestId ->
                        viewModel.deleteAreaWithItemsMutation(requestId, id)
                    }
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
    val visibleActive = active.filter { query.isBlank() || it.name.contains(query, true) }
    val visibleArchived = archived.filter { query.isBlank() || it.name.contains(query, true) }
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
                if (!reordering && active.size > 1) {
                    WhipPageIconAction(
                        icon = Icons.Outlined.DragHandle,
                        label = if (query.isBlank()) "Reorder Areas" else "Clear Search and reorder all Areas",
                        onClick = {
                            if (query.isNotBlank()) onQueryChange("")
                            reordering = true
                        },
                    )
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
        if (!reordering && query.isNotBlank() && visibleActive.isEmpty() && visibleArchived.isEmpty()) item {
            WhipEmptyState(
                title = "No Matching Areas",
                supportingText = "Try another name. Search includes active and archived Areas.",
            )
        }
        if (!reordering && archived.isNotEmpty() && query.isBlank()) item {
            HorizontalDivider(Modifier.padding(top = 8.dp))
            DisclosureButton(
                label = "Archived · ${archived.size}",
                expanded = archivedExpanded,
                onClick = { onArchivedExpandedChange(!archivedExpanded) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (!reordering && query.isNotBlank() && visibleArchived.isNotEmpty()) item {
            HorizontalDivider(Modifier.padding(top = 8.dp))
            Text(
                "Archived Matches · ${visibleArchived.size}",
                style = MaterialTheme.typography.titleSmall,
            )
        }
        if (!reordering && (archivedExpanded || query.isNotBlank())) itemsIndexed(
            visibleArchived,
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
    onRestore: (String) -> Unit,
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
                WhipOutlinedButton(
                    onClick = { if (area.archived) onRestore(area.id) else onArchive() },
                    modifier = Modifier.fillMaxWidth().testTag("area-detail-lifecycle-action"),
                ) {
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
    saving: Boolean = false,
    error: String? = null,
    onDismiss: () -> Unit,
    onMove: (String) -> Unit,
) {
    var targetId by rememberSaveable(sourceId) { mutableStateOf<String?>(null) }
    val options = targets.filterNot(Area::archived)
    val selected = options.firstOrNull { it.id == targetId }
    PaneAwareAlertDialog(
        modifier = modifier.testTag("move-area-items-dialog"),
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Move Everything from $sourceName") },
        text = {
            WhipChoiceList(Modifier.testTag("move-area-choice-list")) {
                item {
                    Text("Move ${usageText(usage)} together. The source area and all item history will remain unchanged.")
                }
                if (options.isEmpty()) {
                    item { Text("Create another active Area before moving these items.") }
                } else {
                    item { Text("Destination", style = MaterialTheme.typography.labelLarge) }
                    items(options, key = Area::id) { option ->
                        WhipSingleChoiceRow(
                            label = option.name,
                            selected = targetId == option.id,
                            onSelect = { targetId = option.id },
                            accessibilityLabel = stringResource(R.string.area_move_to_accessibility, option.name),
                        )
                    }
                }
                error?.let { message ->
                    item { Text(message, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            WhipTextButton(enabled = selected != null && !saving, onClick = { selected?.let { onMove(it.id) } }) {
                Text(if (saving) "Moving…" else "Move ${usage.total} Items")
            }
        },
        dismissButton = { WhipTextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun PermanentAreaDeleteDialog(
    modifier: Modifier = Modifier,
    area: Area,
    usage: AreaUsageCounts,
    replacementAreas: List<Area>,
    saving: Boolean = false,
    error: String? = null,
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
        modifier = modifier.testTag("permanent-area-delete-dialog"),
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Delete ${area.name} Permanently?") },
        text = {
            WhipChoiceList(Modifier.testTag("delete-area-choice-list")) {
                item { Text(explanation) }
                if (usage.total > 0) {
                    item { Text("Move items to", style = MaterialTheme.typography.labelLarge) }
                    items(replacementAreas, key = Area::id) { target ->
                        WhipSingleChoiceRow(
                            label = target.name,
                            selected = replacementId == target.id,
                            onSelect = { replacementId = target.id },
                            accessibilityLabel = stringResource(R.string.area_move_items_to_accessibility, target.name),
                        )
                    }
                }
                error?.let { message ->
                    item { Text(message, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (usage.total > 0) {
                    WhipTextButton(
                        enabled = replacement != null && !saving,
                        onClick = { replacement?.let { onMoveItems(it.id) } },
                    ) { Text(if (saving) "Deleting…" else "Move Items and Delete Area") }
                }
                WhipTextButton(
                    enabled = !saving,
                    onClick = onDeleteItems,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(if (usage.total == 0) "Delete Area" else "Delete Area and ${usage.total} Items")
                }
            }
        },
        dismissButton = { WhipTextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
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
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by rememberSaveable(area.id) { mutableStateOf(area.name) }
    val conflict = existingAreas.firstOrNull { it.id != area.id && it.name.equals(name.trim(), true) }
    PaneAwareAlertDialog(
        modifier = modifier.testTag("rename-area-dialog"),
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Rename Area") },
        text = { Column {
            OutlinedTextField(name, { name = it.take(40) }, label = { Text("Area name") }, supportingText = { Text("${name.length}/40") }, singleLine = true, isError = conflict != null || error != null, enabled = !saving, modifier = Modifier.testTag("rename-area-name"))
            conflict?.let {
                Text(
                    if (it.archived) {
                        "${it.name} already exists and is archived. Restore it before merging, or choose another name."
                    } else {
                        "${it.name} already exists. Cancel and use Merge instead."
                    },
                    color = MaterialTheme.colorScheme.error,
                )
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { WhipTextButton(enabled = name.isNotBlank() && conflict == null && !saving, onClick = {
            onRename(name.trim())
        }) { Text(if (saving) "Saving…" else "Rename") } },
        dismissButton = { WhipTextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AreaColorDialog(
    modifier: Modifier,
    area: Area,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSelect: (Long?) -> Unit,
) {
    WhipColorPickerDialog(
        modifier = modifier,
        title = "Color for ${area.name}",
        initialColor = area.colorArgb,
        onDismiss = onDismiss,
        onConfirm = onSelect,
        saving = saving,
        error = error,
    )
}

@Composable
private fun MergeAreaDialog(
    modifier: Modifier,
    source: Area,
    targets: List<Area>,
    usage: AreaUsageCounts,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onMerge: (Area) -> Unit,
) {
    var targetId by rememberSaveable { mutableStateOf<String?>(null) }
    val target = targets.firstOrNull { it.id == targetId }
    PaneAwareAlertDialog(
        modifier = modifier,
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Merge ${source.name}") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Move ${usageText(usage)} to another area. ${source.name} will then be removed. This cannot be undone.")
            if (targets.isEmpty()) Text("Create another active area before merging.") else AreaSelectionDropdown(targets, targetId, onSelect = { id, _ -> targetId = id })
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { WhipTextButton(enabled = target != null && !saving, onClick = { target?.let(onMerge) }) { Text(if (saving) "Merging…" else "Merge into ${target?.name ?: "…"}") } },
        dismissButton = { WhipTextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )
}
