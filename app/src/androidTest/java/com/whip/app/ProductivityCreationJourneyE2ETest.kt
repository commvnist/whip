package com.whip.app

import android.Manifest
import android.app.ActivityOptions
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.view.Display
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.core.content.ContextCompat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductivityCreationJourneyE2ETest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: WhipApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun reset() = runBlocking {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            check(
                ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_DENIED,
            ) {
                "The creation journey must run with notification permission denied"
            }
        }
        app.backupRepository.deleteAllData()
        app.settingsRepository.update {
            it.copy(setupCompleted = true, backupPrivacyChoiceHandled = true, powerMode = false)
        }
    }

    @After
    fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test
    fun taskHabitAndGoalCanBeCreatedAndUsedThroughTheRealUi() {
        val intent = Intent(app, MainActivity::class.java).putExtra("commvne.com.whip.app.DEBUG_SHOW_WHEN_LOCKED", true)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY).toBundle()
        ActivityScenario.launch<MainActivity>(intent, options).use { scenario ->
            compose.waitForIdle()

            compose.onNodeWithContentDescription("Tasks tab").performClick()
            compose.onNodeWithTag("task-quick-capture").performTextInput("Journey task")
            compose.onNodeWithText("Review").performClick()
            compose.onNodeWithTag("task-editor-title").assertIsDisplayed()
            compose.onNodeWithText("Save").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Journey task").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Open task details for Journey task").performClick()
            compose.onNodeWithText("Schedule").assertIsDisplayed()
            check(compose.onAllNodesWithText("More").fetchSemanticsNodes().isNotEmpty())
            compose.onNodeWithText("Close").performClick()

            compose.onNodeWithContentDescription("Habits tab").performClick()
            compose.onNodeWithContentDescription("Add habit").performClick()
            compose.onNodeWithTag("habit-editor-name").performTextInput("Journey water")
            compose.onNodeWithText("Count").performClick()
            compose.onNodeWithText("Save").performClick()
            val habitId = runBlocking {
                withTimeout(5_000) {
                    app.habitRepository.habits.first { habits -> habits.any { it.name == "Journey water" } }
                        .first { it.name == "Journey water" }.id
                }
            }
            compose.waitUntil(5_000) {
                runCatching {
                    compose.onNodeWithTag("habit-card-$habitId").fetchSemanticsNode()
                    true
                }.getOrDefault(false)
            }
            compose.onNodeWithText("+1").performClick()
            runBlocking {
                withTimeout(5_000) { app.habitRepository.logs.first { logs -> logs.any { it.habitId == habitId } } }
            }
            compose.onNodeWithContentDescription("Edit habit Journey water")
                .performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithText("Edit habit").assertIsDisplayed()
            compose.onNodeWithText("Cancel").performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithContentDescription("Open habit details for Journey water")
                .performSemanticsAction(SemanticsActions.OnClick)
            listOf("Today", "History", "Connections", "More").forEach { section ->
                compose.onNodeWithTag("habit-detail-section-$section")
                    .performSemanticsAction(SemanticsActions.OnClick)
                compose.onNodeWithText("Edit habit").assertIsDisplayed()
            }
            compose.onNodeWithText("Edit habit").performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithText("Edit habit").assertIsDisplayed()
            compose.onNodeWithText("Cancel").performSemanticsAction(SemanticsActions.OnClick)

            compose.onNodeWithContentDescription("Goals tab").performClick()
            compose.onNodeWithContentDescription("Add goal").performClick()
            compose.onNodeWithTag("goal-editor-name").performTextInput("Journey target")
            compose.onNodeWithTag("goal-editor-fields").performScrollToNode(hasTestTag("goal-editor-target"))
            compose.onNodeWithTag("goal-editor-target").performTextInput("10")
            compose.onNodeWithText("Save").performClick()
            val goalId = runBlocking {
                withTimeout(5_000) {
                    app.goalRepository.goals.first { goals -> goals.any { it.name == "Journey target" } }
                        .first { it.name == "Journey target" }.id
                }
            }
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Goal created").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Done").performClick()
            compose.onNodeWithContentDescription("Edit goal Journey target")
                .performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithText("Edit goal").assertIsDisplayed()
            compose.onNodeWithText("Cancel").performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithContentDescription("Open goal details for Journey target")
                .performSemanticsAction(SemanticsActions.OnClick)
            listOf("Overview", "History", "Connections", "More").forEach { section ->
                compose.onNodeWithTag("goal-detail-section-$section")
                    .performSemanticsAction(SemanticsActions.OnClick)
                compose.onNodeWithText("Edit goal").assertIsDisplayed()
            }
            compose.onNodeWithText("Edit goal").performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithText("Edit goal").assertIsDisplayed()
            compose.onNodeWithText("Cancel").performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithTag("goal-destination-more").performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithTag("goal-destination-Archived").assertIsDisplayed()
            compose.onNodeWithTag("goal-destination-Insights").performSemanticsAction(SemanticsActions.OnClick)
            compose.onNodeWithText("Goal insights").assertIsDisplayed()
            compose.onNodeWithTag("goal-insight-$goalId").assertIsDisplayed()
        }
    }
}
