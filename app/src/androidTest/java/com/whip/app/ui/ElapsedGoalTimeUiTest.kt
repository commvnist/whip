package com.whip.app.ui

import com.whip.app.AndroidFontScale
import com.whip.app.AndroidFontScaleRule
import com.whip.app.assertDialogFontScale

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.captureVisualCatalogSurface
import com.whip.app.domain.ElapsedDisplayUnit
import com.whip.app.domain.ElapsedDisplayFormat
import com.whip.app.domain.Goal
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalDirection
import com.whip.app.domain.GoalPaceType
import com.whip.app.domain.GoalProjection
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.UnitDimension
import com.whip.app.ui.theme.WhipTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ElapsedGoalTimeUiTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)

    @Test
    @AndroidFontScale
    fun resetWithoutEditingPreservesTheExactInstantAndActionsRemainUsableAtLargeText() {
        val original = Instant.parse("2026-08-30T14:15:45.321Z")
        var saved: Instant? = null
        var resetNowCount = 0
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
                        onResetNow = { resetNowCount++ },
                    )
                }
            }
        }

        compose.assertDialogFontScale()

        captureVisualCatalogSurface("goals.elapsed-reset")
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
        val resetNow = compose.onNodeWithTag("elapsed-reset-now").fetchSemanticsNode().boundsInRoot
        val cancel = compose.onNodeWithTag("elapsed-reset-cancel").fetchSemanticsNode().boundsInRoot
        val confirm = compose.onNodeWithTag("elapsed-reset-confirm").fetchSemanticsNode().boundsInRoot
        check(resetNow.bottom <= minOf(cancel.top, confirm.top)) {
            "Reset to Now should read as a body alternative above the Cancel/confirm footer"
        }
        compose.onNodeWithText("Reset to Chosen Time").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(original, saved) }
        compose.onNodeWithTag("elapsed-reset-now").performClick()
        compose.runOnIdle { assertEquals(1, resetNowCount) }
    }

    @Test
    fun editorPersistsAnyElapsedUnitCombinationAndShowsALivePreview() {
        val zone = ZoneId.of("America/Toronto")
        val started = zoneMoment(zone, 2026, 7, 1, 8, 0)
        val now = zoneMoment(zone, 2026, 9, 6, 12, 34)
        var saved: GoalDraft? = null
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                GoalEditorDialog(
                    projection = null,
                    initialDraft = GoalDraft(
                        name = "Sober",
                        type = GoalType.ElapsedSince,
                        startDate = LocalDate.of(2026, 7, 1),
                        elapsedStartMillis = started,
                    ),
                    today = LocalDate.of(2026, 9, 6),
                    activeZoneId = zone,
                    nowMillis = now,
                    customUnits = emptyList(),
                    onDismiss = {},
                    onSave = { saved = it },
                )
            }
        }

        compose.onNodeWithTag("goal-editor-fields").performScrollToNode(hasTestTag("elapsed-display-config"))
        compose.onNodeWithTag("elapsed-display-auto").assertIsSelected()
        listOf("months", "weeks", "days", "hours", "minutes").forEach { unit ->
            compose.onNodeWithTag("elapsed-display-$unit").performClick()
        }
        compose.onNodeWithTag("goal-editor-fields").performScrollToNode(hasTestTag("elapsed-display-preview"))
        compose.onNodeWithTag("elapsed-display-preview")
            .assertContentDescriptionContains(
                "2 months · 0 weeks · 5 days · 4 hours · 34 minutes",
                substring = true,
            )
            .assertIsDisplayed()
        captureVisualCatalogSurface("goals.editor.elapsed-display")
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle {
            assertEquals(
                ElapsedDisplayFormat.selected(
                    ElapsedDisplayUnit.Months,
                    ElapsedDisplayUnit.Weeks,
                    ElapsedDisplayUnit.Days,
                    ElapsedDisplayUnit.Hours,
                    ElapsedDisplayUnit.Minutes,
                ),
                saved?.elapsedDisplay,
            )
        }
    }

    @Test
    fun collapsedElapsedGoalCardKeepsEveryConfiguredUnitVisibleAtLargeText() {
        val zone = ZoneId.of("America/Toronto")
        val started = zoneMoment(zone, 2025, 7, 1, 8, 0)
        val now = zoneMoment(zone, 2026, 9, 6, 12, 34)
        val goal = elapsedGoal(Instant.ofEpochMilli(started)).copy(
            name = "Sober",
            elapsedDisplay = ElapsedDisplayFormat.selected(ElapsedDisplayFormat.DISPLAY_ORDER),
        )
        val projection = GoalProjection(
            goal = goal,
            currentValue = null,
            progress = null,
            deltaFromBaseline = null,
            expectedProgress = null,
            paceDelta = null,
            forecastDate = null,
            onPace = null,
            milestones = emptyList(),
            entries = emptyList(),
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                    Box(Modifier.width(320.dp)) {
                        GoalCard(
                            projection = projection,
                            onOpen = {},
                            onEdit = {},
                            onRecord = {},
                            onResetElapsed = {},
                            onToggleMilestone = { _, _ -> },
                            nowMillis = now,
                            zoneId = zone,
                        )
                    }
                }
            }
        }

        compose.onNodeWithContentDescription("1 year · 2 months · 0 weeks · 5 days · 4 hours · 34 minutes")
            .assertIsDisplayed()
    }

    private fun zoneMoment(zone: ZoneId, year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDate.of(year, month, day).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

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
        elapsedDisplay = ElapsedDisplayFormat.selected(ElapsedDisplayUnit.Days),
    )
}
