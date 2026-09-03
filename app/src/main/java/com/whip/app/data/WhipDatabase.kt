package com.whip.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TaskEntity::class,
        TaskOccurrenceEntity::class,
        TaskStepEntity::class,
        TaskStepStateEntity::class,
        TaskStepSnapshotEntity::class,
        UnitDefinitionEntity::class,
        MetricDefinitionEntity::class,
        MetricEntryEntity::class,
        AreaEntity::class,
        TagEntity::class,
        ExerciseEntity::class,
        ExerciseCategoryEntity::class,
        ExerciseCategoryJoinEntity::class,
        GymMachineEntity::class,
        GymMachineExerciseJoinEntity::class,
        WorkoutSessionEntity::class,
        WorkoutGroupEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        GymRoutineEntity::class,
        RoutineDayEntity::class,
        RoutineExerciseEntity::class,
        RoutineSetEntity::class,
        TrainingMaxDecisionEntity::class,
        PersonalRecordEntity::class,
        GraphPresetEntity::class,
        HabitEntity::class,
        HabitChecklistItemEntity::class,
        HabitLogEntity::class,
        HabitChecklistStateEntity::class,
        HabitPauseEntity::class,
        HabitSkipEntity::class,
        HabitTimerSessionEntity::class,
        GoalEntity::class,
        GoalMilestoneEntity::class,
        GoalClosureSnapshotEntity::class,
        GoalElapsedResetEventEntity::class,
        LinkRuleEntity::class,
        ContributionEntity::class,
        TriggerRuleEntity::class,
        TriggerOccurrenceEntity::class,
        LinkRuleConditionEntity::class,
        TriggerRuleConditionEntity::class,
        TriggerFieldMappingEntity::class,
        LinkConditionChoiceEntity::class,
        TriggerConditionChoiceEntity::class,
        TrackEntity::class,
        TrackFieldEntity::class,
        TrackChoiceOptionEntity::class,
        TrackEntryEntity::class,
        TrackValueEntity::class,
        TrackEntrySearchEntity::class,
        TrackCsvImportReceiptEntity::class,
    ],
    version = 42,
    exportSchema = true,
)
abstract class WhipDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun gymDao(): GymDao
    abstract fun routineDao(): RoutineDao
    abstract fun habitDao(): HabitDao
    abstract fun goalDao(): GoalDao
    abstract fun linkDao(): LinkDao
    abstract fun trackDao(): TrackDao

    companion object {
        @Volatile
        private var instance: WhipDatabase? = null

        /** Final consistency boundary for concurrent writers, restored data, and future write paths. */
        val integrityGuardCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) = installIntegrityGuards(db)
            override fun onOpen(db: SupportSQLiteDatabase) = installIntegrityGuards(db)
        }

        fun get(context: Context): WhipDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                WhipDatabase::class.java,
                "whip.db",
            )
                .addCallback(integrityGuardCallback)
                .build()
                .also { instance = it }
        }

        @Synchronized
        fun closeForReset() {
            instance?.close()
            instance = null
        }

        private fun installIntegrityGuards(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS workout_sessions_one_active_insert
                BEFORE INSERT ON workout_sessions
                WHEN NEW.state = 'Active'
                  AND EXISTS (SELECT 1 FROM workout_sessions WHERE state = 'Active')
                BEGIN
                    SELECT RAISE(ABORT, 'Only one active workout is allowed');
                END
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS workout_sessions_one_active_update
                BEFORE UPDATE OF state ON workout_sessions
                WHEN NEW.state = 'Active'
                  AND EXISTS (
                      SELECT 1 FROM workout_sessions
                      WHERE state = 'Active' AND id != NEW.id
                  )
                BEGIN
                    SELECT RAISE(ABORT, 'Only one active workout is allowed');
                END
                """.trimIndent(),
            )
            listOf("INSERT" to "", "UPDATE" to " OF trackId, primaryField, type, required").forEach { (operation, columns) ->
                db.execSQL("DROP TRIGGER IF EXISTS track_fields_valid_primary_${operation.lowercase()}")
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS track_fields_valid_primary_${operation.lowercase()}
                    BEFORE $operation$columns ON track_fields
                    WHEN NEW.primaryField = 1 AND NEW.required != 1
                    BEGIN
                        SELECT RAISE(ABORT, 'Every Track Entry Identity Field must be required');
                    END
                    """.trimIndent(),
                )
            }
            listOf(
                "INSERT" to "",
                "UPDATE" to " OF type, scaleMin, scaleMax, scaleStep",
            ).forEach { (operation, columns) ->
                val triggerName = "track_fields_valid_scale_${operation.lowercase()}"
                db.execSQL("DROP TRIGGER IF EXISTS $triggerName")
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS $triggerName
                    BEFORE $operation$columns ON track_fields
                    WHEN NEW.type = 'Scale' AND (
                        NEW.scaleMin IS NULL OR NEW.scaleMax IS NULL OR NEW.scaleStep IS NULL
                        OR NEW.scaleMin >= NEW.scaleMax OR NEW.scaleStep <= 0
                        OR ((NEW.scaleMax - NEW.scaleMin) / NEW.scaleStep) > 1000
                        OR ABS(
                            ((NEW.scaleMax - NEW.scaleMin) / NEW.scaleStep) -
                            ROUND((NEW.scaleMax - NEW.scaleMin) / NEW.scaleStep)
                        ) > 0.0000001
                        OR EXISTS (
                            SELECT 1 FROM track_values
                            WHERE fieldId = NEW.id AND (
                                scaleValue < NEW.scaleMin OR scaleValue > NEW.scaleMax
                                OR ABS(
                                    ((scaleValue - NEW.scaleMin) / NEW.scaleStep) -
                                    ROUND((scaleValue - NEW.scaleMin) / NEW.scaleStep)
                                ) > 0.0000001
                            )
                        )
                    )
                    BEGIN
                        SELECT RAISE(ABORT, 'Scale range, increment, or existing values are invalid');
                    END
                    """.trimIndent(),
                )
            }
            listOf(
                "INSERT" to "",
                "UPDATE" to " OF entryId, fieldId, textValue, enteredNumber, canonicalNumber, enteredUnitId, dateEpochDay, booleanValue, choiceOptionId, scaleValue",
            ).forEach { (operation, columns) ->
                val triggerName = "track_values_typed_${operation.lowercase()}"
                db.execSQL("DROP TRIGGER IF EXISTS $triggerName")
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS $triggerName
                    BEFORE $operation$columns ON track_values
                    WHEN
                        (SELECT trackId FROM track_entries WHERE id = NEW.entryId) !=
                            (SELECT trackId FROM track_fields WHERE id = NEW.fieldId)
                        OR NOT (
                            ((SELECT type FROM track_fields WHERE id = NEW.fieldId) IN ('ShortText', 'LongText')
                                AND NEW.textValue IS NOT NULL AND NEW.enteredNumber IS NULL AND NEW.canonicalNumber IS NULL
                                AND NEW.enteredUnitId IS NULL AND NEW.dateEpochDay IS NULL AND NEW.booleanValue IS NULL
                                AND NEW.choiceOptionId IS NULL AND NEW.scaleValue IS NULL)
                            OR ((SELECT type FROM track_fields WHERE id = NEW.fieldId) = 'Number'
                                AND NEW.textValue IS NULL AND NEW.enteredNumber IS NOT NULL AND NEW.canonicalNumber IS NOT NULL
                                AND NEW.enteredUnitId IS NOT NULL AND NEW.dateEpochDay IS NULL AND NEW.booleanValue IS NULL
                                AND NEW.choiceOptionId IS NULL AND NEW.scaleValue IS NULL)
                            OR ((SELECT type FROM track_fields WHERE id = NEW.fieldId) = 'SingleChoice'
                                AND NEW.textValue IS NULL AND NEW.enteredNumber IS NULL AND NEW.canonicalNumber IS NULL
                                AND NEW.enteredUnitId IS NULL AND NEW.dateEpochDay IS NULL AND NEW.booleanValue IS NULL
                                AND NEW.choiceOptionId IS NOT NULL AND NEW.scaleValue IS NULL
                                AND (SELECT fieldId FROM track_choice_options WHERE id = NEW.choiceOptionId) = NEW.fieldId)
                            OR ((SELECT type FROM track_fields WHERE id = NEW.fieldId) = 'Scale'
                                AND NEW.textValue IS NULL AND NEW.enteredNumber IS NULL AND NEW.canonicalNumber IS NULL
                                AND NEW.enteredUnitId IS NULL AND NEW.dateEpochDay IS NULL AND NEW.booleanValue IS NULL
                                AND NEW.choiceOptionId IS NULL AND NEW.scaleValue IS NOT NULL
                                AND NEW.scaleValue BETWEEN (SELECT scaleMin FROM track_fields WHERE id = NEW.fieldId)
                                    AND (SELECT scaleMax FROM track_fields WHERE id = NEW.fieldId)
                                AND ABS(
                                    ((NEW.scaleValue - (SELECT scaleMin FROM track_fields WHERE id = NEW.fieldId)) /
                                        (SELECT scaleStep FROM track_fields WHERE id = NEW.fieldId)) -
                                    ROUND((NEW.scaleValue - (SELECT scaleMin FROM track_fields WHERE id = NEW.fieldId)) /
                                        (SELECT scaleStep FROM track_fields WHERE id = NEW.fieldId))
                                ) <= 0.0000001)
                            OR ((SELECT type FROM track_fields WHERE id = NEW.fieldId) = 'Date'
                                AND NEW.textValue IS NULL AND NEW.enteredNumber IS NULL AND NEW.canonicalNumber IS NULL
                                AND NEW.enteredUnitId IS NULL AND NEW.dateEpochDay IS NOT NULL AND NEW.booleanValue IS NULL
                                AND NEW.choiceOptionId IS NULL AND NEW.scaleValue IS NULL)
                            OR ((SELECT type FROM track_fields WHERE id = NEW.fieldId) = 'YesNo'
                                AND NEW.textValue IS NULL AND NEW.enteredNumber IS NULL AND NEW.canonicalNumber IS NULL
                                AND NEW.enteredUnitId IS NULL AND NEW.dateEpochDay IS NULL AND NEW.booleanValue IN (0, 1)
                                AND NEW.choiceOptionId IS NULL AND NEW.scaleValue IS NULL)
                        )
                    BEGIN
                        SELECT RAISE(ABORT, 'Track value does not match its Field type or Track');
                    END
                    """.trimIndent(),
                )
            }
        }
    }
}
