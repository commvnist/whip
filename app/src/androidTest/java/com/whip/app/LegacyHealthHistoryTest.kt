package com.whip.app

import android.content.pm.PackageManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.whip.app.core.SystemWhipClock
import com.whip.app.core.UuidWhipIdGenerator
import com.whip.app.data.*
import com.whip.app.domain.*
import com.whip.app.ui.mirrorMeasurementEntriesAsHabitLogs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LegacyHealthHistoryTest {
    private lateinit var database: WhipDatabase
    private lateinit var measurements: RoomMeasurementRepository
    private lateinit var habits: RoomHabitRepository
    private lateinit var backups: RoomBackupRepository

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WhipDatabase::class.java).build()
        measurements = RoomMeasurementRepository(database, SystemWhipClock, UuidWhipIdGenerator)
        habits = RoomHabitRepository(database, measurements, SystemWhipClock, UuidWhipIdGenerator)
        backups = RoomBackupRepository(database)
    }

    @After fun close() = database.close()

    @Test fun retirementPreservesCompleteProjectedAndManualHistoryAndAllowsNewCheckIns() = runBlocking {
        val id = seed()
        val manualId = habits.log(id, 155.0, note = "Earlier manual history")
        link(id)
        val beforeHabit = database.habitDao().getHabit(id)!!
        val entries = measurements.entries.first()
        val definitions = measurements.measurements.first()
        val projected = mirrorMeasurementEntriesAsHabitLogs(habits.habits.first(), entries, measurements.customUnits.first())
        val manual = habits.logs.first().single { it.id == manualId }
        database.retireLegacyHealthHabitSources()
        assertEquals(beforeHabit.copy(sourceMeasurementId = null), database.habitDao().getHabit(id))
        val after = habits.logs.first()
        projected.forEach { expected ->
            assertEquals(expected.copy(id = after.single { it.uuid == expected.uuid }.id), after.single { it.uuid == expected.uuid })
        }
        assertEquals(manual, after.single { it.id == manualId })
        assertEquals(entries, measurements.entries.first())
        assertEquals(definitions, measurements.measurements.first())
        database.retireLegacyHealthHabitSources()
        assertEquals(after, habits.logs.first())
        habits.log(id, 158.0, note = "New manual check-in")
        assertTrue(habits.logs.first().any { it.note == "New manual check-in" && it.sourceType == MeasurementSourceType.Manual })
        assertTrue(habits.logs.first().containsAll(after))
    }

    @Test fun interruptedRetirementRollsBackAllHabitsAndLogsThenRetriesExactlyOnce() = runBlocking {
        val first = seed()
        measurements.createCustomUnitExact("mass-scoop", "Mass scoop", "sc", UnitDimension.Mass, 0.5)
        val second = habits.create(HabitDraft(name = "Archived weight", trackingMode = HabitTrackingMode.Decimal, dimension = UnitDimension.Mass, unitId = "mass-scoop", startDate = SystemWhipClock.today()))
        link(first)
        link(second)
        database.habitDao().updateHabit(database.habitDao().getHabit(second)!!.copy(archived = true))
        val before = database.habitDao().getAllHabits()
        val projected = mirrorMeasurementEntriesAsHabitLogs(habits.habits.first(), measurements.entries.first(), measurements.customUnits.first())
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_retirement BEFORE UPDATE ON habits WHEN OLD.id = $second AND NEW.sourceMeasurementId IS NULL BEGIN SELECT RAISE(ABORT, 'simulated interrupted write'); END")
        assertTrue(runCatching { database.retireLegacyHealthHabitSources() }.isFailure)
        assertEquals(before, database.habitDao().getAllHabits())
        assertTrue(database.habitDao().getAllLogs().isEmpty())
        database.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_retirement")
        database.retireLegacyHealthHabitSources()
        val logs = database.habitDao().getAllLogs()
        assertEquals(4, logs.size)
        val after = habits.logs.first()
        projected.forEach { expected -> assertEquals(expected.copy(id = after.single { it.uuid == expected.uuid }.id), after.single { it.uuid == expected.uuid }) }
        assertTrue(database.habitDao().getHabit(second)!!.archived)
        database.retireLegacyHealthHabitSources()
        assertEquals(logs, database.habitDao().getAllLogs())
    }

    @Test fun oldBackupsRestoreAndMergeHistoryWithoutRestoringTheSourceLink() = runBlocking {
        val id = seed()
        link(id)
        val entries = measurements.entries.first()
        val oldBackup = backups.exportBackup()
        backups.restoreBackup(oldBackup)
        assertNull(habits.habits.first().single().sourceMeasurementId)
        val restored = habits.logs.first()
        assertEquals(2, restored.size)
        assertEquals(entries, measurements.entries.first())
        backups.mergeBackup(oldBackup)
        assertEquals(restored, habits.logs.first())
        backups.deleteAllData()
        backups.mergeBackup(oldBackup)
        assertNull(habits.habits.first().single().sourceMeasurementId)
        assertEquals(2, habits.logs.first().size)
        assertEquals(entries, measurements.entries.first())
        val merged = habits.logs.first()
        backups.mergeBackup(oldBackup)
        assertEquals(merged, habits.logs.first())
    }

    @Test fun anEmptyRetiredSourceBecomesManualWithoutInventingHistory() = runBlocking {
        val id = seed()
        link(id)
        measurements.entries.first().forEach { measurements.deleteEntry(it.id) }
        database.retireLegacyHealthHabitSources()
        assertTrue(habits.logs.first().isEmpty())
        assertNull(habits.habits.first().single().sourceMeasurementId)
        habits.log(id, 150.0)
        assertEquals(1, habits.logs.first().size)
    }

    @Test fun installedAppHasNoHealthPermissionsActivitiesOrSdk() {
        val app = ApplicationProvider.getApplicationContext<WhipApplication>()
        val info = app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES)
        assertFalse(info.requestedPermissions.orEmpty().any { it.startsWith("android.permission.health.") })
        assertFalse(info.activities.orEmpty().any { it.name.contains("HealthPermission") })
        assertTrue(runCatching { Class.forName("androidx.health.connect.client.HealthConnectClient") }.isFailure)
    }

    private suspend fun seed(): Long {
        measurements.ensureMeasurement("health-connect-weight", "Health weight", MeasurementValueKind.Decimal, UnitDimension.Mass, "kilogram", 2)
        measurements.record("health-connect-weight", 70.0, "kilogram", sourceType = MeasurementSourceType.HealthConnect, sourceId = "health:weight:original", note = "Original imported weight")
        measurements.record("health-connect-weight", 71.0, "kilogram", status = MeasurementEntryStatus.Failed, sourceType = MeasurementSourceType.HealthConnect, sourceId = "health:weight:failed", note = "Original failed observation")
        measurements.record("health-connect-weight", null, null, status = MeasurementEntryStatus.Missing)
        return habits.create(HabitDraft(name = "Weight history", trackingMode = HabitTrackingMode.Decimal, dimension = UnitDimension.Mass, unitId = "pound", startDate = SystemWhipClock.today()))
    }

    private suspend fun link(id: Long) {
        database.habitDao().updateHabit(database.habitDao().getHabit(id)!!.copy(sourceMeasurementId = "health-connect-weight"))
    }
}
