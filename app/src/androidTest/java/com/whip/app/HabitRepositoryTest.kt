package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.RoomHabitRepository
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitChecklistItemDraft
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.valueInUnit
import com.whip.app.domain.valueForPeriod
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitRepositoryTest {
    private lateinit var database: WhipDatabase
    private lateinit var repository: RoomHabitRepository
    private lateinit var measurements: RoomMeasurementRepository

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WhipDatabase::class.java).build()
        val ids = SequentialIds()
        measurements = RoomMeasurementRepository(database, FixedClock, ids)
        repository = RoomHabitRepository(database, measurements, FixedClock, ids)
    }

    @After fun tearDown() = database.close()

    @Test fun userDefinedEightCountHabitStoresSixIndependentIncrements() = runBlocking {
        val id = repository.create(HabitDraft(name = "Glasses", trackingMode = HabitTrackingMode.Count, targetMin = 8.0, startDate = FixedClock.today()))
        repeat(6) { repository.log(id, 1.0) }
        assertEquals(6.0, repository.logs.first().sumOf { it.value ?: 0.0 }, 0.0)
    }

    @Test fun skipIsASeparateOccurrenceThatNeverCreatesAMeasurement() = runBlocking {
        val id = repository.create(HabitDraft(name = "Observe", startDate = FixedClock.today()))

        repository.skipDay(id, FixedClock.today())

        assertTrue(repository.logs.first().isEmpty())
        assertTrue(database.measurementDao().observeEntries().first().isEmpty())
        assertEquals(FixedClock.today(), repository.skips.first().single().localDate)

        repository.undoSkip(id, FixedClock.today())
        assertTrue(repository.skips.first().isEmpty())
    }

    @Test fun aRealCheckInClearsSkipAndFailedCheckOffDoesNotInventValueOne() = runBlocking {
        val today = FixedClock.today()
        val id = repository.create(HabitDraft(name = "Check", startDate = today))
        repository.skipDay(id, today)
        repository.setCheckOff(id, today, true)
        assertTrue(repository.skips.first().isEmpty())
        assertEquals(1.0, repository.logs.first().single().value ?: -1.0, 0.0)

        repository.setCheckOff(id, today, false)
        repository.log(id, null, HabitLogStatus.Failed)
        assertEquals(null, repository.logs.first().single().value)
        assertEquals(null, database.measurementDao().observeEntries().first().single().enteredValue)
    }

    @Test fun archiveRetainsHistoryAndUndoRemovesBothRepresentations() = runBlocking {
        val id = repository.create(HabitDraft(name = "Tracked", startDate = FixedClock.today()))
        val logId = repository.log(id, 1.0)
        repository.setArchived(id, true)
        assertTrue(repository.habits.first().single().archived)
        assertFalse(repository.logs.first().isEmpty())
        repository.undoLog(logId)
        assertTrue(repository.logs.first().isEmpty())
    }

    @Test fun checkOffCanBeToggledWithoutLeavingAZeroLog() = runBlocking {
        val id = repository.create(HabitDraft(name = "Check", startDate = FixedClock.today()))
        repository.setCheckOff(id, FixedClock.today(), true)
        repository.setCheckOff(id, FixedClock.today(), true)
        assertEquals(1, repository.logs.first().size)
        repository.setCheckOff(id, FixedClock.today(), false)
        assertTrue(repository.logs.first().isEmpty())
    }

    @Test fun logCanBeBackdatedAndEditedWithoutChangingItsIdentity() = runBlocking {
        val id = repository.create(HabitDraft(name = "Journal", trackingMode = HabitTrackingMode.Count, startDate = FixedClock.today().minusDays(7)))
        val logId = repository.log(id, 1.0, note = "first")

        repository.updateLog(logId, 2.5, HabitLogStatus.Recorded, FixedClock.today().minusDays(2), "corrected")

        val updated = repository.logs.first().single()
        assertEquals(logId, updated.id)
        assertEquals(2.5, updated.value ?: -1.0, 0.0)
        assertEquals(FixedClock.today().minusDays(2), updated.localDate)
        assertEquals("corrected", updated.note)
        assertEquals(FixedClock.today().minusDays(2), database.measurementDao().observeEntries().first().single().localEpochDay.let(LocalDate::ofEpochDay))
    }

    @Test fun customVolumeUnitUsesItsCanonicalConversionFactor() = runBlocking {
        val glassUnitId = measurements.createCustomUnit("glass", "gl", UnitDimension.Volume, 250.0)
        val habitId = repository.create(
            HabitDraft(
                name = "Water",
                trackingMode = HabitTrackingMode.Decimal,
                dimension = UnitDimension.Volume,
                unitId = glassUnitId,
                startDate = FixedClock.today(),
            ),
        )

        repository.log(habitId, 2.0)

        assertEquals(500.0, repository.logs.first().single().canonicalValue ?: -1.0, 0.0)
    }

    @Test fun customMassUnitUsesKilogramsAsItsCanonicalBase() = runBlocking {
        val stoneUnitId = measurements.createCustomUnit("stone", "st", UnitDimension.Mass, 6.35029318)
        val habitId = repository.create(
            HabitDraft(
                name = "Morning weight",
                trackingMode = HabitTrackingMode.Decimal,
                dimension = UnitDimension.Mass,
                unitId = stoneUnitId,
                startDate = FixedClock.today(),
            ),
        )

        repository.log(habitId, 12.0)

        assertEquals(76.20351816, repository.logs.first().single().canonicalValue ?: -1.0, 0.00000001)
    }

    @Test fun customUnitVersionArchivesOldMeaningWithoutReinterpretingHistory() = runBlocking {
        val oldUnitId = measurements.createCustomUnit("glass", "gl", UnitDimension.Volume, 250.0)
        val habitId = repository.create(
            HabitDraft(
                name = "Water",
                trackingMode = HabitTrackingMode.Decimal,
                dimension = UnitDimension.Volume,
                unitId = oldUnitId,
                startDate = FixedClock.today(),
            ),
        )
        repository.log(habitId, 2.0)

        val newUnitId = measurements.createCustomUnitVersion(oldUnitId, "large glass", "lgl", 300.0)
        val units = measurements.customUnits.first()
        val oldUnit = units.single { it.id == oldUnitId }
        val newUnit = units.single { it.id == newUnitId }
        val log = repository.logs.first().single()

        assertTrue(oldUnit.archived)
        assertFalse(newUnit.archived)
        assertEquals(2.0, log.valueInUnit(oldUnitId, units) ?: -1.0, 0.0)
        assertEquals(500.0 / 300.0, log.valueInUnit(newUnitId, units) ?: -1.0, 0.0000001)
    }

    @Test fun changingHabitDisplayUnitDoesNotRelabelOrResaveHistoricalEntry() = runBlocking {
        val today = FixedClock.today()
        val id = repository.create(
            HabitDraft(
                name = "Weight",
                trackingMode = HabitTrackingMode.Decimal,
                dimension = UnitDimension.Mass,
                unitId = "kilogram",
                targetMin = 100.0,
                startDate = today,
            ),
        )
        val logId = repository.log(id, 100.0)
        repository.update(
            id,
            HabitDraft(
                name = "Weight",
                trackingMode = HabitTrackingMode.Decimal,
                dimension = UnitDimension.Mass,
                unitId = "pound",
                targetMin = 220.0,
                startDate = today,
            ),
        )

        val poundsHabit = repository.habits.first().single()
        val original = repository.logs.first().single()
        assertEquals("kilogram", original.enteredUnitId)
        assertEquals(220.4622621849, poundsHabit.valueForPeriod(listOf(original), today), 0.000001)

        repository.updateLog(logId, 100.0, HabitLogStatus.Recorded, today, "unchanged")
        val edited = repository.logs.first().single()
        assertEquals("kilogram", edited.enteredUnitId)
        assertEquals(100.0, edited.canonicalValue ?: -1.0, 0.0)
    }

    @Test fun checklistInsertReorderAndRemovalPreserveItemCompletionIdentity() = runBlocking {
        val today = FixedClock.today()
        val id = repository.create(
            HabitDraft(
                name = "Pack",
                trackingMode = HabitTrackingMode.Checklist,
                startDate = today,
                checklistItems = listOf(
                    HabitChecklistItemDraft("Bottle", 0),
                    HabitChecklistItemDraft("Keys", 1),
                ),
            ),
        )
        val original = repository.checklistItems.first()
        val bottle = original.single { it.name == "Bottle" }
        val keys = original.single { it.name == "Keys" }
        repository.toggleChecklistItem(id, keys.id, today, true)

        repository.update(
            id,
            HabitDraft(
                name = "Pack",
                trackingMode = HabitTrackingMode.Checklist,
                startDate = today,
                checklistItems = listOf(
                    HabitChecklistItemDraft("Wallet", 0),
                    HabitChecklistItemDraft("Keys", 1, keys.id, keys.uuid),
                    HabitChecklistItemDraft("Bottle", 2, bottle.id, bottle.uuid),
                ),
            ),
        )

        val reordered = repository.checklistItems.first()
        assertEquals(listOf("Wallet", "Keys", "Bottle"), reordered.map { it.name })
        assertEquals(keys.id, reordered.single { it.name == "Keys" }.id)
        assertTrue(repository.checklistStates.first().single { it.completed }.itemId == keys.id)

        repository.update(
            id,
            HabitDraft(
                name = "Pack",
                trackingMode = HabitTrackingMode.Checklist,
                startDate = today,
                checklistItems = listOf(
                    HabitChecklistItemDraft("Bottle", 0, bottle.id, bottle.uuid),
                ),
            ),
        )

        assertEquals(listOf(bottle.id), repository.checklistItems.first().map { it.id })
        assertTrue(repository.checklistStates.first().isEmpty())
    }

    @Test fun checklistOnlyAutoCompletesAfterTheFinalItem() = runBlocking {
        val today = FixedClock.today()
        val id = repository.create(
            HabitDraft(
                name = "Medication",
                trackingMode = HabitTrackingMode.Checklist,
                startDate = today,
                checklistItems = listOf(
                    HabitChecklistItemDraft("Medication 1", 0),
                    HabitChecklistItemDraft("Medication 2", 1),
                    HabitChecklistItemDraft("Medication 3", 2),
                ),
            ),
        )
        val items = repository.checklistItems.first()

        repository.toggleChecklistItem(id, items[0].id, today, true)
        repository.toggleChecklistItem(id, items[1].id, today, true)
        assertTrue(repository.logs.first().isEmpty())

        repository.toggleChecklistItem(id, items[2].id, today, true)
        assertEquals(1, repository.logs.first().size)
        assertEquals(1.0, repository.logs.first().single().value ?: -1.0, 0.0)

        repository.toggleChecklistItem(id, items[2].id, today, false)
        assertEquals(1, repository.logs.first().size)
    }

    @Test fun checklistCanKeepItemsIndependentAndUseManualParentCompletion() = runBlocking {
        val today = FixedClock.today()
        val id = repository.create(
            HabitDraft(
                name = "Medication",
                trackingMode = HabitTrackingMode.Checklist,
                autoCompleteFromItems = false,
                startDate = today,
                checklistItems = listOf(
                    HabitChecklistItemDraft("Medication 1", 0),
                    HabitChecklistItemDraft("Medication 2", 1),
                ),
            ),
        )
        assertFalse(repository.habits.first().single().autoCompleteFromItems)

        repository.checklistItems.first().forEach { item ->
            repository.toggleChecklistItem(id, item.id, today, true)
        }
        assertTrue(repository.logs.first().isEmpty())

        repository.setCheckOff(id, today, true)
        assertEquals(1.0, repository.logs.first().single().value ?: -1.0, 0.0)
        repository.setCheckOff(id, today, false)
        assertTrue(repository.logs.first().isEmpty())
    }

    @Test fun invalidEndConditionsAreRejectedAndIrrelevantFieldsAreCleared() = runBlocking {
        val today = FixedClock.today()
        assertTrue(
            runCatching {
                repository.create(
                    HabitDraft(
                        name = "Invalid date",
                        startDate = today,
                        endType = HabitEndType.OnDate,
                    ),
                )
            }.isFailure,
        )
        assertTrue(
            runCatching {
                repository.create(
                    HabitDraft(
                        name = "Invalid threshold",
                        startDate = today,
                        endType = HabitEndType.AfterStreak,
                        endValue = 0.0,
                    ),
                )
            }.isFailure,
        )

        val id = repository.create(
            HabitDraft(
                name = "Normalized",
                startDate = today,
                endType = HabitEndType.Never,
                endDate = today.plusDays(1),
                endValue = 12.0,
            ),
        )
        val saved = repository.habits.first().single { it.id == id }
        assertEquals(null, saved.endDate)
        assertEquals(null, saved.endValue)
    }

    @Test fun notificationIncrementSourceIsIdempotentAcrossRetry() = runBlocking {
        val id = repository.create(
            HabitDraft(name = "Water", trackingMode = HabitTrackingMode.Count, targetMin = 8.0, startDate = FixedClock.today()),
        )

        repository.log(id, 1.0, sourceType = MetricSourceType.Habit, sourceId = "notification-action-1")
        repository.log(id, 1.0, sourceType = MetricSourceType.Habit, sourceId = "notification-action-1")

        assertEquals(1, repository.logs.first().size)
        assertEquals(1.0, repository.logs.first().single().value ?: 0.0, 0.0)
    }

    private object FixedClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-08-17T16:00:00Z")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 8, 17)
    }
    private class SequentialIds : WhipIdGenerator {
        private val count = AtomicInteger()
        override fun nextId() = "habit-test-${count.incrementAndGet()}"
    }
}
