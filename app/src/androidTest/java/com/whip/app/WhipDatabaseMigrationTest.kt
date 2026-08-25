package com.whip.app

import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whip.app.data.WhipDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhipDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WhipDatabase::class.java,
    )

    @Test
    fun migrationOneToNinePreservesTasksLinksContributionsTriggersAndOccurrences() {
        helper.createDatabase(V1_DATABASE_NAME, 1).apply {
            insertMainArea()
            insertSchemaOneTask()
            insertSchemaOneGoal()
            execSQL(
                """
                INSERT INTO link_rules (
                    id, uuid, name, kind, sourceType, sourceEntityId, sourceMetricId,
                    sourceItemId, sourceMetric, targetGoalId, targetMilestoneId,
                    valueMode, fixedValue, multiplier, offset, retroactiveFromEpochDay,
                    enabled, createdAtMillis, updatedAtMillis
                ) VALUES (
                    11, 'link-11', 'Habit to Goal', 'Contribution', 'Habit', 3, NULL,
                    NULL, 'Value', 7, NULL, 'SourceValue', NULL, 1, 0, NULL,
                    1, 100, 100
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO contributions (
                    id, uuid, linkRuleId, sourceEventId, sourceType, sourceEntityId,
                    targetGoalId, metricEntryId, canonicalValue, localEpochDay,
                    timestampMillis, excluded, overrideValue, explanation,
                    createdAtMillis, updatedAtMillis
                ) VALUES (
                    12, 'contribution-12', 11, 'habit-event-1', 'Habit', 3,
                    7, NULL, 2.5, 20690, 1000, 0, NULL, 'Habit value', 1000, 1000
                )
                """.trimIndent(),
            )
            insertSchemaOneTrigger(
                id = 21,
                uuid = "prompt-task",
                targetType = "Task",
                autoCompleteTargetHabit = 0,
            )
            insertSchemaOneTrigger(
                id = 22,
                uuid = "check-habit",
                targetType = "Habit",
                autoCompleteTargetHabit = 1,
            )
            execSQL(
                "INSERT INTO trigger_occurrences " +
                    "(id, triggerRuleId, sourceEventId, availableAtMillis, deliveredAtMillis, dismissedAtMillis) " +
                    "VALUES (31, 21, 'task-event-1', 2000, 2100, NULL)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            V1_DATABASE_NAME,
            9,
            true,
            *allMigrations,
        ).use { database ->
            database.query(
                """
                SELECT l.id, l.uuid, l.conditionMode, c.id, c.canonicalValue, c.explanation
                FROM link_rules l JOIN contributions c ON c.linkRuleId = l.id
                WHERE l.id = 11
                """.trimIndent(),
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(11L, cursor.getLong(0))
                assertEquals("link-11", cursor.getString(1))
                assertEquals("MatchAll", cursor.getString(2))
                assertEquals(12L, cursor.getLong(3))
                assertEquals(2.5, cursor.getDouble(4), 0.0)
                assertEquals("Habit value", cursor.getString(5))
            }
            database.query("SELECT uuid, action, notificationEnabled, conditionMode, sourceItemId FROM trigger_rules ORDER BY id").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("prompt-task", cursor.getString(0))
                assertEquals("PromptTask", cursor.getString(1))
                assertEquals(0, cursor.getInt(2))
                assertEquals("MatchAll", cursor.getString(3))
                check(cursor.isNull(4))
                check(cursor.moveToNext())
                assertEquals("check-habit", cursor.getString(0))
                assertEquals("CheckOffHabit", cursor.getString(1))
            }
            database.query(
                "SELECT id, sourceEventId, deliveredAtMillis, remindAtMillis, fulfilledEntryId, sourceSnapshot " +
                    "FROM trigger_occurrences WHERE id = 31",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(31L, cursor.getLong(0))
                assertEquals("task-event-1", cursor.getString(1))
                assertEquals(2100L, cursor.getLong(2))
                check(cursor.isNull(3))
                check(cursor.isNull(4))
                assertEquals("", cursor.getString(5))
            }
            database.query("SELECT title, icon FROM tasks WHERE id = 5").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Preserved Task", cursor.getString(0))
                assertEquals("✅", cursor.getString(1))
            }
        }
    }

    @Test
    fun migrationTwoToNinePreservesTrackIdentityFieldsEntriesAndFractionalValues() {
        helper.createDatabase(V2_DATABASE_NAME, 2).apply {
            insertMainArea()
            execSQL(
                "INSERT INTO tracks " +
                    "(id, uuid, name, description, icon, areaId, area, tagsCsv, pinned, archived, position, createdAtMillis, updatedAtMillis) " +
                    "VALUES (41, 'track-41', 'Movies', '', '▤', 'main', 'Main', '', 1, 0, 0, 100, 100)",
            )
            execSQL(
                "INSERT INTO track_fields " +
                    "(id, uuid, trackId, name, type, position, required, primaryField, showInList, dimension, unitId, precision, scaleMin, scaleMax, scaleLowLabel, scaleHighLabel, createdAtMillis, updatedAtMillis) " +
                    "VALUES (42, 'field-42', 41, 'Rating', 'Scale', 0, 1, 1, 1, NULL, NULL, 1, 1, 5, 'Poor', 'Excellent', 100, 100)",
            )
            execSQL(
                "INSERT INTO track_entries " +
                    "(id, uuid, trackId, entryEpochDay, sourceOccurrenceId, sourceExplanation, createdAtMillis, updatedAtMillis) " +
                    "VALUES (43, 'entry-43', 41, 20690, NULL, '', 100, 100)",
            )
            execSQL(
                "INSERT INTO track_values " +
                    "(id, uuid, entryId, fieldId, textValue, enteredNumber, canonicalNumber, enteredUnitId, dateEpochDay, booleanValue, choiceOptionId, scaleValue, createdAtMillis, updatedAtMillis) " +
                    "VALUES (44, 'value-44', 43, 42, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 4, 100, 100)",
            )
            execSQL(
                """
                INSERT INTO trigger_rules (
                    id, uuid, name, sourceType, sourceEntityId, outcome, targetType,
                    targetEntityId, delayMinutes, quietStartMinutes, quietEndMinutes,
                    action, notificationEnabled, conditionMode, enabled,
                    createdAtMillis, updatedAtMillis
                ) VALUES (
                    45, 'trigger-45', 'Follow Up', 'Track', 41, 'Recorded', 'Habit',
                    9, 0, NULL, NULL, 'PromptHabit', 0, 'MatchAll', 1, 100, 100
                )
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            V2_DATABASE_NAME,
            9,
            true,
            *allMigrations.drop(1).toTypedArray(),
        ).use { database ->
            database.query(
                """
                SELECT t.id, t.uuid, t.name, t.icon, f.id, f.primaryField,
                    f.showInList, f.scaleStep, e.id, v.id, v.scaleValue, typeof(v.scaleValue)
                FROM tracks t
                JOIN track_fields f ON f.trackId = t.id
                JOIN track_entries e ON e.trackId = t.id
                JOIN track_values v ON v.entryId = e.id AND v.fieldId = f.id
                WHERE t.id = 41
                """.trimIndent(),
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(41L, cursor.getLong(0))
                assertEquals("track-41", cursor.getString(1))
                assertEquals("Movies", cursor.getString(2))
                assertEquals("📋", cursor.getString(3))
                assertEquals(42L, cursor.getLong(4))
                assertEquals(1, cursor.getInt(5))
                assertEquals(1, cursor.getInt(6))
                assertEquals(1.0, cursor.getDouble(7), 0.0)
                assertEquals(43L, cursor.getLong(8))
                assertEquals(44L, cursor.getLong(9))
                assertEquals(4.0, cursor.getDouble(10), 0.0)
                assertEquals("real", cursor.getString(11))
            }
            database.query("SELECT sourceItemId FROM trigger_rules WHERE id = 45").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.isNull(0))
            }
        }
    }

    @Test
    fun migrationEightToNineMovesNeutralOccurrencesOutOfMeasurementsAndDropsMissing() {
        helper.createDatabase(V8_DATABASE_NAME, 8).apply {
            insertMainArea()
            execSQL("INSERT INTO metric_definitions (id, name, valueKind, dimension, defaultUnitId, precision, dimensionLocked, archived, createdAtMillis, updatedAtMillis) VALUES ('metric-habit', 'Read', 'Boolean', 'Count', 'count', 0, 0, 0, 1, 1)")
            execSQL(
                """
                INSERT INTO habits (
                    id, uuid, metricId, name, notes, areaId, area, tagsCsv, icon,
                    trackingMode, dimension, unitId, precision, comparison, targetMin,
                    targetMax, targetPeriod, rollingDays, scheduleType, scheduleInterval,
                    weekdaysMask, flexibleTimesPerWeek, startEpochDay, endType, endEpochDay,
                    endValue, quickIncrement, quickActionsCsv, reminderMinutesCsv,
                    weekdayReminderMinutesCsv, weekStart, timerStartedAtMillis, pinned,
                    position, archived, paused, createdAtMillis, updatedAtMillis, sourceMetricId
                ) VALUES (
                    3, 'habit-3', 'metric-habit', 'Read', '', 'main', 'Main', '', '📖',
                    'CheckOff', 'Count', 'count', 0, 'AtLeast', 1, NULL, 'Day', NULL,
                    'Daily', 1, 0, NULL, 20690, 'Never', NULL, NULL, 1, '', '', '',
                    'MONDAY', NULL, 0, 0, 0, 0, 1, 1, NULL
                )
                """.trimIndent(),
            )
            listOf(
                Triple("entry-skip", "Skipped", 100L),
                Triple("entry-excuse", "Excused", 200L),
                Triple("entry-missing", "Missing", 300L),
                Triple("entry-recorded", "Recorded", 400L),
            ).forEach { (entryId, status, timestamp) ->
                execSQL(
                    "INSERT INTO metric_entries (id, metricId, canonicalValue, enteredValue, enteredUnitId, status, timestampMillis, localEpochDay, zoneId, offsetSeconds, sourceType, sourceId, note, createdAtMillis, updatedAtMillis) VALUES ('$entryId', 'metric-habit', NULL, NULL, NULL, '$status', $timestamp, ${if (status == "Recorded") 20692 else if (status == "Missing") 20691 else 20690}, 'UTC', 0, 'Habit', 'source-$status', '', $timestamp, $timestamp)",
                )
                execSQL(
                    "INSERT INTO habit_logs (uuid, habitId, value, canonicalValue, enteredUnitId, status, timestampMillis, localEpochDay, zoneId, offsetSeconds, note, sourceType, sourceId, metricEntryId, createdAtMillis, updatedAtMillis) VALUES ('log-${status.lowercase()}', 3, NULL, NULL, NULL, '$status', $timestamp, ${if (status == "Recorded") 20692 else if (status == "Missing") 20691 else 20690}, 'UTC', 0, '', 'Manual', NULL, '$entryId', $timestamp, $timestamp)",
                )
            }
            close()
        }

        helper.runMigrationsAndValidate(V8_DATABASE_NAME, 9, true, WhipDatabase.migration8To9).use { database ->
            database.query("SELECT uuid, habitId, localEpochDay, skippedAtMillis FROM habit_skips").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("habit-skip-log-excused", cursor.getString(0))
                assertEquals(3L, cursor.getLong(1))
                assertEquals(20690L, cursor.getLong(2))
                assertEquals(200L, cursor.getLong(3))
                assertEquals(1, cursor.count)
            }
            database.query("SELECT status, metricEntryId FROM habit_logs").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Recorded", cursor.getString(0))
                assertEquals("entry-recorded", cursor.getString(1))
                assertEquals(1, cursor.count)
            }
            database.query("SELECT id FROM metric_entries").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("entry-recorded", cursor.getString(0))
                assertEquals(1, cursor.count)
            }
        }
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.insertMainArea() {
        execSQL(
            "INSERT INTO areas (id, name, nameKey, position, archived, createdAtMillis, updatedAtMillis) " +
                "VALUES ('main', 'Main', 'main', 0, 0, 100, 100)",
        )
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.insertSchemaOneGoal() {
        execSQL(
            """
            INSERT INTO goals (
                id, uuid, metricId, name, description, areaId, area, tagsCsv, icon,
                type, dimension, unitId, precision, baseline, targetMin, targetMax,
                direction, startEpochDay, deadlineEpochDay, aggregation,
                aggregationPeriod, rollingDays, paceType, consistencyPeriod,
                consistencyRequiredPeriods, reminderMinutes, status, pinned,
                position, createdAtMillis, updatedAtMillis
            ) VALUES (
                7, 'goal-7', 'metric-7', 'Preserved Goal', '', 'main', 'Main', '', '🎯',
                'ReachValue', 'Count', 'count', 1, 0, 50, NULL,
                'Increase', 20690, NULL, 'Sum', 'All', NULL, 'None', 'Week',
                NULL, NULL, 'Active', 1, 0, 100, 100
            )
            """.trimIndent(),
        )
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.insertSchemaOneTask() {
        execSQL(
            """
            INSERT INTO tasks (
                id, uuid, title, notes, scheduleKind, dateEpochDay, recurrenceUnit,
                recurrenceInterval, weekdaysMask, recurrenceEnd, recurrenceEndEpochDay,
                recurrenceCount, timeMinutes, reminderEnabled, archived, completedAtMillis,
                createdAtMillis, updatedAtMillis, showSubtaskProgress, progressDisplay,
                autoCompleteFromSteps, repeatStepPolicy, pinned, priority, areaId, area,
                tagsCsv, deadlineEpochDay, recurrenceAnchor, reminderOffsetsMinutesCsv,
                missedOccurrencePolicy, inbox, durationMinutes, effort, manualPosition
            ) VALUES (
                5, 'task-5', 'Preserved Task', '', 'Anytime', NULL, NULL,
                1, 0, NULL, NULL, NULL, NULL, 0, 0, NULL,
                100, 100, 0, 'Percent', 1, 'Reset', 0, 'None', 'main', 'Main',
                '', NULL, 'Schedule', '', 'KeepLatest', 1, NULL, 'Unspecified', 0
            )
            """.trimIndent(),
        )
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.insertSchemaOneTrigger(
        id: Long,
        uuid: String,
        targetType: String,
        autoCompleteTargetHabit: Int,
    ) {
        execSQL(
            """
            INSERT INTO trigger_rules (
                id, uuid, name, sourceType, sourceEntityId, outcome, targetType,
                targetEntityId, delayMinutes, quietStartMinutes, quietEndMinutes,
                autoCompleteTargetHabit, enabled, createdAtMillis, updatedAtMillis
            ) VALUES (
                $id, '$uuid', 'Preserved Trigger', 'Task', 5, 'Completed', '$targetType',
                9, 0, NULL, NULL, $autoCompleteTargetHabit, 1, 100, 100
            )
            """.trimIndent(),
        )
    }

    private companion object {
        const val V1_DATABASE_NAME = "complete-v1-to-v9-migration"
        const val V2_DATABASE_NAME = "complete-v2-to-v9-migration"
        const val V8_DATABASE_NAME = "habit-skip-v8-to-v9-migration"

        val allMigrations: Array<Migration> = arrayOf(
            WhipDatabase.migration1To2,
            WhipDatabase.migration2To3,
            WhipDatabase.migration3To4,
            WhipDatabase.migration4To5,
            WhipDatabase.migration5To6,
            WhipDatabase.migration6To7,
            WhipDatabase.migration7To8,
            WhipDatabase.migration8To9,
        )
    }
}
