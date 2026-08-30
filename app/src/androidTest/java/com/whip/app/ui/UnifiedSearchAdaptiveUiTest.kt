package com.whip.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnifiedSearchAdaptiveUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compact320By480AtTwoXKeepsStickyQueryCloseAndScrollableResultsReachable() {
        val density = Density(density = compose.density.density, fontScale = 2f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                WhipTheme(dynamicColor = false) {
                    Box(Modifier.width(320.dp).height(480.dp)) {
                        SearchWorkspaceForTest(Modifier.fillMaxSize(), resultCount = 12)
                    }
                }
            }
        }

        compose.onNodeWithTag("unified-search-compact-workspace").assertIsDisplayed()
        compose.onAllNodesWithTag("unified-search-wide-workspace").assertCountEquals(0)
        compose.onNodeWithTag("unified-search-query").assertIsDisplayed()
        val root = compose.onNodeWithTag("unified-search-workspace").fetchSemanticsNode().boundsInRoot
        val query = compose.onNodeWithTag("unified-search-query").fetchSemanticsNode().boundsInRoot
        val sticky = compose.onNodeWithTag("unified-search-sticky-controls").fetchSemanticsNode().boundsInRoot
        val results = compose.onNodeWithTag("unified-search-results-pane").fetchSemanticsNode().boundsInRoot
        val close = compose.onNodeWithContentDescription("Close Search").fetchSemanticsNode().boundsInRoot
        val searchAll = compose.onNodeWithText("Search All Whip").fetchSemanticsNode().boundsInRoot
        val filters = compose.onNodeWithTag("search-filter-disclosure").fetchSemanticsNode().boundsInRoot

        assertEquals(320f * density.density, root.width, 1f)
        assertEquals(480f * density.density, root.height, 1f)
        assertTrue(query.top >= sticky.top && query.bottom <= sticky.bottom)
        assertTrue("results must scroll below the sticky controls", results.top >= sticky.bottom)
        assertTrue(close.center.x > root.center.x)
        assertTrue(close.width >= 48f * density.density)
        assertTrue(close.height >= 48f * density.density)
        assertTrue(searchAll.left >= root.left && searchAll.right <= root.right)
        assertTrue(filters.left >= root.left && filters.right <= root.right)
        compose.onNodeWithTag("unified-search-results-list")
            .performScrollToNode(hasTestTag("unified-search-result-Task-12"))
        compose.onNodeWithTag("unified-search-result-Task-12").assertIsDisplayed()
        compose.onNodeWithTag("unified-search-query").performClick().assertIsDisplayed()
    }

    @Test
    fun wideWorkspaceSeparatesControlsAndResultsAndExposesPoliteCount() {
        val density = Density(density = 1f, fontScale = 1f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                WhipTheme(dynamicColor = false) {
                    Box(Modifier.width(720.dp).height(520.dp)) {
                        SearchWorkspaceForTest(Modifier.fillMaxSize(), filtersExpanded = true)
                    }
                }
            }
        }

        compose.onNodeWithTag("unified-search-wide-workspace").assertIsDisplayed()
        compose.onAllNodesWithTag("unified-search-compact-workspace").assertCountEquals(0)
        val root = compose.onNodeWithTag("unified-search-workspace").fetchSemanticsNode().boundsInRoot
        val controls = compose.onNodeWithTag("unified-search-controls-pane").fetchSemanticsNode().boundsInRoot
        val results = compose.onNodeWithTag("unified-search-results-pane").fetchSemanticsNode().boundsInRoot
        val close = compose.onNodeWithContentDescription("Close Search").fetchSemanticsNode().boundsInRoot
        val announcement = compose.onNodeWithTag("unified-search-result-announcement").fetchSemanticsNode().config

        assertEquals(720f, root.width, 1f)
        assertTrue("controls and results must not overlap", controls.right <= results.left)
        assertTrue("results need more room than controls", results.width > controls.width)
        assertTrue("Close stays at the trailing edge", close.center.x > root.center.x)
        assertEquals(LiveRegionMode.Polite, announcement[SemanticsProperties.LiveRegion])
        assertEquals(
            listOf("1 search result for report. Scope Tasks."),
            announcement[SemanticsProperties.ContentDescription],
        )
    }

    @Test
    fun incompleteSelectedSourcesShowPartialStatusWithoutDefinitiveEmptyAnnouncement() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.width(360.dp).height(520.dp)) {
                    SearchWorkspaceForTest(
                        modifier = Modifier.fillMaxSize(),
                        resultCount = 0,
                        dataStatus = UnifiedSearchDataStatus(
                            loadingSources = listOf("Tracks"),
                            failedSources = listOf("Habits"),
                        ),
                    )
                }
            }
        }

        compose.onNodeWithText("Results incomplete").assertIsDisplayed()
        compose.onNodeWithText("Still loading · Tracks").assertIsDisplayed()
        compose.onNodeWithText("Couldn't load · Habits").assertIsDisplayed()
        compose.onNodeWithText("Results may be incomplete.").assertIsDisplayed()
        compose.onNodeWithText("No matches from loaded data yet.").assertIsDisplayed()
        compose.onAllNodesWithText("No matching items").assertCountEquals(0)
        val announcement = compose.onNodeWithTag("unified-search-result-announcement")
            .fetchSemanticsNode().config
        assertFalse(announcement.contains(SemanticsProperties.ContentDescription))
    }

    @Test
    fun hundredPlusResultsPaginateInReachableFiftyItemSteps() {
        val visibleResults = androidx.compose.runtime.mutableIntStateOf(50)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.width(360.dp).height(520.dp)) {
                    SearchWorkspaceForTest(
                        modifier = Modifier.fillMaxSize(),
                        resultCount = visibleResults.intValue,
                        matchingResultCount = 125,
                        onShowMore = {
                            visibleResults.intValue = (visibleResults.intValue + 50).coerceAtMost(125)
                        },
                    )
                }
            }
        }

        compose.onNodeWithTag("unified-search-results-list")
            .performScrollToNode(hasTestTag("unified-search-result-Task-50"))
        compose.onNodeWithTag("unified-search-result-Task-50").assertIsDisplayed()
        compose.onNodeWithText("Show 50 More · 75 Remaining").performClick()
        compose.onNodeWithTag("unified-search-results-list")
            .performScrollToNode(hasTestTag("unified-search-result-Task-100"))
        compose.onNodeWithTag("unified-search-result-Task-100").assertIsDisplayed()
        compose.onNodeWithText("Show 50 More · 25 Remaining").assertIsDisplayed()
    }

    @Test
    fun imeSearchActionRemainsAvailable() {
        var submitted = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.width(360.dp).height(520.dp)) {
                    SearchWorkspaceForTest(
                        modifier = Modifier.fillMaxSize(),
                        onSubmit = { submitted += 1 },
                    )
                }
            }
        }
        compose.onNodeWithTag("unified-search-query").performImeAction()
        compose.runOnIdle { assertEquals(1, submitted) }
    }

    @Test
    fun escapeDismissesTheProductionSearchDialog() {
        var dismissed = false
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                UnifiedSearchDialog(
                    taskState = TaskUiState(loading = false),
                    habitState = HabitUiState(loading = false),
                    goalState = GoalUiState(loading = false),
                    trackState = TrackUiState(loading = false),
                    gymState = GymUiState(loading = false),
                    onDismiss = { dismissed = true },
                    onSelect = {},
                )
            }
        }
        compose.onNodeWithTag("unified-search-query").performKeyInput {
            keyDown(Key.Escape)
            keyUp(Key.Escape)
        }
        compose.runOnIdle { assertTrue(dismissed) }
    }

    @Test
    fun compactProductionShellGivesSearchTheEntireContentPaneWidth() {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Box(Modifier.width(320.dp).height(480.dp)) {
                    WhipScreen(
                        state = TaskUiState(loading = false),
                        habitState = HabitUiState(loading = false),
                        goalState = GoalUiState(loading = false),
                        trackState = TrackUiState(loading = false),
                        gymState = GymUiState(loading = false),
                        modifier = Modifier.fillMaxSize(),
                        adaptiveLayout = WhipAdaptiveLayout.Compact,
                        onSaveTask = { _, _, _ -> },
                        onComplete = {},
                        onSkip = {},
                        onReschedule = { _, _ -> },
                        onArchive = {},
                        onReopen = {},
                    )
                }
            }
        }

        val contentWidth = compose.onNodeWithTag("workspace-top-app-bar")
            .fetchSemanticsNode().boundsInRoot.width
        if (compose.onAllNodesWithTag("workspace-search-action").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("workspace-search-action").performClick()
        } else {
            compose.onNodeWithContentDescription("App actions").performClick()
            compose.onNodeWithTag("workspace-search-menu-action").performClick()
        }
        val searchWidth = compose.onNodeWithTag("unified-search-surface")
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot.width

        assertTrue("Search must never be narrower than the active compact content", searchWidth >= contentWidth)
        assertTrue("Production search must not retain the old constrained-dialog width", searchWidth > contentWidth * 1.1f)
    }

    @Composable
    private fun SearchWorkspaceForTest(
        modifier: Modifier,
        filtersExpanded: Boolean = false,
        resultCount: Int = 1,
        matchingResultCount: Int = resultCount,
        dataStatus: UnifiedSearchDataStatus = UnifiedSearchDataStatus(),
        onSubmit: () -> Unit = {},
        onShowMore: () -> Unit = {},
    ) {
        val results = (1..resultCount).map { id ->
            WhipSearchResult(
                domain = SearchDomain.Task,
                id = id.toLong(),
                title = "Quarterly report $id",
                detail = "Final numbers",
            )
        }
        UnifiedSearchWorkspace(
            model = UnifiedSearchWorkspaceModel(
                query = "report",
                placeholder = "Search Tasks",
                scopeLabel = "Tasks",
                canSearchAllWhip = true,
                areaSummary = "Productivity: Work · Gym: All data",
                areaToggleLabel = "All Areas",
                domains = setOf(SearchDomain.Task),
                initialDomains = setOf(SearchDomain.Task),
                requireAllTerms = true,
                filtersExpanded = filtersExpanded,
                activeFilterCount = 0,
                results = results,
                matchingResultCount = matchingResultCount,
                queryStarted = true,
                searchSettled = true,
                resultAnnouncement = "$resultCount search ${if (resultCount == 1) "result" else "results"} for report. Scope Tasks.",
                dataStatus = dataStatus,
            ),
            modifier = modifier,
            onDismiss = {},
            onQueryChange = {},
            onSubmit = onSubmit,
            onSearchAllWhip = {},
            onToggleAreaScope = {},
            onToggleFilters = {},
            onDomainsChange = {},
            onRequireAllTermsChange = {},
            onSelect = {},
            onShowMore = onShowMore,
        )
    }
}
