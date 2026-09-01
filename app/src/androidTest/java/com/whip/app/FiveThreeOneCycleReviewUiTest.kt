package com.whip.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.FiveThreeOneProgressionCategory
import com.whip.app.domain.FiveThreeOneProgressionRecommendation
import com.whip.app.domain.TrainingMaxCycleDecision
import com.whip.app.domain.TrainingMaxDecisionAction
import com.whip.app.ui.FiveThreeOneCycleReview
import com.whip.app.ui.FiveThreeOneCycleReviewDialog
import com.whip.app.ui.FiveThreeOneLiftCycleReview
import com.whip.app.ui.theme.WhipTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FiveThreeOneCycleReviewUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun defaultsToStandardAndValidatesSuggestionAndCustomDecision() {
        var applied: List<TrainingMaxCycleDecision>? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                FiveThreeOneCycleReviewDialog(
                    review = review(),
                    onDismiss = {},
                    onApply = { decisions -> applied = decisions },
                )
            }
        }

        compose.onNodeWithTag("training-max-choice-standard-7").performClick()
        compose.onNodeWithTag("apply-training-max-decisions").assertIsEnabled().performClick()
        compose.runOnIdle {
            val decision = requireNotNull(applied).single()
            assertEquals(TrainingMaxDecisionAction.UseStandard, decision.action)
            assertEquals(10.0, decision.requestedDelta, 0.0)
            assertEquals(300.0, decision.expectedCurrentTrainingMax!!, 0.0)
        }

        applied = null
        compose.onNodeWithTag("training-max-choice-suggestion-7").performClick()
        compose.onNodeWithTag("apply-training-max-decisions").assertIsEnabled().performClick()
        compose.runOnIdle {
            val decision = requireNotNull(applied).single()
            assertEquals(TrainingMaxDecisionAction.UseSuggestion, decision.action)
            assertEquals(5.0, decision.requestedDelta, 0.0)
        }

        applied = null
        compose.onNodeWithTag("training-max-choice-ignore-7").performClick()
        compose.onNodeWithTag("apply-training-max-decisions").assertIsEnabled().performClick()
        compose.runOnIdle {
            val decision = requireNotNull(applied).single()
            assertEquals(TrainingMaxDecisionAction.IgnoreRecommendation, decision.action)
            assertEquals(0.0, decision.requestedDelta, 0.0)
        }

        applied = null
        compose.onNodeWithTag("training-max-choice-custom-7").performClick()
        compose.onNodeWithTag("training-max-custom-delta-7").performTextReplacement("20")
        compose.onNodeWithTag("apply-training-max-decisions").assertIsNotEnabled()
        compose.onNodeWithTag("training-max-custom-delta-7").performTextReplacement("-10")
        compose.onNodeWithTag("apply-training-max-decisions").assertIsEnabled().performClick()
        compose.runOnIdle {
            val decision = requireNotNull(applied).single()
            assertEquals(TrainingMaxDecisionAction.Custom, decision.action)
            assertEquals(-10.0, decision.requestedDelta, 0.0)
        }
    }

    @Test
    fun applyActionRemainsReachableAtTwoHundredPercentText() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                WhipTheme(darkTheme = true, dynamicColor = false) {
                    FiveThreeOneCycleReviewDialog(
                        review = review(),
                        onDismiss = {},
                        onApply = {},
                    )
                }
            }
        }

        compose.onNodeWithTag("training-max-review-7").assertIsDisplayed()
        compose.onNodeWithTag("apply-training-max-decisions").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun changedEvidenceResetsAChoiceInsteadOfApplyingAnUnreviewedSuggestion() {
        var currentReview by mutableStateOf(review())
        var revision by mutableStateOf(4L)
        var applied: List<TrainingMaxCycleDecision>? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                FiveThreeOneCycleReviewDialog(
                    review = currentReview,
                    reviewRevision = revision,
                    onDismiss = {},
                    onApply = { applied = it },
                )
            }
        }

        compose.onNodeWithTag("training-max-choice-suggestion-7").performClick()
        compose.runOnIdle {
            val lift = currentReview.lifts.single()
            currentReview = currentReview.copy(
                lifts = listOf(
                    lift.copy(
                        recommendation = lift.recommendation.copy(
                            suggestedDelta = 2.5,
                            confidence = 0.61,
                        ),
                    ),
                ),
            )
            revision = 5
        }
        compose.onNodeWithTag("apply-training-max-decisions").performClick()
        compose.runOnIdle {
            assertEquals(TrainingMaxDecisionAction.UseStandard, requireNotNull(applied).single().action)
        }
    }

    private fun review() = FiveThreeOneCycleReview(
        routineName = "Custom 5/3/1",
        cycle = 2,
        lifts = listOf(
            FiveThreeOneLiftCycleReview(
                exerciseId = 7,
                exerciseName = "Zercher Squat",
                currentTrainingMax = 300.0,
                unitId = "pound",
                standardDelta = 10.0,
                eligible = true,
                recommendation = FiveThreeOneProgressionRecommendation(
                    category = FiveThreeOneProgressionCategory.LowerIncrease,
                    suggestedDelta = 5.0,
                    standardDelta = 10.0,
                    confidence = 0.7,
                    reasons = listOf("All required work passed.", "Effort evidence was marginal."),
                    engineVersion = "five-three-one-progression/1",
                ),
            ),
        ),
    )
}
