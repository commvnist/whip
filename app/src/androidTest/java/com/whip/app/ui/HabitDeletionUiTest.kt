package com.whip.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.data.HabitDeletionImpact
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitDeletionUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun preparingReviewKeepsTheDialogOpenAndDisablesPermanentDelete() {
        showDialog(
            HabitDeletionReviewUiState(
                habitId = 41,
                habitUuid = "habit-41",
                displayName = "Morning pages",
                requestNamespace = "habit-delete-41",
                preparing = true,
            ),
        )

        compose.onNodeWithTag("habit-permanent-delete-dialog").assertIsDisplayed()
        compose.onNodeWithTag("habit-delete-preparing").assertIsDisplayed()
        compose.onNodeWithTag("habit-delete-confirm").assertIsNotEnabled()
        compose.onNodeWithText("Cancel").assertIsEnabled()
    }

    @Test
    fun staleReviewDisablesDeleteAndOffersExactRefresh() {
        var refreshes = 0
        var deletes = 0
        showDialog(
            state = readyState().copy(
                reviewRequired = true,
                errorMessage = "The Habit or its history changed while the confirmation was open.",
            ),
            onRefresh = { refreshes++ },
            onConfirm = { deletes++ },
        )

        compose.onNodeWithTag("habit-delete-problem").assertIsDisplayed()
        compose.onNodeWithTag("habit-delete-confirm").assertIsNotEnabled()
        compose.onNodeWithText("Review Updated Impact").performClick()
        compose.runOnIdle {
            assertEquals(1, refreshes)
            assertEquals(0, deletes)
        }
    }

    @Test
    fun definitePreCommitFailureRetainsTheReviewForRetryOrRefresh() {
        var refreshes = 0
        var deletes = 0
        showDialog(
            state = readyState(),
            persistenceError = "Storage was temporarily unavailable.",
            onRefresh = { refreshes++ },
            onConfirm = { deletes++ },
        )

        compose.onNodeWithText("Retry Delete").assertIsEnabled().performClick()
        compose.onNodeWithText("Refresh Impact").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(1, refreshes)
            assertEquals(1, deletes)
        }
    }

    private fun showDialog(
        state: HabitDeletionReviewUiState,
        persistenceError: String? = null,
        onRefresh: () -> Unit = {},
        onConfirm: (HabitDeletionImpact) -> Unit = {},
    ) {
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                HabitPermanentDeleteDialog(
                    state = state,
                    saving = false,
                    persistenceError = persistenceError,
                    onDismiss = {},
                    onRefresh = onRefresh,
                    onConfirm = onConfirm,
                )
            }
        }
    }

    private fun readyState() = HabitDeletionReviewUiState(
        habitId = 41,
        habitUuid = "habit-41",
        displayName = "Morning pages",
        requestNamespace = "habit-delete-41",
        impact = HabitDeletionImpact(
            habitId = 41,
            habitUuid = "habit-41",
            displayName = "Morning pages",
            archived = false,
            measurementEntryCount = 3,
            checklistItemCount = 2,
            checklistStateCount = 5,
            logCount = 3,
            skipCount = 1,
            pauseCount = 1,
            timerSessionCount = 2,
            activeTimerSessionCount = 0,
            revisionToken = "reviewed-habit-revision",
        ),
    )
}
