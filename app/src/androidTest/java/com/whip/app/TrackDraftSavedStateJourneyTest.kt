package com.whip.app

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.TrackCsvMapping
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.previewTrackCsvImport
import com.whip.app.domain.trackCsvPayloadFingerprint
import com.whip.app.ui.validateTrackCsvEnvelope
import com.whip.app.ui.TrackEditorSessionViewModel
import java.util.UUID
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackDraftSavedStateJourneyTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()

    // Opt-in host-driven process-death preparation. Ordinary regression runs
    // always clean up; the external emulator driver owns this explicit fixture.
    private val seedOnly get() = InstrumentationRegistry.getArguments()
        .getString("whipTrackDraftSeedOnly") == "true"

    @After fun clean() = runBlocking { if (!seedOnly) app.backupRepository.deleteAllData() }

    @Test fun largeAcceptedEntryKeepsSavedStateWithinPlatformTransport() = verifySavedState(0)

    @Test fun savedEntriesDoNotAccumulateDraftPayloadsInActivityState() = verifySavedState(3)

    @Test fun rowDeletionReviewKeepsSavedStateWithinPlatformTransport() = verifySavedState(0, rowDeletion = true)

    private fun verifySavedState(completedSessions: Int, rowDeletion: Boolean = false) {
        val original = buildString {
            repeat(if (rowDeletion) 25_000 else 12_000) { index ->
                append("Observation $index: check the route, weather and conditions on the next walk.\n")
            }
        }.take(if (rowDeletion) 1_200_000 else 600_000) + "KEEP-END"
        val before = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Light)
            }
            val id = app.trackRepository.create(TrackDraft(
                name = "Imported observation archive",
                fields = listOf(
                    TrackFieldDraft("Name", TrackFieldType.ShortText, primary = true),
                    TrackFieldDraft("Source notes", TrackFieldType.LongText),
                ),
            ))
            val form = requireNotNull(app.trackRepository.csvImportForm(id))
            val csv = "Name,Source notes\nRiver trail,\"$original\"\n"
            val envelope = validateTrackCsvEnvelope(csv)
            assertEquals(1, envelope.dataRows)
            assertEquals(2, envelope.maximumColumns)
            val mapping = TrackCsvMapping(fieldColumns = form.fields.associate { it.uuid to it.name })
            val preview = previewTrackCsvImport(form, csv, mapping, app.clock.today())
            assertTrue(preview.issues.isEmpty())
            assertEquals(1, preview.validRows)
            val preparation = requireNotNull(app.trackRepository.prepareCsvImport(
                openingForm = form,
                batchUuid = UUID.randomUUID().toString(),
                payloadFingerprint = trackCsvPayloadFingerprint(csv),
                mapping = mapping,
                defaultEntryDate = app.clock.today(),
                drafts = preview.validDrafts,
            ))
            assertTrue(app.trackRepository.importEntries(preparation.request, preview.validDrafts).changed)
            requireNotNull(app.trackRepository.projection(id))
        }
        val field = before.fields.single { it.name == "Source notes" }
        assertEquals(original, before.entries.single().value(field.id)?.textValue)
        if (seedOnly) {
            File(app.filesDir, "track-draft-process-fixture.txt").writeText(
                "trackId=${before.track.id}\nentryId=${before.entries.single().entry.id}\n" +
                    "entryUuid=${before.entries.single().entry.uuid}\nfieldUuid=${field.uuid}\n" +
                    "originalChars=${original.length}\n",
            )
            return
        }
        val tag = "track-entry-long-text-${field.uuid}"
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithTag("track-list").performScrollToNode(hasTestTag("track-card-${before.track.id}"))
            compose.onNodeWithTag("track-card-${before.track.id}").performClick()
            if (rowDeletion) {
                compose.onNodeWithContentDescription("More Actions for River trail").performClick()
                compose.onNodeWithText("Delete Entry").performClick()
                compose.waitUntil(10_000) {
                    compose.onAllNodesWithTag("track-entry-row-delete-confirmation").fetchSemanticsNodes().isNotEmpty()
                }
            } else repeat(maxOf(1, completedSessions)) { index ->
                compose.onNodeWithTag("track-entry-list")
                    .performScrollToNode(hasContentDescription("Edit Entry River trail"))
                compose.onNodeWithContentDescription("Edit Entry River trail").performClick()
                compose.waitUntil(10_000) {
                    compose.onAllNodesWithTag("track-entry-editor-list").fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag("track-entry-editor-list").performScrollToNode(hasTestTag(tag))
                compose.onNodeWithTag(tag).performClick().performTextReplacement("Changed $index: $original")
                closeSoftKeyboard()
                if (completedSessions > 0) {
                    compose.onNodeWithText("Save", substring = false).performClick()
                    compose.waitUntil(10_000) {
                        compose.onAllNodesWithTag("track-entry-editor-surface").fetchSemanticsNodes().isEmpty()
                    }
                }
            }
            compose.waitForIdle()

            // Collect the real Activity registry state without sending an oversized
            // baseline across Binder and killing the instrumentation process.
            val state = Bundle()
            scenario.onActivity { activity ->
                InstrumentationRegistry.getInstrumentation().callActivityOnSaveInstanceState(activity, state)
            }
            val size = parcelBytes(state)
            Log.i("WhipDraftState", "acceptedChars=${original.length}; completedSessions=$completedSessions; rowDeletion=$rowDeletion; activityStateBytes=$size")
            state.keySet().sorted().forEach { key ->
                val part = Bundle(state)
                part.keySet().filter { it != key }.forEach(part::remove)
                Log.i("WhipDraftState", "stateKey=$key; bytes=${parcelBytes(part)}")
            }
            assertTrue(
                "Entry state must leave ample shared Binder headroom; measured $size bytes",
                size < 128 * 1024,
            )
            scenario.onActivity { activity ->
                val owner = ViewModelProvider(activity)[TrackEditorSessionViewModel.KEY, TrackEditorSessionViewModel::class.java]
                if (rowDeletion) {
                    assertNull(owner.routeState.value)
                    assertTrue(owner.saveCheckpoint().isEmpty)
                } else if (completedSessions == 0) {
                    val checkpoint = owner.saveCheckpoint()
                    val recovered = TrackEditorSessionViewModel(app, SavedStateHandle(mapOf(
                        TrackEditorSessionViewModel.STATE_KEY to checkpoint,
                    )))
                    assertEquals(owner.routeState.value, recovered.routeState.value)
                    assertEquals(owner.entry.state.value, recovered.entry.state.value)
                    assertEquals("Changed 0: $original", recovered.entry.state.value.draft?.values?.get(field.uuid)?.textValue)
                    assertNull(recovered.recoveryProblem)
                } else {
                    assertNull(owner.entry.state.value.draft)
                    assertNull(owner.definition.state.value.draft)
                    assertTrue(owner.saveCheckpoint().isEmpty)
                }
            }
        }
    }

    private fun parcelBytes(state: Bundle): Int {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(state)
            parcel.dataSize()
        } finally {
            parcel.recycle()
        }
    }
}
