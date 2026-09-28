package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.*
import com.whip.app.domain.*
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductivityDuplicateRepositoryTest {
    @Test fun maximumNamesDuplicateIndependentStructuresWithoutCopyingHistory() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WhipDatabase::class.java)
            .addCallback(WhipDatabase.integrityGuardCallback).build()
        try {
            val clock = object : WhipClock {
                override fun now() = Instant.parse("2026-09-28T12:00:00Z")
                override fun zoneId() = ZoneId.of("UTC")
            }
            var nextId = 0
            val ids = WhipIdGenerator { "duplicate-audit-${++nextId}" }
            RoomAreaRepository(database, clock, ids).ensureDefaultArea()
            val measurements = RoomMeasurementRepository(database, clock, ids)
            val tasks = RoomTaskRepository(database, clock)
            val habits = RoomHabitRepository(database, measurements, clock, ids)
            val goals = RoomGoalRepository(database, measurements, clock, ids)
            val tracks = RoomTrackRepository(database, clock, ids)

            val taskId = tasks.create(TaskDraft("🌱".repeat(200), steps = listOf(TaskStepDraft(title = "Step", position = 0))))
            tasks.completeOccurrence(taskId, null)
            val originalTask = requireNotNull(tasks.getTask(taskId))
            val copiedTask = requireNotNull(tasks.getTask(tasks.duplicate(taskId)))
            assertEquals(200, copiedTask.title.codePointCount(0, copiedTask.title.length))
            assertTrue(copiedTask.title.endsWith(" copy"))
            assertNotEquals(originalTask.id, copiedTask.id)
            assertNull(copiedTask.completedAtMillis)
            assertNotEquals(originalTask.steps.single().id, copiedTask.steps.single().id)
            assertEquals(originalTask, tasks.getTask(taskId))

            val habitId = habits.create(HabitDraft("H".repeat(100), trackingMode = HabitTrackingMode.Checklist,
                startDate = clock.today(), checklistItems = listOf(HabitChecklistItemDraft("Item", 0))))
            habits.log(habitId, 1.0, HabitLogStatus.Success)
            val originalHabit = requireNotNull(habits.get(habitId))
            val originalLogs = habits.logs.first()
            val copiedHabit = requireNotNull(habits.get(habits.duplicate(habitId)))
            assertEquals(100, copiedHabit.name.length)
            assertTrue(copiedHabit.name.endsWith(" copy"))
            assertNotEquals(originalHabit.measurementId, copiedHabit.measurementId)
            val checklist = habits.checklistItems.first()
            assertNotEquals(checklist.single { it.habitId == habitId }.uuid, checklist.single { it.habitId == copiedHabit.id }.uuid)
            assertEquals(originalLogs, habits.logs.first())
            assertEquals(originalHabit, habits.get(habitId))

            val goalId = goals.create(GoalDraft("G".repeat(100), type = GoalType.WeightedMilestones,
                startDate = clock.today(), milestones = listOf(GoalMilestoneDraft("Finish", 2.0))))
            goals.toggleMilestone(goals.milestones.first().single().id, true)
            val originalGoal = requireNotNull(goals.get(goalId))
            val copiedGoal = requireNotNull(goals.get(goals.duplicate(goalId)))
            assertEquals(100, copiedGoal.name.length)
            assertTrue(copiedGoal.name.endsWith(" copy"))
            val milestones = goals.milestones.first()
            assertNotEquals(milestones.single { it.goalId == goalId }.uuid, milestones.single { it.goalId == copiedGoal.id }.uuid)
            assertNull(milestones.single { it.goalId == copiedGoal.id }.completedAtMillis)
            assertEquals(originalGoal, goals.get(goalId))

            val trackId = tracks.create(TrackDraft("T".repeat(100), fields = listOf(
                TrackFieldDraft("Title", TrackFieldType.ShortText, primary = true),
                TrackFieldDraft("State", TrackFieldType.SingleChoice, options = listOf(TrackChoiceOptionDraft("Saved"))),
            )))
            val emptyTrack = requireNotNull(tracks.projection(trackId))
            tracks.addEntry(trackId, TrackEntryDraft(clock.today(), mapOf(emptyTrack.primaryField.uuid to TrackValueDraft(textValue = "Original"))))
            val originalTrack = requireNotNull(tracks.projection(trackId))
            val copiedTrack = requireNotNull(tracks.projection(tracks.duplicate(trackId)))
            assertEquals(100, copiedTrack.track.name.length)
            assertTrue(copiedTrack.track.name.endsWith(" Copy"))
            assertTrue(copiedTrack.entries.isEmpty())
            assertTrue(originalTrack.fields.map { it.uuid }.toSet().intersect(copiedTrack.fields.map { it.uuid }.toSet()).isEmpty())
            assertNotEquals(originalTrack.options.single().uuid, copiedTrack.options.single().uuid)
            assertEquals(originalTrack, tracks.projection(trackId))
            assertEquals(100, requireNotNull(tracks.projection(tracks.duplicate(copiedTrack.track.id))).track.name.length)
        } finally { database.close() }
    }
}
