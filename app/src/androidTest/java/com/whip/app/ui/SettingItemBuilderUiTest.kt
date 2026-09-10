package com.whip.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingItemBuilderUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun settingChoiceKeepsCompleteValuesAndAnIdentityOwnedCurrentMenu() {
        val key = mutableStateOf("first")
        val enabled = mutableStateOf(true)
        val revision = mutableStateOf(1)
        val longValue = "Research, writing and professional development across the whole week"
        var chosen = ""
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val currentRevision = revision.value
                Box(Modifier.width(320.dp)) {
                    WhipSettingItem("Opening area", Modifier.testTag("setting-choice"), enabled.value, key.value) {
                        choice(listOf(longValue, "All Areas"), longValue, { it }) { chosen = "$it/$currentRevision" }
                    }
                }
            }
        }
        compose.onNodeWithText(longValue, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { get ->
            val layouts = mutableListOf<TextLayoutResult>()
            assertTrue(get(layouts))
            assertFalse(layouts.single().hasVisualOverflow)
        }
        compose.onNodeWithTag("setting-choice").performClick()
        compose.onNodeWithContentDescription("Opening area option: $longValue").assertIsSelected()
        compose.runOnIdle { key.value = "second" }
        compose.onAllNodesWithContentDescription("Opening area option: All Areas").assertCountEquals(0)
        compose.onNodeWithTag("setting-choice").performClick()
        compose.runOnIdle { revision.value = 2 }
        compose.onNodeWithContentDescription("Opening area option: All Areas").performClick()
        assertEquals("All Areas/2", chosen)
        compose.onAllNodesWithContentDescription("Opening area option: All Areas").assertCountEquals(0)
        compose.onNodeWithTag("setting-choice").performClick()
        compose.runOnIdle { enabled.value = false }
        compose.onNodeWithTag("setting-choice").assertIsNotEnabled()
        compose.runOnIdle { enabled.value = true }
        compose.onAllNodesWithContentDescription("Opening area option: All Areas").assertCountEquals(0)
    }

    @Test fun settingInformationUsesFullWidthAndEachRoleDispatchesOnce() {
        var toggles = 0
        var edits = 0
        val explanation = "Keep a complete explanation readable below the label and switch."
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Column(Modifier.width(320.dp)) {
                    WhipSettingItem("Sync selected categories", Modifier.testTag("setting-toggle")) {
                        description(explanation)
                        toggle(false) { toggles++ }
                    }
                    WhipSettingItem("Rest duration", Modifier.testTag("setting-edit")) {
                        edit("120 seconds") { edits++ }
                    }
                }
            }
        }
        val parent = compose.onNodeWithTag("setting-toggle").fetchSemanticsNode().boundsInRoot
        val info = compose.onNodeWithText(explanation, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText("Sync selected categories", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(parent.left, info.left, 1f)
        assertEquals(parent.right, info.right, 1f)
        assertTrue(info.top >= title.bottom)
        assertTrue(parent.height >= with(compose.density) { 48.dp.toPx() })
        compose.onAllNodes(isToggleable()).assertCountEquals(1)
        compose.onNodeWithTag("setting-toggle").performClick()
        compose.onNodeWithText("120 seconds").performClick()
        assertEquals(1, toggles)
        assertEquals(1, edits)
    }
}
