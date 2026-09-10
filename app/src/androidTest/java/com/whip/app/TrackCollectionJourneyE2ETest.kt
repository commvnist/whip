package com.whip.app

import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** Native collection changes cannot carry hidden Tracks into a bulk action. */
@RunWith(AndroidJUnit4::class)
class TrackCollectionJourneyE2ETest {
    private val compose = createEmptyComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()

    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun areaChangesKeepOnlyVisibleSelection() = areaSelection(false)
    @Test @AndroidFontScale
    fun areaChangesKeepOnlyVisibleSelectionAtLargeText() = areaSelection(true)
    @Test fun archivedAndActiveSelectionsStaySeparate() = archivedSelection(false)
    @Test @AndroidFontScale
    fun archivedAndActiveSelectionsStaySeparateAtLargeText() = archivedSelection(true)

    private fun areaSelection(large: Boolean) {
        val before = seed()
        val suffix = if (large) "large" else "ordinary"
        val trail = before[0]
        val reading = before[1]
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            selectMode()
            row(trail).performClick()
            changeArea("Main")
            summary(0)
            capture("tracks.collection.area-scope.$suffix")
            scroll(hasText("Pin to Whip Home")).assertIsNotEnabled()
            scroll(hasText("Archive")).assertIsNotEnabled()
            changeArea("All Areas")
            summary(0)
            row(trail).assertIsOff().performClick()
            row(reading).performClick()
            summary(2)
            scenario.recreate()
            summary(2)
            changeArea("Main")
            summary(1)
            scroll(hasText("Pin to Whip Home")).assertIsEnabled().performClick()
            compose.waitUntil(10_000) { projection(reading.track.id).track.pinned }
            assertEquals(trail, projection(trail.track.id))
            val pinnedReading = projection(reading.track.id)
            assertHistory(reading, pinnedReading)
            changeArea("All Areas")
            selectMode()
            row(trail).performClick()
            row(reading).performClick()
            changeArea("Outdoors")
            summary(1)
            capture("tracks.collection.visible-selection.$suffix")
            if (large) assertFontScale("1 Track selected")
            scroll(hasText("Unpin from Whip Home")).assertIsEnabled().assertIsDisplayed()
            capture("tracks.collection.unpin.$suffix")
            compose.onNodeWithText("Unpin from Whip Home").performClick()
            compose.waitUntil(10_000) { !projection(trail.track.id).track.pinned }
            assertEquals(pinnedReading, projection(reading.track.id))
            assertHistory(trail, projection(trail.track.id))
            assertEquals(before[2], projection(before[2].track.id))
            scenario.recreate()
            row(trail).assertIsDisplayed()
        }
    }

    private fun archivedSelection(large: Boolean) {
        val before = seed()
        val suffix = if (large) "large" else "ordinary"
        val trail = before[0]
        val archived = before[2]
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Tracks tab").performClick()
            destination("Archived")
            selectMode()
            row(archived).performClick()
            destination("Tracks")
            summary(0)
            capture("tracks.collection.archive-scope.$suffix")
            scroll(hasText("Archive")).assertIsNotEnabled()
            row(trail).performClick()
            summary(1)
            scenario.recreate()
            summary(1)
            scroll(hasText("Archive")).performClick()
            compose.waitUntil(10_000) { projection(trail.track.id).track.archived }
            assertEquals(archived, projection(archived.track.id))
            destination("Archived")
            selectMode()
            row(trail).performClick()
            scroll(hasText("Restore")).assertIsEnabled()
            capture("tracks.collection.restore.$suffix")
            if (large) assertFontScale("1 Track selected")
            scenario.recreate()
            summary(1)
            scroll(hasText("Restore")).performClick()
            compose.waitUntil(10_000) { !projection(trail.track.id).track.archived }
            destination("Tracks")
            scenario.recreate()
            row(trail).assertIsDisplayed()
            assertHistory(trail, projection(trail.track.id))
            assertEquals(trail.track.pinned, projection(trail.track.id).track.pinned)
            assertEquals(before[1], projection(before[1].track.id))
            assertEquals(archived, projection(archived.track.id))
        }
    }

    private fun seed(): List<TrackProjection> = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { AppSettings(setupCompleted = true, dynamicColor = false, themeMode = AppThemeMode.Light) }
        val outdoors = app.areaRepository.create("Outdoors")
        listOf("Trail notes", "Reading", "Older walks").mapIndexed { index, name ->
            val id = app.trackRepository.create(TrackDraft(name, areaId = if (index == 1) null else outdoors,
                fields = listOf(TrackFieldDraft("Name", TrackFieldType.ShortText, primary = true))))
            val form = requireNotNull(app.trackRepository.projection(id))
            app.trackRepository.addEntry(id, TrackEntryDraft(entryDate = app.clock.today(), values = mapOf(
                form.primaryField.uuid to TrackValueDraft(textValue = "A saved observation for $name"),
            )))
            if (index != 1) app.trackRepository.setPinned(id, true)
            if (index == 2) app.trackRepository.setArchived(id, true)
            requireNotNull(app.trackRepository.projection(id))
        }
    }

    private fun projection(id: Long) = runBlocking { requireNotNull(app.trackRepository.projection(id)) }
    private fun assertHistory(before: TrackProjection, after: TrackProjection) {
        assertEquals(before.entries, after.entries)
        assertEquals(before.fields, after.fields)
        assertEquals(before.options, after.options)
        assertEquals(before.track.position, after.track.position)
        assertEquals(before.track.areaId, after.track.areaId)
    }
    private fun selectMode() {
        scroll(hasContentDescription("More Track Options")).performClick()
        compose.onNodeWithText("Select Tracks").performClick()
    }
    private fun changeArea(name: String) {
        compose.onNodeWithTag("workspace-area-action").performClick()
        val label = if (name == "All Areas") hasText(name) else hasText("$name ·", substring = true)
        compose.onNode(label and hasAnyAncestor(isPopup())).performClick()
    }
    private fun destination(name: String) = compose.onNodeWithTag("track-workspace-destination-$name").performClick()
    private fun row(item: TrackProjection) = scroll(hasTestTag("track-card-${item.track.id}"))
    private fun summary(count: Int) = scroll(hasText("$count ${if (count == 1) "Track" else "Tracks"} selected")).assertIsDisplayed()
    private fun scroll(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        compose.onNodeWithTag("track-list").performScrollToNode(matcher)
        return compose.onNode(matcher).performScrollTo()
    }
    private fun capture(id: String) { compose.waitForIdle(); captureVisualCatalogSurface(id) }
    private fun assertFontScale(text: String) {
        scroll(hasText(text)).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { get ->
            val results = mutableListOf<TextLayoutResult>()
            assertTrue(get(results))
            assertTrue(results.isNotEmpty())
            results.forEach { assertEquals(2f, it.layoutInput.density.fontScale, 0.01f) }
        }
    }
}
