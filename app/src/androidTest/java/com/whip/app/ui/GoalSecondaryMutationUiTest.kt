package com.whip.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.data.GoalDeletionImpact
import com.whip.app.domain.ElapsedDisplayUnit
import com.whip.app.domain.Goal
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalClosureSnapshot
import com.whip.app.domain.GoalDirection
import com.whip.app.domain.GoalElapsedResetEvent
import com.whip.app.domain.GoalMilestone
import com.whip.app.domain.GoalPaceType
import com.whip.app.domain.GoalProjection
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.MetricEntry
import com.whip.app.domain.MetricEntryStatus
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import com.whip.app.ui.theme.WhipTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoalSecondaryMutationUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun leavingAnActionSurfaceClearsItsFailedRequestBeforeOpeningAnotherDialog() {
        val request = mutableStateOf<String?>(null)
        val error = mutableStateOf<String?>(null)
        val coordinator = EntitySaveCoordinator(request, error, "goal-workspace")
        coordinator.finishFailure("Stale action failure")
        var transitioned = false

        coordinator.leaveGoalActionSurface { transitioned = true }

        assertTrue(transitioned)
        assertNull(coordinator.requestId)
        assertNull(coordinator.errorMessage)
    }

    @Test
    fun terminalMilestoneSummaryUsesTheFrozenClosureRatherThanTheEditedDefinition() {
        val terminal = GoalClosureSnapshot(
            id = 8,
            uuid = "closure-8",
            goalId = 17,
            completedAtMillis = 100,
            value = null,
            progress = 0.5,
            status = GoalStatus.Completed,
            completedMilestoneCount = 2,
            totalMilestoneCount = 4,
        )
        val closed = projection(goal(type = GoalType.WeightedMilestones, status = GoalStatus.Completed)).copy(
            terminalSnapshot = terminal,
            closureSnapshots = listOf(terminal),
        )

        assertEquals("2/4 milestones", closed.collectionStatus(nowMillis = 1_000))
    }

    @Test
    fun progressFailureKeepsDraftAndSavingBlocksBackAndDuplicateSubmit() {
        var saving by mutableStateOf(false)
        var error by mutableStateOf<String?>(null)
        var submissions = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale = 2f),
                    LocalWhipDialogPlacement provides WhipDialogPlacement(maxWidth = 320.dp),
                ) {
                    GoalMeasurementDialog(
                        projection = projection(goal()),
                        today = TODAY,
                        entry = null,
                        onDismiss = {},
                        onRecord = { _, _, _ ->
                            submissions++
                            saving = true
                        },
                        saving = saving,
                        persistenceError = error,
                    )
                }
            }
        }

        compose.onNodeWithTag("goal-measurement-value").performTextReplacement("123.5")
        compose.onNodeWithTag("goal-measurement-save").performClick()
        compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()
        pressBack()
        compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1, submissions)
            saving = false
            error = "Storage is unavailable"
        }

        compose.onNodeWithTag("goal-measurement-save-problem").assertIsDisplayed()
        compose.onNodeWithTag("goal-measurement-value").assertTextContains("123.5")
        compose.onNodeWithTag("goal-measurement-save").assertIsEnabled()
    }

    @Test
    fun progressDatePolicyRejectsFutureAndConfirmsOutsideGoalWindow() {
        val trackingGoal = goal(startDate = TODAY.minusDays(7), deadline = TODAY.plusDays(7))
        val customUnit = UnitDefinition(
            id = "custom-stone-id",
            name = "Stone",
            symbol = "st",
            dimension = UnitDimension.Mass,
            toCanonicalFactor = 6.35029318,
            custom = true,
        )
        var entry by mutableStateOf(metricEntry(localDate = TODAY.plusDays(1), enteredUnitId = customUnit.id))
        var saves = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                GoalMeasurementDialog(
                    projection = projection(trackingGoal.copy(unitId = customUnit.id)),
                    today = TODAY,
                    entry = entry,
                    customUnits = listOf(customUnit),
                    onDismiss = {},
                    onRecord = { _, _, _ -> saves++ },
                )
            }
        }

        compose.onNodeWithTag("goal-measurement-value").assertIsDisplayed()
        compose.onNodeWithText("Observed Value (st)", substring = true).assertExists()
        compose.onNodeWithTag("goal-measurement-date-problem").assertIsDisplayed()
        compose.onNodeWithTag("goal-measurement-save").assertIsNotEnabled()
        compose.onAllNodesWithText(customUnit.id, substring = true).fetchSemanticsNodes().let { nodes ->
            assertTrue("Opaque custom-unit ID leaked into the UI", nodes.isEmpty())
        }

        compose.runOnIdle { entry = metricEntry(localDate = TODAY.minusDays(8), enteredUnitId = customUnit.id) }
        compose.onNodeWithTag("goal-measurement-window-warning").assertIsDisplayed()
        compose.onNodeWithTag("goal-measurement-save").assertIsEnabled().performClick()
        compose.onNodeWithTag("goal-measurement-window-confirmation").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, saves) }
        compose.onNodeWithTag("goal-measurement-window-confirm").performClick()
        compose.runOnIdle { assertEquals(1, saves) }
    }

    @Test
    fun permanentDeleteShowsCompleteImpactAndRequiresRereviewAfterStaleFailure() {
        val impact = GoalDeletionImpact(
            goalId = 17,
            exists = true,
            name = "Launch",
            status = GoalStatus.Completed.name,
            archived = true,
            milestoneCount = 3,
            completedMilestoneCount = 2,
            progressEntryCount = 5,
            legacyClosureSnapshotCount = 1,
            elapsedResetEventCount = 2,
            linkRuleCount = 4,
            contributionCount = 6,
            revisionToken = "reviewed-revision",
        )
        var reviewRequests = 0
        var deletes = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                GoalPermanentDeleteDialog(
                    goalName = impact.name,
                    impact = impact,
                    preparing = false,
                    saving = false,
                    persistenceError = "The Goal or its history changed while the confirmation was open.",
                    onDismiss = {},
                    onReviewUpdatedImpact = { reviewRequests++ },
                    onConfirm = { deletes++ },
                )
            }
        }

        compose.onNodeWithText("archived completed Goal", substring = true).assertIsDisplayed()
        compose.onNodeWithText("5 progress updates", substring = true).assertIsDisplayed()
        compose.onNodeWithText("3 milestones (2 completed)", substring = true).assertIsDisplayed()
        compose.onNodeWithText("2 timer reset history events", substring = true).assertIsDisplayed()
        compose.onNodeWithText("4 linked automation rules", substring = true).assertIsDisplayed()
        compose.onNodeWithText("6 linked contribution records", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("goal-delete-confirm").assertIsNotEnabled()
        compose.onNodeWithTag("goal-delete-review-impact").performClick()
        compose.runOnIdle {
            assertEquals(1, reviewRequests)
            assertEquals(0, deletes)
        }
    }

    @Test
    fun archivedInspectorPreservesLifecycleHistoryAndHidesMisleadingPin() {
        val archivedGoal = goal(
            type = GoalType.ElapsedSince,
            status = GoalStatus.Completed,
            archived = true,
            elapsedStartMillis = Instant.parse("2026-08-01T12:00:00Z").toEpochMilli(),
        )
        val closure = GoalClosureSnapshot(
            id = 1,
            uuid = "closure-1",
            goalId = archivedGoal.id,
            completedAtMillis = Instant.parse("2026-08-20T15:00:00Z").toEpochMilli(),
            value = 10.0,
            progress = 0.75,
            status = GoalStatus.Completed,
            elapsedDurationMillis = 19L * 24L * 60L * 60L * 1_000L,
        )
        val projection = projection(archivedGoal).copy(
            closureSnapshots = listOf(closure),
            terminalSnapshot = closure,
            elapsedResetEvents = listOf(
                GoalElapsedResetEvent(
                    id = 2,
                    uuid = "reset-2",
                    goalId = archivedGoal.id,
                    goalUuid = archivedGoal.uuid,
                    previousStartMillis = Instant.parse("2026-08-01T12:00:00Z").toEpochMilli(),
                    newStartMillis = Instant.parse("2026-08-05T12:00:00Z").toEpochMilli(),
                    resetAtMillis = Instant.parse("2026-08-10T12:00:00Z").toEpochMilli(),
                    elapsedDurationMillis = 9L * 24L * 60L * 60L * 1_000L,
                ),
            ),
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                GoalActionsDialog(
                    projection = projection,
                    zoneId = ZoneId.of("UTC"),
                    nowMillis = Instant.parse("2026-09-01T12:00:00Z").toEpochMilli(),
                    onDismiss = {},
                    onEditMeasurement = {},
                    onRecordProgress = {},
                    onResetElapsed = {},
                    onEdit = {},
                    onDuplicate = {},
                    onPin = {},
                    onPause = {},
                    onComplete = {},
                    onAbandon = {},
                    onReopen = {},
                    onArchive = {},
                    onDelete = {},
                )
            }
        }

        compose.onNodeWithText("Archived").assertIsDisplayed()
        compose.onNodeWithTag("goal-inspector-outcome").assertTextContains("19 days", substring = true)
        compose.onNodeWithText("Restore Goal").assertIsDisplayed()
        compose.onNodeWithText("History").performClick()
        compose.onNodeWithTag("goal-closure-history-1").assertTextContains("Completed on", substring = true)
        compose.onNodeWithTag("goal-reset-history-2").assertTextContains("Counter origin changed", substring = true)
        compose.onNodeWithText("Options").performClick()
        compose.onAllNodesWithText("Pin to Whip Home").fetchSemanticsNodes().let { nodes ->
            assertTrue("Closed/archived Goal offered a misleading Home pin", nodes.isEmpty())
        }
    }

    @Test
    fun milestoneIsEditableOnlyForAnUnarchivedActiveGoal() {
        var archived by mutableStateOf(true)
        var toggles = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                GoalCard(
                    projection = projection(goal(archived = archived)),
                    onOpen = {},
                    onEdit = {},
                    onRecord = {},
                    onResetElapsed = {},
                    onToggleMilestone = { boundary, completed ->
                        assertEquals(91L, boundary.milestoneId)
                        assertTrue(completed)
                        toggles++
                    },
                )
            }
        }

        compose.onNodeWithTag("goal-milestone-91", useUnmergedTree = true).assertIsNotEnabled()
        compose.runOnIdle { archived = false }
        compose.onNodeWithTag("goal-milestone-91", useUnmergedTree = true).assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, toggles) }
    }

    private fun goal(
        type: GoalType = GoalType.ReachValue,
        status: GoalStatus = GoalStatus.Active,
        archived: Boolean = false,
        pinned: Boolean = false,
        startDate: LocalDate = TODAY.minusDays(30),
        deadline: LocalDate? = null,
        elapsedStartMillis: Long? = null,
    ) = Goal(
        id = 17,
        uuid = "goal-17",
        metricId = "metric-17",
        name = "Launch",
        description = "",
        area = "",
        tags = emptyList(),
        icon = "🎯",
        type = type,
        dimension = UnitDimension.Mass,
        unitId = "pound",
        precision = 1,
        baseline = 0.0,
        targetMin = 100.0,
        targetMax = null,
        direction = GoalDirection.Increase,
        startDate = startDate,
        deadline = deadline,
        aggregation = GoalAggregation.Latest,
        paceType = GoalPaceType.None,
        reminderMinutes = null,
        status = status,
        archived = archived,
        pinned = pinned,
        position = 0,
        createdAtMillis = 1,
        updatedAtMillis = 2,
        elapsedStartMillis = elapsedStartMillis,
        elapsedDisplayUnit = ElapsedDisplayUnit.Days,
    )

    private fun projection(goal: Goal) = GoalProjection(
        goal = goal,
        currentValue = null,
        progress = null,
        deltaFromBaseline = null,
        expectedProgress = null,
        paceDelta = null,
        forecastDate = null,
        onPace = null,
        milestones = listOf(
            GoalMilestone(
                id = 91,
                uuid = "milestone-91",
                goalId = goal.id,
                name = "Finish",
                position = 0,
                weight = 1.0,
                completed = false,
                completedAtMillis = null,
                reward = "",
                createdAtMillis = 1,
                updatedAtMillis = 1,
            ),
        ),
        entries = emptyList(),
    )

    private fun metricEntry(localDate: LocalDate, enteredUnitId: String) = MetricEntry(
        id = "entry-${localDate.toEpochDay()}",
        metricId = "metric-17",
        canonicalValue = 10.0,
        enteredValue = 10.0,
        enteredUnitId = enteredUnitId,
        status = MetricEntryStatus.Recorded,
        timestamp = Instant.parse("2026-09-01T12:00:00Z"),
        localDate = localDate,
        zoneId = "UTC",
        offsetSeconds = 0,
        sourceType = MetricSourceType.Goal,
        sourceId = "goal-17",
        note = "",
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    companion object {
        private val TODAY: LocalDate = LocalDate.of(2026, 9, 1)
    }
}
