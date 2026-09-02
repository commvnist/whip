package com.whip.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.data.WorkoutDeletionImpact
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.ui.theme.WhipTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutDeletionUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun exactReviewDistinguishesRemovedRecalculatedAndPreservedHistory() {
        var confirmed = 0
        showDialog(
            impact = impact(),
            onConfirm = { confirmed++ },
        )

        compose.onNodeWithText("Delete “5/3/1 Anchor” Permanently?").assertIsDisplayed()
        compose.onNodeWithText("Removed").assertIsDisplayed()
        compose.onNodeWithText("Recalculated").assertIsDisplayed()
        compose.onNodeWithTag("workout-delete-impact-list").performScrollToNode(
            hasText("1 Training Max decision remains", substring = true),
        )
        compose.onNodeWithText("1 Training Max decision remains", substring = true).assertIsDisplayed()
        compose.onNodeWithText("1 Goal contribution", substring = true).assertIsDisplayed()
        compose.onNodeWithText("3 automation occurrences remain", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("workout-delete-confirm").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, confirmed) }
    }

    @Test
    fun staleReviewDisablesDeletionAndOffersAnExplicitRereview() {
        var rereviews = 0
        var deletes = 0
        showDialog(
            impact = impact(),
            errorMessage = "The Workout or its recorded history changed while the confirmation was open.",
            onReviewUpdatedImpact = { rereviews++ },
            onConfirm = { deletes++ },
        )

        compose.onNodeWithTag("workout-delete-error").assertIsDisplayed()
        compose.onNodeWithTag("workout-delete-confirm").assertIsNotEnabled()
        compose.onNodeWithText("Review Updated Impact").performClick()
        compose.runOnIdle {
            assertEquals(1, rereviews)
            assertEquals(0, deletes)
        }
    }

    @Test
    fun activeWorkoutIsBlockedAndActionsRemainReachableAtLargeText() {
        val active = impact().copy(state = WorkoutSessionState.Active.name)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
                LocalWhipDialogPlacement provides WhipDialogPlacement(maxWidth = 320.dp),
            ) {
                WhipTheme(dynamicColor = false) {
                    Box(Modifier.width(320.dp).fillMaxHeight()) {
                        WorkoutPermanentDeleteDialog(
                            workoutName = active.displayName,
                            impact = active,
                            targetMissing = false,
                            preparing = false,
                            deleting = false,
                            errorMessage = null,
                            onDismiss = {},
                            onReviewUpdatedImpact = {},
                            onConfirm = {},
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("workout-delete-impact-list").performScrollToNode(hasText("Active Workout"))
        compose.onNodeWithText("Active Workout").assertIsDisplayed()
        compose.onNodeWithTag("workout-delete-confirm").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Cancel").assertIsDisplayed()
    }

    private fun showDialog(
        impact: WorkoutDeletionImpact?,
        errorMessage: String? = null,
        onReviewUpdatedImpact: () -> Unit = {},
        onConfirm: (WorkoutDeletionImpact) -> Unit = {},
    ) {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutPermanentDeleteDialog(
                    workoutName = impact?.displayName.orEmpty(),
                    impact = impact,
                    targetMissing = false,
                    preparing = impact == null && errorMessage == null,
                    deleting = false,
                    errorMessage = errorMessage,
                    onDismiss = {},
                    onReviewUpdatedImpact = onReviewUpdatedImpact,
                    onConfirm = onConfirm,
                )
            }
        }
    }

    private fun impact() = WorkoutDeletionImpact(
        sessionId = 41,
        sessionUuid = "workout-41",
        displayName = "5/3/1 Anchor",
        localDate = LocalDate.of(2026, 9, 1),
        state = WorkoutSessionState.Finished.name,
        archived = false,
        workoutPlacementCount = 3,
        workoutGroupCount = 1,
        workoutSetCount = 11,
        completedSetCount = 10,
        personalRecordCount = 2,
        trainingMaxDecisionCount = 1,
        contributionCount = 1,
        generatedHabitLogCount = 1,
        triggerOccurrenceCount = 3,
        revisionToken = "reviewed-workout-revision",
    )
}
