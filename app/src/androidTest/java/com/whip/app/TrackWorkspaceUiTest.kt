package com.whip.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
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
import com.whip.app.ui.GymUiState
import com.whip.app.ui.GoalUiState
import com.whip.app.ui.HabitUiState
import com.whip.app.ui.SettingsUiState
import com.whip.app.ui.TaskUiState
import com.whip.app.ui.LocalCompactItemLayout
import com.whip.app.ui.TrackRow
import com.whip.app.ui.TrackUiState
import com.whip.app.ui.TrackViewModel
import com.whip.app.ui.WhipAdaptiveLayout
import com.whip.app.ui.WhipScreen
import com.whip.app.ui.theme.WhipTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackWorkspaceUiTest {
    @get:Rule
    val compose = createComposeRule()

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
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val trackViewModel: TrackViewModel = viewModel()
                WhipScreen(
                    state = TaskUiState(loading = false),
                    habitState = HabitUiState(loading = false),
                    goalState = GoalUiState(loading = false),
                    gymState = GymUiState(loading = false),
                    trackState = TrackUiState(projections = listOf(movies, learning), currentDate = today, loading = false),
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

        compose.onNodeWithContentDescription("Search Track Activity").performClick()
        compose.onNodeWithTag("track-activity-search").performTextInput("Arrival")
        compose.onAllNodesWithText("Arrival").assertCountEquals(2)
        compose.onAllNodesWithText("Old Opening").assertCountEquals(0)
        compose.onNodeWithContentDescription("Clear Search").performClick()

        compose.onNodeWithContentDescription("Filter Track Activity").performClick()
        compose.onNodeWithText("7 Days").performClick()
        compose.onNodeWithText("Arrival").assertIsDisplayed()
        compose.onAllNodesWithText("Old Opening").assertCountEquals(0)
        compose.onNodeWithText("Any Date").performClick()
        compose.onNodeWithTag("track-activity-area-filter").performClick()
        compose.onNodeWithText("Work").performClick()
        compose.onNodeWithText("Old Opening").assertIsDisplayed()
        compose.onAllNodesWithText("Arrival").assertCountEquals(0)

        compose.onNodeWithTag("track-workspace-destination-Insights").performClick()
        compose.onNodeWithText("Entry Frequency").assertIsDisplayed()
        compose.onNodeWithText("Automation Status").assertIsDisplayed()
        compose.onNodeWithTag("track-workspace-insights-list").performScrollToNode(hasText("Recently Active Tracks"))
        compose.onNodeWithText("Recently Active Tracks").assertIsDisplayed()
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

        compose.onNodeWithContentDescription("Tracks tab").performClick()
        compose.onNodeWithTag("track-workspace-navigation").assertIsDisplayed()
        compose.onNodeWithTag("track-workspace-destination-Activity").performClick()
        compose.onNodeWithContentDescription("Search Track Activity").assertIsDisplayed()
        compose.onNodeWithContentDescription("Open Pages").performClick()
        compose.onNodeWithText("Insights").performClick()
        compose.onNodeWithText("Patterns and automation health across visible Tracks.").assertIsDisplayed()
    }

    @Test
    fun userSelectedCompactTrackRowsHideSecondaryControlsAndRemainEditable() {
        val compact = mutableStateOf(false)
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
                CompositionLocalProvider(LocalCompactItemLayout provides compact.value) {
                    TrackRow(projection, {}, {}, {})
                }
            }
        }

        val standardHeight = compose.onNodeWithTag("track-card-3").getUnclippedBoundsInRoot().let { it.bottom - it.top }
        compose.onAllNodesWithText("Latest:", substring = true).assertCountEquals(1)

        compose.runOnIdle { compact.value = true }
        compose.waitForIdle()

        val compactHeight = compose.onNodeWithTag("track-card-3").getUnclippedBoundsInRoot().let { it.bottom - it.top }
        assertTrue("Compact Track row should be shorter: $standardHeight vs $compactHeight", compactHeight < standardHeight)
        assertTrue(
            "Compact Track edit action must retain a 48 dp target",
            compose.onNodeWithTag("track-edit-action-3", useUnmergedTree = true).getUnclippedBoundsInRoot().let { it.bottom - it.top } >= 48.dp,
        )
        compose.onAllNodesWithText("Latest:", substring = true).assertCountEquals(0)
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
    ): TrackProjection {
        val titleFieldId = id * 10 + 1
        val scoreFieldId = id * 10 + 2
        return TrackProjection(
            track = Track(id, "track-$id", name, "", icon, areaId, area, emptyList(), false, false, 0, 1, 1),
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
