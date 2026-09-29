package com.whip.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.Area
import com.whip.app.domain.Track
import com.whip.app.domain.TrackEntry
import com.whip.app.domain.TrackEntryProjection
import com.whip.app.domain.TrackField
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackFieldValue
import com.whip.app.domain.TrackProjection
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.TrackChoiceOption
import com.whip.app.ui.GymUiState
import com.whip.app.ui.GoalUiState
import com.whip.app.ui.HabitUiState
import com.whip.app.ui.SettingsUiState
import com.whip.app.ui.TaskUiState
import com.whip.app.ui.TrackRow
import com.whip.app.ui.TrackUiState
import com.whip.app.ui.TrackViewModel
import com.whip.app.ui.WhipAdaptiveLayout
import com.whip.app.ui.WhipScreen
import com.whip.app.ui.theme.WhipTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackWorkspaceUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test fun numericIdentitiesAndEveryLatestTrackRemainReachableInInsights() {
        val today = LocalDate.of(2026, 9, 28)
        val unit = UnitDefinition("private-trail-unit", "trail lengths", "tl", UnitDimension.Distance, 2000.0, archived = true)
        val first = trackProjection(1, "Measured routes", "🥾", "main", "Main", 11, "Route", 2.5, today)
        val scoreField = first.fields.last().copy(name = "Distance identity", primary = true, required = true, dimension = UnitDimension.Distance, unitId = unit.id)
        val terrain = scoreField.copy(id = 13, uuid = "terrain-identity", name = "Terrain identity", type = TrackFieldType.SingleChoice,
            position = 2, dimension = null, unitId = null)
        val identityTrack = first.copy(fields = listOf(first.fields.first().copy(primary = false), scoreField, terrain),
            options = listOf(TrackChoiceOption(99, "trail-option", terrain.id, "Trail", 0, 1, 1)),
            entries = first.entries.map { entry -> entry.copy(values = entry.values + mapOf(
                scoreField.id to entry.values.getValue(scoreField.id).copy(enteredUnitId = unit.id, canonicalNumber = 5000.0),
                terrain.id to TrackFieldValue(113, "terrain-value", entry.entry.id, terrain.id, choiceOptionId = 99, createdAtMillis = 1, updatedAtMillis = 1))) })
        val tracks = listOf(identityTrack) + (2L..10L).map { id ->
            trackProjection(id, "Observed Track $id", "📓", "main", "Main", id * 10 + 1, "Entry $id", id.toDouble(), today.minusDays(id))
        }
        compose.setContent { WhipTheme(dynamicColor = false) {
            val trackViewModel: TrackViewModel = viewModel()
            WhipScreen(state = TaskUiState(loading = false), habitState = HabitUiState(loading = false, customUnits = listOf(unit)),
                goalState = GoalUiState(loading = false), gymState = GymUiState(loading = false),
                trackState = TrackUiState(projections = tracks, currentDate = today, loading = false), trackViewModel = trackViewModel,
                settingsState = SettingsUiState(areas = listOf(Area("main", "Main", null, 0, false, 1, 1)), customUnits = listOf(unit)),
                adaptiveLayout = WhipAdaptiveLayout.Compact, onSaveTask = { _, _, _ -> }, onComplete = {}, onSkip = {},
                onReschedule = { _, _ -> }, onArchive = {}, onReopen = {})
        } }
        compose.onNodeWithContentDescription("Tracks tab").performClick()
        compose.onNodeWithTag("track-workspace-destination-Insights").performClick()
        compose.onNodeWithTag("track-workspace-insights-list").performScrollToNode(hasText("2.5 tl · Trail"))
        compose.onNodeWithText("2.5 tl · Trail").assertIsDisplayed()
        compose.onNodeWithTag("track-workspace-insights-list").performScrollToNode(hasText("Showing 8 of 10 Tracks with Entries"))
        compose.onNodeWithText("Showing 8 of 10 Tracks with Entries").assertIsDisplayed()
        compose.onNodeWithTag("track-workspace-insights-list").performScrollToNode(androidx.compose.ui.test.hasTestTag("track-insights-more-latest"))
        compose.onNodeWithTag("track-insights-more-latest").performClick()
        compose.onNodeWithTag("track-workspace-insights-list").performScrollToNode(hasText("Observed Track 10"))
        compose.onNodeWithText("Observed Track 10").assertIsDisplayed()
        captureVisualCatalogSurface("ux-audit-2.tracks.complete-latest-list")
        compose.onNodeWithTag("track-workspace-insights-list").performScrollToNode(hasText("Measured routes"))
        compose.onNodeWithText("Measured routes").performClick()
        compose.onNodeWithTag("track-destination-Track Insights").assertIsSelected()
        compose.onNodeWithTag("track-insights-list").performScrollToNode(hasText("Distance identity"))
        compose.onNodeWithText("Distance identity").assertIsDisplayed()
        compose.onNodeWithTag("track-insights-list").performScrollToNode(hasText("2.5 tl"))
        compose.onAllNodesWithText("2.5 tl").fetchSemanticsNodes().let { assertTrue(it.isNotEmpty()) }
        captureVisualCatalogSurface("ux-audit-2.tracks.identity-field-insights")
        compose.onNodeWithTag("track-insights-list").performScrollToNode(hasText("Terrain identity"))
        compose.onNodeWithText("Terrain identity").assertIsDisplayed()
        compose.onNodeWithTag("track-insights-list").performScrollToNode(hasText("Trail"))
        compose.onNodeWithText("Trail").assertIsDisplayed()
    }

    @Test
    fun tracksAndEntriesHaveOneExplicitSearchOwnerAndArchivedResultsReturnToArchive() {
        val today = LocalDate.of(2026, 9, 2)
        val active = trackProjection(
            id = 1,
            name = "Fermentation Log",
            icon = "🥬",
            areaId = "personal",
            area = "Personal",
            entryId = 11,
            title = "Kimchi batch",
            score = 4.5,
            date = today,
        )
        val archived = trackProjection(
            id = 2,
            name = "Medication Archive",
            icon = "💊",
            areaId = "personal",
            area = "Personal",
            entryId = 22,
            title = "Prior dosage",
            score = 3.0,
            date = today.minusDays(30),
            archived = true,
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val trackViewModel: TrackViewModel = viewModel()
                WhipScreen(
                    state = TaskUiState(loading = false),
                    trackState = TrackUiState(projections = listOf(active, archived), currentDate = today, loading = false),
                    trackViewModel = trackViewModel,
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

        compose.onNodeWithContentDescription("Tracks tab").performClick()
        compose.onAllNodesWithContentDescription("Search Tracks & Entries").assertCountEquals(1)
        compose.onAllNodesWithContentDescription("Search Tracks").assertCountEquals(0)
        compose.onNodeWithContentDescription("Search Tracks & Entries").performClick()
        compose.onNodeWithText("Scope · Tracks & Entries").assertIsDisplayed()
        compose.onNodeWithText(
            "Search by name, note, tag, area, status, or date within Tracks & Entries.",
        ).assertIsDisplayed()

        compose.onNodeWithTag("unified-search-query").performTextReplacement("Kimchi batch")
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("unified-search-result-TrackEntry-11").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("unified-search-result-TrackEntry-11").assertIsDisplayed()

        compose.onNodeWithTag("unified-search-query").performTextReplacement("Prior dosage")
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("unified-search-result-TrackEntry-22").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("unified-search-result-TrackEntry-22").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("Close Track Entry details").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("track-workspace-destination-Tracks").assertIsSelected()
        compose.onAllNodesWithTag("track-detail-navigation").assertCountEquals(1)
        compose.onNodeWithTag("entity-inspector-title").assertTextContains("Prior dosage")
        compose.onAllNodesWithContentDescription("Edit Entry").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Edit Entry Prior dosage").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("More Actions for Prior dosage").assertCountEquals(0)
        compose.onNodeWithContentDescription("Close Track Entry details").performClick()
        compose.onNodeWithContentDescription("Back to Tracks").performClick()
        compose.onNodeWithTag("workspace-context-summary").assertTextContains("Archived Tracks")

        compose.onNodeWithContentDescription("Search Tracks & Entries").performClick()
        compose.onNodeWithTag("unified-search-query").performTextReplacement("Medication Archive")
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("unified-search-result-Track-2").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("unified-search-result-Track-2").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("track-detail-navigation").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("track-workspace-destination-Tracks").assertIsSelected()
        compose.onAllNodesWithText("Medication Archive")[0].assertIsDisplayed()
    }

    @Test
    fun activityFiltersAndCrossTrackInsightsUseVisibleTrackData() {
        val today = LocalDate.of(2026, 8, 24)
        val movies = trackProjection(
            id = 1,
            name = "Movies",
            icon = "🎬",
            areaId = "personal",
            area = "Personal",
            entryId = 11,
            title = "Arrival",
            score = 4.5,
            date = today,
        )
        val learning = trackProjection(
            id = 2,
            name = "Chess Openings",
            icon = "♟️",
            areaId = "work",
            area = "Work",
            entryId = 22,
            title = "Old Opening",
            score = 3.0,
            date = LocalDate.of(2015, 1, 5),
        )
        val visibleTracks = mutableStateOf(listOf(movies, learning))
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val trackViewModel: TrackViewModel = viewModel()
                WhipScreen(
                    state = TaskUiState(loading = false),
                    habitState = HabitUiState(loading = false),
                    goalState = GoalUiState(loading = false),
                    gymState = GymUiState(loading = false),
                    trackState = TrackUiState(projections = visibleTracks.value, currentDate = today, loading = false),
                    trackViewModel = trackViewModel,
                    settingsState = SettingsUiState(
                        areas = listOf(
                            Area("personal", "Personal", null, 0, false, 1, 1),
                            Area("work", "Work", null, 1, false, 1, 1),
                        ),
                    ),
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

        compose.onNodeWithContentDescription("Tracks tab").performClick()
        compose.onNodeWithTag("track-workspace-destination-Activity").performClick()
        compose.onNodeWithText("Arrival").assertIsDisplayed()
        compose.onNodeWithText("Old Opening").assertIsDisplayed()

        compose.onNodeWithContentDescription("Search Track Activity").assertDoesNotExist()
        compose.onNodeWithContentDescription("Filter Track Activity", substring = true).performClick()
        compose.onNodeWithTag("track-activity-area-filter").assertDoesNotExist()
        captureVisualCatalogSurface("tracks.activity.filters")
        compose.onNodeWithTag("track-activity-search").performTextInput("Arrival")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Old Opening").assertIsDisplayed()
        compose.onNodeWithContentDescription("Filter Track Activity", substring = true).performClick()
        compose.onNodeWithTag("track-activity-search").performTextReplacement("Arrival")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Apply").performClick()
        compose.onNodeWithText("Arrival").assertIsDisplayed()
        compose.onAllNodesWithText("Old Opening").assertCountEquals(0)

        compose.onNodeWithContentDescription("Filter Track Activity", substring = true).performClick()
        compose.onNodeWithText("Reset Filters").performClick()
        compose.onNodeWithText("7 Days").performScrollTo().performClick()
        compose.onNodeWithText("Apply").performClick()
        compose.onNodeWithText("Arrival").assertIsDisplayed()
        compose.onAllNodesWithText("Old Opening").assertCountEquals(0)
        compose.onNodeWithContentDescription("Filter Track Activity", substring = true).performClick()
        compose.onNodeWithText("Any Date").performScrollTo().performClick()
        compose.onNodeWithTag("track-activity-track-filter").performScrollTo().performClick()
        compose.onNodeWithText("♟️ Chess Openings").performClick()
        compose.onNodeWithText("Apply").performClick()
        compose.onNodeWithText("Old Opening").assertIsDisplayed()
        compose.onAllNodesWithText("Arrival").assertCountEquals(0)
        compose.onNodeWithTag("track-activity-filter-summary").assertIsDisplayed()

        compose.onNodeWithContentDescription("Filter Track Activity", substring = true).performClick()
        compose.onNodeWithTag("track-activity-search").performTextReplacement("no matching entry")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Apply").performClick()
        compose.onNodeWithText("No Matching Activity").assertIsDisplayed()
        captureVisualCatalogSurface("ux-upgrades.tracks.activity-recovery")
        compose.onNodeWithText("Clear Search & Filters").performClick()
        compose.onNodeWithText("Arrival").assertIsDisplayed()
        compose.onNodeWithText("Old Opening").assertIsDisplayed()
        compose.onAllNodesWithTag("track-activity-filter-summary").assertCountEquals(0)
        compose.onNodeWithContentDescription("Filter Track Activity", substring = true).performClick()
        compose.onNodeWithTag("track-activity-track-filter").performScrollTo().performClick()
        compose.onNodeWithText("♟️ Chess Openings").performClick()
        compose.onNodeWithText("Apply").performClick()
        compose.runOnIdle { visibleTracks.value = listOf(movies) }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Arrival"))
        compose.onNodeWithText("Arrival").assertIsDisplayed()
        compose.runOnIdle { visibleTracks.value = listOf(movies, learning) }

        compose.onNodeWithTag("track-workspace-destination-Insights").performClick()
        compose.onNodeWithText("Entry Frequency").assertIsDisplayed()
        compose.onNodeWithTag("track-workspace-insights-list").performScrollToNode(hasText("Latest Entries by Track"))
        compose.onNodeWithText("Latest Entries by Track").assertIsDisplayed()
        compose.onNodeWithTag("track-workspace-insights-list").performScrollToNode(hasText("Numeric Summaries"))
        compose.onNodeWithText("Numeric Summaries").assertIsDisplayed()
        compose.onNodeWithTag("track-workspace-insights-list").performScrollToNode(hasText("Total Entries"))
        compose.onNodeWithText("Total Entries").assertIsDisplayed()
    }

    @Test
    fun tracksWorkspaceRemainsNavigableInRtlAtTwoHundredPercentText() {
        val largeText = Density(compose.density.density, fontScale = 2f)
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides largeText,
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                WhipTheme(dynamicColor = false) {
                    val trackViewModel: TrackViewModel = viewModel()
                    WhipScreen(
                        state = TaskUiState(loading = false),
                        trackState = TrackUiState(loading = false),
                        trackViewModel = trackViewModel,
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

        compose.onAllNodesWithContentDescription("More destinations").assertCountEquals(0)
        compose.onNodeWithContentDescription("Tracks tab").performClick()
        compose.onNodeWithTag("track-workspace-navigation").assertIsDisplayed()
        compose.onNodeWithTag("track-workspace-destination-Activity").performClick()
        compose.onNodeWithContentDescription("Filter Track Activity").assertIsDisplayed()
        compose.onNodeWithTag("track-workspace-destination-Insights").performClick()
        compose.onNodeWithTag("workspace-context-summary").assertTextContains("Patterns across Tracks", substring = true)
    }

    @Test
    fun trackSummaryRowsKeepLatestAddAndEditActions() {
        var addedTrackId: Long? = null
        var editedTrackId: Long? = null
        val projection = trackProjection(
            id = 3,
            name = "Movies",
            icon = "🎬",
            areaId = "personal",
            area = "Personal",
            entryId = 33,
            title = "Arrival",
            score = 4.5,
            date = LocalDate.of(2026, 8, 24),
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackRow(projection, {}, { editedTrackId = it }, { addedTrackId = it })
            }
        }

        val summaryHeight = compose.onNodeWithTag("track-card-3").getUnclippedBoundsInRoot().let { it.bottom - it.top }
        assertTrue("Two-row Track summary should remain bounded: $summaryHeight", summaryHeight <= 96.dp)
        val titleBounds = compose.onNodeWithTag("track-card-title-3", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val identityBounds = compose.onNodeWithTag("track-icon-3", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val statusBounds = compose.onNodeWithText("Personal · 1 Entry", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val disclosureBounds = compose.onNodeWithTag("track-expand-3", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val primaryBounds = compose.onNodeWithTag("track-primary-action-3", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Track title must remain after the emoji", titleBounds.left > identityBounds.left)
        assertEquals(identityBounds.left.value, statusBounds.left.value, 0.5f)
        assertTrue("Track disclosure must precede its primary action", disclosureBounds.right <= primaryBounds.left)
        assertTrue(
            "Track primary action must retain a 48 dp target",
            compose.onNodeWithTag("track-primary-action-3", useUnmergedTree = true).getUnclippedBoundsInRoot().let { it.bottom - it.top } >= 48.dp,
        )
        compose.onAllNodesWithText("Latest:", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("Add Title").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Edit Track Movies").assertCountEquals(0)

        compose.onNodeWithContentDescription("Add Title").performClick()
        compose.onAllNodesWithText("Latest:", substring = true).assertCountEquals(0)
        compose.onNodeWithTag("track-expand-3", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Latest:", substring = true).assertIsDisplayed()
        compose.onAllNodesWithText("Add Title").assertCountEquals(0)
        val editHeight = compose.onNodeWithTag("track-edit-action-3", useUnmergedTree = true)
            .getUnclippedBoundsInRoot().let { it.bottom - it.top }
        assertTrue(
            "Expanded Track edit action must retain a 48 dp target: $editHeight",
            editHeight >= 48.dp - 0.01.dp,
        )
        compose.onNodeWithContentDescription("Edit Track Movies").performClick()
        compose.runOnIdle {
            assertTrue(addedTrackId == 3L)
            assertTrue(editedTrackId == 3L)
        }
    }

    @Test
    fun archivedTrackSummaryOmitsUnavailableAddAction() {
        val projection = trackProjection(
            id = 6,
            name = "Archived Reading",
            icon = "📚",
            areaId = "personal",
            area = "Personal",
            entryId = 66,
            title = "Finished book",
            score = 4.0,
            date = LocalDate.of(2026, 8, 24),
            archived = true,
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackRow(projection, {}, {}, {})
            }
        }

        compose.onNodeWithTag("track-card-6").assertIsDisplayed()
        compose.onAllNodesWithTag("track-primary-action-6", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Archived").assertCountEquals(0)
        compose.onNodeWithTag("track-expand-6", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun trackSelectionUsesOneCheckboxSemanticOwner() {
        val selected = mutableStateOf(false)
        val projection = trackProjection(
            id = 4,
            name = "Books",
            icon = "📚",
            areaId = "personal",
            area = "Personal",
            entryId = 44,
            title = "Kindred",
            score = 5.0,
            date = LocalDate.of(2026, 8, 24),
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackRow(
                    projection = projection,
                    onOpen = {},
                    onEdit = {},
                    onAddEntry = {},
                    selectionMode = true,
                    selected = selected.value,
                    onSelectionToggle = { selected.value = !selected.value },
                    compact = true,
                )
            }
        }

        val checkboxRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        compose.onAllNodes(checkboxRole, useUnmergedTree = true).assertCountEquals(1)
        assertEquals(
            Role.Checkbox,
            compose.onNodeWithTag("track-card-4", useUnmergedTree = true)
                .fetchSemanticsNode().config[SemanticsProperties.Role],
        )
        compose.onNodeWithTag("track-card-4").performClick()
        compose.runOnIdle { assertTrue(selected.value) }
        compose.onAllNodes(checkboxRole, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun adaptiveMasterPaneReorderKeepsItsDedicatedControls() {
        val projection = trackProjection(
            id = 5,
            name = "Reading",
            icon = "📚",
            areaId = "personal",
            area = "Personal",
            entryId = 55,
            title = "Parable",
            score = 4.0,
            date = LocalDate.of(2026, 8, 24),
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                TrackRow(
                    projection = projection,
                    onOpen = {},
                    onEdit = {},
                    onAddEntry = {},
                    onMove = {},
                    canMoveLater = true,
                    reorderPosition = 1,
                    reorderTotal = 2,
                    reordering = true,
                    compact = true,
                )
            }
        }

        compose.onNodeWithContentDescription("Reorder Reading").assertIsDisplayed()
        compose.onAllNodesWithTag("track-expand-5", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag("track-primary-action-5", useUnmergedTree = true).assertCountEquals(0)
    }

    private fun trackProjection(
        id: Long,
        name: String,
        icon: String,
        areaId: String,
        area: String,
        entryId: Long,
        title: String,
        score: Double,
        date: LocalDate,
        archived: Boolean = false,
    ): TrackProjection {
        val titleFieldId = id * 10 + 1
        val scoreFieldId = id * 10 + 2
        return TrackProjection(
            track = Track(id, "track-$id", name, "", icon, areaId, area, emptyList(), false, archived, 0, 1, 1),
            fields = listOf(
                TrackField(titleFieldId, "title-$id", id, "Title", TrackFieldType.ShortText, 0, true, true, true, null, null, 0, null, null, "", "", 1, 1),
                TrackField(scoreFieldId, "score-$id", id, "Score", TrackFieldType.Number, 1, false, false, true, null, null, 1, null, null, "", "", 1, 1),
            ),
            options = emptyList(),
            entries = listOf(
                TrackEntryProjection(
                    entry = TrackEntry(entryId, "entry-$entryId", id, date, createdAtMillis = entryId, updatedAtMillis = entryId),
                    values = mapOf(
                        titleFieldId to TrackFieldValue(entryId * 10 + 1, "value-title-$entryId", entryId, titleFieldId, textValue = title, createdAtMillis = 1, updatedAtMillis = 1),
                        scoreFieldId to TrackFieldValue(entryId * 10 + 2, "value-score-$entryId", entryId, scoreFieldId, enteredNumber = score, canonicalNumber = score, createdAtMillis = 1, updatedAtMillis = 1),
                    ),
                ),
            ),
        )
    }
}
