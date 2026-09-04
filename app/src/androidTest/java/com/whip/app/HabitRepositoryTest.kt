package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.core.HabitTimerClock
import com.whip.app.core.HabitTimerClockReading
import com.whip.app.core.AndroidHabitTimerClock
import com.whip.app.data.RoomHabitRepository
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitChecklistItemDraft
import com.whip.app.domain.HabitLogStatus
import com.whip.app.domain.HabitEndType
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.HabitScheduleType
import com.whip.app.domain.TargetComparison
import com.whip.app.domain.HabitTimerBoundary
import com.whip.app.domain.HabitTimerReviewResolution
import com.whip.app.domain.HabitTimerStartOutcome
import com.whip.app.domain.HabitTimerStartRequest
import com.whip.app.domain.HabitTimerStopOutcome
import com.whip.app.domain.MeasurementSourceType
import com.whip.app.domain.MeasurementValueKind
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
    private lateinit var timerClock: MutableHabitTimerClock

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WhipDatabase::class.java).build()
        val ids = SequentialIds()
        timerClock = MutableHabitTimerClock()
        measurements = RoomMeasurementRepository(database, FixedClock, ids)
        repository = RoomHabitRepository(database, measurements, FixedClock, ids, timerClock)
    }

    @After fun tearDown() = database.close()

    @Test fun androidTimerClockProvidesMonotonicBootOwnedReadings() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val first = AndroidHabitTimerClock(context, FixedClock).read()
        val second = AndroidHabitTimerClock(context, FixedClock).read()

        assertEquals(FixedClock.now().toEpochMilli(), first.wallMillis)
        assertTrue(requireNotNull(first.elapsedRealtimeMillis) >= 0L)
        assertTrue(first.bootId.orEmpty().isNotBlank())
        assertTrue(requireNotNull(second.elapsedRealtimeMillis) >= requireNotNull(first.elapsedRealtimeMillis))
        assertEquals(first.bootId, second.bootId)
    }

    @Test fun userDefinedEightCountHabitStoresSixIndependentIncrements() = runBlocking {
        val id = repository.create(HabitDraft(name = "Glasses", trackingMode = HabitTrackingMode.Count, targetMin = 8.0, startDate = FixedClock.today()))
        repeat(6) { repository.log(id, 1.0) }
        assertEquals(6.0, repository.logs.first().sumOf { it.value ?: 0.0 }, 0.0)
    }

    @Test fun repositoryPersistsOnlyTheControlsEnabledByTheHabitConfiguration() = runBlocking {
        val id = repository.create(
            HabitDraft(
                name = "Caffeine",
                trackingMode = HabitTrackingMode.Count,
                comparison = TargetComparison.AtMost,
                targetMin = 3.0,
                targetMax = null,
                scheduleType = HabitScheduleType.Daily,
                scheduleInterval = 0,
                startDate = FixedClock.today(),
            ),
        )

        val saved = requireNotNull(repository.get(id))
        assertEquals(null, saved.targetMin)
        assertEquals(3.0, saved.targetMax ?: -1.0, 0.0)
        assertEquals(1, saved.scheduleInterval)
    }

    @Test fun invalidSourcesAndUnavailableHabitsRejectNewProgress() = runBlocking {
        val today = FixedClock.today()
        val missingSource = HabitDraft(
            name = "Missing source",
            trackingMode = HabitTrackingMode.Count,
            sourceMeasurementId = "missing",
            startDate = today,
        )
        assertTrue(runCatching { repository.create(missingSource) }.isFailure)

        val sourceId = measurements.ensureMeasurement(
            id = "source-count",
            name = "Source count",
            valueKind = MeasurementValueKind.Integer,
            dimension = UnitDimension.Count,
            defaultUnitId = "count",
            precision = 0,
        )
        val syncedId = repository.create(missingSource.copy(name = "Synced", sourceMeasurementId = sourceId))
        assertTrue(runCatching { repository.log(syncedId, 1.0) }.isFailure)

        val pausedId = repository.create(HabitDraft(name = "Paused", startDate = today))
        repository.setPaused(pausedId, true)
        assertTrue(runCatching { repository.log(pausedId, 1.0) }.isFailure)

        val archivedId = repository.create(HabitDraft(name = "Archived", startDate = today))
        repository.setArchived(archivedId, true)
        assertTrue(runCatching { repository.log(archivedId, 1.0) }.isFailure)

        val currentId = repository.create(HabitDraft(name = "Current", startDate = today))
        assertTrue(runCatching { repository.log(currentId, 1.0, date = today.plusDays(1)) }.isFailure)
        assertTrue(repository.logs.first().isEmpty())
    }

    @Test fun reorderNormalizesOmittedArchivedHabitsWithoutDuplicatePositions() = runBlocking {
        val first = repository.create(HabitDraft(name = "First", startDate = FixedClock.today()))
        val second = repository.create(HabitDraft(name = "Second", startDate = FixedClock.today()))
        val archived = repository.create(HabitDraft(name = "Archived", startDate = FixedClock.today()))
        repository.setArchived(archived, true)

        repository.reorder(listOf(second, first))

        val stored = database.habitDao().getAllHabits()
        assertEquals(stored.size, stored.map { it.position }.distinct().size)
        assertEquals(listOf(second, first), stored.sortedBy { it.position }.take(2).map { it.id })
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

    @Test fun historyDeletionRejectsTheWrongHabitOwnerWithoutRemovingAnything() = runBlocking {
        val first = repository.create(HabitDraft(name = "First", startDate = FixedClock.today()))
        val second = repository.create(HabitDraft(name = "Second", startDate = FixedClock.today()))
        val logId = repository.log(first, 1.0)

        assertTrue(runCatching { repository.undoLog(logId, expectedHabitId = second) }.isFailure)
        assertEquals(logId, repository.logs.first().single().id)
        assertEquals(first, repository.undoLog(logId, expectedHabitId = first))
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

    @Test fun settingAPeriodValueUsesTheAuthoritativeStoredTotalInsteadOfAStaleProjection() = runBlocking {
        val today = FixedClock.today()
        val id = repository.create(
            HabitDraft(
                name = "Water",
                trackingMode = HabitTrackingMode.Count,
                targetMin = 8.0,
                startDate = today,
            ),
        )
        repository.log(id, 2.0, date = today)

        val firstSetId = repository.setPeriodValue(id, today, 5.0)
        assertTrue(firstSetId != null)
        assertEquals(listOf(2.0, 3.0), repository.logs.first().sortedBy { it.id }.mapNotNull { it.value })
        assertEquals(null, repository.setPeriodValue(id, today, 5.0))

        // A separate check-in may arrive after the UI projected its value. The
        // next absolute Set must reread Room and write only the remaining delta.
        repository.log(id, 1.0, date = today)
        repository.setPeriodValue(id, today, 8.0)
        val habit = repository.habits.first().single()
        assertEquals(8.0, habit.valueForPeriod(repository.logs.first(), today), 0.0)
        assertEquals(
            listOf(2.0, 3.0, 1.0, 2.0),
            repository.logs.first().sortedBy { it.id }.mapNotNull { it.value },
        )
    }

    @Test fun settingDisplayedDecimalTotalDoesNotCreateMicroscopicCorrectionLog() = runBlocking {
        val today = FixedClock.today()
        val id = repository.create(
            HabitDraft(
                name = "Water",
                trackingMode = HabitTrackingMode.Decimal,
                targetMin = 1.0,
                startDate = today,
            ),
        )
        repository.log(id, 0.1, date = today)
        repository.log(id, 0.2, date = today)

        assertEquals(null, repository.setPeriodValue(id, today, 0.3))
        assertEquals(2, repository.logs.first().size)
    }

    @Test fun settingHighMagnitudeTotalNeverTreatsARealChangeAsFloatingNoise() = runBlocking {
        val today = FixedClock.today()
        val id = repository.create(
            HabitDraft(
                name = "Large total",
                trackingMode = HabitTrackingMode.Count,
                targetMin = 1.0,
                startDate = today,
            ),
        )
        repository.log(id, 1_000_000_000_000.0, date = today)

        val correctionId = repository.setPeriodValue(id, today, 999_999_999_500.0)
        val oneUnitCorrectionId = repository.setPeriodValue(id, today, 999_999_999_501.0)

        assertTrue(correctionId != null)
        assertTrue(oneUnitCorrectionId != null)
        assertEquals(
            listOf(1_000_000_000_000.0, -500.0, 1.0),
            repository.logs.first().sortedBy { it.id }.mapNotNull { it.value },
        )
    }

    @Test fun settingCustomUnitTotalUsesStoredConversionsAndNoOpsWhenUnchanged() = runBlocking {
        val today = FixedClock.today()
        val glassUnitId = measurements.createCustomUnit("glass", "gl", UnitDimension.Volume, 250.0)
        val id = repository.create(
            HabitDraft(
                name = "Water",
                trackingMode = HabitTrackingMode.Decimal,
                dimension = UnitDimension.Volume,
                unitId = glassUnitId,
                targetMin = 8.0,
                startDate = today,
            ),
        )
        repository.log(id, 2.0, date = today)

        repository.setPeriodValue(id, today, 3.0)
        assertEquals(3.0, repository.habits.first().single().valueForPeriod(repository.logs.first(), today), 0.0)
        assertEquals(750.0, repository.logs.first().sumOf { it.canonicalValue ?: 0.0 }, 0.0)
        assertEquals(null, repository.setPeriodValue(id, today, 3.0))
        assertEquals(2, repository.logs.first().size)
    }

    @Test fun scheduledPausesCanBeCreatedEditedMadeOpenEndedAndDeleted() = runBlocking {
        val today = FixedClock.today()
        val habitId = repository.create(HabitDraft(name = "Training", startDate = today))

        val pauseId = repository.addPause(habitId, today.plusDays(1), today.plusDays(4), "  Travel  ")
        val created = repository.pauses.first().single()
        assertEquals(pauseId, created.id)
        assertEquals("Travel", created.note)
        assertEquals(today.plusDays(4), created.endDate)

        assertEquals(habitId, repository.updatePause(pauseId, today.plusDays(2), null, "Recovery"))
        val updated = repository.pauses.first().single()
        assertEquals(today.plusDays(2), updated.startDate)
        assertEquals(null, updated.endDate)
        assertEquals("Recovery", updated.note)

        assertEquals(habitId, repository.deletePause(pauseId))
        assertTrue(repository.pauses.first().isEmpty())
        assertTrue(runCatching { repository.deletePause(pauseId) }.isFailure)
    }

    @Test fun scheduledPauseMutationsRejectInvalidOrMissingTargets() = runBlocking {
        val today = FixedClock.today()
        val habitId = repository.create(HabitDraft(name = "Training", startDate = today))
        assertTrue(
            runCatching {
                repository.addPause(habitId, today.plusDays(2), today.plusDays(1))
            }.isFailure,
        )
        assertTrue(runCatching { repository.addPause(Long.MAX_VALUE, today, null) }.isFailure)
        assertTrue(runCatching { repository.updatePause(Long.MAX_VALUE, today, null) }.isFailure)
    }

    @Test fun skipUndoRequiresAnExistingExactRecord() = runBlocking {
        val today = FixedClock.today()
        val habitId = repository.create(HabitDraft(name = "Training", startDate = today))
        repository.skipDay(habitId, today)

        repository.undoSkip(habitId, today)

        assertTrue(repository.skips.first().isEmpty())
        assertTrue(runCatching { repository.undoSkip(habitId, today) }.isFailure)
        assertTrue(runCatching { repository.undoSkip(Long.MAX_VALUE, today) }.isFailure)
    }

    @Test fun historyAndPauseUpdatesRejectTheWrongHabitOwnerWithoutMutation() = runBlocking {
        val today = FixedClock.today()
        val first = repository.create(
            HabitDraft(name = "First", trackingMode = HabitTrackingMode.Count, startDate = today),
        )
        val second = repository.create(
            HabitDraft(name = "Second", trackingMode = HabitTrackingMode.Count, startDate = today),
        )
        val logId = repository.log(first, 1.0, date = today, note = "original")
        val pauseId = repository.addPause(first, today.plusDays(1), today.plusDays(2), "original")

        assertTrue(
            runCatching {
                repository.updateLog(
                    logId,
                    9.0,
                    HabitLogStatus.Recorded,
                    today.minusDays(1),
                    "wrong owner",
                    expectedHabitId = second,
                )
            }.isFailure,
        )
        assertTrue(
            runCatching {
                repository.updatePause(
                    pauseId,
                    today.plusDays(4),
                    null,
                    "wrong owner",
                    expectedHabitId = second,
                )
            }.isFailure,
        )
        assertTrue(runCatching { repository.deletePause(pauseId, expectedHabitId = second) }.isFailure)

        val log = repository.logs.first().single()
        assertEquals(1.0, log.value ?: -1.0, 0.0)
        assertEquals(today, log.localDate)
        assertEquals("original", log.note)
        val pause = repository.pauses.first().single()
        assertEquals(today.plusDays(1), pause.startDate)
        assertEquals(today.plusDays(2), pause.endDate)
        assertEquals("original", pause.note)
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

    @Test fun editingHabitLogKeepsBackingMeasurementOwnedByHabitWrapper() = runBlocking {
        val today = FixedClock.today()
        val habitId = repository.create(
            HabitDraft(name = "Water", trackingMode = HabitTrackingMode.Count, startDate = today),
        )
        val logId = repository.log(
            habitId,
            1.0,
            sourceType = MeasurementSourceType.Habit,
            sourceId = "notification-action-1",
        )
        val logUuid = repository.logs.first().single().uuid

        repository.updateLog(logId, 2.0, HabitLogStatus.Recorded, today, "corrected")

        val measurement = database.measurementDao().observeEntries().first().single()
        assertEquals(MeasurementSourceType.Habit.name, measurement.sourceType)
        assertEquals(logUuid, measurement.sourceId)
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

        val retained = repository.checklistItems.first()
        assertEquals(listOf(bottle.id), retained.filterNot { it.archived }.map { it.id })
        assertEquals(setOf(keys.id, reordered.single { it.name == "Wallet" }.id), retained.filter { it.archived }.map { it.id }.toSet())
        assertEquals(keys.id, repository.checklistStates.first().single { it.completed }.itemId)
    }

    @Test fun checklistPersistenceDiscardsIrrelevantNumericQuickAdds() = runBlocking {
        val id = repository.create(
            HabitDraft(
                name = "Pack",
                trackingMode = HabitTrackingMode.Checklist,
                quickIncrement = Double.NaN,
                quickActions = listOf(-1.0, 5.0, 10.0),
                startDate = FixedClock.today(),
                checklistItems = listOf(HabitChecklistItemDraft("Keys", 0)),
            ),
        )

        val saved = requireNotNull(repository.get(id))
        assertEquals(1.0, saved.quickIncrement, 0.0)
        assertTrue(saved.quickActions.isEmpty())
    }

    @Test fun archivedChecklistHistoryDoesNotCompleteTheRemainingActiveChecklist() = runBlocking {
        val today = FixedClock.today()
        val id = repository.create(
            HabitDraft(
                name = "Pack",
                trackingMode = HabitTrackingMode.Checklist,
                startDate = today,
                checklistItems = listOf(
                    HabitChecklistItemDraft("Keys", 0),
                    HabitChecklistItemDraft("Bottle", 1),
                ),
            ),
        )
        val original = repository.checklistItems.first()
        val keys = original.single { it.name == "Keys" }
        val bottle = original.single { it.name == "Bottle" }
        repository.toggleChecklistItem(id, keys.id, today, true)

        repository.update(
            id,
            HabitDraft(
                name = "Pack",
                trackingMode = HabitTrackingMode.Checklist,
                startDate = today,
                checklistItems = listOf(HabitChecklistItemDraft("Bottle", 0, bottle.id, bottle.uuid)),
            ),
        )
        repository.toggleChecklistItem(id, bottle.id, today, false)

        assertTrue(repository.logs.first().isEmpty())
        assertEquals(keys.id, repository.checklistStates.first().single { it.completed }.itemId)
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

        repository.log(id, 1.0, sourceType = MeasurementSourceType.Habit, sourceId = "notification-action-1")
        repository.log(id, 1.0, sourceType = MeasurementSourceType.Habit, sourceId = "notification-action-1")

        assertEquals(1, repository.logs.first().size)
        assertEquals(1.0, repository.logs.first().single().value ?: 0.0, 0.0)
    }

    @Test fun fiveMinuteTimerWritesMinutesAndCanonicalSecondsExactlyOnce() = runBlocking {
        val habitId = repository.create(durationHabit("Meditation", "minute"))
        val habit = requireNotNull(repository.get(habitId))
        val boundary = HabitTimerBoundary(habit.id, habit.uuid, "timer-five-minutes")

        repository.startTimer(HabitTimerStartRequest(habit.id, habit.uuid, boundary.sessionId))
        assertEquals(timerClock.elapsedMillis, repository.get(habitId)?.timerAnchorElapsedRealtimeMillis)
        timerClock.advance(elapsedMillis = 300_000L, wallMillis = 3_600_000L)
        val first = repository.stopTimer(boundary)
        val repeated = repository.stopTimer(boundary)

        assertTrue(first is HabitTimerStopOutcome.Stopped)
        assertTrue(repeated is HabitTimerStopOutcome.AlreadyCompleted)
        val log = repository.logs.first().single()
        assertEquals(5.0, log.value ?: -1.0, 0.0)
        assertEquals(300.0, log.canonicalValue ?: -1.0, 0.0)
        assertEquals("minute", log.enteredUnitId)
        val entry = database.measurementDao().observeEntries().first().single()
        assertEquals(5.0, entry.enteredValue ?: -1.0, 0.0)
        assertEquals(300.0, entry.canonicalValue ?: -1.0, 0.0)
    }

    @Test fun timerUsesSnapshottedCustomDurationUnitAndRejectsStaleStop() = runBlocking {
        val customUnit = measurements.createCustomUnit("Focus block", "block", UnitDimension.Duration, 1_500.0)
        val habitId = repository.create(durationHabit("Focus", customUnit))
        val habit = requireNotNull(repository.get(habitId))
        val firstBoundary = HabitTimerBoundary(habit.id, habit.uuid, "timer-a")
        repository.startTimer(HabitTimerStartRequest(habit.id, habit.uuid, firstBoundary.sessionId))
        timerClock.advance(elapsedMillis = 300_000L)
        repository.stopTimer(firstBoundary)
        val secondBoundary = HabitTimerBoundary(habit.id, habit.uuid, "timer-b")
        repository.startTimer(HabitTimerStartRequest(habit.id, habit.uuid, secondBoundary.sessionId))

        val stale = repository.stopTimer(firstBoundary)

        assertTrue(stale is HabitTimerStopOutcome.AlreadyCompleted)
        assertEquals(secondBoundary.sessionId, repository.get(habitId)?.timerSessionId)
        val log = repository.logs.first().single()
        assertEquals(0.2, log.value ?: -1.0, 0.0)
        assertEquals(300.0, log.canonicalValue ?: -1.0, 0.0)
        assertEquals(customUnit, log.enteredUnitId)
    }

    @Test fun competingStartRequestIsConsumedAndCannotStartLater() = runBlocking {
        val habitId = repository.create(durationHabit("Meditation", "minute"))
        val habit = requireNotNull(repository.get(habitId))
        val firstRequest = HabitTimerStartRequest(habit.id, habit.uuid, "timer-first")
        val delayedRequest = HabitTimerStartRequest(habit.id, habit.uuid, "timer-delayed")

        val started = repository.startTimer(firstRequest) as HabitTimerStartOutcome.Started
        assertTrue(repository.startTimer(delayedRequest) is HabitTimerStartOutcome.AlreadyRunning)
        timerClock.advance(elapsedMillis = 30_000L)
        repository.stopTimer(started.boundary)

        assertEquals(HabitTimerStartOutcome.AlreadyResolved, repository.startTimer(delayedRequest))
        assertEquals(null, repository.get(habitId)?.timerSessionId)
        assertEquals(1, repository.logs.first().size)
    }

    @Test fun rebootRequiresReviewBeforeLoggingAndCanContinueFromConfirmedDuration() = runBlocking {
        val habitId = repository.create(durationHabit("Meditation", "second"))
        val habit = requireNotNull(repository.get(habitId))
        val boundary = HabitTimerBoundary(habit.id, habit.uuid, "timer-review")
        repository.startTimer(HabitTimerStartRequest(habit.id, habit.uuid, boundary.sessionId))
        timerClock.advance(elapsedMillis = 90_000L, wallMillis = 90_000L)
        timerClock.bootId = "boot-2"

        val review = repository.stopTimer(boundary)
        assertTrue(review is HabitTimerStopOutcome.ReviewRequired)
        assertTrue(requireNotNull(repository.get(habitId)).timerNeedsReview)
        assertTrue(repository.logs.first().isEmpty())

        repository.resolveTimerReview(boundary, HabitTimerReviewResolution.Continue(90.0))
        timerClock.advance(elapsedMillis = 30_000L, wallMillis = 30_000L)
        val stopped = repository.stopTimer(boundary)

        assertTrue(stopped is HabitTimerStopOutcome.Stopped)
        assertEquals(120.0, repository.logs.first().single().canonicalValue ?: -1.0, 0.0)
    }

    @Test fun runningTimerBlocksArchivePauseAndIncompatibleEdit() = runBlocking {
        val habitId = repository.create(durationHabit("Meditation", "minute"))
        val habit = requireNotNull(repository.get(habitId))
        repository.startTimer(HabitTimerStartRequest(habit.id, habit.uuid, "timer-guards"))

        assertTrue(runCatching { repository.setArchived(habitId, true) }.isFailure)
        assertTrue(runCatching { repository.setPaused(habitId, true) }.isFailure)
        assertTrue(runCatching {
            repository.update(habitId, durationHabit("Meditation", "second"))
        }.isFailure)
        assertEquals("timer-guards", repository.get(habitId)?.timerSessionId)
    }

    private fun durationHabit(name: String, unitId: String) = HabitDraft(
        name = name,
        trackingMode = HabitTrackingMode.Duration,
        dimension = UnitDimension.Duration,
        unitId = unitId,
        precision = 2,
        targetMin = 5.0,
        quickIncrement = 1.0,
        startDate = FixedClock.today(),
    )

    private object FixedClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-08-17T16:00:00Z")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 8, 17)
    }
    private class SequentialIds : WhipIdGenerator {
        private val count = AtomicInteger()
        override fun nextId() = "habit-test-${count.incrementAndGet()}"
    }

    private class MutableHabitTimerClock : HabitTimerClock {
        var wallMillis: Long = FixedClock.now().toEpochMilli()
        var elapsedMillis: Long = 10_000L
        var bootId: String = "boot-1"

        override fun read() = HabitTimerClockReading(wallMillis, elapsedMillis, bootId)

        fun advance(elapsedMillis: Long, wallMillis: Long = elapsedMillis) {
            this.elapsedMillis += elapsedMillis
            this.wallMillis += wallMillis
        }
    }
}
