package com.whip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whip.app.domain.WhipTag
import kotlinx.coroutines.launch

@Composable
internal fun TagManagementDialog(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    paneMaxWidth: Dp = 640.dp,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }
    var createOpen by rememberSaveable { mutableStateOf(false) }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var mergeSourceId by rememberSaveable { mutableStateOf<String?>(null) }
    var archiveId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingContext by rememberSaveable { mutableStateOf<String?>(null) }
    val mutationState by viewModel.tagMutationState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val active = state.tags.filterNot(WhipTag::archived)
    val archived = state.tags.filter(WhipTag::archived)
    val dialogModifier = Modifier.width(paneMaxWidth)

    lateinit var coordinator: EntitySaveCoordinator
    coordinator = rememberPersistenceRequestCoordinator(
        state = mutationState,
        consume = viewModel::consumeTagMutation,
        key = "tag-management",
        requestNamespace = "tag-management",
        onPersisted = { receipt ->
            val completedContext = pendingContext
            when (receipt.kind) {
                TagMutationKind.Create -> createOpen = false
                TagMutationKind.Rename -> renameId = null
                TagMutationKind.Merge -> mergeSourceId = null
                TagMutationKind.Archive -> archiveId = null
                TagMutationKind.Restore -> if (completedContext == "create-restore") createOpen = false
            }
            pendingContext = null
            scope.launch {
                val result = snackbar.showSnackbar(
                    message = when (receipt.kind) {
                        TagMutationKind.Create -> "Tag created"
                        TagMutationKind.Rename -> "Tag renamed everywhere"
                        TagMutationKind.Merge -> "Tags merged everywhere"
                        TagMutationKind.Archive -> "Tag archived"
                        TagMutationKind.Restore -> "Tag restored"
                    },
                    actionLabel = "Undo".takeIf { receipt.kind == TagMutationKind.Archive },
                )
                if (result == SnackbarResult.ActionPerformed && receipt.kind == TagMutationKind.Archive) {
                    val requestId = coordinator.begin() ?: return@launch
                    pendingContext = "undo-archive"
                    if (!viewModel.setTagArchivedMutation(requestId, receipt.tagId, false)) {
                        pendingContext = null
                        coordinator.finishFailure("Another Tag change is still finishing. Review Tags and try Restore again.")
                    }
                }
            }
        },
        orphanedMessage =
            "The previous Tag change was interrupted. Review the Tag names, references, and archive state before retrying.",
    )

    fun submit(context: String, mutation: (String) -> Boolean) {
        val requestId = coordinator.begin() ?: return
        pendingContext = context
        if (!mutation(requestId)) {
            pendingContext = null
            coordinator.finishFailure("Another Tag change is still finishing. Review Tags and try again.")
        }
    }
    fun saving(context: String): Boolean = coordinator.saving && pendingContext == context
    fun error(context: String): String? = coordinator.errorMessage.takeIf { pendingContext == context }

    BackHandler { if (!coordinator.saving) onDismiss() }
    WhipFullScreenSurface(title = "Tags") {
        Box(Modifier.fillMaxSize().testTag("tag-manager")) {
            Column(Modifier.fillMaxSize()) {
                TagManagerHeader(
                    saving = coordinator.saving,
                    onCreate = { createOpen = true },
                    onClose = onDismiss,
                )
                HorizontalDivider()
                if (coordinator.saving) {
                    WhipStatusCard(
                        kind = WhipStatusKind.Loading,
                        title = "Saving Tag Change",
                        message = "Whip is confirming the exact Tag and every current reference before this action finishes.",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                            .testTag("tag-mutation-saving"),
                    )
                } else coordinator.errorMessage?.let { message ->
                    WhipStatusCard(
                        kind = WhipStatusKind.Error,
                        title = "Tag Change Not Saved",
                        message = message,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                            .testTag("tag-mutation-error"),
                    )
                }
                TagList(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = state,
                    active = active,
                    archived = archived,
                    actionsEnabled = !coordinator.saving,
                    query = query,
                    onQueryChange = { query = it.take(40) },
                    archivedExpanded = archivedExpanded,
                    onArchivedExpandedChange = { archivedExpanded = it },
                    onRename = { renameId = it },
                    onMerge = { mergeSourceId = it },
                    onArchive = { archiveId = it },
                    onRestore = { tagId ->
                        submit("restore-$tagId") { requestId ->
                            viewModel.setTagArchivedMutation(requestId, tagId, false)
                        }
                    },
                )
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }

    if (createOpen) {
        CreateTagDialog(
            modifier = dialogModifier,
            existingTags = state.tags,
            saving = coordinator.saving && pendingContext in setOf("create", "create-restore"),
            error = coordinator.errorMessage.takeIf { pendingContext in setOf("create", "create-restore") },
            onErrorCleared = coordinator::clear,
            onDismiss = { if (!coordinator.saving) createOpen = false },
            onCreate = { name ->
                submit("create") { requestId -> viewModel.createTagMutation(requestId, name) }
            },
            onRestore = { tagId ->
                submit("create-restore") { requestId ->
                    viewModel.setTagArchivedMutation(requestId, tagId, false)
                }
            },
        )
    }

    renameId?.let { tagId ->
        state.tags.firstOrNull { it.id == tagId }?.let { tag ->
            RenameTagDialog(
                modifier = dialogModifier,
                tag = tag,
                existingTags = state.tags,
                saving = saving("rename"),
                error = error("rename"),
                onErrorCleared = coordinator::clear,
                onDismiss = { if (!coordinator.saving) renameId = null },
                onRename = { name ->
                    submit("rename") { requestId -> viewModel.renameTagMutation(requestId, tag.id, name) }
                },
            )
        }
    }

    mergeSourceId?.let { sourceId ->
        state.tags.firstOrNull { it.id == sourceId }?.let { source ->
            MergeTagDialog(
                modifier = dialogModifier,
                source = source,
                usage = state.tagUsage[source.id] ?: TagUsageCounts(),
                targets = active.filter { it.id != source.id },
                saving = saving("merge"),
                error = error("merge"),
                onDismiss = { if (!coordinator.saving) mergeSourceId = null },
                onMerge = { targetId ->
                    submit("merge") { requestId ->
                        viewModel.mergeTagsMutation(requestId, source.id, targetId)
                    }
                },
            )
        }
    }

    archiveId?.let { tagId ->
        state.tags.firstOrNull { it.id == tagId }?.let { tag ->
            val usage = state.tagUsage[tag.id] ?: TagUsageCounts()
            PaneAwareAlertDialog(
                modifier = dialogModifier.testTag("archive-tag-dialog"),
                onDismissRequest = { if (!saving("archive")) archiveId = null },
                title = { Text("Archive #${tag.name}?") },
                text = {
                    WhipDialogBody {
                        Text(
                            if (usage.total == 0) {
                                "It will move to Archived and remain available to restore."
                            } else {
                                "${tagUsageText(usage)} keep this label and remain searchable. The Tag moves out of the active list until restored."
                            },
                        )
                        error("archive")?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                },
                confirmButton = {
                    WhipTextButton(
                        enabled = !saving("archive"),
                        onClick = {
                            submit("archive") { requestId ->
                                viewModel.setTagArchivedMutation(requestId, tag.id, true)
                            }
                        },
                    ) { Text(if (saving("archive")) "Archiving…" else "Archive") }
                },
                dismissButton = {
                    WhipTextButton(enabled = !saving("archive"), onClick = { archiveId = null }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

@Composable
private fun TagManagerHeader(
    saving: Boolean,
    onCreate: () -> Unit,
    onClose: () -> Unit,
) {
    BoxWithConstraints(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        val stacked = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.5f
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Tags",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                    WhipTrailingCloseAction(
                        label = "Close Tags",
                        enabled = !saving,
                        onClick = onClose,
                        modifier = Modifier.testTag("tag-close-action"),
                    )
                }
                Text(
                    "Flexible labels shared by Tasks, Habits, Goals, and Tracks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WhipButton(
                    enabled = !saving,
                    onClick = onCreate,
                    modifier = Modifier.fillMaxWidth().testTag("create-tag-action"),
                ) { Text("Create Tag") }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Tags", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Flexible labels shared by Tasks, Habits, Goals, and Tracks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                WhipButton(
                    enabled = !saving,
                    onClick = onCreate,
                    modifier = Modifier.testTag("create-tag-action"),
                ) { Text("Create Tag") }
                WhipTrailingCloseAction(
                    label = "Close Tags",
                    enabled = !saving,
                    onClick = onClose,
                    modifier = Modifier.testTag("tag-close-action"),
                )
            }
        }
    }
}

@Composable
private fun TagList(
    modifier: Modifier,
    state: SettingsUiState,
    active: List<WhipTag>,
    archived: List<WhipTag>,
    actionsEnabled: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    archivedExpanded: Boolean,
    onArchivedExpandedChange: (Boolean) -> Unit,
    onRename: (String) -> Unit,
    onMerge: (String) -> Unit,
    onArchive: (String) -> Unit,
    onRestore: (String) -> Unit,
) {
    val visibleActive = active.filter { query.isBlank() || it.name.contains(query, true) }
    val visibleArchived = archived.filter { query.isBlank() || it.name.contains(query, true) }
    LazyColumn(
        modifier = modifier.testTag("tag-manager-list"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 16.dp, 20.dp, 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            WhipPageHeader(
                title = "Your Tags",
                supportingText = "Rename updates current references. Merge is a separate, explicit action.",
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Find Tag") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("tag-search"),
            )
        }
        if (visibleActive.isEmpty() && query.isBlank()) {
            item {
                WhipEmptyState(
                    title = "No Active Tags",
                    supportingText = "Create a Tag here or type one while editing an item.",
                )
            }
        }
        items(visibleActive, key = WhipTag::id) { tag ->
            TagRow(
                tag = tag,
                usage = state.tagUsage[tag.id] ?: TagUsageCounts(),
                activeTagCount = active.size,
                actionsEnabled = actionsEnabled,
                onRename = { onRename(tag.id) },
                onMerge = { onMerge(tag.id) },
                onArchive = { onArchive(tag.id) },
            )
        }
        if (query.isNotBlank() && visibleActive.isEmpty() && visibleArchived.isEmpty()) {
            item {
                WhipEmptyState(
                    title = "No Matching Tags",
                    supportingText = "Try another name. Search includes active and archived Tags.",
                )
            }
        }
        if (archived.isNotEmpty() && query.isBlank()) {
            item {
                HorizontalDivider(Modifier.padding(top = 8.dp))
                DisclosureButton(
                    label = "Archived · ${archived.size}",
                    expanded = archivedExpanded,
                    onClick = { onArchivedExpandedChange(!archivedExpanded) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (query.isNotBlank() && visibleArchived.isNotEmpty()) {
            item {
                HorizontalDivider(Modifier.padding(top = 8.dp))
                Text("Archived Matches · ${visibleArchived.size}", style = MaterialTheme.typography.titleSmall)
            }
        }
        if (archivedExpanded || query.isNotBlank()) {
            items(visibleArchived, key = { "archived-${it.id}" }) { tag ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("#${tag.name}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Archived · ${tagUsageText(state.tagUsage[tag.id] ?: TagUsageCounts())}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        WhipTextButton(
                            enabled = actionsEnabled,
                            onClick = { onRestore(tag.id) },
                        ) { Text("Restore") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagRow(
    tag: WhipTag,
    usage: TagUsageCounts,
    activeTagCount: Int,
    actionsEnabled: Boolean,
    onRename: () -> Unit,
    onMerge: () -> Unit,
    onArchive: () -> Unit,
) {
    var menuOpen by rememberSaveable(tag.id) { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("#${tag.name}", style = MaterialTheme.typography.titleMedium)
                Text(
                    tagUsageText(usage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            WhipOverflowMenu(
                label = "Options for #${tag.name}",
                expanded = menuOpen,
                onExpandedChange = { menuOpen = it },
                modifier = Modifier.testTag("tag-menu-${tag.id}"),
                enabled = actionsEnabled,
            ) {
                WhipMenuItem(
                    "Rename",
                    enabled = actionsEnabled,
                    onClick = { menuOpen = false; onRename() },
                )
                if (activeTagCount > 1) {
                    WhipMenuItem(
                        "Merge",
                        enabled = actionsEnabled,
                        onClick = { menuOpen = false; onMerge() },
                    )
                }
                WhipMenuItem(
                    "Archive",
                    enabled = actionsEnabled,
                    onClick = { menuOpen = false; onArchive() },
                )
            }
        }
    }
}

@Composable
private fun CreateTagDialog(
    modifier: Modifier,
    existingTags: List<WhipTag>,
    saving: Boolean,
    error: String?,
    onErrorCleared: () -> Unit,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onRestore: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    val duplicate = existingTags.firstOrNull { it.name.equals(name.trim(), true) }
    val invalidSeparator = ',' in name
    PaneAwareAlertDialog(
        modifier = modifier.testTag("create-tag-dialog"),
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Create Tag") },
        text = {
            WhipDialogBody {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40); onErrorCleared() },
                    label = { Text("Tag name") },
                    supportingText = { Text("${name.length}/40") },
                    singleLine = true,
                    enabled = !saving,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth().testTag("create-tag-name"),
                )
                duplicate?.let {
                    Text(
                        if (it.archived) {
                            "#${it.name} is archived. Restore the same Tag and its current references."
                        } else {
                            "#${it.name} is already active."
                        },
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (invalidSeparator) {
                    Text("Use separate Tags instead of commas.", color = MaterialTheme.colorScheme.error)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            when {
                duplicate?.archived == true -> WhipTextButton(
                    enabled = !saving,
                    onClick = { onRestore(duplicate.id) },
                ) { Text(if (saving) "Restoring…" else "Restore Existing Tag") }
                duplicate != null -> WhipTextButton(enabled = !saving, onClick = onDismiss) { Text("Done") }
                else -> WhipTextButton(
                    enabled = name.isNotBlank() && !invalidSeparator && !saving,
                    onClick = { onCreate(name.trim()) },
                ) { Text(if (saving) "Saving…" else "Create") }
            }
        },
        dismissButton = { WhipTextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RenameTagDialog(
    modifier: Modifier,
    tag: WhipTag,
    existingTags: List<WhipTag>,
    saving: Boolean,
    error: String?,
    onErrorCleared: () -> Unit,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by rememberSaveable(tag.id) { mutableStateOf(tag.name) }
    val conflict = existingTags.firstOrNull { it.id != tag.id && it.name.equals(name.trim(), true) }
    val invalidSeparator = ',' in name
    PaneAwareAlertDialog(
        modifier = modifier.testTag("rename-tag-dialog"),
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Rename #${tag.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Every current Task, Habit, Goal, and Track reference will use the new spelling.")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40); onErrorCleared() },
                    label = { Text("Tag name") },
                    supportingText = { Text("${name.length}/40") },
                    singleLine = true,
                    enabled = !saving,
                    isError = conflict != null || error != null,
                    modifier = Modifier.fillMaxWidth().testTag("rename-tag-name"),
                )
                conflict?.let {
                    Text(
                        if (it.archived) {
                            "#${it.name} already exists and is archived. Restore it before merging, or choose another name."
                        } else {
                            "#${it.name} already exists. Cancel and use Merge instead."
                        },
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (invalidSeparator) {
                    Text("Use separate Tags instead of commas.", color = MaterialTheme.colorScheme.error)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = name.isNotBlank() && !name.equals(tag.name, false) &&
                    conflict == null && !invalidSeparator && !saving,
                onClick = { onRename(name.trim()) },
            ) { Text(if (saving) "Saving…" else "Rename Everywhere") }
        },
        dismissButton = { WhipTextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MergeTagDialog(
    modifier: Modifier,
    source: WhipTag,
    usage: TagUsageCounts,
    targets: List<WhipTag>,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onMerge: (String) -> Unit,
) {
    var targetId by rememberSaveable(source.id) { mutableStateOf<String?>(null) }
    val target = targets.firstOrNull { it.id == targetId }
    PaneAwareAlertDialog(
        modifier = modifier.testTag("merge-tag-dialog"),
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Merge #${source.name}") },
        text = {
            WhipChoiceList(Modifier.testTag("merge-tag-choice-list")) {
                item {
                    Text(
                        "Replace #${source.name} with one active Tag on ${tagUsageText(usage)}. The source Tag is then removed; item history stays intact.",
                    )
                }
                if (targets.isEmpty()) {
                    item { Text("Create or restore another active Tag before merging.") }
                } else {
                    item { Text("Destination", style = MaterialTheme.typography.labelLarge) }
                    items(targets, key = WhipTag::id) { option ->
                        WhipSingleChoiceRow(
                            label = "#${option.name}",
                            selected = option.id == targetId,
                            onSelect = { targetId = option.id },
                            accessibilityLabel = "Merge #${source.name} into #${option.name}",
                        )
                    }
                }
                error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            }
        },
        confirmButton = {
            WhipTextButton(
                enabled = target != null && !saving,
                onClick = { target?.let { onMerge(it.id) } },
            ) { Text(if (saving) "Merging…" else "Merge into #${target?.name ?: "…"}") }
        },
        dismissButton = { WhipTextButton(enabled = !saving, onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun tagUsageText(usage: TagUsageCounts): String = buildList {
    if (usage.tasks > 0) add(tagCount(usage.tasks, "Task"))
    if (usage.habits > 0) add(tagCount(usage.habits, "Habit"))
    if (usage.goals > 0) add(tagCount(usage.goals, "Goal"))
    if (usage.tracks > 0) add(tagCount(usage.tracks, "Track"))
}.joinToString(" · ").ifBlank { "No current items" }

private fun tagCount(count: Int, noun: String): String = "$count $noun${if (count == 1) "" else "s"}"
