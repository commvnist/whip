package com.whip.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
        EntityTagLinkEntity::class,
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
        GoalCompletionSnapshotEntity::class,
        LinkRuleEntity::class,
        ContributionEntity::class,
        TriggerRuleEntity::class,
        TriggerOccurrenceEntity::class,
    ],
    version = 27,
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

        fun get(context: Context): WhipDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                WhipDatabase::class.java,
                "whip.db",
            )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19,
                    MIGRATION_19_20,
                    MIGRATION_20_21,
                    MIGRATION_21_22,
                    MIGRATION_22_23,
                    MIGRATION_23_24,
                    MIGRATION_24_25,
                    MIGRATION_25_26,
                    MIGRATION_26_27,
                )
                .build()
                .also { instance = it }
        }
    }
}

val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE areas ADD COLUMN nameKey TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE areas SET name = trim(name), nameKey = lower(trim(name))")
        db.execSQL("DELETE FROM areas WHERE nameKey = ''")
        db.execSQL(
            "DELETE FROM areas WHERE rowid NOT IN " +
                "(SELECT MIN(rowid) FROM areas GROUP BY nameKey)",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_areas_nameKey ON areas(nameKey)")

        listOf("tasks", "habits", "goals").forEach { table ->
            db.execSQL(
                "INSERT OR IGNORE INTO areas " +
                    "(id, name, nameKey, colorArgb, position, archived, createdAtMillis, updatedAtMillis) " +
                    "SELECT 'legacy-area-' || lower(hex(randomblob(16))), trim(area), lower(trim(area)), " +
                    "NULL, 100000, 0, 0, 0 FROM $table WHERE trim(area) <> '' GROUP BY lower(trim(area))",
            )
        }

        db.execSQL("ALTER TABLE tasks ADD COLUMN areaId TEXT REFERENCES areas(id) ON DELETE RESTRICT")
        db.execSQL("ALTER TABLE habits ADD COLUMN areaId TEXT REFERENCES areas(id) ON DELETE RESTRICT")
        db.execSQL("ALTER TABLE goals ADD COLUMN areaId TEXT REFERENCES areas(id) ON DELETE RESTRICT")

        listOf("tasks", "habits", "goals").forEach { table ->
            db.execSQL(
                "UPDATE $table SET areaId = " +
                    "(SELECT id FROM areas WHERE nameKey = lower(trim($table.area)) LIMIT 1) " +
                    "WHERE trim(area) <> ''",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_${table}_areaId ON $table(areaId)")
            db.execSQL(
                "UPDATE $table SET area = COALESCE((SELECT name FROM areas WHERE id = $table.areaId), '')",
            )
        }
    }
}

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE tasks SET uuid = 'legacy-task-' || id WHERE uuid = ''")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tasks_uuid ON tasks(uuid)")
        db.execSQL("ALTER TABLE task_steps ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE task_steps SET uuid = 'legacy-task-step-' || id WHERE uuid = ''")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_task_steps_uuid ON task_steps(uuid)")
        db.execSQL("ALTER TABLE tasks ADD COLUMN manualPosition INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE tasks SET manualPosition = id")
    }
}

val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN trainingMaxPercent REAL NOT NULL DEFAULT 90")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN progressionPercentagesCsv TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN alternativeExerciseIdsCsv TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE routine_sets ADD COLUMN loadPrescriptionType TEXT NOT NULL DEFAULT 'Absolute'")
        db.execSQL("ALTER TABLE routine_sets ADD COLUMN loadPercentage REAL")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN alternativeExerciseIdsCsvSnapshot TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE workout_sets ADD COLUMN prescriptionSourceLabel TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE unit_definitions ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_unit_definitions_archived ON unit_definitions(archived)")
    }
}

val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE routine_sets ADD COLUMN repetitionsMax INTEGER")
        db.execSQL("ALTER TABLE workout_sets ADD COLUMN prescribedRepetitionsMax INTEGER")
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN machineProfileUuidSnapshot TEXT")
        db.execSQL(
            """
            UPDATE workout_exercises
            SET machineProfileUuidSnapshot = CASE
                WHEN machineId IS NULL THEN NULL
                ELSE COALESCE(
                    (SELECT uuid FROM gym_machines WHERE gym_machines.id = workout_exercises.machineId),
                    'legacy-machine-id:' || machineId
                )
            END
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_workout_exercises_exerciseId_machineProfileUuidSnapshot " +
                "ON workout_exercises(exerciseId, machineProfileUuidSnapshot)",
        )
        db.execSQL(
            "UPDATE workout_exercises SET machineId = NULL WHERE machineId IS NOT NULL " +
                "AND NOT EXISTS(SELECT 1 FROM gym_machines WHERE gym_machines.id = workout_exercises.machineId)",
        )

        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN equipmentBindingState TEXT NOT NULL DEFAULT 'None'")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN machineProfileUuidSnapshot TEXT")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN machineNameSnapshot TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN machineLoadTypeSnapshot TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN machineUnitIdSnapshot TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN machineLevelLabelSnapshot TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN machineLoadInterpretationSnapshot TEXT NOT NULL DEFAULT 'Total'")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN machineConfigurationGroupSnapshot TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN machineConfigurationVersionSnapshot INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN machineConfigurationSnapshot TEXT NOT NULL DEFAULT ''")
        db.execSQL(
            """
            UPDATE routine_exercises SET
                equipmentBindingState = CASE
                    WHEN machineId IS NULL THEN 'None'
                    WHEN EXISTS(SELECT 1 FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId) THEN 'Resolved'
                    ELSE 'NeedsEquipment'
                END,
                machineProfileUuidSnapshot = COALESCE(
                    (SELECT uuid FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId),
                    CASE WHEN machineId IS NULL THEN NULL ELSE 'legacy-machine-id:' || machineId END
                ),
                machineNameSnapshot = COALESCE((SELECT CASE WHEN location = '' THEN name ELSE name || ' · ' || location END FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId), ''),
                machineLoadTypeSnapshot = COALESCE((SELECT loadType FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId), ''),
                machineUnitIdSnapshot = COALESCE((SELECT unitId FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId), ''),
                machineLevelLabelSnapshot = COALESCE((SELECT levelLabel FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId), ''),
                machineLoadInterpretationSnapshot = COALESCE((SELECT loadInterpretation FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId), 'Total'),
                machineConfigurationGroupSnapshot = COALESCE((SELECT configurationGroupId FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId), ''),
                machineConfigurationVersionSnapshot = COALESCE((SELECT configurationVersion FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId), 1),
                machineConfigurationSnapshot = COALESCE((SELECT trim(
                    CASE WHEN seatPosition = '' THEN '' ELSE 'Seat ' || seatPosition END ||
                    CASE WHEN backPosition = '' THEN '' ELSE CASE WHEN seatPosition = '' THEN '' ELSE ' · ' END || 'Back ' || backPosition END ||
                    CASE WHEN attachment = '' THEN '' ELSE CASE WHEN seatPosition = '' AND backPosition = '' THEN '' ELSE ' · ' END || attachment END
                ) FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId), '')
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_exercises_machineId ON routine_exercises(machineId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_exercises_machineProfileUuidSnapshot ON routine_exercises(machineProfileUuidSnapshot)")
        db.execSQL(
            "UPDATE routine_exercises SET machineId = NULL WHERE machineId IS NOT NULL " +
                "AND NOT EXISTS(SELECT 1 FROM gym_machines WHERE gym_machines.id = routine_exercises.machineId)",
        )

        db.execSQL("ALTER TABLE personal_records ADD COLUMN machineProfileUuidSnapshot TEXT")
        db.execSQL(
            """
            UPDATE personal_records
            SET machineProfileUuidSnapshot = COALESCE(
                (SELECT uuid FROM gym_machines WHERE gym_machines.id = personal_records.machineId),
                (SELECT we.machineProfileUuidSnapshot FROM workout_sets ws
                    JOIN workout_exercises we ON we.id = ws.workoutExerciseId
                    WHERE ws.id = personal_records.sourceSetId),
                CASE WHEN machineId IS NULL THEN NULL ELSE 'legacy-machine-id:' || machineId END
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_personal_records_exerciseId_machineProfileUuidSnapshot " +
                "ON personal_records(exerciseId, machineProfileUuidSnapshot)",
        )
        db.execSQL(
            "UPDATE personal_records SET machineId = NULL WHERE machineId IS NOT NULL " +
                "AND NOT EXISTS(SELECT 1 FROM gym_machines WHERE gym_machines.id = personal_records.machineId)",
        )
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE habits ADD COLUMN sourceMetricId TEXT")
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN inbox INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tasks ADD COLUMN durationMinutes INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN effort TEXT NOT NULL DEFAULT 'Moderate'")
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE gym_machines ADD COLUMN configurationGroupId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE gym_machines ADD COLUMN configurationVersion INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE gym_machines ADD COLUMN seatPosition TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE gym_machines ADD COLUMN backPosition TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE gym_machines ADD COLUMN attachment TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE gym_machines ADD COLUMN pulleyRatio REAL NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE gym_machines ADD COLUMN stackMode TEXT NOT NULL DEFAULT 'Single'")
        db.execSQL("ALTER TABLE gym_machines ADD COLUMN addOnPlateKg REAL")
        db.execSQL("ALTER TABLE gym_machines ADD COLUMN stackLabelsCsv TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE gym_machines ADD COLUMN massMappingCsv TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE gym_machines ADD COLUMN compatibleForComparison INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE gym_machines SET configurationGroupId = uuid WHERE configurationGroupId = ''")

        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN exerciseWeightUnitSnapshot TEXT NOT NULL DEFAULT 'kilogram'")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN loadMultiplierSnapshot REAL NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN machineConfigurationGroupSnapshot TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN machineConfigurationVersionSnapshot INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN machineConfigurationSnapshot TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN machinePulleyRatioSnapshot REAL NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN machineStackModeSnapshot TEXT NOT NULL DEFAULT 'Single'")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN machineAddOnPlateKgSnapshot REAL")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN machineMassMappingCsvSnapshot TEXT NOT NULL DEFAULT ''")
        db.execSQL(
            """
            UPDATE workout_exercises SET
                exerciseWeightUnitSnapshot = COALESCE((SELECT weightUnitId FROM exercises WHERE exercises.id = workout_exercises.exerciseId), 'kilogram'),
                loadMultiplierSnapshot = CASE WHEN loadInterpretationSnapshot IN ('PerHand', 'PerSide') THEN 2 ELSE 1 END,
                machineConfigurationGroupSnapshot = COALESCE((SELECT configurationGroupId FROM gym_machines WHERE gym_machines.id = workout_exercises.machineId), ''),
                machineConfigurationVersionSnapshot = COALESCE((SELECT configurationVersion FROM gym_machines WHERE gym_machines.id = workout_exercises.machineId), 1),
                machinePulleyRatioSnapshot = COALESCE((SELECT pulleyRatio FROM gym_machines WHERE gym_machines.id = workout_exercises.machineId), 1),
                machineStackModeSnapshot = COALESCE((SELECT stackMode FROM gym_machines WHERE gym_machines.id = workout_exercises.machineId), 'Single'),
                machineAddOnPlateKgSnapshot = (SELECT addOnPlateKg FROM gym_machines WHERE gym_machines.id = workout_exercises.machineId),
                machineMassMappingCsvSnapshot = COALESCE((SELECT massMappingCsv FROM gym_machines WHERE gym_machines.id = workout_exercises.machineId), '')
            """.trimIndent(),
        )

        db.execSQL("ALTER TABLE workout_sets ADD COLUMN unilateral INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE workout_sets ADD COLUMN prescribedCanonicalWeightKg REAL")
        db.execSQL("ALTER TABLE workout_sets ADD COLUMN prescribedEnteredWeight REAL")
        db.execSQL("ALTER TABLE workout_sets ADD COLUMN prescribedWeightUnitId TEXT")
        db.execSQL("ALTER TABLE workout_sets ADD COLUMN prescribedRepetitions INTEGER")
        db.execSQL("ALTER TABLE workout_sets ADD COLUMN prescribedRpe REAL")
        db.execSQL("ALTER TABLE workout_sets ADD COLUMN prescribedRir REAL")
        db.execSQL("ALTER TABLE workout_sets ADD COLUMN prescribedDurationSeconds INTEGER")
        db.execSQL("ALTER TABLE workout_sets ADD COLUMN prescribedMachineLoadValue REAL")
        db.execSQL(
            """
            UPDATE workout_sets SET
                prescribedCanonicalWeightKg = canonicalWeightKg,
                prescribedEnteredWeight = enteredWeight,
                prescribedWeightUnitId = enteredWeightUnitId,
                prescribedRepetitions = repetitions,
                prescribedRpe = rpe,
                prescribedRir = rir,
                prescribedDurationSeconds = durationSeconds,
                prescribedMachineLoadValue = machineLoadValue
            WHERE planned = 1
            """.trimIndent(),
        )
        db.execSQL("ALTER TABLE routine_sets ADD COLUMN unilateral INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN trackingTypeSnapshot TEXT NOT NULL DEFAULT 'WeightReps'")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN bodyweightLoadPolicySnapshot TEXT NOT NULL DEFAULT 'ExternalWeightOnly'")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN effectiveBodyweightPercentSnapshot REAL NOT NULL DEFAULT 100")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN oneRepMaxFormulaSnapshot TEXT NOT NULL DEFAULT 'Epley'")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN includeInVolumeSnapshot INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN includeInPersonalRecordsSnapshot INTEGER NOT NULL DEFAULT 1")
        db.execSQL(
            """
            UPDATE workout_exercises SET
                trackingTypeSnapshot = COALESCE((SELECT trackingType FROM exercises WHERE exercises.id = workout_exercises.exerciseId), 'WeightReps'),
                bodyweightLoadPolicySnapshot = COALESCE((SELECT bodyweightLoadPolicy FROM exercises WHERE exercises.id = workout_exercises.exerciseId), 'ExternalWeightOnly'),
                effectiveBodyweightPercentSnapshot = COALESCE((SELECT effectiveBodyweightPercent FROM exercises WHERE exercises.id = workout_exercises.exerciseId), 100),
                oneRepMaxFormulaSnapshot = COALESCE((SELECT oneRepMaxFormula FROM exercises WHERE exercises.id = workout_exercises.exerciseId), 'Epley'),
                includeInVolumeSnapshot = COALESCE((SELECT includeInVolume FROM exercises WHERE exercises.id = workout_exercises.exerciseId), 1),
                includeInPersonalRecordsSnapshot = COALESCE((SELECT includeInPersonalRecords FROM exercises WHERE exercises.id = workout_exercises.exerciseId), 1)
            """.trimIndent(),
        )
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN missedOccurrencePolicy TEXT NOT NULL DEFAULT 'KeepLatest'")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN loadInterpretation TEXT NOT NULL DEFAULT 'Total'")
        db.execSQL("ALTER TABLE gym_machines ADD COLUMN loadInterpretation TEXT NOT NULL DEFAULT 'Total'")
        db.execSQL("ALTER TABLE gym_machines ADD COLUMN baseLoadKg REAL")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN loadInterpretationSnapshot TEXT NOT NULL DEFAULT 'Total'")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN baseLoadKgSnapshot REAL")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS gym_machines (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                exerciseId INTEGER NOT NULL,
                name TEXT NOT NULL,
                location TEXT NOT NULL,
                details TEXT NOT NULL,
                loadType TEXT NOT NULL,
                unitId TEXT NOT NULL,
                levelLabel TEXT NOT NULL,
                availableLoadsCsv TEXT NOT NULL,
                archived INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_gym_machines_uuid ON gym_machines(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_machines_exerciseId ON gym_machines(exerciseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_machines_archived ON gym_machines(archived)")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN machineId INTEGER")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN machineNameSnapshot TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN machineLoadTypeSnapshot TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN machineUnitIdSnapshot TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE workout_exercises ADD COLUMN machineLevelLabelSnapshot TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE workout_sets ADD COLUMN machineLoadValue REAL")
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN machineId INTEGER")
        db.execSQL("ALTER TABLE routine_sets ADD COLUMN machineLoadValue REAL")
        db.execSQL("ALTER TABLE personal_records ADD COLUMN machineId INTEGER")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN priority TEXT NOT NULL DEFAULT 'None'")
        db.execSQL("ALTER TABLE tasks ADD COLUMN area TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE tasks ADD COLUMN tagsCsv TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE tasks ADD COLUMN deadlineEpochDay INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceAnchor TEXT NOT NULL DEFAULT 'Schedule'")
        db.execSQL("ALTER TABLE tasks ADD COLUMN reminderOffsetsMinutesCsv TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE tasks SET reminderOffsetsMinutesCsv = '0' WHERE reminderEnabled = 1")
        db.execSQL("ALTER TABLE tasks ADD COLUMN locationReminderEnabled INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE tasks ADD COLUMN locationName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE tasks ADD COLUMN locationLatitude REAL")
        db.execSQL("ALTER TABLE tasks ADD COLUMN locationLongitude REAL")
        db.execSQL("ALTER TABLE tasks ADD COLUMN locationRadiusMeters REAL NOT NULL DEFAULT 150")
        db.execSQL("ALTER TABLE tasks ADD COLUMN locationTrigger TEXT NOT NULL DEFAULT 'Arrive'")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE task_steps ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE task_step_snapshots ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
        // Weighted subtasks were never part of the intended product. Normalize legacy rows
        // while retaining the column so older backups remain importable.
        db.execSQL("UPDATE task_steps SET weight = 1.0")
        db.execSQL("UPDATE task_step_snapshots SET weight = 1.0")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goals ADD COLUMN aggregationPeriod TEXT NOT NULL DEFAULT 'All'")
        db.execSQL("ALTER TABLE goals ADD COLUMN rollingDays INTEGER")
        db.execSQL("ALTER TABLE goals ADD COLUMN consistencyPeriod TEXT NOT NULL DEFAULT 'Week'")
        db.execSQL("ALTER TABLE goals ADD COLUMN consistencyRequiredPeriods INTEGER")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE gym_routines ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE workout_sessions ADD COLUMN sourceRoutineId INTEGER")
        db.execSQL("ALTER TABLE habits ADD COLUMN area TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE habits ADD COLUMN tagsCsv TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE goals ADD COLUMN area TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE goals ADD COLUMN tagsCsv TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE habits ADD COLUMN weekdayReminderMinutesCsv TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS link_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                name TEXT NOT NULL,
                kind TEXT NOT NULL,
                sourceType TEXT NOT NULL,
                sourceEntityId INTEGER,
                sourceMetricId TEXT,
                sourceItemId INTEGER,
                sourceMetric TEXT NOT NULL,
                targetGoalId INTEGER NOT NULL,
                targetMilestoneId INTEGER,
                valueMode TEXT NOT NULL,
                fixedValue REAL,
                multiplier REAL NOT NULL,
                offset REAL NOT NULL,
                retroactiveFromEpochDay INTEGER,
                enabled INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                FOREIGN KEY(targetGoalId) REFERENCES goals(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(targetMilestoneId) REFERENCES goal_milestones(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_link_rules_uuid ON link_rules(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_link_rules_targetGoalId ON link_rules(targetGoalId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_link_rules_targetMilestoneId ON link_rules(targetMilestoneId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_link_rules_sourceType_sourceEntityId ON link_rules(sourceType, sourceEntityId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS contributions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                linkRuleId INTEGER NOT NULL,
                sourceEventId TEXT NOT NULL,
                sourceType TEXT NOT NULL,
                sourceEntityId INTEGER,
                targetGoalId INTEGER NOT NULL,
                metricEntryId TEXT,
                canonicalValue REAL,
                localEpochDay INTEGER NOT NULL,
                timestampMillis INTEGER NOT NULL,
                excluded INTEGER NOT NULL,
                overrideValue REAL,
                explanation TEXT NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                FOREIGN KEY(linkRuleId) REFERENCES link_rules(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(targetGoalId) REFERENCES goals(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_contributions_uuid ON contributions(uuid)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_contributions_linkRuleId_sourceEventId ON contributions(linkRuleId, sourceEventId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_contributions_targetGoalId ON contributions(targetGoalId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_contributions_metricEntryId ON contributions(metricEntryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_contributions_sourceType_sourceEntityId ON contributions(sourceType, sourceEntityId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS trigger_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                name TEXT NOT NULL,
                sourceType TEXT NOT NULL,
                sourceEntityId INTEGER NOT NULL,
                outcome TEXT NOT NULL,
                targetType TEXT NOT NULL,
                targetEntityId INTEGER NOT NULL,
                delayMinutes INTEGER NOT NULL,
                quietStartMinutes INTEGER,
                quietEndMinutes INTEGER,
                autoCompleteTargetHabit INTEGER NOT NULL,
                enabled INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_trigger_rules_uuid ON trigger_rules(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_trigger_rules_sourceType_sourceEntityId ON trigger_rules(sourceType, sourceEntityId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_trigger_rules_targetType_targetEntityId ON trigger_rules(targetType, targetEntityId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS trigger_occurrences (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                triggerRuleId INTEGER NOT NULL,
                sourceEventId TEXT NOT NULL,
                availableAtMillis INTEGER NOT NULL,
                deliveredAtMillis INTEGER,
                dismissedAtMillis INTEGER,
                FOREIGN KEY(triggerRuleId) REFERENCES trigger_rules(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_trigger_occurrences_triggerRuleId_sourceEventId ON trigger_occurrences(triggerRuleId, sourceEventId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_trigger_occurrences_availableAtMillis ON trigger_occurrences(availableAtMillis)")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                metricId TEXT NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                icon TEXT NOT NULL,
                colorArgb INTEGER,
                type TEXT NOT NULL,
                dimension TEXT NOT NULL,
                unitId TEXT NOT NULL,
                precision INTEGER NOT NULL,
                baseline REAL,
                targetMin REAL,
                targetMax REAL,
                direction TEXT NOT NULL,
                startEpochDay INTEGER NOT NULL,
                deadlineEpochDay INTEGER,
                aggregation TEXT NOT NULL,
                entryMode TEXT NOT NULL,
                paceType TEXT NOT NULL,
                reminderMinutes INTEGER,
                status TEXT NOT NULL,
                pinned INTEGER NOT NULL,
                position INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_goals_uuid ON goals(uuid)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_goals_metricId ON goals(metricId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_goals_status ON goals(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_goals_pinned ON goals(pinned)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS goal_milestones (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                goalId INTEGER NOT NULL,
                name TEXT NOT NULL,
                position INTEGER NOT NULL,
                weight REAL NOT NULL,
                targetValue REAL,
                completed INTEGER NOT NULL,
                completedAtMillis INTEGER,
                linkedTaskId INTEGER,
                reward TEXT NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                FOREIGN KEY(goalId) REFERENCES goals(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(linkedTaskId) REFERENCES tasks(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_goal_milestones_uuid ON goal_milestones(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_milestones_goalId ON goal_milestones(goalId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_milestones_linkedTaskId ON goal_milestones(linkedTaskId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS goal_completion_snapshots (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                goalId INTEGER NOT NULL,
                completedAtMillis INTEGER NOT NULL,
                value REAL,
                progress REAL,
                status TEXT NOT NULL,
                FOREIGN KEY(goalId) REFERENCES goals(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_completion_snapshots_goalId ON goal_completion_snapshots(goalId)")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS habits (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                metricId TEXT NOT NULL,
                name TEXT NOT NULL,
                notes TEXT NOT NULL,
                icon TEXT NOT NULL,
                colorArgb INTEGER,
                intent TEXT NOT NULL,
                trackingMode TEXT NOT NULL,
                dimension TEXT NOT NULL,
                unitId TEXT NOT NULL,
                precision INTEGER NOT NULL,
                comparison TEXT NOT NULL,
                targetMin REAL,
                targetMax REAL,
                targetPeriod TEXT NOT NULL,
                rollingDays INTEGER,
                scheduleType TEXT NOT NULL,
                scheduleInterval INTEGER NOT NULL,
                weekdaysMask INTEGER NOT NULL,
                flexibleTimesPerWeek INTEGER,
                startEpochDay INTEGER NOT NULL,
                endType TEXT NOT NULL,
                endEpochDay INTEGER,
                endValue REAL,
                timeWindowStartMinutes INTEGER,
                timeWindowEndMinutes INTEGER,
                quickIncrement REAL NOT NULL,
                quickActionsCsv TEXT NOT NULL,
                reminderMinutesCsv TEXT NOT NULL,
                weekStart TEXT NOT NULL,
                avoidMissingPolicy TEXT NOT NULL,
                timerStartedAtMillis INTEGER,
                pinned INTEGER NOT NULL,
                position INTEGER NOT NULL,
                archived INTEGER NOT NULL,
                paused INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_habits_uuid ON habits(uuid)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_habits_metricId ON habits(metricId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_habits_archived ON habits(archived)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_habits_pinned ON habits(pinned)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS habit_checklist_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                habitId INTEGER NOT NULL,
                name TEXT NOT NULL,
                position INTEGER NOT NULL,
                archived INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_habit_checklist_items_uuid ON habit_checklist_items(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_checklist_items_habitId ON habit_checklist_items(habitId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS habit_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                habitId INTEGER NOT NULL,
                value REAL,
                canonicalValue REAL,
                enteredUnitId TEXT,
                status TEXT NOT NULL,
                timestampMillis INTEGER NOT NULL,
                localEpochDay INTEGER NOT NULL,
                zoneId TEXT NOT NULL,
                offsetSeconds INTEGER NOT NULL,
                note TEXT NOT NULL,
                sourceType TEXT NOT NULL,
                sourceId TEXT,
                metricEntryId TEXT,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_habit_logs_uuid ON habit_logs(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_logs_habitId ON habit_logs(habitId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_logs_habitId_localEpochDay ON habit_logs(habitId, localEpochDay)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_logs_sourceType_sourceId ON habit_logs(sourceType, sourceId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS habit_checklist_states (
                habitId INTEGER NOT NULL,
                itemId INTEGER NOT NULL,
                localEpochDay INTEGER NOT NULL,
                completed INTEGER NOT NULL,
                completedAtMillis INTEGER,
                nameSnapshot TEXT NOT NULL,
                PRIMARY KEY(habitId, itemId, localEpochDay),
                FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(itemId) REFERENCES habit_checklist_items(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_checklist_states_habitId ON habit_checklist_states(habitId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_checklist_states_itemId ON habit_checklist_states(itemId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_checklist_states_habitId_localEpochDay ON habit_checklist_states(habitId, localEpochDay)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS habit_pauses (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                habitId INTEGER NOT NULL,
                startEpochDay INTEGER NOT NULL,
                endEpochDay INTEGER,
                note TEXT NOT NULL,
                FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_pauses_habitId ON habit_pauses(habitId)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS gym_routines (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                name TEXT NOT NULL,
                notes TEXT NOT NULL,
                position INTEGER NOT NULL,
                archived INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_gym_routines_uuid ON gym_routines(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_routines_name ON gym_routines(name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_routines_archived ON gym_routines(archived)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS routine_days (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                routineId INTEGER NOT NULL,
                name TEXT NOT NULL,
                position INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                FOREIGN KEY(routineId) REFERENCES gym_routines(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_routine_days_uuid ON routine_days(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_days_routineId ON routine_days(routineId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS routine_exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                routineDayId INTEGER NOT NULL,
                exerciseId INTEGER NOT NULL,
                position INTEGER NOT NULL,
                notes TEXT NOT NULL,
                groupKey TEXT,
                copyPreviousWorkout INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                FOREIGN KEY(routineDayId) REFERENCES routine_days(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_routine_exercises_uuid ON routine_exercises(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_exercises_routineDayId ON routine_exercises(routineDayId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_exercises_exerciseId ON routine_exercises(exerciseId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS routine_sets (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                routineExerciseId INTEGER NOT NULL,
                position INTEGER NOT NULL,
                classification TEXT NOT NULL,
                enteredWeight REAL,
                enteredWeightUnitId TEXT,
                repetitions INTEGER,
                enteredDistance REAL,
                enteredDistanceUnitId TEXT,
                durationSeconds INTEGER,
                bodyweightKg REAL,
                note TEXT NOT NULL,
                rpe REAL,
                rir REAL,
                tempo TEXT NOT NULL,
                restSeconds INTEGER,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                FOREIGN KEY(routineExerciseId) REFERENCES routine_exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_routine_sets_uuid ON routine_sets(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_sets_routineExerciseId ON routine_sets(routineExerciseId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS personal_records (
                uuid TEXT NOT NULL PRIMARY KEY,
                exerciseId INTEGER NOT NULL,
                type TEXT NOT NULL,
                value REAL NOT NULL,
                secondaryValue REAL,
                unitId TEXT NOT NULL,
                sourceSetId INTEGER,
                sourceSessionId INTEGER,
                achievedAtMillis INTEGER NOT NULL,
                current INTEGER NOT NULL,
                imported INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(sourceSetId) REFERENCES workout_sets(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(sourceSessionId) REFERENCES workout_sessions(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_personal_records_exerciseId ON personal_records(exerciseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_personal_records_sourceSetId ON personal_records(sourceSetId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_personal_records_sourceSessionId ON personal_records(sourceSessionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_personal_records_exerciseId_type_current ON personal_records(exerciseId, type, current)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS graph_presets (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                name TEXT NOT NULL,
                exerciseIdsCsv TEXT NOT NULL,
                metric TEXT NOT NULL,
                dateRange TEXT NOT NULL,
                aggregation TEXT NOT NULL,
                archived INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_graph_presets_uuid ON graph_presets(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_graph_presets_archived ON graph_presets(archived)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                name TEXT NOT NULL,
                trackingType TEXT NOT NULL,
                notes TEXT NOT NULL,
                equipment TEXT NOT NULL,
                primaryMuscles TEXT NOT NULL,
                secondaryMuscles TEXT NOT NULL,
                weightUnitId TEXT NOT NULL,
                weightIncrement REAL NOT NULL,
                repetitionIncrement INTEGER NOT NULL,
                defaultRestSeconds INTEGER,
                defaultGraphMetric TEXT NOT NULL,
                oneRepMaxFormula TEXT NOT NULL,
                barWeightKg REAL,
                availablePlatesKgCsv TEXT NOT NULL,
                includeInVolume INTEGER NOT NULL,
                includeInPersonalRecords INTEGER NOT NULL,
                bodyweightLoadPolicy TEXT NOT NULL,
                effectiveBodyweightPercent REAL NOT NULL,
                showRpe INTEGER,
                showRir INTEGER,
                showTempo INTEGER,
                favorite INTEGER NOT NULL,
                position INTEGER NOT NULL,
                archived INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_exercises_uuid ON exercises(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercises_name ON exercises(name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercises_archived ON exercises(archived)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercises_favorite ON exercises(favorite)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS exercise_categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                name TEXT NOT NULL,
                kind TEXT NOT NULL,
                colorArgb INTEGER,
                position INTEGER NOT NULL,
                archived INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_exercise_categories_uuid ON exercise_categories(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_categories_name ON exercise_categories(name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_categories_archived ON exercise_categories(archived)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS exercise_category_joins (
                exerciseId INTEGER NOT NULL,
                categoryId INTEGER NOT NULL,
                PRIMARY KEY(exerciseId, categoryId),
                FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(categoryId) REFERENCES exercise_categories(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_category_joins_exerciseId ON exercise_category_joins(exerciseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_category_joins_categoryId ON exercise_category_joins(categoryId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS workout_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                name TEXT NOT NULL,
                notes TEXT NOT NULL,
                startedAtMillis INTEGER NOT NULL,
                endedAtMillis INTEGER,
                localEpochDay INTEGER NOT NULL,
                zoneId TEXT NOT NULL,
                state TEXT NOT NULL,
                keepScreenAwake INTEGER NOT NULL,
                restTimerDeadlineMillis INTEGER,
                restTimerDurationSeconds INTEGER,
                archived INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_workout_sessions_uuid ON workout_sessions(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_state ON workout_sessions(state)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_localEpochDay ON workout_sessions(localEpochDay)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_archived ON workout_sessions(archived)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS workout_groups (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                sessionId INTEGER NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                position INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_workout_groups_uuid ON workout_groups(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_groups_sessionId ON workout_groups(sessionId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS workout_exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                sessionId INTEGER NOT NULL,
                exerciseId INTEGER NOT NULL,
                position INTEGER NOT NULL,
                notes TEXT NOT NULL,
                groupId INTEGER,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(groupId) REFERENCES workout_groups(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_workout_exercises_uuid ON workout_exercises(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_exercises_sessionId ON workout_exercises(sessionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_exercises_exerciseId ON workout_exercises(exerciseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_exercises_groupId ON workout_exercises(groupId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS workout_sets (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uuid TEXT NOT NULL,
                workoutExerciseId INTEGER NOT NULL,
                position INTEGER NOT NULL,
                classification TEXT NOT NULL,
                planned INTEGER NOT NULL,
                completed INTEGER NOT NULL,
                canonicalWeightKg REAL,
                enteredWeight REAL,
                enteredWeightUnitId TEXT,
                repetitions INTEGER,
                canonicalDistanceMetres REAL,
                enteredDistance REAL,
                enteredDistanceUnitId TEXT,
                durationSeconds INTEGER,
                bodyweightKg REAL,
                note TEXT NOT NULL,
                rpe REAL,
                rir REAL,
                tempo TEXT NOT NULL,
                restSeconds INTEGER,
                completedAtMillis INTEGER,
                deletedAtMillis INTEGER,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                FOREIGN KEY(workoutExerciseId) REFERENCES workout_exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_workout_sets_uuid ON workout_sets(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sets_workoutExerciseId ON workout_sets(workoutExerciseId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sets_completedAtMillis ON workout_sets(completedAtMillis)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sets_deletedAtMillis ON workout_sets(deletedAtMillis)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS unit_definitions (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                symbol TEXT NOT NULL,
                dimension TEXT NOT NULL,
                toCanonicalFactor REAL NOT NULL,
                toCanonicalOffset REAL NOT NULL,
                custom INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS metric_definitions (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                valueKind TEXT NOT NULL,
                dimension TEXT NOT NULL,
                defaultUnitId TEXT NOT NULL,
                precision INTEGER NOT NULL,
                dimensionLocked INTEGER NOT NULL,
                archived INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_metric_definitions_archived " +
                "ON metric_definitions(archived)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS metric_entries (
                id TEXT NOT NULL PRIMARY KEY,
                metricId TEXT NOT NULL,
                canonicalValue REAL,
                enteredValue REAL,
                enteredUnitId TEXT,
                status TEXT NOT NULL,
                timestampMillis INTEGER NOT NULL,
                localEpochDay INTEGER NOT NULL,
                zoneId TEXT NOT NULL,
                offsetSeconds INTEGER NOT NULL,
                sourceType TEXT NOT NULL,
                sourceId TEXT,
                note TEXT NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                FOREIGN KEY(metricId) REFERENCES metric_definitions(id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_metric_entries_metricId ON metric_entries(metricId)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_metric_entries_metricId_timestampMillis " +
                "ON metric_entries(metricId, timestampMillis)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_metric_entries_sourceType_sourceId " +
                "ON metric_entries(sourceType, sourceId)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS areas (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                colorArgb INTEGER,
                position INTEGER NOT NULL,
                archived INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_areas_archived ON areas(archived)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tags (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                colorArgb INTEGER,
                archived INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags(name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tags_archived ON tags(archived)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS entity_tag_links (
                entityType TEXT NOT NULL,
                entityId TEXT NOT NULL,
                tagId TEXT NOT NULL,
                PRIMARY KEY(entityType, entityId, tagId),
                FOREIGN KEY(tagId) REFERENCES tags(id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_entity_tag_links_tagId ON entity_tag_links(tagId)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_entity_tag_links_entityType_entityId " +
                "ON entity_tag_links(entityType, entityId)",
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS task_step_snapshots (
                taskId INTEGER NOT NULL,
                occurrenceKey INTEGER NOT NULL,
                stepId INTEGER NOT NULL,
                title TEXT NOT NULL,
                position INTEGER NOT NULL,
                weight REAL NOT NULL,
                completed INTEGER NOT NULL,
                completedAtMillis INTEGER,
                PRIMARY KEY(taskId, occurrenceKey, stepId),
                FOREIGN KEY(taskId) REFERENCES tasks(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_task_step_snapshots_taskId " +
                "ON task_step_snapshots(taskId)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_task_step_snapshots_taskId_occurrenceKey " +
                "ON task_step_snapshots(taskId, occurrenceKey)",
        )
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE tasks ADD COLUMN updatedAtMillis INTEGER NOT NULL DEFAULT 0",
        )
        db.execSQL("UPDATE tasks SET updatedAtMillis = createdAtMillis")
        db.execSQL(
            "ALTER TABLE tasks ADD COLUMN showSubtaskProgress INTEGER NOT NULL DEFAULT 0",
        )
        db.execSQL(
            "ALTER TABLE tasks ADD COLUMN progressDisplay TEXT NOT NULL DEFAULT 'Percent'",
        )
        db.execSQL(
            "ALTER TABLE tasks ADD COLUMN autoCompleteFromSteps INTEGER NOT NULL DEFAULT 1",
        )
        db.execSQL(
            "ALTER TABLE tasks ADD COLUMN repeatStepPolicy TEXT NOT NULL DEFAULT 'Reset'",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS task_steps (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                taskId INTEGER NOT NULL,
                title TEXT NOT NULL,
                position INTEGER NOT NULL,
                weight REAL NOT NULL,
                archived INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                FOREIGN KEY(taskId) REFERENCES tasks(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_task_steps_taskId ON task_steps(taskId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS task_step_states (
                stepId INTEGER NOT NULL,
                taskId INTEGER NOT NULL,
                occurrenceKey INTEGER NOT NULL,
                completed INTEGER NOT NULL,
                completedAtMillis INTEGER,
                titleSnapshot TEXT NOT NULL,
                PRIMARY KEY(stepId, occurrenceKey),
                FOREIGN KEY(stepId) REFERENCES task_steps(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(taskId) REFERENCES tasks(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_task_step_states_stepId " +
                "ON task_step_states(stepId)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_task_step_states_taskId " +
                "ON task_step_states(taskId)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_task_step_states_taskId_occurrenceKey " +
                "ON task_step_states(taskId, occurrenceKey)",
        )
    }
}
