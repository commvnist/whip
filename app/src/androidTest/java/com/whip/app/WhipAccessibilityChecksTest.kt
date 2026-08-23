package com.whip.app

import android.os.Build
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhipAccessibilityChecksTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @OptIn(ExperimentalTestApi::class)
    @Test fun primaryAreasPassComposeAccessibilityTestFramework() {
        assumeTrue("Compose accessibility checks require Android 14+", Build.VERSION.SDK_INT >= 34)
        compose.onAllNodesWithText("Skip and show everything").fetchSemanticsNodes()
            .firstOrNull()
            ?.let { compose.onAllNodesWithText("Skip and show everything")[0].performClick() }
        compose.enableAccessibilityChecks()
        compose.onRoot().tryPerformAccessibilityChecks()
        listOf("Tasks tab", "Habits tab", "Goals tab", "Gym tab").forEach { tab ->
            compose.onNodeWithContentDescription(tab).performClick()
            compose.onRoot().tryPerformAccessibilityChecks()
        }
        compose.onNodeWithContentDescription("Open Settings").performClick()
        compose.onRoot().tryPerformAccessibilityChecks()
        compose.onNodeWithContentDescription("Close settings").performClick()
        compose.onNodeWithContentDescription("Home tab").performClick()
        compose.onRoot().tryPerformAccessibilityChecks()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun productivityEditorsAndDetailsPassAccessibilityChecks() {
        assumeTrue("Compose accessibility checks require Android 14+", Build.VERSION.SDK_INT >= 34)
        compose.onAllNodesWithText("Skip and show everything").fetchSemanticsNodes()
            .firstOrNull()
            ?.let { compose.onAllNodesWithText("Skip and show everything")[0].performClick() }
        compose.enableAccessibilityChecks()

        listOf("Task", "Habit", "Goal").forEach { type ->
            compose.onNodeWithContentDescription("Home tab").performClick()
            compose.onNodeWithContentDescription("Add task, habit, goal, exercise, workout, or measurement").performClick()
            compose.onNodeWithText(type).performClick()
            compose.onRoot().tryPerformAccessibilityChecks()
            compose.onNodeWithText("Cancel").performClick()
        }
    }
}
