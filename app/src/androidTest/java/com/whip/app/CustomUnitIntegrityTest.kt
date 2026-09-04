package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.MeasurementValueKind
import com.whip.app.domain.MeasurementEntryStatus
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.customUnitBoundary
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
class CustomUnitIntegrityTest {
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

    @Test fun exactCreateIsReplaySafeAndRejectsIdentityCollision() = runBlocking {
        assertEquals(
            "unit-request-1",
            measurements.createCustomUnitExact(
                "unit-request-1", "Glass", "gl", UnitDimension.Volume, 250.0,
            ),
        )
        assertEquals(
            "unit-request-1",
            measurements.createCustomUnitExact(
                "unit-request-1", "Glass", "gl", UnitDimension.Volume, 250.0,
            ),
        )
        assertEquals(1, measurements.customUnits.first().size)

        val failure = expectFailure {
            measurements.createCustomUnitExact(
                "unit-request-1", "Different", "d", UnitDimension.Volume, 100.0,
            )
        }
        assertTrue(failure.message.orEmpty().contains("different data"))
        assertEquals("Glass", measurements.customUnits.first().single().name)
    }

    @Test fun normalizedEnsureCannotOverwriteAnExistingConversionIdentity() = runBlocking {
        measurements.createCustomUnitExact("unit-glass", "Glass", "gl", UnitDimension.Volume, 250.0)

        assertEquals(
            "unit-glass",
            measurements.ensureCustomUnit(
                id = "  unit-glass  ",
                name = "Conflicting name",
                symbol = "conflict",
                dimension = UnitDimension.Volume,
                toCanonicalFactor = 999.0,
            ),
        )

        val stored = measurements.customUnits.first().single()
        assertEquals("Glass", stored.name)
        assertEquals("gl", stored.symbol)
        assertEquals(250.0, stored.toCanonicalFactor, 0.0)
    }

    @Test fun measurementValueAndUnitMustAlwaysBeStoredAsAPair() = runBlocking {
        val measurementId = measurements.createMeasurement(
            name = "Water",
            valueKind = MeasurementValueKind.Decimal,
            dimension = UnitDimension.Volume,
            defaultUnitId = "millilitre",
        )

        expectFailure {
            measurements.record(measurementId, value = null, unitId = "millilitre", status = MeasurementEntryStatus.Missing)
        }
        expectFailure {
            measurements.record(measurementId, value = 1.0, unitId = null, status = MeasurementEntryStatus.Missing)
        }
        measurements.record(measurementId, value = null, unitId = null, status = MeasurementEntryStatus.Missing)

        assertEquals(1, measurements.entries.first().size)
    }

    @Test fun staleRenameAndArchiveCannotOverwriteAConcurrentEdit() = runBlocking {
        measurements.createCustomUnitExact("unit-source", "Glass", "gl", UnitDimension.Volume, 250.0)
        val opening = measurements.customUnits.first().single().customUnitBoundary()

        measurements.renameCustomUnitExact(opening, "Tumbler", "tb")
        // Exact achieved-state replay is safe even though the row version changed.
        measurements.renameCustomUnitExact(opening, "Tumbler", "tb")

        val failure = expectFailure {
            measurements.setCustomUnitArchivedExact(opening, true)
        }
        assertTrue(failure.message.orEmpty().contains("changed"))
        val current = measurements.customUnits.first().single()
        assertEquals("Tumbler", current.name)
        assertFalse(current.archived)

        measurements.setCustomUnitArchivedExact(current.customUnitBoundary(), true)
        assertTrue(measurements.customUnits.first().single().archived)
    }

    @Test fun versionRetryCannotForkAndHistoryKeepsItsOriginalUnit() = runBlocking {
        measurements.createCustomUnitExact("unit-source", "Bottle", "btl", UnitDimension.Volume, 500.0)
        val source = measurements.customUnits.first().single().customUnitBoundary()
        val measurementId = measurements.createMeasurement(
            name = "Water",
            valueKind = MeasurementValueKind.Decimal,
            dimension = UnitDimension.Volume,
            defaultUnitId = source.id,
        )
        measurements.record(measurementId = measurementId, value = 2.0, unitId = source.id)

        assertEquals(
            "unit-version-1",
            measurements.createCustomUnitVersionExact(
                source, "unit-version-1", "Large bottle", "lbtl", 750.0,
            ),
        )
        // Same request is an achieved-state replay, not another insert.
        assertEquals(
            "unit-version-1",
            measurements.createCustomUnitVersionExact(
                source, "unit-version-1", "Large bottle", "lbtl", 750.0,
            ),
        )

        val forkFailure = expectFailure {
            measurements.createCustomUnitVersionExact(
                source, "unit-version-2", "Large bottle", "lbtl", 750.0,
            )
        }
        assertTrue(forkFailure.message.orEmpty().contains("changed"))

        val units = measurements.customUnits.first().associateBy { it.id }
        assertEquals(setOf("unit-source", "unit-version-1"), units.keys)
        assertTrue(requireNotNull(units["unit-source"]).archived)
        assertFalse(requireNotNull(units["unit-version-1"]).archived)
        val historical = measurements.entries.first().single()
        assertEquals("unit-source", historical.enteredUnitId)
        assertEquals(1_000.0, historical.canonicalValue ?: -1.0, 0.0)
    }

    @Test fun authoredBoundsAndFinitePositiveConversionAreEnforced() = runBlocking {
        expectFailure {
            measurements.createCustomUnitExact("too-long", "x".repeat(101), "x", UnitDimension.Count, 1.0)
        }
        expectFailure {
            measurements.createCustomUnitExact("symbol-long", "Valid", "x".repeat(21), UnitDimension.Count, 1.0)
        }
        expectFailure {
            measurements.createCustomUnitExact("zero", "Valid", "v", UnitDimension.Count, 0.0)
        }
        expectFailure {
            measurements.createCustomUnitExact("nan", "Valid", "v", UnitDimension.Count, Double.NaN)
        }
        expectFailure {
            measurements.createCustomUnitExact("kilogram", "Shadow kg", "kg", UnitDimension.Mass, 1.0)
        }
        assertTrue(measurements.customUnits.first().isEmpty())
    }

    @Test fun anAlreadyArchivedSourceCannotForkConversionVersions() = runBlocking {
        measurements.createCustomUnitExact("archived-source", "Cup", "cup", UnitDimension.Volume, 250.0)
        val active = measurements.customUnits.first().single().customUnitBoundary()
        measurements.setCustomUnitArchivedExact(active, true)
        val archived = measurements.customUnits.first().single().customUnitBoundary()

        val failure = expectFailure {
            measurements.createCustomUnitVersionExact(
                archived, "archived-fork", "New cup", "nc", 300.0,
            )
        }
        assertTrue(failure.message.orEmpty().contains("Restore"))
        assertEquals(setOf("archived-source"), measurements.customUnits.first().mapTo(mutableSetOf()) { it.id })
    }

    private suspend fun expectFailure(block: suspend () -> Unit): Throwable {
        val result = runCatching { block() }
        assertTrue("Expected operation to fail", result.isFailure)
        return requireNotNull(result.exceptionOrNull())
    }

    private object FixedClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-09-01T12:00:00Z")
        override fun zoneId(): ZoneId = ZoneId.of("UTC")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 9, 1)
    }

    private class SequentialIds : WhipIdGenerator {
        private val count = AtomicInteger()
        override fun nextId(): String = "unit-test-${count.incrementAndGet()}"
    }
}
