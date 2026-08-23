package com.whip.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.time.LocalDate
import com.whip.app.domain.AreaScope
import com.whip.app.domain.matches
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class SearchDomain { Task, Habit, Goal, Exercise, Machine, Workout, Routine }

data class WhipSearchResult(
    val domain: SearchDomain,
    val id: Long,
    val title: String,
    val detail: String,
    val area: String = "",
    val areaId: String? = null,
    val tags: Set<String> = emptySet(),
    val date: LocalDate? = null,
    val deadline: LocalDate? = null,
    val status: String = "active",
)

@Composable
internal fun UnifiedSearchDialog(
    taskState: TaskUiState,
    habitState: HabitUiState,
    goalState: GoalUiState,
    gymState: GymUiState,
    onDismiss: () -> Unit,
    dialogModifier: Modifier = Modifier,
    areaScope: AreaScope = AreaScope.All,
    areaScopeLabel: String? = null,
    onSearchAllAreas: () -> Unit = {},
    initialScope: WhipSearchScope = WhipSearchEntryContext.AllWhip.defaultSearchScope(),
    onSelect: (WhipSearchResult) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var domains by rememberSaveable(initialScope.label) { mutableStateOf(initialScope.domains) }
    var requireAllTerms by rememberSaveable { mutableStateOf(true) }
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var visibleResults by rememberSaveable { mutableIntStateOf(50) }
    val queryFocusRequester = remember { FocusRequester() }
    val all = remember(taskState, habitState, goalState, gymState) {
        buildList {
            (taskState.inbox + taskState.today + taskState.upcoming + taskState.planning + taskState.anytime + taskState.completed + taskState.archived)
                .distinctBy { it.task.id }
                .forEach { item ->
                    add(
                        WhipSearchResult(
                            SearchDomain.Task,
                            item.task.id,
                            item.task.title,
                            (listOf(item.task.notes) + item.subtasks.map { it.step.title + " " + it.step.notes })
                                .filter(String::isNotBlank).joinToString(" · "),
                            area = item.task.area,
                            areaId = item.task.areaId,
                            tags = item.task.tags,
                            date = item.completedAtMillis?.let { millis -> java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
                                ?: item.scheduledDate,
                            deadline = item.task.deadline,
                            status = when {
                                item.task.archived -> "archived"
                                item.completedAtMillis != null -> "completed"
                                item.task.inbox -> "inbox"
                                item.isOverdue -> "overdue"
                                else -> "active"
                            },
                        ),
                    )
                }
            (habitState.all.map { it.habit } + habitState.archived).distinctBy { it.id }.forEach { habit ->
                val logText = habitState.logs.asSequence().filter { it.habitId == habit.id }
                    .joinToString(" · ") { "${it.localDate} ${it.value ?: it.status.name} ${it.note}" }
                add(WhipSearchResult(SearchDomain.Habit, habit.id, habit.name, listOf(habit.notes, logText).filter(String::isNotBlank).joinToString(" · "), area = habit.area, areaId = habit.areaId, tags = habit.tags.toSet(), status = if (habit.archived) "archived" else "active"))
            }
            (goalState.active + goalState.completed + goalState.archived).distinctBy { it.goal.id }.forEach { item ->
                val measurementText = item.entries.joinToString(" · ") { "${it.localDate} ${it.enteredValue ?: it.status.name} ${it.note}" }
                val contributionText = goalState.contributions.filter { contribution -> contribution.targetGoalId == item.goal.id }
                    .joinToString(" · ") { it.explanation }
                add(WhipSearchResult(SearchDomain.Goal, item.goal.id, item.goal.name, listOf(item.goal.description, measurementText, contributionText).filter(String::isNotBlank).joinToString(" · "), area = item.goal.area, areaId = item.goal.areaId, tags = item.goal.tags.toSet(), deadline = item.goal.deadline, status = item.goal.status.name.lowercase()))
            }
            (gymState.exercises + gymState.archivedExercises).distinctBy { it.id }.forEach { exercise ->
                add(WhipSearchResult(SearchDomain.Exercise, exercise.id, exercise.name, exercise.notes, status = if (exercise.archived) "archived" else "active"))
            }
            (gymState.machines + gymState.archivedMachines).distinctBy { it.id }.forEach { machine ->
                add(
                    WhipSearchResult(
                        SearchDomain.Machine,
                        machine.id,
                        machine.name,
                        listOf(machine.location, machine.details, machine.attachment, machine.seatPosition, machine.backPosition)
                            .filter(String::isNotBlank).joinToString(" · "),
                        status = if (machine.archived) "archived" else "active",
                    ),
                )
            }
            gymState.allSessions.distinctBy { it.id }.forEach { workout ->
                add(
                    WhipSearchResult(
                        SearchDomain.Workout,
                        workout.id,
                        workout.name.ifBlank { "Workout ${workout.localDate}" },
                        workout.notes,
                        date = workout.localDate,
                        status = workout.state.name.lowercase(),
                    ),
                )
            }
            (gymState.routines + gymState.archivedRoutines).distinctBy { it.id }.forEach { routine ->
                add(WhipSearchResult(SearchDomain.Routine, routine.id, routine.name, routine.notes, status = if (routine.archived) "archived" else "active"))
            }
        }
    }
    val explicitAreaOverride = query.trim().split(Regex("\\s+")).any { it.startsWith("area:", ignoreCase = true) }
    val matchingResults = if (query.isBlank()) emptyList() else all.filter { result ->
        val inScope = result.isVisibleInAreaScope(areaScope, explicitAreaOverride)
        inScope && result.domain in domains && result.matchesQuery(query, requireAllTerms)
    }.sortedWith(
        compareBy<WhipSearchResult> { it.searchRank(query) }
            .thenBy { it.title.lowercase() }
            .thenBy { it.domain.ordinal },
    )
    val results = matchingResults.take(visibleResults)
    LaunchedEffect(initialScope.label) { queryFocusRequester.requestFocus() }

    PaneAwareAlertDialog(
        modifier = dialogModifier,
        onDismissRequest = onDismiss,
        title = { Text(if (initialScope.isAllWhip) "Search Whip" else "Search ${initialScope.label}") },
        text = {
            Column(
                modifier = Modifier.onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        onDismiss()
                        true
                    } else if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && results.isNotEmpty()) {
                        onSelect(results.first())
                        true
                    } else false
                },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Scope: ${if (domains == SearchDomain.entries.toSet()) "All Whip" else domains.joinToString { it.name }}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    if (domains != SearchDomain.entries.toSet()) {
                        WhipTextButton(onClick = { domains = SearchDomain.entries.toSet() }) { Text("Search All Whip") }
                    }
                }
                areaScopeLabel?.let { label ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Productivity: $label · Gym: All data", style = MaterialTheme.typography.labelMedium)
                        WhipTextButton(onClick = onSearchAllAreas) { Text("All Areas") }
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; visibleResults = 50 },
                    label = { Text("Search ${if (domains == SearchDomain.entries.toSet()) "Whip" else domains.joinToString { it.name }}") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { results.firstOrNull()?.let(onSelect) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(queryFocusRequester).testTag("unified-search-query"),
                )
                DisclosureButton(
                    label = "Search Filters",
                    expanded = filtersExpanded,
                    onClick = { filtersExpanded = !filtersExpanded },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (filtersExpanded) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        WhipFilterChip(
                            selected = domains == SearchDomain.entries.toSet(),
                            onClick = { domains = SearchDomain.entries.toSet() },
                            label = { Text("All Types") },
                        )
                        SearchDomain.entries.forEach { domain ->
                            WhipFilterChip(
                                selected = domains != SearchDomain.entries.toSet() && domain in domains,
                                onClick = {
                                    domains = when {
                                        domains == SearchDomain.entries.toSet() -> setOf(domain)
                                        domain !in domains -> domains + domain
                                        domains.size > 1 -> domains - domain
                                        else -> SearchDomain.entries.toSet()
                                    }
                                },
                                label = { Text(domain.name) },
                            )
                        }
                    }
                }
                DisclosureButton(
                    label = "Advanced Search",
                    expanded = advancedExpanded,
                    onClick = { advancedExpanded = !advancedExpanded },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (advancedExpanded) {
                    WhipFilterChip(
                        selected = requireAllTerms,
                        onClick = { requireAllTerms = !requireAllTerms },
                        label = { Text(if (requireAllTerms) "Match All Terms" else "Match Any Term") },
                    )
                    Text(
                        "Use tag:work, area:home, status:completed, before:2026-09-01, after:2026-08-01, or deadline:true.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LazyColumn(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    if (query.isNotBlank() && results.isEmpty()) item { Text("No matching items", modifier = Modifier.padding(16.dp)) }
                    items(results, key = { "${it.domain}-${it.id}" }) { result ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                .clickable(onClickLabel = "Open ${result.title}") { onSelect(result) },
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(result.title, fontWeight = FontWeight.SemiBold)
                                Text(
                                    result.domain.name + result.area.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty() + result.detail.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                    if (visibleResults < matchingResults.size) item {
                        WhipTextButton(
                            onClick = { visibleResults = (visibleResults + 50).coerceAtMost(matchingResults.size) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Show 50 More · ${matchingResults.size - visibleResults} Remaining") }
                    }
                }
            }
        },
        confirmButton = { WhipTextButton(onClick = onDismiss) { Text("Close") } },
    )
}

internal fun WhipSearchResult.isVisibleInAreaScope(scope: AreaScope, explicitAreaOverride: Boolean): Boolean {
    val productivity = domain in setOf(SearchDomain.Task, SearchDomain.Habit, SearchDomain.Goal)
    return !productivity || explicitAreaOverride || scope.matches(areaId)
}

internal fun WhipSearchResult.matchesQuery(query: String, requireAllTerms: Boolean = true): Boolean {
    val tokens = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    val plain = mutableListOf<String>()
    var structuredCount = 0
    tokens.forEach { raw ->
        val token = raw.lowercase()
        val key = token.substringBefore(':', "")
        val value = token.substringAfter(':', "")
        when (key) {
            "tag" -> { structuredCount++; if (tags.none { it.equals(value, true) }) return false }
            "area" -> { structuredCount++; if (!area.contains(value, true)) return false }
            "status" -> { structuredCount++; if (!status.equals(value, true)) return false }
            "domain" -> { structuredCount++; if (!domain.name.equals(value, true)) return false }
            "before" -> {
                structuredCount++
                val boundary = parseDateOrNull(value) ?: return false
                if (date?.isBefore(boundary) != true) return false
            }
            "after" -> {
                structuredCount++
                val boundary = parseDateOrNull(value) ?: return false
                if (date?.isAfter(boundary) != true) return false
            }
            "deadline" -> {
                structuredCount++
                val required = value.toBooleanStrictOrNull() ?: return false
                if ((deadline != null) != required) return false
            }
            else -> {
                plain += token
            }
        }
    }
    if (plain.isEmpty()) return structuredCount > 0
    val haystack = listOf(domain.name, title, detail, area, tags.joinToString(" "), status, date?.toString().orEmpty(), deadline?.toString().orEmpty())
        .joinToString(" ").lowercase()
    return if (requireAllTerms) plain.all(haystack::contains) else plain.any(haystack::contains)
}

/** Stable user-facing rank: exact titles, title prefixes, title matches, then detail matches. */
internal fun WhipSearchResult.searchRank(query: String): Int {
    val plainQuery = query.trim().split(Regex("\\s+")).filterNot { ':' in it }.joinToString(" ").lowercase()
    if (plainQuery.isBlank()) return 3
    val normalizedTitle = title.trim().lowercase()
    return when {
        normalizedTitle == plainQuery -> 0
        normalizedTitle.startsWith(plainQuery) -> 1
        normalizedTitle.contains(plainQuery) -> 2
        else -> 3
    }
}

private fun parseDateOrNull(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()
