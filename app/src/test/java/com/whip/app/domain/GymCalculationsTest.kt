package com.whip.app.domain

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class GymCalculationsTest {
    @Test
    fun epleyAndBrzyckiMatchDocumentedFormulas() {
        assertEquals(
            101.333333,
            requireNotNull(estimatedOneRepMax(80.0, 8, EstimatedOneRepMaxFormula.Epley)),
            0.00001,
        )
        assertEquals(
            99.310344,
            requireNotNull(estimatedOneRepMax(80.0, 8, EstimatedOneRepMaxFormula.Brzycki)),
            0.00001,
        )
        assertNull(estimatedOneRepMax(80.0, 11, EstimatedOneRepMaxFormula.Epley))
    }

    @Test
    fun completedWorkingSetsContributeToVolumeAndWarmupsDoNot() {
        val exercise = exercise()
        val working = set(weight = 80.0, reps = 8)
        val warmup = set(weight = 40.0, reps = 10).copy(
            id = 2,
            classification = WorkoutSetClassification.WarmUp,
        )

        assertEquals(640.0, working.volumeKg(exercise), 0.0)
        assertEquals(0.0, warmup.volumeKg(exercise), 0.0)
    }

    @Test
    fun workoutSummaryTotalsThreeByEightAtEightyKg() {
        val exercise = exercise()
        val session = WorkoutSession(
            id = 1,
            uuid = "session",
            name = "Push",
            notes = "",
            startedAt = Instant.ofEpochMilli(1_000),
            endedAt = Instant.ofEpochMilli(3_601_000),
            localDate = LocalDate.of(2026, 8, 17),
            zoneId = "UTC",
            state = WorkoutSessionState.Finished,
            keepScreenAwake = false,
            restTimerDeadlineMillis = null,
            restTimerDurationSeconds = null,
            archived = false,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        val workoutExercise = WorkoutExercise(1, "we", 1, 1, 0, "", null, 1, 1)
        val sets = (1L..3L).map { set(80.0, 8).copy(id = it, uuid = "set-$it") }

        val summary = calculateWorkoutSummary(
            session,
            listOf(workoutExercise),
            sets,
            mapOf(1L to exercise),
            nowMillis = session.endedAt!!.toEpochMilli(),
        )

        assertEquals(3, summary.completedSetCount)
        assertEquals(24, summary.repetitions)
        assertEquals(1_920.0, summary.volumeKg, 0.0)
        assertEquals(3_600L, summary.elapsedSeconds)
    }

    @Test
    fun assistedBodyweightNeverProducesNegativeLoad() {
        val exercise = exercise().copy(
            trackingType = ExerciseTrackingType.AssistedBodyweightReps,
            effectiveBodyweightPercent = 100.0,
        )
        val assisted = set(weight = 100.0, reps = 5).copy(bodyweightKg = 80.0)

        assertEquals(0.0, assisted.effectiveLoadKg(exercise)!!, 0.0)
    }

    @Test
    fun completedSetsRequireTheFieldsTheirTrackingTypeNeeds() {
        assertThrows(IllegalArgumentException::class.java) {
            validateWorkoutSetDraft(
                WorkoutSetDraft(completed = true, weight = 80.0),
                ExerciseTrackingType.WeightReps,
            )
        }
        validateWorkoutSetDraft(
            WorkoutSetDraft(completed = true, weight = 80.0, reps = 5),
            ExerciseTrackingType.WeightReps,
        )
        assertThrows(IllegalArgumentException::class.java) {
            validateWorkoutSetDraft(
                WorkoutSetDraft(completed = true, reps = 5),
                ExerciseTrackingType.WeightReps,
                MachineLoadType.Level,
            )
        }
    }

    @Test
    fun setEffortAndTimerValuesHaveStrictBounds() {
        assertThrows(IllegalArgumentException::class.java) {
            validateWorkoutSetDraft(WorkoutSetDraft(rpe = 0.0), ExerciseTrackingType.RepsOnly)
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateWorkoutSetDraft(WorkoutSetDraft(rir = 11.0), ExerciseTrackingType.RepsOnly)
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateWorkoutSetDraft(WorkoutSetDraft(restSeconds = -1), ExerciseTrackingType.RepsOnly)
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateWorkoutSetDraft(WorkoutSetDraft(weight = Double.NaN), ExerciseTrackingType.WeightOnly)
        }
    }

    @Test
    fun loadInterpretationsProduceExplicitCanonicalResistance() {
        assertEquals(
            45.0,
            canonicalResistanceKg(45.0, "kilogram", interpretation = LoadInterpretation.Total)!!,
            0.0,
        )
        assertEquals(
            90.0,
            canonicalResistanceKg(45.0, "kilogram", interpretation = LoadInterpretation.PerHand)!!,
            0.0,
        )
        assertEquals(
            45.0,
            canonicalResistanceKg(
                45.0,
                "kilogram",
                interpretation = LoadInterpretation.PerSide,
                unilateral = true,
            )!!,
            0.0,
        )
        assertEquals(
            50.0,
            canonicalResistanceKg(
                25.0,
                "kilogram",
                interpretation = LoadInterpretation.MachineDisplayedMass,
                stackMode = MachineStackMode.DualCombined,
            )!!,
            0.0,
        )
        assertEquals(
            25.0,
            canonicalResistanceKg(
                25.0,
                "kilogram",
                interpretation = LoadInterpretation.MachineDisplayedMass,
                stackMode = MachineStackMode.DualIndependent,
                unilateral = true,
            )!!,
            0.0,
        )
        assertEquals(
            25.0,
            canonicalResistanceKg(
                10.0,
                "kilogram",
                interpretation = LoadInterpretation.Total,
                pulleyRatio = 2.0,
                baseLoadKg = 2.5,
                addOnPlateKg = 2.5,
            )!!,
            0.0,
        )
    }

    @Test
    fun ordinalSettingsRequireMappingBeforeMassAnalytics() {
        assertNull(
            canonicalResistanceKg(
                enteredValue = null,
                enteredUnitId = null,
                machineSetting = 7.0,
                interpretation = LoadInterpretation.OrdinalSetting,
            ),
        )
        assertEquals(
            34.0,
            canonicalResistanceKg(
                enteredValue = null,
                enteredUnitId = null,
                machineSetting = 7.0,
                interpretation = LoadInterpretation.OrdinalSetting,
                massMappingKg = mapOf(7.0 to 32.0),
                addOnPlateKg = 2.0,
            )!!,
            0.0,
        )
    }

    @Test
    fun bodyweightAssistanceAndEffortAdjustedE1rmStayTruthful() {
        val bodyweightSet = set(weight = 10.0, reps = 5).copy(bodyweightKg = 80.0)
        val plusExternal = exercise().copy(loadInterpretation = LoadInterpretation.BodyweightPlusExternal)
        val percentage = exercise().copy(
            loadInterpretation = LoadInterpretation.BodyweightPercentage,
            effectiveBodyweightPercent = 60.0,
        )
        val assisted = exercise().copy(
            trackingType = ExerciseTrackingType.AssistedBodyweightReps,
            loadInterpretation = LoadInterpretation.AssistedSubtraction,
        )

        assertEquals(90.0, bodyweightSet.effectiveLoadKg(plusExternal)!!, 0.0)
        assertEquals(58.0, bodyweightSet.effectiveLoadKg(percentage)!!, 0.0)
        assertEquals(70.0, bodyweightSet.effectiveLoadKg(assisted)!!, 0.0)

        val effortSet = set(weight = 100.0, reps = 5).copy(rir = 2.0)
        assertEquals(116.666666, effortSet.estimatedOneRepMaxKg(exercise())!!, 0.00001)
        assertEquals(
            123.333333,
            effortSet.estimatedOneRepMaxKg(exercise(), adjustForEffort = true)!!,
            0.00001,
        )
    }

    private fun exercise() = Exercise(
        id = 1,
        uuid = "exercise",
        name = "Flat Barbell Bench Press",
        trackingType = ExerciseTrackingType.WeightReps,
        notes = "",
        equipment = "",
        primaryMuscles = "",
        secondaryMuscles = "",
        weightUnitId = "kilogram",
        weightIncrement = 2.5,
        repetitionIncrement = 1,
        defaultRestSeconds = 120,
        defaultGraphMetric = "Estimated1RM",
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

    @Test
    fun practicalMachineConversionAvoidsLongUnitConversionDecimals() {
        assertEquals(5.0, convertPracticalMassValue(2.5, "kilogram", "pound"), 0.0)
        assertEquals(45.0, convertPracticalMassValue(20.4, "kilogram", "pound"), 0.0)
        assertEquals(2.5, convertPracticalMassValue(5.0, "pound", "kilogram"), 0.0)
    }

    private fun set(weight: Double, reps: Int) = WorkoutSet(
        id = 1,
        uuid = "set",
        workoutExerciseId = 1,
        position = 0,
        classification = WorkoutSetClassification.Working,
        planned = false,
        completed = true,
        canonicalWeightKg = weight,
        enteredWeight = weight,
        enteredWeightUnitId = "kilogram",
        repetitions = reps,
        canonicalDistanceMetres = null,
        enteredDistance = null,
        enteredDistanceUnitId = null,
        durationSeconds = null,
        bodyweightKg = null,
        note = "",
        rpe = null,
        rir = null,
        tempo = "",
        restSeconds = null,
        completedAtMillis = 1,
        deletedAtMillis = null,
        createdAtMillis = 1,
        updatedAtMillis = 1,
    )
}
