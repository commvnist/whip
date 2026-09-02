package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.core.AppSettings
import com.whip.app.core.HealthDataType
import com.whip.app.core.SettingsRepository
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.RoomBackupRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.HealthMetricContract
import com.whip.app.domain.HealthSourceRecord
import com.whip.app.domain.HealthSourceWindow
import com.whip.app.domain.MetricEntryStatus
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.MetricValueKind
import com.whip.app.domain.UnitDimension
import com.whip.app.health.reconcileHealthRecords
import com.whip.app.health.HealthConnectManager
import com.whip.app.health.HealthConnectAvailability
import com.whip.app.health.HealthConnectRuntimeSeam
import com.whip.app.health.HealthWindowReadRequest
import com.whip.app.reminders.CoordinatedMeasurementRepository
import com.whip.app.reminders.ReminderDeliveryCoordinator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthReconciliationTest {
    private lateinit var database: WhipDatabase
    private lateinit var measurements: RoomMeasurementRepository

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WhipDatabase::class.java,
        ).build()
        measurements = RoomMeasurementRepository(database, FixedClock, SequentialIds())
    }

    @After fun tearDown() = database.close()

    @Test fun providerEditsAndDeletionsReconcileInsideTheExactWindow() = runBlocking {
        val first = record("a", 80.0, "2026-08-17T12:00:00Z")
        val second = record("b", 75.0, "2026-08-16T12:00:00Z")
        val window = window(records = listOf(first, second))

        assertEquals(2, reconcileHealthRecords(measurements, window))
        assertEquals(2, measurements.entries.first().size)

        val edited = first.copy(value = 79.5, note = "Edited in provider")
        assertEquals(1, reconcileHealthRecords(measurements, window(records = listOf(edited))))
        val remaining = measurements.entries.first().single()
        assertEquals("entry-health:weight:a", remaining.id)
        assertEquals(79.5, remaining.canonicalValue ?: -1.0, 0.0)
        assertEquals(MetricSourceType.HealthConnect, remaining.sourceType)
        assertEquals("health:weight:a", remaining.sourceId)
        assertTrue(remaining.note.contains("Edited"))

        assertEquals(0, reconcileHealthRecords(measurements, window(records = emptyList())))
        assertTrue(measurements.entries.first().isEmpty())
    }

    @Test fun narrowingTheReadWindowPreservesOlderLocalCopies() = runBlocking {
        val old = record("old", 82.0, "2026-01-15T12:00:00Z")
        val recent = record("recent", 79.0, "2026-08-15T12:00:00Z")
        measurements.reconcileHealthSourceWindows(
            listOf(window(start = "2026-01-01T00:00:00Z", records = listOf(old, recent))),
        )

        measurements.reconcileHealthSourceWindows(
            listOf(window(start = "2026-07-19T00:00:00Z", records = listOf(recent))),
        )

        assertEquals(
            setOf("entry-health:weight:old", "entry-health:weight:recent"),
            measurements.entries.first().mapTo(mutableSetOf()) { it.id },
        )
    }

    @Test fun providerOffsetKeepsTravelHistoryOnItsOriginalDayAcrossResyncs() = runBlocking {
        val traveledWeight = record(
            id = "travel",
            value = 80.0,
            timestamp = "2026-08-17T02:30:00Z",
            zoneOffsetSeconds = -7 * 60 * 60,
        )

        measurements.reconcileHealthSourceWindows(
            listOf(window(zoneId = ZoneId.of("America/Toronto"), records = listOf(traveledWeight))),
        )
        val original = measurements.entries.first().single()
        assertEquals(LocalDate.of(2026, 8, 16), original.localDate)
        assertEquals("-07:00", original.zoneId)
        assertEquals(-7 * 60 * 60, original.offsetSeconds)

        measurements.reconcileHealthSourceWindows(
            listOf(window(zoneId = ZoneId.of("Europe/Helsinki"), records = listOf(traveledWeight))),
        )
        val resynchronized = measurements.entries.first().single()
        assertEquals(original.localDate, resynchronized.localDate)
        assertEquals(original.zoneId, resynchronized.zoneId)
        assertEquals(original.offsetSeconds, resynchronized.offsetSeconds)
    }

    @Test fun missingProviderOffsetKeepsStableHistoryOnItsOriginalDayAcrossResyncs() = runBlocking {
        val weightWithoutOffset = record(
            id = "stable-without-offset",
            value = 80.0,
            timestamp = "2026-08-17T02:30:00Z",
            zoneOffsetSeconds = null,
        )

        measurements.reconcileHealthSourceWindows(
            listOf(window(zoneId = ZoneId.of("America/Toronto"), records = listOf(weightWithoutOffset))),
        )
        val original = measurements.entries.first().single()
        assertEquals(LocalDate.of(2026, 8, 16), original.localDate)
        assertEquals("America/Toronto", original.zoneId)

        measurements.reconcileHealthSourceWindows(
            listOf(window(zoneId = ZoneId.of("Europe/Helsinki"), records = listOf(weightWithoutOffset))),
        )
        val resynchronized = measurements.entries.first().single()
        assertEquals(original.localDate, resynchronized.localDate)
        assertEquals(original.zoneId, resynchronized.zoneId)
        assertEquals(original.offsetSeconds, resynchronized.offsetSeconds)
    }

    @Test fun malformedRecordRollsBackEveryRecordAndMetricInTheBatch() = runBlocking {
        val failure = expectFailure {
            measurements.reconcileHealthSourceWindows(
                listOf(
                    window(records = listOf(record("valid", 80.0), record("invalid", -1.0))),
                ),
            )
        }

        assertTrue(failure.message.orEmpty().contains("negative"))
        assertTrue(measurements.entries.first().isEmpty())
        assertTrue(measurements.metrics.first().isEmpty())
    }

    @Test fun invalidSecondCategoryRollsBackTheFirstCategoryToo() = runBlocking {
        val steps = HealthMetricContract(
            id = "health-connect-steps",
            name = "Health steps",
            valueKind = MetricValueKind.Integer,
            dimension = UnitDimension.Count,
            defaultUnitId = "count",
            precision = 0,
        )
        expectFailure {
            measurements.reconcileHealthSourceWindows(
                listOf(
                    window(records = listOf(record("weight", 80.0))),
                    window(
                        contract = steps,
                        prefix = "health:steps:",
                        records = listOf(record("steps", Double.NaN, unitId = "count")),
                    ),
                ),
            )
        }

        assertTrue(measurements.entries.first().isEmpty())
        assertTrue(measurements.metrics.first().isEmpty())
    }

    @Test fun duplicateProviderIdsAndStableIdentityCollisionsFailClosed() = runBlocking {
        expectFailure {
            measurements.reconcileHealthSourceWindows(
                listOf(window(records = listOf(record("same", 80.0), record("same", 81.0)))),
            )
        }
        assertTrue(measurements.entries.first().isEmpty())

        val otherContract = WEIGHT.copy(id = "health-connect-other-weight", name = "Other health weight")
        measurements.reconcileHealthSourceWindows(
            listOf(window(contract = otherContract, records = listOf(record("same", 80.0)))),
        )
        val failure = expectFailure {
            measurements.reconcileHealthSourceWindows(
                listOf(window(records = listOf(record("same", 81.0)))),
            )
        }
        assertTrue(failure.message.orEmpty().contains("another metric"))
        val preserved = measurements.entries.first().single()
        assertEquals(otherContract.id, preserved.metricId)
        assertEquals(80.0, preserved.canonicalValue ?: -1.0, 0.0)

        val absentCollision = expectFailure {
            measurements.reconcileHealthSourceWindows(
                listOf(window(records = emptyList())),
            )
        }
        assertTrue(absentCollision.message.orEmpty().contains("another metric"))
        assertEquals(otherContract.id, measurements.entries.first().single().metricId)
    }

    @Test fun restoredReservedMetricMismatchCannotReceiveHealthData() = runBlocking {
        measurements.ensureMetric(
            id = WEIGHT.id,
            name = "User-restored conflicting name",
            valueKind = WEIGHT.valueKind,
            dimension = WEIGHT.dimension,
            defaultUnitId = WEIGHT.defaultUnitId,
            precision = WEIGHT.precision,
        )

        val failure = expectFailure {
            measurements.reconcileHealthSourceWindows(
                listOf(window(records = listOf(record("a", 80.0)))),
            )
        }
        assertTrue(failure.message.orEmpty().contains("reserved contract"))
        assertTrue(measurements.entries.first().isEmpty())
        assertEquals("User-restored conflicting name", measurements.metrics.first().single().name)
    }

    @Test fun deleteHealthCopiesPreservesManualHistoryAndMetricDefinitions() = runBlocking {
        val manualMetric = measurements.createMetric(
            name = "Manual body weight",
            valueKind = MetricValueKind.Decimal,
            dimension = UnitDimension.Mass,
            defaultUnitId = "kilogram",
        )
        measurements.record(
            metricId = manualMetric,
            value = 77.0,
            unitId = "kilogram",
            status = MetricEntryStatus.Recorded,
            sourceType = MetricSourceType.Manual,
        )
        measurements.reconcileHealthSourceWindows(
            listOf(window(records = listOf(record("a", 80.0), record("b", 79.0)))),
        )

        assertEquals(2, measurements.deleteHealthConnectEntries())

        val entries = measurements.entries.first()
        assertEquals(1, entries.size)
        assertEquals(MetricSourceType.Manual, entries.single().sourceType)
        assertEquals(setOf(manualMetric, WEIGHT.id), measurements.metrics.first().mapTo(mutableSetOf()) { it.id })
        assertEquals(0, measurements.deleteHealthConnectEntries())
    }

    @Test fun managerKeepsDeletionJournalUntilDerivedStateIsExplicitlyRebuilt() = runBlocking {
        measurements.reconcileHealthSourceWindows(
            listOf(window(records = listOf(record("a", 80.0)))),
        )
        val settings = FakeSettingsRepository(
            AppSettings(
                healthConnectEnabled = true,
                healthDataTypes = setOf(HealthDataType.Weight),
                healthLastSyncMillis = 123L,
                healthLastSyncCount = 1,
            ),
        )
        val manager = HealthConnectManager(
            ApplicationProvider.getApplicationContext(),
            measurements,
            settings,
        )

        val deletion = manager.deleteImportedData()
        assertEquals(1, deletion.deletedEntries)
        assertTrue(measurements.entries.first().isEmpty())
        assertTrue(settings.current().healthConnectDeletionPending)
        assertFalse(settings.current().healthConnectEnabled)
        assertEquals(null, settings.current().healthLastSyncMillis)

        assertTrue(manager.completeImportedDataDeletion())
        assertFalse(settings.current().healthConnectDeletionPending)
    }

    @Test fun productionLockOrderLetsTypedSettingsWaitForAndFollowAnExactHealthSync() = runBlocking {
        val settings = FakeSettingsRepository(
            AppSettings(
                timeZoneId = "UTC",
                healthConnectEnabled = true,
                healthDataTypes = setOf(HealthDataType.Weight),
                healthSyncDays = 30,
            ),
        )
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val permissions = HealthConnectManager(context, measurements, settings)
            .requiredPermissions(setOf(HealthDataType.Weight))
        val providerReadStarted = CompletableDeferred<Unit>()
        val releaseProviderRead = CompletableDeferred<Unit>()
        val seam = object : HealthConnectRuntimeSeam {
            override fun availability() = HealthConnectAvailability.Available
            override suspend fun grantedPermissions() = permissions
            override fun now() = Instant.parse("2026-08-17T16:00:00Z")
            override suspend fun readWindows(request: HealthWindowReadRequest): List<HealthSourceWindow> {
                providerReadStarted.complete(Unit)
                releaseProviderRead.await()
                return listOf(
                    window(
                        start = request.start.toString(),
                        end = request.end.toString(),
                        zoneId = request.zoneId,
                        records = listOf(record("serialized", 80.0)),
                    ),
                )
            }
        }
        val coordinator = ReminderDeliveryCoordinator()
        val manager = HealthConnectManager(
            context,
            CoordinatedMeasurementRepository(measurements, coordinator),
            settings,
            seam,
        )

        val synchronization = async { manager.sync(setOf(HealthDataType.Weight), 30) }
        providerReadStarted.await()
        val timeZoneChange = async {
            manager.withMutationBoundary {
                coordinator.withStateBoundary {
                    settings.updateAndConfirm { it.copy(timeZoneId = "Europe/Helsinki") }
                }
            }
        }
        yield()
        assertFalse(timeZoneChange.isCompleted)

        releaseProviderRead.complete(Unit)
        assertEquals(1, withTimeout(5_000) { synchronization.await() }.importedEntries)
        assertTrue(withTimeout(5_000) { timeZoneChange.await() })
        assertEquals("Europe/Helsinki", settings.current().timeZoneId)
    }

    @Test fun resetAllWaitsForAnInFlightHealthSyncThenLeavesNoLateImportedRows() = runBlocking {
        val settings = FakeSettingsRepository(
            AppSettings(
                timeZoneId = "UTC",
                healthConnectEnabled = true,
                healthDataTypes = setOf(HealthDataType.Weight),
                healthSyncDays = 30,
            ),
        )
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val permissions = HealthConnectManager(context, measurements, settings)
            .requiredPermissions(setOf(HealthDataType.Weight))
        val providerReadStarted = CompletableDeferred<Unit>()
        val releaseProviderRead = CompletableDeferred<Unit>()
        val seam = object : HealthConnectRuntimeSeam {
            override fun availability() = HealthConnectAvailability.Available
            override suspend fun grantedPermissions() = permissions
            override fun now() = Instant.parse("2026-08-17T16:00:00Z")
            override suspend fun readWindows(request: HealthWindowReadRequest): List<HealthSourceWindow> {
                providerReadStarted.complete(Unit)
                releaseProviderRead.await()
                return listOf(
                    window(
                        start = request.start.toString(),
                        end = request.end.toString(),
                        zoneId = request.zoneId,
                        records = listOf(record("before-reset", 80.0)),
                    ),
                )
            }
        }
        val coordinator = ReminderDeliveryCoordinator()
        val manager = HealthConnectManager(
            context,
            CoordinatedMeasurementRepository(measurements, coordinator),
            settings,
            seam,
        )
        val backupRepository = RoomBackupRepository(database, settings)

        val synchronization = async { manager.sync(setOf(HealthDataType.Weight), 30) }
        providerReadStarted.await()
        val reset = async {
            manager.withMutationBoundary {
                coordinator.withStateBoundary { backupRepository.deleteAllData() }
            }
        }
        yield()
        assertFalse(reset.isCompleted)

        releaseProviderRead.complete(Unit)
        assertEquals(1, withTimeout(5_000) { synchronization.await() }.importedEntries)
        withTimeout(5_000) { reset.await() }
        assertTrue(measurements.entries.first().isEmpty())
        assertFalse(settings.current().healthConnectEnabled)
        assertTrue(settings.current().healthDataTypes.isEmpty())
    }

    @Test fun permissionDriftAfterProviderReadFailsBeforeAnyHealthCommit() = runBlocking {
        val settings = FakeSettingsRepository(
            AppSettings(
                timeZoneId = "UTC",
                healthConnectEnabled = true,
                healthDataTypes = setOf(HealthDataType.Weight),
                healthSyncDays = 30,
            ),
        )
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val permissions = HealthConnectManager(context, measurements, settings)
            .requiredPermissions(setOf(HealthDataType.Weight))
        var permissionChecks = 0
        val seam = object : HealthConnectRuntimeSeam {
            override fun availability() = HealthConnectAvailability.Available
            override suspend fun grantedPermissions(): Set<String> =
                if (permissionChecks++ == 0) permissions else emptySet()
            override fun now() = Instant.parse("2026-08-17T16:00:00Z")
            override suspend fun readWindows(request: HealthWindowReadRequest) = listOf(
                window(
                    start = request.start.toString(),
                    end = request.end.toString(),
                    zoneId = request.zoneId,
                    records = listOf(record("permission-drift", 80.0)),
                ),
            )
        }

        val failure = runCatching {
            HealthConnectManager(context, measurements, settings, seam)
                .sync(setOf(HealthDataType.Weight), 30)
        }

        assertTrue(failure.isFailure)
        assertTrue(failure.exceptionOrNull()?.message.orEmpty().contains("access changed"))
        assertTrue(measurements.entries.first().isEmpty())
        assertTrue(measurements.metrics.first().isEmpty())
    }

    @Test fun localDeletionCannotInterleaveWithAnInFlightProviderSync() = runBlocking {
        val settings = FakeSettingsRepository(
            AppSettings(
                timeZoneId = "UTC",
                healthConnectEnabled = true,
                healthDataTypes = setOf(HealthDataType.Weight),
                healthSyncDays = 30,
            ),
        )
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val permissions = HealthConnectManager(context, measurements, settings)
            .requiredPermissions(setOf(HealthDataType.Weight))
        val providerReadStarted = CompletableDeferred<Unit>()
        val releaseProviderRead = CompletableDeferred<Unit>()
        val seam = object : HealthConnectRuntimeSeam {
            override fun availability() = HealthConnectAvailability.Available
            override suspend fun grantedPermissions() = permissions
            override fun now() = Instant.parse("2026-08-17T16:00:00Z")
            override suspend fun readWindows(request: HealthWindowReadRequest): List<HealthSourceWindow> {
                providerReadStarted.complete(Unit)
                releaseProviderRead.await()
                return listOf(
                    window(
                        start = request.start.toString(),
                        end = request.end.toString(),
                        zoneId = request.zoneId,
                        records = listOf(record("before-delete", 80.0)),
                    ),
                )
            }
        }
        val manager = HealthConnectManager(context, measurements, settings, seam)

        val synchronization = async { manager.sync(setOf(HealthDataType.Weight), 30) }
        providerReadStarted.await()
        val deletion = async { manager.deleteImportedData() }
        yield()
        assertFalse(deletion.isCompleted)

        releaseProviderRead.complete(Unit)
        assertEquals(1, synchronization.await().importedEntries)
        assertEquals(1, deletion.await().deletedEntries)
        assertTrue(measurements.entries.first().isEmpty())
        assertTrue(settings.current().healthConnectDeletionPending)
    }

    @Test fun outOfWindowRecordsAndDuplicateWindowsAreRejectedWithoutWrites() = runBlocking {
        expectFailure {
            measurements.reconcileHealthSourceWindows(
                listOf(window(records = listOf(record("late", 80.0, "2026-08-18T00:00:00Z")))),
            )
        }
        assertTrue(measurements.entries.first().isEmpty())
        assertTrue(measurements.metrics.first().isEmpty())

        expectFailure {
            val empty = window(records = emptyList())
            measurements.reconcileHealthSourceWindows(listOf(empty, empty))
        }
        assertFalse(measurements.metrics.first().any { it.id == WEIGHT.id })

        expectFailure {
            measurements.reconcileHealthSourceWindows(
                listOf(window(prefix = "health:%:", records = emptyList())),
            )
        }
        assertTrue(measurements.metrics.first().isEmpty())
    }

    private fun window(
        contract: HealthMetricContract = WEIGHT,
        prefix: String = "health:weight:",
        start: String = "2026-01-01T00:00:00Z",
        end: String = "2026-08-18T00:00:00Z",
        zoneId: ZoneId = ZoneId.of("UTC"),
        records: List<HealthSourceRecord>,
    ) = HealthSourceWindow(
        metric = contract,
        sourcePrefix = prefix,
        startInclusive = Instant.parse(start),
        endExclusive = Instant.parse(end),
        zoneId = zoneId,
        records = records,
    )

    private fun record(
        id: String,
        value: Double,
        timestamp: String = "2026-08-17T12:00:00Z",
        unitId: String = "kilogram",
        zoneOffsetSeconds: Int? = null,
    ) = HealthSourceRecord(
        providerRecordId = id,
        value = value,
        unitId = unitId,
        timestamp = Instant.parse(timestamp),
        zoneOffsetSeconds = zoneOffsetSeconds,
        note = "Imported from test provider",
    )

    private suspend fun expectFailure(block: suspend () -> Unit): Throwable {
        val result = runCatching { block() }
        assertTrue("Expected operation to fail", result.isFailure)
        return requireNotNull(result.exceptionOrNull())
    }

    private object FixedClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-08-17T16:00:00Z")
        override fun zoneId(): ZoneId = ZoneId.of("UTC")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 8, 17)
    }

    private class SequentialIds : WhipIdGenerator {
        private val count = AtomicInteger()
        override fun nextId(): String = "health-test-${count.incrementAndGet()}"
    }

    private class FakeSettingsRepository(initial: AppSettings) : SettingsRepository {
        private val state = MutableStateFlow(initial)
        override val settings: Flow<AppSettings> = state
        override fun current(): AppSettings = state.value
        override fun update(transform: (AppSettings) -> AppSettings) {
            state.value = transform(state.value)
        }
        override fun updateAndConfirm(transform: (AppSettings) -> AppSettings): Boolean {
            update(transform)
            return true
        }
    }

    private companion object {
        val WEIGHT = HealthMetricContract(
            id = "health-connect-weight",
            name = "Health weight",
            valueKind = MetricValueKind.Decimal,
            dimension = UnitDimension.Mass,
            defaultUnitId = "kilogram",
            precision = 2,
        )
    }
}
