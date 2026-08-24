package com.whip.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.whip.app.domain.DEFAULT_GOAL_EMOJI
import com.whip.app.domain.DEFAULT_HABIT_EMOJI
import com.whip.app.domain.DEFAULT_TRACK_EMOJI
import com.whip.app.domain.normalizedIdentityEmoji

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
    ],
    version = 7,
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

        val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createTrackTablesV2(db)
                migrateLinkTablesToV2(db)
                migrateTriggerTablesToV2(db)
                createAutomationDetailTablesV2(db)
            }
        }

        val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trigger_rules ADD COLUMN sourceItemId INTEGER")
            }
        }

        val migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateIdentityEmoji(db, "habits", DEFAULT_HABIT_EMOJI)
                migrateIdentityEmoji(db, "goals", DEFAULT_GOAL_EMOJI)
                migrateIdentityEmoji(db, "tracks", DEFAULT_TRACK_EMOJI)
            }
        }

        val migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE goals ADD COLUMN elapsedStartMillis INTEGER")
                db.execSQL("ALTER TABLE goals ADD COLUMN elapsedDisplayUnit TEXT NOT NULL DEFAULT 'Auto'")
            }
        }

        val migration5To6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE track_fields ADD COLUMN scaleStep REAL NOT NULL DEFAULT 1.0")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS track_values_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        uuid TEXT NOT NULL,
                        entryId INTEGER NOT NULL,
                        fieldId INTEGER NOT NULL,
                        textValue TEXT,
                        enteredNumber REAL,
                        canonicalNumber REAL,
                        enteredUnitId TEXT,
                        dateEpochDay INTEGER,
                        booleanValue INTEGER,
                        choiceOptionId INTEGER,
                        scaleValue REAL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL,
                        FOREIGN KEY(entryId) REFERENCES track_entries(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(fieldId) REFERENCES track_fields(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(choiceOptionId) REFERENCES track_choice_options(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO track_values_new (
                        id, uuid, entryId, fieldId, textValue, enteredNumber, canonicalNumber,
                        enteredUnitId, dateEpochDay, booleanValue, choiceOptionId, scaleValue,
                        createdAtMillis, updatedAtMillis
                    )
                    SELECT id, uuid, entryId, fieldId, textValue, enteredNumber, canonicalNumber,
                        enteredUnitId, dateEpochDay, booleanValue, choiceOptionId,
                        CAST(scaleValue AS REAL), createdAtMillis, updatedAtMillis
                    FROM track_values
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE track_values")
                db.execSQL("ALTER TABLE track_values_new RENAME TO track_values")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_track_values_uuid ON track_values (uuid)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_track_values_entryId_fieldId ON track_values (entryId, fieldId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_track_values_fieldId ON track_values (fieldId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_track_values_choiceOptionId ON track_values (choiceOptionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_track_values_fieldId_canonicalNumber ON track_values (fieldId, canonicalNumber)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_track_values_fieldId_dateEpochDay ON track_values (fieldId, dateEpochDay)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trigger_field_mappings_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        triggerRuleId INTEGER NOT NULL,
                        targetFieldId INTEGER NOT NULL,
                        sourceProperty TEXT NOT NULL,
                        constantText TEXT,
                        constantNumber REAL,
                        constantUnitId TEXT,
                        constantDateEpochDay INTEGER,
                        constantBoolean INTEGER,
                        constantChoiceOptionId INTEGER,
                        constantScale REAL,
                        FOREIGN KEY(triggerRuleId) REFERENCES trigger_rules(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(targetFieldId) REFERENCES track_fields(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(constantChoiceOptionId) REFERENCES track_choice_options(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO trigger_field_mappings_new (
                        id, triggerRuleId, targetFieldId, sourceProperty, constantText,
                        constantNumber, constantUnitId, constantDateEpochDay, constantBoolean,
                        constantChoiceOptionId, constantScale
                    )
                    SELECT id, triggerRuleId, targetFieldId, sourceProperty, constantText,
                        constantNumber, constantUnitId, constantDateEpochDay, constantBoolean,
                        constantChoiceOptionId, CAST(constantScale AS REAL)
                    FROM trigger_field_mappings
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE trigger_field_mappings")
                db.execSQL("ALTER TABLE trigger_field_mappings_new RENAME TO trigger_field_mappings")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trigger_field_mappings_triggerRuleId ON trigger_field_mappings (triggerRuleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trigger_field_mappings_targetFieldId ON trigger_field_mappings (targetFieldId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trigger_field_mappings_constantChoiceOptionId ON trigger_field_mappings (constantChoiceOptionId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_trigger_field_mappings_triggerRuleId_targetFieldId ON trigger_field_mappings (triggerRuleId, targetFieldId)")
            }
        }

        /**
         * A Track backfill is an explicit request to count Entries from its
         * selected start date. Older builds created the contributions but left
         * the Goal window starting later, so the Automation could report four
         * eligible/current contributions while the Goal displayed only two.
         * Keep the two user-facing promises aligned for existing databases.
         */
        val migration6To7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE goals
                    SET startEpochDay = (
                        SELECT MIN(link_rules.retroactiveFromEpochDay)
                        FROM link_rules
                        WHERE link_rules.targetGoalId = goals.id
                          AND link_rules.sourceType = 'Track'
                          AND link_rules.kind = 'Contribution'
                          AND link_rules.targetMilestoneId IS NULL
                          AND link_rules.enabled = 1
                          AND link_rules.retroactiveFromEpochDay IS NOT NULL
                    )
                    WHERE startEpochDay > (
                        SELECT MIN(link_rules.retroactiveFromEpochDay)
                        FROM link_rules
                        WHERE link_rules.targetGoalId = goals.id
                          AND link_rules.sourceType = 'Track'
                          AND link_rules.kind = 'Contribution'
                          AND link_rules.targetMilestoneId IS NULL
                          AND link_rules.enabled = 1
                          AND link_rules.retroactiveFromEpochDay IS NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * Repository checks provide friendly errors; these triggers are the final consistency
         * boundary for concurrent writers, restored data, and any future write path.
         */
        val integrityGuardCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                installIntegrityGuards(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                installIntegrityGuards(db)
            }
        }

        fun get(context: Context): WhipDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                WhipDatabase::class.java,
                "whip.db",
            )
                .addMigrations(
                    migration1To2,
                    migration2To3,
                    migration3To4,
                    migration4To5,
                    migration5To6,
                    migration6To7,
                )
                .addCallback(integrityGuardCallback)
                .build()
                .also { instance = it }
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
            listOf("INSERT" to "", "UPDATE" to " OF entryId, fieldId, textValue, enteredNumber, canonicalNumber, enteredUnitId, dateEpochDay, booleanValue, choiceOptionId, scaleValue").forEach { (operation, columns) ->
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

        private fun createTrackTablesV2(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS tracks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    uuid TEXT NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    icon TEXT NOT NULL,
                    areaId TEXT NOT NULL,
                    area TEXT NOT NULL,
                    tagsCsv TEXT NOT NULL,
                    pinned INTEGER NOT NULL,
                    archived INTEGER NOT NULL,
                    position INTEGER NOT NULL,
                    createdAtMillis INTEGER NOT NULL,
                    updatedAtMillis INTEGER NOT NULL,
                    FOREIGN KEY(areaId) REFERENCES areas(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tracks_uuid ON tracks (uuid)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_areaId ON tracks (areaId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_archived ON tracks (archived)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tracks_pinned ON tracks (pinned)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS track_fields (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    uuid TEXT NOT NULL,
                    trackId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    required INTEGER NOT NULL,
                    primaryField INTEGER NOT NULL,
                    showInList INTEGER NOT NULL,
                    dimension TEXT,
                    unitId TEXT,
                    precision INTEGER NOT NULL,
                    scaleMin INTEGER,
                    scaleMax INTEGER,
                    scaleLowLabel TEXT NOT NULL,
                    scaleHighLabel TEXT NOT NULL,
                    createdAtMillis INTEGER NOT NULL,
                    updatedAtMillis INTEGER NOT NULL,
                    FOREIGN KEY(trackId) REFERENCES tracks(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_track_fields_uuid ON track_fields (uuid)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_track_fields_trackId_position ON track_fields (trackId, position)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_track_fields_trackId_primaryField ON track_fields (trackId, primaryField)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS track_choice_options (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    uuid TEXT NOT NULL,
                    fieldId INTEGER NOT NULL,
                    label TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    createdAtMillis INTEGER NOT NULL,
                    updatedAtMillis INTEGER NOT NULL,
                    FOREIGN KEY(fieldId) REFERENCES track_fields(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_track_choice_options_uuid ON track_choice_options (uuid)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_track_choice_options_fieldId_position ON track_choice_options (fieldId, position)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS track_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    uuid TEXT NOT NULL,
                    trackId INTEGER NOT NULL,
                    entryEpochDay INTEGER NOT NULL,
                    sourceOccurrenceId INTEGER,
                    sourceExplanation TEXT NOT NULL,
                    createdAtMillis INTEGER NOT NULL,
                    updatedAtMillis INTEGER NOT NULL,
                    FOREIGN KEY(trackId) REFERENCES tracks(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_track_entries_uuid ON track_entries (uuid)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_track_entries_trackId_entryEpochDay ON track_entries (trackId, entryEpochDay)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_track_entries_sourceOccurrenceId ON track_entries (sourceOccurrenceId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS track_values (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    uuid TEXT NOT NULL,
                    entryId INTEGER NOT NULL,
                    fieldId INTEGER NOT NULL,
                    textValue TEXT,
                    enteredNumber REAL,
                    canonicalNumber REAL,
                    enteredUnitId TEXT,
                    dateEpochDay INTEGER,
                    booleanValue INTEGER,
                    choiceOptionId INTEGER,
                    scaleValue INTEGER,
                    createdAtMillis INTEGER NOT NULL,
                    updatedAtMillis INTEGER NOT NULL,
                    FOREIGN KEY(entryId) REFERENCES track_entries(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(fieldId) REFERENCES track_fields(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(choiceOptionId) REFERENCES track_choice_options(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_track_values_uuid ON track_values (uuid)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_track_values_entryId_fieldId ON track_values (entryId, fieldId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_track_values_fieldId ON track_values (fieldId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_track_values_choiceOptionId ON track_values (choiceOptionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_track_values_fieldId_canonicalNumber ON track_values (fieldId, canonicalNumber)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_track_values_fieldId_dateEpochDay ON track_values (fieldId, dateEpochDay)")
            db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS track_entry_search USING FTS4(trackId INTEGER NOT NULL, content TEXT NOT NULL)")
        }

        private fun migrateLinkTablesToV2(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE link_rules_new (
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
                    trackAggregation TEXT,
                    sourceFieldId INTEGER,
                    conditionMode TEXT NOT NULL,
                    FOREIGN KEY(targetGoalId) REFERENCES goals(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(targetMilestoneId) REFERENCES goal_milestones(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                    FOREIGN KEY(sourceFieldId) REFERENCES track_fields(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO link_rules_new (
                    id, uuid, name, kind, sourceType, sourceEntityId, sourceMetricId,
                    sourceItemId, sourceMetric, targetGoalId, targetMilestoneId,
                    valueMode, fixedValue, multiplier, offset, retroactiveFromEpochDay,
                    enabled, createdAtMillis, updatedAtMillis, trackAggregation,
                    sourceFieldId, conditionMode
                )
                SELECT id, uuid, name, kind, sourceType, sourceEntityId, sourceMetricId,
                    sourceItemId, sourceMetric, targetGoalId, targetMilestoneId,
                    valueMode, fixedValue, multiplier, offset, retroactiveFromEpochDay,
                    enabled, createdAtMillis, updatedAtMillis, NULL, NULL, 'MatchAll'
                FROM link_rules
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE contributions_new (
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
                    FOREIGN KEY(linkRuleId) REFERENCES link_rules_new(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(targetGoalId) REFERENCES goals(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("INSERT INTO contributions_new SELECT * FROM contributions")
            db.execSQL("DROP TABLE contributions")
            db.execSQL("DROP TABLE link_rules")
            db.execSQL("ALTER TABLE link_rules_new RENAME TO link_rules")
            db.execSQL("ALTER TABLE contributions_new RENAME TO contributions")
            db.execSQL(
                """
                CREATE TABLE contributions_final (
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
            db.execSQL("INSERT INTO contributions_final SELECT * FROM contributions")
            db.execSQL("DROP TABLE contributions")
            db.execSQL("ALTER TABLE contributions_final RENAME TO contributions")

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_link_rules_uuid ON link_rules (uuid)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_link_rules_targetGoalId ON link_rules (targetGoalId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_link_rules_targetMilestoneId ON link_rules (targetMilestoneId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_link_rules_sourceFieldId ON link_rules (sourceFieldId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_link_rules_sourceType_sourceEntityId ON link_rules (sourceType, sourceEntityId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_contributions_uuid ON contributions (uuid)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_contributions_linkRuleId_sourceEventId ON contributions (linkRuleId, sourceEventId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_contributions_targetGoalId ON contributions (targetGoalId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_contributions_metricEntryId ON contributions (metricEntryId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_contributions_sourceType_sourceEntityId ON contributions (sourceType, sourceEntityId)")
        }

        private fun migrateTriggerTablesToV2(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE trigger_rules_new (
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
                    action TEXT NOT NULL,
                    notificationEnabled INTEGER NOT NULL,
                    conditionMode TEXT NOT NULL,
                    enabled INTEGER NOT NULL,
                    createdAtMillis INTEGER NOT NULL,
                    updatedAtMillis INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO trigger_rules_new (
                    id, uuid, name, sourceType, sourceEntityId, outcome, targetType,
                    targetEntityId, delayMinutes, quietStartMinutes, quietEndMinutes,
                    action, notificationEnabled, conditionMode, enabled,
                    createdAtMillis, updatedAtMillis
                )
                SELECT id, uuid, name, sourceType, sourceEntityId, outcome, targetType,
                    targetEntityId, delayMinutes, quietStartMinutes, quietEndMinutes,
                    CASE
                        WHEN autoCompleteTargetHabit = 1 THEN 'CheckOffHabit'
                        WHEN targetType = 'Task' THEN 'PromptTask'
                        ELSE 'PromptHabit'
                    END,
                    0, 'MatchAll', enabled, createdAtMillis, updatedAtMillis
                FROM trigger_rules
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE trigger_occurrences_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    triggerRuleId INTEGER NOT NULL,
                    sourceEventId TEXT NOT NULL,
                    availableAtMillis INTEGER NOT NULL,
                    deliveredAtMillis INTEGER,
                    dismissedAtMillis INTEGER,
                    remindAtMillis INTEGER,
                    fulfilledEntryId INTEGER,
                    sourceSnapshot TEXT NOT NULL,
                    FOREIGN KEY(triggerRuleId) REFERENCES trigger_rules_new(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(fulfilledEntryId) REFERENCES track_entries(id) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO trigger_occurrences_new (
                    id, triggerRuleId, sourceEventId, availableAtMillis,
                    deliveredAtMillis, dismissedAtMillis, remindAtMillis,
                    fulfilledEntryId, sourceSnapshot
                )
                SELECT id, triggerRuleId, sourceEventId, availableAtMillis,
                    deliveredAtMillis, dismissedAtMillis, NULL, NULL, ''
                FROM trigger_occurrences
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE trigger_occurrences")
            db.execSQL("DROP TABLE trigger_rules")
            db.execSQL("ALTER TABLE trigger_rules_new RENAME TO trigger_rules")
            db.execSQL("ALTER TABLE trigger_occurrences_new RENAME TO trigger_occurrences")
            db.execSQL(
                """
                CREATE TABLE trigger_occurrences_final (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    triggerRuleId INTEGER NOT NULL,
                    sourceEventId TEXT NOT NULL,
                    availableAtMillis INTEGER NOT NULL,
                    deliveredAtMillis INTEGER,
                    dismissedAtMillis INTEGER,
                    remindAtMillis INTEGER,
                    fulfilledEntryId INTEGER,
                    sourceSnapshot TEXT NOT NULL,
                    FOREIGN KEY(triggerRuleId) REFERENCES trigger_rules(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(fulfilledEntryId) REFERENCES track_entries(id) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent(),
            )
            db.execSQL("INSERT INTO trigger_occurrences_final SELECT * FROM trigger_occurrences")
            db.execSQL("DROP TABLE trigger_occurrences")
            db.execSQL("ALTER TABLE trigger_occurrences_final RENAME TO trigger_occurrences")

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_trigger_rules_uuid ON trigger_rules (uuid)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_trigger_rules_sourceType_sourceEntityId ON trigger_rules (sourceType, sourceEntityId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_trigger_rules_targetType_targetEntityId ON trigger_rules (targetType, targetEntityId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_trigger_occurrences_triggerRuleId_sourceEventId ON trigger_occurrences (triggerRuleId, sourceEventId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_trigger_occurrences_availableAtMillis ON trigger_occurrences (availableAtMillis)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_trigger_occurrences_fulfilledEntryId ON trigger_occurrences (fulfilledEntryId)")
        }

        private fun createAutomationDetailTablesV2(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS link_rule_conditions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    linkRuleId INTEGER NOT NULL,
                    fieldId INTEGER,
                    entryDate INTEGER NOT NULL,
                    operator TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    textValue TEXT,
                    numberValue REAL,
                    secondNumberValue REAL,
                    dateEpochDay INTEGER,
                    secondDateEpochDay INTEGER,
                    FOREIGN KEY(linkRuleId) REFERENCES link_rules(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(fieldId) REFERENCES track_fields(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_link_rule_conditions_linkRuleId ON link_rule_conditions (linkRuleId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_link_rule_conditions_fieldId ON link_rule_conditions (fieldId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_link_rule_conditions_linkRuleId_position ON link_rule_conditions (linkRuleId, position)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS trigger_rule_conditions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    triggerRuleId INTEGER NOT NULL,
                    fieldId INTEGER,
                    entryDate INTEGER NOT NULL,
                    operator TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    textValue TEXT,
                    numberValue REAL,
                    secondNumberValue REAL,
                    dateEpochDay INTEGER,
                    secondDateEpochDay INTEGER,
                    FOREIGN KEY(triggerRuleId) REFERENCES trigger_rules(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(fieldId) REFERENCES track_fields(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_trigger_rule_conditions_triggerRuleId ON trigger_rule_conditions (triggerRuleId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_trigger_rule_conditions_fieldId ON trigger_rule_conditions (fieldId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_trigger_rule_conditions_triggerRuleId_position ON trigger_rule_conditions (triggerRuleId, position)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS trigger_field_mappings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    triggerRuleId INTEGER NOT NULL,
                    targetFieldId INTEGER NOT NULL,
                    sourceProperty TEXT NOT NULL,
                    constantText TEXT,
                    constantNumber REAL,
                    constantUnitId TEXT,
                    constantDateEpochDay INTEGER,
                    constantBoolean INTEGER,
                    constantChoiceOptionId INTEGER,
                    constantScale INTEGER,
                    FOREIGN KEY(triggerRuleId) REFERENCES trigger_rules(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(targetFieldId) REFERENCES track_fields(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(constantChoiceOptionId) REFERENCES track_choice_options(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_trigger_field_mappings_triggerRuleId ON trigger_field_mappings (triggerRuleId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_trigger_field_mappings_targetFieldId ON trigger_field_mappings (targetFieldId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_trigger_field_mappings_constantChoiceOptionId ON trigger_field_mappings (constantChoiceOptionId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_trigger_field_mappings_triggerRuleId_targetFieldId ON trigger_field_mappings (triggerRuleId, targetFieldId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS link_condition_choices (
                    conditionId INTEGER NOT NULL,
                    optionId INTEGER NOT NULL,
                    PRIMARY KEY(conditionId, optionId),
                    FOREIGN KEY(conditionId) REFERENCES link_rule_conditions(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(optionId) REFERENCES track_choice_options(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_link_condition_choices_conditionId ON link_condition_choices (conditionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_link_condition_choices_optionId ON link_condition_choices (optionId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS trigger_condition_choices (
                    conditionId INTEGER NOT NULL,
                    optionId INTEGER NOT NULL,
                    PRIMARY KEY(conditionId, optionId),
                    FOREIGN KEY(conditionId) REFERENCES trigger_rule_conditions(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(optionId) REFERENCES track_choice_options(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_trigger_condition_choices_conditionId ON trigger_condition_choices (conditionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_trigger_condition_choices_optionId ON trigger_condition_choices (optionId)")
        }

        private fun migrateIdentityEmoji(db: SupportSQLiteDatabase, table: String, defaultEmoji: String) {
            val updates = mutableListOf<Pair<Long, String>>()
            db.query("SELECT id, icon FROM $table").use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow("id")
                val iconColumn = cursor.getColumnIndexOrThrow("icon")
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val current = cursor.getString(iconColumn).orEmpty()
                    val normalized = current.normalizedIdentityEmoji(defaultEmoji)
                    if (normalized != current) updates += id to normalized
                }
            }
            updates.forEach { (id, emoji) ->
                db.execSQL("UPDATE $table SET icon = ? WHERE id = ?", arrayOf<Any>(emoji, id))
            }
        }
    }
}
