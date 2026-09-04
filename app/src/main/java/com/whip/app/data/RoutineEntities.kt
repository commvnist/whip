package com.whip.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gym_routines",
    indices = [Index("uuid", unique = true), Index("name"), Index("archived")],
)
data class GymRoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val name: String,
    val notes: String,
    val position: Int,
    val archived: Boolean,
    val pinned: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val programKind: String = "Static",
    val programPhaseCount: Int = 1,
    val programPhaseLabelsCsv: String = "",
    val currentProgramPhaseIndex: Int = 0,
    val currentProgramCycle: Int = 1,
    val nextProgramDayPosition: Int = 0,
    val trainingMaxIncreaseEligible: Boolean = true,
    val programPhaseRolesCsv: String = "",
    val trainingMaxAdvanceAfterPhaseIndicesCsv: String = "",
    val programTemplateKey: String = "None",
    val programTemplateRevision: Int = 0,
    val progressionMode: String = "Standard",
    val allowNonStandardHigherSuggestions: Boolean = false,
)

@Entity(
    tableName = "routine_days",
    foreignKeys = [
        ForeignKey(
            entity = GymRoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("uuid", unique = true), Index("routineId")],
)
data class RoutineDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val routineId: Long,
    val name: String,
    val position: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    /** Persisted cursor for canonical per-day load-multiplier waves. */
    val progressionIndex: Int = 0,
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineDayId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index("uuid", unique = true),
        Index("routineDayId"),
        Index("exerciseId"),
        Index("machineId"),
        Index("machineProfileUuidSnapshot"),
    ],
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val routineDayId: Long,
    val exerciseId: Long,
    val position: Int,
    val notes: String,
    val groupKey: String?,
    val copyPreviousWorkout: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val machineId: Long? = null,
    val equipmentBindingState: String = "None",
    val machineProfileUuidSnapshot: String? = null,
    val machineNameSnapshot: String = "",
    val machineLoadTypeSnapshot: String = "",
    val machineUnitIdSnapshot: String = "",
    val machineLevelLabelSnapshot: String = "",
    val machineLoadInterpretationSnapshot: String = "Total",
    val machineConfigurationGroupSnapshot: String = "",
    val machineConfigurationVersionSnapshot: Int = 1,
    val machineConfigurationSnapshot: String = "",
    val trainingMaxPercent: Double = 90.0,
    val progressionPercentagesCsv: String = "",
    val alternativeExerciseIdsCsv: String = "",
    val trainingMaxKg: Double? = null,
    val trainingMaxValue: Double? = null,
    val trainingMaxUnitId: String = "kilogram",
    val cycleIncrementValue: Double? = null,
    val trainingMaxSource: String = "EstimatedOneRepMaxPercent",
    val trainingMaxBasisKind: String = "Unspecified",
    val trainingMaxBasisValue: Double? = null,
    val trainingMaxBasisUnitId: String = "",
    val trainingMaxIncreaseEligible: Boolean = true,
    val mainWorkScheme: String = "Unspecified",
    val supplementalScheme: String = "None",
    val placementKind: String = "General",
    val assistanceCategory: String = "Unspecified",
    val jokerSetsEnabled: Boolean = false,
)

@Entity(
    tableName = "routine_sets",
    foreignKeys = [
        ForeignKey(
            entity = RoutineExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("uuid", unique = true), Index("routineExerciseId")],
)
data class RoutineSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val routineExerciseId: Long,
    val position: Int,
    val classification: String,
    val enteredWeight: Double?,
    val enteredWeightUnitId: String?,
    val repetitions: Int?,
    val enteredDistance: Double?,
    val enteredDistanceUnitId: String?,
    val durationSeconds: Long?,
    val bodyweightKg: Double?,
    val note: String,
    val rpe: Double?,
    val rir: Double?,
    val tempo: String,
    val restSeconds: Int?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val machineLoadValue: Double? = null,
    val unilateral: Boolean = false,
    val repetitionsMax: Int? = null,
    val loadPrescriptionType: String = "Absolute",
    val loadPercentage: Double? = null,
    /** Null means the set applies in every phase. */
    val routinePhaseIndex: Int? = null,
    val workSection: String = "Unspecified",
    val optionalWorkKind: String = "None",
    val mainWorkScheme: String = "",
    val supplementalScheme: String = "",
)

@Entity(
    tableName = "training_max_decisions",
    indices = [
        Index("uuid", unique = true),
        Index(value = ["routineUuid", "cycle"]),
        Index(value = ["sessionUuid", "exerciseUuid"], unique = true),
        Index("sessionUuid"),
        Index("exerciseUuid"),
    ],
)
data class TrainingMaxDecisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val routineUuid: String,
    val sessionUuid: String,
    val exerciseUuid: String,
    val exerciseName: String,
    val cycle: Int,
    val previousTrainingMax: Double,
    val appliedDelta: Double,
    val resultingTrainingMax: Double,
    val unitId: String,
    val standardDelta: Double,
    val recommendationCategory: String,
    val recommendationDelta: Double,
    val confidence: Double,
    val reasonsText: String,
    val engineVersion: String,
    val action: String,
    val createdAtMillis: Long,
)

@Entity(
    tableName = "personal_records",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkoutSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceSetId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceSessionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("exerciseId"),
        Index("sourceSetId"),
        Index("sourceSessionId"),
        Index(value = ["exerciseId", "type", "current"]),
        Index(value = ["exerciseId", "machineProfileUuidSnapshot"]),
    ],
)
data class PersonalRecordEntity(
    @PrimaryKey val uuid: String,
    val exerciseId: Long,
    val type: String,
    val value: Double,
    val secondaryValue: Double?,
    val unitId: String,
    val sourceSetId: Long?,
    val sourceSessionId: Long?,
    val achievedAtMillis: Long,
    val current: Boolean,
    val imported: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val machineId: Long? = null,
    val machineProfileUuidSnapshot: String? = null,
)

@Entity(
    tableName = "graph_presets",
    indices = [Index("uuid", unique = true), Index("archived")],
)
data class GraphPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val name: String,
    val exerciseIdsCsv: String,
    val measurement: String,
    val dateRange: String,
    val aggregation: String,
    val archived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
