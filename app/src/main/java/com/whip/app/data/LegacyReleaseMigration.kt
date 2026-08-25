package com.whip.app.data

import androidx.sqlite.db.SupportSQLiteDatabase
import com.whip.app.domain.DEFAULT_TASK_EMOJI

/** Upgrade path from every public 0.3.x build, all of which shipped Room schema 27. */
internal object LegacyReleaseMigration {
    private const val LEGACY_PREFIX = "whip_schema_27_"

    private data class CapturedTable(
        val createSql: String,
        val indexSql: List<String>,
    )

    private data class TargetTable(
        val createSql: String,
        val indexSql: List<String>,
    )

    /** Schema 9 was used only by pre-release audit builds; add the retained public-history tables. */
    fun createCompatibilityTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS entity_tag_links (
                entityType TEXT NOT NULL,
                entityId TEXT NOT NULL,
                tagId TEXT NOT NULL,
                PRIMARY KEY(entityType, entityId, tagId),
                FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_entity_tag_links_tagId ON entity_tag_links (tagId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_entity_tag_links_entityType_entityId ON entity_tag_links (entityType, entityId)")
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
        db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_completion_snapshots_goalId ON goal_completion_snapshots (goalId)")
    }

    fun migrateSchema27(db: SupportSQLiteDatabase) {
        val captured = rebuiltTables.associateWith { table -> captureTable(db, table) }

        // Explicit index names are database-global and would otherwise collide with the new
        // tables while the old data is parked under temporary names.
        captured.values.flatMap(CapturedTable::indexSql).forEach { sql ->
            indexName(sql)?.let { db.execSQL("DROP INDEX IF EXISTS `${it}`") }
        }
        rebuiltTables.asReversed().forEach { table ->
            db.execSQL("ALTER TABLE `${table}` RENAME TO `${LEGACY_PREFIX}${table}`")
        }

        rebuiltTables.forEach { table ->
            db.execSQL(targetTables[table]?.createSql ?: captured.getValue(table).createSql)
        }
        rebuiltTables.forEach { table ->
            val indexes = targetTables[table]?.indexSql ?: captured.getValue(table).indexSql
            indexes.forEach(db::execSQL)
        }

        rebuiltTables.forEach { table -> copyCompatibleColumns(db, table) }

        // Tracks and detailed Automation records did not exist in public schema 27. Reuse the
        // already-tested current migration steps so their final affinities and indexes match.
        WhipDatabase.createTrackTablesV2(db)
        WhipDatabase.createAutomationDetailTablesV2(db)
        WhipDatabase.migration5To6.migrate(db)
        WhipDatabase.migration8To9.migrate(db)

        rebuiltTables.asReversed().forEach { table ->
            db.execSQL("DROP TABLE `${LEGACY_PREFIX}${table}`")
        }
    }

    private fun captureTable(db: SupportSQLiteDatabase, table: String): CapturedTable {
        val createSql = db.query(
            "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(table),
        ).use { cursor ->
            check(cursor.moveToFirst()) { "Public schema 27 is missing $table" }
            cursor.getString(0)
        }
        val indexes = mutableListOf<String>()
        db.query(
            "SELECT sql FROM sqlite_master WHERE type = 'index' AND tbl_name = ? AND sql IS NOT NULL ORDER BY name",
            arrayOf(table),
        ).use { cursor ->
            while (cursor.moveToNext()) indexes += cursor.getString(0)
        }
        return CapturedTable(createSql, indexes)
    }

    private fun copyCompatibleColumns(db: SupportSQLiteDatabase, table: String) {
        val legacyTable = "$LEGACY_PREFIX$table"
        val legacyColumns = columns(db, legacyTable).toSet()
        val targetColumns = columns(db, table)
        val overrides = copyOverrides[table].orEmpty()
        val copied = targetColumns.filter { it in legacyColumns || it in overrides }
        val expressions = copied.map { column -> overrides[column] ?: "`${column}`" }
        db.execSQL(
            "INSERT INTO `${table}` (${copied.joinToString { "`${it}`" }}) " +
                "SELECT ${expressions.joinToString()} FROM `${legacyTable}`",
        )
    }

    private fun columns(db: SupportSQLiteDatabase, table: String): List<String> = buildList {
        db.query("PRAGMA table_info(`${table}`)").use { cursor ->
            val name = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) add(cursor.getString(name))
        }
    }

    private fun indexName(createSql: String): String? =
        Regex("(?i)CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?[`\"]?([^`\"\\s]+)")
            .find(createSql)
            ?.groupValues
            ?.get(1)

    private val rebuiltTables = listOf(
        "exercise_categories",
        "exercise_category_joins",
        "tasks",
        "task_occurrences",
        "task_steps",
        "task_step_states",
        "task_step_snapshots",
        "goals",
        "goal_milestones",
        "goal_completion_snapshots",
        "link_rules",
        "contributions",
        "habits",
        "habit_checklist_items",
        "habit_logs",
        "habit_checklist_states",
        "habit_pauses",
        "tags",
        "entity_tag_links",
        "trigger_rules",
        "trigger_occurrences",
    )

    private val copyOverrides = mapOf(
        "goals" to mapOf(
            "elapsedStartMillis" to "NULL",
            "elapsedDisplayUnit" to "'Auto'",
        ),
        "link_rules" to mapOf(
            "trackAggregation" to "NULL",
            "sourceFieldId" to "NULL",
            "conditionMode" to "'MatchAll'",
        ),
        "tasks" to mapOf("icon" to "'$DEFAULT_TASK_EMOJI'"),
        "trigger_occurrences" to mapOf(
            "remindAtMillis" to "NULL",
            "fulfilledEntryId" to "NULL",
            "sourceSnapshot" to "''",
        ),
        "trigger_rules" to mapOf(
            "sourceItemId" to "NULL",
            "action" to "CASE WHEN autoCompleteTargetHabit = 1 THEN 'CheckOffHabit' " +
                "WHEN targetType = 'Task' THEN 'PromptTask' ELSE 'PromptHabit' END",
            "notificationEnabled" to "0",
            "conditionMode" to "'MatchAll'",
        ),
    )

    private val targetTables = mapOf(
        "exercise_categories" to TargetTable(
            """
            CREATE TABLE exercise_categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uuid TEXT NOT NULL,
                name TEXT NOT NULL, kind TEXT NOT NULL, position INTEGER NOT NULL,
                archived INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL
            )
            """.trimIndent(),
            listOf(
                "CREATE UNIQUE INDEX index_exercise_categories_uuid ON exercise_categories (uuid)",
                "CREATE INDEX index_exercise_categories_name ON exercise_categories (name)",
                "CREATE INDEX index_exercise_categories_archived ON exercise_categories (archived)",
            ),
        ),
        "goal_milestones" to TargetTable(
            """
            CREATE TABLE goal_milestones (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uuid TEXT NOT NULL,
                goalId INTEGER NOT NULL, name TEXT NOT NULL, position INTEGER NOT NULL,
                weight REAL NOT NULL, completed INTEGER NOT NULL, completedAtMillis INTEGER,
                reward TEXT NOT NULL, createdAtMillis INTEGER NOT NULL, updatedAtMillis INTEGER NOT NULL,
                FOREIGN KEY(goalId) REFERENCES goals(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
            listOf(
                "CREATE UNIQUE INDEX index_goal_milestones_uuid ON goal_milestones (uuid)",
                "CREATE INDEX index_goal_milestones_goalId ON goal_milestones (goalId)",
            ),
        ),
        "goals" to TargetTable(
            """
            CREATE TABLE goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uuid TEXT NOT NULL,
                metricId TEXT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL,
                areaId TEXT, area TEXT NOT NULL, tagsCsv TEXT NOT NULL, icon TEXT NOT NULL,
                type TEXT NOT NULL, dimension TEXT NOT NULL, unitId TEXT NOT NULL,
                precision INTEGER NOT NULL, baseline REAL, targetMin REAL, targetMax REAL,
                direction TEXT NOT NULL, startEpochDay INTEGER NOT NULL, deadlineEpochDay INTEGER,
                aggregation TEXT NOT NULL, aggregationPeriod TEXT NOT NULL, rollingDays INTEGER,
                paceType TEXT NOT NULL, consistencyPeriod TEXT NOT NULL,
                consistencyRequiredPeriods INTEGER, elapsedStartMillis INTEGER,
                elapsedDisplayUnit TEXT NOT NULL, reminderMinutes INTEGER, status TEXT NOT NULL,
                pinned INTEGER NOT NULL, position INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                FOREIGN KEY(areaId) REFERENCES areas(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
            listOf(
                "CREATE UNIQUE INDEX index_goals_uuid ON goals (uuid)",
                "CREATE UNIQUE INDEX index_goals_metricId ON goals (metricId)",
                "CREATE INDEX index_goals_status ON goals (status)",
                "CREATE INDEX index_goals_pinned ON goals (pinned)",
                "CREATE INDEX index_goals_areaId ON goals (areaId)",
            ),
        ),
        "habits" to TargetTable(
            """
            CREATE TABLE habits (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uuid TEXT NOT NULL,
                metricId TEXT NOT NULL, name TEXT NOT NULL, notes TEXT NOT NULL, areaId TEXT,
                area TEXT NOT NULL, tagsCsv TEXT NOT NULL, icon TEXT NOT NULL,
                trackingMode TEXT NOT NULL, dimension TEXT NOT NULL, unitId TEXT NOT NULL,
                precision INTEGER NOT NULL, comparison TEXT NOT NULL, targetMin REAL,
                targetMax REAL, targetPeriod TEXT NOT NULL, rollingDays INTEGER,
                scheduleType TEXT NOT NULL, scheduleInterval INTEGER NOT NULL,
                weekdaysMask INTEGER NOT NULL, flexibleTimesPerWeek INTEGER,
                startEpochDay INTEGER NOT NULL, endType TEXT NOT NULL, endEpochDay INTEGER,
                endValue REAL, quickIncrement REAL NOT NULL, quickActionsCsv TEXT NOT NULL,
                reminderMinutesCsv TEXT NOT NULL, weekdayReminderMinutesCsv TEXT NOT NULL,
                weekStart TEXT NOT NULL, timerStartedAtMillis INTEGER, pinned INTEGER NOT NULL,
                position INTEGER NOT NULL, archived INTEGER NOT NULL, paused INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL, updatedAtMillis INTEGER NOT NULL,
                sourceMetricId TEXT,
                FOREIGN KEY(areaId) REFERENCES areas(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
            listOf(
                "CREATE UNIQUE INDEX index_habits_uuid ON habits (uuid)",
                "CREATE UNIQUE INDEX index_habits_metricId ON habits (metricId)",
                "CREATE INDEX index_habits_archived ON habits (archived)",
                "CREATE INDEX index_habits_pinned ON habits (pinned)",
                "CREATE INDEX index_habits_areaId ON habits (areaId)",
            ),
        ),
        "link_rules" to TargetTable(
            """
            CREATE TABLE link_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uuid TEXT NOT NULL,
                name TEXT NOT NULL, kind TEXT NOT NULL, sourceType TEXT NOT NULL,
                sourceEntityId INTEGER, sourceMetricId TEXT, sourceItemId INTEGER,
                sourceMetric TEXT NOT NULL, targetGoalId INTEGER NOT NULL,
                targetMilestoneId INTEGER, valueMode TEXT NOT NULL, fixedValue REAL,
                multiplier REAL NOT NULL, offset REAL NOT NULL, retroactiveFromEpochDay INTEGER,
                enabled INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL, trackAggregation TEXT, sourceFieldId INTEGER,
                conditionMode TEXT NOT NULL,
                FOREIGN KEY(targetGoalId) REFERENCES goals(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(targetMilestoneId) REFERENCES goal_milestones(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(sourceFieldId) REFERENCES track_fields(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
            listOf(
                "CREATE UNIQUE INDEX index_link_rules_uuid ON link_rules (uuid)",
                "CREATE INDEX index_link_rules_targetGoalId ON link_rules (targetGoalId)",
                "CREATE INDEX index_link_rules_targetMilestoneId ON link_rules (targetMilestoneId)",
                "CREATE INDEX index_link_rules_sourceFieldId ON link_rules (sourceFieldId)",
                "CREATE INDEX index_link_rules_sourceType_sourceEntityId ON link_rules (sourceType, sourceEntityId)",
            ),
        ),
        "tags" to TargetTable(
            """
            CREATE TABLE tags (
                id TEXT NOT NULL, name TEXT NOT NULL, archived INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL, updatedAtMillis INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
            listOf(
                "CREATE UNIQUE INDEX index_tags_name ON tags (name)",
                "CREATE INDEX index_tags_archived ON tags (archived)",
            ),
        ),
        "task_step_snapshots" to TargetTable(
            """
            CREATE TABLE task_step_snapshots (
                taskId INTEGER NOT NULL, occurrenceKey INTEGER NOT NULL, stepId INTEGER NOT NULL,
                title TEXT NOT NULL, position INTEGER NOT NULL, notes TEXT NOT NULL,
                completed INTEGER NOT NULL, completedAtMillis INTEGER,
                PRIMARY KEY(taskId, occurrenceKey, stepId),
                FOREIGN KEY(taskId) REFERENCES tasks(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
            listOf(
                "CREATE INDEX index_task_step_snapshots_taskId ON task_step_snapshots (taskId)",
                "CREATE INDEX index_task_step_snapshots_taskId_occurrenceKey ON task_step_snapshots (taskId, occurrenceKey)",
            ),
        ),
        "task_steps" to TargetTable(
            """
            CREATE TABLE task_steps (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uuid TEXT NOT NULL,
                taskId INTEGER NOT NULL, title TEXT NOT NULL, position INTEGER NOT NULL,
                notes TEXT NOT NULL, archived INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                FOREIGN KEY(taskId) REFERENCES tasks(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
            listOf(
                "CREATE INDEX index_task_steps_taskId ON task_steps (taskId)",
                "CREATE UNIQUE INDEX index_task_steps_uuid ON task_steps (uuid)",
            ),
        ),
        "tasks" to TargetTable(
            """
            CREATE TABLE tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uuid TEXT NOT NULL,
                title TEXT NOT NULL, notes TEXT NOT NULL, scheduleKind TEXT NOT NULL,
                dateEpochDay INTEGER, recurrenceUnit TEXT, recurrenceInterval INTEGER NOT NULL,
                weekdaysMask INTEGER NOT NULL, recurrenceEnd TEXT, recurrenceEndEpochDay INTEGER,
                recurrenceCount INTEGER, timeMinutes INTEGER, reminderEnabled INTEGER NOT NULL,
                archived INTEGER NOT NULL, completedAtMillis INTEGER, createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL, showSubtaskProgress INTEGER NOT NULL,
                progressDisplay TEXT NOT NULL, autoCompleteFromSteps INTEGER NOT NULL,
                repeatStepPolicy TEXT NOT NULL, pinned INTEGER NOT NULL, priority TEXT NOT NULL,
                areaId TEXT, area TEXT NOT NULL, tagsCsv TEXT NOT NULL, deadlineEpochDay INTEGER,
                recurrenceAnchor TEXT NOT NULL, reminderOffsetsMinutesCsv TEXT NOT NULL,
                missedOccurrencePolicy TEXT NOT NULL, inbox INTEGER NOT NULL, durationMinutes INTEGER,
                effort TEXT NOT NULL, manualPosition INTEGER NOT NULL,
                icon TEXT NOT NULL DEFAULT '$DEFAULT_TASK_EMOJI',
                FOREIGN KEY(areaId) REFERENCES areas(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
            listOf(
                "CREATE UNIQUE INDEX index_tasks_uuid ON tasks (uuid)",
                "CREATE INDEX index_tasks_areaId ON tasks (areaId)",
            ),
        ),
        "trigger_occurrences" to TargetTable(
            """
            CREATE TABLE trigger_occurrences (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, triggerRuleId INTEGER NOT NULL,
                sourceEventId TEXT NOT NULL, availableAtMillis INTEGER NOT NULL,
                deliveredAtMillis INTEGER, dismissedAtMillis INTEGER, remindAtMillis INTEGER,
                fulfilledEntryId INTEGER, sourceSnapshot TEXT NOT NULL,
                FOREIGN KEY(triggerRuleId) REFERENCES trigger_rules(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(fulfilledEntryId) REFERENCES track_entries(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
            listOf(
                "CREATE UNIQUE INDEX index_trigger_occurrences_triggerRuleId_sourceEventId ON trigger_occurrences (triggerRuleId, sourceEventId)",
                "CREATE INDEX index_trigger_occurrences_availableAtMillis ON trigger_occurrences (availableAtMillis)",
                "CREATE INDEX index_trigger_occurrences_fulfilledEntryId ON trigger_occurrences (fulfilledEntryId)",
            ),
        ),
        "trigger_rules" to TargetTable(
            """
            CREATE TABLE trigger_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uuid TEXT NOT NULL,
                name TEXT NOT NULL, sourceType TEXT NOT NULL, sourceEntityId INTEGER NOT NULL,
                sourceItemId INTEGER, outcome TEXT NOT NULL, targetType TEXT NOT NULL,
                targetEntityId INTEGER NOT NULL, delayMinutes INTEGER NOT NULL,
                quietStartMinutes INTEGER, quietEndMinutes INTEGER, action TEXT NOT NULL,
                notificationEnabled INTEGER NOT NULL, conditionMode TEXT NOT NULL,
                enabled INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL
            )
            """.trimIndent(),
            listOf(
                "CREATE UNIQUE INDEX index_trigger_rules_uuid ON trigger_rules (uuid)",
                "CREATE INDEX index_trigger_rules_sourceType_sourceEntityId ON trigger_rules (sourceType, sourceEntityId)",
                "CREATE INDEX index_trigger_rules_targetType_targetEntityId ON trigger_rules (targetType, targetEntityId)",
            ),
        ),
    )
}
