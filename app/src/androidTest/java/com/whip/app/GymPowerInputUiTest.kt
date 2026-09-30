package com.whip.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOn
import com.whip.app.ui.defaultSearchScope
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.domain.BodyweightLoadPolicy
import com.whip.app.domain.MachineLoadType
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.GymGraphMetric
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.GymMachineDraft
import com.whip.app.domain.MachineLevelDirection
import com.whip.app.domain.GymRoutine
import com.whip.app.domain.RoutineDay
import com.whip.app.domain.RoutineExercise
import com.whip.app.domain.RoutineLoadPrescriptionType
import com.whip.app.domain.RoutineMainWorkScheme
import com.whip.app.domain.RoutinePlacementKind
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineProgramPhaseRole
import com.whip.app.domain.RoutineProgressionMode
import com.whip.app.domain.RoutineSet
import com.whip.app.domain.RoutineSupplementalScheme
import com.whip.app.domain.RoutineTrainingMaxSource
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.TrainingMaxBasisKind
import com.whip.app.domain.WorkoutExercise
import com.whip.app.domain.WorkoutExerciseOutcome
import com.whip.app.domain.WorkoutGroup
import com.whip.app.domain.WorkoutGroupType
import com.whip.app.domain.WorkoutSession
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.WorkoutSet
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.WorkoutSetRemovalReason
import com.whip.app.domain.WorkoutSetMutationBoundary
import com.whip.app.core.DEFAULT_REST_TIMER_PRESET_SECONDS
import com.whip.app.ui.ExerciseEditorDialog
import com.whip.app.ui.ExerciseActionsDialog
import com.whip.app.ui.MachineEditorDialog
import com.whip.app.ui.MachinePermanentDeleteDialog
import com.whip.app.ui.ExercisePermanentDeleteDialog
import com.whip.app.ui.RoutinePermanentDeleteDialog
import com.whip.app.ui.LocalWhipDialogPlacement
import com.whip.app.ui.WhipDialogPlacement
import com.whip.app.ui.QuickSetEntry
import com.whip.app.ui.QuickSetAuthorshipBoundary
import com.whip.app.ui.RestTimerCard
import com.whip.app.domain.resolveWorkoutRestDuration
import com.whip.app.ui.RoutineProgramPositionDialog
import com.whip.app.ui.TrackedRecordsManagerDialog
import com.whip.app.ui.WorkoutExerciseCard
import com.whip.app.ui.WorkoutExerciseNotesDialog
import com.whip.app.ui.WorkoutExerciseGroupSurface
import com.whip.app.ui.WorkoutExerciseUi
import com.whip.app.ui.WorkoutHistoryCard
import com.whip.app.ui.WorkoutHistoryContent
import com.whip.app.ui.WorkoutSetEditorDialog
import com.whip.app.ui.GymUiState
import com.whip.app.ui.GymProgressContent
import com.whip.app.ui.buildWorkoutExerciseBlocks
import com.whip.app.ui.customDisplayName
import com.whip.app.ui.reorderWorkoutBlock
import com.whip.app.ui.reorderWorkoutGroupMember
import com.whip.app.ui.routineDraftForEditing
import com.whip.app.ui.routineProgramStatusLabel
import com.whip.app.ui.workoutProgramSnapshotLabel
import com.whip.app.data.MachineDeletionImpact
import com.whip.app.data.ExerciseDeletionImpact
import com.whip.app.data.RoutineDeletionImpact
import com.whip.app.ui.theme.WhipTheme
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.time.LocalDate
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class GymPowerInputUiTest {
    @AndroidFontScale
    @Test
    fun setDetailsKeepsMalformedOptionalTextThroughRestoreAndDiscardReview() {
        val exercise = testExercise()
        val placement = testWorkoutExercise(exercise)
        val set = testWorkoutSet(700, placement.id).copy(completed = true, restSeconds = null, rpe = null)
        var saved: WorkoutSetDraft? = null
        var dismissed = false
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutSetEditorDialog(set = set, exercise = exercise, workoutExercise = placement, machine = null,
                    preferredWeightUnitId = "kilogram", preferredDistanceUnitId = "kilometre",
                    showRpe = true, showRir = false, showTempo = false, saving = false, errorMessage = null,
                    onDismiss = { dismissed = true }, onSave = { saved = it })
            }
        }
        val rest = compose.onNode(hasText("Rest seconds") and hasSetTextAction())
        rest.performScrollTo().performTextReplacement("2147483648")
        compose.onNodeWithText("Save", substring = false).assertIsNotEnabled()
        restoration.emulateSavedInstanceStateRestore()
        rest.performScrollTo().assertTextContains("2147483648")
        compose.onNodeWithContentDescription("Cancel set editing").performClick()
        compose.onNodeWithText("Keep Editing").performClick()
        compose.runOnIdle { assertFalse(dismissed); assertNull(saved) }
        rest.performScrollTo().performTextReplacement("0")
        val rpe = compose.onNode(hasText("RPE (1–10)") and hasSetTextAction())
        rpe.performScrollTo().performTextReplacement(".")
        compose.onNodeWithText("Save", substring = false).assertIsNotEnabled()
        compose.onNodeWithText("Enter a valid number").performScrollTo().assertIsDisplayed()
        compose.assertEditorHeaderVisibleWithKeyboard("Edit Set · ${exercise.name}", "Cancel set editing")
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val device = androidx.test.uiautomator.UiDevice.getInstance(instrumentation)
        val titleBounds = checkNotNull(device.findObject(androidx.test.uiautomator.By.text("Edit Set · ${exercise.name}"))).visibleBounds
        val statusBounds = checkNotNull(device.findObject(androidx.test.uiautomator.By.res("com.android.systemui", "status_bar"))).visibleBounds
        assertTrue("Set title must stay below the native status bar: $titleBounds versus $statusBounds", titleBounds.top >= statusBounds.bottom)
        val saveLabelBounds = checkNotNull(device.findObject(androidx.test.uiautomator.By.text("Save"))).visibleBounds
        val titleLayout = compose.onNodeWithText("Edit Set · ${exercise.name}").getUnclippedBoundsInRoot()
        val saveLayout = compose.onNodeWithText("Save", substring = false).getUnclippedBoundsInRoot()
        val saveLabelLayout = compose.onNodeWithText("Save", substring = false, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val density = instrumentation.targetContext.resources.displayMetrics.density
        // A disabled Compose Button may expose only its label to Android accessibility.
        // The complete, visible title supplies the editor root's actual screen offset.
        val rootScreenY = titleBounds.top - titleLayout.top.value * density
        val saveBottom = rootScreenY + saveLayout.bottom.value * density
        val imeBounds = android.graphics.Rect().also { bounds ->
            checkNotNull(instrumentation.uiAutomation.windows.singleOrNull {
                it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD
            }).getBoundsInScreen(bounds)
        }
        assertTrue("Complete Save must remain above the keyboard: bottom=$saveBottom versus $imeBounds",
            saveBottom <= imeBounds.top && saveLabelBounds.height() >= (saveLabelLayout.bottom - saveLabelLayout.top).value * density - 1f)
        captureVisualCatalogSurface("fresh.gym.set-invalid-optional")
        rpe.performTextReplacement("8.5")
        compose.onNodeWithText("Save", substring = false).performClick()
        compose.runOnIdle { assertEquals(0, saved?.restSeconds); assertEquals(8.5, saved?.rpe ?: -1.0, 0.0) }
    }

    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(AndroidFontScaleRule()).around(compose)

    @AndroidFontScale
    @Test
    fun workoutOverviewKeepsLongExerciseNamesAndFinishReachableAtLargeText() {
        val items = (1L..6L).map { id ->
            val exercise = testExercise().copy(id = id, name = "Station $id · Long dumbbell exercise name")
            val placement = testWorkoutExercise(exercise).copy(id = id)
            WorkoutExerciseUi(placement, exercise, List(4) { index ->
                testWorkoutSet(id * 10 + index, id).copy(position = index, completed = id == 1L)
            }, emptyList(), 0, null, null)
        }
        var chosen: Long? = null
        var finished = false
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                com.whip.app.ui.WorkoutOverviewDialog(items, emptySet(), 20L, false,
                    onChoose = { _, setId -> chosen = setId }, onFollowOrder = {}, onFinish = { finished = true }, onDismiss = {})
            }
        }
        compose.onNodeWithTag("workout-overview-finish").assertIsDisplayed()
        captureVisualCatalogSurface("deep.gym.session-overview.native200")
        compose.onNodeWithTag("workout-overview-list").performScrollToNode(hasTestTag("workout-overview-exercise-6"))
        compose.onNodeWithTag("workout-overview-exercise-6").performClick()
        compose.runOnIdle { assertEquals(60L, chosen) }
        compose.onNodeWithTag("workout-overview-finish").performClick()
        compose.runOnIdle { assertTrue(finished) }
    }

    @Test
    fun durationExerciseBasicsOmitLoadAndCancelKeepsAuthoredValuesUnsaved() {
        var dismissed = false
        var saved = false
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                ExerciseEditorDialog(exercise = testExercise().copy(trackingType = ExerciseTrackingType.DurationOnly),
                    categories = emptyList(), selectedCategoryIds = emptySet(), defaultWeightUnit = "kilogram",
                    defaultRestSeconds = 120, defaultFormula = EstimatedOneRepMaxFormula.Epley, platePresets = emptyList(),
                    onDismiss = { dismissed = true }, onSave = { saved = true })
            }
        }
        compose.onAllNodesWithText("What one weight entry means").assertCountEquals(0)
        compose.onNodeWithTag("exercise-editor-name").performTextReplacement("Timed hold")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithContentDescription("Cancel exercise editing").performClick()
        compose.onNodeWithText("Keep Editing").performClick()
        compose.onNodeWithTag("exercise-editor-name").assertTextContains("Timed hold")
        compose.onNodeWithContentDescription("Cancel exercise editing").performClick()
        compose.onNodeWithText("Discard Changes").performClick()
        compose.runOnIdle { assertTrue(dismissed); assertFalse(saved) }
    }

    @Test
    fun progressChoosesPerformedExerciseAndRefreshesExactSourceAfterDataChange() {
        val unused = testExercise().copy(id = 1, name = "Never performed")
        val performed = testExercise().copy(id = 2, name = "Recent squat", defaultGraphMetric = GymGraphMetric.MaxWeight.name)
        val session = testHistorySession().copy(localDate = LocalDate.now())
        val placement = testWorkoutExercise(performed).copy(sessionId = session.id,
            machineProfileUuidSnapshot = "recent-machine", machineNameSnapshot = "Recent rack",
            machineLoadTypeSnapshot = MachineLoadType.Mass, machineUnitIdSnapshot = "kilogram")
        val oldSession = session.copy(id = 49, uuid = "older-session", startedAt = session.startedAt.minusSeconds(86_400), localDate = session.localDate.minusDays(150))
        val oldPlacement = placement.copy(id = 99, sessionId = oldSession.id, machineProfileUuidSnapshot = "old-machine", machineNameSnapshot = "Old rack")
        val set = testWorkoutSet(700, placement.id).copy(completed = true, enteredWeight = 50.0, canonicalWeightKg = 50.0)
        val oldSet = set.copy(id = 701, workoutExerciseId = oldPlacement.id)
        var state by mutableStateOf(GymUiState(loading = false, exercises = listOf(unused, performed),
            history = listOf(oldSession, session), allSessions = listOf(oldSession, session), allWorkoutExercises = listOf(oldPlacement, placement), allSets = listOf(oldSet, set)))
        var opened by mutableStateOf<Long?>(null)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                if (opened == null) GymProgressContent(state, {}, {}, { opened = it }, {})
                else WorkoutHistoryContent(history = state.history, state = state, onCopy = {}, onResume = {},
                    onEditDetails = {}, onOpenActiveWorkout = {}, onSaveAsRoutine = { _, _ -> }, onCopyExercise = {},
                    onShare = {}, onRestore = {}, onDelete = {}, focusedWorkoutId = opened)
            }
        }
        val list = compose.onNodeWithTag("gym-progress-list")
        list.performScrollToNode(hasTestTag("gym-progress-exercise-selector"))
        compose.onNode(hasText("Recent squat") and hasAnyAncestor(hasTestTag("gym-progress-exercise-selector")), useUnmergedTree = true).assertIsDisplayed()
        list.performScrollToNode(hasText("Data Points"))
        compose.onNodeWithText("Data Points").performClick()
        list.performScrollToNode(hasText("50 kg"))
        compose.onNodeWithText("50 kg").assertIsDisplayed()
        compose.runOnIdle { state = state.copy(nowMillis = state.nowMillis + 1000) }
        compose.onNodeWithText("50 kg").assertIsDisplayed()
        compose.runOnIdle { state = state.copy(allSets = listOf(oldSet, set.copy(enteredWeight = 60.0, canonicalWeightKg = 60.0))) }
        compose.onNodeWithText("60 kg").assertIsDisplayed()
        captureVisualCatalogSurface("deep.gym.progress-performed-default")
        compose.onNodeWithText("60 kg").performClick()
        compose.onNodeWithTag("gym-chart-point-open-workout").performClick()
        compose.runOnIdle { assertEquals(session.id, opened) }
        compose.onNodeWithTag("history-set-performed-700", useUnmergedTree = true).performScrollTo().assertTextEquals("60 kg × 5 reps")
        captureVisualCatalogSurface("deep.gym.progress-exact-history")
    }

    @AndroidFontScale
    @Test
    fun previousWeightUsesConvertedEntryAndEffortPrecedesCompletion() {
        val exercise = testExercise().copy(loadInterpretation = LoadInterpretation.PerHand)
        val placement = testWorkoutExercise(exercise).copy(loadInterpretationSnapshot = LoadInterpretation.PerHand)
        val source = placement.copy(exerciseWeightUnitSnapshot = "pound")
        val set = testWorkoutSet(4, placement.id).copy(enteredWeight = null, canonicalWeightKg = null,
            enteredWeightUnitId = "kilogram", repetitions = null)
        val previous = set.copy(id = 3, completed = true, enteredWeight = 100.0, enteredWeightUnitId = "pound",
            canonicalWeightKg = 90.718474, repetitions = 8, rpe = 8.0)
        var saved: WorkoutSetDraft? = null
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    QuickSetEntry(set = set, exercise = exercise, workoutExercise = placement, machine = null,
                        preferredWeightUnitId = "kilogram", preferredDistanceUnitId = "kilometre", showRpe = true, showRir = false,
                        suggestedSet = previous, suggestedWorkoutExercise = source, onMoreDetails = {},
                        onSave = { _, draft, _ -> saved = draft })
                }
            }
        }
        compose.onNodeWithTag("quick-set-use-last-4").performClick()
        compose.onNodeWithTag("quick-set-load-4").performScrollTo().assertTextContains("45.359237")
        compose.onNode(hasText("RPE (1–10)") and hasSetTextAction()).performScrollTo().performTextReplacement("8")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        val effortTop = compose.onNodeWithText("RPE (1–10)").getUnclippedBoundsInRoot().top
        val completeTop = compose.onNodeWithTag("quick-set-save-next-4").getUnclippedBoundsInRoot().top
        assertTrue(effortTop < completeTop)
        compose.onNodeWithTag("quick-set-save-next-4").performScrollTo()
        captureVisualCatalogSurface("deep.gym.quick-entry-effort.native200")
        compose.onNodeWithTag("quick-set-save-next-4").performClick()
        compose.runOnIdle {
            assertEquals(45.359237, requireNotNull(saved?.weight), 0.000001)
            assertEquals("kilogram", saved?.weightUnitId)
            assertEquals(8.0, saved?.rpe)
        }
    }

    @Test
    fun quickDistanceKeepsSavedUnitWhenPreferenceChanges() {
        val exercise = testExercise().copy(trackingType = ExerciseTrackingType.DistanceDuration)
        val placement = testWorkoutExercise(exercise).copy(trackingTypeSnapshot = ExerciseTrackingType.DistanceDuration)
        val set = testWorkoutSet(4, placement.id).copy(enteredWeight = null, canonicalWeightKg = null,
            repetitions = null, enteredDistance = null, canonicalDistanceMetres = null, enteredDistanceUnitId = "mile", durationSeconds = 600)
        val previous = set.copy(id = 3, completed = true, enteredDistance = 1.609344,
            enteredDistanceUnitId = "kilometre", canonicalDistanceMetres = 1609.344)
        var preferred by mutableStateOf("kilometre")
        var saved: WorkoutSetDraft? = null
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    QuickSetEntry(set = set, exercise = exercise, workoutExercise = placement, machine = null,
                        preferredWeightUnitId = "kilogram", preferredDistanceUnitId = preferred, showRpe = false, showRir = false,
                        suggestedSet = previous, onMoreDetails = {}, onSave = { _, draft, _ -> saved = draft })
                }
            }
        }
        compose.onNodeWithTag("quick-set-use-last-4").performClick()
        compose.onNode(hasText("Distance (mi)") and hasSetTextAction()).assertTextContains("1")
        compose.runOnIdle { preferred = "distance_m" }
        compose.onNode(hasText("Distance (mi)") and hasSetTextAction()).assertTextContains("1")
        compose.onNodeWithTag("quick-set-save-next-4").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("mile", saved?.distanceUnitId); assertEquals(1.0, saved?.distance) }
    }

    @AndroidFontScale
    @Test
    fun exerciseBasicsPrecedeSearchableCategoriesAndRestoreSelection() {
        val categories = (1L..30L).map { id ->
            com.whip.app.domain.ExerciseCategory(id, "category-$id", "Category $id", "Custom", id.toInt(), false, 1, 1)
        }
        var saved: ExerciseDraft? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                ExerciseEditorDialog(
                    exercise = null, initialName = "Dumbbell press", categories = categories,
                    selectedCategoryIds = emptySet(), defaultWeightUnit = "pound", defaultRestSeconds = 120,
                    defaultFormula = EstimatedOneRepMaxFormula.Epley, platePresets = emptyList(),
                    onDismiss = {}, onSave = { saved = it },
                )
            }
        }
        val list = compose.onNodeWithTag("exercise-editor-list")
        list.performScrollToNode(hasText("What one weight entry means"))
        compose.onNodeWithContentDescription("What one weight entry means: Total Load").performScrollTo().performClick()
        compose.onNodeWithContentDescription("What one weight entry means option: Per Hand").performClick()
        compose.onNodeWithText("Keep Entered Numbers").performClick()
        captureVisualCatalogSurface("deep.gym.exercise-basics.native200")
        list.performScrollToNode(hasTestTag("exercise-category-picker-open"))
        compose.onNodeWithTag("exercise-category-picker-open").performClick()
        compose.onNodeWithTag("exercise-category-search").performTextReplacement("Category 29")
        compose.onNodeWithTag("exercise-category-choice-29").performClick()
        compose.onNodeWithTag("exercise-category-search").performTextReplacement("Category 3")
        compose.onNodeWithTag("exercise-category-choice-3").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("exercise-category-search").assertTextContains("Category 3")
        compose.onNodeWithText("2 selected · Browsing and analytics labels; routine roles are chosen separately.").assertIsDisplayed()
        captureVisualCatalogSurface("deep.gym.exercise-categories.native200")
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle {
            assertEquals("pound", saved?.weightUnitId)
            assertEquals(LoadInterpretation.PerHand, saved?.loadInterpretation)
            assertEquals(setOf(3L, 29L), saved?.categoryIds)
        }
    }

    @AndroidFontScale
    @Test
    fun workoutLaunchpadKeepsExactChoicesAtLargeText() {
        val planned = GymRoutine(71, "planned", "Phased strength", "", 0, false, true, 1, 1,
            programKind = RoutineProgramKind.Custom, programPhaseCount = 2, programPhaseLabels = listOf("Build", "Recovery"), nextProgramDayPosition = 1)
        val static = planned.copy(id = 72, uuid = "static", name = "Choose your training", pinned = false, programKind = RoutineProgramKind.Static)
        val days = listOf(RoutineDay(81, "upper", planned.id, "Upper", 0, 1, 1),
            RoutineDay(82, "lower", planned.id, "Lower", 1, 1, 1),
            RoutineDay(83, "static-upper", static.id, "Upper", 0, 1, 1),
            RoutineDay(84, "static-lower", static.id, "Lower", 1, 1, 1))
        var state by mutableStateOf(GymUiState(loading = false, exercises = listOf(testExercise()),
            routines = listOf(static, planned), routineDays = days, history = listOf(testHistorySession())))
        var started: Pair<Long, Long?>? = null
        var openedRoutine: Long? = null
        var openedHistory: Long? = null
        var browsed = false
        var emptyStarted = false
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                com.whip.app.ui.WorkoutStartContent(state, { emptyStarted = true }, { browsed = true }, {},
                    { routine, day -> started = routine to day }, { openedRoutine = it }, { openedHistory = it })
            }
        }
        val list = compose.onNodeWithTag("workout-start-list")
        list.performScrollToNode(hasTestTag("workout-quick-start-${planned.id}"))
        compose.onNodeWithTag("workout-quick-start-${planned.id}").assertTextContains("Start Next · Lower").performClick()
        assertEquals(planned.id to null, started)
        captureVisualCatalogSurface("experience.gym.launchpad.native200")
        list.performScrollToNode(hasTestTag("workout-quick-start-${static.id}"))
        compose.onNodeWithTag("workout-quick-start-${static.id}").assertTextContains("Choose Training Day").performClick()
        assertEquals(static.id, openedRoutine)
        list.performScrollToNode(hasText("Browse All Routines"))
        compose.onNodeWithText("Browse All Routines").performClick()
        assertTrue(browsed)
        list.performScrollToNode(hasTestTag("workout-latest-history"))
        compose.onNodeWithTag("workout-latest-history").performClick()
        assertEquals(testHistorySession().id, openedHistory)
        restoration.emulateSavedInstanceStateRestore()
        list.performScrollToNode(hasText("Start Empty Workout"))
        compose.onNodeWithText("Start Empty Workout").performClick()
        assertTrue(emptyStarted)
        compose.runOnIdle {
            state = state.copy(routineExercises = listOf(RoutineExercise(91, "placement", 82, testExercise().id,
                0, "", null, false, 1, 1, equipmentBindingState = com.whip.app.domain.RoutineEquipmentBindingState.NeedsEquipment)))
        }
        list.performScrollToNode(hasTestTag("workout-quick-start-${planned.id}"))
        compose.onNodeWithTag("workout-quick-start-${planned.id}").assertTextContains("Resolve Equipment").performClick()
        assertEquals(planned.id, openedRoutine)
        compose.runOnIdle { state = GymUiState(loading = false) }
        list.performScrollToNode(hasText("Create First Exercise"))
        compose.onNodeWithText("Create First Exercise").assertIsDisplayed()
        captureVisualCatalogSurface("experience.gym.first-use.native200")
    }

    @Test
    fun trackedMachineRecordDraftSurvivesRecreationAndOpensItsSource() {
        val exercise = testExercise()
        val session = testHistorySession()
        val record = com.whip.app.domain.PersonalRecord(uuid = "level-record", exerciseId = exercise.id,
            type = com.whip.app.domain.PersonalRecordType.MaxMachineSetting, value = 3.0, secondaryValue = null,
            unitId = "level", sourceSetId = 9, sourceSessionId = session.id, achievedAtMillis = 1,
            current = true, imported = false, createdAtMillis = 1, updatedAtMillis = 1,
            machineProfileUuidSnapshot = "removed-level-profile")
        var saved by mutableStateOf(emptyList<com.whip.app.core.TrackedGymRecord>())
        var editorOpen by mutableStateOf(true)
        var firstOpen by mutableStateOf(true)
        var openedSource: Long? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            val state = GymUiState(loading = false, exercises = listOf(exercise), history = listOf(session),
                personalRecords = listOf(record), appSettings = com.whip.app.core.AppSettings(trackedGymRecords = saved))
            WhipTheme(dynamicColor = false) {
                if (editorOpen) {
                    TrackedRecordsManagerDialog(Modifier, state, exercise.id.takeIf { firstOpen },
                        onDismiss = { editorOpen = false }, onSave = { saved = it; firstOpen = false; editorOpen = false })
                } else {
                    GymProgressContent(state, {}, {}, { openedSource = it }, { editorOpen = true })
                }
            }
        }
        compose.onNodeWithContentDescription("Track Heaviest Weight").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Stop tracking Best Machine Setting").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Track Best Machine Setting").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Stop tracking Heaviest Weight").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertTrue(saved.isEmpty()) }
        captureVisualCatalogSurface("product-audit.gym.tracked-record-draft")
        compose.onNodeWithContentDescription("Close tracked records editor").performClick()
        compose.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()
        compose.onNodeWithText("Keep Editing").performClick()
        compose.onNodeWithTag("tracked-records-save").performClick()
        compose.runOnIdle {
            assertEquals(listOf(com.whip.app.domain.PersonalRecordType.MaxWeight,
                com.whip.app.domain.PersonalRecordType.MaxMachineSetting), saved.map { it.type })
        }
        compose.onNodeWithTag("gym-progress-list").performScrollToNode(hasText("Best Machine Setting"))
        compose.onNodeWithText("Best Machine Setting").performClick()
        compose.runOnIdle { assertEquals(session.id, openedSource) }
        captureVisualCatalogSurface("product-audit.gym.machine-record")
        compose.onNodeWithTag("gym-progress-list").performScrollToNode(hasTestTag("gym-manage-tracked-records"))
        compose.onNodeWithTag("gym-manage-tracked-records").performClick()
        compose.onNodeWithContentDescription("Stop tracking Best Machine Setting").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("tracked-records-save").performClick()
        compose.runOnIdle { assertEquals(2, saved.size) }
    }

    @Test
    fun archivedExerciseKeepsWeeklyVolumeAndHistoricalEnteredUnits() {
        val exercise = testExercise().copy(name = "Archived dumbbell row", archived = true, weightUnitId = "pound",
            defaultGraphMetric = GymGraphMetric.MaxWeight.name)
        val session = testHistorySession().copy(localDate = LocalDate.now())
        val placement = testWorkoutExercise(exercise).copy(sessionId = session.id, loadInterpretationSnapshot = LoadInterpretation.PerHand)
        val set = testWorkoutSet(501, placement.id).copy(completed = true, canonicalWeightKg = 40.0, enteredWeight = 20.0)
        val category = com.whip.app.domain.ExerciseCategory(3, "category", "Back", "Muscle", 0, true, 1, 1)
        val state = GymUiState(loading = false, archivedExercises = listOf(exercise), history = listOf(session),
            allSessions = listOf(session), allWorkoutExercises = listOf(placement), allSets = listOf(set),
            archivedCategories = listOf(category), categoryLinks = listOf(com.whip.app.domain.ExerciseCategoryLink(exercise.id, category.id)),
            appSettings = com.whip.app.core.AppSettings(numberPrecision = 2))
        var openedSource by mutableStateOf<Long?>(null)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                if (openedSource == null) {
                    GymProgressContent(state, {}, {}, { openedSource = it }, {})
                } else {
                    WorkoutHistoryContent(history = state.history, state = state, onCopy = {}, onResume = {},
                        onEditDetails = {}, onOpenActiveWorkout = {}, onSaveAsRoutine = { _, _ -> }, onCopyExercise = {},
                        onShare = {}, onRestore = {}, onDelete = {}, focusedWorkoutId = openedSource)
                }
            }
        }
        compose.onAllNodesWithText("No Exercises to Track").assertCountEquals(0)
        val progress = compose.onNodeWithTag("gym-progress-list")
        progress.performScrollToNode(hasText("Weekly Details"))
        compose.onNodeWithText("Weekly Details").performClick()
        progress.performScrollToNode(hasText("5 reps · 200 kg·rep", substring = true))
        compose.onNodeWithText("5 reps · 200 kg·rep", substring = true).assertIsDisplayed()
        progress.performScrollToNode(hasText("Back · Archived: 1 allocated hard set · 200 kg·rep"))
        compose.onNodeWithText("Back · Archived: 1 allocated hard set · 200 kg·rep").assertIsDisplayed()
        captureVisualCatalogSurface("product-audit.gym.archived-progress")
        progress.performScrollToNode(hasText("Data Points"))
        compose.onNodeWithText("Data Points").performClick()
        progress.performScrollToNode(hasText("88.18 lb"))
        compose.onNodeWithText("88.18 lb").performClick()
        compose.onNodeWithTag("gym-chart-point-open-workout").performClick()
        compose.runOnIdle { assertEquals(session.id, openedSource) }
        compose.onNodeWithTag("history-set-performed-${set.id}", useUnmergedTree = true)
            .performScrollTo().assertTextContains("44.09 lb per hand (88.18 total)", substring = true)
        captureVisualCatalogSurface("product-audit.gym.archived-history-units")
        compose.runOnIdle { assertEquals(20.0, set.enteredWeight!!, 0.0) }
    }

    @Test
    fun archivedHistoryFiltersKeepOlderTrainingMaxChangesReachable() {
        val exercise = testExercise().copy(archived = true)
        val routine = GymRoutine(4, "archived-routine", "Old routine", "", 0, true, false, 1, 1)
        val category = com.whip.app.domain.ExerciseCategory(3, "category", "Back", "Muscle", 0, true, 1, 1)
        val decisions = (1..21).map { index ->
            com.whip.app.domain.TrainingMaxDecision("decision-$index", routine.uuid, "manual-$index", exercise.uuid,
                exercise.name, index, 100.0, 2.5, 102.5, "kilogram", 2.5, "Manual", 2.5, 1.0,
                listOf("Manual decision $index"), "manual", com.whip.app.domain.TrainingMaxDecisionAction.Custom,
                index * 86_400_000L)
        }
        val state = GymUiState(loading = false, archivedExercises = listOf(exercise), archivedRoutines = listOf(routine),
            archivedCategories = listOf(category), categoryLinks = listOf(com.whip.app.domain.ExerciseCategoryLink(exercise.id, category.id)),
            trainingMaxDecisions = decisions)
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutHistoryContent(history = emptyList(), state = state, onCopy = {}, onResume = {},
                    onEditDetails = {}, onOpenActiveWorkout = {}, onSaveAsRoutine = { _, _ -> }, onCopyExercise = {},
                    onShare = {}, onRestore = {}, onDelete = {})
            }
        }
        compose.onNodeWithText("History Options").performClick()
        compose.onNodeWithText("All Exercises").performScrollTo().performClick()
        compose.onNodeWithText("${exercise.name} · Archived").performClick()
        compose.onNodeWithText("All Routines").performScrollTo().performClick()
        compose.onNodeWithText("Old routine · Archived").performClick()
        compose.onNodeWithText("All Categories").performScrollTo().performClick()
        compose.onNodeWithText("Back · Archived").performClick()
        compose.onNodeWithText("Program Training Max Changes").performScrollTo().performClick()
        compose.onNodeWithText("Show All 21 Changes").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Manual decision 1", substring = false).performScrollTo().assertIsDisplayed()
        captureVisualCatalogSurface("product-audit.gym.training-max-history")
    }

    @Test
    fun restAlertExplainsBlockedCurrentChannel() {
        val app: WhipApplication = androidx.test.core.app.ApplicationProvider.getApplicationContext()
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val device = androidx.test.uiautomator.UiDevice.getInstance(instrumentation)
        val manager = app.getSystemService(android.app.NotificationManager::class.java)
        val permission = android.Manifest.permission.POST_NOTIFICATIONS
        val runtimePermission = android.os.Build.VERSION.SDK_INT >= 33
        val permissionWasGranted = !runtimePermission || app.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        com.whip.app.reminders.RestTimerNotifications.createChannel(app)
        val channelId = com.whip.app.reminders.RestTimerNotifications.channelId(false, false)
        val channelWasEnabled = manager.getNotificationChannel(channelId).importance != android.app.NotificationManager.IMPORTANCE_NONE
        fun setChannelEnabled(enabled: Boolean) {
            app.startActivity(android.content.Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, app.packageName)
                .putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, channelId)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            assertTrue(device.wait(androidx.test.uiautomator.Until.hasObject(androidx.test.uiautomator.By.pkg("com.android.settings")), 5_000))
            val switch = device.wait(androidx.test.uiautomator.Until.findObject(androidx.test.uiautomator.By.clazz("android.widget.Switch")), 5_000)
                ?: error("Android channel settings did not expose its notification switch")
            if (switch.isChecked != enabled) switch.click()
            compose.waitUntil(5_000) { (manager.getNotificationChannel(channelId).importance != android.app.NotificationManager.IMPORTANCE_NONE) == enabled }
            device.pressBack()
            assertTrue(device.wait(androidx.test.uiautomator.Until.hasObject(androidx.test.uiautomator.By.pkg(app.packageName)), 5_000))
            compose.waitForIdle()
        }
        var sound by mutableStateOf(false)
        try {
            if (runtimePermission && !permissionWasGranted) instrumentation.uiAutomation.grantRuntimePermission(app.packageName, permission)
            compose.setContent {
                WhipTheme(dynamicColor = false) {
                    RestTimerCard(session = testHistorySession(), remaining = 45,
                        duration = resolveWorkoutRestDuration(null, null, null, 120), presetSeconds = DEFAULT_REST_TIMER_PRESET_SECONDS,
                        notificationPermissionRequested = true, timerSound = sound, timerVibration = false,
                        onSelectedSecondsChange = {}, onPresetSecondsChange = {}, onStart = { _, _ -> }, onAdjust = { _, _ -> }, onStop = {})
                }
            }
            setChannelEnabled(true)
            compose.onAllNodesWithText("Background alert off", substring = true).assertCountEquals(0)
            setChannelEnabled(false)
            compose.onNodeWithText("Background alert off", substring = true).assertIsDisplayed()
            compose.onNodeWithContentDescription("Stop rest timer").assertIsEnabled()
            captureVisualCatalogSurface("audit2.gym.rest-blocked-channel")
            val otherChannel = manager.getNotificationChannel(com.whip.app.reminders.RestTimerNotifications.channelId(true, false))
            if (otherChannel.importance != android.app.NotificationManager.IMPORTANCE_NONE) {
                compose.runOnIdle { sound = true }
                compose.onAllNodesWithText("Background alert off", substring = true).assertCountEquals(0)
                compose.runOnIdle { sound = false }
                compose.onNodeWithText("Background alert off", substring = true).assertIsDisplayed()
            }
        } finally {
            setChannelEnabled(channelWasEnabled)
        }
    }

    @Test
    fun largeMachineLibraryKeepsProfileLookupAndArchiveScopeClear() {
        val first = testExercise().copy(name = "Seated row")
        val second = first.copy(id = 900, uuid = "secondary", name = "Cable fly")
        val profiles = List(30) { index -> com.whip.app.domain.GymMachine(
            id = index.toLong() + 1, uuid = "machine-$index", exerciseId = first.id,
            name = "Cable station ${index + 1}", location = if (index == 29) "North room" else "South room",
            details = "", loadType = com.whip.app.domain.MachineLoadType.Mass, unitId = "kilogram", levelLabel = "level",
            availableLoads = listOf(5.0, 10.0), loadInterpretation = LoadInterpretation.MachineDisplayedMass,
            baseLoadKg = null, archived = false, createdAtMillis = 1, updatedAtMillis = 1,
            configurationVersion = if (index == 29) 3 else 1,
            exerciseIds = if (index == 29) setOf(first.id, second.id) else setOf(first.id),
        ) }
        val archived = profiles.last().copy(id = 31, uuid = "archived-machine", archived = true, configurationVersion = 2)
        var edited: Long? = null
        var searching by mutableStateOf(false)
        val libraryState = GymUiState(loading = false, exercises = listOf(first, second), machines = profiles, archivedMachines = listOf(archived))
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                com.whip.app.ui.MachineLibraryContent(
                    state = libraryState,
                    onCreate = {}, onEdit = { edited = it.id }, onArchive = { _, _ -> }, onNewVersion = {}, onDelete = {},
                )
                if (searching) com.whip.app.ui.UnifiedSearchDialog(
                    taskState = com.whip.app.ui.TaskUiState(loading = false),
                    habitState = com.whip.app.ui.HabitUiState(loading = false),
                    goalState = com.whip.app.ui.GoalUiState(loading = false),
                    gymState = libraryState,
                    initialScope = com.whip.app.ui.WhipSearchEntryContext.Machines.defaultSearchScope(),
                    onDismiss = { searching = false },
                    onSelect = { edited = it.id; searching = false },
                )
            }
        }
        captureVisualCatalogSurface("audit2.gym.machines.collection-30")
        val list = compose.onNodeWithTag("gym-machine-list")
        assertTrue("The distant profile starts outside the visible collection",
            compose.onAllNodesWithText("Cable station 30 · North room").fetchSemanticsNodes().isEmpty() ||
                !compose.onNodeWithText("Cable station 30 · North room").isDisplayed())
        var lookupSwipes = 0
        while ((compose.onAllNodesWithText("Cable station 30 · North room").fetchSemanticsNodes().isEmpty() ||
                !compose.onNodeWithText("Cable station 30 · North room").isDisplayed()) && lookupSwipes < 30) {
            list.performTouchInput { swipeUp() }
            compose.waitForIdle()
            lookupSwipes++
        }
        assertTrue("Thirty-profile lookup should measure actual navigation before querying", lookupSwipes > 0)
        compose.onNodeWithText("Cable station 30 · North room").assertIsDisplayed()
        captureVisualCatalogSurface("audit2.gym.machines.distant-target-after-$lookupSwipes-swipes")
        compose.onAllNodesWithTag("machine-library-search").assertCountEquals(0)
        compose.runOnIdle { searching = true }
        compose.onNodeWithTag("unified-search-query").performTextReplacement("north v3 cable fly")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("unified-search-result-Machine-30").fetchSemanticsNodes().isNotEmpty() }
        captureVisualCatalogSurface("search-consistency.gym.machines.lookup")
        compose.onNodeWithTag("unified-search-result-Machine-30").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(30L, edited) }
        list.performScrollToNode(hasText("Show archived", ignoreCase = true))
        compose.onNodeWithText("Show archived", ignoreCase = true).performClick()
        compose.runOnIdle { searching = true }
        compose.onNodeWithTag("unified-search-query").performTextReplacement("north v2 cable fly status:archived")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("unified-search-query").assertTextContains("north v2 cable fly status:archived")
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("unified-search-result-Machine-31").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("unified-search-result-Machine-31").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(31L, edited) }
        list.performScrollToNode(hasText("Show archived", ignoreCase = true))
        compose.onNodeWithText("Show archived", ignoreCase = true).assertIsOn()
        compose.onNodeWithContentDescription("Edit machine Cable station 30 · North room").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun exerciseInspectorDefaultsFollowMeasurementCapabilities() {
        var exercise by mutableStateOf(testExercise().copy(trackingType = ExerciseTrackingType.BodyweightReps))
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                ExerciseActionsDialog(exercise = exercise, trackedInProgress = false, onDismiss = {}, onEdit = {},
                    onFavorite = {}, onDuplicate = {}, onConfigureTrackedRecords = {}, onArchive = {}, onDelete = {})
            }
        }
        compose.onNodeWithText("Weight increment").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Repetition increment").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { exercise = exercise.copy(trackingType = ExerciseTrackingType.AssistedBodyweightReps) }
        compose.onNodeWithText("Weight increment").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { exercise = exercise.copy(trackingType = ExerciseTrackingType.DurationOnly) }
        compose.onAllNodesWithText("Weight increment").assertCountEquals(0)
        compose.onAllNodesWithText("Repetition increment").assertCountEquals(0)
        compose.onAllNodesWithText("Load meaning").assertCountEquals(0)
    }

    @Test
    fun chartPointsRetainExactWorkoutAndSeriesIdentity() {
        val exercise = testExercise().copy(name = "Same named exercise", defaultGraphMetric = GymGraphMetric.MaxWeight.name)
        val comparison = exercise.copy(id = 900, uuid = "comparison")
        val first = testHistorySession()
        val second = first.copy(id = 51, uuid = "same-day-second", startedAt = first.startedAt.plusSeconds(3600))
        val third = first.copy(id = 52, uuid = "comparison-session")
        val placements = listOf(first, second, third).mapIndexed { index, session ->
            testWorkoutExercise(if (index == 2) comparison else exercise).copy(id = 100L + index, uuid = "placement-$index", sessionId = session.id)
        }
        var opened: Long? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                GymProgressContent(state = GymUiState(loading = false, exercises = listOf(exercise, comparison),
                    history = listOf(first, second, third), allWorkoutExercises = placements,
                    allSets = placements.mapIndexed { index, placement -> testWorkoutSet(200L + index, placement.id)
                        .copy(completed = true, canonicalWeightKg = 40.0 + 10 * index, enteredWeight = 40.0 + 10 * index) }),
                    onOpenExercises = {}, onOpenWorkout = {}, onOpenWorkoutHistory = { opened = it }, onManageTrackedRecords = {})
            }
        }
        val list = compose.onNodeWithTag("gym-progress-list")
        list.performScrollToNode(hasText("Graph Options"))
        compose.onNodeWithText("Graph Options").performClick()
        list.performScrollToNode(hasText("Add up to 3 Comparisons"))
        compose.onNodeWithText("Add up to 3 Comparisons").performClick()
        compose.onNode(hasText(exercise.name) and hasAnyAncestor(hasTestTag("gym-exercise-comparison-menu"))).performClick()
        androidx.test.espresso.Espresso.pressBack()
        list.performScrollToNode(hasText("Graph Options"))
        compose.onNodeWithText("Graph Options").performClick()
        listOf(first.id, second.id, third.id).forEachIndexed { index, sessionId ->
            val value = 40 + index * 10
            list.performScrollToNode(hasTestTag("gym-chart-point"))
            val point = compose.onNode(hasTestTag("gym-chart-point") and hasText("$value", substring = true))
            point.performScrollTo()
            repeat(3) { if (!point.isDisplayed()) list.performTouchInput { swipeUp() } }
            point.assertIsDisplayed().performTouchInput { click() }
            if (index == 0) restoration.emulateSavedInstanceStateRestore()
            compose.onNodeWithText("Built from 1 source.").assertIsDisplayed()
            captureVisualCatalogSurface("audit2.gym.chart-source-$index")
            compose.onNodeWithTag("gym-chart-point-open-workout").performClick()
            compose.runOnIdle { assertEquals(sessionId, opened) }
        }
        list.performScrollToNode(hasText("Graph Options"))
        compose.onNodeWithText("Graph Options").performClick()
        list.performScrollToNode(hasText("Each Workout"))
        compose.onNodeWithText("Each Workout", substring = false).performClick()
        compose.onNodeWithText("Weekly", substring = false).performClick()
        list.performScrollToNode(hasText("Graph Options"))
        compose.onNodeWithText("Graph Options").performClick()
        list.performScrollToNode(hasTestTag("gym-chart-point"))
        val aggregate = compose.onNode(hasTestTag("gym-chart-point") and hasText("50", substring = true))
        aggregate.performScrollTo()
        repeat(3) { if (!aggregate.isDisplayed()) list.performTouchInput { swipeUp() } }
        aggregate.assertIsDisplayed().performTouchInput { click() }
        compose.onNodeWithText("Built from 2 sources.").assertIsDisplayed()
        compose.onAllNodesWithTag("gym-chart-point-open-workout").assertCountEquals(0)
    }

    @AndroidFontScale
    @Test
    fun populatedHistoryCalendarKeepsDatesAndCountsReadableAtNativeLargeText() {
        val date = LocalDate.of(2026, 9, 28)
        var selected by mutableStateOf<LocalDate?>(null)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Surface(Modifier.width(320.dp)) {
                    com.whip.app.ui.WorkoutMonthCalendar(
                        month = java.time.YearMonth.from(date),
                        sessions = List(12) { testHistorySession().copy(id = it.toLong(), localDate = date) },
                        selectedDate = selected, onSelectDate = { selected = it },
                        onPreviousMonth = {}, onNextMonth = {}, firstDayOfWeek = java.time.DayOfWeek.MONDAY,
                    )
                }
            }
        }
        captureVisualCatalogSurface("audit2.gym.calendar.native200")
        listOf("28", "12").forEach { label ->
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            compose.onAllNodesWithText(label, useUnmergedTree = true).fetchSemanticsNodes().forEach { node ->
                val key = androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult
                if (node.config.contains(key)) node.config[key].action?.invoke(layouts)
            }
            assertTrue("Expected measured calendar text for $label", layouts.isNotEmpty())
            layouts.forEach { result ->
                assertEquals("Calendar $label must stay on one line", 1, result.lineCount)
                assertFalse("Calendar $label must not clip", result.hasVisualOverflow)
            }
        }
        compose.onNodeWithContentDescription("Monday, September 28, 2026, 12 workouts").performClick()
        compose.runOnIdle { assertEquals(date, selected) }
    }

    @AndroidFontScale
    @Test
    fun compactActiveLaneLeavesRoomForSetEntryAtNativeLargeText() {
        org.junit.Assume.assumeTrue(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
        val exercise = testExercise().copy(name = "Chest supported alternating single arm cable row with controlled pause")
        val session = testHistorySession().copy(endedAt = null, state = WorkoutSessionState.Active)
        val placement = testWorkoutExercise(exercise).copy(sessionId = session.id)
        val set = testWorkoutSet(1, placement.id)
        val item = WorkoutExerciseUi(placement, exercise, listOf(set), emptyList(), 0, null, null)
        val app: WhipApplication = androidx.test.core.app.ApplicationProvider.getApplicationContext()
        val viewModel = com.whip.app.ui.GymViewModel(app)
        val permission = android.Manifest.permission.POST_NOTIFICATIONS
        val originallyGranted = app.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val automation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
        try {
        automation.revokeRuntimePermission(app.packageName, permission)
        check(app.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_DENIED)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                androidx.compose.foundation.layout.Box(Modifier.width(320.dp).height(640.dp)) {
                    com.whip.app.ui.GymAreaContent(
                        state = GymUiState(loading = false, exercises = listOf(exercise), activeSession = session,
                            activeWorkoutExercises = listOf(item), activeWorkoutPerformanceExercises = listOf(item),
                            allWorkoutExercises = listOf(placement), allSets = listOf(set), allSessions = listOf(session),
                            restSecondsRemaining = 45, appSettings = com.whip.app.core.AppSettings(notificationPermissionRequested = true)),
                        innerPadding = androidx.compose.foundation.layout.PaddingValues(), viewModel = viewModel,
                    )
                }
            }
        }
        compose.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("workout-rest-alert-explanation"))
        compose.onNodeWithText("Background alert off · the in-app timer still works").assertIsDisplayed()
        compose.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("workout-next-set-details"))
        compose.onNodeWithTag("workout-next-set-details").assertTextContains(exercise.name, substring = true)
        compose.onNodeWithTag("next-set-focus").performClick()
        captureVisualCatalogSurface("ux-upgrades.gym.active-lane.native200")
        compose.onNodeWithText("Background alerts off").assertIsDisplayed()
        val viewport = compose.onNodeWithTag("active-workout-list").fetchSemanticsNode().boundsInRoot
        val rest = compose.onNodeWithTag("rest-timer-card").fetchSemanticsNode().boundsInRoot
        assertTrue("Sticky execution lane must leave at least 144dp for Set entry: viewport=$viewport rest=$rest",
            viewport.bottom - rest.bottom >= 144f * compose.density.density)
        compose.onNodeWithTag("active-set-composer").assertIsDisplayed()
        compose.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("quick-set-save-next-${set.id}"))
        compose.onNodeWithTag("quick-set-save-next-${set.id}").assertIsDisplayed()
        } finally {
            if (originallyGranted) automation.grantRuntimePermission(app.packageName, permission)
        }
    }

    @Test
    fun repSpecificProgressRequiresVisibleValidRepetitions() {
        val exercise = testExercise().copy(defaultGraphMetric = GymGraphMetric.MaxWeightForReps.name)
        val session = testHistorySession()
        val placement = testWorkoutExercise(exercise).copy(sessionId = session.id)
        val set = testWorkoutSet(1, placement.id).copy(completed = true, repetitions = 5)
        var progressState by mutableStateOf(GymUiState(
            loading = false, exercises = listOf(exercise), history = listOf(session),
            allWorkoutExercises = listOf(placement), allSets = listOf(set), nowMillis = session.startedAt.toEpochMilli(),
        ))
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                androidx.compose.runtime.key(progressState.exercises.single().id) {
                    GymProgressContent(
                        state = progressState,
                        onOpenExercises = {}, onOpenWorkout = {}, onOpenWorkoutHistory = {}, onManageTrackedRecords = {},
                    )
                }
            }
        }
        compose.onNodeWithTag("gym-progress-list").performScrollToNode(hasTestTag("gym-progress-rep-target"))
        compose.onNodeWithTag("gym-progress-rep-target").performTextReplacement("0")
        compose.onNodeWithTag("gym-progress-list").performScrollToNode(hasText("Check Trend Inputs"))
        compose.onNodeWithText("Check Trend Inputs").assertIsDisplayed()
        captureVisualCatalogSurface("ux-upgrades.gym.progress-invalid-reps")
        compose.onNodeWithTag("gym-progress-list").performScrollToNode(hasTestTag("gym-progress-rep-target"))
        compose.onNodeWithTag("gym-progress-rep-target").performTextReplacement("5")
        compose.onNodeWithTag("gym-progress-list").performScrollToNode(hasTestTag("gym-chart-summary"))
        compose.onNodeWithTag("gym-chart-summary").assertTextContains("1 recorded point", substring = true)
        captureVisualCatalogSurface("ux-upgrades.gym.progress-context")

        // No live profile remains: both graph aggregation and displayed Best must use the saved direction.
        val reverseExercise = exercise.copy(id = 901, name = "Historical reverse machine", defaultGraphMetric = GymGraphMetric.MaxMachineSetting.name)
        val laterSession = session.copy(id = 51, uuid = "later-history", localDate = session.localDate.plusDays(1),
            startedAt = session.startedAt.plusSeconds(86_400), endedAt = session.endedAt?.plusSeconds(86_400))
        val reversePlacement = placement.copy(exerciseId = reverseExercise.id,
            machineProfileUuidSnapshot = "deleted-reverse-machine", machineNameSnapshot = "Deleted reverse machine",
            machineLoadTypeSnapshot = com.whip.app.domain.MachineLoadType.Level, machineLevelLabelSnapshot = "level",
            machineLevelDirectionSnapshot = MachineLevelDirection.HigherNumberLessResistance)
        val laterPlacement = reversePlacement.copy(id = 902, uuid = "later-placement", sessionId = laterSession.id)
        compose.runOnIdle {
            progressState = progressState.copy(exercises = listOf(reverseExercise), history = listOf(session, laterSession),
                allWorkoutExercises = listOf(reversePlacement, laterPlacement),
                allSets = listOf(set.copy(machineLoadValue = 8.0),
                    set.copy(id = 903, uuid = "later-set", workoutExerciseId = laterPlacement.id, machineLoadValue = 3.0)),
                appSettings = progressState.appSettings.copy(numberPrecision = 0))
        }
        compose.onNodeWithTag("gym-progress-list").performScrollToNode(hasText("3 level · best", substring = true))
        compose.onNodeWithText("3 level · best", substring = true).assertIsDisplayed()
        captureVisualCatalogSurface("ux-upgrades.gym.progress-deleted-reverse-machine")
    }

    @Test
    fun captureWorkoutComponentCatalog() {
        val exercise = testExercise().copy(name = "Cable Row", notes = "Keep the torso stable")
        val workoutExercise = testWorkoutExercise(exercise).copy(notes = "Seat position 4")
        val workoutSet = testWorkoutSet(4, workoutExercise.id).copy(note = "Controlled eccentric", rpe = 8.0)
        val routine = GymRoutine(
            id = 1,
            uuid = "routine",
            name = "Phased Strength Plan",
            notes = "Four-phase program",
            position = 0,
            archived = false,
            pinned = true,
            createdAtMillis = 1,
            updatedAtMillis = 2,
            programKind = RoutineProgramKind.Custom,
            programPhaseCount = 4,
            programPhaseLabels = listOf("Volume", "Build", "Peak", "Recovery"),
            currentProgramPhaseIndex = 2,
            currentProgramCycle = 3,
            nextProgramDayPosition = 1,
            programPhaseRoles = listOf(
                RoutineProgramPhaseRole.Standard,
                RoutineProgramPhaseRole.Standard,
                RoutineProgramPhaseRole.Standard,
                RoutineProgramPhaseRole.Deload,
            ),
            trainingMaxAdvanceAfterPhaseIndices = setOf(3),
            progressionMode = RoutineProgressionMode.PerformanceInformed,
            allowNonStandardHigherSuggestions = true,
        )
        val days = listOf(
            RoutineDay(2, "day-a", routine.id, "Upper", 0, 1, 2, progressionIndex = 4),
            RoutineDay(3, "day-b", routine.id, "Lower", 1, 1, 2, progressionIndex = 4),
        )
        var surface by mutableStateOf(0)
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    androidx.compose.runtime.key(surface) {
                        when (surface) {
                            0 -> WorkoutSetEditorDialog(
                                set = workoutSet,
                                exercise = exercise,
                                workoutExercise = workoutExercise,
                                machine = null,
                                preferredWeightUnitId = "kilogram",
                                preferredDistanceUnitId = "kilometre",
                                showRpe = true,
                                showRir = false,
                                showTempo = true,
                                saving = false,
                                errorMessage = null,
                                onDismiss = {},
                                onSave = {},
                            )
                            1 -> WorkoutExerciseNotesDialog(
                                exerciseName = exercise.name,
                                initialNotes = workoutExercise.notes,
                                machines = emptyList(),
                                selectedMachineId = null,
                                machineLocked = false,
                                saving = false,
                                errorMessage = null,
                                onDismiss = {},
                                onSave = { _, _ -> },
                                onCreateMachine = {},
                            )
                            2 -> ExerciseActionsDialog(
                                exercise = exercise,
                                trackedInProgress = true,
                                onDismiss = {},
                                onEdit = {},
                                onFavorite = {},
                                onDuplicate = {},
                                onConfigureTrackedRecords = {},
                                onArchive = {},
                                onDelete = {},
                            )
                            3 -> TrackedRecordsManagerDialog(
                                modifier = Modifier,
                                state = GymUiState(exercises = listOf(exercise), loading = false),
                                initialExerciseId = exercise.id,
                                onDismiss = {},
                                onSave = {},
                            )
                            4 -> RoutineProgramPositionDialog(
                                routine = routine,
                                days = days,
                                onDismiss = {},
                                onSave = { _, _, _ -> },
                            )
                            else -> WorkoutExerciseCard(
                                item = WorkoutExerciseUi(
                                    workoutExercise,
                                    exercise,
                                    listOf(workoutSet),
                                    emptyList(),
                                    0,
                                    null,
                                    null,
                                ),
                                preferredWeightUnitId = "kilogram",
                                preferredDistanceUnitId = "kilometre",
                                numberPrecision = 1,
                                showRpe = true,
                                showRir = false,
                                nextSetId = null,
                                nextInGroup = false,
                                canMoveUp = false,
                                canMoveDown = false,
                                onMoveUp = {},
                                onMoveDown = {},
                                onRemoveExercise = {},
                                onSubstituteExercise = {},
                                onAddSet = {},
                                onEditSet = {},
                                onEditNotes = {},
                                onCompleteSet = { _, _ -> },
                                onSaveQuickSet = { _, _, _ -> },
                                onDuplicateSet = {},
                                onDeleteSet = {},
                                onUndoDeleteSet = {},
                                onReorderSets = {},
                            )
                        }
                    }
                }
            }
        }

        captureVisualCatalogSurface("gym.workout.set-editor")
        compose.runOnIdle { surface = 1 }
        compose.onNodeWithText("Cable Row Notes").assertIsDisplayed()
        compose.waitForIdle()
        captureVisualCatalogSurface("gym.workout.notes")
        compose.runOnIdle { surface = 2 }
        compose.onNodeWithTag("exercise-detail-surface").assertIsDisplayed()
        compose.waitForIdle()
        captureVisualCatalogSurface("gym.exercise.actions")
        compose.runOnIdle { surface = 3 }
        compose.onNodeWithTag("tracked-records-manager").assertIsDisplayed()
        compose.waitForIdle()
        captureVisualCatalogSurface("gym.tracked-records")
        compose.runOnIdle { surface = 4 }
        compose.onNodeWithText("Program Position", substring = true).assertIsDisplayed()
        compose.waitForIdle()
        captureVisualCatalogSurface("gym.routine.program-position")
        compose.runOnIdle { surface = 5 }
        compose.onNodeWithContentDescription("More options for Cable Row").performClick()
        compose.onNodeWithText("Substitute Exercise").assertIsDisplayed()
        compose.waitForIdle()
        captureVisualCatalogSurface("gym.workout-exercise.menu")
        compose.runOnIdle { surface = 6 }
        compose.onNodeWithContentDescription("Manage set 1").performClick()
        compose.onNodeWithText("Duplicate Set").assertIsDisplayed()
        compose.waitForIdle()
        captureVisualCatalogSurface("gym.workout-set.menu")
    }

    @Test
    fun routineEditReconstructionPreservesAdvancedProgrammingFields() {
        val routine = GymRoutine(
            id = 1,
            uuid = "routine",
            name = "Strength cycle",
            notes = "Keep the plan",
            position = 0,
            archived = false,
            pinned = false,
            createdAtMillis = 1,
            updatedAtMillis = 2,
            programKind = RoutineProgramKind.FiveThreeOne,
            programPhaseCount = 4,
            programPhaseLabels = listOf("5s", "3s", "5/3/1", "Deload"),
            currentProgramPhaseIndex = 2,
            currentProgramCycle = 3,
            nextProgramDayPosition = 1,
            programPhaseRoles = listOf(
                RoutineProgramPhaseRole.Leader,
                RoutineProgramPhaseRole.Leader,
                RoutineProgramPhaseRole.Anchor,
                RoutineProgramPhaseRole.Deload,
            ),
            trainingMaxAdvanceAfterPhaseIndices = setOf(3),
            progressionMode = RoutineProgressionMode.PerformanceInformed,
            allowNonStandardHigherSuggestions = true,
        )
        val day = RoutineDay(2, "day", routine.id, "Upper", 0, 1, 2, progressionIndex = 4)
        val placement = RoutineExercise(
            id = 3,
            uuid = "placement",
            routineDayId = day.id,
            exerciseId = 4,
            position = 0,
            notes = "Bar path",
            groupKey = null,
            copyPreviousWorkout = false,
            createdAtMillis = 1,
            updatedAtMillis = 2,
            trainingMaxPercent = 87.5,
            progressionPercentages = listOf(100.0, 102.5, 90.0),
            alternativeExerciseIds = listOf(7L, 9L),
            trainingMaxValue = 225.0,
            trainingMaxUnitId = "pound",
            cycleIncrementValue = 5.0,
            trainingMaxSource = RoutineTrainingMaxSource.Explicit,
            trainingMaxBasisKind = TrainingMaxBasisKind.ActualOneRepMax,
            trainingMaxBasisValue = 265.0,
            trainingMaxBasisUnitId = "pound",
            trainingMaxIncreaseEligible = false,
            mainWorkScheme = RoutineMainWorkScheme.ClassicPrSet,
            supplementalScheme = RoutineSupplementalScheme.FirstSetLast,
            placementKind = RoutinePlacementKind.MainExercise,
            jokerSetsEnabled = true,
        )
        val plannedSet = RoutineSet(
            id = 5,
            uuid = "planned-set",
            routineExerciseId = placement.id,
            position = 0,
            draft = WorkoutSetDraft(
                reps = 5,
                repsMax = 8,
                planned = true,
                classification = WorkoutSetClassification.Amrap,
                loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                loadPercentage = 85.0,
                routinePhaseIndex = 2,
            ),
            createdAtMillis = 1,
            updatedAtMillis = 2,
        )

        val reconstructed = routineDraftForEditing(
            GymUiState(
                routines = listOf(routine),
                routineDays = listOf(day),
                routineExercises = listOf(placement),
                routineSets = listOf(plannedSet),
            ),
            routine,
        )
        val reconstructedPlacement = reconstructed.days.single().exercises.single()

        assertEquals(87.5, reconstructedPlacement.trainingMaxPercent, 0.0)
        assertEquals(listOf(100.0, 102.5, 90.0), reconstructedPlacement.progressionPercentages)
        assertEquals(listOf(7L, 9L), reconstructedPlacement.alternativeExerciseIds)
        assertEquals(225.0, reconstructedPlacement.trainingMaxValue!!, 0.0)
        assertEquals("pound", reconstructedPlacement.trainingMaxUnitId)
        assertEquals(5.0, reconstructedPlacement.cycleIncrementValue!!, 0.0)
        assertEquals(RoutineTrainingMaxSource.Explicit, reconstructedPlacement.trainingMaxSource)
        assertEquals(TrainingMaxBasisKind.ActualOneRepMax, reconstructedPlacement.trainingMaxBasisKind)
        assertEquals(265.0, reconstructedPlacement.trainingMaxBasisValue!!, 0.0)
        assertEquals("pound", reconstructedPlacement.trainingMaxBasisUnitId)
        assertFalse(reconstructedPlacement.trainingMaxIncreaseEligible)
        assertEquals(RoutineMainWorkScheme.ClassicPrSet, reconstructedPlacement.mainWorkScheme)
        assertEquals(RoutineSupplementalScheme.FirstSetLast, reconstructedPlacement.supplementalScheme)
        assertEquals(RoutinePlacementKind.MainExercise, reconstructedPlacement.placementKind)
        assertTrue(reconstructedPlacement.jokerSetsEnabled)
        assertEquals(4, reconstructed.days.single().progressionIndex)
        assertEquals(RoutineProgramKind.FiveThreeOne, reconstructed.program?.kind)
        assertEquals(listOf("5s", "3s", "5/3/1", "Deload"), reconstructed.program?.phaseLabels)
        assertEquals(
            listOf(
                RoutineProgramPhaseRole.Leader,
                RoutineProgramPhaseRole.Leader,
                RoutineProgramPhaseRole.Anchor,
                RoutineProgramPhaseRole.Deload,
            ),
            reconstructed.program?.phaseRoles,
        )
        assertEquals(setOf(3), reconstructed.program?.trainingMaxAdvanceAfterPhaseIndices)
        assertEquals(RoutineProgressionMode.PerformanceInformed, reconstructed.program?.progressionMode)
        assertTrue(reconstructed.program?.allowNonStandardHigherSuggestions == true)
        assertEquals(plannedSet.draft, reconstructedPlacement.plannedSets.single())
        assertEquals(
            "Phased Routine · Cycle 3 · 5/3/1 · Next · Upper",
            routineProgramStatusLabel(routine, "Upper"),
        )
    }

    @Test
    fun convertingExerciseToPoundsUsesPoundHardwareRatherThanConvertedDecimals() {
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                ExerciseEditorDialog(
                    exercise = null,
                    categories = emptyList(),
                    selectedCategoryIds = emptySet(),
                    defaultWeightUnit = "kilogram",
                    defaultRestSeconds = 120,
                    defaultFormula = EstimatedOneRepMaxFormula.Epley,
                    platePresets = emptyList(),
                    onDismiss = {},
                    onSave = {},
                )
            }
        }

        compose.onNodeWithText("Advanced Options").performClick()
        compose.onNodeWithText("lb").performScrollTo().performClick()
        captureVisualCatalogSurface("gym.defaults-change")
        compose.onNodeWithText("Convert Default Values").performClick()
        compose.onNodeWithTag("exercise-editor-list").performScrollToNode(hasTestTag("exercise-weight-increment"))
        compose.onNodeWithTag("exercise-weight-increment").assertTextContains("5")
        compose.onNodeWithTag("exercise-editor-list").performScrollToNode(hasTestTag("exercise-bar-weight"))
        compose.onNodeWithTag("exercise-bar-weight").assertTextContains("45")
        compose.onNodeWithTag("exercise-plates").assertTextContains("45,35,25,10,5,2.5")
        compose.onNodeWithTag("exercise-editor-list").performScrollToNode(hasText("Effort field"))
        compose.onNodeWithText("Effort field").assertIsDisplayed()
        compose.onAllNodes(hasText("RPE field")).assertCountEquals(0)
        compose.onAllNodes(hasText("RIR field")).assertCountEquals(0)
    }

    @Test
    fun exerciseEditorExplainsInvalidDefaultsAndDoesNotSubmitThem() {
        var submitted = false
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                ExerciseEditorDialog(
                    exercise = null,
                    categories = emptyList(),
                    selectedCategoryIds = emptySet(),
                    defaultWeightUnit = "kilogram",
                    defaultRestSeconds = 120,
                    defaultFormula = EstimatedOneRepMaxFormula.Epley,
                    platePresets = emptyList(),
                    onDismiss = {},
                    onSave = { submitted = true },
                )
            }
        }

        compose.onNodeWithTag("exercise-editor-name").performTextInput("Bench press")
        compose.onNodeWithText("Advanced Options").performClick()
        compose.onNodeWithTag("exercise-editor-list").performScrollToNode(hasTestTag("exercise-weight-increment"))
        compose.onNodeWithTag("exercise-weight-increment").performTextReplacement("0")
        compose.onNodeWithText("Save").performClick()

        compose.onNodeWithTag("exercise-editor-list").performScrollToNode(hasTestTag("exercise-save-problem"))
        compose.onNodeWithTag("exercise-save-problem")
            .assertIsDisplayed()
        compose.onNodeWithContentDescription("Save blocked. Weight increment must be above 0")
            .assertIsDisplayed()
        compose.runOnIdle { assertEquals(false, submitted) }
    }

    @Test
    fun durationExerciseOnlyShowsApplicableAdvancedOptionsAndRepairsItsGraphDefault() {
        var submitted: ExerciseDraft? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                ExerciseEditorDialog(
                    exercise = testExercise().copy(
                        trackingType = ExerciseTrackingType.DurationOnly,
                        defaultGraphMetric = GymGraphMetric.EstimatedOneRepMax.name,
                    ),
                    categories = emptyList(),
                    selectedCategoryIds = emptySet(),
                    defaultWeightUnit = "kilogram",
                    defaultRestSeconds = 120,
                    defaultFormula = EstimatedOneRepMaxFormula.Epley,
                    platePresets = emptyList(),
                    onDismiss = {},
                    onSave = { submitted = it },
                )
            }
        }

        captureVisualCatalogSurface("gym.exercise.editor")
        compose.onNodeWithText("Advanced Options").performClick()
        compose.onNodeWithTag("exercise-editor-list").performScrollToNode(hasText("Default graph"))
        compose.onAllNodesWithTag("exercise-weight-increment").assertCountEquals(0)
        compose.onAllNodesWithTag("exercise-repetition-increment").assertCountEquals(0)
        compose.onAllNodesWithText("Estimated 1RM formula").assertCountEquals(0)
        compose.onAllNodesWithText("Include in volume").assertCountEquals(0)
        compose.onNodeWithContentDescription("Default graph: Duration").assertIsDisplayed()
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle {
            assertEquals(GymGraphMetric.Duration.name, requireNotNull(submitted).defaultGraphMetric)
            assertEquals(false, requireNotNull(submitted).includeInVolume)
        }
    }

    @Test
    fun progressWithoutCompletedWorkoutsDoesNotShowInventedTrendControls() {
        var workoutRequested = false
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                GymProgressContent(
                    state = GymUiState(exercises = listOf(testExercise()), loading = false),
                    onOpenExercises = {},
                    onOpenWorkout = { workoutRequested = true },
                    onOpenWorkoutHistory = {},
                    onManageTrackedRecords = {},
                )
            }
        }

        compose.onNodeWithText("No Progress Data Yet").assertIsDisplayed()
        compose.onAllNodes(hasTestTag("gym-progress-exercise-selector")).assertCountEquals(0)
        compose.onNode(hasText("Start a Workout") and hasClickAction()).performClick()
        compose.runOnIdle { assertTrue(workoutRequested) }
    }

    @Test
    fun restTimerStacksActionsAtLargeTextAndNamesEveryAction() {
        val session = WorkoutSession(
            id = 91,
            uuid = "large-text-timer",
            name = "Workout",
            notes = "",
            startedAt = Instant.parse("2026-08-31T16:00:00Z"),
            endedAt = null,
            localDate = LocalDate.of(2026, 8, 31),
            zoneId = "UTC",
            state = WorkoutSessionState.Active,
            keepScreenAwake = false,
            restTimerDeadlineMillis = null,
            restTimerDurationSeconds = null,
            archived = false,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        compose.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, fontScale = 2f)) {
                WhipTheme(darkTheme = true, dynamicColor = false) {
                    RestTimerCard(
                        session = session,
                        remaining = 45,
                        duration = resolveWorkoutRestDuration(null, null, null, 120),
                        presetSeconds = DEFAULT_REST_TIMER_PRESET_SECONDS,
                        notificationPermissionRequested = false,
                        onSelectedSecondsChange = {},
                        onPresetSecondsChange = {},
                        onStart = { _, _ -> },
                        onAdjust = { _, _ -> },
                        onStop = {},
                    )
                }
            }
        }

        val timer = compose.onNodeWithText("Rest · 0:45").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val subtract = compose.onNodeWithContentDescription("Subtract 15 seconds from rest timer").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val add = compose.onNodeWithContentDescription("Add 15 seconds to rest timer").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val stop = compose.onNodeWithContentDescription("Stop rest timer").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        check(timer.bottom <= subtract.top) { "Large-text timer actions must move below the timer label" }
        check(subtract.right <= add.left && add.right <= stop.left) { "Timer actions must not overlap" }
    }

    @Test
    fun captureRestDurationCatalog() {
        val session = testHistorySession().copy(
            id = 9,
            uuid = "rest-catalog",
            name = "Full Body",
            endedAt = null,
            state = WorkoutSessionState.Active,
        )
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    RestTimerCard(
                        session = session,
                        remaining = null,
                        duration = resolveWorkoutRestDuration(null, null, null, 120),
                        presetSeconds = DEFAULT_REST_TIMER_PRESET_SECONDS,
                        notificationPermissionRequested = true,
                        onSelectedSecondsChange = {},
                        onPresetSecondsChange = {},
                        onStart = { _, _ -> },
                        onAdjust = { _, _ -> },
                        onStop = {},
                    )
                }
            }
        }

        compose.onNodeWithContentDescription("Custom Time for this workout").performClick()
        compose.onNodeWithText("Rest Time for This Workout").assertIsDisplayed()
        captureVisualCatalogSurface("gym.rest-duration")
    }

    @Test
    fun numberedMachineDefaultsToCompactOneThroughTenRangeAndIncrement() {
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                MachineEditorDialog(
                    machine = null,
                    exercises = listOf(testExercise()),
                    definitionLocked = false,
                    onDismiss = {},
                    onSave = {},
                )
            }
        }

        captureVisualCatalogSurface("gym.machine.editor")
        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasText("Numbered Stack / Level"))
        compose.onNodeWithText("Numbered Stack / Level").performClick()
        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasTestTag("machine-load-spec"))
        compose.onNodeWithTag("machine-load-spec").assertTextContains("1-10")
        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasTestTag("machine-load-increment"))
        compose.onNodeWithTag("machine-load-increment").assertTextContains("1")
        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasTestTag("machine-load-preview"))
        compose.onNodeWithTag("machine-load-preview").assertIsDisplayed().assertTextContains("Preview · 10 values · 1-10 by 1")
    }

    @Test
    fun machineCanBeSavedWithoutExercises() {
        var saved: GymMachineDraft? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                MachineEditorDialog(
                    machine = null,
                    exercises = emptyList(),
                    definitionLocked = false,
                    onDismiss = {},
                    onSave = { saved = it },
                )
            }
        }

        compose.onNodeWithTag("machine-editor-name").performTextInput("Standalone cable")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasTestTag("machine-choose-exercises"))
        compose.onNodeWithTag("machine-choose-exercises").performClick()
        compose.onNodeWithTag("machine-exercise-picker").assertIsDisplayed()
        compose.onAllNodesWithTag("machine-create-exercise").assertCountEquals(0)
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Save").assertIsEnabled().performClick()

        compose.runOnIdle { assertEquals(emptySet<Long>(), saved?.exerciseIds) }
    }

    @Test
    fun machineDraftSurvivesCreatingAndLinkingAnExerciseAndSavesReverseLevels() {
        val existing = testExercise().copy(id = 1, name = "Cable row")
        val created = testExercise().copy(id = 2, name = "Cable press")
        var createdRequest by mutableStateOf<Long?>(null)
        var createdName: String? = null
        var saved: GymMachineDraft? = null
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                MachineEditorDialog(
                    machine = null,
                    exercises = listOf(existing, created),
                    definitionLocked = false,
                    createdExerciseIdRequest = createdRequest,
                    onCreatedExerciseRequestConsumed = { createdRequest = null },
                    onCreateExercise = { seed -> createdName = seed; createdRequest = created.id },
                    onDismiss = {},
                    onSave = { saved = it },
                )
            }
        }

        compose.onNodeWithTag("machine-editor-name").performTextInput("Shared cable")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasTestTag("machine-choose-exercises"))
        compose.onNodeWithTag("machine-choose-exercises").performClick()
        compose.onNodeWithTag("machine-exercise-search").performTextInput("  Cable fly  ")
        compose.onNodeWithTag("machine-exercise-picker").assertIsDisplayed()
        compose.onNodeWithTag("machine-exercise-picker-list").assertIsDisplayed()
        compose.onNodeWithTag("machine-create-exercise-empty").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("Cable fly", createdName) }
        compose.onNodeWithTag("machine-editor-name").assertTextContains("Shared cable")
        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasTestTag("machine-choose-exercises"))
        compose.onNodeWithTag("machine-choose-exercises").performClick()
        compose.onNodeWithTag("machine-exercise-picker-list").performScrollToNode(hasText("Cable row"))
        compose.onNodeWithText("Cable row").performClick()
        compose.onNodeWithText("Done").performClick()

        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasText("Numbered Stack / Level"))
        compose.onNodeWithText("Numbered Stack / Level").performClick()
        compose.onNodeWithTag("machine-editor-list").performScrollToNode(hasText("Higher number = less resistance"))
        compose.onNodeWithText("Higher number = less resistance").performClick()
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle {
            assertEquals(setOf(1L, 2L), saved?.exerciseIds)
            assertEquals(MachineLevelDirection.HigherNumberLessResistance, saved?.levelDirection)
        }
    }

    @Test
    @AndroidFontScale
    fun machineDeleteDialogExplainsPreservedHistoryAndBlocksActiveUse() {
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(compose.density.density, fontScale = 2f),
                LocalWhipDialogPlacement provides WhipDialogPlacement(maxWidth = 320.dp),
            ) {
                WhipTheme(darkTheme = true, dynamicColor = false) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        MachinePermanentDeleteDialog(
                            modifier = Modifier.width(320.dp),
                            machineName = "Downtown cable stack",
                            impact = MachineDeletionImpact(
                                machineId = 1,
                                machineUuid = "machine-1",
                                displayName = "Downtown cable stack",
                                configurationVersion = 2,
                                historicalPlacements = 9,
                                completedSessions = 9,
                                setCount = 46,
                                firstWorkoutDate = LocalDate.of(2026, 1, 1),
                                lastWorkoutDate = LocalDate.of(2026, 8, 1),
                                activePlacements = 1,
                                routineReferences = 2,
                                routineNames = listOf("Push A", "Upper"),
                                currentPersonalRecords = 3,
                                siblingVersions = 2,
                                revisionToken = "revision",
                            ),
                            targetMissing = false,
                            preparing = false,
                            deleting = false,
                            errorMessage = null,
                            onDismiss = {},
                            onReviewUpdatedImpact = {},
                            onConfirm = {},
                            onReviewRoutines = {},
                            onOpenActiveWorkout = {},
                            onBackUpFirst = {},
                        )
                    }
                }
            }
        }

        compose.assertDialogFontScale()

        compose.onNodeWithText("Delete “Downtown cable stack” v2 Permanently?").assertIsDisplayed()
        compose.onNodeWithText("Active Workout").assertIsDisplayed()
        compose.onNodeWithTag("machine-delete-confirm").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Cancel").assertIsDisplayed()
        captureVisualCatalogSurface("gym.machine.permanent-delete")
        compose.onNodeWithTag("machine-delete-impact-list").performScrollToNode(hasText("Kept"))
        compose.onNodeWithText("Kept").assertIsDisplayed()
        compose.onNodeWithTag("machine-delete-impact-list").performScrollToNode(hasText("Needs Attention"))
        compose.onNodeWithText("Needs Attention").assertIsDisplayed()
        compose.onNodeWithTag("machine-delete-impact-list").performScrollToNode(hasText("Other configuration versions stay.", substring = true))
        compose.onNodeWithText("Other configuration versions stay.", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("machine-delete-confirm").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    @AndroidFontScale
    fun machineDeleteDialogMakesAnUnverifiedOutcomeRetryableAtNarrowLargeText() {
        var retried = false
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(compose.density.density, fontScale = 2f),
                LocalWhipDialogPlacement provides WhipDialogPlacement(maxWidth = 320.dp),
            ) {
                WhipTheme(darkTheme = true, dynamicColor = false) {
                    MachinePermanentDeleteDialog(
                        modifier = Modifier.width(320.dp),
                        machineName = "Cable stack",
                        impact = null,
                        targetMissing = false,
                        preparing = false,
                        deleting = false,
                        errorMessage = "The previous deletion outcome could not be verified.",
                        onDismiss = {},
                        onReviewUpdatedImpact = { retried = true },
                        onConfirm = {},
                        onReviewRoutines = {},
                        onOpenActiveWorkout = {},
                        onBackUpFirst = {},
                        outcomeVerificationPending = true,
                    )
                }
            }
        }

        compose.assertDialogFontScale()
        captureVisualCatalogSurface("gym.machine.delete-retry.large")

        compose.onNodeWithTag("machine-delete-error").assertIsDisplayed()
        compose.onNodeWithText("Retry Verification").performClick()
        compose.runOnIdle { assertTrue(retried) }
        compose.onNodeWithTag("machine-delete-confirm").assertIsNotEnabled()
    }

    @Test
    @AndroidFontScale
    fun exerciseDeleteDialogBlocksActiveUseAndPreservesTrainingMaxAuditHistory() {
        var openedWorkout = false
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(compose.density.density, fontScale = 2f),
                LocalWhipDialogPlacement provides WhipDialogPlacement(maxWidth = 320.dp),
            ) {
                WhipTheme(darkTheme = true, dynamicColor = false) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        ExercisePermanentDeleteDialog(
                            modifier = Modifier.width(320.dp),
                            exerciseName = "Zercher squat",
                            impact = exerciseDeletionImpact(activePlacements = 1, trainingMaxDecisionCount = 3),
                            targetMissing = false,
                            preparing = false,
                            deleting = false,
                            errorMessage = null,
                            onDismiss = {},
                            onReviewUpdatedImpact = {},
                            onOpenActiveWorkout = { openedWorkout = true },
                            onConfirm = {},
                        )
                    }
                }
            }
        }

        compose.assertDialogFontScale()

        captureVisualCatalogSurface("gym.exercise.permanent-delete")
        val dialog = compose.onNodeWithTag("exercise-delete-dialog").getUnclippedBoundsInRoot()
        assertTrue(dialog.right - dialog.left <= 321.dp)
        compose.onNodeWithText("Active Workout").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("exercise-delete-impact-list").performScrollToNode(hasText("Preserved"))
        compose.onNodeWithText("Preserved").assertIsDisplayed()
        compose.onNodeWithTag("exercise-delete-impact-list").performScrollToNode(hasText("References changed"))
        compose.onNodeWithText("References changed").assertIsDisplayed()
        compose.onNodeWithTag("exercise-delete-impact-list").performScrollToNode(hasText("Open Active Workout"))
        compose.onNodeWithText("Open Active Workout").assertIsDisplayed().performClick()
        compose.onNodeWithTag("exercise-delete-confirm").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Cancel").assertIsDisplayed()
        compose.runOnIdle { assertTrue(openedWorkout) }
    }

    @Test
    fun routineDeleteDialogBlocksActiveSourceAndKeepsWorkoutAndTrainingMaxHistory() {
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    RoutinePermanentDeleteDialog(
                        routineName = "Phased Strength Plan",
                        impact = RoutineDeletionImpact(
                            routineId = 8,
                            displayName = "Phased Strength Plan",
                            activeSession = true,
                            dayCount = 3,
                            routinePlacementCount = 3,
                            routineSetCount = 36,
                            preservedWorkoutHistoryCount = 11,
                            trainingMaxDecisionCount = 6,
                            revisionToken = "routine-revision",
                        ),
                        targetMissing = false,
                        preparing = false,
                        deleting = false,
                        errorMessage = null,
                        onDismiss = {},
                        onReviewUpdatedImpact = {},
                        onOpenActiveWorkout = {},
                        onConfirm = {},
                    )
                }
            }
        }

        captureVisualCatalogSurface("gym.routine.permanent-delete")
        compose.onNodeWithText("Active Workout").assertIsDisplayed()
        compose.onNodeWithText("Kept").assertIsDisplayed()
        compose.onNodeWithTag("routine-delete-confirm").assertIsNotEnabled()
    }

    @Test
    @AndroidFontScale
    fun exerciseDeleteDialogKeepsStaleImpactFailureInlineAndRetryable() {
        var reviews = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                WhipTheme(darkTheme = true, dynamicColor = false) {
                    ExercisePermanentDeleteDialog(
                        exerciseName = "Bench press",
                        impact = exerciseDeletionImpact(),
                        targetMissing = false,
                        preparing = false,
                        deleting = false,
                        errorMessage = "The Exercise or its deletion impact changed while the confirmation was open.",
                        onDismiss = {},
                        onReviewUpdatedImpact = { reviews++ },
                        onOpenActiveWorkout = {},
                        onConfirm = {},
                    )
                }
            }
        }

        compose.assertDialogFontScale()
        captureVisualCatalogSurface("gym.exercise.delete-retry.large")

        compose.onNodeWithTag("exercise-delete-error").assertIsDisplayed()
        compose.onNodeWithText("Review Updated Impact").performClick()
        compose.onNodeWithTag("exercise-delete-confirm").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, reviews) }
    }

    @Test
    fun missingExerciseDeletionTargetOffersCloseInsteadOfDestructiveRetry() {
        var closes = 0
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                ExercisePermanentDeleteDialog(
                    exerciseName = "Bench press",
                    impact = null,
                    targetMissing = true,
                    preparing = false,
                    deleting = false,
                    errorMessage = "Exercise no longer exists. It may already have been deleted.",
                    onDismiss = { closes++ },
                    onReviewUpdatedImpact = {},
                    onOpenActiveWorkout = {},
                    onConfirm = {},
                )
            }
        }

        compose.onNodeWithText("Exercise unavailable").assertIsDisplayed()
        val liveRegion = compose.onNodeWithTag("exercise-delete-error")
            .fetchSemanticsNode().config[SemanticsProperties.LiveRegion]
        assertEquals(LiveRegionMode.Polite, liveRegion)
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithTag("exercise-delete-confirm").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, closes) }
    }

    @Test
    fun missingRoutineDeletionTargetIsAnnouncedAndCannotBeDeleted() {
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                RoutinePermanentDeleteDialog(
                    routineName = "Custom 5/3/1",
                    impact = null,
                    targetMissing = true,
                    preparing = false,
                    deleting = false,
                    errorMessage = "Routine no longer exists. It may already have been deleted.",
                    onDismiss = {},
                    onReviewUpdatedImpact = {},
                    onOpenActiveWorkout = {},
                    onConfirm = {},
                )
            }
        }

        compose.onNodeWithText("Routine unavailable").assertIsDisplayed()
        val liveRegion = compose.onNodeWithTag("routine-delete-error")
            .fetchSemanticsNode().config[SemanticsProperties.LiveRegion]
        assertEquals(LiveRegionMode.Polite, liveRegion)
        compose.onNodeWithTag("routine-delete-confirm").assertIsNotEnabled()
    }

    @Test
    fun restoredUnverifiedMissingExerciseKeepsRetryVerificationInsteadOfAbandoningRecovery() {
        val restoration = StateRestorationTester(compose)
        var reviews = 0
        restoration.setContent {
            val verificationPending = androidx.compose.runtime.saveable.rememberSaveable {
                mutableStateOf(true)
            }
            WhipTheme(darkTheme = true, dynamicColor = false) {
                ExercisePermanentDeleteDialog(
                    exerciseName = "Bench press",
                    impact = null,
                    targetMissing = true,
                    preparing = false,
                    deleting = false,
                    errorMessage = "Exercise no longer exists. It may already have been deleted.",
                    onDismiss = {},
                    onReviewUpdatedImpact = { reviews++ },
                    onOpenActiveWorkout = {},
                    onConfirm = {},
                    outcomeVerificationPending = verificationPending.value,
                )
            }
        }

        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithText("Outcome not verified").assertIsDisplayed()
        compose.onNodeWithText("Retry Verification").performClick()
        compose.onAllNodesWithText("Close").assertCountEquals(0)
        compose.runOnIdle { assertEquals(1, reviews) }
    }

    @Test
    fun quickSetEntryPresentsInputsBeforeItsPrimarySaveAction() {
        val exercise = testExercise()
        val workoutExercise = WorkoutExercise(
            id = 2,
            uuid = "workout-exercise",
            sessionId = 3,
            exerciseId = exercise.id,
            position = 0,
            notes = "",
            groupId = null,
            createdAtMillis = 1,
            updatedAtMillis = 1,
            loadInterpretationSnapshot = LoadInterpretation.Total,
            trackingTypeSnapshot = ExerciseTrackingType.WeightReps,
            exerciseWeightUnitSnapshot = "kilogram",
        )
        val set = WorkoutSet(
            id = 4,
            uuid = "set",
            workoutExerciseId = workoutExercise.id,
            position = 0,
            classification = WorkoutSetClassification.Working,
            planned = false,
            completed = false,
            canonicalWeightKg = 50.0,
            enteredWeight = 50.0,
            enteredWeightUnitId = "kilogram",
            repetitions = 5,
            canonicalDistanceMetres = null,
            enteredDistance = null,
            enteredDistanceUnitId = null,
            durationSeconds = null,
            bodyweightKg = null,
            note = "",
            rpe = null,
            rir = null,
            tempo = "",
            restSeconds = 120,
            completedAtMillis = null,
            deletedAtMillis = null,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                QuickSetEntry(
                    set = set,
                    exercise = exercise,
                    workoutExercise = workoutExercise,
                    machine = null,
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    showRpe = false,
                    showRir = false,
                    onMoreDetails = {},
                    onSave = { _, _, _ -> },
                )
            }
        }

        val load = compose.onNodeWithTag("quick-set-load-${set.id}").fetchSemanticsNode().boundsInRoot
        val reps = compose.onNodeWithTag("quick-set-reps-${set.id}").fetchSemanticsNode().boundsInRoot
        val saveNext = compose.onNodeWithTag("quick-set-save-next-${set.id}").fetchSemanticsNode().boundsInRoot
        check(load.bottom <= saveNext.top && reps.bottom <= saveNext.top) {
            "Quick-set actions must follow data entry: load=$load reps=$reps saveNext=$saveNext"
        }
        compose.onNodeWithContentDescription("Decrease Cable row Weight (kg) by 2.5").assertIsDisplayed()
        compose.onNodeWithContentDescription("Increase Cable row Weight (kg) by 2.5").assertIsDisplayed()
        val decrement = compose.onNodeWithContentDescription("Decrease Cable row Weight (kg) by 2.5").fetchSemanticsNode().boundsInRoot
        val increment = compose.onNodeWithContentDescription("Increase Cable row Weight (kg) by 2.5").fetchSemanticsNode().boundsInRoot
        check(decrement.left >= load.left && decrement.right <= load.right)
        check(increment.left >= load.left && increment.right <= load.right)
    }

    @Test
    fun pendingRestPresetSurvivesDiscardReviewFailureAndRestoreUntilConfirmedReceipt() {
        var presets by mutableStateOf(DEFAULT_REST_TIMER_PRESET_SECONDS)
        var saving by mutableStateOf(false)
        var error by mutableStateOf<String?>(null)
        var revision by mutableStateOf(0)
        var submitted: List<Int>? = null
        var attempts = 0
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                com.whip.app.ui.RestDurationDialog(
                    initialSeconds = 120, isWorkoutOverride = false, presetSeconds = presets,
                    onDismiss = {}, onConfirm = {}, saving = saving, error = error,
                    presetSaveRevision = revision,
                    onPresetSecondsChange = { submitted = it; attempts++; error = null; saving = true },
                )
            }
        }
        compose.onNodeWithText("Manage Presets").performClick()
        compose.onNodeWithTag("rest-preset-seconds").performTextReplacement("45")
        closeSoftKeyboard()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Discard Unsaved Changes?").assertIsDisplayed()
        compose.onNodeWithText("Keep Editing").performClick()
        compose.onNodeWithTag("rest-preset-seconds").assertTextContains("45")
        captureVisualCatalogSurface("overhaul.gym.rest-preset-before-save")
        compose.onNodeWithText("Save Presets").performClick()
        compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("persistence-saving-overlay").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, attempts); assertTrue(45 in requireNotNull(submitted)); saving = false; error = "Storage unavailable" }
        compose.onNodeWithTag("rest-presets-error").assertIsDisplayed()
        compose.onNodeWithText("Manage Rest Presets").assertIsDisplayed()
        compose.onNodeWithTag("rest-preset-seconds").assertTextContains("45")
        captureVisualCatalogSurface("overhaul.gym.rest-preset-failed-retained")
        compose.onNodeWithText("Save Presets").performClick()
        compose.runOnIdle { assertEquals(2, attempts); presets = requireNotNull(submitted); saving = false; revision++ }
        compose.onNodeWithText("Rest Time for This Workout").assertIsDisplayed()
        compose.onNodeWithText("0:45").assertIsDisplayed()
        captureVisualCatalogSurface("overhaul.gym.rest-preset-after-confirmed-save")
        compose.onNodeWithText("Manage Presets").performClick()
        compose.onNodeWithTag("rest-preset-seconds").performTextReplacement("75")
        closeSoftKeyboard()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Manage Rest Presets").assertIsDisplayed()
        compose.onNodeWithTag("rest-preset-seconds").assertTextContains("75")
        compose.runOnIdle { assertEquals(2, attempts) }
        captureVisualCatalogSurface("overhaul.gym.rest-preset-later-draft-restored")
    }

    @Test
    fun readyRestTimerMakesWorkoutScopedDurationDiscoverableAndUsesIt() {
        val session = WorkoutSession(
            id = 9,
            uuid = "session",
            name = "Workout",
            notes = "",
            startedAt = Instant.parse("2026-08-22T16:00:00Z"),
            endedAt = null,
            localDate = LocalDate.of(2026, 8, 22),
            zoneId = "UTC",
            state = WorkoutSessionState.Active,
            keepScreenAwake = false,
            restTimerDeadlineMillis = null,
            restTimerDurationSeconds = null,
            archived = false,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        var startedWith: Int? = null
        var savedPresets: List<Int>? = null
        compose.setContent {
            var selectedSeconds by remember { mutableStateOf<Int?>(null) }
            var presets by remember { mutableStateOf(DEFAULT_REST_TIMER_PRESET_SECONDS) }
            var presetRevision by remember { mutableStateOf(0) }
            WhipTheme(dynamicColor = false) {
                RestTimerCard(
                    session = session,
                    remaining = null,
                    duration = resolveWorkoutRestDuration(selectedSeconds, null, null, 120),
                    presetSeconds = presets,
                    notificationPermissionRequested = true,
                    onSelectedSecondsChange = { selectedSeconds = it },
                    onPresetSecondsChange = { changed -> presets = changed; savedPresets = changed; presetRevision++ },
                    presetsSaveRevision = presetRevision,
                    onStart = { _, seconds -> startedWith = seconds },
                    onAdjust = { _, _ -> },
                    onStop = {},
                )
            }
        }

        compose.onNodeWithText("Rest · 2:00").assertIsDisplayed()
        compose.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Rest timer ready, 2:00 selected, App default"),
        ).assertIsDisplayed()
        compose.onNodeWithText("Custom Time").assertIsDisplayed()
        compose.onNodeWithContentDescription("Custom Time for this workout").assertIsDisplayed().performClick()
        compose.onNodeWithText("Rest Time for This Workout").assertIsDisplayed()
        listOf("1:00", "1:30", "2:00", "2:30", "3:00", "5:00").forEach { preset ->
            compose.onAllNodes(hasText(preset)).fetchSemanticsNodes().also { nodes ->
                check(nodes.isNotEmpty()) { "Missing default rest preset $preset" }
            }
        }
        compose.onNodeWithText("Manage Presets").performClick()
        compose.onNodeWithTag("rest-preset-seconds").performTextReplacement("45")
        compose.onNodeWithTag("rest-preset-seconds").assertTextContains("45")
        compose.waitForIdle()
        compose.onNodeWithText("Add Preset").assertIsDisplayed().assertIsEnabled().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText("0:45 ×")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Save Presets").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText("Rest Time for This Workout")).fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodes(hasText("0:45")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("0:45").assertIsDisplayed()
        compose.runOnIdle { check(savedPresets?.contains(45) == true) }
        compose.onNodeWithContentDescription("Increase workout rest time by 15").performClick()
        compose.onNodeWithText("Use for This Workout").performClick()
        compose.onNodeWithText("Rest · 2:15").assertIsDisplayed()
        compose.onNodeWithText("Start").performClick()
        compose.runOnIdle { assertEquals(135, startedWith) }
        compose.onNodeWithText("Workout override").assertIsDisplayed()
        compose.onNodeWithContentDescription("Custom Time for this workout").performClick()
        compose.onNodeWithText("Follow Set rest").performClick()
        compose.onNodeWithText("Rest · 2:00").assertIsDisplayed()
        compose.onNodeWithText("App default").assertIsDisplayed()
        compose.onNodeWithContentDescription("Start rest timer").performClick()
        compose.runOnIdle { assertEquals(120, startedWith) }
    }

    @Test
    fun activeWorkoutUsesOneFocusedComposerWithExerciseAndSetReordering() {
        val exercise = testExercise().copy(name = "Bench press")
        val workoutExercise = testWorkoutExercise(exercise)
        val first = testWorkoutSet(4, workoutExercise.id).copy(planned = true, rpe = 8.0)
        val second = testWorkoutSet(5, workoutExercise.id).copy(position = 1)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(compose.density.density, fontScale = 2f)) {
                WhipTheme(dynamicColor = false) {
                    androidx.compose.foundation.layout.Box(Modifier.width(320.dp)) {
                        WorkoutExerciseCard(
                    item = WorkoutExerciseUi(workoutExercise, exercise, listOf(first, second), emptyList(), 0, null, null),
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    numberPrecision = 1,
                    showRpe = false,
                    showRir = false,
                    nextSetId = second.id,
                    nextInGroup = false,
                    arranging = true,
                    canMoveUp = false,
                    canMoveDown = true,
                    onMoveUp = {},
                    onMoveDown = {},
                    onRemoveExercise = {},
                    onSubstituteExercise = {},
                    onAddSet = {},
                    onEditSet = {},
                    onEditNotes = {},
                    onCompleteSet = { _, _ -> },
                    onSaveQuickSet = { _, _, _ -> },
                    onDuplicateSet = {},
                    onDeleteSet = {},
                    onUndoDeleteSet = {},
                    onReorderSets = {},
                        )
                    }
                }
            }
        }

        compose.onAllNodesWithContentDescription("Reorder Bench press").assertCountEquals(1)
        compose.onAllNodes(hasTestTag("active-set-composer")).assertCountEquals(1)
        compose.onAllNodesWithContentDescription("Reorder set 1").assertCountEquals(1)
        compose.onAllNodesWithContentDescription("Reorder set 2").assertCountEquals(1)
        compose.onNodeWithText("Set 2", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Working · Planned · RPE 8").assertIsDisplayed()
        compose.onNodeWithTag("quick-set-load-${second.id}").assertIsDisplayed()
        compose.onNodeWithTag("quick-set-reps-${second.id}").assertIsDisplayed()
        compose.onNodeWithTag("quick-set-save-next-${second.id}").assertIsDisplayed()
        val firstBounds = compose.onNodeWithContentDescription("Reorder set 1").fetchSemanticsNode().boundsInRoot
        val focusedBounds = compose.onNodeWithTag("active-set-composer").fetchSemanticsNode().boundsInRoot
        check(firstBounds.bottom <= focusedBounds.top) {
            "Up Next composer must remain in set order instead of being extracted above set 1: first=$firstBounds focused=$focusedBounds"
        }
    }

    @Test
    fun passiveWorkoutSetCardSeparatesIdentityValuesStatusAndTargetAtLargeText() {
        val exercise = testExercise().copy(name = "Flat barbell bench press")
        val workoutExercise = testWorkoutExercise(exercise)
        val completed = testWorkoutSet(14, workoutExercise.id).copy(
            classification = WorkoutSetClassification.Amrap,
            completed = true,
            completedAtMillis = 2,
            rpe = 9.5,
            workSectionSnapshot = RoutineWorkSection.Main,
            prescribedCanonicalWeightKg = 50.0,
            prescribedEnteredWeight = 50.0,
            prescribedWeightUnitId = "kilogram",
            prescribedRepetitions = 5,
            prescriptionSourceLabel = "85.0% of explicit training max",
        )
        var editedSetId: Long? = null
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(compose.density.density, fontScale = 2f)) {
                WhipTheme(dynamicColor = false) {
                    androidx.compose.foundation.layout.Box(Modifier.width(320.dp)) {
                        WorkoutExerciseCard(
                            item = WorkoutExerciseUi(workoutExercise, exercise, listOf(completed), emptyList(), 0, null, null),
                            preferredWeightUnitId = "kilogram",
                            preferredDistanceUnitId = "kilometre",
                            numberPrecision = 1,
                            showRpe = true,
                            showRir = false,
                            nextSetId = null,
                            nextInGroup = false,
                            canMoveUp = false,
                            canMoveDown = false,
                            onMoveUp = {},
                            onMoveDown = {},
                            onRemoveExercise = {},
                            onSubstituteExercise = {},
                            onAddSet = {},
                            onEditSet = { editedSetId = it.id },
                            onEditNotes = {},
                            onCompleteSet = { _, _ -> },
                            onSaveQuickSet = { _, _, _ -> },
                            onDuplicateSet = {},
                            onDeleteSet = {},
                            onUndoDeleteSet = {},
                            onReorderSets = {},
                        )
                    }
                }
            }
        }

        compose.onNodeWithContentDescription("1 Completed Set").performClick()
        val card = compose.onNodeWithTag("workout-set-card-${completed.id}").assertIsDisplayed()
        val identity = compose.onNodeWithText("Main · Set 1", useUnmergedTree = true)
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val values = compose.onNodeWithTag("workout-set-values-${completed.id}", useUnmergedTree = true).assertTextContains("50 kg × 5 reps")
            .fetchSemanticsNode().boundsInRoot
        val status = compose.onNodeWithTag("workout-set-status-${completed.id}", useUnmergedTree = true)
            .assertTextContains("AMRAP · Performed · RPE 9.5").fetchSemanticsNode().boundsInRoot
        val target = compose.onNodeWithText(
            "explicit training max",
            substring = true,
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        check(identity.bottom <= values.top && values.bottom <= status.top && status.bottom <= target.top) {
            "Set-card hierarchy must remain vertically scannable at 200% text: identity=$identity values=$values status=$status target=$target"
        }
        val density = compose.density.density
        val menu = compose.onNodeWithContentDescription("Manage set 1").fetchSemanticsNode().boundsInRoot
        val completion = compose.onNodeWithContentDescription("Mark set 1 incomplete").fetchSemanticsNode().boundsInRoot
        check(menu.width >= 48f * density && menu.height >= 48f * density) { "Set menu target must remain at least 48 dp: $menu" }
        check(completion.width >= 48f * density && completion.height >= 48f * density) {
            "Set completion target must remain at least 48 dp: $completion"
        }
        card.performClick()
        compose.runOnIdle { assertEquals(completed.id, editedSetId) }
    }

    @Test
    fun substitutingAnExerciseWithSavedSetsDisclosesReplacementBeforeOpeningPicker() {
        val exercise = testExercise().copy(name = "Bench press")
        val workoutExercise = testWorkoutExercise(exercise)
        val savedSet = testWorkoutSet(4, workoutExercise.id).copy(completed = true, completedAtMillis = 2)
        var substitutionRequested = false
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutExerciseCard(
                    item = WorkoutExerciseUi(workoutExercise, exercise, listOf(savedSet), emptyList(), 0, null, null),
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    numberPrecision = 1,
                    showRpe = false,
                    showRir = false,
                    nextSetId = null,
                    nextInGroup = false,
                    canMoveUp = false,
                    canMoveDown = false,
                    onMoveUp = {},
                    onMoveDown = {},
                    onRemoveExercise = {},
                    onSubstituteExercise = { substitutionRequested = true },
                    onAddSet = {},
                    onEditSet = {},
                    onEditNotes = {},
                    onCompleteSet = { _, _ -> },
                    onSaveQuickSet = { _, _, _ -> },
                    onDuplicateSet = {},
                    onDeleteSet = {},
                    onUndoDeleteSet = {},
                    onReorderSets = {},
                )
            }
        }

        compose.onNodeWithContentDescription("More options for Bench press").performClick()
        compose.onNodeWithText("Substitute Exercise").performClick()
        compose.onNodeWithText("Replace Bench press?").assertIsDisplayed()
        compose.onNodeWithText(
            "Completed sets stay attached to Bench press in History. Unperformed sets are marked as replaced; the new exercise is logged separately.",
        )
            .assertIsDisplayed()
        compose.runOnIdle { assertEquals(false, substitutionRequested) }
        compose.onNodeWithText("Choose Replacement").performClick()
        compose.runOnIdle { assertEquals(true, substitutionRequested) }
    }

    @Test
    fun requiredMainSetUsesExplicitNotPerformedWarningInsteadOfGenericRemoval() {
        val exercise = testExercise().copy(name = "Deadlift")
        val workoutExercise = testWorkoutExercise(exercise)
        val mainSet = testWorkoutSet(44, workoutExercise.id).copy(
            workSectionSnapshot = RoutineWorkSection.Main,
            requiredForProgressionSnapshot = true,
        )
        val reviewedBoundary = WorkoutSetMutationBoundary(
            sessionId = workoutExercise.sessionId,
            sessionUuid = "session-reviewed",
            workoutRevision = 3,
            workoutExerciseId = workoutExercise.id,
            workoutExerciseUuid = workoutExercise.uuid,
            setId = mainSet.id,
            setUuid = mainSet.uuid,
            setUpdatedAtMillis = mainSet.updatedAtMillis,
            expectedDeletedAtMillis = null,
            expectedRemovalReason = null,
        )
        val latestBoundary = mutableStateOf(reviewedBoundary)
        var submittedBoundary: WorkoutSetMutationBoundary? = null
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val currentBoundary = latestBoundary.value
                    WorkoutExerciseCard(
                        item = WorkoutExerciseUi(workoutExercise, exercise, listOf(mainSet), emptyList(), 0, null, null),
                        preferredWeightUnitId = "kilogram",
                        preferredDistanceUnitId = "kilometre",
                        numberPrecision = 1,
                        showRpe = false,
                        showRir = false,
                        nextSetId = null,
                        nextInGroup = false,
                        canMoveUp = false,
                        canMoveDown = false,
                        onMoveUp = {},
                        onMoveDown = {},
                        onRemoveExercise = {},
                        onSubstituteExercise = {},
                        onAddSet = {},
                        onEditSet = {},
                        onEditNotes = {},
                        onCompleteSet = { _, _ -> },
                        onSaveQuickSet = { _, _, _ -> },
                        onDuplicateSet = {},
                        captureSetBoundary = { currentBoundary },
                        onDeleteSet = { submittedBoundary = it },
                        onUndoDeleteSet = {},
                        onReorderSets = {},
                    )
                }
            }
        }

        compose.onAllNodesWithContentDescription("Reorder Deadlift").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Reorder set 1").assertCountEquals(0)
        compose.onNodeWithContentDescription("Manage set 1").performClick()
        compose.onNodeWithText("Mark Main Set Not Performed").performClick()
        compose.onNodeWithText("Mark Main Set not performed?").assertIsDisplayed()
        captureVisualCatalogSurface("gym.confirmation")
        compose.onNodeWithText("It can hold this exercise's Training Max progression", substring = true).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(null, submittedBoundary)
            latestBoundary.value = reviewedBoundary.copy(
                workoutRevision = 4,
                setUpdatedAtMillis = reviewedBoundary.setUpdatedAtMillis + 1,
            )
        }
        compose.onNodeWithText("Mark Not Performed").performClick()
        compose.runOnIdle { assertEquals(reviewedBoundary, submittedBoundary) }
    }

    @Test
    fun destructiveFailureStaysInReviewAndCancelClearsItsOwnedError() {
        val exercise = testExercise().copy(name = "Paused squat")
        val workoutExercise = testWorkoutExercise(exercise)
        val mainSet = testWorkoutSet(45, workoutExercise.id).copy(
            workSectionSnapshot = RoutineWorkSection.Main,
            requiredForProgressionSnapshot = true,
        )
        val error = mutableStateOf<String?>(null)
        var clearCount = 0
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutExerciseCard(
                    item = WorkoutExerciseUi(workoutExercise, exercise, listOf(mainSet), emptyList(), 0, null, null),
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    numberPrecision = 1,
                    showRpe = false,
                    showRir = false,
                    nextSetId = null,
                    nextInGroup = false,
                    canMoveUp = false,
                    canMoveDown = false,
                    onMoveUp = {},
                    onMoveDown = {},
                    sessionMutationError = error.value,
                    onClearSessionMutationError = { error.value = null; clearCount += 1 },
                    onRemoveExercise = {},
                    onSubstituteExercise = {},
                    onAddSet = {},
                    onEditSet = {},
                    onEditNotes = {},
                    onCompleteSet = { _, _ -> },
                    onSaveQuickSet = { _, _, _ -> },
                    onDuplicateSet = {},
                    onDeleteSet = {},
                    onUndoDeleteSet = {},
                    onReorderSets = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Manage set 1").performClick()
        compose.onNodeWithText("Mark Main Set Not Performed").performClick()
        compose.runOnIdle { error.value = "Set changed after review" }
        compose.onNodeWithText("Set changed after review").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle {
            assertEquals(2, clearCount)
            assertEquals(null, error.value)
        }
        compose.onNodeWithContentDescription("Manage set 1").performClick()
        compose.onNodeWithText("Mark Main Set Not Performed").performClick()
        compose.onAllNodesWithText("Set changed after review").assertCountEquals(0)
    }

    @Test
    fun groupedExerciseUsesOneClearHeaderAndCanRemoveItsDesignation() {
        val exercise = testExercise().copy(name = "Bench press")
        val group = WorkoutGroup(
            id = 20,
            uuid = "upper-superset",
            sessionId = 3,
            name = "Upper A",
            type = WorkoutGroupType.Superset,
            position = 0,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        val workoutExercise = testWorkoutExercise(exercise).copy(groupId = group.id)
        var removalRequested = false
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutExerciseGroupSurface(
                    group = group,
                    exerciseCount = 2,
                    canMoveUp = false,
                    canMoveDown = true,
                    onMoveUp = {},
                    onMoveDown = {},
                ) {
                    WorkoutExerciseCard(
                        item = WorkoutExerciseUi(workoutExercise, exercise, emptyList(), emptyList(), 0, group, null),
                        preferredWeightUnitId = "kilogram",
                        preferredDistanceUnitId = "kilometre",
                        numberPrecision = 1,
                        showRpe = false,
                        showRir = false,
                        nextSetId = null,
                        nextInGroup = false,
                        canMoveUp = false,
                        canMoveDown = false,
                        onMoveUp = {},
                        onMoveDown = {},
                        onRemoveExercise = {},
                        onRemoveFromGroup = { removalRequested = true },
                        onSubstituteExercise = {},
                        onAddSet = {},
                        onEditSet = {},
                        onEditNotes = {},
                        onCompleteSet = { _, _ -> },
                        onSaveQuickSet = { _, _, _ -> },
                        onDuplicateSet = {},
                        onDeleteSet = {},
                        onUndoDeleteSet = {},
                        onReorderSets = {},
                    )
                }
            }
        }

        compose.onNodeWithText("Superset").assertIsDisplayed()
        compose.onNodeWithText("Upper A · 2 exercises").assertIsDisplayed()
        compose.onNodeWithContentDescription("Superset group, 2 exercises").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Reorder Superset group").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Reorder Bench press").assertCountEquals(0)
        compose.onNodeWithContentDescription("More options for Bench press").performClick()
        compose.onNodeWithText("Remove from Superset").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(removalRequested) }
    }

    @Test
    fun genericGroupNamesNeverRepeatOrConflictWithTheSelectedType() {
        val group = WorkoutGroup(
            id = 20,
            uuid = "circuit",
            sessionId = 3,
            name = "Superset",
            type = WorkoutGroupType.Circuit,
            position = 0,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        assertEquals(null, group.customDisplayName())

        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutExerciseGroupSurface(
                    group = group,
                    exerciseCount = 3,
                    canMoveUp = false,
                    canMoveDown = false,
                    onMoveUp = {},
                    onMoveDown = {},
                ) {}
            }
        }

        compose.onNodeWithText("Circuit").assertIsDisplayed()
        compose.onNodeWithText("3 exercises").assertIsDisplayed()
        compose.onAllNodesWithText("Superset").assertCountEquals(0)
    }

    @Test
    fun groupedWorkoutExercisesMoveAsABlockAndReorderWithinTheirGroup() {
        val group = WorkoutGroup(
            id = 20,
            uuid = "circuit",
            sessionId = 3,
            name = "Circuit",
            type = WorkoutGroupType.Circuit,
            position = 0,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        fun item(id: Long, position: Int, grouped: Boolean): WorkoutExerciseUi {
            val exercise = testExercise().copy(id = id, uuid = "exercise-$id", name = "Exercise $id")
            val placement = testWorkoutExercise(exercise).copy(
                id = id,
                uuid = "placement-$id",
                position = position,
                groupId = group.id.takeIf { grouped },
            )
            return WorkoutExerciseUi(
                placement,
                exercise,
                emptyList(),
                emptyList(),
                0,
                group.takeIf { grouped },
                null,
            )
        }
        val firstMember = item(10, 0, grouped = true)
        val independent = item(11, 1, grouped = false)
        val secondMember = item(12, 2, grouped = true)

        val blocks = buildWorkoutExerciseBlocks(listOf(firstMember, independent, secondMember))

        assertEquals(2, blocks.size)
        assertEquals(listOf(10L, 12L), blocks.first().exercises.map { it.workoutExercise.id })
        assertEquals(listOf(11L, 10L, 12L), reorderWorkoutBlock(blocks, 0, 1))
        assertEquals(listOf(12L, 10L, 11L), reorderWorkoutGroupMember(blocks, 0, 0, 1))
    }

    @Test
    fun completedSetsStayQuietUntilTheUserExpandsThem() {
        val exercise = testExercise().copy(name = "Bench press")
        val workoutExercise = testWorkoutExercise(exercise)
        val completed = testWorkoutSet(4, workoutExercise.id).copy(completed = true, completedAtMillis = 2)
        val next = testWorkoutSet(5, workoutExercise.id).copy(position = 1)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                WorkoutExerciseCard(
                    item = WorkoutExerciseUi(workoutExercise, exercise, listOf(completed, next), emptyList(), 0, null, null),
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    numberPrecision = 1,
                    showRpe = false,
                    showRir = false,
                    nextSetId = next.id,
                    nextInGroup = false,
                    canMoveUp = false,
                    canMoveDown = false,
                    onMoveUp = {},
                    onMoveDown = {},
                    onRemoveExercise = {},
                    onSubstituteExercise = {},
                    onAddSet = {},
                    onEditSet = {},
                    onEditNotes = {},
                    onCompleteSet = { _, _ -> },
                    onSaveQuickSet = { _, _, _ -> },
                    onDuplicateSet = {},
                    onDeleteSet = {},
                    onUndoDeleteSet = {},
                    onReorderSets = {},
                )
            }
        }

        compose.onNodeWithText("1 Completed Set").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Reorder set 1").assertCountEquals(0)
        compose.onNodeWithTag("active-set-composer").assertIsDisplayed()
        compose.onNodeWithText("1 Completed Set").performClick()
        compose.onNodeWithText("Set 1", substring = true).assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Reorder set 1").assertCountEquals(0)
        compose.onNodeWithContentDescription("Manage set 1").assertIsDisplayed()
    }

    @Test
    fun pristineQuickSetDefersRequiredErrorsUntilCompletionAttempt() {
        val exercise = testExercise()
        val workoutExercise = testWorkoutExercise(exercise)
        val set = testWorkoutSet(4, workoutExercise.id).copy(
            canonicalWeightKg = null,
            enteredWeight = null,
            repetitions = null,
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                QuickSetEntry(
                    set = set,
                    exercise = exercise,
                    workoutExercise = workoutExercise,
                    machine = null,
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    showRpe = false,
                    showRir = false,
                    onMoreDetails = {},
                    onSave = { _, _, _ -> },
                )
            }
        }

        compose.onAllNodes(hasText("Required")).assertCountEquals(0)
        compose.onAllNodes(hasText("Enter at least 1")).assertCountEquals(0)
        compose.onNodeWithTag("quick-set-save-next-${set.id}").performClick()
        compose.onNodeWithText("Required").assertIsDisplayed()
        compose.onNodeWithText("Enter at least 1").assertIsDisplayed()
    }

    @Test
    fun previousSetSuggestionPopulatesPrimaryInputs() {
        val exercise = testExercise()
        val workoutExercise = testWorkoutExercise(exercise)
        val set = testWorkoutSet(4, workoutExercise.id).copy(
            canonicalWeightKg = null,
            enteredWeight = null,
            repetitions = null,
        )
        val previous = testWorkoutSet(3, workoutExercise.id).copy(
            canonicalWeightKg = 55.0,
            enteredWeight = 55.0,
            repetitions = 8,
            completed = true,
        )
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                QuickSetEntry(
                    set = set,
                    exercise = exercise,
                    workoutExercise = workoutExercise,
                    machine = null,
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    showRpe = false,
                    showRir = false,
                    suggestedSet = previous,
                    onMoreDetails = {},
                    onSave = { _, _, _ -> },
                )
            }
        }

        compose.onNodeWithTag("quick-set-use-last-${set.id}").performClick()
        compose.onNodeWithTag("quick-set-load-${set.id}").assertTextContains("55")
        compose.onNodeWithTag("quick-set-reps-${set.id}").assertTextContains("8")
    }

    @Test
    fun quickSetKeepsItsDraftAndBoundaryThroughStateRestoration() {
        val restoration = StateRestorationTester(compose)
        val exercise = testExercise()
        val workoutExercise = testWorkoutExercise(exercise)
        val opened = testWorkoutSet(4, workoutExercise.id).copy(
            uuid = "set-opened",
            updatedAtMillis = 40,
        )
        var displayedSet by mutableStateOf(opened)
        var committedBoundary: QuickSetAuthorshipBoundary? = null
        var committedDraft: WorkoutSetDraft? = null
        restoration.setContent {
            WhipTheme(dynamicColor = false) {
                QuickSetEntry(
                    set = displayedSet,
                    exercise = exercise,
                    workoutExercise = workoutExercise,
                    machine = null,
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    showRpe = false,
                    showRir = false,
                    onMoreDetails = {},
                    onSave = { boundary, draft, _ ->
                        committedBoundary = boundary
                        committedDraft = draft
                    },
                )
            }
        }

        compose.onNodeWithTag("quick-set-load-${opened.id}").performTextReplacement("62.5")
        compose.onNodeWithTag("quick-set-reps-${opened.id}").performTextReplacement("8")
        compose.runOnIdle {
            displayedSet = opened.copy(
                uuid = "set-newer-details-save",
                updatedAtMillis = 41,
                canonicalWeightKg = 100.0,
                enteredWeight = 100.0,
                repetitions = 1,
            )
        }
        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag("quick-set-load-${opened.id}").assertTextContains("62.5")
        compose.onNodeWithTag("quick-set-reps-${opened.id}").assertTextContains("8")
        compose.onNodeWithTag("quick-set-save-next-${opened.id}").performClick()
        compose.runOnIdle {
            val boundary = requireNotNull(committedBoundary)
            assertEquals("set-opened", boundary.setUuid)
            assertEquals(40, boundary.setUpdatedAtMillis)
            assertEquals(workoutExercise.uuid, boundary.workoutExerciseUuid)
            assertEquals(workoutExercise.updatedAtMillis, boundary.workoutExerciseUpdatedAtMillis)
            val draft = requireNotNull(committedDraft)
            assertEquals(62.5, draft.weight ?: error("Missing restored weight"), 0.0)
            assertEquals(8, draft.reps)
        }
    }

    @Test
    fun quickSetShowsOnlyOneEffortScaleWhenConnectedPreferencesEnableBoth() {
        val exercise = testExercise()
        val workoutExercise = testWorkoutExercise(exercise)
        val set = testWorkoutSet(4, workoutExercise.id)
        compose.setContent {
            WhipTheme(dynamicColor = false) {
                QuickSetEntry(
                    set = set,
                    exercise = exercise,
                    workoutExercise = workoutExercise,
                    machine = null,
                    preferredWeightUnitId = "kilogram",
                    preferredDistanceUnitId = "kilometre",
                    showRpe = true,
                    showRir = true,
                    onMoreDetails = {},
                    onSave = { _, _, _ -> },
                )
            }
        }

        compose.onNodeWithText("RPE (1–10)").assertIsDisplayed()
        compose.onAllNodes(hasText("RIR (0–10)")).assertCountEquals(0)
    }

    @Test
    fun workoutHistoryCardUsesOneStableActionPerExerciseAndMovesSecondaryActionsToMenu() {
        val exercises = listOf(
            testExercise().copy(id = 1, name = "Flat Barbell Bench Press"),
            testExercise().copy(id = 2, name = "Lat Pulldown"),
            testExercise().copy(id = 3, name = "Dumbbell Bicep Curl"),
        )
        val placements = exercises.mapIndexed { index, exercise ->
            testWorkoutExercise(exercise).copy(
                id = (index + 10).toLong(),
                exerciseId = exercise.id,
                position = index,
                machineNameSnapshot = "Rack A".takeIf { index == 0 }.orEmpty(),
                machineConfigurationSnapshot = "Safety 8 · Bench 3".takeIf { index == 0 }.orEmpty(),
            )
        }
        val sets = placements.flatMapIndexed { index, placement ->
            List(index + 1) { setIndex ->
                testWorkoutSet((index * 10 + setIndex + 1).toLong(), placement.id).copy(
                    completed = true,
                    classification = if (index == 0) WorkoutSetClassification.Amrap else WorkoutSetClassification.Working,
                    enteredWeight = if (index == 0) 85.0 else 50.0,
                    canonicalWeightKg = if (index == 0) 85.0 else 50.0,
                    repetitions = if (index == 0) 7 else 5,
                    rpe = 9.0.takeIf { index == 0 },
                    rir = 1.0.takeIf { index == 0 },
                    tempo = "3-1-1".takeIf { index == 0 }.orEmpty(),
                    restSeconds = if (index == 0) 180 else 120,
                    note = "Strong set".takeIf { index == 0 }.orEmpty(),
                    prescribedEnteredWeight = 82.5.takeIf { index == 0 },
                    prescribedWeightUnitId = "kilogram".takeIf { index == 0 },
                    prescribedRepetitions = 5.takeIf { index == 0 },
                    prescribedRepetitionsMax = 5.takeIf { index == 0 },
                    prescriptionSourceLabel = "85% TM".takeIf { index == 0 }.orEmpty(),
                )
            }
        }
        val session = testHistorySession().copy(
            sourceRoutineProgramKind = RoutineProgramKind.FiveThreeOne,
            sourceRoutinePhaseIndex = 2,
            sourceRoutinePhaseLabel = "Anchor 1",
            sourceRoutinePhaseRole = RoutineProgramPhaseRole.Anchor,
            sourceRoutineCycle = 3,
            sourceRoutineDayPosition = 1,
            sourceRoutineDayProgressionIndex = 4,
            programProgressAdvanced = false,
        )
        compose.setContent {
            var expanded by remember { mutableStateOf(false) }
            var menuExpanded by remember { mutableStateOf(false) }
            WhipTheme(darkTheme = true, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    WorkoutHistoryCard(
                        session = session,
                        workoutExercises = placements,
                        sets = sets,
                        exerciseById = exercises.associateBy(Exercise::id),
                        expanded = expanded,
                        archivedView = false,
                        hasActiveWorkout = false,
                        menuExpanded = menuExpanded,
                        onToggleExpanded = { expanded = !expanded },
                        onMenuExpandedChange = { menuExpanded = it },
                        onRepeatWorkout = {},
                        onOpenActiveWorkout = {},
                        onEditDetails = {},
                        onResume = {},
                        onSaveAsRoutine = {},
                        onShare = {},
                        onRestore = {},
                        onDelete = {},
                        onReuseExercise = {},
                    )
                }
            }
        }

        compose.onNodeWithTag("history-workout-preview-${session.id}").assertIsDisplayed()
        compose.onNodeWithText("Flat Barbell Bench Press · Lat Pulldown · Dumbbell Bicep Curl").assertIsDisplayed()
        compose.onAllNodesWithText("View Details").assertCountEquals(0)
        compose.onAllNodesWithText("Use Again").assertCountEquals(0)

        compose.onNodeWithTag("history-workout-toggle-${session.id}").performClick()
        compose.onNodeWithTag("history-program-snapshot-${session.id}")
            .assertTextContains(
                "Program snapshot · 5/3/1 · Cycle 3 · Anchor 1 · Anchor · Day 2 · Day progression 5 · Did not advance program progress",
            )
        compose.onAllNodesWithText("Use Again").assertCountEquals(3)
        compose.onNodeWithText("Repeat Workout").assertExists()
        compose.onNodeWithText("Equipment: Rack A", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Setup: Safety 8 · Bench 3").assertIsDisplayed()
        compose.onNodeWithTag("history-set-identity-${sets.first().id}")
            .assertTextContains("Set 1")
        compose.onNodeWithTag("history-set-performed-${sets.first().id}")
            .assertTextContains("85 kg × 7 reps")
        compose.onNodeWithTag("history-set-status-${sets.first().id}")
            .assertTextContains("AMRAP · Performed · RPE 9 · RIR 1")
        compose.onNodeWithTag("history-set-target-${sets.first().id}")
            .assertTextContains("85% TM", substring = true)
            .assertTextContains("82.5 kg entered", substring = true)
            .assertTextContains("5+ reps", substring = true)
        compose.onNodeWithTag("history-set-details-${sets.first().id}")
            .assertTextContains("3:00 rest · Tempo 3-1-1")
        compose.onNodeWithText("Note · Strong set").assertIsDisplayed()
        compose.onAllNodesWithText("Copy Flat Barbell Bench Press Sets").assertCountEquals(0)
        compose.onAllNodesWithText("Edit Details").assertCountEquals(0)
        compose.onAllNodesWithText("Resume Workout").assertCountEquals(0)
        placements.forEach { placement ->
            val row = compose.onNodeWithTag("history-exercise-row-${placement.id}").fetchSemanticsNode().boundsInRoot
            val action = compose.onNodeWithTag("history-exercise-reuse-${placement.id}").fetchSemanticsNode().boundsInRoot
            check(action.left >= row.left && action.right <= row.right && action.top >= row.top && action.bottom <= row.bottom) {
                "Exercise reuse action must remain inside its row: row=$row action=$action"
            }
        }

        compose.onNodeWithTag("history-workout-menu-${session.id}").performClick()
        compose.onNodeWithText("Edit Details").assertIsDisplayed()
        compose.onNodeWithText("Resume Original Workout").assertIsDisplayed()
        compose.onNodeWithText("Save as Routine").assertIsDisplayed()
        captureVisualCatalogSurface("gym.workout-history.menu")
        assertEquals(
            "Program snapshot · 5/3/1 · Cycle 3 · Anchor 1 · Anchor · Day 2 · Day progression 5 · Did not advance program progress",
            workoutProgramSnapshotLabel(session),
        )
    }

    @Test
    fun historicalSetCardSharesActiveHierarchyAndInsetsAtLargeText() {
        val exercise = testExercise().copy(id = 1, name = "Flat Barbell Bench Press")
        val placement = testWorkoutExercise(exercise).copy(
            id = 10,
            exerciseId = exercise.id,
            placementKindSnapshot = RoutinePlacementKind.MainExercise,
        )
        val set = testWorkoutSet(1, placement.id).copy(
            completed = true,
            classification = WorkoutSetClassification.Amrap,
            enteredWeight = 85.0,
            canonicalWeightKg = 85.0,
            repetitions = 7,
            rpe = 9.0,
            rir = 1.0,
            restSeconds = 180,
            tempo = "3-1-1",
            note = "Strong set",
            workSectionSnapshot = RoutineWorkSection.Main,
            prescribedEnteredWeight = 82.5,
            prescribedWeightUnitId = "kilogram",
            prescribedRepetitions = 5,
            prescribedRepetitionsMax = 5,
            prescriptionSourceLabel = "85% of explicit training max",
        )
        val session = testHistorySession()
        var responsiveReview by mutableStateOf(true)
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(
                    compose.density.density,
                    fontScale = if (responsiveReview) 2f else 1f,
                ),
            ) {
                WhipTheme(darkTheme = true, dynamicColor = false) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        androidx.compose.foundation.layout.Box(
                            if (responsiveReview) Modifier.width(320.dp) else Modifier.fillMaxSize(),
                        ) {
                            WorkoutHistoryCard(
                                session = session,
                                workoutExercises = listOf(placement),
                                sets = listOf(set),
                                exerciseById = mapOf(exercise.id to exercise),
                                expanded = true,
                                archivedView = true,
                                hasActiveWorkout = false,
                                menuExpanded = false,
                                onToggleExpanded = {},
                                onMenuExpandedChange = {},
                                onRepeatWorkout = {},
                                onOpenActiveWorkout = {},
                                onEditDetails = {},
                                onResume = {},
                                onSaveAsRoutine = {},
                                onShare = {},
                                onRestore = {},
                                onDelete = {},
                                onReuseExercise = {},
                            )
                        }
                    }
                }
            }
        }

        val card = compose.onNodeWithTag("history-set-card-${set.id}").assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val identity = compose.onNodeWithTag("history-set-identity-${set.id}", useUnmergedTree = true)
            .assertTextContains("Main · Set 1").fetchSemanticsNode().boundsInRoot
        val values = compose.onNodeWithTag("history-set-performed-${set.id}", useUnmergedTree = true)
            .assertTextContains("85 kg × 7 reps").fetchSemanticsNode().boundsInRoot
        val status = compose.onNodeWithTag("history-set-status-${set.id}", useUnmergedTree = true)
            .assertTextContains("AMRAP · Performed · RPE 9 · RIR 1").fetchSemanticsNode().boundsInRoot
        val target = compose.onNodeWithTag("history-set-target-${set.id}", useUnmergedTree = true)
            .assertTextContains("explicit training max", substring = true).fetchSemanticsNode().boundsInRoot
        val details = compose.onNodeWithTag("history-set-details-${set.id}", useUnmergedTree = true)
            .assertTextContains("3:00 rest · Tempo 3-1-1").fetchSemanticsNode().boundsInRoot
        val note = compose.onNodeWithTag("history-set-note-${set.id}", useUnmergedTree = true)
            .assertTextContains("Note · Strong set").fetchSemanticsNode().boundsInRoot
        check(
            identity.bottom <= values.top &&
                values.bottom <= status.top &&
                status.bottom <= target.top &&
                target.bottom <= details.top &&
                details.bottom <= note.top,
        ) {
            "Historical Set hierarchy must match the active Set core at 200% text: " +
                "identity=$identity values=$values status=$status target=$target details=$details note=$note"
        }
        val leadingInsetDp = (identity.left - card.left) / compose.density.density
        check(leadingInsetDp in 11f..13f) {
            "Historical Set must use the shared 12 dp leading inset: card=$card identity=$identity inset=$leadingInsetDp"
        }

        compose.runOnIdle { responsiveReview = false }
        compose.waitForIdle()
        captureVisualCatalogSurface("gym.history.expanded")
    }

    @Test
    fun workoutHistoryExplainsReplacementAndUnperformedSetOutcome() {
        val bench = testExercise().copy(id = 1, name = "Back Squat")
        val zercher = testExercise().copy(id = 2, name = "Zercher Squat")
        val replacement = testWorkoutExercise(zercher).copy(id = 12, uuid = "replacement", exerciseId = 2)
        val original = testWorkoutExercise(bench).copy(
            id = 11,
            uuid = "original",
            exerciseId = 1,
            outcome = WorkoutExerciseOutcome.Substituted,
            replacementWorkoutExerciseUuid = replacement.uuid,
        )
        val completed = testWorkoutSet(21, original.id).copy(completed = true, repetitions = 5)
        val unperformed = testWorkoutSet(22, original.id).copy(
            deletedAtMillis = 10,
            removalReason = WorkoutSetRemovalReason.ExerciseSubstituted,
        )
        val session = testHistorySession()
        compose.setContent {
            WhipTheme(darkTheme = true, dynamicColor = false) {
                WorkoutHistoryCard(
                    session = session,
                    workoutExercises = listOf(original, replacement),
                    sets = listOf(completed, unperformed),
                    exerciseById = listOf(bench, zercher).associateBy(Exercise::id),
                    expanded = true,
                    archivedView = false,
                    hasActiveWorkout = false,
                    menuExpanded = false,
                    onToggleExpanded = {},
                    onMenuExpandedChange = {},
                    onRepeatWorkout = {},
                    onOpenActiveWorkout = {},
                    onEditDetails = {},
                    onResume = {},
                    onSaveAsRoutine = {},
                    onShare = {},
                    onRestore = {},
                    onDelete = {},
                    onReuseExercise = {},
                )
            }
        }

        compose.onNodeWithText("Replaced by Zercher Squat during this workout").assertIsDisplayed()
        compose.onNodeWithText("Not performed · exercise replaced", substring = true).assertIsDisplayed()
    }

    private fun testHistorySession() = WorkoutSession(
        id = 50,
        uuid = "history-session",
        name = "Upper Body",
        notes = "Controlled reps",
        startedAt = Instant.parse("2026-08-26T22:00:00Z"),
        endedAt = Instant.parse("2026-08-26T22:47:00Z"),
        localDate = LocalDate.of(2026, 8, 26),
        zoneId = "UTC",
        state = WorkoutSessionState.Finished,
        keepScreenAwake = false,
        restTimerDeadlineMillis = null,
        restTimerDurationSeconds = null,
        archived = false,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun testWorkoutExercise(exercise: Exercise) = WorkoutExercise(
        id = 2,
        uuid = "workout-exercise",
        sessionId = 3,
        exerciseId = exercise.id,
        position = 0,
        notes = "",
        groupId = null,
        createdAtMillis = 1,
        updatedAtMillis = 1,
        loadInterpretationSnapshot = LoadInterpretation.Total,
        trackingTypeSnapshot = ExerciseTrackingType.WeightReps,
        exerciseWeightUnitSnapshot = "kilogram",
    )

    private fun testWorkoutSet(id: Long, workoutExerciseId: Long) = WorkoutSet(
        id = id,
        uuid = "set-$id",
        workoutExerciseId = workoutExerciseId,
        position = 0,
        classification = WorkoutSetClassification.Working,
        planned = false,
        completed = false,
        canonicalWeightKg = 50.0,
        enteredWeight = 50.0,
        enteredWeightUnitId = "kilogram",
        repetitions = 5,
        canonicalDistanceMetres = null,
        enteredDistance = null,
        enteredDistanceUnitId = null,
        durationSeconds = null,
        bodyweightKg = null,
        note = "",
        rpe = null,
        rir = null,
        tempo = "",
        restSeconds = 120,
        completedAtMillis = null,
        deletedAtMillis = null,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun testExercise() = Exercise(
        id = 1,
        uuid = "exercise",
        name = "Cable row",
        trackingType = ExerciseTrackingType.WeightReps,
        notes = "",
        equipment = "",
        primaryMuscles = "",
        secondaryMuscles = "",
        weightUnitId = "kilogram",
        weightIncrement = 2.5,
        repetitionIncrement = 1,
        defaultRestSeconds = 120,
        defaultGraphMetric = "EstimatedOneRepMax",
        oneRepMaxFormula = EstimatedOneRepMaxFormula.Epley,
        barWeightKg = 20.0,
        availablePlatesKg = emptyList(),
        includeInVolume = true,
        includeInPersonalRecords = true,
        bodyweightLoadPolicy = BodyweightLoadPolicy.ExternalWeightOnly,
        effectiveBodyweightPercent = 100.0,
        showRpe = null,
        showRir = null,
        showTempo = null,
        favorite = false,
        position = 0,
        archived = false,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )

    private fun exerciseDeletionImpact(
        activePlacements: Int = 0,
        trainingMaxDecisionCount: Int = 0,
    ) = ExerciseDeletionImpact(
        exerciseId = 7,
        displayName = "Zercher squat",
        activePlacements = activePlacements,
        routinePlacementCount = 2,
        routineSetCount = 16,
        routineAlternativeReferenceCount = 1,
        workoutPlacementCount = 9,
        workoutSetCount = 43,
        graphPresetUpdateCount = 1,
        graphPresetDeleteCount = 0,
        personalRecordCount = 2,
        trainingMaxDecisionCount = trainingMaxDecisionCount,
        machineReferenceCount = 1,
        categoryReferenceCount = 2,
        revisionToken = "exercise-revision",
    )
}
