package com.whip.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gym_machines",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("uuid", unique = true), Index("exerciseId"), Index("archived")],
)
data class GymMachineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val exerciseId: Long,
    val name: String,
    val location: String,
    val details: String,
    val loadType: String,
    val unitId: String,
    val levelLabel: String,
    val availableLoadsCsv: String,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val loadInterpretation: String = "Total",
    val baseLoadKg: Double? = null,
    val configurationGroupId: String = "",
    val configurationVersion: Int = 1,
    val seatPosition: String = "",
    val backPosition: String = "",
    val attachment: String = "",
    val pulleyRatio: Double = 1.0,
    val stackMode: String = "Single",
    val addOnPlateKg: Double? = null,
    val stackLabelsCsv: String = "",
    val massMappingCsv: String = "",
    val compatibleForComparison: Boolean = false,
)

@Entity(
    tableName = "exercises",
    indices = [
        Index("uuid", unique = true),
        Index("name"),
        Index("archived"),
        Index("favorite"),
    ],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val name: String,
    val trackingType: String,
    val notes: String,
    val equipment: String,
    val primaryMuscles: String,
    val secondaryMuscles: String,
    val weightUnitId: String,
    val weightIncrement: Double,
    val repetitionIncrement: Int,
    val defaultRestSeconds: Int?,
    val defaultGraphMetric: String,
    val oneRepMaxFormula: String,
    val barWeightKg: Double?,
    val availablePlatesKgCsv: String,
    val includeInVolume: Boolean,
    val includeInPersonalRecords: Boolean,
    val bodyweightLoadPolicy: String,
    val effectiveBodyweightPercent: Double,
    val showRpe: Boolean?,
    val showRir: Boolean?,
    val showTempo: Boolean?,
    val favorite: Boolean,
    val position: Int,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val loadInterpretation: String = "Total",
)

@Entity(
    tableName = "exercise_categories",
    indices = [Index("uuid", unique = true), Index("name"), Index("archived")],
)
data class ExerciseCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val name: String,
    val kind: String,
    val position: Int,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "exercise_category_joins",
    primaryKeys = ["exerciseId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseId"), Index("categoryId")],
)
data class ExerciseCategoryJoinEntity(
    val exerciseId: Long,
    val categoryId: Long,
)

@Entity(
    tableName = "workout_sessions",
    indices = [
        Index("uuid", unique = true),
        Index("state"),
        Index("localEpochDay"),
        Index("archived"),
    ],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val name: String,
    val notes: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val localEpochDay: Long,
    val zoneId: String,
    val state: String,
    val keepScreenAwake: Boolean,
    val restTimerDeadlineMillis: Long?,
    val restTimerDurationSeconds: Int?,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val sourceRoutineId: Long?,
)

@Entity(
    tableName = "workout_groups",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("uuid", unique = true), Index("sessionId")],
)
data class WorkoutGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val sessionId: Long,
    val name: String,
    val type: String,
    val position: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = WorkoutGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("uuid", unique = true),
        Index("sessionId"),
        Index("exerciseId"),
        Index("groupId"),
        Index(value = ["exerciseId", "machineProfileUuidSnapshot"]),
    ],
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val sessionId: Long,
    val exerciseId: Long,
    val position: Int,
    val notes: String,
    val groupId: Long?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    /**
     * Immutable equipment identity used by history, graphs, previous-set lookup, and PRs.
     * machineId is only a nullable link to the editable live profile.
     */
    val machineProfileUuidSnapshot: String? = null,
    val machineId: Long? = null,
    val machineNameSnapshot: String = "",
    val machineLoadTypeSnapshot: String = "",
    val machineUnitIdSnapshot: String = "",
    val machineLevelLabelSnapshot: String = "",
    val loadInterpretationSnapshot: String = "Total",
    val baseLoadKgSnapshot: Double? = null,
    val trackingTypeSnapshot: String = "WeightReps",
    val bodyweightLoadPolicySnapshot: String = "ExternalWeightOnly",
    val effectiveBodyweightPercentSnapshot: Double = 100.0,
    val oneRepMaxFormulaSnapshot: String = "Epley",
    val includeInVolumeSnapshot: Boolean = true,
    val includeInPersonalRecordsSnapshot: Boolean = true,
    val exerciseWeightUnitSnapshot: String = "kilogram",
    val loadMultiplierSnapshot: Double = 1.0,
    val machineConfigurationGroupSnapshot: String = "",
    val machineConfigurationVersionSnapshot: Int = 1,
    val machineConfigurationSnapshot: String = "",
    val machinePulleyRatioSnapshot: Double = 1.0,
    val machineStackModeSnapshot: String = "Single",
    val machineAddOnPlateKgSnapshot: Double? = null,
    val machineMassMappingCsvSnapshot: String = "",
    val alternativeExerciseIdsCsvSnapshot: String = "",
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("uuid", unique = true),
        Index("workoutExerciseId"),
        Index("completedAtMillis"),
        Index("deletedAtMillis"),
    ],
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val workoutExerciseId: Long,
    val position: Int,
    val classification: String,
    val planned: Boolean,
    val completed: Boolean,
    val canonicalWeightKg: Double?,
    val enteredWeight: Double?,
    val enteredWeightUnitId: String?,
    val repetitions: Int?,
    val canonicalDistanceMetres: Double?,
    val enteredDistance: Double?,
    val enteredDistanceUnitId: String?,
    val durationSeconds: Long?,
    val bodyweightKg: Double?,
    val note: String,
    val rpe: Double?,
    val rir: Double?,
    val tempo: String,
    val restSeconds: Int?,
    val completedAtMillis: Long?,
    val deletedAtMillis: Long?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val machineLoadValue: Double? = null,
    val unilateral: Boolean = false,
    val prescribedCanonicalWeightKg: Double? = null,
    val prescribedEnteredWeight: Double? = null,
    val prescribedWeightUnitId: String? = null,
    val prescribedRepetitions: Int? = null,
    val prescribedRpe: Double? = null,
    val prescribedRir: Double? = null,
    val prescribedDurationSeconds: Long? = null,
    val prescribedMachineLoadValue: Double? = null,
    val prescribedRepetitionsMax: Int? = null,
    val prescriptionSourceLabel: String = "",
)
