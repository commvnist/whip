package com.whip.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.RoomGoalRepository
import com.whip.app.data.RoomGymRepository
import com.whip.app.data.RoomHabitRepository
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.RoomTaskRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalEntryMode
import com.whip.app.domain.GoalType
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskStepDraft
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WorkoutSetDraft
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
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
class PersistentStorageE2ETest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var databaseName: String
    private var openDatabase: WhipDatabase? = null

    @Before
    fun setUp() {
        databaseName = "whip-persistence-test-${UUID.randomUUID()}.db"
    }

    @After
    fun tearDown() {
        openDatabase?.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun coreRecordsRemainAfterDatabaseIsClosedAndReopened() = runBlocking {
        val firstDatabase = open()
        val first = Repositories(firstDatabase, SequentialIds())
        val taskId = first.tasks.create(
            TaskDraft(
                title = "Persist me",
                scheduleKind = ScheduleKind.Once,
                date = FixedClock.today(),
                steps = listOf(TaskStepDraft(title = "Stored subtask", position = 0)),
                showSubtaskProgress = true,
            ),
        )
        first.tasks.setPinned(taskId, true)
        val habitId = first.habits.create(
            HabitDraft(
                name = "Persistent habit",
                trackingMode = HabitTrackingMode.Count,
                targetMin = 8.0,
                startDate = FixedClock.today(),
            ),
        )
        first.habits.log(habitId, 6.0, note = "Survives restart")
        val goalId = first.goals.create(
            GoalDraft(
                name = "Persistent goal",
                type = GoalType.AccumulateTotal,
                dimension = UnitDimension.Count,
                unitId = "count",
                targetMin = 100.0,
                startDate = FixedClock.today(),
                aggregation = GoalAggregation.Sum,
                entryMode = GoalEntryMode.AmountToAdd,
            ),
        )
        first.goals.recordMeasurement(goalId, 12.0, note = "Local database")
        val exerciseId = first.gym.createExercise(ExerciseDraft(name = "Persistent press"))
        val sessionId = first.gym.startWorkout("Stored workout")
        val workoutExerciseId = first.gym.addExerciseToWorkout(sessionId, exerciseId)
        first.gym.addSet(workoutExerciseId, WorkoutSetDraft(weight = 100.0, reps = 5, completed = true))
        first.gym.finishWorkout(sessionId)

        firstDatabase.close()
        openDatabase = null

        val reopenedDatabase = open()
        val reopened = Repositories(reopenedDatabase, SequentialIds())
        assertEquals("Persist me", reopened.tasks.tasks.first().single().title)
        assertTrue(reopened.tasks.tasks.first().single().pinned)
        assertEquals("Stored subtask", reopened.tasks.steps.first().single().title)
        assertEquals("Persistent habit", reopened.habits.habits.first().single().name)
        assertEquals(6.0, reopened.habits.logs.first().single().value!!, 0.0)
        val reopenedGoal = reopened.goals.goals.first().single()
        assertEquals("Persistent goal", reopenedGoal.name)
        assertEquals(
            12.0,
            reopened.goals.metricEntries.first().single { it.metricId == reopenedGoal.metricId }.canonicalValue!!,
            0.0,
        )
        assertEquals("Persistent press", reopened.gym.exercises.first().single().name)
        assertEquals("Stored workout", reopened.gym.sessions.first().single().name)
        assertEquals(100.0, reopened.gym.sets.first().single().canonicalWeightKg!!, 0.0)
    }

    private fun open(): WhipDatabase = Room.databaseBuilder(context, WhipDatabase::class.java, databaseName)
        .build()
        .also { openDatabase = it }

    private class Repositories(database: WhipDatabase, ids: WhipIdGenerator) {
        private val measurements = RoomMeasurementRepository(database, FixedClock, ids)
        val tasks = RoomTaskRepository(database, FixedClock)
        val habits = RoomHabitRepository(database, measurements, FixedClock, ids)
        val goals = RoomGoalRepository(database, measurements, FixedClock, ids)
        val gym = RoomGymRepository(database, FixedClock, ids)
    }

    private object FixedClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-08-18T16:00:00Z")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 8, 18)
    }

    private class SequentialIds : WhipIdGenerator {
        private val count = AtomicInteger()
        override fun nextId(): String = "persistence-${count.incrementAndGet()}"
    }
}
