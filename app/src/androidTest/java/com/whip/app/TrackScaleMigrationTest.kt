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
class TrackScaleMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WhipDatabase::class.java,
    )

    @Test
    fun migrationFourToSixPreservesIntegerRatingsAsFractionalReadyScales() {
        helper.createDatabase(DATABASE_NAME, 4).apply {
            execSQL(
                "INSERT INTO areas (id, name, nameKey, position, archived, createdAtMillis, updatedAtMillis) " +
                    "VALUES ('main', 'Main', 'main', 0, 0, 1, 1)",
            )
            execSQL(
                "INSERT INTO tracks " +
                    "(id, uuid, name, description, icon, areaId, area, tagsCsv, pinned, archived, position, createdAtMillis, updatedAtMillis) " +
                    "VALUES (1, 'films', 'Films', '', '🎬', 'main', 'Main', '', 0, 0, 0, 1, 1)",
            )
            execSQL(
                "INSERT INTO track_fields " +
                    "(id, uuid, trackId, name, type, position, required, primaryField, showInList, dimension, unitId, precision, scaleMin, scaleMax, scaleLowLabel, scaleHighLabel, createdAtMillis, updatedAtMillis) " +
                    "VALUES (2, 'rating', 1, 'Rating', 'Scale', 0, 1, 1, 1, NULL, NULL, 0, 1, 5, 'Poor', 'Excellent', 1, 1)",
            )
            execSQL(
                "INSERT INTO track_entries " +
                    "(id, uuid, trackId, entryEpochDay, sourceOccurrenceId, sourceExplanation, createdAtMillis, updatedAtMillis) " +
                    "VALUES (3, 'arrival', 1, 20689, NULL, '', 1, 1)",
            )
            execSQL(
                "INSERT INTO track_values " +
                    "(id, uuid, entryId, fieldId, textValue, enteredNumber, canonicalNumber, enteredUnitId, dateEpochDay, booleanValue, choiceOptionId, scaleValue, createdAtMillis, updatedAtMillis) " +
                    "VALUES (4, 'arrival-rating', 3, 2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 4, 1, 1)",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            DATABASE_NAME,
            6,
            true,
            WhipDatabase.migration4To5,
            WhipDatabase.migration5To6,
        ).use { database ->
            database.query(
                "SELECT f.scaleStep, v.scaleValue, typeof(v.scaleValue) " +
                    "FROM track_fields f JOIN track_values v ON v.fieldId = f.id WHERE f.id = 2",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(1.0, cursor.getDouble(0), 0.0)
                assertEquals(4.0, cursor.getDouble(1), 0.0)
                assertEquals("real", cursor.getString(2))
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "fractional-track-scale-migration"
    }
}
