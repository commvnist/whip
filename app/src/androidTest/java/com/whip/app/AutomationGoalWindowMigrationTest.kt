package com.whip.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whip.app.data.WhipDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutomationGoalWindowMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WhipDatabase::class.java,
    )

    @Test
    fun migrationSixToSevenIncludesExplicitTrackBackfillInGoalWindow() {
        helper.createDatabase(DATABASE_NAME, 6).apply {
            execSQL(
                "INSERT INTO areas (id, name, nameKey, position, archived, createdAtMillis, updatedAtMillis) " +
                    "VALUES ('main', 'Main', 'main', 0, 0, 1, 1)",
            )
            insertGoal(id = 1, uuid = "movies-goal", metricId = "movies-metric", startEpochDay = 20_690)
            insertGoal(id = 2, uuid = "untouched-goal", metricId = "untouched-metric", startEpochDay = 20_690)
            insertRule(id = 10, uuid = "active-backfill", goalId = 1, retroactiveFromEpochDay = 20_689, enabled = 1)
            insertRule(id = 11, uuid = "disabled-older-backfill", goalId = 1, retroactiveFromEpochDay = 20_680, enabled = 0)
            close()
        }

        helper.runMigrationsAndValidate(
            DATABASE_NAME,
            7,
            true,
            WhipDatabase.migration6To7,
        ).use { database ->
            database.query("SELECT id, startEpochDay FROM goals ORDER BY id").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(1L, cursor.getLong(0))
                assertEquals(20_689L, cursor.getLong(1))
                check(cursor.moveToNext())
                assertEquals(2L, cursor.getLong(0))
                assertEquals(20_690L, cursor.getLong(1))
            }
        }
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.insertGoal(
        id: Long,
        uuid: String,
        metricId: String,
        startEpochDay: Long,
    ) {
        execSQL(
            """
            INSERT INTO goals (
                id, uuid, metricId, name, description, areaId, area, tagsCsv, icon,
                type, dimension, unitId, precision, baseline, targetMin, targetMax,
                direction, startEpochDay, deadlineEpochDay, aggregation,
                aggregationPeriod, rollingDays, paceType, consistencyPeriod,
                consistencyRequiredPeriods, elapsedStartMillis, elapsedDisplayUnit,
                reminderMinutes, status, pinned, position, createdAtMillis, updatedAtMillis
            ) VALUES (
                $id, '$uuid', '$metricId', 'Goal $id', '', 'main', 'Main', '', '🎯',
                'ReachValue', 'Count', 'count', 0, NULL, 20, NULL,
                'Increase', $startEpochDay, NULL, 'Sum',
                'All', NULL, 'None', 'Week',
                NULL, NULL, 'Auto', NULL, 'Active', 0, 0, 1, 1
            )
            """.trimIndent(),
        )
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.insertRule(
        id: Long,
        uuid: String,
        goalId: Long,
        retroactiveFromEpochDay: Long,
        enabled: Int,
    ) {
        execSQL(
            """
            INSERT INTO link_rules (
                id, uuid, name, kind, sourceType, sourceEntityId, sourceMetricId,
                sourceItemId, sourceMetric, targetGoalId, targetMilestoneId,
                valueMode, fixedValue, multiplier, offset, retroactiveFromEpochDay,
                enabled, createdAtMillis, updatedAtMillis, trackAggregation,
                sourceFieldId, conditionMode
            ) VALUES (
                $id, '$uuid', 'Movies to Goal', 'Contribution', 'Track', 1, NULL,
                NULL, 'EntryCount', $goalId, NULL,
                'SourceValue', NULL, 1, 0, $retroactiveFromEpochDay,
                $enabled, 1, 1, 'CountEntries', NULL, 'MatchAll'
            )
            """.trimIndent(),
        )
    }

    private companion object {
        const val DATABASE_NAME = "automation-goal-window-migration"
    }
}
