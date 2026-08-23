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
import com.whip.app.domain.GoalEntryMode
import com.whip.app.domain.GoalType
import com.whip.app.domain.GymMachineDraft
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.LinkRuleDraft
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.MachineLoadType
import com.whip.app.domain.MachineStackMode
import com.whip.app.domain.RecurrenceRule
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExerciseDraft
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.TaskStepDraft
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.outcomeForPeriod
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.security.MessageDigest
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
        assertTrue(preview.checksumValid)
        assertTrue(preview.settingsIncluded)
        assertTrue(preview.totalRecords >= 3)

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

    @Test fun legacyTaskMergeIsIdempotentWithoutCollidingAcrossBackupContents() = runBlocking {
        tasks.create(TaskDraft(title = "Legacy portable task"))
        val firstLegacy = legacyTaskFixture(backups.exportBackup(), 25)
        backups.deleteAllData()

        backups.mergeBackup(firstLegacy)
        backups.mergeBackup(firstLegacy)
        assertEquals(listOf("Legacy portable task"), tasks.tasks.first().map { it.title })

        val secondRoot = JSONObject(firstLegacy)
        secondRoot.getJSONObject("tables").getJSONArray("tasks").getJSONObject(0)
            .put("title", "Different device task")
        val secondTables = secondRoot.getJSONObject("tables")
        val secondPayload = secondTables.toString() + "\n" + secondRoot.optJSONObject("settings")?.toString().orEmpty()
        secondRoot.put(
            "checksumSha256",
            MessageDigest.getInstance("SHA-256").digest(secondPayload.toByteArray()).joinToString("") { "%02x".format(it) },
        )

        backups.mergeBackup(secondRoot.toString())
        assertEquals(
            setOf("Legacy portable task", "Different device task"),
            tasks.tasks.first().mapTo(mutableSetOf()) { it.title },
        )
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
                entryMode = GoalEntryMode.AmountToAdd,
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

    @Test fun newerBackupCanBeInspectedButCannotReplaceLiveData() = runBlocking {
        habits.create(HabitDraft(name = "Keep local", startDate = FixedClock.today()))
        val future = JSONObject(backups.exportBackup()).put("databaseVersion", 999).toString()

        val preview = backups.previewBackup(future)
        assertEquals(false, preview.restoreCompatible)
        assertTrue(preview.compatibilityMessage.orEmpty().contains("newer Whip data format"))
        assertTrue(runCatching { backups.restoreBackup(future) }.isFailure)
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
                entryMode = GoalEntryMode.AmountToAdd,
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

    @Test fun legacyVersionOneTaskRowsReceiveEveryAdditiveDefault() = runBlocking {
        tasks.create(TaskDraft(title = "Legacy task"))
        val root = JSONObject(backups.exportBackup())
        val task = root.getJSONObject("tables").getJSONArray("tasks").getJSONObject(0)
        listOf(
            "updatedAtMillis", "showSubtaskProgress", "progressDisplay", "autoCompleteFromSteps",
            "repeatStepPolicy", "pinned", "priority", "area", "tagsCsv", "deadlineEpochDay",
            "recurrenceAnchor", "reminderOffsetsMinutesCsv", "locationReminderEnabled", "locationName",
            "locationLatitude", "locationLongitude", "locationRadiusMeters", "locationTrigger",
            "missedOccurrencePolicy",
        ).forEach(task::remove)
        root.put("databaseVersion", 1)
        val payload = root.getJSONObject("tables").toString() + "\n" + root.optJSONObject("settings")?.toString().orEmpty()
        val checksum = MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray()).joinToString("") { "%02x".format(it) }
        root.put("checksumSha256", checksum)

        backups.restoreBackup(root.toString())

        val restored = tasks.tasks.first().single()
        assertEquals("Legacy task", restored.title)
        assertEquals(TaskPriority.None, restored.priority)
        assertEquals("KeepLatest", restored.missedOccurrencePolicy.name)
        assertEquals(restored.createdAtMillis, restored.updatedAtMillis)
    }

    @Test fun everyAdvertisedLegacyBackupVersionRestoresAndReexportsAsCurrent() = runBlocking {
        val taskId = tasks.create(
            TaskDraft(
                title = "Version matrix task",
                scheduleKind = ScheduleKind.Recurring,
                date = FixedClock.today(),
                recurrence = RecurrenceRule(RecurrenceUnit.Weeks, weekdays = setOf(java.time.DayOfWeek.MONDAY), startDate = FixedClock.today()),
                priority = TaskPriority.High,
                area = "Migration",
                tags = setOf("fixture"),
                inbox = true,
                durationMinutes = 45,
                steps = listOf(TaskStepDraft(title = "Preserve me", notes = "Legacy note", position = 0)),
                showSubtaskProgress = true,
            ),
        )
        val customUnitId = measurements.createCustomUnit("fixture glass", "fg", UnitDimension.Volume, 240.0)
        val habitId = habits.create(
            HabitDraft(
                name = "Version matrix hydration",
                trackingMode = HabitTrackingMode.Decimal,
                dimension = UnitDimension.Volume,
                unitId = customUnitId,
                targetMin = 8.0,
                startDate = FixedClock.today(),
            ),
        )
        habits.log(habitId, 2.0)
        val goalId = goals.create(
            GoalDraft(
                name = "Version matrix goal",
                type = GoalType.AccumulateTotal,
                dimension = UnitDimension.Count,
                unitId = "count",
                targetMin = 20.0,
                startDate = FixedClock.today(),
                aggregation = GoalAggregation.Sum,
                entryMode = GoalEntryMode.AmountToAdd,
            ),
        )
        goals.recordMeasurement(goalId, 3.0)
        links.createRule(
            LinkRuleDraft(
                name = "Version matrix link",
                sourceType = LinkSourceType.Task,
                sourceEntityId = taskId,
                sourceMetric = LinkSourceMetric.Completion,
                targetGoalId = goalId,
                retroactiveFrom = FixedClock.today(),
            ),
            commitBackfill = true,
        )
        val exerciseId = gym.createExercise(
            ExerciseDraft(name = "Version matrix press", loadInterpretation = LoadInterpretation.PerHand),
        )
        val machineId = gym.createMachine(
            GymMachineDraft(
                exerciseId = exerciseId,
                name = "Version matrix stack",
                location = "Home",
                loadType = MachineLoadType.Mass,
                unitId = "kilogram",
                availableLoads = listOf(10.0, 15.0, 20.0),
                loadInterpretation = LoadInterpretation.MachineDisplayedMass,
                configurationGroupId = "matrix-stack",
                configurationVersion = 2,
                seatPosition = "4",
                backPosition = "2",
                attachment = "neutral handles",
                pulleyRatio = 0.5,
                stackMode = MachineStackMode.DualCombined,
                addOnPlateKg = 2.5,
                stackLabels = listOf("left", "right"),
                massMappingKg = mapOf(10.0 to 8.0, 15.0 to 12.0),
                compatibleForComparison = true,
            ),
        )
        repeat(2) { index ->
            val sessionId = gym.startWorkout("Version matrix session ${index + 1}")
            val placementId = gym.addExerciseToWorkout(sessionId, exerciseId, machineId)
            gym.addSet(placementId, WorkoutSetDraft(weight = 15.0 + index, reps = 8, planned = index == 0, completed = true))
            gym.finishWorkout(sessionId)
        }
        routines.createRoutine(
            RoutineDraft(
                name = "Version matrix routine",
                days = listOf(
                    RoutineDayDraft(
                        "Day A",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = exerciseId,
                                machineId = machineId,
                                plannedSets = listOf(WorkoutSetDraft(weight = 17.5, reps = 6, planned = true)),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val current = backups.exportBackup()

        (1..26).forEach { version ->
            val legacy = legacyTaskFixture(current, version)
            backups.restoreBackup(legacy)

            val restored = tasks.tasks.first().single()
            assertEquals("v$version title", "Version matrix task", restored.title)
            if (version >= 2) assertEquals("v$version step", "Preserve me", tasks.steps.first().single().title)
            if (version >= 4) assertEquals("v$version custom unit", "fixture glass", measurements.customUnits.first().single().name)
            if (version >= 5) {
                assertEquals("v$version workout sessions", 2, gym.sessions.first().size)
                assertEquals("v$version workout sets", 2, gym.sets.first().size)
            }
            if (version >= 6) assertEquals("v$version routine", "Version matrix routine", routines.routines.first().single().name)
            if (version >= 7) assertEquals("v$version habit", "Version matrix hydration", habits.habits.first().single().name)
            if (version >= 8) assertEquals("v$version goal", "Version matrix goal", goals.goals.first().single().name)
            if (version >= 9) assertEquals("v$version link", "Version matrix link", links.rules.first().single().name)
            if (version >= 15) {
                val machine = gym.machines.first().single()
                assertEquals("v$version machine", "Version matrix stack", machine.name)
                assertTrue(gym.workoutExercises.first().all { it.machineProfileUuidSnapshot == machine.uuid })
                if (version >= 6) assertTrue(routines.exercises.first().all {
                    it.machineProfileUuidSnapshot == machine.uuid
                })
            }
            assertEquals("v$version current re-export", 27, backups.previewBackup(backups.exportBackup()).databaseVersion)
        }
    }

    private fun legacyTaskFixture(current: String, version: Int): String {
        val root = JSONObject(current)
        val tables = root.getJSONObject("tables")
        mapOf(
            "task_occurrences" to 2, "task_steps" to 2, "task_step_states" to 2,
            "task_step_snapshots" to 3,
            "unit_definitions" to 4, "metric_definitions" to 4, "metric_entries" to 4,
            "areas" to 4, "tags" to 4, "entity_tag_links" to 4,
            "exercises" to 5, "exercise_categories" to 5, "exercise_category_joins" to 5,
            "workout_sessions" to 5, "workout_groups" to 5, "workout_exercises" to 5,
            "workout_sets" to 5,
            "gym_routines" to 6, "routine_days" to 6, "routine_exercises" to 6,
            "routine_sets" to 6, "personal_records" to 6, "graph_presets" to 6,
            "habits" to 7, "habit_checklist_items" to 7, "habit_logs" to 7,
            "habit_checklist_states" to 7, "habit_pauses" to 7,
            "goals" to 8, "goal_milestones" to 8, "goal_completion_snapshots" to 8,
            "link_rules" to 9, "contributions" to 9, "trigger_rules" to 9, "trigger_occurrences" to 9,
            "gym_machines" to 15,
        ).forEach { (table, introduced) -> if (version < introduced) tables.remove(table) }
        fun remove(table: String, vararg fields: String) {
            val rows = tables.optJSONArray(table) ?: return
            repeat(rows.length()) { index -> fields.forEach(rows.getJSONObject(index)::remove) }
        }
        if (version < 2) remove(
            "tasks",
            "updatedAtMillis", "showSubtaskProgress", "progressDisplay", "autoCompleteFromSteps", "repeatStepPolicy",
        )
        if (version < 10) remove("habits", "weekdayReminderMinutesCsv")
        if (version < 11) {
            remove("tasks", "pinned")
            remove("gym_routines", "pinned")
            remove("workout_sessions", "sourceRoutineId")
            remove("habits", "area", "tagsCsv")
            remove("goals", "area", "tagsCsv")
        }
        if (version < 12) remove(
            "goals",
            "aggregationPeriod", "rollingDays", "consistencyPeriod", "consistencyRequiredPeriods",
        )
        if (version < 13) {
            remove("task_steps", "notes", "weight")
            remove("task_step_snapshots", "notes", "weight")
        }
        if (version < 14) remove(
            "tasks",
            "priority", "area", "tagsCsv", "deadlineEpochDay", "recurrenceAnchor",
            "reminderOffsetsMinutesCsv", "locationReminderEnabled", "locationName",
            "locationLatitude", "locationLongitude", "locationRadiusMeters", "locationTrigger",
        )
        if (version < 15) {
            remove(
                "workout_exercises",
                "machineId", "machineNameSnapshot", "machineLoadTypeSnapshot", "machineUnitIdSnapshot", "machineLevelLabelSnapshot",
            )
            remove("workout_sets", "machineLoadValue")
            remove("routine_sets", "machineLoadValue")
            remove("routine_exercises", "machineId")
            remove("personal_records", "machineId")
        }
        if (version < 16) {
            remove("exercises", "loadInterpretation")
            remove("gym_machines", "loadInterpretation", "baseLoadKg")
            remove("workout_exercises", "loadInterpretationSnapshot", "baseLoadKgSnapshot")
        }
        if (version < 17) remove("tasks", "missedOccurrencePolicy")
        if (version < 18) remove(
            "workout_exercises",
            "trackingTypeSnapshot", "bodyweightLoadPolicySnapshot", "effectiveBodyweightPercentSnapshot",
            "oneRepMaxFormulaSnapshot", "includeInVolumeSnapshot", "includeInPersonalRecordsSnapshot",
        )
        if (version < 19) {
            remove(
                "gym_machines",
                "configurationGroupId", "configurationVersion", "seatPosition", "backPosition", "attachment",
                "pulleyRatio", "stackMode", "addOnPlateKg", "stackLabelsCsv", "massMappingCsv", "compatibleForComparison",
            )
            remove(
                "workout_exercises",
                "exerciseWeightUnitSnapshot", "loadMultiplierSnapshot", "machineConfigurationGroupSnapshot",
                "machineConfigurationVersionSnapshot", "machineConfigurationSnapshot", "machinePulleyRatioSnapshot",
                "machineStackModeSnapshot", "machineAddOnPlateKgSnapshot", "machineMassMappingCsvSnapshot",
            )
            remove(
                "workout_sets",
                "unilateral", "prescribedCanonicalWeightKg", "prescribedEnteredWeight", "prescribedWeightUnitId",
                "prescribedRepetitions", "prescribedRpe", "prescribedRir", "prescribedDurationSeconds",
                "prescribedMachineLoadValue",
            )
            remove("routine_sets", "unilateral")
        }
        if (version < 20) remove("tasks", "inbox", "durationMinutes", "effort")
        if (version < 21) remove("habits", "sourceMetricId")
        if (version < 22) {
            remove("workout_exercises", "machineProfileUuidSnapshot")
            remove(
                "routine_exercises",
                "equipmentBindingState", "machineProfileUuidSnapshot", "machineNameSnapshot",
                "machineLoadTypeSnapshot", "machineUnitIdSnapshot", "machineLevelLabelSnapshot",
                "machineLoadInterpretationSnapshot", "machineConfigurationGroupSnapshot",
                "machineConfigurationVersionSnapshot", "machineConfigurationSnapshot",
            )
            remove("personal_records", "machineProfileUuidSnapshot")
        }
        if (version < 26) {
            remove("tasks", "uuid", "manualPosition")
            remove("task_steps", "uuid")
        }
        if (version < 27) {
            remove("areas", "nameKey")
            remove("tasks", "areaId")
            remove("habits", "areaId")
            remove("goals", "areaId")
        }
        root.put("databaseVersion", version)
        val payload = tables.toString() + "\n" + root.optJSONObject("settings")?.toString().orEmpty()
        root.put(
            "checksumSha256",
            MessageDigest.getInstance("SHA-256").digest(payload.toByteArray()).joinToString("") { "%02x".format(it) },
        )
        return root.toString()
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
