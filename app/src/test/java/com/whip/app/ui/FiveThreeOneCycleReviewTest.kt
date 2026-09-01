package com.whip.app.ui

import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.BodyweightLoadPolicy
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.GymRoutine
import com.whip.app.domain.RoutineDay
import com.whip.app.domain.RoutineExercise
import com.whip.app.domain.RoutinePlacementKind
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineProgramPhaseRole
import com.whip.app.domain.RoutineProgressionMode
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.RoutineOptionalWorkKind
import com.whip.app.domain.WorkoutExercise
import com.whip.app.domain.WorkoutSession
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.WorkoutSet
import com.whip.app.domain.WorkoutSetClassification
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FiveThreeOneCycleReviewTest {
    @Test
    fun oneStrongPrSetAndJokerStillRecommendStandard() {
        val session = session(10, WorkoutSessionState.Active)
        val main = set(101, 1001, WorkoutSetClassification.Amrap, RoutineWorkSection.Main, reps = 10)
        val joker = set(102, 1001, WorkoutSetClassification.Working, RoutineWorkSection.Optional, reps = 3)
        val state = state(
            sessions = listOf(session),
            workoutExercises = listOf(workoutExercise(1001, session.id)),
            sets = listOf(main, joker),
            activeSets = listOf(main, joker),
            allowHigher = true,
        )

        val review = requireNotNull(state.activeFiveThreeOneCycleReview())
        assertEquals("StandardIncrease", review.lifts.single().recommendation.category.name)
        assertEquals(10.0, review.lifts.single().recommendation.suggestedDelta, 0.0)
    }

    @Test
    fun repeatedStrongPrSetsMayExposeExplicitlyEnabledNonStandardHigherOption() {
        val earlier = session(9, WorkoutSessionState.Finished)
        val active = session(10, WorkoutSessionState.Active)
        val earlierMain = set(91, 901, WorkoutSetClassification.Amrap, RoutineWorkSection.Main, reps = 10)
        val currentMain = set(101, 1001, WorkoutSetClassification.Amrap, RoutineWorkSection.Main, reps = 9)
        val joker = set(102, 1001, WorkoutSetClassification.Working, RoutineWorkSection.Optional, reps = 3)
        val state = state(
            sessions = listOf(earlier, active),
            workoutExercises = listOf(workoutExercise(901, earlier.id), workoutExercise(1001, active.id)),
            sets = listOf(earlierMain, currentMain, joker),
            activeSets = listOf(currentMain, joker),
            allowHigher = true,
        )

        val lift = requireNotNull(state.activeFiveThreeOneCycleReview()).lifts.single()
        assertEquals("CautiousHigherIncrease", lift.recommendation.category.name)
        assertEquals(15.0, lift.recommendation.suggestedDelta, 0.0)
        assertTrue(lift.eligible)
    }

    @Test
    fun evidenceFromDifferentProgramKindIsExcludedFromCycleReview() {
        val mismatchedEarlier = session(9, WorkoutSessionState.Finished).copy(
            sourceRoutineProgramKind = RoutineProgramKind.FiveSPro,
        )
        val active = session(10, WorkoutSessionState.Active)
        val earlierMain = set(91, 901, WorkoutSetClassification.Amrap, RoutineWorkSection.Main, reps = 10)
        val currentMain = set(101, 1001, WorkoutSetClassification.Amrap, RoutineWorkSection.Main, reps = 9)
        val joker = set(102, 1001, WorkoutSetClassification.Working, RoutineWorkSection.Optional, reps = 3)
        val review = requireNotNull(
            state(
                sessions = listOf(mismatchedEarlier, active),
                workoutExercises = listOf(workoutExercise(901, mismatchedEarlier.id), workoutExercise(1001, active.id)),
                sets = listOf(earlierMain, currentMain, joker),
                activeSets = listOf(currentMain, joker),
                allowHigher = true,
            ).activeFiveThreeOneCycleReview(),
        )

        assertEquals("StandardIncrease", review.lifts.single().recommendation.category.name)
    }

    @Test
    fun activeSessionMustMatchCurrentRoutineProgramKind() {
        val mismatched = session(10, WorkoutSessionState.Active).copy(
            sourceRoutineProgramKind = RoutineProgramKind.FiveSPro,
        )
        val main = set(101, 1001, WorkoutSetClassification.Amrap, RoutineWorkSection.Main, reps = 9)
        val state = state(
            sessions = listOf(mismatched),
            workoutExercises = listOf(workoutExercise(1001, mismatched.id)),
            sets = listOf(main),
            activeSets = listOf(main),
            allowHigher = true,
        )

        assertEquals(null, state.activeFiveThreeOneCycleReview())
    }

    @Test
    fun underTargetMainWorkHoldsOnlyThatLift() {
        val active = session(10, WorkoutSessionState.Active)
        val missed = set(101, 1001, WorkoutSetClassification.Working, RoutineWorkSection.Main, reps = 4)
        val state = state(
            sessions = listOf(active),
            workoutExercises = listOf(workoutExercise(1001, active.id)),
            sets = listOf(missed),
            activeSets = listOf(missed),
            allowHigher = false,
        )

        val lift = requireNotNull(state.activeFiveThreeOneCycleReview()).lifts.single()
        assertFalse(lift.eligible)
        assertEquals("Hold", lift.recommendation.category.name)
    }

    @Test
    fun earlierDayLiftRemainsEligibleAtFinalDayReview() {
        val earlier = session(9, WorkoutSessionState.Finished).copy(sourceRoutineDayPosition = 0)
        val active = session(10, WorkoutSessionState.Active).copy(sourceRoutineDayPosition = 1)
        val earlierPlacement = workoutExercise(901, earlier.id)
        val activePlacement = workoutExercise(1001, active.id).copy(exerciseId = 8)
        val earlierMain = set(91, earlierPlacement.id, WorkoutSetClassification.Amrap, RoutineWorkSection.Main, reps = 8)
        val activeMain = set(101, activePlacement.id, WorkoutSetClassification.Amrap, RoutineWorkSection.Main, reps = 8)
        val base = state(
            sessions = listOf(earlier, active),
            workoutExercises = listOf(earlierPlacement, activePlacement),
            sets = listOf(earlierMain, activeMain),
            activeSets = listOf(activeMain),
            allowHigher = false,
        )
        val bench = base.exercises.single().copy(id = 8, uuid = "exercise-8", name = "Bench Press")
        val reviewState = base.copy(
            exercises = base.exercises + bench,
            activeWorkoutExercises = listOf(
                base.activeWorkoutExercises.single().copy(
                    workoutExercise = activePlacement,
                    exercise = bench,
                ),
            ),
            routineDays = listOf(
                RoutineDay(2, "day-2", 1, "Zercher", 0, 1, 1),
                RoutineDay(4, "day-4", 1, "Bench", 1, 1, 1),
            ),
            routineExercises = listOf(
                base.routineExercises.single(),
                base.routineExercises.single().copy(
                    id = 5,
                    uuid = "routine-exercise-5",
                    routineDayId = 4,
                    exerciseId = 8,
                    trainingMaxValue = 200.0,
                    trainingMaxKg = 200.0,
                    cycleIncrementValue = 5.0,
                ),
            ),
        )

        val lifts = requireNotNull(reviewState.activeFiveThreeOneCycleReview()).lifts.associateBy { it.exerciseId }
        assertTrue(requireNotNull(lifts[7]).eligible)
        assertTrue(requireNotNull(lifts[8]).eligible)
    }

    @Test
    fun legacyUnknownInvalidationConservativelyHoldsLift() {
        val active = session(10, WorkoutSessionState.Active).copy(
            requiredMainWorkInvalidated = true,
            invalidatedMainExerciseIds = emptySet(),
        )
        val main = set(101, 1001, WorkoutSetClassification.Amrap, RoutineWorkSection.Main, reps = 8)
        val review = state(
            sessions = listOf(active),
            workoutExercises = listOf(workoutExercise(1001, active.id)),
            sets = listOf(main),
            activeSets = listOf(main),
            allowHigher = false,
        ).activeFiveThreeOneCycleReview()

        assertFalse(requireNotNull(review).lifts.single().eligible)
    }

    @Test
    fun onlyExplicitValidTrainingMaxTestSetCanTriggerTestDecreaseReview() {
        val active = session(10, WorkoutSessionState.Active).copy(
            sourceRoutinePhaseRole = RoutineProgramPhaseRole.TrainingMaxTest,
        )
        val placement = workoutExercise(1001, active.id)
        val buildUpMiss = set(
            101,
            placement.id,
            WorkoutSetClassification.Working,
            RoutineWorkSection.Main,
            reps = 4,
        )
        val validTest = set(
            102,
            placement.id,
            WorkoutSetClassification.TrainingMaxTest,
            RoutineWorkSection.Main,
            reps = 5,
        ).copy(
            canonicalWeightKg = 300.0,
            enteredWeight = 300.0,
            prescribedCanonicalWeightKg = 300.0,
            prescribedEnteredWeight = 300.0,
        )
        fun recommendation(vararg evidence: WorkoutSet) = requireNotNull(
            state(
                sessions = listOf(active),
                workoutExercises = listOf(placement),
                sets = evidence.toList(),
                activeSets = evidence.toList(),
                allowHigher = false,
            ).activeFiveThreeOneCycleReview(),
        ).lifts.single().recommendation.category

        assertEquals(
            "Hold",
            recommendation(buildUpMiss, validTest).name,
        )
        assertEquals(
            "InsufficientEvidence",
            recommendation(validTest.copy(
                canonicalWeightKg = 285.0,
                enteredWeight = 285.0,
                prescribedCanonicalWeightKg = 285.0,
                prescribedEnteredWeight = 285.0,
            )).name,
        )
        assertEquals(
            "DecreaseReview",
            recommendation(validTest.copy(repetitions = 2)).name,
        )
    }

    private fun state(
        sessions: List<WorkoutSession>,
        workoutExercises: List<WorkoutExercise>,
        sets: List<WorkoutSet>,
        activeSets: List<WorkoutSet>,
        allowHigher: Boolean,
    ): GymUiState {
        val active = sessions.single { it.state == WorkoutSessionState.Active }
        val exercise = Exercise(
            id = 7,
            uuid = "exercise-7",
            name = "Zercher Squat",
            trackingType = ExerciseTrackingType.WeightReps,
            notes = "",
            equipment = "Barbell",
            primaryMuscles = "Legs",
            secondaryMuscles = "Core",
            weightUnitId = "kilogram",
            weightIncrement = 2.5,
            repetitionIncrement = 1,
            defaultRestSeconds = 180,
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
        val activePlacement = workoutExercises.single { it.sessionId == active.id }
        return GymUiState(
            exercises = listOf(exercise),
            activeSession = active,
            activeWorkoutExercises = listOf(
                WorkoutExerciseUi(activePlacement, exercise, activeSets, emptyList(), 0, null, null),
            ),
            allSessions = sessions,
            allWorkoutExercises = workoutExercises,
            allSets = sets,
            routines = listOf(
                GymRoutine(
                    id = 1,
                    uuid = "routine-1",
                    name = "Custom 5/3/1",
                    notes = "",
                    position = 0,
                    archived = false,
                    pinned = false,
                    createdAtMillis = 1,
                    updatedAtMillis = 1,
                    programKind = RoutineProgramKind.FiveThreeOne,
                    programPhaseCount = 1,
                    trainingMaxAdvanceAfterPhaseIndices = setOf(0),
                    progressionMode = RoutineProgressionMode.PerformanceInformed,
                    allowNonStandardHigherSuggestions = allowHigher,
                ),
            ),
            routineDays = listOf(RoutineDay(2, "day-2", 1, "Zercher", 0, 1, 1)),
            routineExercises = listOf(
                RoutineExercise(
                    id = 3,
                    uuid = "routine-exercise-3",
                    routineDayId = 2,
                    exerciseId = 7,
                    position = 0,
                    notes = "",
                    groupKey = null,
                    copyPreviousWorkout = false,
                    createdAtMillis = 1,
                    updatedAtMillis = 1,
                    trainingMaxValue = 300.0,
                    trainingMaxKg = 300.0,
                    trainingMaxUnitId = "kilogram",
                    cycleIncrementValue = 10.0,
                    placementKind = RoutinePlacementKind.MainLift,
                ),
            ),
        )
    }

    private fun session(id: Long, state: WorkoutSessionState) = WorkoutSession(
        id = id,
        uuid = "session-$id",
        name = "Cycle workout",
        notes = "",
        startedAt = Instant.ofEpochMilli(id * 1_000),
        endedAt = Instant.ofEpochMilli(id * 1_000 + 100).takeIf { state == WorkoutSessionState.Finished },
        localDate = LocalDate.of(2026, 8, id.toInt().coerceIn(1, 28)),
        zoneId = "UTC",
        state = state,
        keepScreenAwake = false,
        restTimerDeadlineMillis = null,
        restTimerDurationSeconds = null,
        archived = false,
        createdAtMillis = 1,
        updatedAtMillis = 1,
        sourceRoutineId = 1,
        sourceRoutineProgramKind = RoutineProgramKind.FiveThreeOne,
        sourceRoutinePhaseIndex = 0,
        sourceRoutineCycle = 1,
        sourceRoutineDayPosition = 0,
        sourceRoutinePhaseRole = RoutineProgramPhaseRole.Standard,
        programProgressAdvanced = state == WorkoutSessionState.Finished,
    )

    private fun workoutExercise(id: Long, sessionId: Long) = WorkoutExercise(
        id = id,
        uuid = "workout-exercise-$id",
        sessionId = sessionId,
        exerciseId = 7,
        position = 0,
        notes = "",
        groupId = null,
        createdAtMillis = 1,
        updatedAtMillis = 1,
        trainingMaxValueSnapshot = 300.0,
        trainingMaxKgSnapshot = 300.0,
        trainingMaxUnitIdSnapshot = "kilogram",
        cycleIncrementValueSnapshot = 10.0,
        placementKindSnapshot = RoutinePlacementKind.MainLift,
    )

    private fun set(
        id: Long,
        workoutExerciseId: Long,
        classification: WorkoutSetClassification,
        section: RoutineWorkSection,
        reps: Int,
    ) = WorkoutSet(
        id = id,
        uuid = "set-$id",
        workoutExerciseId = workoutExerciseId,
        position = id.toInt(),
        classification = classification,
        planned = true,
        completed = true,
        canonicalWeightKg = if (section == RoutineWorkSection.Optional) 300.0 else 285.0,
        enteredWeight = if (section == RoutineWorkSection.Optional) 300.0 else 285.0,
        enteredWeightUnitId = "kilogram",
        repetitions = reps,
        canonicalDistanceMetres = null,
        enteredDistance = null,
        enteredDistanceUnitId = null,
        durationSeconds = null,
        bodyweightKg = null,
        note = "",
        rpe = 8.5,
        rir = null,
        tempo = "",
        restSeconds = null,
        completedAtMillis = 2,
        deletedAtMillis = null,
        createdAtMillis = 1,
        updatedAtMillis = 1,
        prescribedCanonicalWeightKg = if (section == RoutineWorkSection.Optional) 300.0 else 285.0,
        prescribedEnteredWeight = if (section == RoutineWorkSection.Optional) 300.0 else 285.0,
        prescribedWeightUnitId = "kilogram",
        prescribedRepetitions = if (section == RoutineWorkSection.Optional) 3 else 5,
        workSectionSnapshot = section,
        optionalWorkKindSnapshot = if (section == RoutineWorkSection.Optional) {
            RoutineOptionalWorkKind.Joker
        } else {
            RoutineOptionalWorkKind.None
        },
    )
}
