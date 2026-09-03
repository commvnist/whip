package com.whip.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.time.LocalDate
import com.whip.app.R
import com.whip.app.domain.AreaScope
import com.whip.app.domain.formatTrackScaleValue
import com.whip.app.domain.matches
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAlt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

enum class SearchDomain { Task, Habit, Goal, Track, TrackEntry, Exercise, Machine, Workout, Routine }

private const val MaxSearchResultsPerDomain = 2_000
private const val MaxSearchHistoryValuesPerEntity = 100
private const val MaxSearchEntriesPerTrack = 500

internal fun <T, C : Comparable<C>> newestSearchValues(
    values: List<T>,
    limit: Int,
    selector: (T) -> C,
): List<T> = values.asSequence().sortedByDescending(selector).take(limit).toList()

private fun SearchDomain.uiLabel(): String = if (this == SearchDomain.TrackEntry) "Track Entry" else name

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

internal enum class UnifiedSearchWorkspaceLayout { Compact, Wide }

internal fun unifiedSearchWorkspaceLayout(width: Dp, height: Dp): UnifiedSearchWorkspaceLayout =
    if (width >= 720.dp && height >= 440.dp) {
        UnifiedSearchWorkspaceLayout.Wide
    } else {
        UnifiedSearchWorkspaceLayout.Compact
    }

internal data class UnifiedSearchDataStatus(
    val loadingSources: List<String> = emptyList(),
    val failedSources: List<String> = emptyList(),
    val limitedSources: List<String> = emptyList(),
) {
    val complete: Boolean get() =
        loadingSources.isEmpty() && failedSources.isEmpty() && limitedSources.isEmpty()
}

internal data class BoundedSearchIndex(
    val results: List<WhipSearchResult>,
    val limitedDomains: Set<SearchDomain>,
)

internal class BoundedSearchIndexBuilder(
    private val maxResultsPerDomain: Int,
) {
    private val counts = mutableMapOf<SearchDomain, Int>()
    private val limited = linkedSetOf<SearchDomain>()
    private val results = mutableListOf<WhipSearchResult>()

    init {
        require(maxResultsPerDomain > 0)
    }

    fun add(result: WhipSearchResult) {
        val count = counts.getOrDefault(result.domain, 0)
        if (count < maxResultsPerDomain) {
            results += result
            counts[result.domain] = count + 1
        } else {
            limited += result.domain
        }
    }

    fun build(): BoundedSearchIndex = BoundedSearchIndex(results.toList(), limited.toSet())
}

internal fun buildBoundedSearchIndex(
    maxResultsPerDomain: Int,
    block: BoundedSearchIndexBuilder.() -> Unit,
): BoundedSearchIndex = BoundedSearchIndexBuilder(maxResultsPerDomain).apply(block).build()

/**
 * Apply the memory bound independently to every domain. A single large Task or
 * Track history must never prevent another selected domain from being indexed.
 * Callers surface [limitedDomains] as an incomplete result state rather than
 * presenting a definitive empty result from a partial index.
 */
internal fun boundSearchIndex(
    values: List<WhipSearchResult>,
    maxResultsPerDomain: Int,
): BoundedSearchIndex {
    return buildBoundedSearchIndex(maxResultsPerDomain) { values.forEach(::add) }
}

internal fun unifiedSearchDataStatus(
    domains: Set<SearchDomain>,
    taskState: TaskUiState,
    habitState: HabitUiState,
    goalState: GoalUiState,
    trackState: TrackUiState,
    gymState: GymUiState,
): UnifiedSearchDataStatus {
    val loadingSources = mutableListOf<String>()
    val failedSources = mutableListOf<String>()

    fun includeSource(selected: Boolean, label: String, loading: Boolean, errorMessage: String?) {
        if (!selected) return
        when {
            errorMessage != null -> failedSources += label
            loading -> loadingSources += label
        }
    }

    includeSource(SearchDomain.Task in domains, "Tasks", taskState.loading, taskState.errorMessage)
    includeSource(SearchDomain.Habit in domains, "Habits", habitState.loading, habitState.errorMessage)
    includeSource(SearchDomain.Goal in domains, "Goals", goalState.loading, goalState.errorMessage)
    includeSource(
        domains.any { it == SearchDomain.Track || it == SearchDomain.TrackEntry },
        "Tracks",
        trackState.loading,
        trackState.errorMessage,
    )
    includeSource(
        domains.any {
            it == SearchDomain.Exercise ||
                it == SearchDomain.Machine ||
                it == SearchDomain.Workout ||
                it == SearchDomain.Routine
        },
        "Gym",
        gymState.loading,
        gymState.errorMessage,
    )
    return UnifiedSearchDataStatus(loadingSources, failedSources)
}

private data class SearchResultsSnapshot(
    val requestKey: String,
    val results: List<WhipSearchResult>,
)

internal data class UnifiedSearchWorkspaceModel(
    val query: String,
    val placeholder: String,
    val scopeLabel: String,
    val canSearchAllWhip: Boolean,
    val areaSummary: String?,
    val areaToggleLabel: String?,
    val domains: Set<SearchDomain>,
    val initialDomains: Set<SearchDomain>,
    val requireAllTerms: Boolean,
    val filtersExpanded: Boolean,
    val activeFilterCount: Int,
    val results: List<WhipSearchResult>,
    val matchingResultCount: Int,
    val queryStarted: Boolean,
    val searchSettled: Boolean,
    val resultAnnouncement: String,
    val dataStatus: UnifiedSearchDataStatus = UnifiedSearchDataStatus(),
)

@Composable
internal fun UnifiedSearchDialog(
    taskState: TaskUiState,
    habitState: HabitUiState,
    goalState: GoalUiState,
    gymState: GymUiState,
    modifier: Modifier = Modifier,
    trackState: TrackUiState = TrackUiState(loading = false),
    onDismiss: () -> Unit,
    areaScope: AreaScope = AreaScope.All,
    areaScopeLabel: String? = null,
    initialScope: WhipSearchScope = WhipSearchEntryContext.AllWhip.defaultSearchScope(),
    onSelect: (WhipSearchResult) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var settledQuery by rememberSaveable { mutableStateOf("") }
    var domains by rememberSaveable(initialScope.label) { mutableStateOf(initialScope.domains) }
    var requireAllTerms by rememberSaveable { mutableStateOf(true) }
    var searchAllAreas by rememberSaveable { mutableStateOf(false) }
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    var visibleResults by rememberSaveable { mutableIntStateOf(50) }
    val queryFocusRequester = remember { FocusRequester() }
    val resources = LocalContext.current.resources
    val resourceConfiguration = LocalConfiguration.current
    val activeZoneId = LocalWhipZone.current
    val yesLabel = stringResource(R.string.search_yes)
    val noLabel = stringResource(R.string.search_no)
    val searchIndex by produceState<BoundedSearchIndex?>(
        null,
        taskState,
        habitState,
        goalState,
        gymState,
        trackState,
        resourceConfiguration,
        activeZoneId,
        yesLabel,
        noLabel,
    ) {
        value = withContext(Dispatchers.Default) {
            val recentHabitLogs = habitState.logs.groupBy { it.habitId }.mapValues { (_, logs) ->
                newestSearchValues(logs, MaxSearchHistoryValuesPerEntity) { it.timestamp }
            }
            buildBoundedSearchIndex(MaxSearchResultsPerDomain) {
            (taskState.inbox + taskState.today + taskState.upcoming + taskState.planning + taskState.completed + taskState.archived)
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
                            date = item.completedAtMillis?.let { millis -> completedTaskSearchDate(millis, activeZoneId) }
                                ?: item.scheduledDate,
                            deadline = item.task.deadline,
                            status = when {
                                item.task.archived -> "archived"
                                item.completedAtMillis != null -> "completed"
                                item.task.scheduleKind == com.whip.app.domain.ScheduleKind.Anytime -> "inbox"
                                item.isDeadlineOverdue -> "deadline overdue"
                                item.isPastScheduledDate -> "past scheduled date"
                                else -> "active"
                            },
                        ),
                    )
            }
            (habitState.all.map { it.habit } + habitState.archived).distinctBy { it.id }.forEach { habit ->
                val logText = recentHabitLogs[habit.id].orEmpty().asSequence()
                    .joinToString(" · ") { log ->
                        "${log.activityTitle(habit)} · ${log.activitySupportingText(habitState.currentDate)}"
                    }
                add(WhipSearchResult(SearchDomain.Habit, habit.id, habit.name, listOf(habit.notes, logText).filter(String::isNotBlank).joinToString(" · "), area = habit.area, areaId = habit.areaId, tags = habit.tags.toSet(), status = if (habit.archived) "archived" else "active"))
            }
            (goalState.active + goalState.completed + goalState.archived).distinctBy { it.goal.id }.forEach { item ->
                val measurementText = newestSearchValues(item.entries, MaxSearchHistoryValuesPerEntity) { it.timestamp }
                    .joinToString(" · ") { entry -> "${entry.historyTitle()} · ${entry.historySupportingText()}" }
                add(WhipSearchResult(SearchDomain.Goal, item.goal.id, item.goal.name, listOf(item.goal.description, measurementText).filter(String::isNotBlank).joinToString(" · "), area = item.goal.area, areaId = item.goal.areaId, tags = item.goal.tags.toSet(), deadline = item.goal.deadline, status = if (item.goal.archived) "archived" else item.goal.status.label.lowercase()))
            }
            trackState.projections.forEach { projection ->
                add(
                    WhipSearchResult(
                        SearchDomain.Track,
                        projection.track.id,
                        projection.track.name,
                        listOf(
                            projection.track.description,
                            projection.track.tags.joinToString(" "),
                            resources.getString(R.string.search_entry_count, projection.entries.size),
                        ).filter(String::isNotBlank).joinToString(" · "),
                        area = projection.track.area,
                        areaId = projection.track.areaId,
                        tags = projection.track.tags.toSet(),
                        date = projection.entries.maxOfOrNull { it.entry.entryDate },
                        status = if (projection.track.archived) "archived" else "active",
                    ),
                )
                newestSearchValues(projection.entries, MaxSearchEntriesPerTrack) { it.entry.createdAtMillis }.forEach { entry ->
                    add(
                        WhipSearchResult(
                            SearchDomain.TrackEntry,
                            entry.entry.id,
                            projection.primaryText(entry),
                            projection.fields.joinToString(" · ") { field ->
                                val value = entry.value(field.id)
                                when (field.type) {
                                    com.whip.app.domain.TrackFieldType.ShortText, com.whip.app.domain.TrackFieldType.LongText -> value?.textValue.orEmpty()
                                    com.whip.app.domain.TrackFieldType.Number -> value?.enteredNumber?.toString().orEmpty() + value?.enteredUnitId?.let { " $it" }.orEmpty()
                                    com.whip.app.domain.TrackFieldType.SingleChoice -> projection.options.firstOrNull { it.id == value?.choiceOptionId }?.label.orEmpty()
                                    com.whip.app.domain.TrackFieldType.Scale -> value?.scaleValue?.let(::formatTrackScaleValue).orEmpty()
                                    com.whip.app.domain.TrackFieldType.Date -> value?.dateValue?.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)).orEmpty()
                                    com.whip.app.domain.TrackFieldType.YesNo -> value?.booleanValue?.let { if (it) yesLabel else noLabel }.orEmpty()
                                }
                            },
                            area = projection.track.area,
                            areaId = projection.track.areaId,
                            tags = projection.track.tags.toSet(),
                            date = entry.entry.entryDate,
                            status = if (projection.track.archived) "archived" else "active",
                        ),
                    )
                }
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
                        workout.name.ifBlank { resources.getString(R.string.search_workout_title, workout.localDate) },
                        workout.notes,
                        date = workout.localDate,
                        status = workout.state.label.lowercase(),
                    ),
                )
            }
            (gymState.routines + gymState.archivedRoutines).distinctBy { it.id }.forEach { routine ->
                add(WhipSearchResult(SearchDomain.Routine, routine.id, routine.name, routine.notes, status = if (routine.archived) "archived" else "active"))
            }
            }
        }
    }
    val all = searchIndex?.results.orEmpty()
    LaunchedEffect(query) {
        if (query.isBlank()) {
            settledQuery = ""
        } else {
            // Avoid rebuilding and sorting the complete cross-feature result
            // set for every intermediate key event.
            delay(140)
            settledQuery = query
        }
    }
    val explicitAreaOverride = settledQuery.trim().split(Regex("\\s+")).any { it.startsWith("area:", ignoreCase = true) }
    val effectiveAreaScope = if (searchAllAreas || explicitAreaOverride) AreaScope.All else areaScope
    val searchRequestKey = listOf(
        settledQuery,
        domains.sortedBy(SearchDomain::ordinal).joinToString(",", transform = SearchDomain::name),
        effectiveAreaScope.toString(),
        requireAllTerms.toString(),
    ).joinToString("|")
    val matchingSnapshot by produceState(
        initialValue = SearchResultsSnapshot(requestKey = "", results = emptyList()),
        all,
        settledQuery,
        domains,
        effectiveAreaScope,
        requireAllTerms,
    ) {
        val matches = withContext(Dispatchers.Default) {
            if (settledQuery.isBlank()) {
                emptyList()
            } else {
                all.filter { result ->
                    val inScope = result.isVisibleInAreaScope(effectiveAreaScope, explicitAreaOverride)
                    inScope && result.domain in domains && result.matchesQuery(settledQuery, requireAllTerms)
                }.sortedWith(
                    compareBy<WhipSearchResult> { it.searchRank(settledQuery) }
                        .thenBy { it.title.lowercase() }
                        .thenBy { it.domain.ordinal },
                )
            }
        }
        value = SearchResultsSnapshot(searchRequestKey, matches)
    }
    val searchSettled = query == settledQuery && matchingSnapshot.requestKey == searchRequestKey
    val matchingResults = if (searchSettled) matchingSnapshot.results else emptyList()
    val results = matchingResults.take(visibleResults)
    val baseDataStatus = unifiedSearchDataStatus(
        domains = domains,
        taskState = taskState,
        habitState = habitState,
        goalState = goalState,
        trackState = trackState,
        gymState = gymState,
    )
    val dataStatus = baseDataStatus.copy(
        loadingSources = baseDataStatus.loadingSources +
            if (searchIndex == null) listOf(stringResource(R.string.search_index_source)) else emptyList(),
        limitedSources = searchIndex?.limitedDomains.orEmpty()
            .filter { it in domains }
            .sortedBy(SearchDomain::ordinal)
            .map(SearchDomain::uiLabel),
    )
    val resultsComplete = searchSettled && dataStatus.complete
    val activeFilterCount = (if (domains != initialScope.domains) 1 else 0) + (if (!requireAllTerms) 1 else 0)
    val resultScopeLabel = initialScope.displayLabel(domains)
    val completeResultAnnouncement = if (matchingResults.isEmpty()) {
        stringResource(R.string.search_no_results_announcement, settledQuery, resultScopeLabel)
    } else {
        pluralStringResource(
            R.plurals.search_result_announcement,
            matchingResults.size,
            matchingResults.size,
            settledQuery,
            resultScopeLabel,
        )
    }
    var resultAnnouncement by remember { mutableStateOf("") }
    var resultAnnouncementKey by remember { mutableStateOf("") }
    LaunchedEffect(searchRequestKey, matchingSnapshot, query, settledQuery, dataStatus, completeResultAnnouncement) {
        if (query.isBlank() || !resultsComplete) {
            resultAnnouncement = ""
            resultAnnouncementKey = ""
            return@LaunchedEffect
        }
        // Announce only after both query debouncing and background matching
        // have settled. This updates semantics in place and never requests focus.
        delay(180)
        resultAnnouncement = completeResultAnnouncement
        resultAnnouncementKey = searchRequestKey
    }
    LaunchedEffect(searchRequestKey) { visibleResults = 50 }
    LaunchedEffect(initialScope.label) { queryFocusRequester.requestFocus() }

    val workspaceModel = UnifiedSearchWorkspaceModel(
        query = query,
        placeholder = initialScope.placeholder(domains),
        scopeLabel = initialScope.displayLabel(domains),
        canSearchAllWhip = domains != SearchDomain.entries.toSet(),
        areaSummary = areaScopeLabel?.let { label ->
            when {
                explicitAreaOverride -> stringResource(R.string.search_area_all_override)
                searchAllAreas -> stringResource(R.string.search_area_all)
                else -> stringResource(R.string.search_area_current, label)
            }
        },
        areaToggleLabel = areaScopeLabel?.let {
            stringResource(if (searchAllAreas) R.string.search_current_area else R.string.search_all_areas)
        },
        domains = domains,
        initialDomains = initialScope.domains,
        requireAllTerms = requireAllTerms,
        filtersExpanded = filtersExpanded,
        activeFilterCount = activeFilterCount,
        results = results,
        matchingResultCount = matchingResults.size,
        queryStarted = query.isNotBlank(),
        searchSettled = searchSettled,
        resultAnnouncement = resultAnnouncement.takeIf {
            resultsComplete && resultAnnouncementKey == searchRequestKey
        }.orEmpty(),
        dataStatus = dataStatus,
    )
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BackHandler(onBack = onDismiss)
        Box(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            val workspaceModifier = if (modifier == Modifier) {
                Modifier.fillMaxSize()
            } else {
                modifier.fillMaxHeight()
            }
            WhipFullScreenSurface(
                title = stringResource(R.string.search_title),
                modifier = workspaceModifier.testTag("unified-search-surface"),
            ) {
                UnifiedSearchWorkspace(
                    model = workspaceModel,
                    modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                        when {
                            event.type == KeyEventType.KeyDown && event.key == Key.Escape -> {
                                onDismiss()
                                true
                            }
                            event.type == KeyEventType.KeyDown && event.key == Key.Enter && results.isNotEmpty() -> {
                                onSelect(results.first())
                                true
                            }
                            else -> false
                        }
                    },
                    queryModifier = Modifier.focusRequester(queryFocusRequester),
                    onDismiss = onDismiss,
                    onQueryChange = { query = it; visibleResults = 50 },
                    onSubmit = { results.firstOrNull()?.let(onSelect) },
                    onSearchAllWhip = { domains = SearchDomain.entries.toSet() },
                    onToggleAreaScope = { searchAllAreas = !searchAllAreas },
                    onToggleFilters = { filtersExpanded = !filtersExpanded },
                    onDomainsChange = { domains = it },
                    onRequireAllTermsChange = { requireAllTerms = it },
                    onSelect = onSelect,
                    onShowMore = { visibleResults = (visibleResults + 50).coerceAtMost(matchingResults.size) },
                )
            }
        }
    }
}

internal fun completedTaskSearchDate(completedAtMillis: Long, activeZoneId: java.time.ZoneId): java.time.LocalDate =
    java.time.Instant.ofEpochMilli(completedAtMillis).atZone(activeZoneId).toLocalDate()

@Composable
internal fun UnifiedSearchWorkspace(
    model: UnifiedSearchWorkspaceModel,
    modifier: Modifier = Modifier,
    queryModifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSearchAllWhip: () -> Unit,
    onToggleAreaScope: () -> Unit,
    onToggleFilters: () -> Unit,
    onDomainsChange: (Set<SearchDomain>) -> Unit,
    onRequireAllTermsChange: (Boolean) -> Unit,
    onSelect: (WhipSearchResult) -> Unit,
    onShowMore: () -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize().testTag("unified-search-workspace")) {
        val workspaceLayout = unifiedSearchWorkspaceLayout(maxWidth, maxHeight)
        val availableWidth = maxWidth
        Column(Modifier.fillMaxSize()) {
            UnifiedSearchHeader(onDismiss)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (workspaceLayout) {
                    UnifiedSearchWorkspaceLayout.Wide -> {
                        val controlsWidth = (availableWidth * 0.36f).coerceIn(320.dp, 400.dp)
                        Row(Modifier.fillMaxSize().testTag("unified-search-wide-workspace")) {
                        Surface(
                            modifier = Modifier.width(controlsWidth).fillMaxHeight().testTag("unified-search-controls-pane"),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Column(Modifier.fillMaxSize()) {
                                UnifiedSearchStickyControls(
                                    model = model,
                                    queryModifier = queryModifier,
                                    onQueryChange = onQueryChange,
                                    onSubmit = onSubmit,
                                    onSearchAllWhip = onSearchAllWhip,
                                    onToggleAreaScope = onToggleAreaScope,
                                    onToggleFilters = onToggleFilters,
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().testTag("unified-search-filter-pane"),
                                    contentPadding = PaddingValues(WhipSpacing.standard),
                                ) {
                                    item {
                                        UnifiedSearchFilters(
                                            model = model,
                                            onDomainsChange = onDomainsChange,
                                            onRequireAllTermsChange = onRequireAllTermsChange,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(
                            Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.outlineVariant)
                                .testTag("unified-search-pane-divider"),
                        )
                        UnifiedSearchResultsPane(
                            model = model,
                            modifier = Modifier.weight(1f).fillMaxHeight().testTag("unified-search-results-pane"),
                            includeFilters = false,
                            onDomainsChange = onDomainsChange,
                            onRequireAllTermsChange = onRequireAllTermsChange,
                            onSelect = onSelect,
                            onShowMore = onShowMore,
                        )
                        }
                    }
                    UnifiedSearchWorkspaceLayout.Compact -> {
                        Column(Modifier.fillMaxSize().testTag("unified-search-compact-workspace")) {
                        UnifiedSearchStickyControls(
                            model = model,
                            queryModifier = queryModifier,
                            onQueryChange = onQueryChange,
                            onSubmit = onSubmit,
                            onSearchAllWhip = onSearchAllWhip,
                            onToggleAreaScope = onToggleAreaScope,
                            onToggleFilters = onToggleFilters,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        UnifiedSearchResultsPane(
                            model = model,
                            modifier = Modifier.weight(1f).fillMaxWidth().testTag("unified-search-results-pane"),
                            includeFilters = true,
                            onDomainsChange = onDomainsChange,
                            onRequireAllTermsChange = onRequireAllTermsChange,
                            onSelect = onSelect,
                            onShowMore = onShowMore,
                        )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnifiedSearchHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = WhipSpacing.compact, vertical = WhipSpacing.sibling),
        horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.search_title),
            modifier = Modifier.weight(1f).semantics { heading() }.testTag("unified-search-title"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        WhipTrailingCloseAction(
            label = stringResource(R.string.search_close),
            onClick = onDismiss,
            modifier = Modifier.testTag("unified-search-close-action"),
        )
    }
}

@Composable
private fun UnifiedSearchStickyControls(
    model: UnifiedSearchWorkspaceModel,
    queryModifier: Modifier,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSearchAllWhip: () -> Unit,
    onToggleAreaScope: () -> Unit,
    onToggleFilters: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WhipSpacing.screenCompact, vertical = WhipSpacing.compact)
            .testTag("unified-search-sticky-controls"),
        verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
    ) {
        OutlinedTextField(
            value = model.query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.search_title)) },
            placeholder = { Text(model.placeholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            modifier = queryModifier.fillMaxWidth().testTag("unified-search-query"),
        )
        Text(
            stringResource(R.string.search_scope, model.scopeLabel),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
        ) {
            if (model.canSearchAllWhip) {
                WhipTextButton(
                    onClick = onSearchAllWhip,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("unified-search-all-whip"),
                ) { Text(stringResource(R.string.search_all_whip)) }
            }
            WhipTextButton(
                onClick = onToggleFilters,
                modifier = Modifier.heightIn(min = 48.dp).testTag("search-filter-disclosure"),
            ) {
                Icon(Icons.Outlined.FilterAlt, contentDescription = null)
                Text(
                    if (model.activeFilterCount == 0) {
                        stringResource(R.string.search_filters)
                    } else {
                        stringResource(R.string.search_filters_count, model.activeFilterCount)
                    },
                )
            }
        }
        model.areaSummary?.let { summary ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    summary,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                model.areaToggleLabel?.let { label ->
                    WhipTextButton(
                        onClick = onToggleAreaScope,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("unified-search-area-scope"),
                    ) { Text(label) }
                }
            }
        }
    }
}

@Composable
private fun UnifiedSearchFilters(
    model: UnifiedSearchWorkspaceModel,
    onDomainsChange: (Set<SearchDomain>) -> Unit,
    onRequireAllTermsChange: (Boolean) -> Unit,
) {
    val matchAllLabel = stringResource(R.string.search_match_all)
    val matchAnyLabel = stringResource(R.string.search_match_any)
    if (model.filtersExpanded) {
        Column(
            modifier = Modifier.fillMaxWidth().testTag("unified-search-filter-controls"),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
        ) {
            Text(stringResource(R.string.search_content_types), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
                verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
            ) {
                WhipFilterChip(
                    selected = model.domains == SearchDomain.entries.toSet(),
                    onClick = { onDomainsChange(SearchDomain.entries.toSet()) },
                    label = { Text(stringResource(R.string.search_all_types)) },
                    modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 92.dp),
                )
                SearchDomain.entries.forEach { domain ->
                    WhipFilterChip(
                        selected = model.domains != SearchDomain.entries.toSet() && domain in model.domains,
                        onClick = {
                            onDomainsChange(
                                when {
                                    model.domains == SearchDomain.entries.toSet() -> setOf(domain)
                                    domain !in model.domains -> model.domains + domain
                                    model.domains.size > 1 -> model.domains - domain
                                    else -> SearchDomain.entries.toSet()
                                },
                            )
                        },
                        label = { Text(domain.uiLabel()) },
                        modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 92.dp),
                    )
                }
            }
            Text(stringResource(R.string.search_terms), style = MaterialTheme.typography.labelLarge)
            SegmentedChoiceBar(
                selected = model.requireAllTerms,
                choices = listOf(true, false),
                onSelect = onRequireAllTermsChange,
                label = { if (it) matchAllLabel else matchAnyLabel },
                modifier = Modifier.fillMaxWidth(),
                testTagPrefix = "search-terms",
            )
            Text(
                stringResource(R.string.search_query_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else if (model.activeFilterCount > 0) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().testTag("unified-search-active-filters"),
            horizontalArrangement = Arrangement.spacedBy(WhipSpacing.sibling),
            verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
        ) {
            if (model.domains != model.initialDomains) {
                model.domains.sortedBy(SearchDomain::ordinal).forEach { domain ->
                    WhipFilterChip(
                        selected = true,
                        onClick = {
                            onDomainsChange(
                                if (model.domains.size == 1) model.initialDomains else model.domains - domain,
                            )
                        },
                        label = { Text(stringResource(R.string.search_removable_filter, domain.uiLabel())) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .widthIn(min = 92.dp)
                            .testTag("search-active-domain-${domain.name}"),
                    )
                }
            }
            if (!model.requireAllTerms) {
                WhipFilterChip(
                    selected = true,
                    onClick = { onRequireAllTermsChange(true) },
                    label = { Text(stringResource(R.string.search_removable_filter, matchAnyLabel)) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .widthIn(min = 104.dp)
                        .testTag("search-active-match-any"),
                )
            }
        }
    }
}

@Composable
private fun UnifiedSearchResultsPane(
    model: UnifiedSearchWorkspaceModel,
    modifier: Modifier,
    includeFilters: Boolean,
    onDomainsChange: (Set<SearchDomain>) -> Unit,
    onRequireAllTermsChange: (Boolean) -> Unit,
    onSelect: (WhipSearchResult) -> Unit,
    onShowMore: () -> Unit,
) {
    val resultsListState = rememberLazyListState()
    LaunchedEffect(model.filtersExpanded) {
        if (includeFilters) resultsListState.scrollToItem(0)
    }
    Column(modifier) {
        val resultsComplete = model.searchSettled && model.dataStatus.complete
        val resultsLabel = when {
            model.queryStarted && !model.searchSettled -> stringResource(R.string.search_searching)
            model.queryStarted && !resultsComplete && model.matchingResultCount > 0 ->
                stringResource(R.string.search_partial_results, model.matchingResultCount)
            model.queryStarted && !resultsComplete -> stringResource(R.string.search_results_incomplete)
            model.queryStarted -> stringResource(R.string.search_results_count, model.matchingResultCount)
            else -> stringResource(R.string.search_results)
        }
        Text(
            resultsLabel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WhipSpacing.screenCompact, vertical = WhipSpacing.compact)
                .semantics {
                    heading()
                    if (resultsComplete && model.resultAnnouncement.isNotBlank()) {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = model.resultAnnouncement
                    }
                }
                .testTag("unified-search-result-announcement"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("unified-search-results-list"),
            state = resultsListState,
            contentPadding = PaddingValues(
                start = WhipSpacing.screenCompact,
                end = WhipSpacing.screenCompact,
                bottom = WhipSpacing.screenExpanded,
            ),
        ) {
            if (includeFilters && (model.filtersExpanded || model.activeFilterCount > 0)) {
                item(key = "filters") {
                    UnifiedSearchFilters(
                        model = model,
                        onDomainsChange = onDomainsChange,
                        onRequireAllTermsChange = onRequireAllTermsChange,
                    )
                    HorizontalDivider(Modifier.padding(vertical = WhipSpacing.compact))
                }
            }
            if (!model.dataStatus.complete) {
                item(key = "data-status") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = WhipSpacing.compact)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag("unified-search-data-status"),
                        verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
                    ) {
                        if (model.dataStatus.loadingSources.isNotEmpty()) {
                            Text(
                                stringResource(
                                    R.string.search_loading_sources,
                                    model.dataStatus.loadingSources.joinToString(),
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (model.dataStatus.failedSources.isNotEmpty()) {
                            Text(
                                stringResource(
                                    R.string.search_failed_sources,
                                    model.dataStatus.failedSources.joinToString(),
                                ),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        if (model.dataStatus.limitedSources.isNotEmpty()) {
                            Text(
                                stringResource(
                                    R.string.search_limited_sources,
                                    model.dataStatus.limitedSources.joinToString(),
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            stringResource(R.string.search_results_may_be_incomplete),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            when {
                !model.queryStarted -> item(key = "start") {
                    Text(
                        stringResource(R.string.search_start_hint, model.scopeLabel),
                        modifier = Modifier.padding(vertical = WhipSpacing.standard),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                !model.searchSettled -> item(key = "searching") {
                    Text(
                        stringResource(R.string.search_searching),
                        modifier = Modifier.padding(vertical = WhipSpacing.standard),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                !model.dataStatus.complete && model.results.isEmpty() -> item(key = "incomplete-empty") {
                    Text(
                        stringResource(R.string.search_no_matches_loaded),
                        modifier = Modifier.padding(vertical = WhipSpacing.standard),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                model.results.isEmpty() -> item(key = "empty") {
                    Text(
                        stringResource(R.string.search_no_matches),
                        modifier = Modifier.padding(vertical = WhipSpacing.standard),
                    )
                }
            }
            items(model.results, key = { "${it.domain}-${it.id}" }) { result ->
                Surface(
                    onClick = { onSelect(result) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("unified-search-result-${result.domain}-${result.id}"),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = WhipSpacing.compact, vertical = WhipSpacing.sibling),
                        verticalArrangement = Arrangement.spacedBy(WhipSpacing.micro),
                    ) {
                        val statusLabel = result.status.takeUnless { it == "active" }?.let { searchStatusLabel(it) }
                        Text(result.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            buildList {
                                add(result.domain.uiLabel())
                                statusLabel?.let { add(it) }
                                result.area.takeIf(String::isNotBlank)?.let { add(it) }
                                result.detail.takeIf(String::isNotBlank)?.let { add(it) }
                            }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            if (model.results.size < model.matchingResultCount) {
                item(key = "show-more") {
                    WhipTextButton(
                        onClick = onShowMore,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(
                            stringResource(
                                R.string.search_show_more,
                                50,
                                model.matchingResultCount - model.results.size,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun searchStatusLabel(status: String): String = when (status) {
    "archived" -> stringResource(R.string.search_status_archived)
    "completed" -> stringResource(R.string.search_status_completed)
    "inbox" -> stringResource(R.string.search_status_inbox)
    "deadline overdue" -> stringResource(R.string.search_status_deadline_overdue)
    "past scheduled date" -> stringResource(R.string.search_status_past_scheduled_date)
    "paused" -> stringResource(R.string.search_status_paused)
    "discarded" -> stringResource(R.string.search_status_discarded)
    else -> status.uiTitleCase()
}

internal fun WhipSearchResult.isVisibleInAreaScope(scope: AreaScope, explicitAreaOverride: Boolean): Boolean {
    val productivity = domain in setOf(SearchDomain.Task, SearchDomain.Habit, SearchDomain.Goal, SearchDomain.Track, SearchDomain.TrackEntry)
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
            "domain" -> { structuredCount++; if (!domain.name.equals(value, true) && !domain.uiLabel().replace(" ", "").equals(value.replace(" ", ""), true)) return false }
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
    val haystack = listOf(domain.uiLabel(), title, detail, area, tags.joinToString(" "), status, date?.toString().orEmpty(), deadline?.toString().orEmpty())
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
