package com.whip.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
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
