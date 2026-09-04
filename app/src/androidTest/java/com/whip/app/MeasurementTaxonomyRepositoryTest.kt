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
import com.whip.app.data.RoomTrackRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalType
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.MeasurementValueKind
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackValueDraft
import com.whip.app.domain.UnitDimension
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
    private lateinit var tracks: RoomTrackRepository

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
        tracks = RoomTrackRepository(database, FixedClock, ids)
    }

    @After fun tearDown() = database.close()

    @Test
    fun areaAndTagMergeUpdateEveryDomainWithTargetSpellingAndNoDuplicates() = runBlocking {
        val personalId = measurements.ensureArea("Personal")
        val workId = measurements.ensureArea("Work")
        val oldTagId = measurements.ensureTag("Next")
        val focusTagId = measurements.ensureTag("Focus")
        tasks.create(TaskDraft(title = "Task", area = "personal", tags = setOf("Next", "Focus")))
        habits.create(HabitDraft(name = "Habit", area = "Personal", tags = listOf("next"), startDate = FixedClock.today()))
        goals.create(GoalDraft(name = "Goal", type = GoalType.OpenEndedTrend, area = "Personal", tags = listOf("Next"), startDate = FixedClock.today()))
        val trackId = tracks.create(
            TrackDraft(
                name = "Track",
                areaId = personalId,
                tags = listOf("Next"),
                fields = listOf(TrackFieldDraft("Value", TrackFieldType.ShortText, primary = true)),
            ),
        )

        val renameFailure = runCatching { measurements.renameArea(personalId, "work") }
        assertTrue(renameFailure.isFailure)
        areas.merge(personalId, workId)
        val tagRenameFailure = runCatching { measurements.renameTag(oldTagId, "focus") }
        assertTrue(tagRenameFailure.isFailure)
        measurements.mergeTags(oldTagId, focusTagId)

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
        assertEquals(listOf("Focus"), tracks.tracks.first().single { it.id == trackId }.tags)
    }

    @Test
    fun taxonomyRenamesAdvanceRevisionsAndRebuildTrackSearch() = runBlocking {
        val areaId = measurements.ensureArea("Alpha Area")
        val tagId = measurements.ensureTag("Alpha Tag")
        val taskId = tasks.create(TaskDraft(title = "Task", areaId = areaId, tags = setOf("Alpha Tag")))
        val habitId = habits.create(
            HabitDraft(name = "Habit", areaId = areaId, tags = listOf("Alpha Tag"), startDate = FixedClock.today()),
        )
        val goalId = goals.create(
            GoalDraft(
                name = "Goal",
                areaId = areaId,
                tags = listOf("Alpha Tag"),
                type = GoalType.OpenEndedTrend,
                startDate = FixedClock.today(),
            ),
        )
        val trackId = tracks.create(
            TrackDraft(
                name = "Journal",
                areaId = areaId,
                tags = listOf("Alpha Tag"),
                fields = listOf(TrackFieldDraft("Note", TrackFieldType.ShortText, primary = true)),
            ),
        )
        val projection = requireNotNull(tracks.projection(trackId))
        val entryId = tracks.addEntry(
            trackId,
            TrackEntryDraft(
                entryDate = FixedClock.today(),
                values = mapOf(projection.primaryField.uuid to TrackValueDraft(textValue = "Entry")),
            ),
        )
        database.taskDao().getTask(taskId)?.let { database.taskDao().updateTask(it.copy(updatedAtMillis = 1L)) }
        database.habitDao().getHabit(habitId)?.let { database.habitDao().updateHabit(it.copy(updatedAtMillis = 1L)) }
        database.goalDao().getGoal(goalId)?.let { database.goalDao().updateGoal(it.copy(updatedAtMillis = 1L)) }
        database.trackDao().getTrack(trackId)?.let { database.trackDao().updateTrack(it.copy(updatedAtMillis = 1L)) }

        measurements.renameArea(areaId, "Beta Zone")
        measurements.renameTag(tagId, "Beta Tag")

        assertEquals(setOf(entryId), tracks.searchEntryIds(trackId, "Beta Zone"))
        assertEquals(setOf(entryId), tracks.searchEntryIds(trackId, "Beta Tag"))
        assertTrue(tracks.searchEntryIds(trackId, "Alpha Area").isEmpty())
        assertTrue(tracks.searchEntryIds(trackId, "Alpha Tag").isEmpty())
        assertEquals(FixedClock.now().toEpochMilli(), requireNotNull(database.taskDao().getTask(taskId)).updatedAtMillis)
        assertEquals(FixedClock.now().toEpochMilli(), requireNotNull(database.habitDao().getHabit(habitId)).updatedAtMillis)
        assertEquals(FixedClock.now().toEpochMilli(), requireNotNull(database.goalDao().getGoal(goalId)).updatedAtMillis)
        assertEquals(FixedClock.now().toEpochMilli(), requireNotNull(database.trackDao().getTrack(trackId)).updatedAtMillis)
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

    @Test
    fun permanentAreaDeleteMovesEveryAssignedItemToAnotherArea() = runBlocking {
        val mainId = areas.ensureDefaultArea()
        val areaId = areas.create("Client Delta")
        tasks.create(TaskDraft(title = "Task", areaId = areaId, area = "Client Delta"))
        habits.create(HabitDraft(name = "Habit", areaId = areaId, area = "Client Delta", startDate = FixedClock.today()))
        goals.create(
            GoalDraft(
                name = "Goal",
                areaId = areaId,
                area = "Client Delta",
                type = GoalType.OpenEndedTrend,
                startDate = FixedClock.today(),
            ),
        )

        areas.deletePermanently(areaId, mainId)

        assertEquals(listOf("Main"), areas.areas.first().map { it.name })
        assertEquals(1, tasks.tasks.first().size)
        assertEquals(mainId, tasks.tasks.first().single().areaId)
        assertEquals("Main", tasks.tasks.first().single().area)
        assertEquals(1, habits.habits.first().size)
        assertEquals(mainId, habits.habits.first().single().areaId)
        assertEquals("Main", habits.habits.first().single().area)
        assertEquals(1, goals.goals.first().size)
        assertEquals(mainId, goals.goals.first().single().areaId)
        assertEquals("Main", goals.goals.first().single().area)
    }

    @Test
    fun blankAssignmentsDefaultToMainAndCanMoveBetweenAreas() = runBlocking {
        val mainId = areas.ensureDefaultArea()
        val personalId = areas.create("Personal")
        tasks.create(TaskDraft(title = "Task"))
        habits.create(HabitDraft(name = "Habit", startDate = FixedClock.today()))
        goals.create(GoalDraft(name = "Goal", type = GoalType.OpenEndedTrend, startDate = FixedClock.today()))

        assertEquals(mainId, tasks.tasks.first().single().areaId)
        assertEquals(mainId, habits.habits.first().single().areaId)
        assertEquals(mainId, goals.goals.first().single().areaId)

        database.openHelper.writableDatabase.execSQL("UPDATE tasks SET areaId = NULL, area = ''")
        database.openHelper.writableDatabase.execSQL("UPDATE habits SET areaId = NULL, area = ''")
        database.openHelper.writableDatabase.execSQL("UPDATE goals SET areaId = NULL, area = ''")
        areas.ensureDefaultArea()

        assertEquals(mainId, tasks.tasks.first().single().areaId)
        assertEquals(mainId, habits.habits.first().single().areaId)
        assertEquals(mainId, goals.goals.first().single().areaId)

        areas.moveAssignments(sourceId = mainId, targetId = personalId)

        assertEquals(personalId, tasks.tasks.first().single().areaId)
        assertEquals("Personal", tasks.tasks.first().single().area)
        assertEquals(personalId, habits.habits.first().single().areaId)
        assertEquals("Personal", habits.habits.first().single().area)
        assertEquals(personalId, goals.goals.first().single().areaId)
        assertEquals("Personal", goals.goals.first().single().area)
        assertEquals(listOf("Main", "Personal"), areas.areas.first().map { it.name })

        areas.moveAssignments(sourceId = personalId, targetId = mainId)

        assertEquals(mainId, tasks.tasks.first().single().areaId)
        assertEquals("Main", tasks.tasks.first().single().area)
        assertEquals(mainId, habits.habits.first().single().areaId)
        assertEquals("Main", habits.habits.first().single().area)
        assertEquals(mainId, goals.goals.first().single().areaId)
        assertEquals("Main", goals.goals.first().single().area)
        assertEquals(listOf("Main", "Personal"), areas.areas.first().map { it.name })
    }

    @Test
    fun lastActiveAreaCannotBeArchivedOrDeleted() = runBlocking {
        val mainId = areas.ensureDefaultArea()

        val archiveFailure = runCatching { areas.setArchived(mainId, true) }.exceptionOrNull()
        val deleteFailure = runCatching { areas.deletePermanently(mainId) }.exceptionOrNull()

        assertTrue(archiveFailure?.message?.contains("Create another Area") == true)
        assertTrue(deleteFailure?.message?.contains("Create another Area") == true)
        assertEquals(listOf("Main"), areas.areas.first().filterNot { it.archived }.map { it.name })
    }

    @Test
    fun renameConflictNamesAnArchivedDestinationThatMustBeRestored() = runBlocking {
        val mainId = areas.ensureDefaultArea()
        val archivedId = areas.create("Archived Project")
        areas.setArchived(archivedId, true)

        val failure = runCatching { areas.rename(mainId, "Archived Project") }.exceptionOrNull()

        assertTrue(failure?.message?.contains("archived Area") == true)
        assertTrue(failure?.message?.contains("Restore it before merging") == true)
        assertEquals("Main", areas.areas.first().single { it.id == mainId }.name)
    }

    @Test
    fun creatingAnArchivedAreaNameRestoresTheSameIdentityAndPreservesItsColor() = runBlocking {
        areas.ensureDefaultArea()
        val originalColor = 0xFF1565C0
        val archivedId = areas.create("Archived Project", originalColor)
        areas.setArchived(archivedId, true)

        val restoredId = areas.create("  archived project  ", 0xFFFF0000)
        val restored = areas.areas.first().single { it.id == archivedId }

        assertEquals(archivedId, restoredId)
        assertFalse(restored.archived)
        assertEquals(originalColor, restored.colorArgb)
        assertEquals(2, areas.areas.first().size)
    }

    @Test
    fun ensuringAnArchivedTagReusesItsIdentityWithoutSilentlyRestoringIt() = runBlocking {
        val tagId = measurements.ensureTag("Recovery")
        measurements.setTagArchived(tagId, true)

        val reusedId = measurements.ensureTag("  recovery  ")
        val archived = measurements.tags.first().single()

        assertEquals(tagId, reusedId)
        assertTrue(archived.archived)
        assertEquals("Recovery", archived.name)
    }

    @Test
    fun explicitCreateOrRestoreRestoresTheSameTagIdentity() = runBlocking {
        val tagId = measurements.ensureTag("Recovery")
        measurements.setTagArchived(tagId, true)

        val restoredId = measurements.createOrRestoreTag("  recovery  ")
        val restored = measurements.tags.first().single()

        assertEquals(tagId, restoredId)
        assertFalse(restored.archived)
        assertEquals("Recovery", restored.name)
    }

    @Test
    fun tagRenameConflictRequiresAnExplicitMergeAndPreservesBothTags() = runBlocking {
        val firstId = measurements.ensureTag("First")
        val secondId = measurements.ensureTag("Second")

        val failure = runCatching { measurements.renameTag(firstId, "second") }.exceptionOrNull()

        assertTrue(failure?.message?.contains("Use Merge instead") == true)
        assertEquals(setOf(firstId, secondId), measurements.tags.first().mapTo(mutableSetOf()) { it.id })
    }

    @Test
    fun tagNamesRejectTheCommaReservedByCurrentPersistence() = runBlocking {
        val firstId = measurements.ensureTag("First")

        val createFailure = runCatching { measurements.ensureTag("Two, Tags") }.exceptionOrNull()
        val renameFailure = runCatching { measurements.renameTag(firstId, "First, Second") }.exceptionOrNull()

        assertTrue(createFailure?.message?.contains("separate Tags") == true)
        assertTrue(renameFailure?.message?.contains("separate Tags") == true)
        assertEquals(listOf("First"), measurements.tags.first().map { it.name })
    }

    @Test
    fun tagMergeRejectsAnArchivedDestinationUntilItIsRestored() = runBlocking {
        val sourceId = measurements.ensureTag("Source")
        val targetId = measurements.ensureTag("Target")
        measurements.setTagArchived(targetId, true)

        val failure = runCatching { measurements.mergeTags(sourceId, targetId) }.exceptionOrNull()

        assertTrue(failure?.message?.contains("active destination Tag") == true)
        assertEquals(setOf(sourceId, targetId), measurements.tags.first().mapTo(mutableSetOf()) { it.id })
        measurements.setTagArchived(targetId, false)
        measurements.mergeTags(sourceId, targetId)
        assertEquals(listOf(targetId), measurements.tags.first().map { it.id })
    }

    @Test
    fun measurementRecordsUseTheRepositoryClockZoneUnlessAnExplicitProvenanceZoneIsSupplied() = runBlocking {
        val measurementId = measurements.createMeasurement(
            name = "Timezone provenance",
            valueKind = MeasurementValueKind.Decimal,
            dimension = UnitDimension.Count,
            defaultUnitId = "count",
        )
        val timestamp = Instant.parse("2026-09-01T02:00:00Z")

        val defaultId = measurements.record(measurementId, 1.0, "count", timestamp = timestamp)
        val explicitId = measurements.record(
            measurementId,
            2.0,
            "count",
            timestamp = timestamp,
            localDate = LocalDate.of(2026, 8, 31),
            zoneId = ZoneId.of("America/Toronto"),
        )
        val entries = measurements.entries.first().associateBy { it.id }

        assertEquals("UTC", entries.getValue(defaultId).zoneId)
        assertEquals(LocalDate.of(2026, 9, 1), entries.getValue(defaultId).localDate)
        assertEquals("America/Toronto", entries.getValue(explicitId).zoneId)
        assertEquals(LocalDate.of(2026, 8, 31), entries.getValue(explicitId).localDate)
        assertEquals(-4 * 60 * 60, entries.getValue(explicitId).offsetSeconds)
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
