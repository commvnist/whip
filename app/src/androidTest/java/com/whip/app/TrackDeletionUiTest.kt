package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackDeletionUiTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private var trackId: Long = 0

    @Before
    fun prepareApp() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { it.copy(setupCompleted = true) }
        trackId = app.trackRepository.create(
            TrackDraft(
                name = "Delete review Track",
                fields = listOf(
                    TrackFieldDraft(
                        name = "Note",
                        type = TrackFieldType.ShortText,
                        primary = true,
                        required = true,
                    ),
                ),
            ),
        )
    }

    @Test
    fun deletionReviewSurvivesRecreationAndClosesOnlyAfterExactCommit() {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        ActivityScenario.launch<MainActivity>(
            Intent(app, MainActivity::class.java)
                .putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true),
        ).use { scenario ->
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription("Tracks tab").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("track-card-$trackId").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("track-card-$trackId").performClick()
            compose.onNodeWithTag("track-destination-Options").performClick()
            compose.onNodeWithText("Delete Track Permanently").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Delete Delete review Track Permanently?").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("1 Field", substring = true).assertIsDisplayed()

            scenario.recreate()

            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("track-permanent-delete-dialog").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Delete Delete review Track Permanently?").assertIsDisplayed()
            compose.onNodeWithText("Delete Permanently").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("track-permanent-delete-dialog").fetchSemanticsNodes().isEmpty()
            }
            compose.onAllNodesWithTag("track-card-$trackId").assertCountEquals(0)
        }
    }
}
