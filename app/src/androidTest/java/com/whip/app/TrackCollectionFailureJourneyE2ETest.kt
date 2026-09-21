package com.whip.app

import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackProjection
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real SQLite failures must roll back exactly and leave the native collection retryable. */
@RunWith(AndroidJUnit4::class)
class TrackCollectionFailureJourneyE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()

    @After fun clean() = runBlocking {
        val sql = app.database.openHelper.writableDatabase
        sql.execSQL("DROP TRIGGER IF EXISTS fail_track_reorder")
        sql.execSQL("DROP TRIGGER IF EXISTS fail_track_bulk_archive")
        app.backupRepository.deleteAllData()
    }

    @Test fun storageFailuresRestoreOrderAndKeepBulkSelectionForRetry() {
        val seeded = seed()
        val first = seeded[0]
        val second = seeded[1]
        val third = seeded[2]
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("track-card-${first.track.id}").fetchSemanticsNodes().isNotEmpty()
            }

            enterReorderMode()
            installReorderFailure()
            moveDown(first.track.name)
            compose.onNodeWithText(
                "Could not save the new Track order. Your previous order is unchanged. Try again.",
            ).assertIsDisplayed()
            assertEquals(seeded, projections(first, second, third))
            compose.onAllNodesWithContentDescription("Dismiss")[0].performClick()

            sql("DROP TRIGGER fail_track_reorder")
            moveDown(first.track.name)
            compose.waitUntil(10_000) {
                storedOrder() == listOf(second.track.id, first.track.id, third.track.id)
            }
            compose.onNodeWithText("Done").performClick()

            enterSelectionMode()
            row(first).performClick()
            row(second).performClick()
            compose.onNodeWithText("2 Tracks selected").assertIsDisplayed()
            val beforeFailedArchive = projections(first, second, third)
            installArchiveFailure(second.track.id)
            collectionAction("Archive").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("track-collection-mutation-problem")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("track-collection-mutation-problem").assertIsDisplayed()
            compose.onNodeWithText("2 Tracks selected").assertIsDisplayed()
            compose.waitForIdle()
            captureVisualCatalogSurface("tracks.collection.mutation-failure")
            assertEquals(beforeFailedArchive, projections(first, second, third))
            assertFalse(projection(first).track.archived)
            assertFalse(projection(second).track.archived)

            sql("DROP TRIGGER fail_track_bulk_archive")
            collectionAction("Archive").performClick()
            compose.waitUntil(10_000) {
                projection(first).track.archived && projection(second).track.archived
            }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("2 Tracks selected").fetchSemanticsNodes().isEmpty()
            }
            assertFalse(projection(third).track.archived)

            scenario.recreate()
            compose.onNodeWithTag("track-workspace-destination-Archived").performClick()
            row(first).assertIsDisplayed()
            row(second).assertIsDisplayed()
            assertTrue(projection(first).track.archived)
            assertTrue(projection(second).track.archived)
        }
    }

    private fun seed(): List<TrackProjection> = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update {
            AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Light)
        }
        listOf("First failure Track", "Second failure Track", "Third failure Track").map { name ->
            val id = app.trackRepository.create(
                TrackDraft(
                    name = name,
                    fields = listOf(TrackFieldDraft("Name", TrackFieldType.ShortText, primary = true)),
                ),
            )
            requireNotNull(app.trackRepository.projection(id))
        }
    }

    private fun enterReorderMode() {
        compose.onNodeWithContentDescription("More Track Options").performClick()
        compose.onNodeWithText("Reorder Tracks").performClick()
        compose.onNodeWithTag("reorder-mode-tracks").assertIsDisplayed()
    }

    private fun enterSelectionMode() {
        compose.onNodeWithContentDescription("More Track Options").performClick()
        compose.onNodeWithText("Select Tracks").performClick()
    }

    private fun moveDown(name: String) {
        val action = compose.onNodeWithContentDescription("Reorder $name")
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .single { it.label == "Move $name down" }
        compose.runOnIdle { assertTrue(action.action()) }
    }

    private fun installReorderFailure() = sql(
        """
        CREATE TRIGGER fail_track_reorder
        BEFORE UPDATE OF position ON tracks
        WHEN NEW.position != OLD.position
        BEGIN
            SELECT RAISE(ABORT, 'injected Track reorder failure');
        END
        """.trimIndent(),
    )

    private fun installArchiveFailure(trackId: Long) = sql(
        """
        CREATE TRIGGER fail_track_bulk_archive
        BEFORE UPDATE OF archived ON tracks
        WHEN OLD.id = $trackId AND NEW.archived = 1
        BEGIN
            SELECT RAISE(ABORT, 'injected Track archive failure');
        END
        """.trimIndent(),
    )

    private fun collectionAction(label: String) = compose.onNodeWithTag("track-list").run {
        performScrollToNode(androidx.compose.ui.test.hasText(label))
        compose.onNodeWithText(label).performScrollTo()
    }

    private fun row(item: TrackProjection) = compose.onNodeWithTag("track-list").run {
        performScrollToNode(androidx.compose.ui.test.hasTestTag("track-card-${item.track.id}"))
        compose.onNodeWithTag("track-card-${item.track.id}").performScrollTo()
    }

    private fun projections(vararg expected: TrackProjection): List<TrackProjection> =
        expected.map(::projection)

    private fun projection(expected: TrackProjection): TrackProjection = runBlocking {
        requireNotNull(app.trackRepository.projection(expected.track.id))
    }

    private fun storedOrder(): List<Long> = runBlocking {
        app.database.trackDao().getAllTracks().sortedBy { it.position }.map { it.id }
    }

    private fun sql(statement: String) {
        app.database.openHelper.writableDatabase.execSQL(statement)
    }
}
