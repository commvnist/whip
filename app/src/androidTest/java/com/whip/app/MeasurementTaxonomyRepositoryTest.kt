package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.RoomGoalRepository
import com.whip.app.data.RoomAreaRepository
import com.whip.app.data.RoomHabitRepository
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.RoomTaskRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalType
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.TaskDraft
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
class MeasurementTaxonomyRepositoryTest {
    private lateinit var database: WhipDatabase
    private lateinit var measurements: RoomMeasurementRepository
    private lateinit var tasks: RoomTaskRepository
    private lateinit var habits: RoomHabitRepository
    private lateinit var goals: RoomGoalRepository
    private lateinit var areas: RoomAreaRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WhipDatabase::class.java,
        ).build()
        val ids = SequentialIds()
        measurements = RoomMeasurementRepository(database, FixedClock, ids)
        areas = RoomAreaRepository(database, FixedClock, ids)
        tasks = RoomTaskRepository(database, FixedClock)
        habits = RoomHabitRepository(database, measurements, FixedClock, ids)
        goals = RoomGoalRepository(database, measurements, FixedClock, ids)
    }

    @After fun tearDown() = database.close()

    @Test
    fun areaAndTagMergeUpdateEveryDomainWithTargetSpellingAndNoDuplicates() = runBlocking {
        val personalId = measurements.ensureArea("Personal")
        val workId = measurements.ensureArea("Work")
        val oldTagId = measurements.ensureTag("Next")
        measurements.ensureTag("Focus")
        tasks.create(TaskDraft(title = "Task", area = "personal", tags = setOf("Next", "Focus")))
        habits.create(HabitDraft(name = "Habit", area = "Personal", tags = listOf("next"), startDate = FixedClock.today()))
        goals.create(GoalDraft(name = "Goal", type = GoalType.OpenEndedTrend, area = "Personal", tags = listOf("Next"), startDate = FixedClock.today()))

        val renameFailure = runCatching { measurements.renameArea(personalId, "work") }
        assertTrue(renameFailure.isFailure)
        areas.merge(personalId, workId)
        measurements.renameTag(oldTagId, "focus")

        assertEquals(listOf("Work"), measurements.areas.first().map { it.name })
        assertEquals(listOf("Focus"), measurements.tags.first().map { it.name })
        assertEquals("Work", tasks.tasks.first().single().area)
        assertEquals(workId, tasks.tasks.first().single().areaId)
        assertEquals(setOf("Focus"), tasks.tasks.first().single().tags)
        assertEquals("Work", habits.habits.first().single().area)
        assertEquals(workId, habits.habits.first().single().areaId)
        assertEquals(listOf("Focus"), habits.habits.first().single().tags)
        assertEquals("Work", goals.goals.first().single().area)
        assertEquals(workId, goals.goals.first().single().areaId)
        assertEquals(listOf("Focus"), goals.goals.first().single().tags)
    }

    @Test
    fun archiveRestoreAndMoveAffectTaxonomyWithoutChangingRecords() = runBlocking {
        val first = measurements.ensureArea("First")
        val second = measurements.ensureArea("Second")
        val tag = measurements.ensureTag("Someday")
        tasks.create(TaskDraft(title = "Keep", area = "First", tags = setOf("Someday")))

        measurements.moveArea(second, -1)
        assertEquals(listOf("Second", "First"), measurements.areas.first().map { it.name })
        measurements.setAreaArchived(first, true)
        measurements.setTagArchived(tag, true)
        assertTrue(measurements.areas.first().single { it.id == first }.archived)
        assertTrue(measurements.tags.first().single { it.id == tag }.archived)
        assertEquals("First", tasks.tasks.first().single().area)
        assertEquals(first, tasks.tasks.first().single().areaId)
        assertEquals(setOf("Someday"), tasks.tasks.first().single().tags)

        measurements.setAreaArchived(first, false)
        measurements.setTagArchived(tag, false)
        assertFalse(measurements.areas.first().single { it.id == first }.archived)
        assertFalse(measurements.tags.first().single { it.id == tag }.archived)
    }

    private object FixedClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-08-19T12:00:00Z")
        override fun zoneId(): ZoneId = ZoneId.of("UTC")
    }

    private class SequentialIds : WhipIdGenerator {
        private val counter = AtomicInteger()
        override fun nextId(): String = "taxonomy-${counter.incrementAndGet()}"
    }
}
