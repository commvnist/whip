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
        WorkoutSessionEntity::class,
        WorkoutGroupEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        GymRoutineEntity::class,
        RoutineDayEntity::class,
        RoutineExerciseEntity::class,
        RoutineSetEntity::class,
        PersonalRecordEntity::class,
        GraphPresetEntity::class,
        HabitEntity::class,
        HabitChecklistItemEntity::class,
        HabitLogEntity::class,
        HabitChecklistStateEntity::class,
        HabitPauseEntity::class,
        GoalEntity::class,
        GoalMilestoneEntity::class,
        LinkRuleEntity::class,
        ContributionEntity::class,
        TriggerRuleEntity::class,
        TriggerOccurrenceEntity::class,
    ],
    version = 1,
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

    companion object {
        @Volatile
        private var instance: WhipDatabase? = null

        /**
         * Repository checks provide friendly errors; these triggers are the final consistency
         * boundary for concurrent writers, restored data, and any future write path.
         */
        val singleActiveWorkoutCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                installSingleActiveWorkoutGuards(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                installSingleActiveWorkoutGuards(db)
            }
        }

        fun get(context: Context): WhipDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                WhipDatabase::class.java,
                "whip.db",
            )
                .addCallback(singleActiveWorkoutCallback)
                .build()
                .also { instance = it }
        }

        private fun installSingleActiveWorkoutGuards(db: SupportSQLiteDatabase) {
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
        }
    }
}
