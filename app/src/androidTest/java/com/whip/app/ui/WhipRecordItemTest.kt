package com.whip.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhipRecordItemTest {
    @get:Rule val compose = createComposeRule()

    @Test fun commandAvailabilityClosesAnUnavailableMenuAndRecoversWithoutDispatch() {
        val saving = mutableStateOf(false)
        val canArchive = mutableStateOf(false)
        var renamed = 0
        var archived = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WhipRecordItem("unit", "custom unit", "Bundle") {
                    command("Rename", enabled = !saving.value) { renamed++ }
                    command("Archive", enabled = !saving.value && canArchive.value) { archived++ }
                }
            }
        }
        val menu = compose.onNodeWithContentDescription("More Actions for Bundle")
        menu.performClick()
        compose.onNodeWithText("Rename").assertIsEnabled()
        compose.onNodeWithText("Archive").assertIsNotEnabled()
        compose.runOnIdle { saving.value = true }
        menu.assertIsNotEnabled()
        compose.onNodeWithText("Rename").assertDoesNotExist()
        compose.runOnIdle { saving.value = false; canArchive.value = true }
        menu.assertIsEnabled()
        compose.onNodeWithText("Rename").assertDoesNotExist()
        menu.performClick()
        compose.onNodeWithText("Archive").assertIsEnabled().performClick()
        compose.onNodeWithText("Archive").assertDoesNotExist()
        assertEquals(0, renamed)
        assertEquals(1, archived)
    }

    @Test fun completeCatalogContentAndDirectActionsSurviveReorderMode() {
        val reordering = mutableStateOf(false)
        var opened = 0
        var archived = 0
        var moved = 0
        val title = "Upper body pulling with controlled shoulder movement and a complete range of motion"
        val detail = "Seat 4 · Back 2 · Independent handles with adjustable cable height · resistance ×0.5"
        compose.setContent {
            WhipTheme(darkTheme = false, dynamicColor = false) {
                Box(Modifier.width(320.dp)) {
                    val interaction = rememberWhipReorderInteractionState()
                    WhipRecordItem(1L, "category", title, { opened++ }) {
                        context("Movement family and training emphasis")
                        detail(detail)
                        edit {}
                        action("Archive category", androidx.compose.material.icons.Icons.Outlined.Archive) { archived++ }
                        command("Inspect category") {}
                        if (reordering.value) reorder(2, 3, interaction, "test-catalog") { moved = it }
                    }
                }
            }
        }
        for (text in listOf(title, detail)) {
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onNodeWithText(text, useUnmergedTree = true).performSemanticsAction(
                androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult,
            ) { it(layouts) }
            assertTrue("Complete catalog text must wrap: $text", layouts.isNotEmpty() && layouts.none { it.hasVisualOverflow })
        }
        compose.onNodeWithContentDescription("Archive category").performClick()
        assertEquals(1, archived)
        assertEquals(0, opened)
        compose.onNodeWithContentDescription("More Actions for $title").performClick()
        compose.runOnIdle { reordering.value = true }
        compose.onNodeWithText("Inspect category").assertDoesNotExist()
        compose.onNodeWithContentDescription("Archive category").assertDoesNotExist()
        compose.onNodeWithContentDescription("Edit category $title").assertDoesNotExist()
        compose.onNodeWithText(title).assertHasNoClickAction()
        val move = compose.onNodeWithContentDescription("Reorder $title").fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsActions.CustomActions].single { it.label.endsWith(" up") }
        compose.runOnIdle { assertTrue(move.action()) }
        assertEquals(-1, moved)
        compose.runOnIdle { reordering.value = false }
        compose.onNodeWithText("Inspect category").assertDoesNotExist()
        compose.onNodeWithContentDescription("Archive category").assertIsDisplayed()
        compose.onNodeWithText(title).performClick()
        assertEquals(1, opened)
    }

    @Test fun narrowRecordsKeepInformationBelowTheWholeHeader() {
        compose.setContent {
            WhipTheme(darkTheme = false, dynamicColor = false) {
                Box(Modifier.width(320.dp)) {
                    WhipRecordItem(1L, "Entry", "Morning walk along the river", {}, identityEmoji = "🥾") {
                        context("Today · Outdoor walks")
                        fact("Terrain", "Gravel paths and forest trails after the rain")
                        edit {}
                        command("Delete Entry", role = WhipMenuItemRole.Destructive) {}
                    }
                }
            }
        }
        val title = compose.onNodeWithText("Morning walk along the river", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val context = compose.onNodeWithText("Today · Outdoor walks", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val facts = compose.onNodeWithText("Terrain: Gravel paths and forest trails after the rain", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val edit = compose.onNodeWithContentDescription("Edit Entry Morning walk along the river").fetchSemanticsNode().boundsInRoot
        val more = compose.onNodeWithContentDescription("More Actions for Morning walk along the river").fetchSemanticsNode().boundsInRoot
        assertTrue("Context starts below the title and actions", context.top >= maxOf(title.bottom, edit.bottom, more.bottom))
        assertTrue("Facts follow context", facts.top >= context.bottom)
        assertTrue("Information starts at the identity edge", facts.left < title.left)
        assertTrue("Facts use the width beneath actions", facts.right > more.left)
        assertEquals(context.left, facts.left, 0.5f)
    }

    @Test fun actionsStayWithTheirRecordAndUseCurrentCallbacks() {
        val record = mutableStateOf(1)
        val editable = mutableStateOf(true)
        val callbackVersion = mutableStateOf(1)
        var opened = 0
        var edited = 0
        var deleted = ""
        compose.setContent {
            WhipTheme(darkTheme = false, dynamicColor = false) {
                val id = record.value
                val version = callbackVersion.value
                WhipRecordItem(id, "Entry", "Record $id", { opened = id }) {
                    context("")
                    fact("Empty", "")
                    if (editable.value) {
                        edit { edited = id }
                        command("Delete Entry", role = WhipMenuItemRole.Destructive) { deleted = "$id:$version" }
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Edit Entry Record 1").performClick()
        assertEquals(1, edited)
        assertEquals(0, opened)
        compose.onNodeWithContentDescription("More Actions for Record 1").performClick()
        compose.runOnIdle { record.value = 2 }
        compose.onNodeWithText("Delete Entry").assertDoesNotExist()
        compose.onNodeWithContentDescription("More Actions for Record 2").performClick()
        compose.runOnIdle { callbackVersion.value = 2 }
        compose.onNodeWithText("Delete Entry").performClick()
        assertEquals("2:2", deleted)
        compose.onNodeWithText("Delete Entry").assertDoesNotExist()
        assertEquals(0, opened)
        compose.runOnIdle { editable.value = false }
        compose.onNodeWithContentDescription("Edit Entry Record 2").assertDoesNotExist()
        compose.onNodeWithContentDescription("More Actions for Record 2").assertDoesNotExist()
        compose.onNodeWithText("Record 2").performClick()
        assertEquals(2, opened)
        compose.onNodeWithText("Empty", substring = true).assertDoesNotExist()
    }
}
