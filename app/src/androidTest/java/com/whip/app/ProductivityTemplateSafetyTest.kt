package com.whip.app

import android.app.ActivityOptions
import android.content.Intent
import android.view.Display
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductivityTemplateSafetyTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun reset() = runBlocking {
        app.backupRepository.deleteAllData()
        app.settingsRepository.update { it.copy(setupCompleted = true, powerMode = false) }
    }

    @Test
    fun taskRecipeOnlyPrefillsAndNeverPersistsBeforeSave() {
        val intent = Intent(app, MainActivity::class.java).putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle()
        ActivityScenario.launch<MainActivity>(intent, options).use {
            compose.waitForIdle()
            compose.onNodeWithContentDescription("Add task, habit, goal, track, exercise, or workout").performClick()
            compose.onNodeWithText("Task").performClick()
            compose.onNodeWithText("Use a Template").performClick()
            compose.onNodeWithText("Repeat on Chosen Weekdays").performClick()

            compose.onNodeWithTag("task-editor-title").assertIsDisplayed()
            compose.onNodeWithText("Weekly Task").assertIsDisplayed()
            check(runBlocking { app.taskRepository.tasks.first().isEmpty() }) {
                "Choosing a recipe persisted a task before Save"
            }

            compose.onNodeWithContentDescription("Cancel Task editing").performClick()
            compose.onNodeWithText("Discard Changes").performClick()
            check(runBlocking { app.taskRepository.tasks.first().isEmpty() })
        }
    }
}
