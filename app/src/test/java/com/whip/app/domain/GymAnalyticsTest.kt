package com.whip.app.domain

import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GymAnalyticsTest {
    @Test fun downsamplingKeepsEndpointsWithoutChangingSourceHistory() {
        val source = (0 until 1_000).toList()
        val sampled = downsampleEvenly(source, 100)
        assertEquals(100, sampled.size)
        assertEquals(0, sampled.first())
        assertEquals(999, sampled.last())
        assertEquals(1_000, source.size)
    }
    @Test fun repMaxAndPlateToolsAreDeterministic() {
        val estimate = requireNotNull(calculateRepMaxTable(80.0, 8))
        assertEquals(101.333333, estimate.oneRepMax, 0.00001)
        val plates = calculatePlateLoading(100.0, 20.0, listOf(20.0, 10.0, 5.0, 2.5, 1.25))
        assertEquals(listOf(20.0, 20.0), plates.platesPerSide)
        assertEquals(100.0, plates.achievedWeight, 0.0)
    }

    @Test fun plateSolverRespectsFiniteInventoryAndReturnsClosestLoad() {
        val exact = calculatePlateLoading(
            targetWeight = 100.0,
            barWeight = 20.0,
            availablePlates = listOf(25.0, 10.0, 5.0, 2.5),
            plateQuantities = mapOf(25.0 to 2, 10.0 to 2, 5.0 to 2, 2.5 to 2),
            collarWeight = 0.0,
            perSideLoading = true,
        )
        assertEquals(true, exact.exact)
        assertEquals(listOf(25.0, 10.0, 5.0), exact.platesPerSide)
        assertEquals(100.0, exact.achievedWeight, 0.0)

        val closest = calculatePlateLoading(
            targetWeight = 103.0,
            barWeight = 20.0,
            availablePlates = listOf(25.0, 10.0, 5.0, 2.5),
            plateQuantities = mapOf(25.0 to 2, 10.0 to 2, 5.0 to 2, 2.5 to 2),
            collarWeight = 1.0,
            perSideLoading = true,
        )
        assertEquals(false, closest.exact)
        assertEquals(101.0, closest.achievedWeight, 0.0)
        assertEquals(2.0, closest.remainder, 0.0)
        assertEquals(1, closest.platesPerSide.count { it == 25.0 })
        assertEquals(1, closest.platesPerSide.count { it == 10.0 })
        assertEquals(1, closest.platesPerSide.count { it == 5.0 })

        val singleStack = calculatePlateLoading(
            targetWeight = 17.5,
            barWeight = 5.0,
            availablePlates = listOf(5.0, 2.5),
            plateQuantities = mapOf(5.0 to 2, 2.5 to 1),
            perSideLoading = false,
        )
        assertEquals(listOf(5.0, 5.0, 2.5), singleStack.platesPerSide)
        assertEquals(17.5, singleStack.achievedWeight, 0.0)
    }

    @Test fun e1rmStillHonorsNormalCutoff() {
        assertNull(estimatedOneRepMax(50.0, 11, EstimatedOneRepMaxFormula.Epley))
    }

    @Test fun workoutWeekAndMonthAggregationPreserveSourceMath() {
        val exercise = exercise()
        val sessions = listOf(session(1, LocalDate.of(2026, 8, 17)), session(2, LocalDate.of(2026, 8, 19)))
        val workoutExercises = listOf(workoutExercise(1, 1), workoutExercise(2, 2))
        val sets = listOf(set(1, 1, 80.0, 5), set(2, 2, 90.0, 5))
        val workouts = buildExerciseGraph(exercise, sessions, workoutExercises, sets, GymGraphMetric.WorkoutVolume)
        val week = buildExerciseGraph(exercise, sessions, workoutExercises, sets, GymGraphMetric.WorkoutVolume, GymGraphAggregation.Week)
        assertEquals(2, workouts.size)
        assertEquals(850.0, week.single().value, 0.0)
        assertEquals(2, week.single().sourceCount)
    }

    @Test fun weeklySummaryCountsRecordsOnlyFromIncludedFinishedWorkouts() {
        val weekStart = LocalDate.of(2026, 8, 17)
        val finished = session(1, weekStart)
        val active = session(2, weekStart).copy(state = WorkoutSessionState.Active, endedAt = null)
        fun record(uuid: String, sessionId: Long?) = PersonalRecord(
            uuid = uuid,
            exerciseId = 1,
            type = PersonalRecordType.MaxWeight,
            value = 100.0,
            secondaryValue = null,
            unitId = "kilogram",
            sourceSetId = null,
            sourceSessionId = sessionId,
            // The authored workout date, not this UTC instant, owns weekly attribution.
            achievedAtMillis = Instant.parse("2025-01-01T12:00:00Z").toEpochMilli(),
            current = true,
            imported = sessionId == null,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )

        val summary = buildWeeklyGymSummary(
            weekStart = weekStart,
            sessions = listOf(finished, active),
            workoutExercises = emptyList(),
            sets = emptyList(),
            exercises = listOf(exercise()),
            personalRecords = listOf(
                record("finished", finished.id),
                record("active", active.id),
                record("imported", null),
            ),
        )

        assertEquals(1, summary.workouts)
        assertEquals(1, summary.newPersonalRecords)
    }

    @Test fun weeklyAggregationUsesTheConfiguredFirstDay() {
        val exercise = exercise()
        val sessions = listOf(session(1, LocalDate.of(2026, 8, 16)), session(2, LocalDate.of(2026, 8, 17)))
        val workoutExercises = listOf(workoutExercise(1, 1), workoutExercise(2, 2))
        val sets = listOf(set(1, 1, 80.0, 5), set(2, 2, 90.0, 5))

        val sundayWeek = buildExerciseGraph(
            exercise,
            sessions,
            workoutExercises,
            sets,
            GymGraphMetric.WorkoutVolume,
            GymGraphAggregation.Week,
            firstDayOfWeek = DayOfWeek.SUNDAY,
        )
        val mondayWeek = buildExerciseGraph(
            exercise,
            sessions,
            workoutExercises,
            sets,
            GymGraphMetric.WorkoutVolume,
            GymGraphAggregation.Week,
            firstDayOfWeek = DayOfWeek.MONDAY,
        )

        assertEquals(1, sundayWeek.size)
        assertEquals(LocalDate.of(2026, 8, 16), sundayWeek.single().date)
        assertEquals(2, mondayWeek.size)
    }

    @Test fun machineGraphsNeverMergeDifferentPhysicalMachines() {
        val exercise = exercise()
        val sessions = listOf(session(1, LocalDate.of(2026, 8, 17)), session(2, LocalDate.of(2026, 8, 19)))
        val workoutExercises = listOf(
            workoutExercise(1, 1).copy(machineId = 10, machineLoadTypeSnapshot = MachineLoadType.Level),
            workoutExercise(2, 2).copy(machineId = 20, machineLoadTypeSnapshot = MachineLoadType.Level),
        )
        val sets = listOf(
            set(1, 1, 0.0, 8).copy(canonicalWeightKg = null, enteredWeight = null, machineLoadValue = 7.0),
            set(2, 2, 0.0, 8).copy(canonicalWeightKg = null, enteredWeight = null, machineLoadValue = 50.0),
        )

        val home = buildExerciseGraph(
            exercise, sessions, workoutExercises, sets, GymGraphMetric.MaxMachineSetting,
            machineId = 10, restrictToMachine = true,
        )
        val publicGym = buildExerciseGraph(
            exercise, sessions, workoutExercises, sets, GymGraphMetric.MaxMachineSetting,
            machineId = 20, restrictToMachine = true,
        )

        assertEquals(7.0, home.single().value, 0.0)
        assertEquals(50.0, publicGym.single().value, 0.0)
        assertEquals(0.0, sets.first().volumeKg(exercise), 0.0)
        assertNull(sets.first().estimatedOneRepMaxKg(exercise))
    }

    @Test fun reverseNumberedScaleTreatsTheLowerNumberAsTheStrongerSetting() {
        val exercise = exercise()
        val sessions = listOf(session(1, LocalDate.of(2026, 8, 17)))
        val workoutExercises = listOf(
            workoutExercise(1, 1).copy(machineId = 10, machineLoadTypeSnapshot = MachineLoadType.Level),
        )
        val sets = listOf(
            set(1, 1, 0.0, 8).copy(canonicalWeightKg = null, enteredWeight = null, machineLoadValue = 7.0),
            set(2, 1, 0.0, 8).copy(canonicalWeightKg = null, enteredWeight = null, machineLoadValue = 2.0),
        )

        val points = buildExerciseGraph(
            exercise = exercise,
            sessions = sessions,
            workoutExercises = workoutExercises,
            sets = sets,
            metric = GymGraphMetric.MaxMachineSetting,
            machineId = 10,
            restrictToMachine = true,
            machineLevelDirection = MachineLevelDirection.HigherNumberLessResistance,
        )

        assertEquals(2.0, points.single().value, 0.0)
    }

    @Test fun deletedMachineScopeNeverMergesWithFreeWeightsOrAnotherMachine() {
        val exercise = exercise()
        val sessions = listOf(
            session(1, LocalDate.of(2026, 8, 17)),
            session(2, LocalDate.of(2026, 8, 18)),
            session(3, LocalDate.of(2026, 8, 19)),
        )
        val workoutExercises = listOf(
            workoutExercise(1, 1).copy(machineId = null, machineProfileUuidSnapshot = "deleted-machine"),
            workoutExercise(2, 2).copy(machineId = null, machineProfileUuidSnapshot = null),
            workoutExercise(3, 3).copy(machineId = 30, machineProfileUuidSnapshot = "current-machine"),
        )
        val sets = listOf(
            set(1, 1, 40.0, 5),
            set(2, 2, 60.0, 5),
            set(3, 3, 80.0, 5),
        )

        val deleted = buildExerciseGraph(
            exercise, sessions, workoutExercises, sets, GymGraphMetric.MaxWeight,
            machineScopeUuid = "deleted-machine",
            machineScopeUuids = setOf("deleted-machine"),
            restrictToMachine = true,
        )
        val freeWeight = buildExerciseGraph(
            exercise, sessions, workoutExercises, sets, GymGraphMetric.MaxWeight,
            machineScopeUuid = null,
            machineScopeUuids = emptySet(),
            restrictToMachine = true,
        )

        assertEquals(listOf(40.0), deleted.map(GymGraphPoint::value))
        assertEquals(listOf(60.0), freeWeight.map(GymGraphPoint::value))
    }

    @Test fun machineGraphCanExplicitlyCompareCompatibleConfigurationVersions() {
        val exercise = exercise()
        val sessions = listOf(session(1, LocalDate.of(2026, 8, 17)), session(2, LocalDate.of(2026, 8, 19)))
        val placements = listOf(
            workoutExercise(1, 1).copy(machineId = 10),
            workoutExercise(2, 2).copy(machineId = 11),
        )
        val sets = listOf(set(1, 1, 40.0, 8), set(2, 2, 45.0, 8))

        val combined = buildExerciseGraph(
            exercise,
            sessions,
            placements,
            sets,
            GymGraphMetric.MaxWeight,
            machineIds = setOf(10, 11),
            restrictToMachine = true,
        )
        val oneVersion = buildExerciseGraph(
            exercise,
            sessions,
            placements,
            sets,
            GymGraphMetric.MaxWeight,
            machineIds = setOf(10),
            restrictToMachine = true,
        )

        assertEquals(2, combined.size)
        assertEquals(1, oneVersion.size)
        assertEquals(40.0, oneVersion.single().value, 0.0)
    }

    @Test fun laterExerciseEditsDoNotReinterpretPerformedAnalytics() {
        val editedExercise = exercise().copy(
            trackingType = ExerciseTrackingType.RepsOnly,
            includeInVolume = false,
            includeInPersonalRecords = false,
            oneRepMaxFormula = EstimatedOneRepMaxFormula.Brzycki,
        )
        val placement = workoutExercise(1, 1).copy(
            trackingTypeSnapshot = ExerciseTrackingType.WeightReps,
            includeInVolumeSnapshot = true,
            includeInPersonalRecordsSnapshot = true,
            oneRepMaxFormulaSnapshot = EstimatedOneRepMaxFormula.Epley,
        )
        val sessions = listOf(session(1, LocalDate.of(2026, 8, 17)))
        val sets = listOf(set(1, 1, 80.0, 8))

        val volume = buildExerciseGraph(editedExercise, sessions, listOf(placement), sets, GymGraphMetric.WorkoutVolume)
        val e1rm = buildExerciseGraph(editedExercise, sessions, listOf(placement), sets, GymGraphMetric.EstimatedOneRepMax)

        assertEquals(640.0, volume.single().value, 0.0)
        assertEquals(101.333333, e1rm.single().value, 0.00001)
    }

    private fun exercise() = Exercise(1, "e", "Press", ExerciseTrackingType.WeightReps, "", "", "", "", "kilogram", 2.5, 1, 120, "Estimated1RM", EstimatedOneRepMaxFormula.Epley, 20.0, emptyList(), true, true, BodyweightLoadPolicy.ExternalWeightOnly, 100.0, null, null, null, false, 0, false, 1, 1)
    private fun session(id: Long, date: LocalDate) = WorkoutSession(id, "s$id", "", "", Instant.parse("2026-08-17T12:00:00Z"), Instant.parse("2026-08-17T13:00:00Z"), date, "UTC", WorkoutSessionState.Finished, false, null, null, false, 1, 1)
    private fun workoutExercise(id: Long, sessionId: Long) = WorkoutExercise(id, "w$id", sessionId, 1, 0, "", null, 1, 1)
    private fun set(id: Long, workoutExerciseId: Long, weight: Double, reps: Int) = WorkoutSet(id, "set$id", workoutExerciseId, 0, WorkoutSetClassification.Working, false, true, weight, weight, "kilogram", reps, null, null, null, null, null, "", null, null, "", null, 1, null, 1, 1)
}
