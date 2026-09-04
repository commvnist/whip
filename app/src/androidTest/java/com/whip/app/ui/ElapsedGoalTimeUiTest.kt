package com.whip.app.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.ElapsedDisplayUnit
import com.whip.app.domain.Goal
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalDirection
import com.whip.app.domain.GoalPaceType
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.UnitDimension
import com.whip.app.ui.theme.WhipTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ElapsedGoalTimeUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun resetWithoutEditingPreservesTheExactInstantAndActionsRemainUsableAtLargeText() {
        val original = Instant.parse("2026-08-30T14:15:45.321Z")
        var saved: Instant? = null
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale = 2f),
                    LocalWhipDialogPlacement provides WhipDialogPlacement(maxWidth = 320.dp),
                ) {
                    ElapsedGoalResetDialog(
                        goal = elapsedGoal(original),
                        zoneId = ZoneId.of("America/Toronto"),
                        nowMillis = Instant.parse("2026-09-01T12:00:00Z").toEpochMilli(),
                        onDismiss = {},
                        onReset = { saved = it },
                        onResetNow = {},
                    )
                }
            }
        }

        val surface = compose.onNodeWithTag("elapsed-reset-dialog").assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val density = compose.density
        check(surface.width <= with(density) { 320.dp.toPx() } + 1f)
        listOf("elapsed-reset-now", "elapsed-reset-cancel", "elapsed-reset-confirm").forEach { tag ->
            val bounds = compose.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            check(bounds.left >= surface.left && bounds.right <= surface.right) { "$tag overflows the dialog horizontally" }
            check(bounds.top >= surface.top && bounds.bottom <= surface.bottom) { "$tag overflows the dialog vertically" }
            check(bounds.width >= with(density) { 48.dp.toPx() }) { "$tag is narrower than 48dp" }
            check(bounds.height >= with(density) { 48.dp.toPx() }) { "$tag is shorter than 48dp" }
        }
        compose.onNodeWithText("Reset to Chosen Time").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(original, saved) }
    }

    private fun elapsedGoal(started: Instant) = Goal(
        id = 91,
        uuid = "elapsed-91",
        measurementId = "elapsed-measurement-91",
        name = "Days since smoking",
        description = "",
        area = "",
        tags = emptyList(),
        icon = "⏱️",
        type = GoalType.ElapsedSince,
        dimension = UnitDimension.Unitless,
        unitId = "unitless",
        precision = 0,
        baseline = null,
        targetMin = null,
        targetMax = null,
        direction = GoalDirection.Neutral,
        startDate = LocalDate.of(2026, 8, 30),
        deadline = null,
        aggregation = GoalAggregation.Latest,
        paceType = GoalPaceType.None,
        reminderMinutes = null,
        status = GoalStatus.Active,
        pinned = false,
        position = 0,
        createdAtMillis = 1,
        updatedAtMillis = 1,
        elapsedStartMillis = started.toEpochMilli(),
        elapsedDisplayUnit = ElapsedDisplayUnit.Days,
    )
}
