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
    fun migrationOneToSevenPreservesLinksContributionsTriggersAndOccurrences() {
        helper.createDatabase(V1_DATABASE_NAME, 1).apply {
            insertMainArea()
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
            7,
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
        }
    }

    @Test
    fun migrationTwoToSevenPreservesTrackIdentityFieldsEntriesAndFractionalValues() {
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
            7,
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
        const val V1_DATABASE_NAME = "complete-v1-to-v7-migration"
        const val V2_DATABASE_NAME = "complete-v2-to-v7-migration"

        val allMigrations: Array<Migration> = arrayOf(
            WhipDatabase.migration1To2,
            WhipDatabase.migration2To3,
            WhipDatabase.migration3To4,
            WhipDatabase.migration4To5,
            WhipDatabase.migration5To6,
            WhipDatabase.migration6To7,
        )
    }
}
