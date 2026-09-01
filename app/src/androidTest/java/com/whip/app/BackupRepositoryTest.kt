package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.core.AppSettings
import com.whip.app.core.AppThemeMode
import com.whip.app.core.RepPrescriptionScheme
import com.whip.app.core.TrackedGymRecord
import com.whip.app.core.SettingsRepository
import com.whip.app.data.RoomBackupRepository
import com.whip.app.data.RoomGoalRepository
import com.whip.app.data.RoomGymRepository
import com.whip.app.data.RoomHabitRepository
import com.whip.app.data.RoomLinkRepository
import com.whip.app.data.RoomMeasurementRepository
import com.whip.app.data.RoomRoutineRepository
import com.whip.app.data.RoomTaskRepository
import com.whip.app.data.RoomTrackRepository
import com.whip.app.data.ContributionEntity
import com.whip.app.data.TriggerOccurrenceEntity
import com.whip.app.data.TrainingMaxDecisionEntity
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.CustomIdentityEmoji
import com.whip.app.domain.AreaScope
import com.whip.app.domain.GymMachineDraft
import com.whip.app.domain.GoalAggregation
import com.whip.app.domain.GoalDraft
import com.whip.app.domain.GoalType
import com.whip.app.domain.GoalStatus
import com.whip.app.domain.ElapsedDisplayUnit
import com.whip.app.domain.HabitDraft
import com.whip.app.domain.HabitTrackingMode
import com.whip.app.domain.LinkRuleDraft
import com.whip.app.domain.LinkSourceMetric
import com.whip.app.domain.LinkSourceType
import com.whip.app.domain.MachineLevelDirection
import com.whip.app.domain.MachineLoadType
import com.whip.app.domain.PersonalRecordType
import com.whip.app.domain.RecurrenceRule
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExerciseDraft
import com.whip.app.domain.RoutineLoadPrescriptionType
import com.whip.app.domain.RoutineMainWorkScheme
import com.whip.app.domain.RoutineProgramDraft
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineSupplementalScheme
import com.whip.app.domain.RoutineTrainingMaxSource
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskDraft
import com.whip.app.domain.TaskStepDraft
import com.whip.app.domain.UnitDimension
import com.whip.app.domain.TrackDraft
import com.whip.app.domain.TrackEntryDraft
import com.whip.app.domain.TrackFieldDraft
import com.whip.app.domain.TrackFieldType
import com.whip.app.domain.TrackValueDraft
import com.whip.app.domain.TriggerAction
import com.whip.app.domain.TriggerFieldMapping
import com.whip.app.domain.TriggerRuleDraft
import com.whip.app.domain.TriggerSourceProperty
import com.whip.app.domain.TriggerTargetType
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.outcomeForPeriod
import com.whip.app.domain.mutationBoundary
import com.whip.app.domain.progressBoundary
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
    private lateinit var tracks: RoomTrackRepository
    private lateinit var backups: RoomBackupRepository
    private lateinit var settings: FakeSettingsRepository

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WhipDatabase::class.java)
            .addCallback(WhipDatabase.integrityGuardCallback)
            .build()
        val ids = SequentialIds()
        measurements = RoomMeasurementRepository(database, FixedClock, ids)
        tracks = RoomTrackRepository(database, FixedClock, ids)
        tasks = RoomTaskRepository(database, FixedClock)
        habits = RoomHabitRepository(database, measurements, FixedClock, ids)
        goals = RoomGoalRepository(database, measurements, FixedClock, ids)
        gym = RoomGymRepository(database, FixedClock, ids)
        routines = RoomRoutineRepository(database, FixedClock, ids)
        links = RoomLinkRepository(database, measurements, FixedClock, ids)
        settings = FakeSettingsRepository(
            AppSettings(
                themeMode = AppThemeMode.Dark,
                compactItemLayout = true,
                timeZoneId = "America/Toronto",
                dayCutoffMinutes = 180,
                showAllUpcomingTaskOccurrences = true,
                activeTaskSortMode = "Manual",
                customIdentityEmojis = listOf(
                    CustomIdentityEmoji("🦊", "Fox"),
                    CustomIdentityEmoji("🦄", "Unicorn"),
                ),
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
                trackedGymRecords = listOf(
                    TrackedGymRecord("bench-stable-id", PersonalRecordType.EstimatedOneRepMax),
                    TrackedGymRecord("bench-stable-id", PersonalRecordType.MaxWeight, position = 1),
                ),
            ),
        )
        backups = RoomBackupRepository(database, settings)
    }
    @After fun tearDown() = database.close()

    @Test fun backupPreviewAndTransactionalRestoreRoundTrip() = runBlocking {
        val id = habits.create(HabitDraft(name = "Hydrate, safely", unitId = "glass", startDate = FixedClock.today()))
        habits.log(id, 2.0, note = "morning \"large\" glass")
        habits.skipDay(id, FixedClock.today().plusDays(1))
        tasks.create(TaskDraft(title = "Ship release", icon = "🚀"))
        database.routineDao().insertTrainingMaxDecision(
            TrainingMaxDecisionEntity(
                uuid = "tm-decision-backup",
                routineUuid = "routine-backup",
                sessionUuid = "session-backup",
                exerciseUuid = "exercise-backup",
                exerciseName = "Bench Press",
                cycle = 3,
                previousTrainingMax = 200.0,
                appliedDelta = 5.0,
                resultingTrainingMax = 205.0,
                unitId = "pound",
                standardDelta = 5.0,
                recommendationCategory = "StandardIncrease",
                recommendationDelta = 5.0,
                confidence = 0.85,
                reasonsText = "All required work passed",
                engineVersion = "five-three-one-progression/1",
                action = "UseStandard",
                createdAtMillis = 1234,
            ),
        )
        val json = backups.exportBackup()
        val preview = backups.previewBackup(json)
        assertEquals(2, preview.envelopeVersion)
        assertEquals(16, preview.databaseVersion)
        assertTrue(preview.checksumValid)
        assertTrue(preview.settingsIncluded)
        assertTrue(preview.totalRecords >= 3)
        val exportedTables = JSONObject(json).getJSONObject("tables")
        assertEquals(false, exportedTables.has("entity_tag_links"))
        assertEquals(true, exportedTables.has("goal_completion_snapshots"))
        assertEquals(true, exportedTables.has("goal_elapsed_reset_events"))

        backups.deleteAllData()
        assertTrue(habits.habits.first().isEmpty())
        assertEquals(AppThemeMode.System, settings.current().themeMode)
        assertEquals(false, settings.current().showAllUpcomingTaskOccurrences)
        backups.restoreBackup(json)
        assertEquals("Hydrate, safely", habits.habits.first().single().name)
        assertEquals(2.0, habits.logs.first().single().value!!, 0.0)
        assertEquals(FixedClock.today().plusDays(1), habits.skips.first().single().localDate)
        assertEquals("🚀", tasks.tasks.first().single().icon)
        assertEquals(AppThemeMode.Dark, settings.current().themeMode)
        assertEquals(true, settings.current().compactItemLayout)
        assertEquals("America/Toronto", settings.current().timeZoneId)
        assertEquals(180, settings.current().dayCutoffMinutes)
        assertEquals(true, settings.current().showAllUpcomingTaskOccurrences)
        assertEquals("Manual", settings.current().activeTaskSortMode)
        assertEquals(
            listOf(CustomIdentityEmoji("🦊", "Fox"), CustomIdentityEmoji("🦄", "Unicorn")),
            settings.current().customIdentityEmojis,
        )
        assertEquals("Hypertrophy", settings.current().repPrescriptionSchemes.single().name)
        assertEquals(8, settings.current().repPrescriptionSchemes.single().repetitionsMin)
        assertEquals(12, settings.current().repPrescriptionSchemes.single().repetitionsMax)
        assertEquals(90, settings.current().repPrescriptionSchemes.single().restSeconds)
        assertEquals(2, settings.current().trackedGymRecords.size)
        assertEquals(PersonalRecordType.MaxWeight, settings.current().trackedGymRecords.last().type)
        assertEquals("tm-decision-backup", routines.trainingMaxDecisions.first().single().uuid)
        assertTrue(backups.exportHabitsCsv().contains("\"Hydrate, safely\""))
        assertTrue(backups.exportHabitsCsv().contains("\"Skipped\""))
    }

    @Test fun goalLifecycleSnapshotsArchiveStateAndElapsedResetHistoryRoundTrip() = runBlocking {
        val progressId = goals.create(
            GoalDraft(
                name = "Read",
                type = GoalType.AccumulateTotal,
                targetMin = 10.0,
                startDate = FixedClock.today(),
            ),
        )
        var progressGoal = goals.get(progressId)!!
        goals.recordMeasurement(progressGoal.progressBoundary(), 6.0)
        goals.setStatus(progressGoal.mutationBoundary(), GoalStatus.Completed)
        progressGoal = goals.get(progressId)!!
        goals.setArchived(progressGoal.mutationBoundary(), true)

        val initial = FixedClock.now().minusSeconds(5 * 86_400)
        val elapsedId = goals.create(
            GoalDraft(
                name = "Recovery",
                type = GoalType.ElapsedSince,
                startDate = initial.atZone(ZoneId.of("UTC")).toLocalDate(),
                elapsedStartMillis = initial.toEpochMilli(),
            ),
        )
        val elapsedGoal = goals.get(elapsedId)!!
        goals.resetElapsedStart(elapsedGoal.mutationBoundary(), FixedClock.now().minusSeconds(3_600))

        val backup = backups.exportBackup()
        val preview = backups.previewBackup(backup)
        assertEquals(1, preview.tableCounts.getValue("goal_completion_snapshots"))
        assertEquals(1, preview.tableCounts.getValue("goal_elapsed_reset_events"))

        backups.deleteAllData()
        backups.restoreBackup(backup)

        val restoredClosed = goals.goals.first().single { it.name == "Read" }
        assertEquals(GoalStatus.Completed, restoredClosed.status)
        assertTrue(restoredClosed.archived)
        assertEquals(6.0, goals.closureSnapshots.first().single().value ?: -1.0, 0.0)
        val reset = goals.elapsedResetEvents.first().single()
        assertEquals(initial.toEpochMilli(), reset.previousStartMillis)
        assertEquals(elapsedGoal.uuid, reset.goalUuid)

        backups.mergeBackup(backup)
        assertEquals(1, goals.closureSnapshots.first().size)
        assertEquals(1, goals.elapsedResetEvents.first().size)
    }

    @Test fun versionElevenGymProgrammingBackfillsTypedSemanticsDuringRestore() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Legacy press", weightUnitId = "pound"))
        routines.createRoutine(
            RoutineDraft(
                name = "Legacy BBB",
                program = RoutineProgramDraft(
                    kind = RoutineProgramKind.BoringButBig,
                    phaseCount = 4,
                    phaseLabels = listOf("5s Week", "3s Week", "5/3/1 Week", "Deload"),
                ),
                days = listOf(
                    RoutineDayDraft(
                        "Press",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = exerciseId,
                                trainingMaxValue = 200.0,
                                trainingMaxUnitId = "pound",
                                cycleIncrementValue = 5.0,
                                trainingMaxSource = RoutineTrainingMaxSource.Explicit,
                                plannedSets = listOf(
                                    WorkoutSetDraft(
                                        reps = 5,
                                        classification = WorkoutSetClassification.Amrap,
                                        note = "Main Work · 5s Week · 85% TM",
                                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                        loadPercentage = 85.0,
                                    ),
                                    WorkoutSetDraft(
                                        reps = 10,
                                        note = "Supplemental · 50% TM",
                                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                        loadPercentage = 50.0,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val root = JSONObject(backups.exportBackup()).put("databaseVersion", 11)
        val tables = root.getJSONObject("tables")
        tables.getJSONArray("gym_routines").getJSONObject(0).apply {
            remove("trainingMaxIncreaseEligible")
            remove("programPhaseRolesCsv")
            remove("trainingMaxAdvanceAfterPhaseIndicesCsv")
            remove("programTemplateKey")
            remove("programTemplateRevision")
        }
        tables.getJSONArray("routine_exercises").getJSONObject(0).apply {
            remove("mainWorkScheme")
            remove("supplementalScheme")
            remove("assistanceRole")
            remove("placementKind")
            remove("assistanceCategory")
            remove("jokerSetsEnabled")
        }
        val legacySets = tables.getJSONArray("routine_sets")
        for (index in 0 until legacySets.length()) legacySets.getJSONObject(index).apply {
            remove("workSection")
            remove("optionalWorkKind")
            remove("mainWorkScheme")
            remove("supplementalScheme")
        }
        val payload = tables.toString() + "\n" + root.optJSONObject("settings")?.toString().orEmpty()
        root.put(
            "checksumSha256",
            MessageDigest.getInstance("SHA-256").digest(payload.toByteArray()).joinToString("") { "%02x".format(it) },
        )

        backups.deleteAllData()
        backups.restoreBackup(root.toString())

        assertEquals(RoutineMainWorkScheme.ClassicPrSet, routines.exercises.first().single().mainWorkScheme)
        assertEquals(RoutineSupplementalScheme.BoringButBig, routines.exercises.first().single().supplementalScheme)
        assertEquals(com.whip.app.domain.RoutinePlacementKind.MainLift, routines.exercises.first().single().placementKind)
        assertEquals(
            com.whip.app.domain.RoutineProgramTemplateKey.LegacyFiveThreeOne,
            routines.routines.first().single().programTemplateKey,
        )
        assertEquals(
            listOf(RoutineWorkSection.Main, RoutineWorkSection.Supplemental),
            routines.sets.first().sortedBy { it.position }.map { it.draft.workSection },
        )
        assertTrue(routines.sets.first().all { it.draft.mainWorkScheme == null && it.draft.supplementalScheme == null })
        assertEquals("Deload", routines.routines.first().single().programPhaseRoles.last().name)
        assertEquals(setOf(3), routines.routines.first().single().trainingMaxAdvanceAfterPhaseIndices)
    }

    @Test fun versionTwelveBackupBackfillsPlacementCategoryAndTemplateProvenance() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Legacy row", weightUnitId = "pound"))
        routines.createRoutine(
            RoutineDraft(
                name = "Legacy assistance",
                program = RoutineProgramDraft(RoutineProgramKind.FiveThreeOne, phaseCount = 1),
                days = listOf(
                    RoutineDayDraft(
                        "Pull",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = exerciseId,
                                assistanceRole = com.whip.app.domain.RoutineAssistanceRole.Pull,
                                plannedSets = listOf(
                                    WorkoutSetDraft(
                                        weight = 80.0,
                                        weightUnitId = "pound",
                                        reps = 10,
                                        workSection = RoutineWorkSection.Assistance,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val root = JSONObject(backups.exportBackup()).put("databaseVersion", 12)
        val tables = root.getJSONObject("tables")
        tables.getJSONArray("gym_routines").getJSONObject(0).apply {
            remove("programTemplateKey")
            remove("programTemplateRevision")
        }
        tables.getJSONArray("routine_exercises").getJSONObject(0).apply {
            remove("placementKind")
            remove("assistanceCategory")
        }
        val payload = tables.toString() + "\n" + root.optJSONObject("settings")?.toString().orEmpty()
        root.put(
            "checksumSha256",
            MessageDigest.getInstance("SHA-256").digest(payload.toByteArray()).joinToString("") { "%02x".format(it) },
        )

        backups.deleteAllData()
        backups.restoreBackup(root.toString())

        assertEquals(
            com.whip.app.domain.RoutinePlacementKind.Assistance,
            routines.exercises.first().single().placementKind,
        )
        assertEquals(
            com.whip.app.domain.RoutineAssistanceCategory.Pull,
            routines.exercises.first().single().assistanceCategory,
        )
        assertEquals(
            com.whip.app.domain.RoutineProgramTemplateKey.LegacyFiveThreeOne,
            routines.routines.first().single().programTemplateKey,
        )
    }

    @Test fun machineExerciseLinksAndLevelDirectionRoundTrip() = runBlocking {
        val rowId = gym.createExercise(ExerciseDraft(name = "Cable row"))
        val pressId = gym.createExercise(ExerciseDraft(name = "Cable press"))
        gym.createMachine(
            GymMachineDraft(
                name = "Shared cable tower",
                exerciseIds = setOf(rowId, pressId),
                loadType = MachineLoadType.Level,
                levelLabel = "position",
                availableLoads = listOf(1.0, 2.0, 3.0),
                levelDirection = MachineLevelDirection.HigherNumberLessResistance,
            ),
        )

        val json = backups.exportBackup()
        backups.deleteAllData()
        backups.restoreBackup(json)

        val restored = gym.machines.first().single()
        assertEquals(setOf("Cable row", "Cable press"), gym.exercises.first().filter { it.id in restored.exerciseIds }.mapTo(mutableSetOf()) { it.name })
        assertEquals(MachineLevelDirection.HigherNumberLessResistance, restored.levelDirection)
    }

    @Test fun legacyAutomationRowsAreAlwaysRestoredDormant() = runBlocking {
        val taskId = tasks.create(TaskDraft(title = "Legacy source"))
        val habitId = habits.create(HabitDraft(name = "Legacy target", startDate = FixedClock.today()))
        val goalId = goals.create(
            GoalDraft(
                name = "Legacy goal",
                type = GoalType.AccumulateTotal,
                dimension = UnitDimension.Count,
                unitId = "count",
                targetMin = 10.0,
                startDate = FixedClock.today(),
            ),
        )
        links.createRule(
            LinkRuleDraft(
                name = "Legacy progress",
                sourceType = LinkSourceType.Habit,
                sourceEntityId = habitId,
                sourceMetric = LinkSourceMetric.NumericValue,
                targetGoalId = goalId,
            ),
            commitBackfill = false,
        )
        val triggerId = links.createTrigger(
            TriggerRuleDraft(
                name = "Legacy prompt",
                sourceType = LinkSourceType.Task,
                sourceEntityId = taskId,
                targetType = TriggerTargetType.Habit,
                targetEntityId = habitId,
                action = TriggerAction.PromptHabit,
                notificationEnabled = true,
            ),
        )
        database.linkDao().upsertTriggerOccurrence(
            TriggerOccurrenceEntity(
                triggerRuleId = triggerId,
                sourceEventId = "legacy-pending",
                availableAtMillis = 2_000,
                deliveredAtMillis = null,
                dismissedAtMillis = null,
                remindAtMillis = 3_000,
                fulfilledEntryId = null,
                sourceSnapshot = "{}",
            ),
        )

        val exported = JSONObject(backups.exportBackup())
        assertEquals(16, exported.getInt("databaseVersion"))
        assertEquals(0, exported.getJSONObject("tables").getJSONArray("link_rules").getJSONObject(0).getInt("enabled"))
        assertEquals(0, exported.getJSONObject("tables").getJSONArray("trigger_rules").getJSONObject(0).getInt("enabled"))

        // Simulate a checksum-valid backup written by the last Automation-capable format.
        exported.put("databaseVersion", 9)
        val tables = exported.getJSONObject("tables")
        tables.getJSONArray("link_rules").getJSONObject(0).put("enabled", 1)
        tables.getJSONArray("trigger_rules").getJSONObject(0)
            .put("enabled", 1)
            .put("notificationEnabled", 1)
        tables.getJSONArray("trigger_occurrences").getJSONObject(0)
            .put("dismissedAtMillis", JSONObject.NULL)
            .put("remindAtMillis", 3_000)
        val payload = tables.toString() + "\n" + exported.optJSONObject("settings")?.toString().orEmpty()
        exported.put(
            "checksumSha256",
            MessageDigest.getInstance("SHA-256").digest(payload.toByteArray()).joinToString("") { "%02x".format(it) },
        )

        backups.deleteAllData()
        backups.restoreBackup(exported.toString())

        assertEquals(false, links.rules.first().single().enabled)
        assertEquals(false, links.triggerRules.first().single().enabled)
        assertEquals(false, links.triggerRules.first().single().notificationEnabled)
        val occurrence = links.triggerOccurrences.first().single()
        assertEquals(Instant.ofEpochMilli(2_000), occurrence.dismissedAt)
        assertEquals(null, occurrence.remindAt)
    }

    @Test fun versionEightMachineLinkUpgradesToManyToManyWithSafeDirectionDefault() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft(name = "Legacy row"))
        gym.createMachine(GymMachineDraft(exerciseId = exerciseId, name = "Legacy cable"))
        val root = JSONObject(backups.exportBackup()).put("databaseVersion", 8)
        val tables = root.getJSONObject("tables")
        tables.remove("gym_machine_exercise_joins")
        val machines = tables.getJSONArray("gym_machines")
        for (index in 0 until machines.length()) machines.getJSONObject(index).remove("levelDirection")
        val payload = tables.toString() + "\n" + root.optJSONObject("settings")?.toString().orEmpty()
        root.put(
            "checksumSha256",
            MessageDigest.getInstance("SHA-256").digest(payload.toByteArray()).joinToString("") { "%02x".format(it) },
        )

        backups.deleteAllData()
        backups.restoreBackup(root.toString())

        val restoredExercise = gym.exercises.first().single()
        val restoredMachine = gym.machines.first().single()
        assertEquals(setOf(restoredExercise.id), restoredMachine.exerciseIds)
        assertEquals(MachineLevelDirection.HigherNumberMoreResistance, restoredMachine.levelDirection)
    }

    @Test fun areaIdentityAndScopeRoundTripAcrossEveryProductivityDomain() = runBlocking {
        val workId = measurements.ensureArea("Work")
        settings.update {
            it.copy(
                activeAreaScope = AreaScope.One(workId).storageKey,
                areaOpeningMode = com.whip.app.core.AreaOpeningMode.Chosen,
                chosenOpeningAreaScope = AreaScope.One(workId).storageKey,
            )
        }
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
        assertEquals(com.whip.app.core.AreaOpeningMode.Chosen, settings.current().areaOpeningMode)
        assertEquals(AreaScope.One(workId).storageKey, settings.current().chosenOpeningAreaScope)
        assertTrue(backups.exportTasksCsv().contains("\"Work\""))
        assertTrue(backups.exportHabitsCsv().contains("\"Work\""))
        assertTrue(backups.exportGoalsCsv().contains("\"Work\""))
    }

    @Test fun elapsedGoalBackupAndCsvPreserveItsAuthoritativeStartAndView() = runBlocking {
        val started = FixedClock.now().minusSeconds(12_345)
        goals.create(
            GoalDraft(
                name = "Time Since",
                type = GoalType.ElapsedSince,
                startDate = FixedClock.today(),
                elapsedStartMillis = started.toEpochMilli(),
                elapsedDisplayUnit = ElapsedDisplayUnit.Hours,
            ),
        )

        val backup = backups.exportBackup()
        backups.deleteAllData()
        backups.restoreBackup(backup)

        val restored = goals.goals.first().single()
        assertEquals(started.toEpochMilli(), restored.elapsedStartMillis)
        assertEquals(ElapsedDisplayUnit.Hours, restored.elapsedDisplayUnit)
        val csv = backups.exportGoalsCsv()
        assertTrue(csv.lineSequence().first().contains("elapsedStartMillis"))
        assertTrue(csv.contains(started.toEpochMilli().toString()))
        assertTrue(csv.contains("Hours"))
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

    @Test fun mergeRemapsBothSidesOfFulfilledTrackPromptRelationship() = runBlocking {
        val trackId = tracks.create(
            TrackDraft("Imported Books", fields = listOf(TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true))),
        )
        val track = requireNotNull(tracks.projection(trackId))
        val habitId = habits.create(HabitDraft("Imported Reading Habit", trackingMode = HabitTrackingMode.CheckOff, startDate = FixedClock.today()))
        val triggerRuleId = links.createTrigger(
            TriggerRuleDraft(
                name = "Imported Capture",
                sourceType = LinkSourceType.Habit,
                sourceEntityId = habitId,
                targetType = TriggerTargetType.Track,
                targetEntityId = trackId,
                action = TriggerAction.PromptTrackEntry,
                mappings = listOf(TriggerFieldMapping(track.primaryField.id, TriggerSourceProperty.Name)),
            ),
        )
        val sourceOccurrenceId = database.linkDao().upsertTriggerOccurrence(
            TriggerOccurrenceEntity(
                triggerRuleId = triggerRuleId,
                sourceEventId = "legacy-habit-event",
                availableAtMillis = FixedClock.now().toEpochMilli(),
                deliveredAtMillis = FixedClock.now().toEpochMilli(),
                dismissedAtMillis = null,
                remindAtMillis = null,
                fulfilledEntryId = null,
                sourceSnapshot = "{}",
            ),
        )
        val sourceEntryId = tracks.addEntry(
            trackId,
            TrackEntryDraft(
                entryDate = FixedClock.today(),
                values = mapOf(track.primaryField.uuid to TrackValueDraft(textValue = "Imported Reading Habit")),
                sourceOccurrenceId = sourceOccurrenceId,
                sourceExplanation = "Preserved fulfilled prompt",
            ),
        )
        database.linkDao().fulfillTriggerOccurrence(sourceOccurrenceId, sourceEntryId, FixedClock.now().toEpochMilli())
        val portable = backups.exportBackup()

        backups.deleteAllData()
        tracks.create(TrackDraft("Local Track", fields = listOf(TrackFieldDraft("Name", TrackFieldType.ShortText, required = true, primary = true))))
        habits.create(HabitDraft("Local Habit", startDate = FixedClock.today()))
        backups.mergeBackup(portable)

        val importedTrack = tracks.tracks.first().first { it.name == "Imported Books" }
        val importedEntry = requireNotNull(tracks.projection(importedTrack.id)).entries.single().entry
        val importedOccurrence = links.triggerOccurrences.first().single()
        assertEquals(importedOccurrence.id, importedEntry.sourceOccurrenceId)
        assertEquals(importedEntry.id, importedOccurrence.fulfilledEntryId)
        assertEquals(false, links.triggerRules.first().single().enabled)

        val counts = listOf(tracks.tracks.first().size, tracks.entries.first().size, links.triggerRules.first().size, links.triggerOccurrences.first().size)
        backups.mergeBackup(portable)
        assertEquals(counts, listOf(tracks.tracks.first().size, tracks.entries.first().size, links.triggerRules.first().size, links.triggerOccurrences.first().size))
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
        val retiredRuleId = links.createRule(
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
        database.linkDao().upsertContribution(
            ContributionEntity(
                uuid = "retired-contribution",
                linkRuleId = retiredRuleId,
                sourceEventId = "habit:$habitId:legacy",
                sourceType = LinkSourceType.Habit.name,
                sourceEntityId = habitId,
                targetGoalId = goalId,
                metricEntryId = null,
                canonicalValue = 3.0,
                localEpochDay = FixedClock.today().toEpochDay(),
                timestampMillis = FixedClock.now().toEpochMilli(),
                excluded = false,
                overrideValue = null,
                explanation = "Preserved retired contribution",
                createdAtMillis = FixedClock.now().toEpochMilli(),
                updatedAtMillis = FixedClock.now().toEpochMilli(),
            ),
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

        val trackId = tracks.create(TrackDraft("Books Read", fields = listOf(TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true))))
        val track = requireNotNull(tracks.projection(trackId))
        tracks.addEntry(trackId, TrackEntryDraft(FixedClock.today(), mapOf(track.primaryField.uuid to TrackValueDraft(textValue = "The Dispossessed"))))

        val json = backups.exportBackup()
        val preview = backups.previewBackup(json)
        assertTrue(preview.checksumValid)
        listOf(
            "tasks", "task_steps", "task_occurrences", "habits", "habit_logs", "goals",
            "metric_entries", "link_rules", "contributions", "exercises", "exercise_categories",
            "workout_sessions", "workout_exercises", "workout_sets", "gym_routines", "routine_days",
            "routine_exercises", "routine_sets", "personal_records", "graph_presets",
            "tracks", "track_fields", "track_entries", "track_values",
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
        assertEquals(false, links.rules.first().single().enabled)
        assertEquals(1, links.contributions.first().size)
        assertEquals("Flat Barbell Bench Press", gym.exercises.first().single().name)
        assertEquals(1, gym.sets.first().size)
        assertEquals("Push routine", routines.routines.first().single().name)
        assertEquals(1, routines.sets.first().size)
        assertTrue(routines.personalRecords.first().isNotEmpty())
        assertEquals("Bench e1RM", routines.graphPresets.first().single().name)
        val restoredTrack = requireNotNull(tracks.projection(tracks.tracks.first().single().id))
        assertEquals("The Dispossessed", restoredTrack.primaryText(restoredTrack.entries.single()))
        assertEquals(setOf(restoredTrack.entries.single().entry.id), tracks.searchEntryIds(restoredTrack.track.id, "Dispossessed"))
    }

    @Test fun tamperedBackupIsRejectedBeforeItCanReplaceLiveData() = runBlocking {
        habits.create(HabitDraft(name = "Keep this", startDate = FixedClock.today()))
        val original = backups.exportBackup()
        val tampered = original.replace("Keep this", "Tampered")

        assertEquals(false, backups.previewBackup(tampered).checksumValid)
        assertTrue(runCatching { backups.restoreBackup(tampered) }.isFailure)
        assertEquals("Keep this", habits.habits.first().single().name)
    }

    @Test fun versionFiveBackupWithoutScaleIncrementUpgradesDuringRestore() = runBlocking {
        tasks.create(TaskDraft(title = "Legacy Task"))
        tracks.create(
            TrackDraft(
                "Legacy Ratings",
                fields = listOf(
                    TrackFieldDraft("Title", TrackFieldType.ShortText, required = true, primary = true),
                    TrackFieldDraft("Rating", TrackFieldType.Scale, scaleMin = 1, scaleMax = 5),
                ),
            ),
        )
        val root = JSONObject(backups.exportBackup()).put("databaseVersion", 5)
        val tables = root.getJSONObject("tables")
        tables.remove("habit_skips")
        tables.remove("gym_machine_exercise_joins")
        val fields = tables.getJSONArray("track_fields")
        for (index in 0 until fields.length()) fields.getJSONObject(index).remove("scaleStep")
        val taskRows = tables.getJSONArray("tasks")
        for (index in 0 until taskRows.length()) taskRows.getJSONObject(index).remove("icon")
        val payload = tables.toString() + "\n" + root.optJSONObject("settings")?.toString().orEmpty()
        root.put(
            "checksumSha256",
            MessageDigest.getInstance("SHA-256").digest(payload.toByteArray()).joinToString("") { "%02x".format(it) },
        )

        val preview = backups.previewBackup(root.toString())
        assertEquals(5, preview.databaseVersion)
        assertTrue(preview.restoreCompatible)
        assertTrue(preview.compatibilityMessage.orEmpty().contains("upgraded"))

        backups.deleteAllData()
        backups.restoreBackup(root.toString())

        val restored = requireNotNull(tracks.projection(tracks.tracks.first().single().id))
        assertEquals(1.0, restored.fields.single { it.name == "Rating" }.scaleStep, 0.0)
        assertEquals("✅", tasks.tasks.first().single().icon)
    }

    @Test fun duplicateStableIdentityIsRejectedEvenWithAValidChecksum() = runBlocking {
        tracks.create(TrackDraft("Unique Track", fields = listOf(TrackFieldDraft("Name", TrackFieldType.ShortText, required = true, primary = true))))
        val root = JSONObject(backups.exportBackup())
        val tables = root.getJSONObject("tables")
        val trackRows = tables.getJSONArray("tracks")
        trackRows.put(JSONObject(trackRows.getJSONObject(0).toString()))
        val payload = tables.toString() + "\n" + root.optJSONObject("settings")?.toString().orEmpty()
        root.put(
            "checksumSha256",
            MessageDigest.getInstance("SHA-256").digest(payload.toByteArray()).joinToString("") { "%02x".format(it) },
        )

        assertTrue(runCatching { backups.previewBackup(root.toString()) }.isFailure)
        assertTrue(runCatching { backups.mergeBackup(root.toString()) }.isFailure)
    }

    @Test fun nonCurrentBackupVersionsOrTableSetsCannotBePreviewedOrRestored() = runBlocking {
        habits.create(HabitDraft(name = "Keep local", startDate = FixedClock.today()))
        val current = backups.exportBackup()
        val wrongDatabase = JSONObject(current).put("databaseVersion", 17).toString()
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

    @Test fun sixOfEightActionsUndoAndBackupRemainOneTruthfulPeriodOutcome() = runBlocking {
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
        // +1, quick +2, decrement -1, and Set to 6 (stored as the +4 delta).
        listOf(1.0, 2.0, -1.0, 4.0).forEach { habits.log(habitId, it) }
        val habit = habits.habits.first().single()
        assertEquals(false, habit.outcomeForPeriod(habits.logs.first(), FixedClock.today()))
        assertEquals(6.0, habits.logs.first().sumOf { it.value ?: 0.0 }, 0.0)

        val completingLog = habits.log(habitId, 2.0)
        assertEquals(true, habit.outcomeForPeriod(habits.logs.first(), FixedClock.today()))
        assertEquals(8.0, habits.logs.first().sumOf { it.value ?: 0.0 }, 0.0)

        habits.undoLog(completingLog)
        assertEquals(false, habit.outcomeForPeriod(habits.logs.first(), FixedClock.today()))
        assertEquals(6.0, habits.logs.first().sumOf { it.value ?: 0.0 }, 0.0)
        habits.log(habitId, 2.0)

        val archive = backups.exportBackup()
        backups.deleteAllData()
        backups.restoreBackup(archive)

        val restoredHabit = habits.habits.first().single()
        assertEquals(true, restoredHabit.outcomeForPeriod(habits.logs.first(), FixedClock.today()))
        assertEquals(8.0, habits.logs.first().sumOf { it.value ?: 0.0 }, 0.0)
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
