package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.core.RepPrescriptionScheme
import com.whip.app.core.SettingsRepository
import com.whip.app.data.RoomBackupRepository
import com.whip.app.data.RoomGoalRepository
import com.whip.app.data.RoomGymRepository
import com.whip.app.data.RoomHabitRepository
import com.whip.app.data.RoomLinkRepository
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.RoomRoutineRepository
import com.whip.app.data.RoomTaskRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.AreaScope
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalType
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.LinkRuleDraft
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.RecurrenceRule
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExerciseDraft
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskStepDraft
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.outcomeForPeriod
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    private lateinit var database: WhipDatabase
    private lateinit var habits: RoomHabitRepository
    private lateinit var tasks: RoomTaskRepository
    private lateinit var goals: RoomGoalRepository
    private lateinit var gym: RoomGymRepository
    private lateinit var routines: RoomRoutineRepository
    private lateinit var links: RoomLinkRepository
    private lateinit var measurements: RoomMeasurementRepository
    private lateinit var backups: RoomBackupRepository
    private lateinit var settings: FakeSettingsRepository

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WhipDatabase::class.java).build()
        val ids = SequentialIds()
        measurements = RoomMeasurementRepository(database, FixedClock, ids)
        tasks = RoomTaskRepository(database, FixedClock)
        habits = RoomHabitRepository(database, measurements, FixedClock, ids)
        goals = RoomGoalRepository(database, measurements, FixedClock, ids)
        gym = RoomGymRepository(database, FixedClock, ids)
        routines = RoomRoutineRepository(database, FixedClock, ids)
        links = RoomLinkRepository(database, measurements, FixedClock, ids)
        settings = FakeSettingsRepository(
            AppSettings(
                themeMode = AppThemeMode.Dark,
                timeZoneId = "America/Toronto",
                dayCutoffMinutes = 180,
                showAllUpcomingTaskOccurrences = true,
                repPrescriptionSchemes = listOf(
                    RepPrescriptionScheme(
                        id = "backup-hypertrophy",
                        name = "Hypertrophy",
                        setCount = 3,
                        repetitionsMin = 8,
                        repetitionsMax = 12,
                        restSeconds = 90,
                    ),
                ),
            ),
        )
        backups = RoomBackupRepository(database, settings)
    }
    @After fun tearDown() = database.close()

    @Test fun backupPreviewAndTransactionalRestoreRoundTrip() = runBlocking {
        val id = habits.create(HabitDraft(name = "Hydrate, safely", unitId = "glass", startDate = FixedClock.today()))
        habits.log(id, 2.0, note = "morning \"large\" glass")
        val json = backups.exportBackup()
        val preview = backups.previewBackup(json)
        assertEquals(2, preview.envelopeVersion)
        assertEquals(1, preview.databaseVersion)
        assertTrue(preview.checksumValid)
        assertTrue(preview.settingsIncluded)
        assertTrue(preview.totalRecords >= 3)
        val exportedTables = JSONObject(json).getJSONObject("tables")
        assertEquals(false, exportedTables.has("entity_tag_links"))
        assertEquals(false, exportedTables.has("goal_completion_snapshots"))

        backups.deleteAllData()
        assertTrue(habits.habits.first().isEmpty())
        assertEquals(AppThemeMode.System, settings.current().themeMode)
        assertEquals(false, settings.current().showAllUpcomingTaskOccurrences)
        backups.restoreBackup(json)
        assertEquals("Hydrate, safely", habits.habits.first().single().name)
        assertEquals(2.0, habits.logs.first().single().value!!, 0.0)
        assertEquals(AppThemeMode.Dark, settings.current().themeMode)
        assertEquals("America/Toronto", settings.current().timeZoneId)
        assertEquals(180, settings.current().dayCutoffMinutes)
        assertEquals(true, settings.current().showAllUpcomingTaskOccurrences)
        assertEquals("Hypertrophy", settings.current().repPrescriptionSchemes.single().name)
        assertEquals(8, settings.current().repPrescriptionSchemes.single().repetitionsMin)
        assertEquals(12, settings.current().repPrescriptionSchemes.single().repetitionsMax)
        assertEquals(90, settings.current().repPrescriptionSchemes.single().restSeconds)
        assertTrue(backups.exportHabitsCsv().contains("\"Hydrate, safely\""))
    }

    @Test fun areaIdentityAndScopeRoundTripAcrossEveryProductivityDomain() = runBlocking {
        val workId = measurements.ensureArea("Work")
        settings.update { it.copy(activeAreaScope = AreaScope.One(workId).storageKey) }
        tasks.create(TaskDraft(title = "Work task", areaId = workId, area = "Work"))
        habits.create(HabitDraft(name = "Work habit", areaId = workId, area = "Work", startDate = FixedClock.today()))
        goals.create(GoalDraft(name = "Work goal", areaId = workId, area = "Work", type = GoalType.OpenEndedTrend, startDate = FixedClock.today()))

        val backup = backups.exportBackup()
        backups.deleteAllData()
        backups.restoreBackup(backup)

        assertEquals(workId, tasks.tasks.first().single().areaId)
        assertEquals(workId, habits.habits.first().single().areaId)
        assertEquals(workId, goals.goals.first().single().areaId)
        assertEquals(AreaScope.One(workId).storageKey, settings.current().activeAreaScope)
        assertTrue(backups.exportTasksCsv().contains("\"Work\""))
        assertTrue(backups.exportHabitsCsv().contains("\"Work\""))
        assertTrue(backups.exportGoalsCsv().contains("\"Work\""))
    }

    @Test fun mergeReconcilesSameNameAreasAndRemapsAssignments() = runBlocking {
        val sourceAreaId = measurements.ensureArea("Work")
        tasks.create(TaskDraft(title = "Imported work", areaId = sourceAreaId, area = "Work"))
        val portable = backups.exportBackup()

        backups.deleteAllData()
        val localAreaId = measurements.ensureArea("work")
        backups.mergeBackup(portable)

        assertEquals(1, measurements.areas.first().size)
        assertEquals(localAreaId, tasks.tasks.first().single().areaId)
    }

    @Test fun mergeIsAtomicIdempotentRelationshipSafeAndKeepsCurrentSettings() = runBlocking {
        tasks.create(
            TaskDraft(
                title = "Imported task",
                steps = listOf(TaskStepDraft(title = "Imported step", position = 0)),
            ),
        )
        val importedHabitId = habits.create(HabitDraft(name = "Imported habit", startDate = FixedClock.today()))
        habits.log(importedHabitId, 1.0, note = "portable")
        val portable = backups.exportBackup()

        backups.deleteAllData()
        settings.update { it.copy(themeMode = AppThemeMode.Light, showHabitsInTaskPlanning = true) }
        tasks.create(TaskDraft(title = "Local task"))

        val first = backups.mergeBackup(portable)
        assertTrue(first.importedRecords > 0)
        assertEquals(setOf("Imported task", "Local task"), tasks.tasks.first().mapTo(mutableSetOf()) { it.title })
        val imported = tasks.tasks.first().first { it.title == "Imported task" }
        assertEquals("Imported step", tasks.getTask(imported.id)?.steps?.single()?.title)
        assertEquals("Imported habit", habits.habits.first().single().name)
        assertEquals("portable", habits.logs.first().single().note)
        assertEquals(AppThemeMode.Light, settings.current().themeMode)
        assertTrue(settings.current().showHabitsInTaskPlanning)

        val countsBeforeSecondMerge = listOf(tasks.tasks.first().size, tasks.steps.first().size, habits.habits.first().size, habits.logs.first().size)
        val second = backups.mergeBackup(portable)
        assertTrue(second.skippedExistingRecords > 0)
        assertEquals(countsBeforeSecondMerge, listOf(tasks.tasks.first().size, tasks.steps.first().size, habits.habits.first().size, habits.logs.first().size))
    }

    @Test fun fullBackupRoundTripsEveryFirstClassDomainAndCrossDomainRelationship() = runBlocking {
        val taskId = tasks.create(
            TaskDraft(
                title = "Prepare launch",
                scheduleKind = ScheduleKind.Recurring,
                date = FixedClock.today(),
                recurrence = RecurrenceRule(RecurrenceUnit.Days, 2, startDate = FixedClock.today()),
                steps = listOf(TaskStepDraft(title = "Run release tests", position = 0, notes = "Device too")),
                showSubtaskProgress = true,
            ),
        )
        tasks.completeOccurrence(taskId, FixedClock.today())

        val habitId = habits.create(
            HabitDraft(
                name = "Drink water",
                trackingMode = HabitTrackingMode.Count,
                unitId = "count",
                targetMin = 8.0,
                startDate = FixedClock.today(),
            ),
        )
        habits.log(habitId, 3.0, note = "Morning")

        val goalId = goals.create(
            GoalDraft(
                name = "Complete 100 healthy actions",
                type = GoalType.AccumulateTotal,
                dimension = UnitDimension.Count,
                unitId = "count",
                targetMin = 100.0,
                startDate = FixedClock.today(),
                aggregation = GoalAggregation.Sum,
            ),
        )
        goals.recordMeasurement(goalId, 4.0, note = "Manual baseline")
        links.createRule(
            LinkRuleDraft(
                name = "Water advances goal",
                sourceType = LinkSourceType.Habit,
                sourceEntityId = habitId,
                sourceMetric = LinkSourceMetric.NumericValue,
                targetGoalId = goalId,
                retroactiveFrom = FixedClock.today(),
            ),
            commitBackfill = true,
        )

        val categoryId = gym.createCategory("Chest")
        val exerciseId = gym.createExercise(
            ExerciseDraft(name = "Flat Barbell Bench Press", categoryIds = setOf(categoryId)),
        )
        val sessionId = gym.startWorkout("Push day")
        val workoutExerciseId = gym.addExerciseToWorkout(sessionId, exerciseId)
        gym.addSet(workoutExerciseId, WorkoutSetDraft(weight = 80.0, reps = 8, completed = true))
        gym.finishWorkout(sessionId)
        routines.createRoutine(
            RoutineDraft(
                name = "Push routine",
                days = listOf(
                    RoutineDayDraft(
                        "Day A",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = exerciseId,
                                plannedSets = listOf(WorkoutSetDraft(weight = 82.5, reps = 6, planned = true)),
                            ),
                        ),
                    ),
                ),
            ),
        )
        routines.rebuildPersonalRecords(exerciseId)
        routines.saveGraphPreset("Bench e1RM", listOf(exerciseId), "EstimatedOneRepMax", "All", "Workout")

        val json = backups.exportBackup()
        val preview = backups.previewBackup(json)
        assertTrue(preview.checksumValid)
        listOf(
            "tasks", "task_steps", "task_occurrences", "habits", "habit_logs", "goals",
            "metric_entries", "link_rules", "contributions", "exercises", "exercise_categories",
            "workout_sessions", "workout_exercises", "workout_sets", "gym_routines", "routine_days",
            "routine_exercises", "routine_sets", "personal_records", "graph_presets",
        ).forEach { table -> assertTrue("Expected $table in backup", preview.tableCounts.getValue(table) > 0) }

        backups.deleteAllData()
        backups.restoreBackup(json)

        assertEquals("Prepare launch", tasks.tasks.first().single().title)
        assertEquals("Run release tests", tasks.steps.first().single().title)
        assertEquals(1, tasks.occurrences.first().size)
        assertEquals("Drink water", habits.habits.first().single().name)
        assertTrue(habits.logs.first().any { it.note == "Morning" })
        assertEquals("Complete 100 healthy actions", goals.goals.first().single().name)
        assertEquals("Water advances goal", links.rules.first().single().name)
        assertEquals(1, links.contributions.first().size)
        assertEquals("Flat Barbell Bench Press", gym.exercises.first().single().name)
        assertEquals(1, gym.sets.first().size)
        assertEquals("Push routine", routines.routines.first().single().name)
        assertEquals(1, routines.sets.first().size)
        assertTrue(routines.personalRecords.first().isNotEmpty())
        assertEquals("Bench e1RM", routines.graphPresets.first().single().name)
    }

    @Test fun tamperedBackupIsRejectedBeforeItCanReplaceLiveData() = runBlocking {
        habits.create(HabitDraft(name = "Keep this", startDate = FixedClock.today()))
        val original = backups.exportBackup()
        val tampered = original.replace("Keep this", "Tampered")

        assertEquals(false, backups.previewBackup(tampered).checksumValid)
        assertTrue(runCatching { backups.restoreBackup(tampered) }.isFailure)
        assertEquals("Keep this", habits.habits.first().single().name)
    }

    @Test fun nonCurrentBackupVersionsOrTableSetsCannotBePreviewedOrRestored() = runBlocking {
        habits.create(HabitDraft(name = "Keep local", startDate = FixedClock.today()))
        val current = backups.exportBackup()
        val wrongDatabase = JSONObject(current).put("databaseVersion", 2).toString()
        val wrongEnvelope = JSONObject(current).put("envelopeVersion", 1).toString()
        val incompleteTables = JSONObject(current).also {
            it.getJSONObject("tables").remove("tags")
        }.toString()

        listOf(wrongDatabase, wrongEnvelope, incompleteTables).forEach { unsupported ->
            assertTrue(runCatching { backups.previewBackup(unsupported) }.isFailure)
            assertTrue(runCatching { backups.restoreBackup(unsupported) }.isFailure)
        }
        assertEquals("Keep local", habits.habits.first().single().name)
    }

    @Test fun sixOfEightActionsUndoLinksAndBackupRemainOneTruthfulPeriodOutcome() = runBlocking {
        val habitId = habits.create(
            HabitDraft(
                name = "Eight glasses",
                trackingMode = HabitTrackingMode.Count,
                unitId = "count",
                targetMin = 8.0,
                quickIncrement = 1.0,
                quickActions = listOf(2.0, 4.0),
                startDate = FixedClock.today(),
            ),
        )
        val goalId = goals.create(
            GoalDraft(
                name = "Hydration total",
                type = GoalType.AccumulateTotal,
                dimension = UnitDimension.Count,
                unitId = "count",
                targetMin = 100.0,
                startDate = FixedClock.today(),
                aggregation = GoalAggregation.Sum,
            ),
        )
        links.createRule(
            LinkRuleDraft(
                name = "Hydration link",
                sourceType = LinkSourceType.Habit,
                sourceEntityId = habitId,
                sourceMetric = LinkSourceMetric.NumericValue,
                targetGoalId = goalId,
                retroactiveFrom = FixedClock.today(),
            ),
            commitBackfill = true,
        )

        // +1, quick +2, decrement -1, and Set to 6 (stored as the +4 delta).
        listOf(1.0, 2.0, -1.0, 4.0).forEach { habits.log(habitId, it) }
        links.rebuildAll()
        val habit = habits.habits.first().single()
        assertEquals(false, habit.outcomeForPeriod(habits.logs.first(), FixedClock.today()))
        assertEquals(6.0, links.contributions.first().sumOf { it.canonicalValue ?: 0.0 }, 0.0)

        val completingLog = habits.log(habitId, 2.0)
        links.rebuildAll()
        assertEquals(true, habit.outcomeForPeriod(habits.logs.first(), FixedClock.today()))
        assertEquals(8.0, links.contributions.first().sumOf { it.canonicalValue ?: 0.0 }, 0.0)

        habits.undoLog(completingLog)
        links.rebuildAll()
        assertEquals(false, habit.outcomeForPeriod(habits.logs.first(), FixedClock.today()))
        assertEquals(6.0, links.contributions.first().sumOf { it.canonicalValue ?: 0.0 }, 0.0)
        habits.log(habitId, 2.0)
        links.rebuildAll()

        val archive = backups.exportBackup()
        backups.deleteAllData()
        backups.restoreBackup(archive)
        links.rebuildAll()

        val restoredHabit = habits.habits.first().single()
        assertEquals(true, restoredHabit.outcomeForPeriod(habits.logs.first(), FixedClock.today()))
        assertEquals(8.0, links.contributions.first().sumOf { it.canonicalValue ?: 0.0 }, 0.0)
    }

    private object FixedClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-08-17T16:00:00Z")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 8, 17)
    }
    private class SequentialIds : WhipIdGenerator {
        private val count = AtomicInteger()
        override fun nextId() = "backup-test-${count.incrementAndGet()}"
    }
    private class FakeSettingsRepository(initial: AppSettings) : SettingsRepository {
        private val state = MutableStateFlow(initial)
        override val settings: Flow<AppSettings> = state
        override fun current(): AppSettings = state.value
        override fun update(transform: (AppSettings) -> AppSettings) { state.value = transform(state.value) }
    }
}
