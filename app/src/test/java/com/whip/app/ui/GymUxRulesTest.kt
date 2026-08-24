package com.whip.app.ui

import com.whip.app.domain.BodyweightLoadPolicy
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.Exercise
import com.whip.app.domain.GymGraphRange
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.WorkoutExercise
import com.whip.app.domain.WorkoutGroup
import com.whip.app.domain.WorkoutGroupType
import com.whip.app.domain.WorkoutSet
import com.whip.app.domain.WorkoutSetClassification
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GymUxRulesTest {
    private val today = LocalDate.of(2026, 8, 22)

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
    }

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
