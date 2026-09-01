package com.whip.app.ui

import com.whip.app.core.OperationStatus
import com.whip.app.core.WhipResult
import com.whip.app.domain.BodyweightLoadPolicy
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.Exercise
import com.whip.app.domain.GymGraphRange
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.PersonalRecord
import com.whip.app.domain.PersonalRecordType
import com.whip.app.domain.RoutineOptionalWorkKind
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.WorkoutExercise
import com.whip.app.domain.WorkoutExerciseOutcome
import com.whip.app.domain.WorkoutGroup
import com.whip.app.domain.WorkoutGroupType
import com.whip.app.domain.WorkoutSession
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.WorkoutSet
import com.whip.app.domain.WorkoutSetClassification
import java.time.LocalDate
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GymUxRulesTest {
    private val today = LocalDate.of(2026, 8, 22)

    @Test
    fun restTimerNeverRendersAboveTheSelectedDurationWhenTheUiClockIsStale() {
        val selectedDuration = 5 * 60
        val timerStartedAt = 1_000_500L
        val deadline = timerStartedAt + selectedDuration * 1_000L

        assertEquals(
            selectedDuration,
            restTimerRemainingSeconds(
                deadlineMillis = deadline,
                nowMillis = 1_000_000L,
                configuredDurationSeconds = selectedDuration,
            ),
        )
        assertEquals(
            selectedDuration - 1,
            restTimerRemainingSeconds(
                deadlineMillis = deadline,
                nowMillis = timerStartedAt + 1_000L,
                configuredDurationSeconds = selectedDuration,
            ),
        )
        assertEquals(
            selectedDuration,
            restTimerRemainingSeconds(deadline, timerStartedAt + 1L, selectedDuration),
        )
        assertEquals(
            selectedDuration,
            restTimerRemainingSeconds(deadline, timerStartedAt + 999L, selectedDuration),
        )
        assertNull(
            restTimerRemainingSeconds(
                deadlineMillis = deadline,
                nowMillis = deadline,
                configuredDurationSeconds = selectedDuration,
            ),
        )
    }

    @Test
    fun finishReviewMatchesOnlyTheExactSessionIdentityAndRevision() {
        val session = WorkoutSession(
            id = 7,
            uuid = "session-7",
            name = "Workout",
            notes = "",
            startedAt = Instant.ofEpochMilli(1),
            endedAt = null,
            localDate = today,
            zoneId = "UTC",
            state = WorkoutSessionState.Active,
            keepScreenAwake = false,
            restTimerDeadlineMillis = null,
            restTimerDurationSeconds = null,
            archived = false,
            createdAtMillis = 1,
            updatedAtMillis = 1,
            workoutRevision = 4,
        )

        assertTrue(session.matchesFinishReview(WorkoutFinishBoundary(7, "session-7", 4)))
        assertTrue(!session.matchesFinishReview(WorkoutFinishBoundary(7, "session-7", 5)))
        assertTrue(!session.matchesFinishReview(WorkoutFinishBoundary(7, "other-session", 4)))
    }

    @Test
    fun personalRecordRecoveryKeepsAStaleRecordInTheRetrySetAfterItsLastSetWasRemoved() {
        val placement = WorkoutExercise(
            id = 11,
            uuid = "placement-11",
            sessionId = 7,
            exerciseId = 22,
            position = 0,
            notes = "",
            groupId = null,
            createdAtMillis = 1,
            updatedAtMillis = 1,
            loadInterpretationSnapshot = LoadInterpretation.Total,
        )
        val removedLastSet = performanceSet(placement.id).copy(
            completed = true,
            deletedAtMillis = 3,
        )
        val staleRecord = PersonalRecord(
            uuid = "stale-pr",
            exerciseId = placement.exerciseId,
            type = PersonalRecordType.MaxWeight,
            value = 100.0,
            secondaryValue = null,
            unitId = "kilogram",
            sourceSetId = removedLastSet.id,
            sourceSessionId = placement.sessionId,
            achievedAtMillis = 2,
            current = true,
            imported = false,
            createdAtMillis = 2,
            updatedAtMillis = 2,
        )

        val firstAttempt = personalRecordReconciliationExerciseIds(
            listOf(removedLastSet),
            listOf(placement),
            listOf(staleRecord),
        )
        // A failed first rebuild leaves the PR row in Room, so a recreated ViewModel derives
        // the same target and retries instead of losing the exercise from discovery.
        val recreatedAttempt = personalRecordReconciliationExerciseIds(
            listOf(removedLastSet),
            listOf(placement),
            listOf(staleRecord),
        )

        assertEquals(setOf(placement.exerciseId), firstAttempt)
        assertEquals(firstAttempt, recreatedAttempt)
    }

    @Test
    fun relativeGraphRangeAlwaysEndsToday() {
        val result = validateGymGraphRange(GymGraphRange.ThreeMonths, "", "", today)

        assertEquals(today.minusMonths(3), result.from)
        assertEquals(today, result.to)
        assertNull(result.error)
    }

    @Test
    fun customGraphRangeRequiresBothStrictOrderedDates() {
        assertEquals(
            "Enter both From and To dates",
            validateGymGraphRange(GymGraphRange.Custom, "", "", today).error,
        )
        assertEquals(
            "From must use YYYY-MM-DD",
            validateGymGraphRange(GymGraphRange.Custom, "08/01/2026", "2026-08-22", today).error,
        )
        assertEquals(
            "From must be on or before To",
            validateGymGraphRange(GymGraphRange.Custom, "2026-08-23", "2026-08-22", today).error,
        )

        val valid = validateGymGraphRange(GymGraphRange.Custom, "2026-08-01", "2026-08-22", today)
        assertEquals(LocalDate.of(2026, 8, 1), valid.from)
        assertEquals(today, valid.to)
        assertNull(valid.error)
    }

    @Test
    fun positiveNumberListReportsTheExactInvalidToken() {
        val valid = parsePositiveNumberList("45, 25, 10, 2.5", "plate")
        assertEquals(listOf(45.0, 25.0, 10.0, 2.5), valid.values)
        assertNull(valid.error)

        val invalid = parsePositiveNumberList("45, banana, 10", "plate")
        assertTrue(invalid.values.isEmpty())
        assertEquals("Plate 2 (“banana”) must be a positive number", invalid.error)
    }

    @Test
    fun positiveNumberListDoesNotTurnBlankInputIntoUnlimitedInventory() {
        val result = parsePositiveNumberList("   ", "plate")

        assertTrue(result.values.isEmpty())
        assertEquals("Enter at least one plate", result.error)
    }

    @Test
    fun quantityLabelsUseTheSingularOnlyForOne() {
        assertEquals("0 exercises", quantityLabel(0, "exercise"))
        assertEquals("1 exercise", quantityLabel(1, "exercise"))
        assertEquals("2 exercises", quantityLabel(2, "exercise"))
        assertEquals("1 entry", quantityLabel(1, "entry", "entries"))
        assertEquals("2 entries", quantityLabel(2, "entry"))
        assertEquals("2 boxes", quantityLabel(2, "box"))
    }

    @Test
    fun gymPersistenceResultPreservesFailureForTheCallingEditor() {
        val cause = IllegalStateException("write failed")
        val failure = gymPersistenceResult(
            false,
            OperationStatus.Failed("Could not persist", cause),
        ) as WhipResult.Failure

        assertEquals("Could not persist", failure.message)
        assertSame(cause, failure.cause)
        assertTrue(gymPersistenceResult(true, OperationStatus.Succeeded("Saved")) is WhipResult.Success)
    }

    @Test
    fun sharedExerciseSearchScalesAndMatchesMeaningfulMetadata() {
        val library = (1L..1_000L).map { id ->
            testExercise(
                id = id,
                name = "Exercise $id",
                equipment = if (id == 999L) "Cable tower" else "Dumbbell",
                primaryMuscles = if (id == 999L) "Rear deltoid" else "Chest",
            )
        }

        val match = library.filter { exerciseMatchesQuery(it, "cable rear") }
        assertEquals(listOf(999L), match.map(Exercise::id))
        assertTrue(exerciseMatchesQuery(library.first(), "weight repetitions"))
        assertTrue(exerciseMatchesQuery(library.first(), "garage stack", "Garage Stack v2"))
    }

    @Test
    fun nextSetSelectionHonorsGroupRotationAndFallsBackWhenThatMemberIsDone() {
        val group = WorkoutGroup(50, "group", 1, "Superset", WorkoutGroupType.Superset, 0, 1, 1)
        fun item(id: Long, position: Int, completed: Boolean = false): WorkoutExerciseUi {
            val exercise = testExercise(id, "Exercise $id", "Barbell", "Chest")
            val placement = WorkoutExercise(
                id = id,
                uuid = "placement-$id",
                sessionId = 1,
                exerciseId = id,
                position = position,
                notes = "",
                groupId = group.id,
                createdAtMillis = 1,
                updatedAtMillis = 1,
                loadInterpretationSnapshot = LoadInterpretation.Total,
            )
            val set = WorkoutSet(
                id = id * 10,
                uuid = "set-$id",
                workoutExerciseId = id,
                position = 0,
                classification = WorkoutSetClassification.Working,
                planned = false,
                completed = completed,
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
                completedAtMillis = 2.takeIf { completed }?.toLong(),
                deletedAtMillis = null,
                createdAtMillis = 1,
                updatedAtMillis = 1,
            )
            return WorkoutExerciseUi(placement, exercise, listOf(set), emptyList(), 0, group, null)
        }

        val first = item(1, 0)
        val second = item(2, 1)
        assertEquals(second.workoutExercise.id, selectNextWorkoutSet(listOf(first, second), mapOf(group.id to second.workoutExercise.id))?.first?.workoutExercise?.id)

        val completedFirst = item(1, 0, completed = true)
        assertEquals(second.workoutExercise.id, selectNextWorkoutSet(listOf(completedFirst, second), mapOf(group.id to first.workoutExercise.id))?.first?.workoutExercise?.id)

        val independent = item(3, 1).let { candidate ->
            candidate.copy(
                workoutExercise = candidate.workoutExercise.copy(position = 1, groupId = null),
                group = null,
            )
        }
        val trailingGroupMember = second.copy(workoutExercise = second.workoutExercise.copy(position = 2))
        assertEquals(
            trailingGroupMember.workoutExercise.id,
            selectNextWorkoutSet(
                listOf(first, independent, trailingGroupMember),
                mapOf(group.id to trailingGroupMember.workoutExercise.id),
            )?.first?.workoutExercise?.id,
        )
        assertEquals(second.workoutExercise.id, selectNextWorkoutSet(listOf(completedFirst, second))?.first?.workoutExercise?.id)
    }

    @Test
    fun programmedLiftFinishesMainThenSupplementalBeforeChangingStationsAndOptionalRequiresAcceptance() {
        fun item(id: Long, position: Int, section: RoutineWorkSection, completed: Boolean = false): WorkoutExerciseUi {
            val exercise = testExercise(id, "Exercise $id", "Barbell", "Chest")
            val placement = WorkoutExercise(
                id = id,
                uuid = "placement-$id",
                sessionId = 1,
                exerciseId = id,
                position = position,
                notes = "",
                groupId = null,
                createdAtMillis = 1,
                updatedAtMillis = 1,
                loadInterpretationSnapshot = LoadInterpretation.Total,
            )
            val set = WorkoutSet(
                id = id * 10,
                uuid = "set-$id",
                workoutExerciseId = id,
                position = 0,
                classification = WorkoutSetClassification.Working,
                planned = true,
                completed = completed,
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
                completedAtMillis = 2L.takeIf { completed },
                deletedAtMillis = null,
                createdAtMillis = 1,
                updatedAtMillis = 1,
                workSectionSnapshot = section,
                optionalWorkKindSnapshot = if (section == RoutineWorkSection.Optional) RoutineOptionalWorkKind.Joker else RoutineOptionalWorkKind.None,
            )
            return WorkoutExerciseUi(placement, exercise, listOf(set), emptyList(), 0, null, null)
        }

        val supplemental = item(1, 0, RoutineWorkSection.Supplemental)
        val main = item(2, 1, RoutineWorkSection.Main)
        assertEquals(supplemental.workoutExercise.id, selectNextWorkoutSet(listOf(supplemental, main))?.first?.workoutExercise?.id)

        val completedMain = main.sets.single().copy(completed = true, completedAtMillis = 2L)
        val sameLiftSupplemental = supplemental.sets.single().copy(
            id = 21L,
            workoutExerciseId = main.workoutExercise.id,
        )
        val squatStation = main.copy(sets = listOf(completedMain, sameLiftSupplemental))
        val benchMain = item(4, 2, RoutineWorkSection.Main)
        assertEquals(
            squatStation.workoutExercise.id,
            selectNextWorkoutSet(listOf(squatStation, benchMain))?.first?.workoutExercise?.id,
        )

        val optional = item(3, 2, RoutineWorkSection.Optional)
        assertEquals(supplemental.workoutExercise.id, selectNextWorkoutSet(listOf(supplemental, optional))?.first?.workoutExercise?.id)
        assertEquals(optional.workoutExercise.id, selectPendingOptionalWorkoutSet(listOf(optional))?.first?.workoutExercise?.id)
        assertNull(selectNextWorkoutSet(listOf(optional)))
        assertEquals(optional.workoutExercise.id, selectNextWorkoutSet(listOf(optional), acceptedOptionalSetIds = setOf(30L))?.first?.workoutExercise?.id)
        assertTrue(!optional.sets.single().isIncompleteRequiredWork())
        val workoutOnlyOptional = optional.copy(
            sets = listOf(optional.sets.single().copy(optionalWorkKindSnapshot = RoutineOptionalWorkKind.None)),
        )
        assertNull(selectPendingOptionalWorkoutSet(listOf(workoutOnlyOptional)))
        assertEquals(
            workoutOnlyOptional.workoutExercise.id,
            selectNextWorkoutSet(listOf(workoutOnlyOptional))?.first?.workoutExercise?.id,
        )
        assertEquals(
            workoutOnlyOptional.workoutExercise.id,
            selectRequestedWorkoutSet(
                listOf(main, workoutOnlyOptional),
                workoutOnlyOptional.workoutExercise.id,
            )?.first?.workoutExercise?.id,
        )
    }

    @Test
    fun activePerformanceKeepsCompletedRetiredWorkWithoutResurrectingEmptyPlacements() {
        fun placement(id: Long, outcome: WorkoutExerciseOutcome) = WorkoutExercise(
            id = id,
            uuid = "placement-$id",
            sessionId = 1,
            exerciseId = id,
            position = id.toInt(),
            notes = "",
            groupId = null,
            createdAtMillis = 1,
            updatedAtMillis = 1,
            loadInterpretationSnapshot = LoadInterpretation.Total,
            outcome = outcome,
        )
        val active = placement(1, WorkoutExerciseOutcome.Active)
        val performedRetired = placement(2, WorkoutExerciseOutcome.Substituted)
        val emptyRetired = placement(3, WorkoutExerciseOutcome.Removed)

        assertEquals(
            listOf(active.id, performedRetired.id),
            selectWorkoutPerformancePlacements(
                listOf(active, performedRetired, emptyRetired),
                listOf(performanceSet(workoutExerciseId = performedRetired.id)),
                sessionId = 1,
            ).map(WorkoutExercise::id),
        )
    }

    private fun performanceSet(workoutExerciseId: Long) = WorkoutSet(
        id = 90,
        uuid = "performed-set",
        workoutExerciseId = workoutExerciseId,
        position = 0,
        classification = WorkoutSetClassification.Working,
        planned = false,
        completed = true,
        canonicalWeightKg = 100.0,
        enteredWeight = 100.0,
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
        completedAtMillis = 2,
        deletedAtMillis = null,
        createdAtMillis = 1,
        updatedAtMillis = 2,
    )

    private fun testExercise(
        id: Long,
        name: String,
        equipment: String,
        primaryMuscles: String,
    ) = Exercise(
        id = id,
        uuid = "exercise-$id",
        name = name,
        trackingType = ExerciseTrackingType.WeightReps,
        notes = "",
        equipment = equipment,
        primaryMuscles = primaryMuscles,
        secondaryMuscles = "",
        weightUnitId = "kilogram",
        weightIncrement = 2.5,
        repetitionIncrement = 1,
        defaultRestSeconds = 120,
        defaultGraphMetric = "EstimatedOneRepMax",
        oneRepMaxFormula = EstimatedOneRepMaxFormula.Epley,
        barWeightKg = null,
        availablePlatesKg = emptyList(),
        includeInVolume = true,
        includeInPersonalRecords = true,
        bodyweightLoadPolicy = BodyweightLoadPolicy.ExternalWeightOnly,
        effectiveBodyweightPercent = 100.0,
        showRpe = null,
        showRir = null,
        showTempo = null,
        favorite = false,
        position = id.toInt(),
        archived = false,
        createdAtMillis = id,
        updatedAtMillis = id,
    )
}
