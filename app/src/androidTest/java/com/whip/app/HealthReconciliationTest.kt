package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.MetricSourceType
import com.whip.app.domain.MetricValueKind
import com.whip.app.domain.UnitDimension
import com.whip.app.health.HealthRecordSnapshot
import com.whip.app.health.reconcileHealthRecords
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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

    @Test fun providerEditsAreUpsertedAndProviderDeletionsAreMirrored() = runBlocking {
        measurements.ensureMetric(
            id = "health.weight",
            name = "Weight",
            valueKind = MetricValueKind.Decimal,
            dimension = UnitDimension.Mass,
            defaultUnitId = "kilogram",
        )
        val first = HealthRecordSnapshot("a", 80.0, "kilogram", FixedClock.now())
        val second = HealthRecordSnapshot("b", 75.0, "kilogram", FixedClock.now().minusSeconds(86_400))

        assertEquals(2, reconcileHealthRecords(measurements, "health.weight", "health:weight:", listOf(first, second), ZoneId.of("UTC")))
        assertEquals(2, measurements.entries.first().size)

        val edited = first.copy(value = 79.5, note = "Edited in provider")
        assertEquals(1, reconcileHealthRecords(measurements, "health.weight", "health:weight:", listOf(edited), ZoneId.of("UTC")))
        val remaining = measurements.entries.first().single()
        assertEquals("entry-health:weight:a", remaining.id)
        assertEquals(79.5, remaining.canonicalValue ?: -1.0, 0.0)
        assertEquals(MetricSourceType.HealthConnect, remaining.sourceType)
        assertEquals("health:weight:a", remaining.sourceId)
        assertTrue(remaining.note.contains("Edited"))

        reconcileHealthRecords(measurements, "health.weight", "health:weight:", emptyList(), ZoneId.of("UTC"))
        assertTrue(measurements.entries.first().isEmpty())
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
}
