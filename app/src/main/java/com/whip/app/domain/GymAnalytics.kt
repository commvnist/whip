package com.whip.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.floor

enum class GymGraphMetric(val label: String) {
    EstimatedOneRepMax("Estimated 1RM"),
    MaxWeight("Max weight"),
    MaxRepetitions("Max repetitions"),
    MaxWeightForReps("Max weight for selected reps"),
    ActualRepMaxHistory("Actual rep-max history"),
    SetVolume("Best set volume"),
    WorkoutVolume("Workout volume"),
    TotalRepetitions("Total repetitions"),
    Distance("Distance"),
    Duration("Duration"),
    Speed("Speed"),
    Pace("Pace"),
    MaxMachineSetting("Best machine setting"),
}

enum class GymGraphAggregation { Workout, Week, Month }
enum class GymGraphRange { Month, ThreeMonths, SixMonths, Year, All, Custom }

data class GymGraphPoint(
    val date: LocalDate,
    val value: Double,
    val sourceSessionId: Long?,
    val sourceCount: Int = 1,
)

data class GymWeeklySummary(
    val weekStart: LocalDate,
    val workouts: Int,
    val trainingDays: Int,
    val elapsedSeconds: Long,
    val completedSets: Int,
    val repetitions: Int,
    val volumeKg: Double,
    val newPersonalRecords: Int,
)

fun buildExerciseGraph(
    exercise: Exercise,
    sessions: List<WorkoutSession>,
    workoutExercises: List<WorkoutExercise>,
    sets: List<WorkoutSet>,
    metric: GymGraphMetric,
    aggregation: GymGraphAggregation = GymGraphAggregation.Workout,
    from: LocalDate? = null,
    to: LocalDate? = null,
    selectedRepetitions: Int? = null,
    includeWarmups: Boolean = false,
    oneRepMaxRepCutoff: Int = 10,
    adjustOneRepMaxForEffort: Boolean = false,
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    machineId: Long? = null,
    machineIds: Set<Long>? = null,
    machineScopeUuid: String? = null,
    machineScopeUuids: Set<String>? = null,
    restrictToMachine: Boolean = false,
    machineLevelDirection: MachineLevelDirection = MachineLevelDirection.HigherNumberMoreResistance,
): List<GymGraphPoint> {
    val sessionById = sessions.associateBy(WorkoutSession::id)
    val placements = workoutExercises.filter {
        it.exerciseId == exercise.id && (!restrictToMachine || when {
            machineScopeUuids != null && machineScopeUuids.isEmpty() && machineScopeUuid == null -> it.equipmentScopeKey == null
            machineScopeUuids != null -> it.equipmentScopeKey in machineScopeUuids
            machineScopeUuid != null -> it.equipmentScopeKey == machineScopeUuid
            machineIds != null -> it.machineId in machineIds
            else -> it.machineId == machineId
        })
    }.associateBy(WorkoutExercise::id)
    val sessionIdByWorkoutExercise = placements.mapValues { it.value.sessionId }
    val perSession = sets.filter {
        it.workoutExerciseId in sessionIdByWorkoutExercise && it.completed && it.deletedAtMillis == null
    }.groupBy { sessionIdByWorkoutExercise.getValue(it.workoutExerciseId) }.mapNotNull { (sessionId, sessionSets) ->
        val session = sessionById[sessionId] ?: return@mapNotNull null
        if (session.state == WorkoutSessionState.Discarded || session.archived) return@mapNotNull null
        if (from != null && session.localDate.isBefore(from)) return@mapNotNull null
        if (to != null && session.localDate.isAfter(to)) return@mapNotNull null
        val value = when (metric) {
            GymGraphMetric.EstimatedOneRepMax -> sessionSets.mapNotNull {
                it.estimatedOneRepMaxKg(
                    placements.getValue(it.workoutExerciseId).applyPolicySnapshot(exercise),
                    repCutoff = oneRepMaxRepCutoff,
                    includeWarmups = includeWarmups,
                    adjustForEffort = adjustOneRepMaxForEffort,
                )
            }.maxOrNull()
            GymGraphMetric.MaxWeight -> sessionSets.mapNotNull { it.effectiveLoadKg(placements.getValue(it.workoutExerciseId).applyPolicySnapshot(exercise)) }.maxOrNull()
            GymGraphMetric.MaxRepetitions -> sessionSets.mapNotNull(WorkoutSet::repetitions).maxOrNull()?.toDouble()
            GymGraphMetric.MaxWeightForReps, GymGraphMetric.ActualRepMaxHistory -> sessionSets.filter { it.repetitions == selectedRepetitions }
                .mapNotNull { it.effectiveLoadKg(placements.getValue(it.workoutExerciseId).applyPolicySnapshot(exercise)) }.maxOrNull()
            GymGraphMetric.SetVolume -> sessionSets.maxOfOrNull { it.volumeKg(placements.getValue(it.workoutExerciseId).applyPolicySnapshot(exercise), includeWarmups) }
            GymGraphMetric.WorkoutVolume -> sessionSets.sumOf { it.volumeKg(placements.getValue(it.workoutExerciseId).applyPolicySnapshot(exercise), includeWarmups) }
            GymGraphMetric.TotalRepetitions -> sessionSets.sumOf { it.repetitions ?: 0 }.toDouble()
            GymGraphMetric.Distance -> sessionSets.sumOf { it.canonicalDistanceMetres ?: 0.0 }
            GymGraphMetric.Duration -> sessionSets.sumOf { it.durationSeconds ?: 0L }.toDouble()
            GymGraphMetric.Speed -> sessionSets.mapNotNull(WorkoutSet::speedMetresPerSecond).maxOrNull()
            GymGraphMetric.Pace -> sessionSets.mapNotNull(WorkoutSet::paceSecondsPerKilometre).minOrNull()
            GymGraphMetric.MaxMachineSetting -> sessionSets.mapNotNull(WorkoutSet::machineLoadValue).let { values ->
                when (machineLevelDirection) {
                    MachineLevelDirection.HigherNumberMoreResistance -> values.maxOrNull()
                    MachineLevelDirection.HigherNumberLessResistance -> values.minOrNull()
                }
            }
        } ?: return@mapNotNull null
        GymGraphPoint(session.localDate, value, session.id)
    }.sortedBy(GymGraphPoint::date)
    if (aggregation == GymGraphAggregation.Workout) return perSession
    return perSession.groupBy { point ->
        when (aggregation) {
            GymGraphAggregation.Workout -> point.date
            GymGraphAggregation.Week -> point.date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
            GymGraphAggregation.Month -> point.date.withDayOfMonth(1)
        }
    }.map { (date, points) ->
        val value = when (metric) {
            GymGraphMetric.Pace -> points.minOf(GymGraphPoint::value)
            GymGraphMetric.EstimatedOneRepMax,
            GymGraphMetric.MaxWeight,
            GymGraphMetric.MaxRepetitions,
            GymGraphMetric.MaxWeightForReps,
            GymGraphMetric.ActualRepMaxHistory,
            GymGraphMetric.SetVolume,
            GymGraphMetric.Speed,
            -> points.maxOf(GymGraphPoint::value)
            GymGraphMetric.MaxMachineSetting -> when (machineLevelDirection) {
                MachineLevelDirection.HigherNumberMoreResistance -> points.maxOf(GymGraphPoint::value)
                MachineLevelDirection.HigherNumberLessResistance -> points.minOf(GymGraphPoint::value)
            }
            else -> points.sumOf(GymGraphPoint::value)
        }
        GymGraphPoint(date, value, null, points.size)
    }.sortedBy(GymGraphPoint::date)
}

fun <T> downsampleEvenly(values: List<T>, maximumPoints: Int = 200): List<T> {
    require(maximumPoints >= 2) { "At least two points are required" }
    if (values.size <= maximumPoints) return values
    return List(maximumPoints) { index ->
        values[((values.lastIndex.toDouble() * index) / (maximumPoints - 1)).toInt()]
    }
}

fun graphRangeStart(range: GymGraphRange, through: LocalDate): LocalDate? = when (range) {
    GymGraphRange.Month -> through.minusMonths(1)
    GymGraphRange.ThreeMonths -> through.minusMonths(3)
    GymGraphRange.SixMonths -> through.minusMonths(6)
    GymGraphRange.Year -> through.minusYears(1)
    GymGraphRange.All, GymGraphRange.Custom -> null
}

fun buildWeeklyGymSummary(
    weekStart: LocalDate,
    sessions: List<WorkoutSession>,
    workoutExercises: List<WorkoutExercise>,
    sets: List<WorkoutSet>,
    exercises: List<Exercise>,
    personalRecords: List<PersonalRecord>,
): GymWeeklySummary {
    val end = weekStart.plusDays(6)
    val weekSessions = sessions.filter { it.localDate in weekStart..end && it.state == WorkoutSessionState.Finished && !it.archived }
    val sessionIds = weekSessions.mapTo(mutableSetOf(), WorkoutSession::id)
    val weekWorkoutExercises = workoutExercises.filter { it.sessionId in sessionIds }
    val workoutExerciseById = weekWorkoutExercises.associateBy(WorkoutExercise::id)
    val exerciseById = exercises.associateBy(Exercise::id)
    val eligibleSets = sets.filter { it.workoutExerciseId in workoutExerciseById && it.completed && it.deletedAtMillis == null }
    val startMillis = weekStart.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    val endMillis = end.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    return GymWeeklySummary(
        weekStart = weekStart,
        workouts = weekSessions.size,
        trainingDays = weekSessions.map(WorkoutSession::localDate).distinct().size,
        elapsedSeconds = weekSessions.sumOf { ((it.endedAt?.toEpochMilli() ?: it.startedAt.toEpochMilli()) - it.startedAt.toEpochMilli()).coerceAtLeast(0) / 1_000 },
        completedSets = eligibleSets.size,
        repetitions = eligibleSets.sumOf { it.repetitions ?: 0 },
        volumeKg = eligibleSets.sumOf { set ->
            val placement = workoutExerciseById[set.workoutExerciseId]
            val exerciseId = placement?.exerciseId
            val exercise = exerciseById[exerciseId]
            if (exercise == null || placement == null) 0.0 else set.volumeKg(placement.applyPolicySnapshot(exercise))
        },
        newPersonalRecords = personalRecords.count { it.achievedAtMillis in startMillis until endMillis },
    )
}

data class RepMaxEstimate(
    val oneRepMax: Double,
    val percentages: List<Pair<Int, Double>>,
)

fun calculateRepMaxTable(
    weight: Double,
    repetitions: Int,
    formula: EstimatedOneRepMaxFormula = EstimatedOneRepMaxFormula.Epley,
    increment: Double = 2.5,
): RepMaxEstimate? {
    val max = estimatedOneRepMax(weight, repetitions, formula, repCutoff = 36) ?: return null
    val percentages = (50..100 step 5).map { percent ->
        val raw = max * percent / 100.0
        percent to if (increment > 0.0) kotlin.math.round(raw / increment) * increment else raw
    }
    return RepMaxEstimate(max, percentages)
}

data class PlateLoading(
    val targetWeight: Double,
    val achievedWeight: Double,
    val platesPerSide: List<Double>,
    val remainder: Double,
    val exact: Boolean = remainder < 0.0005,
    val perSideLoading: Boolean = true,
)

fun calculatePlateLoading(
    targetWeight: Double,
    barWeight: Double,
    availablePlates: List<Double>,
    plateQuantities: Map<Double, Int> = emptyMap(),
    collarWeight: Double = 0.0,
    perSideLoading: Boolean = true,
): PlateLoading {
    val base = (barWeight + collarWeight).coerceAtLeast(0.0)
    val divisor = if (perSideLoading) 2.0 else 1.0
    val requested = ((targetWeight - base).coerceAtLeast(0.0)) / divisor
    val plates = availablePlates.filter { it.isFinite() && it > 0.0 }.distinct().sortedDescending()
    if (plates.isEmpty()) return PlateLoading(targetWeight, base, emptyList(), abs(targetWeight - base), perSideLoading = perSideLoading)

    // Integerized bounded subset sum finds the closest reachable load. This is
    // deterministic for irregular home-gym inventories and never invents plates.
    val scale = 1_000
    val targetUnits = (requested * scale).toInt()
    val maxPlateUnits = (plates.max() * scale).toInt().coerceAtLeast(1)
    val maximumUnits = targetUnits + maxPlateUnits
    var states = mutableMapOf(0 to emptyList<Double>())
    plates.forEach { plate ->
        val units = (plate * scale).toInt().coerceAtLeast(1)
        val totalAvailable = plateQuantities[plate]
        val usable = if (totalAvailable == null) {
            targetUnits / units + 1
        } else if (perSideLoading) {
            totalAvailable.coerceAtLeast(0) / 2
        } else {
            totalAvailable.coerceAtLeast(0)
        }
        repeat(usable) {
            val additions = states.entries.mapNotNull { (sum, selected) ->
                (sum + units).takeIf { it <= maximumUnits }?.let { next -> next to (selected + plate) }
            }
            additions.forEach { (sum, selected) -> states.putIfAbsent(sum, selected) }
        }
    }
    val best = states.keys.minWithOrNull(compareBy<Int> { kotlin.math.abs(it - targetUnits) }.thenByDescending { it }) ?: 0
    val chosen = states.getValue(best).sortedDescending()
    val achieved = base + chosen.sum() * divisor
    return PlateLoading(
        targetWeight = targetWeight,
        achievedWeight = achieved,
        platesPerSide = chosen,
        remainder = abs(targetWeight - achieved),
        exact = abs(targetWeight - achieved) < 0.0005,
        perSideLoading = perSideLoading,
    )
}
