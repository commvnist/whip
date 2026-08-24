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
class IdentityEmojiMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WhipDatabase::class.java,
    )

    @Test
    fun migrationThreeToFourNormalizesStoredIdentityWithoutReplacingRows() {
        helper.createDatabase(DATABASE_NAME, 3).apply {
            execSQL(
                "INSERT INTO areas (id, name, nameKey, position, archived, createdAtMillis, updatedAtMillis) " +
                    "VALUES ('main', 'Main', 'main', 0, 0, 1, 1)",
            )
            execSQL(
                "INSERT INTO tracks " +
                    "(id, uuid, name, description, icon, areaId, area, tagsCsv, pinned, archived, position, createdAtMillis, updatedAtMillis) " +
                    "VALUES (7, 'track-7', 'Reading', '', '▤', 'main', 'Main', '', 1, 0, 0, 1, 1)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            DATABASE_NAME,
            4,
            true,
            WhipDatabase.migration3To4,
        ).use { database ->
            database.query("SELECT id, uuid, name, icon, pinned FROM tracks WHERE id = 7").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(7L, cursor.getLong(0))
                assertEquals("track-7", cursor.getString(1))
                assertEquals("Reading", cursor.getString(2))
                assertEquals("📋", cursor.getString(3))
                assertEquals(1, cursor.getInt(4))
            }
        }
    }

    @Test
    fun migrationFourToFiveAddsElapsedGoalStateWithoutChangingExistingGoals() {
        helper.createDatabase(ELAPSED_DATABASE_NAME, 4).apply {
            execSQL(
                "INSERT INTO areas (id, name, nameKey, position, archived, createdAtMillis, updatedAtMillis) " +
                    "VALUES ('main', 'Main', 'main', 0, 0, 1, 1)",
            )
            execSQL(
                "INSERT INTO goals (id, uuid, metricId, name, description, areaId, area, tagsCsv, icon, type, dimension, unitId, precision, baseline, targetMin, targetMax, direction, startEpochDay, deadlineEpochDay, aggregation, aggregationPeriod, rollingDays, paceType, consistencyPeriod, consistencyRequiredPeriods, reminderMinutes, status, pinned, position, createdAtMillis, updatedAtMillis) " +
                    "VALUES (9, 'goal-9', 'metric-9', 'Read', '', 'main', 'Main', '', '🎯', 'ReachValue', 'Count', 'count', 0, 0, 50, NULL, 'Increase', 20000, NULL, 'Latest', 'All', NULL, 'None', 'Week', NULL, NULL, 'Active', 0, 0, 1, 1)",
            )
            close()
        }

        helper.runMigrationsAndValidate(ELAPSED_DATABASE_NAME, 5, true, WhipDatabase.migration4To5).use { database ->
            database.query("SELECT name, elapsedStartMillis, elapsedDisplayUnit FROM goals WHERE id = 9").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Read", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals("Auto", cursor.getString(2))
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "identity-emoji-migration"
        const val ELAPSED_DATABASE_NAME = "elapsed-goal-migration"
    }
}
