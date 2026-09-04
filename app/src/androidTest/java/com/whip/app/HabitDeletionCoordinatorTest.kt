package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.CommittedHabitDeletionCancellation
import com.whip.app.data.DomainDeletionCoordinator
import com.whip.app.data.HabitDeletionConflictException
import com.whip.app.data.HabitDeletionConflictKind
import com.whip.app.data.HabitTimerSessionEntity
import com.whip.app.data.RoomHabitRepository
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.RoomRoutineRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.HabitChecklistItemDraft
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitTrackingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitDeletionCoordinatorTest {
    private lateinit var database: WhipDatabase
    private lateinit var habits: RoomHabitRepository
    private lateinit var coordinator: DomainDeletionCoordinator

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WhipDatabase::class.java,
        ).build()
        val ids = SequentialIds()
        val measurements = RoomMeasurementRepository(database, FixedClock, ids)
        habits = RoomHabitRepository(database, measurements, FixedClock, ids)
        coordinator = DomainDeletionCoordinator(database, RoomRoutineRepository(database, FixedClock, ids))
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun previewAndSummaryCoverTheCompleteCascadedHabitGraph() = runBlocking {
        val habitId = createCompleteGraph()
        val habit = requireNotNull(habits.get(habitId))

        val impact = requireNotNull(coordinator.previewHabitDeletion(habitId))

        assertEquals(habit.uuid, impact.habitUuid)
        assertEquals(1, impact.measurementEntryCount)
        assertEquals(2, impact.checklistItemCount)
        assertEquals(1, impact.checklistStateCount)
        assertEquals(1, impact.logCount)
        assertEquals(1, impact.skipCount)
        assertEquals(1, impact.pauseCount)
        assertEquals(2, impact.timerSessionCount)
        assertEquals(1, impact.activeTimerSessionCount)

        val summary = coordinator.deleteHabit(habitId, habit.uuid, impact.revisionToken)

        assertTrue(summary.habitDeleted)
        assertEquals(impact.measurementEntryCount, summary.measurementEntriesDeleted)
        assertEquals(impact.checklistItemCount, summary.checklistItemsDeleted)
        assertEquals(impact.checklistStateCount, summary.checklistStatesDeleted)
        assertEquals(impact.logCount, summary.logsDeleted)
        assertEquals(impact.skipCount, summary.skipsDeleted)
        assertEquals(impact.pauseCount, summary.pausesDeleted)
        assertEquals(impact.timerSessionCount, summary.timerSessionsDeleted)
        assertNull(database.habitDao().getHabit(habitId))
        assertNull(database.measurementDao().getMeasurement(habit.measurementId))
        listOf(
            "habit_checklist_items",
            "habit_checklist_states",
            "habit_logs",
            "habit_skips",
            "habit_pauses",
            "habit_timer_sessions",
            "measurement_entries",
        ).forEach { table -> assertEquals("Rows remain in $table", 0, rowCount(table)) }
    }

    @Test
    fun changedTimerHistoryRejectsTheStaleReviewWithoutDeletingAnything() = runBlocking {
        val habitId = habits.create(HabitDraft(name = "Reviewed Habit", startDate = FixedClock.today()))
        val habit = requireNotNull(habits.get(habitId))
        val preview = requireNotNull(coordinator.previewHabitDeletion(habitId))
        database.habitDao().insertTimerSession(timerSession("added-after-review", habitId, active = false))

        val error = runCatching {
            coordinator.deleteHabit(habitId, habit.uuid, preview.revisionToken)
        }.exceptionOrNull()

        assertTrue(error is HabitDeletionConflictException)
        assertEquals(HabitDeletionConflictKind.RevisionChanged, (error as HabitDeletionConflictException).kind)
        assertNotNull(database.habitDao().getHabit(habitId))
        assertNotNull(database.measurementDao().getMeasurement(habit.measurementId))
        assertEquals(1, rowCount("habit_timer_sessions"))
    }

    @Test
    fun missingAndMismatchedTargetsFailClosed() = runBlocking {
        val habitId = habits.create(HabitDraft(name = "Stable Habit", startDate = FixedClock.today()))
        val habit = requireNotNull(habits.get(habitId))
        val impact = requireNotNull(coordinator.previewHabitDeletion(habitId))

        val mismatch = runCatching {
            coordinator.deleteHabit(habitId, "another-habit-uuid", impact.revisionToken)
        }.exceptionOrNull() as HabitDeletionConflictException
        assertEquals(HabitDeletionConflictKind.IdentityChanged, mismatch.kind)
        assertNotNull(database.habitDao().getHabit(habitId))

        assertEquals(1, database.habitDao().deleteHabit(habitId))
        assertEquals(1, database.measurementDao().deleteMeasurement(habit.measurementId))
        val missing = runCatching {
            coordinator.deleteHabit(habitId, habit.uuid, impact.revisionToken)
        }.exceptionOrNull() as HabitDeletionConflictException
        assertEquals(HabitDeletionConflictKind.TargetMissing, missing.kind)
    }

    @Test
    fun postCommitReminderFailureReturnsACommittedWarningAndReconciliation() = runBlocking {
        val habitId = habits.create(HabitDraft(name = "Committed warning", startDate = FixedClock.today()))
        val habit = requireNotNull(habits.get(habitId))
        val reconciliations = AtomicInteger()
        val deletion = DomainDeletionCoordinator(
            database,
            RoomRoutineRepository(database, FixedClock, SequentialIds()),
            onDeletionCommitted = { _, _ -> error("cleanup unavailable") },
            onDeletionInterrupted = { reconciliations.incrementAndGet() },
        )
        val impact = requireNotNull(deletion.previewHabitDeletion(habitId))

        val summary = deletion.deleteHabit(habitId, habit.uuid, impact.revisionToken)

        assertTrue(summary.habitDeleted)
        assertTrue(summary.warnings.single().contains("permanent deletion was committed"))
        assertNull(database.habitDao().getHabit(habitId))
        assertEquals(1, reconciliations.get())
    }

    @Test
    fun cancellationAfterCommitCarriesTheSummaryAndCannotInviteReplay() = runBlocking {
        val habitId = habits.create(HabitDraft(name = "Committed cancellation", startDate = FixedClock.today()))
        val habit = requireNotNull(habits.get(habitId))
        val deletion = DomainDeletionCoordinator(
            database,
            RoomRoutineRepository(database, FixedClock, SequentialIds()),
            onDeletionCommitted = { _, _ -> throw CancellationException("cleanup cancelled") },
        )
        val impact = requireNotNull(deletion.previewHabitDeletion(habitId))

        val error = runCatching {
            deletion.deleteHabit(habitId, habit.uuid, impact.revisionToken)
        }.exceptionOrNull() as CommittedHabitDeletionCancellation

        assertTrue(error.summary.habitDeleted)
        assertNull(database.habitDao().getHabit(habitId))
    }

    private suspend fun createCompleteGraph(): Long {
        val habitId = habits.create(
            HabitDraft(
                name = "Complete graph",
                trackingMode = HabitTrackingMode.Checklist,
                autoCompleteFromItems = false,
                startDate = FixedClock.today(),
                checklistItems = listOf(
                    HabitChecklistItemDraft("First", 0),
                    HabitChecklistItemDraft("Second", 1),
                ),
            ),
        )
        val firstItem = habits.checklistItems.first().first()
        habits.toggleChecklistItem(habitId, firstItem.id, FixedClock.today(), true)
        habits.log(habitId, 1.0)
        habits.skipDay(habitId, FixedClock.today().plusDays(1))
        habits.addPause(habitId, FixedClock.today().plusDays(2), FixedClock.today().plusDays(3), "Away")
        database.habitDao().insertTimerSession(timerSession("active-timer", habitId, active = true))
        database.habitDao().insertTimerSession(timerSession("resolved-timer", habitId, active = false))
        return habitId
    }

    private fun timerSession(sessionId: String, habitId: Long, active: Boolean) = HabitTimerSessionEntity(
        sessionId = sessionId,
        habitId = habitId,
        activeHabitId = habitId.takeIf { active },
        state = if (active) "Running" else "Completed",
        anchorWallMillis = 1_000L,
        anchorElapsedRealtimeMillis = 2_000L,
        anchorBootId = "boot",
        accumulatedCanonicalSeconds = 30.0,
        unitId = "second",
        createdAtMillis = 3_000L,
        resolvedAtMillis = if (active) null else 4_000L,
    )

    private fun rowCount(table: String): Int = database.openHelper.readableDatabase
        .query("SELECT COUNT(*) FROM $table")
        .use { cursor -> cursor.moveToFirst(); cursor.getInt(0) }

    private object FixedClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-09-03T16:00:00Z")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 9, 3)
    }

    private class SequentialIds : WhipIdGenerator {
        private val count = AtomicInteger()
        override fun nextId(): String = "habit-delete-test-${count.incrementAndGet()}"
    }
}
