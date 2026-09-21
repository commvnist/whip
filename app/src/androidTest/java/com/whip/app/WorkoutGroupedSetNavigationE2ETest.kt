package com.whip.app

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.WorkoutGroupType
import com.whip.app.domain.WorkoutSetDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** The sticky execution lane must navigate to the actual Set, not merely its group. */
class WorkoutGroupedSetNavigationE2ETest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app: WhipApplication get() = ApplicationProvider.getApplicationContext()

    @After fun clean() = runBlocking { app.backupRepository.deleteAllData() }

    @Test fun nextSetInLongSupersetIsVisibleAfterJumpAndRecreation() {
        val firstSetId = runBlocking {
            app.backupRepository.deleteAllData()
            app.settingsRepository.update {
                AppSettings(setupCompleted = true, themeMode = AppThemeMode.Dark, dynamicColor = false)
            }
            val firstExercise = app.gymRepository.createExercise(ExerciseDraft(name = "Navigation bench"))
            val secondExercise = app.gymRepository.createExercise(ExerciseDraft(name = "Navigation row"))
            val session = app.gymRepository.startWorkout("Grouped navigation")
            val firstPlacement = app.gymRepository.addExerciseToWorkout(session, firstExercise)
            val secondPlacement = app.gymRepository.addExerciseToWorkout(session, secondExercise)
            val first = app.gymRepository.addSet(firstPlacement, WorkoutSetDraft(weight = 40.0, reps = 6))
            repeat(9) { app.gymRepository.addSet(firstPlacement, WorkoutSetDraft(weight = 40.0, reps = 6)) }
            app.gymRepository.addSet(secondPlacement, WorkoutSetDraft(weight = 25.0, reps = 8))
            app.gymRepository.createGroup(session, "Superset", WorkoutGroupType.Superset, listOf(firstPlacement, secondPlacement))
            first
        }
        fun firstSetCompleted() = runBlocking {
            app.gymRepository.sets.first().single { it.id == firstSetId }.completed
        }
        launchMainActivity(Intent(app, MainActivity::class.java)).use { scenario ->
            compose.onNodeWithContentDescription("Gym tab").performClick()
            compose.onNodeWithTag("next-set-focus").performClick()
            compose.onNodeWithTag("quick-set-reps-$firstSetId").performScrollTo().performTextReplacement("6")
            closeSoftKeyboard()
            compose.onNodeWithTag("quick-set-save-next-$firstSetId").performScrollTo().performClick()
            compose.waitUntil(10_000) { firstSetCompleted() }
            compose.onNodeWithText("NEXT · Navigation row · Set 1").assertIsDisplayed()
            compose.onNodeWithTag("active-set-composer").assertIsDisplayed()
            compose.onNodeWithTag("active-workout-list").performScrollToIndex(0)
            compose.onNodeWithTag("next-set-focus").performClick()
            compose.onNodeWithTag("active-set-composer").assertIsDisplayed()
            scenario.recreate()
            compose.onNodeWithText("NEXT · Navigation row · Set 1").assertIsDisplayed()
            compose.onNodeWithTag("next-set-focus").performClick()
            compose.onNodeWithTag("active-set-composer").assertIsDisplayed()
            captureVisualCatalogSurface("gym.workout.grouped-next-set")
            assertEquals(1, runBlocking { app.gymRepository.sets.first() }.count { it.completed })
        }
    }
}
