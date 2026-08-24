package com.whip.app

import android.app.ActivityOptions
import android.content.Intent
import android.view.Display
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class WhipNavigationTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun primaryAreasAndAccessibleTopActionsAreReachable() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<WhipApplication>()
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { it.copy(setupCompleted = true) }
        }
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java,
        ).putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic()
            .setLaunchDisplayId(Display.DEFAULT_DISPLAY)
            .toBundle()

        // The explicit display is required on foldable devices that expose more than one display.
        ActivityScenario.launch<MainActivity>(intent, options).use {
            compose.waitUntil(TIMEOUT_MS) {
                compose.onAllNodesWithText("Review & Trends").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Home").assertIsDisplayed()
            compose.onNodeWithText("Review & Trends").assertIsDisplayed()

            compose.onNodeWithContentDescription("Search All Whip Data").performClick()
            compose.onNodeWithTag("unified-search-query").assertIsDisplayed()
            compose.onNodeWithText("Close").performClick()
            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithTag("task-destination-Upcoming").performClick()
            compose.onNodeWithText("The next 30 days", substring = true).assertIsDisplayed()

            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithText("Check in, log a value, or continue a timer for habits due today.").assertIsDisplayed()

            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("gym-destination-Workout").assertIsDisplayed()

            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithText("Long-term measurements, consistency, ranges, totals, and project milestones.").assertIsDisplayed()

            compose.onNodeWithContentDescription("Tracks tab").performClick()
            compose.onNodeWithText("Define the Evidence That Matters").assertIsDisplayed()
            compose.onNodeWithText("Create Track").assertIsDisplayed()
        }
    }

    @Test
    fun switchingPrimaryAreasResetsEachAreaToItsFirstHeading() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<WhipApplication>()
            app.backupRepository.deleteAllData()
            app.settingsRepository.update { it.copy(setupCompleted = true) }
        }
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java,
        ).putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic()
            .setLaunchDisplayId(Display.DEFAULT_DISPLAY)
            .toBundle()

        ActivityScenario.launch<MainActivity>(intent, options).use {
            compose.waitUntil(TIMEOUT_MS) {
                compose.onAllNodesWithText("Review & Trends").fetchSemanticsNodes().isNotEmpty()
            }

            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithTag("task-destination-Today").assertIsSelected()
            compose.onNodeWithTag("task-destination-Inbox").performClick().assertIsSelected()

            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithTag("habit-destination-Today").assertIsSelected()
            compose.onNodeWithTag("habit-destination-All").performClick().assertIsSelected()

            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithTag("task-destination-Today").assertIsSelected()

            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithTag("habit-destination-Today").assertIsSelected()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
