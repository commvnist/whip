package com.whip.app

import android.os.Build
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhipAccessibilityChecksTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun prepareApp() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { it.copy(setupCompleted = true) }
        compose.waitForIdle()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun primaryAreasPassComposeAccessibilityTestFramework() {
        assumeTrue("Compose accessibility checks require Android 14+", Build.VERSION.SDK_INT >= 34)
        compose.enableAccessibilityChecks()
        compose.onRoot().tryPerformAccessibilityChecks()
        listOf("Tasks tab", "Habits tab", "Goals tab", "Tracks tab", "Gym tab").forEach { tab ->
            compose.onNodeWithContentDescription(tab).performClick()
            compose.onRoot().tryPerformAccessibilityChecks()
        }
        compose.onNodeWithTag("workspace-settings-action").performClick()
        compose.onRoot().tryPerformAccessibilityChecks()
        compose.onNodeWithContentDescription("Go to Home").performClick()
        compose.onRoot().tryPerformAccessibilityChecks()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun productivityEditorsAndDetailsPassAccessibilityChecks() {
        assumeTrue("Compose accessibility checks require Android 14+", Build.VERSION.SDK_INT >= 34)
        compose.enableAccessibilityChecks()

        listOf("Task", "Habit", "Goal").forEach { type ->
            if (compose.onAllNodesWithContentDescription("Go to Home").fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithContentDescription("Go to Home").performClick()
            } else {
                compose.onNodeWithContentDescription("Home").performClick()
            }
            compose.onNodeWithContentDescription("Add task, habit, goal, track, or workout").performClick()
            compose.onNodeWithText("New $type").performClick()
            compose.onRoot().tryPerformAccessibilityChecks()
            if (type == "Task") {
                compose.onNodeWithContentDescription("Cancel Task editing").performClick()
            } else {
                compose.onNodeWithContentDescription("Cancel $type editing").performClick()
            }
        }
    }
}
