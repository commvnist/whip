package com.whip.app.ui

import com.whip.app.AndroidFontScale
import com.whip.app.AndroidFontScaleRule
import com.whip.app.assertDialogFontScale

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.captureVisualCatalogSurface
import com.whip.app.data.GoalDeletionImpact
import com.whip.app.domain.ElapsedDisplayUnit
import com.whip.app.domain.ElapsedDisplayFormat
import com.whip.app.domain.Goal
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalAggregationPeriod
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalClosureSnapshot
import com.whip.app.domain.GoalDirection
import com.whip.app.domain.GoalElapsedResetEvent
import com.whip.app.domain.GoalMilestone
import com.whip.app.domain.GoalPaceType
import com.whip.app.domain.GoalProjection
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.MeasurementEntry
import com.whip.app.domain.MeasurementEntryStatus
import com.whip.app.domain.MeasurementSourceType
import com.whip.app.domain.UnitDefinition
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.BuiltInUnits
import com.whip.app.domain.projectGoal
import com.whip.app.domain.GoalConsistencyPeriod
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.test.espresso.Espresso.closeSoftKeyboard
import com.whip.app.ui.theme.WhipTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoalSecondaryMutationUiTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)

    private fun editorControl(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        compose.onNodeWithTag("goal-editor-fields").performScrollToNode(matcher)
        return compose.onNode(matcher)
    }

    @Test @AndroidFontScale fun unitChangesPreserveTargetsAndPartialDrafts() {
        var saved: GoalDraft? = null
        val restoration = StateRestorationTester(compose)
        val authored = goal(type = GoalType.ReduceValue).copy(
            direction = GoalDirection.Decrease, unitId = "kilogram", baseline = 80.123456789, targetMin = 70.0,
        )
        restoration.setContent { WhipTheme(dynamicColor = false) {
            GoalEditorDialog(projection(authored), today = TODAY, activeZoneId = ZoneId.of("UTC"),
                nowMillis = TODAY.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(), customUnits = emptyList(),
                onDismiss = {}, onSave = { saved = it })
        } }
        editorControl(hasTestTag("goal-editor-target")).performTextReplacement("1e")
        closeSoftKeyboard()
        editorControl(hasContentDescription("Unit:", substring = true)).performClick()
        compose.onNodeWithText("pounds (lb)").performScrollTo().performClick()
        editorControl(hasTestTag("goal-unit-change-error")).assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        editorControl(hasTestTag("goal-editor-target")).assertTextContains("1e").performTextReplacement("70")
        closeSoftKeyboard()
        editorControl(hasContentDescription("Unit:", substring = true)).performClick()
        compose.onNodeWithText("pounds (lb)").performScrollTo().performClick()
        editorControl(hasTestTag("goal-editor-target")).assertIsDisplayed()
        captureVisualCatalogSurface("ux-audit-2.goals.unit-conversion.large")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle {
            val draft = requireNotNull(saved)
            assertEquals("pound", draft.unitId)
            val unit = requireNotNull(BuiltInUnits.get(draft.unitId))
            assertEquals(70.0, unit.toCanonical(requireNotNull(draft.targetMin)), 0.00000001)
            assertEquals(80.123456789, unit.toCanonical(requireNotNull(draft.baseline)), 0.0000000001)
        }
    }

    @Test fun startDateControlsBackdatedProgressAndSurvivesRestoration() {
        var saved: GoalDraft? = null
        val restoration = StateRestorationTester(compose)
        val initial = GoalDraft("Backdated project", type = GoalType.ReachValue, targetMin = 10.0, startDate = TODAY.minusDays(10), deadline = TODAY.minusDays(1))
        restoration.setContent { CompositionLocalProvider(LocalWhipToday provides TODAY) { WhipTheme(dynamicColor = false) {
            GoalEditorDialog(null, initialDraft = initial, today = TODAY, activeZoneId = ZoneId.of("UTC"),
                nowMillis = TODAY.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(), customUnits = emptyList(),
                onDismiss = {}, onSave = { saved = it })
        } } }
        val format = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
        editorControl(hasTestTag("goal-start-date")).assertTextContains(initial.startDate.format(format), substring = true).performClick()
        if (compose.onAllNodesWithTag("date-picker-month-year").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("date-picker-month-year").performScrollTo().performClick()
        }
        compose.onNodeWithTag("date-picker-today").performScrollTo().performClick()
        compose.onNodeWithText("Set").performClick()
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle { assertNull(saved) }
        editorControl(hasText("Start date must be on or before the deadline.")).assertIsDisplayed()
        editorControl(hasTestTag("goal-remove-deadline")).performClick()
        restoration.emulateSavedInstanceStateRestore()
        editorControl(hasTestTag("goal-start-date")).assertTextContains(TODAY.format(format), substring = true)
        captureVisualCatalogSurface("ux-audit-2.goals.start-date")
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle { assertEquals(TODAY, requireNotNull(saved).startDate); assertNull(requireNotNull(saved).deadline) }
    }

    @Test @AndroidFontScale fun rangeAndConsistencyOutcomesKeepTheirMeasurementMeaning() {
        val authored = goal(type = GoalType.MaintainRange).copy(dimension = UnitDimension.Temperature, unitId = "fahrenheit",
            aggregation = GoalAggregation.TimeInRange, targetMin = 18.0, targetMax = 22.0, startDate = TODAY)
        val entries = listOf(20.0, 30.0).mapIndexed { index, value -> MeasurementEntry("range-$index", authored.measurementId, value, value,
            "celsius", MeasurementEntryStatus.Recorded, TODAY.atStartOfDay(ZoneId.of("UTC")).toInstant().plusSeconds(index.toLong()), TODAY,
            "UTC", 0, MeasurementSourceType.Manual, null, "", 1, 1) }
        var shown by mutableStateOf(projectGoal(authored, entries, emptyList(), TODAY))
        compose.setContent { WhipTheme(dynamicColor = false) {
            GoalActionsDialog(shown, zoneId = ZoneId.of("UTC"), nowMillis = TODAY.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
                onDismiss = {}, onEditMeasurement = {}, onRecordProgress = {}, onResetElapsed = {}, onEdit = {}, onDuplicate = {},
                onPin = {}, onPause = {}, onComplete = {}, onAbandon = {}, onReopen = {}, onArchive = {}, onDelete = {})
        } }
        fun detail(matcher: SemanticsMatcher): SemanticsNodeInteraction {
            compose.onNode(hasScrollToIndexAction() and hasAnyAncestor(hasTestTag("goal-detail-surface"))).performScrollToNode(matcher)
            return compose.onNode(matcher and hasAnyAncestor(hasTestTag("goal-detail-surface")))
        }
        detail(hasTestTag("goal-inspector-outcome")).assertTextContains("50.0 % of observations in range", substring = true)
        captureVisualCatalogSurface("ux-audit-2.goals.range-outcome.large")
        val snapshot = GoalClosureSnapshot(99, "closed", authored.id, 100, 50.0, .5, GoalStatus.Completed)
        compose.runOnIdle { shown = shown.copy(goal = authored.copy(status = GoalStatus.Completed, targetMin = 100.0, targetMax = 110.0), terminalSnapshot = snapshot, closureSnapshots = listOf(snapshot)) }
        detail(hasTestTag("goal-inspector-outcome")).assertTextContains("50.0 % of observations in range at closure")
        compose.onNodeWithText("History").performClick()
        detail(hasTestTag("goal-closure-history-99")).assertTextContains("recorded value 50.0 % of observations in range", substring = true)
        // A physical Reach outcome may later be viewed through a Range definition; no old range was saved.
        val physicalSnapshot = snapshot.copy(goalId = 19, value = 500.0, progress = .5)
        compose.runOnIdle { shown = shown.copy(goal = authored.copy(id = 19, status = GoalStatus.Completed, aggregation = GoalAggregation.Latest, targetMin = 400.0, targetMax = 600.0),
            currentValue = 500.0, progress = .5, terminalSnapshot = physicalSnapshot, closureSnapshots = listOf(physicalSnapshot)) }
        detail(hasTestTag("goal-inspector-outcome")).assertTextContains("Observed value at closure", substring = true)
        compose.onAllNodesWithText("Outside range at closure", substring = true).assertCountEquals(0)
        val consistency = authored.copy(id = 18, type = GoalType.Consistency, dimension = UnitDimension.Count, unitId = "count",
            aggregation = GoalAggregation.CompletionCount, targetMin = 1.0, targetMax = null, startDate = TODAY.minusDays(3),
            consistencyPeriod = GoalConsistencyPeriod.Day, consistencyRequiredPeriods = 3)
        val periodEntries = entries.mapIndexed { index, entry -> entry.copy(localDate = TODAY.minusDays(3 - index.toLong()), canonicalValue = 1.0) }
        compose.runOnIdle { shown = projectGoal(consistency, periodEntries, emptyList(), TODAY) }
        detail(hasTestTag("goal-inspector-outcome")).assertTextContains("2 of 3 successful periods")
        detail(hasText("Last tracked day", substring = true)).assertIsDisplayed()
        captureVisualCatalogSurface("ux-audit-2.goals.consistency-outcome.large")
        compose.runOnIdle { assertTrue(requireNotNull(shown.consistency).trackingWindowEnded) }
    }

    @Test fun editingOnlyAProgressNotePreservesItsTinyEnteredValueThroughRestoration() {
        val value = 0.000000012345678
        val entry = MeasurementEntry("precise", "measurement-17", value, value, "pound", MeasurementEntryStatus.Recorded,
            TODAY.atStartOfDay(ZoneId.of("UTC")).toInstant(), TODAY, "UTC", 0, MeasurementSourceType.Goal, null, "old note", 1, 1)
        var recorded: Double? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent { WhipTheme(dynamicColor = false) {
            GoalMeasurementDialog(projection(goal()), TODAY, entry, onDismiss = {}, onRecord = { amount, _, note ->
                recorded = amount
                assertEquals("corrected note", note)
            })
        } }
        compose.onNodeWithTag("goal-measurement-value").performScrollTo().assertTextContains("0.000000012345678")
        compose.onNodeWithTag("goal-measurement-note").performScrollTo().performTextReplacement("corrected note")
        closeSoftKeyboard()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("goal-measurement-save").performClick()
        compose.runOnIdle { assertEquals(value, requireNotNull(recorded), 0.0) }
    }

    @Test fun deadlineRemovalAndCalculationSettingsSurviveRestoration() {
        var saved: GoalDraft? = null
        val restoration = StateRestorationTester(compose)
        val authored = goal(deadline = TODAY.plusDays(30)).copy(aggregationPeriod = GoalAggregationPeriod.RollingDays, rollingDays = 7)
        restoration.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                GoalEditorDialog(projection(authored), today = TODAY, activeZoneId = ZoneId.of("UTC"),
                    nowMillis = TODAY.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(), customUnits = emptyList(),
                    onDismiss = {}, onSave = { saved = it })
            }
        }
        compose.onNodeWithTag("goal-editor-fields").performScrollToNode(hasText("Time window"))
        compose.onNodeWithText("Time window").assertIsDisplayed()
        captureVisualCatalogSurface("ux-upgrades.goals.calculation-settings")
        listOf("Decimal Places (0–6)" to ("8" to "1"), "Rolling Days" to ("0" to "7")).forEach { (label, values) ->
            val field = hasText(label, substring = true) and hasSetTextAction()
            compose.onNodeWithTag("goal-editor-fields").performScrollToNode(field)
            compose.onNode(field).performTextReplacement(values.first)
            compose.onNode(field).assertTextContains(values.first)
            editorControl(hasText("Progress Calculation")).performClick()
            compose.onNodeWithText("Save").performClick()
            compose.runOnIdle { assertNull(saved) }
            compose.onNodeWithTag("goal-editor-fields").performScrollToNode(field)
            compose.onNode(field).assertIsDisplayed().performTextReplacement(values.second)
            compose.onNode(field).assertTextContains(values.second)
            closeSoftKeyboard()
        }
        editorControl(hasTestTag("goal-remove-deadline")).performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("goal-editor-fields").performScrollToNode(hasText("Add Deadline"))
        compose.onNodeWithText("Add Deadline").assertIsDisplayed()
        editorControl(hasText("Decimal Places (0–6)", substring = true) and hasSetTextAction()).assertTextContains("1")
        editorControl(hasText("Rolling Days", substring = true) and hasSetTextAction()).assertTextContains("7")
        closeSoftKeyboard()
        compose.onNode(hasText("Save") and hasClickAction()).assertIsDisplayed().assertIsEnabled().performClick()
        compose.runOnIdle {
            assertNull(requireNotNull(saved).deadline)
            assertEquals(GoalAggregationPeriod.RollingDays, requireNotNull(saved).aggregationPeriod)
            assertEquals(7, requireNotNull(saved).rollingDays)
            assertEquals(GoalPaceType.None, requireNotNull(saved).paceType)
        }
    }

    @Test fun milestoneWeightKeepsPartialDecimalAndValidatesBlankDraft() {
        var saved: GoalDraft? = null
        val restoration = StateRestorationTester(compose)
        val initial = projection(goal(type = GoalType.WeightedMilestones)).let { projection ->
            projection.copy(milestones = projection.milestones + projection.milestones.single().copy(
                id = 92, uuid = "milestone-92", name = "Celebrate", position = 1, weight = 2.0,
            ))
        }
        restoration.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                GoalEditorDialog(initial, today = TODAY,
                    activeZoneId = ZoneId.of("UTC"), nowMillis = TODAY.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
                    customUnits = emptyList(), onDismiss = {}, onSave = { saved = it })
            }
        }
        editorControl(hasTestTag("goal-milestone-details-0")).performClick()
        editorControl(hasTestTag("goal-milestone-weight-0")).performTextReplacement("")
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle { assertNull(saved) }
        editorControl(hasTestTag("goal-milestone-weight-0")).performTextReplacement("0.")
        compose.onNodeWithTag("goal-milestone-weight-0").assertTextContains("0.")
        val move = editorControl(hasContentDescription("Reorder Finish", substring = true))
            .fetchSemanticsNode().config[SemanticsActions.CustomActions].single { it.label == "Move Finish down" }
        compose.runOnIdle { assertTrue(move.action()) }
        editorControl(hasTestTag("goal-milestone-weight-1")).assertTextContains("0.")
        restoration.emulateSavedInstanceStateRestore()
        editorControl(hasTestTag("goal-milestone-weight-1")).assertTextContains("0.")
        compose.onNodeWithTag("goal-milestone-weight-1").performTextReplacement("0.5")
        captureVisualCatalogSurface("ux-upgrades.goals.milestone-weight")
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle {
            assertEquals(listOf("Celebrate", "Finish"), requireNotNull(saved).milestones.map { it.name })
            assertEquals(2.0, requireNotNull(saved).milestones.first().weight, 0.0)
            assertEquals(0.5, requireNotNull(saved).milestones.last().weight, 0.0)
        }
    }

    @Test
    fun captureGoalComponentCatalog() {
        var surface by mutableStateOf(GoalCatalogSurface.EditorCreate)
        val deletionImpact = GoalDeletionImpact(
            goalId = 17,
            exists = true,
            name = "Launch",
            status = GoalStatus.Active.name,
            archived = false,
            milestoneCount = 3,
            completedMilestoneCount = 2,
            progressEntryCount = 5,
            closureSnapshotCount = 1,
            elapsedResetEventCount = 2,
            revisionToken = "reviewed-revision",
        )
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                when (surface) {
                    GoalCatalogSurface.EditorCreate,
                    GoalCatalogSurface.EditorEdit,
                    -> GoalEditorDialog(
                        projection = projection(goal()).takeIf { surface == GoalCatalogSurface.EditorEdit },
                        today = TODAY,
                        activeZoneId = ZoneId.of("America/Toronto"),
                        nowMillis = Instant.parse("2026-09-06T16:00:00Z").toEpochMilli(),
                        customUnits = emptyList(),
                        onDismiss = {},
                        onSave = {},
                    )
                    GoalCatalogSurface.Measurement -> GoalMeasurementDialog(
                        projection = projection(goal()),
                        today = TODAY,
                        entry = null,
                        onDismiss = {},
                        onRecord = { _, _, _ -> },
                    )
                    GoalCatalogSurface.Actions -> GoalActionsDialog(
                        projection = projection(goal()),
                        zoneId = ZoneId.of("America/Toronto"),
                        nowMillis = Instant.parse("2026-09-06T16:00:00Z").toEpochMilli(),
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
                    GoalCatalogSurface.Delete -> GoalPermanentDeleteDialog(
                        goalName = deletionImpact.name,
                        impact = deletionImpact,
                        preparing = false,
                        saving = false,
                        persistenceError = null,
                        onDismiss = {},
                        onReviewUpdatedImpact = {},
                        onConfirm = {},
                    )
                }
            }
        }

        fun show(next: GoalCatalogSurface, surfaceId: String) {
            compose.runOnIdle { surface = next }
            compose.waitForIdle()
            captureVisualCatalogSurface(surfaceId)
        }

        captureVisualCatalogSurface("goals.editor.create")
        show(GoalCatalogSurface.EditorEdit, "goals.editor.edit")
        show(GoalCatalogSurface.Measurement, "goals.measurement")
        show(GoalCatalogSurface.Actions, "goals.actions")
        show(GoalCatalogSurface.Delete, "goals.permanent-delete")
    }

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
    @AndroidFontScale
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

        compose.assertDialogFontScale()
        captureVisualCatalogSurface("goals.measurement.large")

        compose.onNodeWithTag("goal-measurement-value").performTextReplacement("123.5")
        compose.onNodeWithTag("goal-measurement-save").performClick()
        compose.runOnIdle {
            assertEquals("The progress request must be submitted once", 1, submissions)
            assertTrue("The host should hold the save state", saving)
        }
        fun waitForSavingOverlay() {
            compose.waitUntil(5_000) {
                runCatching {
                    compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()
                }.isSuccess
            }
        }
        waitForSavingOverlay()
        pressBack()
        waitForSavingOverlay()
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
        var entry by mutableStateOf(measurementEntry(localDate = TODAY.plusDays(1), enteredUnitId = customUnit.id))
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

        compose.runOnIdle { entry = measurementEntry(localDate = TODAY.minusDays(8), enteredUnitId = customUnit.id) }
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
            closureSnapshotCount = 1,
            elapsedResetEventCount = 2,
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
        compose.onNodeWithTag("goal-inspector-outcome-card").assertIsDisplayed()
        compose.onAllNodesWithTag("goal-inspector-progress-card").assertCountEquals(0)
        compose.onNodeWithTag("goal-inspector-outcome")
            .assertContentDescriptionContains("19 days", substring = true)
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
                    projection = projection(goal(type = GoalType.WeightedMilestones, archived = archived)),
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

        compose.onNodeWithTag("goal-expand-17", useUnmergedTree = true).performClick()
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
        measurementId = "measurement-17",
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
        elapsedDisplay = ElapsedDisplayFormat.selected(ElapsedDisplayUnit.Days),
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

    private fun measurementEntry(localDate: LocalDate, enteredUnitId: String) = MeasurementEntry(
        id = "entry-${localDate.toEpochDay()}",
        measurementId = "measurement-17",
        canonicalValue = 10.0,
        enteredValue = 10.0,
        enteredUnitId = enteredUnitId,
        status = MeasurementEntryStatus.Recorded,
        timestamp = Instant.parse("2026-09-01T12:00:00Z"),
        localDate = localDate,
        zoneId = "UTC",
        offsetSeconds = 0,
        sourceType = MeasurementSourceType.Goal,
        sourceId = "goal-17",
        note = "",
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    companion object {
        private val TODAY: LocalDate = LocalDate.of(2026, 9, 1)
    }
}

private enum class GoalCatalogSurface { EditorCreate, EditorEdit, Measurement, Actions, Delete }
