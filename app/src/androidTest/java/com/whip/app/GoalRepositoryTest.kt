package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.RoomGoalRepository
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalAggregationPeriod
import com.whip.app.domain.GoalConsistencyPeriod
import com.whip.app.domain.GoalDirection
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalMilestoneDraft
import com.whip.app.domain.GoalPaceType
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.GoalType
import com.whip.app.domain.ElapsedDisplayUnit
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.displayValue
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
class GoalRepositoryTest {
    private lateinit var database: WhipDatabase
    private lateinit var repository: RoomGoalRepository
    private lateinit var measurements: RoomMeasurementRepository

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WhipDatabase::class.java).build()
        val ids = SequentialIds()
        measurements = RoomMeasurementRepository(database, FixedClock, ids)
        repository = RoomGoalRepository(database, measurements, FixedClock, ids)
    }
    @After fun tearDown() = database.close()

    @Test fun cumulativeGoalStoresBackdatedContributions() = runBlocking {
        val id = repository.create(GoalDraft(name = "Run 500 km", type = GoalType.AccumulateTotal, dimension = UnitDimension.Distance, unitId = "kilometre", targetMin = 500.0, startDate = FixedClock.today(), aggregation = GoalAggregation.Sum))
        repository.recordMeasurement(id, 5.0, date = FixedClock.today().minusDays(1))
        repository.recordMeasurement(id, 7.0, date = FixedClock.today())
        val entries = repository.metricEntries.first()
        assertEquals(2, entries.size)
        assertEquals(setOf(FixedClock.today().minusDays(1), FixedClock.today()), entries.map { it.localDate }.toSet())
    }

    @Test fun milestoneStateAndCompletedStatusPersist() = runBlocking {
        val id = repository.create(GoalDraft(name = "Ship", type = GoalType.WeightedMilestones, startDate = FixedClock.today(), milestones = listOf(GoalMilestoneDraft("Build", 2.0), GoalMilestoneDraft("Release", 1.0))))
        val milestone = repository.milestones.first().first()
        repository.toggleMilestone(milestone.id, true)
        assertTrue(repository.milestones.first().first { it.id == milestone.id }.completed)
        repository.setStatus(id, GoalStatus.Completed)
        assertEquals(GoalStatus.Completed, repository.goals.first().single().status)
    }

    @Test fun reorderNormalizesOmittedCompletedGoalsWithoutDuplicatePositions() = runBlocking {
        fun draft(name: String) = GoalDraft(
            name = name,
            type = GoalType.AccumulateTotal,
            targetMin = 1.0,
            startDate = FixedClock.today(),
        )
        val first = repository.create(draft("First"))
        val second = repository.create(draft("Second"))
        val completed = repository.create(draft("Completed"))
        repository.setStatus(completed, GoalStatus.Completed)

        repository.reorder(listOf(second, first))

        val stored = database.goalDao().getAllGoals()
        assertEquals(stored.size, stored.map { it.position }.distinct().size)
        assertEquals(listOf(second, first), stored.sortedBy { it.position }.take(2).map { it.id })
    }

    @Test fun goalTypeOwnsCalculationDirectionAndDeadlinePace() = runBlocking {
        repository.create(
            GoalDraft(
                name = "Distance",
                type = GoalType.AccumulateTotal,
                targetMin = 100.0,
                startDate = FixedClock.today(),
                aggregation = GoalAggregation.Latest,
                direction = GoalDirection.Decrease,
                paceType = GoalPaceType.Linear,
            ),
        )

        val saved = repository.goals.first().single()
        assertEquals(GoalAggregation.Sum, saved.aggregation)
        assertEquals(GoalDirection.Increase, saved.direction)
        assertEquals(GoalPaceType.None, saved.paceType)
    }

    @Test fun milestoneInsertReorderAndRemovalPreserveStableIdentity() = runBlocking {
        val today = FixedClock.today()
        val id = repository.create(
            GoalDraft(
                name = "Ship",
                type = GoalType.WeightedMilestones,
                startDate = today,
                milestones = listOf(
                    GoalMilestoneDraft("Build", 2.0),
                    GoalMilestoneDraft("Release", 1.0),
                ),
            ),
        )
        val original = repository.milestones.first()
        val build = original.single { it.name == "Build" }
        val release = original.single { it.name == "Release" }
        repository.toggleMilestone(release.id, true)

        repository.update(
            id,
            GoalDraft(
                name = "Ship",
                type = GoalType.WeightedMilestones,
                startDate = today,
                milestones = listOf(
                    GoalMilestoneDraft("Plan", 1.0),
                    GoalMilestoneDraft("Release", 1.0, id = release.id, uuid = release.uuid),
                    GoalMilestoneDraft("Build", 2.0, id = build.id, uuid = build.uuid),
                ),
            ),
        )

        val reordered = repository.milestones.first()
        assertEquals(listOf("Plan", "Release", "Build"), reordered.map { it.name })
        assertEquals(release.id, reordered.single { it.name == "Release" }.id)
        assertTrue(reordered.single { it.name == "Release" }.completed)
        assertEquals(build.id, reordered.single { it.name == "Build" }.id)

        repository.update(
            id,
            GoalDraft(
                name = "Ship",
                type = GoalType.WeightedMilestones,
                startDate = today,
                milestones = listOf(
                    GoalMilestoneDraft("Build", 2.0, id = build.id, uuid = build.uuid),
                ),
            ),
        )

        assertEquals(listOf(build.id), repository.milestones.first().map { it.id })
    }

    @Test fun numericGoalsRequireFiniteTargets() = runBlocking {
        val today = FixedClock.today()
        assertTrue(
            runCatching {
                repository.create(
                    GoalDraft(
                        name = "Missing target",
                        type = GoalType.ReachValue,
                        startDate = today,
                    ),
                )
            }.isFailure,
        )
        assertTrue(
            runCatching {
                repository.create(
                    GoalDraft(
                        name = "Invalid target",
                        type = GoalType.AccumulateTotal,
                        targetMin = Double.NaN,
                        startDate = today,
                    ),
                )
            }.isFailure,
        )
    }

    @Test fun aggregationAndConsistencyWindowsPersist() = runBlocking {
        repository.create(
            GoalDraft(
                name = "Train consistently",
                type = GoalType.Consistency,
                targetMin = 3.0,
                startDate = FixedClock.today(),
                aggregation = GoalAggregation.CompletionCount,
                aggregationPeriod = GoalAggregationPeriod.RollingDays,
                rollingDays = 28,
                consistencyPeriod = GoalConsistencyPeriod.Week,
                consistencyRequiredPeriods = 12,
            ),
        )

        val saved = repository.goals.first().single()
        assertEquals(GoalAggregationPeriod.RollingDays, saved.aggregationPeriod)
        assertEquals(28, saved.rollingDays)
        assertEquals(GoalConsistencyPeriod.Week, saved.consistencyPeriod)
        assertEquals(12, saved.consistencyRequiredPeriods)
    }

    @Test fun customUnitTargetsEntriesAndDisplayShareOneCanonicalScale() = runBlocking {
        val glassUnitId = measurements.createCustomUnit("glass", "gl", UnitDimension.Volume, 250.0)
        val id = repository.create(
            GoalDraft(
                name = "Hydrate",
                type = GoalType.ReachValue,
                dimension = UnitDimension.Volume,
                unitId = glassUnitId,
                targetMin = 8.0,
                startDate = FixedClock.today(),
            ),
        )
        repository.recordMeasurement(id, 2.0)

        val goal = repository.goals.first().single()
        val unit = measurements.customUnits.first().single()
        assertEquals(2_000.0, goal.targetMin ?: -1.0, 0.0)
        assertEquals(500.0, repository.metricEntries.first().single().canonicalValue ?: -1.0, 0.0)
        assertEquals(8.0, goal.displayValue(goal.targetMin, listOf(unit)) ?: -1.0, 0.0)
    }

    @Test fun elapsedGoalPersistsDisplayRejectsMeasurementsAndResetsExactStart() = runBlocking {
        val initial = FixedClock.now().minusSeconds(10 * 86_400)
        val id = repository.create(
            GoalDraft(
                name = "Recovery",
                type = GoalType.ElapsedSince,
                startDate = initial.atZone(ZoneId.of("UTC")).toLocalDate(),
                elapsedStartMillis = initial.toEpochMilli(),
                elapsedDisplayUnit = ElapsedDisplayUnit.Days,
            ),
        )
        val saved = repository.goals.first().single()
        assertEquals(initial.toEpochMilli(), saved.elapsedStartMillis)
        assertEquals(ElapsedDisplayUnit.Days, saved.elapsedDisplayUnit)
        assertTrue(runCatching { repository.recordMeasurement(id, 1.0) }.isFailure)

        val reset = FixedClock.now().minusSeconds(3_600)
        repository.resetElapsedStart(id, reset)
        assertEquals(reset.toEpochMilli(), repository.goals.first().single().elapsedStartMillis)
        assertTrue(runCatching { repository.resetElapsedStart(id, FixedClock.now().plusSeconds(1)) }.isFailure)
    }

    @Test fun changingGoalDisplayUnitDoesNotRelabelOrResaveHistoricalEntry() = runBlocking {
        val today = FixedClock.today()
        val id = repository.create(
            GoalDraft(
                name = "Weight",
                type = GoalType.ReachValue,
                dimension = UnitDimension.Mass,
                unitId = "kilogram",
                targetMin = 100.0,
                startDate = today,
            ),
        )
        val entryId = repository.recordMeasurement(id, 100.0)
        repository.update(
            id,
            GoalDraft(
                name = "Weight",
                type = GoalType.ReachValue,
                dimension = UnitDimension.Mass,
                unitId = "pound",
                targetMin = 220.0,
                startDate = today,
            ),
        )

        repository.updateMeasurement(id, entryId, 100.0, today, "unchanged")
        val entry = repository.metricEntries.first().single()
        assertEquals("kilogram", entry.enteredUnitId)
        assertEquals(100.0, entry.canonicalValue ?: -1.0, 0.0)
        assertEquals(220.0, repository.goals.first().single().displayValue(repository.goals.first().single().targetMin) ?: -1.0, 0.000001)
    }

    private object FixedClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-08-17T16:00:00Z")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 8, 17)
    }
    private class SequentialIds : WhipIdGenerator {
        private val count = AtomicInteger()
        override fun nextId() = "goal-test-${count.incrementAndGet()}"
    }
}
