package com.whip.app.ui

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasContentDescription
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.whip.app.AndroidFontScale
import com.whip.app.AndroidFontScaleRule
import com.whip.app.WhipApplication
import com.whip.app.assertDialogFontScale
import com.whip.app.captureVisualCatalogSurface
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackValueDraft
import com.whip.app.ui.theme.WhipTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.RuleChain
import java.io.File

@RunWith(AndroidJUnit4::class)
class TrackEntryDeleteRecoveryUiTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()
    private val stores = mutableListOf<ViewModelStore>()

    @After fun clean() {
        compose.runOnIdle { stores.forEach(ViewModelStore::clear) }
        runBlocking { app.backupRepository.deleteAllData() }
    }

    @Test fun newOwnerCannotReplaceTheRestoredDeletionRevision() = verifyRecovery(false)

    @Test @AndroidFontScale
    fun recoveredDeletionConflictRemainsReachableAtLargeText() = verifyRecovery(true)

    private fun verifyRecovery(large: Boolean) {
        val opening = runBlocking {
            app.backupRepository.deleteAllData()
            val id = app.trackRepository.create(TrackDraft(
                name = "Walking archive",
                fields = listOf(
                    TrackFieldDraft("Name", TrackFieldType.ShortText, primary = true),
                    TrackFieldDraft("Note", TrackFieldType.LongText),
                ),
            ))
            val prepared = requireNotNull(app.trackRepository.prepareEntryCreate(id))
            val entry = app.trackRepository.addEntry(prepared.request, TrackEntryDraft(
                app.clock.today(), prepared.form.fields.associate { field ->
                    field.uuid to TrackValueDraft(textValue = if (field.primary) "River trail" else "Opening note")
                },
            ))
            requireNotNull(app.trackRepository.prepareEntryEdit(entry.entryId))
        }
        var projection = runBlocking { requireNotNull(app.trackRepository.projection(opening.form.track.id)) }
        var nextOwner = owner()
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            val currentOwner = remember { nextOwner }
            WhipTheme(darkTheme = false, dynamicColor = false) {
                WhipScreen(
                    state = TaskUiState(loading = false),
                    trackState = TrackUiState(projections = listOf(projection), currentDate = app.clock.today(), loading = false),
                    trackViewModel = currentOwner,
                    adaptiveLayout = WhipAdaptiveLayout.Compact,
                    onSaveTask = { _, _, _ -> }, onComplete = {}, onSkip = {},
                    onReschedule = { _, _ -> }, onArchive = {}, onReopen = {},
                )
            }
        }
        compose.onNodeWithContentDescription("Tracks tab").performClick()
        compose.onNodeWithTag("track-list").performScrollToNode(hasTestTag("track-card-${projection.track.id}"))
        compose.onNodeWithTag("track-card-${projection.track.id}").performClick()
        compose.onNodeWithTag("track-entry-list").performScrollToNode(hasContentDescription("More Actions for River trail"))
        compose.onNodeWithContentDescription("More Actions for River trail").performClick()
        compose.onNodeWithText("Delete Entry").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("track-entry-row-delete-confirmation").fetchSemanticsNodes().isNotEmpty()
        }
        val note = opening.form.fields.single { it.name == "Note" }
        runBlocking {
            app.trackRepository.updateEntry(opening.boundary, opening.draft.copy(
                values = opening.draft.values + (note.uuid to TrackValueDraft(textValue = "Newer unreviewed note")),
            ))
            projection = requireNotNull(app.trackRepository.projection(projection.track.id))
        }
        // A new coordinator plus restored Compose state covers the missing process-owner seam.
        // Actual OS process death is exercised independently by the retained emulator driver.
        nextOwner = owner()
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        if (large) compose.assertDialogFontScale()
        capture("review", large)
        compose.onNodeWithText("You can use Undo immediately after deletion.").performScrollTo().assertIsDisplayed()
        if (large && android.os.Build.VERSION.SDK_INT < 29) capture("undo", large)
        compose.onNodeWithText("Delete Entry").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("track-entry-row-delete-problem").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithTag("track-entry-row-delete-confirmation").fetchSemanticsNodes().isEmpty()
        }
        val after = runBlocking { requireNotNull(app.trackRepository.projection(projection.track.id)) }
        assertEquals("A recovered deletion must reject an unreviewed revision", 1, after.entries.size)
        assertEquals("Newer unreviewed note", after.entries.single().value(note.id)?.textValue)
        compose.onNodeWithTag("track-entry-row-delete-problem").assertIsDisplayed()
        capture("conflict", large)
        compose.onNodeWithText("Review Entry").performClick()
        compose.onNode(hasScrollAction() and hasAnyDescendant(hasText("Recorded evidence")))
            .performScrollToNode(hasText("Newer unreviewed note"))
        compose.onNodeWithText("Newer unreviewed note").assertIsDisplayed()
        capture("latest", large)
    }

    private fun capture(kind: String, large: Boolean) {
        val id = "tracks.entry-delete.$kind.${if (large) "large" else "ordinary"}"
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            captureVisualCatalogSurface(id)
        } else {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            compose.waitForIdle()
            instrumentation.uiAutomation.waitForIdle(750L, 5_000L)
            val directory = checkNotNull(instrumentation.targetContext.getExternalFilesDir("entry-delete-recovery"))
            check(directory.isDirectory || directory.mkdirs())
            val device = UiDevice.getInstance(instrumentation)
            device.waitForIdle()
            check(device.takeScreenshot(File(directory, "$id.png")))
            device.dumpWindowHierarchy(File(directory, "$id.xml"))
        }
    }

    private fun owner(): TrackViewModel {
        val store = ViewModelStore().also(stores::add)
        return ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TrackViewModel(app, SavedStateHandle()) as T
        })[TrackViewModel::class.java]
    }
}
