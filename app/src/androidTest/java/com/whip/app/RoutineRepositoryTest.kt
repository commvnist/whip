package com.whip.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.whip.app.core.WhipClock
import com.whip.app.core.WhipIdGenerator
import com.whip.app.data.RoomGymRepository
import com.whip.app.data.RoomRoutineRepository
import com.whip.app.data.WhipDatabase
import com.whip.app.domain.ExerciseDraft
import com.whip.app.domain.GymMachineDraft
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.MachineLoadType
import com.whip.app.domain.PersonalRecordType
import com.whip.app.domain.RoutineDayDraft
import com.whip.app.domain.RoutineDraft
import com.whip.app.domain.RoutineExerciseDraft
import com.whip.app.domain.RoutineLoadPrescriptionType
import com.whip.app.domain.RoutineMainWorkScheme
import com.whip.app.domain.RoutineOptionalWorkKind
import com.whip.app.domain.RoutineProgramDraft
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineProgramPhaseRole
import com.whip.app.domain.RoutineProgramTemplateKey
import com.whip.app.domain.RoutineProgressionMode
import com.whip.app.domain.RoutineSupplementalScheme
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.RoutineTrainingMaxSource
import com.whip.app.domain.RoutineAssistanceRole
import com.whip.app.domain.RoutineAssistanceCategory
import com.whip.app.domain.RoutinePlacementKind
import com.whip.app.domain.TrainingMaxCycleDecision
import com.whip.app.domain.TrainingMaxDecisionAction
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.WorkoutSetDraft
import com.whip.app.domain.massToKilograms
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
class RoutineRepositoryTest {
    private lateinit var database: WhipDatabase
    private lateinit var gym: RoomGymRepository
    private lateinit var routines: RoomRoutineRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WhipDatabase::class.java,
        ).build()
        val ids = SequentialIds()
        gym = RoomGymRepository(database, FixedClock, ids)
        routines = RoomRoutineRepository(database, FixedClock, ids)
    }

    @After fun tearDown() = database.close()

    @Test
    fun startingRoutineDoesNotMutateTemplate() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Bench"))
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Push",
                days = listOf(
                    RoutineDayDraft(
                        "Day A",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId,
                                plannedSets = listOf(WorkoutSetDraft(weight = 80.0, reps = 8)),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val templateBefore = routines.sets.first()

        routines.startRoutine(routineId)
        val liveSet = gym.sets.first().single()
        gym.updateSet(liveSet.id, WorkoutSetDraft(weight = 85.0, reps = 8))

        assertEquals(80.0, routines.sets.first().single().draft.weight!!, 0.0)
        assertEquals(templateBefore.single().id, routines.sets.first().single().id)
    }

    @Test
    fun exerciseAddedDuringRoutineWorkoutStaysSessionScopedAndRemainsInHistoryAndRecords() = runBlocking {
        val benchId = gym.createExercise(ExerciseDraft("Bench"))
        val curlsId = gym.createExercise(ExerciseDraft("Hammer curls"))
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Bench day",
                days = listOf(
                    RoutineDayDraft(
                        "Day A",
                        listOf(
                            RoutineExerciseDraft(
                                benchId,
                                plannedSets = listOf(WorkoutSetDraft(weight = 80.0, reps = 5)),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val templateExerciseIds = routines.exercises.first().map { it.exerciseId }
        val templateSets = routines.sets.first()

        val firstSessionId = routines.startRoutine(routineId)
        val addedPlacementId = gym.addExerciseToWorkout(firstSessionId, curlsId)
        gym.addSet(
            addedPlacementId,
            WorkoutSetDraft(weight = 20.0, reps = 12, completed = true),
        )
        gym.finishWorkout(firstSessionId)
        routines.rebuildPersonalRecords(curlsId)

        assertEquals(listOf(benchId), templateExerciseIds)
        assertEquals(templateExerciseIds, routines.exercises.first().map { it.exerciseId })
        assertEquals(templateSets, routines.sets.first())
        assertEquals(
            setOf(benchId, curlsId),
            gym.workoutExercises.first().filter { it.sessionId == firstSessionId }.map { it.exerciseId }.toSet(),
        )
        assertTrue(gym.sets.first().single { it.workoutExerciseId == addedPlacementId }.completed)
        assertTrue(routines.personalRecords.first().any { it.exerciseId == curlsId && it.current })

        val nextSessionId = routines.startRoutine(routineId)
        assertEquals(
            listOf(benchId),
            gym.workoutExercises.first().filter { it.sessionId == nextSessionId }.map { it.exerciseId },
        )
        assertEquals(
            setOf(benchId, curlsId),
            gym.workoutExercises.first().filter { it.sessionId == firstSessionId }.map { it.exerciseId }.toSet(),
        )
    }

    @Test
    fun saveWorkoutAsRoutineExcludesEmptyRetiredPlacementsAndKeepsPerformedRetiredWork() = runBlocking {
        val pressId = gym.createExercise(ExerciseDraft("Press"))
        val inclineId = gym.createExercise(ExerciseDraft("Incline press"))
        val rowId = gym.createExercise(ExerciseDraft("Row"))
        val sourceSessionId = gym.startWorkout("Reusable workout")

        val unperformedPress = gym.addExerciseToWorkout(sourceSessionId, pressId)
        gym.addSet(unperformedPress, WorkoutSetDraft(weight = 50.0, reps = 5, planned = true))
        val activeIncline = gym.substituteWorkoutExercise(unperformedPress, inclineId)
        val inclineSet = gym.sets.first().single { it.workoutExerciseId == activeIncline }
        gym.updateSet(inclineSet.id, WorkoutSetDraft(weight = 45.0, reps = 8, completed = true))

        val performedRow = gym.addExerciseToWorkout(sourceSessionId, rowId)
        gym.addSet(
            performedRow,
            WorkoutSetDraft(
                weight = 70.0,
                reps = 10,
                completed = true,
                classification = WorkoutSetClassification.Failure,
                workSection = RoutineWorkSection.Main,
                mainWorkScheme = RoutineMainWorkScheme.ClassicPrSet,
            ),
        )
        gym.removeWorkoutExercise(performedRow)

        val routineId = routines.saveWorkoutAsRoutine(sourceSessionId, "Projected history")
        val routineDayId = routines.days.first().single { it.routineId == routineId }.id
        val savedPlacements = routines.exercises.first()
            .filter { it.routineDayId == routineDayId }
            .sortedBy { it.position }

        assertEquals(listOf(inclineId, rowId), savedPlacements.map { it.exerciseId })
        assertFalse(savedPlacements.any { it.exerciseId == pressId })
        val savedSets = routines.sets.first()
            .filter { set -> savedPlacements.any { it.id == set.routineExerciseId } }
        assertEquals(2, savedSets.size)
        val savedRowPlacement = savedPlacements.single { it.exerciseId == rowId }
        val savedRowSet = savedSets.single { it.routineExerciseId == savedRowPlacement.id }.draft
        assertEquals(10, savedRowSet.reps)
        assertEquals(null, savedRowSet.repsMax)
        assertEquals(WorkoutSetClassification.Working, savedRowSet.classification)
        assertEquals(RoutineWorkSection.Unspecified, savedRowSet.workSection)
        assertEquals(RoutineOptionalWorkKind.None, savedRowSet.optionalWorkKind)
        assertEquals(null, savedRowSet.mainWorkScheme)
        assertEquals(RoutinePlacementKind.General, savedRowPlacement.placementKind)
    }

    @Test
    fun activeRoutineWorkoutBlocksTemplateMutationUntilFinishOrDiscard() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Bench"))
        fun draft(name: String) = RoutineDraft(
            name = name,
            days = listOf(
                RoutineDayDraft(
                    "Day A",
                    listOf(
                        RoutineExerciseDraft(
                            exerciseId,
                            plannedSets = listOf(WorkoutSetDraft(weight = 80.0, reps = 5)),
                        ),
                    ),
                ),
            ),
        )
        val routineId = routines.createRoutine(draft("Original"))

        val finishedSession = routines.startRoutine(routineId)
        val activeFailure = runCatching { routines.updateRoutine(routineId, draft("Blocked")) }.exceptionOrNull()
        assertTrue(activeFailure is IllegalArgumentException)
        assertEquals("Finish or discard the active workout before editing this routine", activeFailure?.message)
        assertEquals("Original", routines.routines.first().single { it.id == routineId }.name)

        gym.finishWorkout(finishedSession)
        routines.updateRoutine(routineId, draft("After finish"))
        assertEquals("After finish", routines.routines.first().single { it.id == routineId }.name)

        val discardedSession = routines.startRoutine(routineId)
        gym.discardWorkout(discardedSession)
        routines.updateRoutine(routineId, draft("After discard"))
        assertEquals("After discard", routines.routines.first().single { it.id == routineId }.name)
    }

    @Test
    fun changingTrainingMaxSourceToDerivedClearsTheStoredExplicitValue() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Bench", weightUnitId = "pound"))
        fun draft(source: RoutineTrainingMaxSource, value: Double?, increment: Double?) = RoutineDraft(
            name = "Bench cycle",
            days = listOf(
                RoutineDayDraft(
                    "Day A",
                    listOf(
                        RoutineExerciseDraft(
                            exerciseId = exerciseId,
                            trainingMaxPercent = 90.0,
                            trainingMaxValue = value,
                            trainingMaxUnitId = "pound",
                            cycleIncrementValue = increment,
                            trainingMaxSource = source,
                            plannedSets = listOf(
                                WorkoutSetDraft(
                                    reps = 5,
                                    loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                    loadPercentage = 80.0,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val routineId = routines.createRoutine(draft(RoutineTrainingMaxSource.Explicit, 225.0, 5.0))

        routines.updateRoutine(
            routineId,
            draft(RoutineTrainingMaxSource.EstimatedOneRepMaxPercent, null, null),
        )

        val placement = routines.exercises.first().single()
        assertEquals(RoutineTrainingMaxSource.EstimatedOneRepMaxPercent, placement.trainingMaxSource)
        assertEquals(null, placement.trainingMaxValue)
        assertEquals(null, placement.trainingMaxKg)
        assertEquals(null, placement.cycleIncrementValue)
    }

    @Test
    fun unilateralPercentagePrescriptionUsesTheSetSpecificLoadMultiplier() = runBlocking {
        val exerciseId = gym.createExercise(
            ExerciseDraft(
                name = "Single-arm press",
                weightUnitId = "kilogram",
                barWeightKg = null,
                loadInterpretation = LoadInterpretation.PerSide,
            ),
        )
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Unilateral percentage",
                days = listOf(
                    RoutineDayDraft(
                        "Day A",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = exerciseId,
                                trainingMaxValue = 100.0,
                                trainingMaxUnitId = "kilogram",
                                trainingMaxSource = RoutineTrainingMaxSource.Explicit,
                                plannedSets = listOf(
                                    WorkoutSetDraft(
                                        reps = 5,
                                        unilateral = true,
                                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                        loadPercentage = 80.0,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        routines.startRoutine(routineId)

        val set = gym.sets.first().single()
        assertEquals(80.0, requireNotNull(set.enteredWeight), 0.0)
        assertEquals(80.0, requireNotNull(set.prescribedCanonicalWeightKg), 0.0)
    }

    @Test
    fun plannedRoutineMachineCanChangeBeforeCompletionAndRetargetsThePrescription() = runBlocking {
        val exerciseId = gym.createExercise(
            ExerciseDraft(name = "Machine press", weightUnitId = "kilogram"),
        )
        val originalMachineId = gym.createMachine(
            GymMachineDraft(
                exerciseId = exerciseId,
                name = "Occupied press",
                unitId = "kilogram",
                availableLoads = listOf(70.0, 80.0, 90.0),
            ),
        )
        val replacementMachineId = gym.createMachine(
            GymMachineDraft(
                exerciseId = exerciseId,
                name = "Plate-loaded press",
                unitId = "kilogram",
                availableLoads = listOf(30.0, 35.0, 40.0),
                loadInterpretation = LoadInterpretation.PerSide,
                baseLoadKg = 10.0,
            ),
        )
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Machine TM",
                days = listOf(
                    RoutineDayDraft(
                        "A",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = exerciseId,
                                machineId = originalMachineId,
                                trainingMaxValue = 100.0,
                                trainingMaxUnitId = "kilogram",
                                trainingMaxSource = RoutineTrainingMaxSource.Explicit,
                                plannedSets = listOf(
                                    WorkoutSetDraft(
                                        reps = 5,
                                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                        loadPercentage = 80.0,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        routines.startRoutine(routineId)
        val placement = gym.workoutExercises.first().single()
        gym.updateWorkoutExerciseDetails(
            placement.id,
            notes = "Use the open plate-loaded press",
            groupId = placement.groupId,
            machineId = replacementMachineId,
        )

        val retargetedPlacement = gym.workoutExercises.first().single()
        val retargetedSet = gym.sets.first().single()
        assertEquals(replacementMachineId, retargetedPlacement.machineId)
        assertEquals("Use the open plate-loaded press", retargetedPlacement.notes)
        assertEquals(35.0, requireNotNull(retargetedSet.enteredWeight), 0.0)
        assertEquals(35.0, requireNotNull(retargetedSet.prescribedEnteredWeight), 0.0)
        assertEquals(80.0, requireNotNull(retargetedSet.canonicalWeightKg), 0.000001)
        assertEquals(80.0, requireNotNull(retargetedSet.prescribedCanonicalWeightKg), 0.000001)

        gym.setSetCompleted(retargetedSet.id, completed = true, autoStartRest = false)
        val locked = runCatching {
            gym.setWorkoutExerciseMachine(placement.id, originalMachineId)
        }.exceptionOrNull()
        assertTrue(locked is IllegalArgumentException)
        assertTrue(locked?.message.orEmpty().contains("after a set is completed"))
    }

    @Test
    fun bodyweightDependentAndAssistedPercentagePrescriptionsAreRejected() = runBlocking {
        suspend fun rejectionFor(draft: ExerciseDraft): Throwable {
            val exerciseId = gym.createExercise(draft)
            return requireNotNull(
                runCatching {
                    routines.createRoutine(
                        RoutineDraft(
                            name = "Invalid percentage",
                            days = listOf(
                                RoutineDayDraft(
                                    "A",
                                    listOf(
                                        RoutineExerciseDraft(
                                            exerciseId = exerciseId,
                                            trainingMaxValue = 100.0,
                                            trainingMaxSource = RoutineTrainingMaxSource.Explicit,
                                            plannedSets = listOf(
                                                WorkoutSetDraft(
                                                    reps = 5,
                                                    loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                                    loadPercentage = 75.0,
                                                ),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    )
                }.exceptionOrNull(),
            )
        }

        val assisted = rejectionFor(
            ExerciseDraft("Assisted pull-up", trackingType = com.whip.app.domain.ExerciseTrackingType.AssistedBodyweightReps),
        )
        assertTrue(assisted.message.orEmpty().contains("mass-tracked exercise"))
        val dynamicBodyweight = rejectionFor(
            ExerciseDraft("Weighted pull-up", loadInterpretation = LoadInterpretation.BodyweightPlusExternal),
        )
        assertTrue(dynamicBodyweight.message.orEmpty().contains("mass-based exercise or machine"))
    }

    @Test
    fun exactMachineLoadRemainsExactInsteadOfSilentlySnapping() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Machine row"))
        val machineId = gym.createMachine(
            GymMachineDraft(
                exerciseId = exerciseId,
                name = "Row stack",
                availableLoads = listOf(50.0, 60.0),
            ),
        )
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Exact row",
                days = listOf(
                    RoutineDayDraft(
                        "A",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = exerciseId,
                                machineId = machineId,
                                plannedSets = listOf(WorkoutSetDraft(weight = 55.0, reps = 8)),
                            ),
                        ),
                    ),
                ),
            ),
        )

        routines.startRoutine(routineId)

        assertEquals(55.0, requireNotNull(gym.sets.first().single().enteredWeight), 0.0)
    }

    @Test
    fun staticLoadCycleAdvancesOnlyAfterCompletedNonFailureWork() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Wave press"))
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Wave",
                days = listOf(
                    RoutineDayDraft(
                        "A",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = exerciseId,
                                progressionPercentages = listOf(100.0, 105.0),
                                plannedSets = listOf(WorkoutSetDraft(weight = 50.0, reps = 5)),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val incomplete = routines.startRoutine(routineId)
        gym.finishWorkout(incomplete)
        assertEquals(0, routines.days.first().single().progressionIndex)

        val completed = routines.startRoutine(routineId)
        val set = gym.sets.first().single { workoutSet ->
            gym.workoutExercises.first().any { placement ->
                placement.sessionId == completed && placement.id == workoutSet.workoutExerciseId
            }
        }
        val completedPlacementId = set.workoutExerciseId
        val workoutOnlySetId = gym.addSet(completedPlacementId)
        assertFalse(gym.sets.first().single { it.id == workoutOnlySetId }.requiredForProgressionSnapshot)
        gym.setSetCompleted(set.id, completed = true, autoStartRest = false)
        gym.finishWorkout(completed)

        assertEquals(1, routines.days.first().single().progressionIndex)

        val removedRequired = routines.startRoutine(routineId)
        val requiredSet = gym.sets.first().single { candidate ->
            candidate.requiredForProgressionSnapshot && gym.workoutExercises.first().any { placement ->
                placement.sessionId == removedRequired && placement.id == candidate.workoutExerciseId
            }
        }
        gym.deleteSet(requiredSet.id)
        gym.finishWorkout(removedRequired)
        assertEquals(1, routines.days.first().single().progressionIndex)
    }

    @Test
    fun trainingMaxEligibilityCanBeRestoredWithoutResettingProgramPosition() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Program press"))
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Recoverable program",
                program = RoutineProgramDraft(
                    kind = RoutineProgramKind.Custom,
                    phaseCount = 2,
                    phaseLabels = listOf("Build", "Test"),
                ),
                days = listOf(
                    RoutineDayDraft(
                        "A",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = exerciseId,
                                plannedSets = listOf(
                                    WorkoutSetDraft(
                                        weight = 50.0,
                                        reps = 5,
                                        routinePhaseIndex = 0,
                                        workSection = RoutineWorkSection.Main,
                                    ),
                                    WorkoutSetDraft(
                                        weight = 55.0,
                                        reps = 5,
                                        routinePhaseIndex = 1,
                                        workSection = RoutineWorkSection.Main,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val incomplete = routines.startRoutine(routineId)
        gym.finishWorkout(incomplete)
        val held = routines.routines.first().single { it.id == routineId }
        assertEquals(1, held.currentProgramPhaseIndex)
        assertFalse(held.trainingMaxIncreaseEligible)

        routines.setRoutineTrainingMaxIncreaseEligible(routineId, true)

        val restored = routines.routines.first().single { it.id == routineId }
        assertEquals(1, restored.currentProgramPhaseIndex)
        assertEquals(1, restored.currentProgramCycle)
        assertTrue(restored.trainingMaxIncreaseEligible)
    }

    @Test
    fun activeRoutineWorkoutBlocksEveryProgramStateControl() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Program state guard"))
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Guarded program",
                program = RoutineProgramDraft(kind = RoutineProgramKind.Custom, phaseCount = 1),
                days = listOf(
                    RoutineDayDraft(
                        "A",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = exerciseId,
                                plannedSets = listOf(
                                    WorkoutSetDraft(weight = 50.0, reps = 5, workSection = RoutineWorkSection.Main),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val sessionId = routines.startRoutine(routineId)

        assertTrue(runCatching { routines.setRoutineProgramPosition(routineId, 0, 0, 2) }.isFailure)
        assertTrue(runCatching { routines.setRoutineTrainingMaxIncreaseEligible(routineId, false) }.isFailure)
        assertTrue(runCatching { routines.resetRoutineProgramProgress(routineId) }.isFailure)

        gym.discardWorkout(sessionId)
        routines.setRoutineProgramPosition(routineId, 0, 0, 2)
        assertEquals(2, routines.routines.first().single { it.id == routineId }.currentProgramCycle)
    }

    @Test
    fun unlinkingMachineExerciseMarksAffectedRoutineBindingForRepair() = runBlocking {
        val pressId = gym.createExercise(ExerciseDraft("Machine press"))
        val rowId = gym.createExercise(ExerciseDraft("Machine row"))
        val machineId = gym.createMachine(
            GymMachineDraft(
                exerciseId = pressId,
                name = "Combo machine",
                exerciseIds = setOf(pressId, rowId),
            ),
        )
        routines.createRoutine(
            RoutineDraft(
                name = "Press",
                days = listOf(
                    RoutineDayDraft(
                        "A",
                        listOf(RoutineExerciseDraft(exerciseId = pressId, machineId = machineId)),
                    ),
                ),
            ),
        )

        gym.updateMachine(
            machineId,
            GymMachineDraft(exerciseId = rowId, name = "Combo machine", exerciseIds = setOf(rowId)),
        )

        val placement = routines.exercises.first().single()
        assertEquals(null, placement.machineId)
        assertEquals(com.whip.app.domain.RoutineEquipmentBindingState.NeedsEquipment, placement.equipmentBindingState)
    }

    @Test
    fun editingProgramMetadataPreservesPositionAndMapsReorderedCurrentPhase() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Position-safe press", weightUnitId = "pound"))
        fun draft(
            labels: List<String>,
            roles: List<RoutineProgramPhaseRole>,
            currentPhaseHint: Int? = null,
            dayNames: List<String> = listOf("Press A", "Press B"),
            nextDayHint: Int? = null,
        ) = RoutineDraft(
            name = "Position-safe 5/3/1",
            program = RoutineProgramDraft(
                kind = RoutineProgramKind.FiveThreeOne,
                phaseCount = labels.size,
                phaseLabels = labels,
                phaseRoles = roles,
                trainingMaxAdvanceAfterPhaseIndices = setOf(labels.lastIndex),
                currentPhaseIndexHint = currentPhaseHint,
            ),
            days = dayNames.map { dayName ->
                RoutineDayDraft(
                    dayName,
                    listOf(
                        RoutineExerciseDraft(
                            exerciseId = exerciseId,
                            trainingMaxValue = 200.0,
                            trainingMaxUnitId = "pound",
                            cycleIncrementValue = 5.0,
                            trainingMaxSource = RoutineTrainingMaxSource.Explicit,
                            mainWorkScheme = RoutineMainWorkScheme.FivesPro,
                            assistanceRole = RoutineAssistanceRole.MainLift,
                            plannedSets = labels.indices.map { phase ->
                                WorkoutSetDraft(
                                    weightUnitId = "pound",
                                    reps = 5,
                                    loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                    loadPercentage = 65.0,
                                    routinePhaseIndex = phase,
                                    workSection = RoutineWorkSection.Main,
                                    mainWorkScheme = RoutineMainWorkScheme.FivesPro,
                                )
                            },
                        ),
                    ),
                )
            },
            nextProgramDayPositionHint = nextDayHint,
        )
        val initial = draft(
            listOf("Leader", "Anchor", "Deload"),
            listOf(RoutineProgramPhaseRole.Leader, RoutineProgramPhaseRole.Anchor, RoutineProgramPhaseRole.Deload),
        )
        val routineId = routines.createRoutine(initial)
        routines.setRoutineProgramPosition(routineId, phaseIndex = 1, dayPosition = 1, cycle = 3)

        routines.updateRoutine(
            routineId,
            draft(
                listOf("Recovery", "Build", "Anchor renamed"),
                listOf(RoutineProgramPhaseRole.Deload, RoutineProgramPhaseRole.Leader, RoutineProgramPhaseRole.Anchor),
                currentPhaseHint = 2,
                dayNames = listOf("Press B", "Press A"),
                nextDayHint = 0,
            ),
        )
        val reordered = routines.routines.first().single { it.id == routineId }
        assertEquals(2, reordered.currentProgramPhaseIndex)
        assertEquals(3, reordered.currentProgramCycle)
        assertEquals(0, reordered.nextProgramDayPosition)

        routines.updateRoutine(
            routineId,
            draft(
                listOf("Leader"),
                listOf(RoutineProgramPhaseRole.Leader),
                currentPhaseHint = 0,
                dayNames = listOf("Press B", "Press A"),
                nextDayHint = 0,
            ),
        )
        val shrunk = routines.routines.first().single { it.id == routineId }
        assertEquals(0, shrunk.currentProgramPhaseIndex)
        assertEquals(3, shrunk.currentProgramCycle)
    }

    @Test
    fun recordRebuildTracksAuditableImprovementsAndHistoricalEdits() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Bench"))
        val sessionId = gym.startWorkout()
        val workoutExerciseId = gym.addExerciseToWorkout(sessionId, exerciseId)
        val first = gym.addSet(workoutExerciseId, WorkoutSetDraft(weight = 80.0, reps = 5, completed = true))
        gym.addSet(workoutExerciseId, WorkoutSetDraft(weight = 90.0, reps = 5, completed = true))

        routines.rebuildPersonalRecords(exerciseId)
        val records = routines.personalRecords.first()
        assertTrue(records.any { it.type == PersonalRecordType.MaxWeight && it.value == 90.0 && it.current })
        assertTrue(records.count { it.type == PersonalRecordType.MaxWeight } >= 2)
        assertTrue(records.any {
            it.type == PersonalRecordType.MaxRepetitionsForWeight && it.value == 5.0 && it.secondaryValue == 90.0
        })
        assertTrue(records.any { it.type == PersonalRecordType.ExerciseWorkoutVolume && it.value == 850.0 && it.current })

        gym.updateSet(first, WorkoutSetDraft(weight = 100.0, reps = 5, completed = true))
        routines.rebuildPersonalRecords(exerciseId)
        assertTrue(routines.personalRecords.first().any {
            it.type == PersonalRecordType.MaxWeight && it.value == 100.0 && it.current
        })
        assertTrue(routines.personalRecords.first().any {
            it.type == PersonalRecordType.ExerciseWorkoutVolume && it.value == 950.0 && it.current
        })
    }

    @Test
    fun recordRebuildDeletesObsoleteRecordsAfterTheOnlyCompletedSetIsRemoved() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Bench"))
        val sessionId = gym.startWorkout()
        val placementId = gym.addExerciseToWorkout(sessionId, exerciseId)
        val setId = gym.addSet(
            placementId,
            WorkoutSetDraft(weight = 100.0, reps = 5, completed = true),
        )
        routines.rebuildPersonalRecords(exerciseId)
        assertTrue(routines.personalRecords.first().any { it.exerciseId == exerciseId })

        gym.deleteSet(setId)
        routines.rebuildPersonalRecords(exerciseId)

        assertFalse(routines.personalRecords.first().any { it.exerciseId == exerciseId })
    }

    @Test
    fun repBenchmarksPreserveEachLoadAfterSpecificTrackingTargetsWereRemoved() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Bench", weightUnitId = "pound"))
        val sessionId = gym.startWorkout()
        val workoutExerciseId = gym.addExerciseToWorkout(sessionId, exerciseId)
        gym.addSet(workoutExerciseId, WorkoutSetDraft(weight = 135.0, weightUnitId = "pound", reps = 20, completed = true))
        val expectedSetId = gym.addSet(
            workoutExerciseId,
            WorkoutSetDraft(weight = 225.0, weightUnitId = "pound", reps = 15, completed = true),
        )
        gym.addSet(workoutExerciseId, WorkoutSetDraft(weight = 250.0, weightUnitId = "pound", reps = 12, completed = true))

        routines.rebuildPersonalRecords(exerciseId)
        val records = routines.personalRecords.first().filter {
            it.exerciseId == exerciseId && it.type == PersonalRecordType.MaxRepetitionsForWeight
        }
        val benchmark = requireNotNull(records.firstOrNull {
            it.secondaryValue?.let { weight ->
                kotlin.math.abs(weight - massToKilograms(225.0, "pound")) < 0.000_001
            } == true
        })

        assertEquals(3, records.count { it.current })
        assertEquals(15.0, benchmark.value, 0.0)
        assertEquals(expectedSetId, benchmark.sourceSetId)
    }

    @Test
    fun routinePreservesMachineIdentityAndNumberedSetting() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Machine press"))
        val machineId = gym.createMachine(
            GymMachineDraft(
                exerciseId = exerciseId,
                name = "Home press",
                loadType = MachineLoadType.Level,
                levelLabel = "pin",
                availableLoads = (1..10).map(Int::toDouble),
            ),
        )
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Home push",
                days = listOf(
                    RoutineDayDraft(
                        "A",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = exerciseId,
                                machineId = machineId,
                                plannedSets = listOf(
                                    WorkoutSetDraft(machineLoadValue = 7.0, reps = 8),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        routines.startRoutine(routineId)

        assertEquals(machineId, gym.workoutExercises.first().single().machineId)
        assertEquals(MachineLoadType.Level, gym.workoutExercises.first().single().machineLoadTypeSnapshot)
        assertEquals(7.0, gym.sets.first().single().machineLoadValue!!, 0.0)
        assertEquals(null, gym.sets.first().single().canonicalWeightKg)
    }

    @Test
    fun duplicatePlacementsAndRepRangesSurviveTemplateAndWorkoutCreation() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Range bench"))
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Power and backoff",
                days = listOf(
                    RoutineDayDraft(
                        "A",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = exerciseId,
                                plannedSets = listOf(WorkoutSetDraft(weight = 100.0, reps = 3, repsMax = 5)),
                            ),
                            RoutineExerciseDraft(
                                exerciseId = exerciseId,
                                plannedSets = listOf(WorkoutSetDraft(weight = 80.0, reps = 8, repsMax = 10)),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(listOf(3 to 5, 8 to 10), routines.sets.first().map { it.draft.reps to it.draft.repsMax })

        routines.startRoutine(routineId)
        assertEquals(2, gym.workoutExercises.first().size)
        assertEquals(listOf(5, 10), gym.sets.first().map { it.prescribedRepetitionsMax })
    }

    @Test
    fun routineWaveAndReusableAlternativesSnapshotIntoEachStartedWorkout() = runBlocking {
        val primary = gym.createExercise(ExerciseDraft("Primary press", weightIncrement = 2.5))
        val alternative = gym.createExercise(ExerciseDraft("Alternative press"))
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Wave",
                days = listOf(
                    RoutineDayDraft(
                        "A",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = primary,
                                plannedSets = listOf(WorkoutSetDraft(weight = 100.0, reps = 5)),
                                progressionPercentages = listOf(100.0, 90.0),
                                alternativeExerciseIds = listOf(alternative),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val firstSession = routines.startRoutine(routineId)
        assertEquals(100.0, gym.sets.first().single().enteredWeight!!, 0.0)
        assertEquals("Exact load", gym.sets.first().single().prescriptionSourceLabel)
        assertEquals(listOf(alternative), gym.workoutExercises.first().single().alternativeExerciseIdsSnapshot)
        gym.setSetCompleted(gym.sets.first().single().id, completed = true, autoStartRest = false)
        gym.finishWorkout(firstSession)

        routines.startRoutine(routineId)
        val latestPlacement = gym.workoutExercises.first().maxBy { it.id }
        val latestSet = gym.sets.first().single { it.workoutExerciseId == latestPlacement.id }
        assertEquals(90.0, latestSet.enteredWeight!!, 0.0)
        assertEquals("Exact load · cycle 90.0%", latestSet.prescriptionSourceLabel)
        assertEquals(listOf(alternative), latestPlacement.alternativeExerciseIdsSnapshot)
    }

    @Test
    fun programmedRoutineAdvancesSafelyAcrossDaysPhasesAndTrainingMaxCycles() = runBlocking {
        val pressId = gym.createExercise(ExerciseDraft("Press", weightUnitId = "pound", weightIncrement = 5.0))
        val squatId = gym.createExercise(ExerciseDraft("Squat", weightUnitId = "pound", weightIncrement = 5.0))
        fun programmedExercise(exerciseId: Long, trainingMax: Double, increment: Double) = RoutineExerciseDraft(
            exerciseId = exerciseId,
            trainingMaxValue = trainingMax,
            trainingMaxUnitId = "pound",
            cycleIncrementValue = increment,
            trainingMaxSource = RoutineTrainingMaxSource.Explicit,
            plannedSets = listOf(
                WorkoutSetDraft(
                    weightUnitId = "pound",
                    reps = 5,
                    loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                    loadPercentage = 65.0,
                    routinePhaseIndex = 0,
                    workSection = RoutineWorkSection.Main,
                ),
                WorkoutSetDraft(
                    weightUnitId = "pound",
                    reps = 3,
                    loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                    loadPercentage = 70.0,
                    routinePhaseIndex = 1,
                    workSection = RoutineWorkSection.Main,
                ),
            ),
        )
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Two-phase strength program",
                program = RoutineProgramDraft(
                    kind = RoutineProgramKind.FiveThreeOneClassic,
                    phaseCount = 2,
                    phaseLabels = listOf("Fives", "Threes"),
                    trainingMaxAdvanceAfterPhaseIndices = setOf(1),
                ),
                days = listOf(
                    RoutineDayDraft("Press", listOf(programmedExercise(pressId, 200.0, 5.0))),
                    RoutineDayDraft("Squat", listOf(programmedExercise(squatId, 300.0, 10.0))),
                ),
            ),
        )
        val days = routines.days.first().filter { it.routineId == routineId }.sortedBy { it.position }

        suspend fun assertStartedLoad(sessionId: Long, expected: Double, expectedPhase: Int, expectedCycle: Int) {
            val session = gym.sessions.first().single { it.id == sessionId }
            val placement = gym.workoutExercises.first().single { it.sessionId == sessionId }
            val set = gym.sets.first().single { it.workoutExerciseId == placement.id }
            assertEquals(expectedPhase, session.sourceRoutinePhaseIndex)
            assertEquals(expectedCycle, session.sourceRoutineCycle)
            assertEquals(expected, set.enteredWeight!!, 0.0)
        }

        suspend fun completeRequiredMainWork(sessionId: Long) {
            val placementIds = gym.workoutExercises.first()
                .filter { it.sessionId == sessionId }
                .mapTo(mutableSetOf()) { it.id }
            gym.sets.first()
                .filter { it.workoutExerciseId in placementIds && it.workSectionSnapshot == RoutineWorkSection.Main }
                .forEach { set -> gym.setSetCompleted(set.id, completed = true, autoStartRest = false) }
        }

        val firstPress = routines.startRoutine(routineId)
        assertStartedLoad(firstPress, expected = 130.0, expectedPhase = 0, expectedCycle = 1)
        assertEquals(200.0, gym.workoutExercises.first().single { it.sessionId == firstPress }.trainingMaxValueSnapshot!!, 0.0)
        completeRequiredMainWork(firstPress)
        gym.finishWorkout(firstPress)
        assertEquals(1, routines.routines.first().single { it.id == routineId }.nextProgramDayPosition)

        val unscheduledRepeat = routines.startRoutine(routineId, days.first().id)
        gym.finishWorkout(unscheduledRepeat)
        assertFalse(gym.sessions.first().single { it.id == unscheduledRepeat }.programProgressAdvanced)
        assertEquals(1, routines.routines.first().single { it.id == routineId }.nextProgramDayPosition)

        val discardedSquat = routines.startRoutine(routineId)
        assertStartedLoad(discardedSquat, expected = 195.0, expectedPhase = 0, expectedCycle = 1)
        gym.discardWorkout(discardedSquat)
        assertEquals(1, routines.routines.first().single { it.id == routineId }.nextProgramDayPosition)

        val firstSquat = routines.startRoutine(routineId)
        completeRequiredMainWork(firstSquat)
        gym.finishWorkout(firstSquat)
        val threesStart = routines.routines.first().single { it.id == routineId }
        assertEquals(1, threesStart.currentProgramPhaseIndex)
        assertEquals(0, threesStart.nextProgramDayPosition)

        val threesPress = routines.startRoutine(routineId)
        assertStartedLoad(threesPress, expected = 140.0, expectedPhase = 1, expectedCycle = 1)
        completeRequiredMainWork(threesPress)
        gym.finishWorkout(threesPress)
        val threesSquat = routines.startRoutine(routineId)
        assertStartedLoad(threesSquat, expected = 210.0, expectedPhase = 1, expectedCycle = 1)
        completeRequiredMainWork(threesSquat)
        gym.finishWorkout(threesSquat)

        val nextCycle = routines.routines.first().single { it.id == routineId }
        assertEquals(0, nextCycle.currentProgramPhaseIndex)
        assertEquals(2, nextCycle.currentProgramCycle)
        assertEquals(0, nextCycle.nextProgramDayPosition)
        assertEquals(
            listOf(205.0, 310.0),
            routines.exercises.first()
                .filter { exercise -> days.any { it.id == exercise.routineDayId } }
                .sortedBy { it.routineDayId }
                .map { it.trainingMaxValue },
        )

        val secondCyclePress = routines.startRoutine(routineId)
        assertStartedLoad(secondCyclePress, expected = 135.0, expectedPhase = 0, expectedCycle = 2)
        assertEquals(
            205.0,
            gym.workoutExercises.first().single { it.sessionId == secondCyclePress }.trainingMaxValueSnapshot!!,
            0.0,
        )
    }

    @Test
    fun incompleteRequiredMainWorkAdvancesScheduleButHoldsTrainingMaxAtBoundary() = runBlocking {
        val pressId = gym.createExercise(ExerciseDraft("Press", weightUnitId = "pound", weightIncrement = 5.0))
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Safe 5/3/1 progression",
                program = RoutineProgramDraft(
                    kind = RoutineProgramKind.FiveThreeOne,
                    phaseCount = 1,
                    phaseLabels = listOf("5s Week"),
                    trainingMaxAdvanceAfterPhaseIndices = setOf(0),
                ),
                days = listOf(
                    RoutineDayDraft(
                        "Press",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = pressId,
                                trainingMaxValue = 200.0,
                                trainingMaxUnitId = "pound",
                                cycleIncrementValue = 5.0,
                                trainingMaxSource = RoutineTrainingMaxSource.Explicit,
                                mainWorkScheme = RoutineMainWorkScheme.ClassicPrSet,
                                plannedSets = listOf(
                                    WorkoutSetDraft(
                                        weightUnitId = "pound",
                                        reps = 5,
                                        classification = WorkoutSetClassification.Amrap,
                                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                        loadPercentage = 65.0,
                                        workSection = RoutineWorkSection.Main,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val incompleteSession = routines.startRoutine(routineId)
        val incompletePlacement = gym.workoutExercises.first().single { it.sessionId == incompleteSession }
        assertEquals(RoutineMainWorkScheme.ClassicPrSet, incompletePlacement.mainWorkSchemeSnapshot)
        assertEquals(RoutineWorkSection.Main, gym.sets.first().single().workSectionSnapshot)
        gym.finishWorkout(incompleteSession)

        assertEquals(2, routines.routines.first().single { it.id == routineId }.currentProgramCycle)
        assertEquals(200.0, routines.exercises.first().single().trainingMaxValue!!, 0.0)

        val completedSession = routines.startRoutine(routineId)
        val completedSet = gym.sets.first().single { set ->
            gym.workoutExercises.first().any { it.sessionId == completedSession && it.id == set.workoutExerciseId }
        }
        gym.setSetCompleted(completedSet.id, completed = true, autoStartRest = false)
        gym.finishWorkout(completedSession)

        assertEquals(3, routines.routines.first().single { it.id == routineId }.currentProgramCycle)
        assertEquals(205.0, routines.exercises.first().single().trainingMaxValue!!, 0.0)

        gym.finishWorkout(completedSession)
        assertEquals(3, routines.routines.first().single { it.id == routineId }.currentProgramCycle)
        assertEquals(205.0, routines.exercises.first().single().trainingMaxValue!!, 0.0)
    }

    @Test
    fun removingOrSubstitutingOneOfSeveralMainLiftsInvalidatesOnlyThatSessionTrainingMaxAdvance() = runBlocking {
        val benchId = gym.createExercise(ExerciseDraft("Bench Press", weightUnitId = "pound"))
        val squatId = gym.createExercise(ExerciseDraft("Squat", weightUnitId = "pound"))
        val rowId = gym.createExercise(ExerciseDraft("Row", weightUnitId = "pound"))
        val zercherId = gym.createExercise(ExerciseDraft("Zercher Squat", weightUnitId = "pound"))
        fun mainLift(exerciseId: Long, trainingMax: Double, increment: Double) = RoutineExerciseDraft(
            exerciseId = exerciseId,
            trainingMaxValue = trainingMax,
            trainingMaxUnitId = "pound",
            cycleIncrementValue = increment,
            trainingMaxSource = RoutineTrainingMaxSource.Explicit,
            mainWorkScheme = RoutineMainWorkScheme.FivesPro,
            assistanceRole = RoutineAssistanceRole.MainLift,
            plannedSets = listOf(
                WorkoutSetDraft(
                    weightUnitId = "pound",
                    reps = 5,
                    loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                    loadPercentage = 65.0,
                    workSection = RoutineWorkSection.Main,
                ),
            ),
        )
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Multi-main safety",
                program = RoutineProgramDraft(
                    kind = RoutineProgramKind.FiveThreeOne,
                    phaseCount = 1,
                    trainingMaxAdvanceAfterPhaseIndices = setOf(0),
                ),
                days = listOf(
                    RoutineDayDraft(
                        "Bench + Squat",
                        listOf(
                            mainLift(benchId, trainingMax = 200.0, increment = 5.0),
                            mainLift(squatId, trainingMax = 300.0, increment = 10.0),
                            RoutineExerciseDraft(
                                exerciseId = rowId,
                                // Simulates a former Main placement reclassified as assistance.
                                // Hidden legacy TM fields must not progress at a boundary.
                                trainingMaxValue = 100.0,
                                trainingMaxUnitId = "pound",
                                cycleIncrementValue = 20.0,
                                assistanceRole = RoutineAssistanceRole.Pull,
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

        suspend fun placementFor(sessionId: Long, exerciseId: Long) = gym.workoutExercises.first()
            .single { it.sessionId == sessionId && it.exerciseId == exerciseId }
        suspend fun completeRemainingMain(sessionId: Long) {
            val placementIds = gym.workoutExercises.first()
                .filter { it.sessionId == sessionId }
                .mapTo(mutableSetOf()) { it.id }
            gym.sets.first()
                .filter {
                    it.workoutExerciseId in placementIds &&
                        it.workSectionSnapshot == RoutineWorkSection.Main && it.deletedAtMillis == null
                }
                .forEach { gym.setSetCompleted(it.id, completed = true, autoStartRest = false) }
        }
        suspend fun assertTrainingMaxes(bench: Double, squat: Double, row: Double) {
            val byExercise = routines.exercises.first().associateBy { it.exerciseId }
            assertEquals(bench, requireNotNull(byExercise[benchId]?.trainingMaxValue), 0.0)
            assertEquals(squat, requireNotNull(byExercise[squatId]?.trainingMaxValue), 0.0)
            assertEquals(row, requireNotNull(byExercise[rowId]?.trainingMaxValue), 0.0)
        }

        val removedSession = routines.startRoutine(routineId)
        gym.removeWorkoutExercise(placementFor(removedSession, squatId).id)
        assertEquals(
            setOf(squatId),
            gym.sessions.first().single { it.id == removedSession }.invalidatedMainExerciseIds,
        )
        completeRemainingMain(removedSession)
        gym.finishWorkout(removedSession)
        assertEquals(2, routines.routines.first().single { it.id == routineId }.currentProgramCycle)
        assertTrainingMaxes(bench = 205.0, squat = 300.0, row = 100.0)
        val removedSessionUuid = gym.sessions.first().single { it.id == removedSession }.uuid
        val firstBoundaryAudit = routines.trainingMaxDecisions.first().filter { it.sessionUuid == removedSessionUuid }
        assertEquals(2, firstBoundaryAudit.size)
        assertEquals(
            TrainingMaxDecisionAction.UseStandard,
            firstBoundaryAudit.single { it.exerciseName == "Bench Press" }.action,
        )
        assertEquals(
            TrainingMaxDecisionAction.Hold,
            firstBoundaryAudit.single { it.exerciseName == "Squat" }.action,
        )

        val substitutedSession = routines.startRoutine(routineId)
        gym.substituteWorkoutExercise(placementFor(substitutedSession, squatId).id, zercherId)
        assertEquals(
            setOf(squatId),
            gym.sessions.first().single { it.id == substitutedSession }.invalidatedMainExerciseIds,
        )
        completeRemainingMain(substitutedSession)
        gym.finishWorkout(substitutedSession)
        assertEquals(3, routines.routines.first().single { it.id == routineId }.currentProgramCycle)
        assertTrainingMaxes(bench = 210.0, squat = 300.0, row = 100.0)

        val intactSession = routines.startRoutine(routineId)
        val addedSetId = gym.addSet(placementFor(intactSession, benchId).id, draft = null)
        val addedSet = gym.sets.first().single { it.id == addedSetId }
        assertFalse(addedSet.planned)
        assertEquals(RoutineWorkSection.Optional, addedSet.workSectionSnapshot)
        completeRemainingMain(intactSession)
        gym.finishWorkout(intactSession)
        assertEquals(4, routines.routines.first().single { it.id == routineId }.currentProgramCycle)
        assertTrainingMaxes(bench = 215.0, squat = 310.0, row = 100.0)
    }

    @Test
    fun performanceReviewNeverChangesTrainingMaxWithoutDecisionAndAuditsAcceptedChoice() = runBlocking {
        val pressId = gym.createExercise(ExerciseDraft("Press", weightUnitId = "pound"))
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Reviewed 5/3/1",
                program = RoutineProgramDraft(
                    kind = RoutineProgramKind.FiveThreeOne,
                    phaseCount = 1,
                    trainingMaxAdvanceAfterPhaseIndices = setOf(0),
                    progressionMode = RoutineProgressionMode.PerformanceInformed,
                    allowNonStandardHigherSuggestions = true,
                ),
                days = listOf(
                    RoutineDayDraft(
                        "Press",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = pressId,
                                trainingMaxValue = 200.0,
                                trainingMaxUnitId = "pound",
                                cycleIncrementValue = 5.0,
                                trainingMaxSource = RoutineTrainingMaxSource.Explicit,
                                placementKind = RoutinePlacementKind.MainLift,
                                plannedSets = listOf(
                                    WorkoutSetDraft(
                                        weightUnitId = "pound",
                                        reps = 5,
                                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                        loadPercentage = 85.0,
                                        workSection = RoutineWorkSection.Main,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        suspend fun finishCurrent(
            decision: TrainingMaxCycleDecision? = null,
            markFailure: Boolean = false,
        ) {
            val sessionId = routines.startRoutine(routineId)
            val placementId = gym.workoutExercises.first().single { it.sessionId == sessionId }.id
            gym.sets.first().filter { it.workoutExerciseId == placementId }.forEach { set ->
                if (markFailure) {
                    gym.updateSet(
                        set.id,
                        WorkoutSetDraft(
                            weight = set.enteredWeight,
                            weightUnitId = set.enteredWeightUnitId ?: "pound",
                            reps = set.repetitions,
                            completed = true,
                            classification = WorkoutSetClassification.Failure,
                        ),
                    )
                } else {
                    gym.setSetCompleted(set.id, completed = true, autoStartRest = false)
                }
            }
            gym.finishWorkout(sessionId, decision?.let(::listOf))
        }

        finishCurrent()
        assertEquals(200.0, routines.exercises.first().single().trainingMaxValue!!, 0.0)
        assertTrue(routines.trainingMaxDecisions.first().isEmpty())

        finishCurrent(
            TrainingMaxCycleDecision(
                exerciseId = pressId,
                expectedCurrentTrainingMax = 200.0,
                requestedDelta = 5.0,
                standardDelta = 5.0,
                recommendationCategory = "forged metadata is ignored",
                recommendationDelta = 999.0,
                confidence = 0.0,
                reasons = listOf("caller text must not become history"),
                engineVersion = "five-three-one-progression/1",
                action = TrainingMaxDecisionAction.UseStandard,
            ),
        )

        assertEquals(205.0, routines.exercises.first().single().trainingMaxValue!!, 0.0)
        val audit = routines.trainingMaxDecisions.first().single()
        assertEquals(200.0, audit.previousTrainingMax, 0.0)
        assertEquals(5.0, audit.appliedDelta, 0.0)
        assertEquals(205.0, audit.resultingTrainingMax, 0.0)
        assertEquals(TrainingMaxDecisionAction.UseStandard, audit.action)
        assertFalse(audit.reasons.any { it.contains("caller text") })

        finishCurrent(
            decision = TrainingMaxCycleDecision(
                exerciseId = pressId,
                expectedCurrentTrainingMax = 205.0,
                requestedDelta = -5.0,
                standardDelta = 5.0,
                recommendationCategory = "DecreaseReview",
                recommendationDelta = -5.0,
                confidence = 0.95,
                reasons = listOf("Repeated required-work failures"),
                engineVersion = "five-three-one-progression/1",
                action = TrainingMaxDecisionAction.Custom,
            ),
            markFailure = true,
        )

        assertEquals(200.0, routines.exercises.first().single().trainingMaxValue!!, 0.0)
        val decreaseAudit = routines.trainingMaxDecisions.first().maxBy { it.createdAtMillis }
        assertEquals(-5.0, decreaseAudit.appliedDelta, 0.0)
        assertEquals(200.0, decreaseAudit.resultingTrainingMax, 0.0)
        assertEquals(TrainingMaxDecisionAction.Custom, decreaseAudit.action)

        finishCurrent(
            TrainingMaxCycleDecision(
                exerciseId = pressId,
                expectedCurrentTrainingMax = 200.0,
                requestedDelta = 0.0,
                standardDelta = 5.0,
                recommendationCategory = "forged metadata is ignored",
                recommendationDelta = 999.0,
                confidence = 0.0,
                reasons = listOf("caller text must not become history"),
                engineVersion = "forged",
                action = TrainingMaxDecisionAction.IgnoreRecommendation,
            ),
        )

        assertEquals(200.0, routines.exercises.first().single().trainingMaxValue!!, 0.0)
        val ignoreAudit = routines.trainingMaxDecisions.first().single {
            it.action == TrainingMaxDecisionAction.IgnoreRecommendation
        }
        assertEquals(0.0, ignoreAudit.appliedDelta, 0.0)
        assertTrue(ignoreAudit.reasons.any { it.contains("declined Whip's advisory recommendation") })
        assertFalse(ignoreAudit.reasons.any { it.contains("caller text") })
    }

    @Test
    fun performanceReviewRejectsStaleForgedAndActionMismatchedDecisions() = runBlocking {
        val liftId = gym.createExercise(ExerciseDraft("Decision-boundary Press", weightUnitId = "pound"))
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Decision boundary",
                program = RoutineProgramDraft(
                    kind = RoutineProgramKind.FiveThreeOne,
                    phaseCount = 1,
                    trainingMaxAdvanceAfterPhaseIndices = setOf(0),
                    progressionMode = RoutineProgressionMode.PerformanceInformed,
                    allowNonStandardHigherSuggestions = false,
                ),
                days = listOf(
                    RoutineDayDraft(
                        "Press",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = liftId,
                                trainingMaxValue = 200.0,
                                trainingMaxUnitId = "pound",
                                cycleIncrementValue = 5.0,
                                trainingMaxSource = RoutineTrainingMaxSource.Explicit,
                                placementKind = RoutinePlacementKind.MainLift,
                                plannedSets = listOf(
                                    WorkoutSetDraft(
                                        weightUnitId = "pound",
                                        reps = 5,
                                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                        loadPercentage = 85.0,
                                        workSection = RoutineWorkSection.Main,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val sessionId = routines.startRoutine(routineId)
        val placementId = gym.workoutExercises.first().single { it.sessionId == sessionId }.id
        gym.sets.first().filter { it.workoutExerciseId == placementId }.forEach { set ->
            gym.setSetCompleted(set.id, completed = true, autoStartRest = false)
        }
        fun decision(
            action: TrainingMaxDecisionAction,
            delta: Double,
            expectedCurrent: Double = 200.0,
        ) = TrainingMaxCycleDecision(
            exerciseId = liftId,
            expectedCurrentTrainingMax = expectedCurrent,
            requestedDelta = delta,
            standardDelta = 5.0,
            recommendationCategory = "CautiousHigherIncrease",
            recommendationDelta = delta,
            confidence = 1.0,
            reasons = listOf("forged caller rationale"),
            engineVersion = "forged",
            action = action,
        )
        suspend fun assertRejected(candidate: TrainingMaxCycleDecision) {
            val failure = runCatching { gym.finishWorkout(sessionId, listOf(candidate)) }.exceptionOrNull()
            assertTrue(failure is IllegalArgumentException)
            assertEquals(200.0, routines.exercises.first().single { it.exerciseId == liftId }.trainingMaxValue!!, 0.0)
            assertTrue(routines.trainingMaxDecisions.first().isEmpty())
        }

        assertRejected(decision(TrainingMaxDecisionAction.UseSuggestion, 7.5))
        assertRejected(decision(TrainingMaxDecisionAction.Hold, 1.0))
        assertRejected(decision(TrainingMaxDecisionAction.IgnoreRecommendation, 1.0))
        assertRejected(decision(TrainingMaxDecisionAction.UseStandard, 5.0, expectedCurrent = 195.0))
        assertRejected(decision(TrainingMaxDecisionAction.Custom, 10.0))

        gym.finishWorkout(
            sessionId,
            listOf(decision(TrainingMaxDecisionAction.UseStandard, 5.0)),
        )
        assertEquals(205.0, routines.exercises.first().single { it.exerciseId == liftId }.trainingMaxValue!!, 0.0)
        val audit = routines.trainingMaxDecisions.first().single()
        assertEquals("StandardIncrease", audit.recommendationCategory)
        assertFalse(audit.reasons.any { it.contains("forged") })
        assertEquals("five-three-one-progression/1", audit.engineVersion)
    }

    @Test
    fun performanceReviewEvidenceRequiresMatchingSourceProgramKindAtCommit() = runBlocking {
        val liftId = gym.createExercise(ExerciseDraft("Program-kind Press", weightUnitId = "pound"))
        fun programDay(name: String) = RoutineDayDraft(
            name,
            listOf(
                RoutineExerciseDraft(
                    exerciseId = liftId,
                    trainingMaxValue = 200.0,
                    trainingMaxUnitId = "pound",
                    cycleIncrementValue = 5.0,
                    trainingMaxSource = RoutineTrainingMaxSource.Explicit,
                    placementKind = RoutinePlacementKind.MainLift,
                    plannedSets = buildList {
                        add(
                            WorkoutSetDraft(
                                weightUnitId = "pound",
                                reps = 5,
                                classification = WorkoutSetClassification.Amrap,
                                loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                loadPercentage = 85.0,
                                workSection = RoutineWorkSection.Main,
                            ),
                        )
                        add(
                            WorkoutSetDraft(
                                weightUnitId = "pound",
                                reps = 3,
                                loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                loadPercentage = 100.0,
                                workSection = RoutineWorkSection.Optional,
                                optionalWorkKind = RoutineOptionalWorkKind.Joker,
                            ),
                        )
                    },
                ),
            ),
        )
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Program-kind evidence boundary",
                program = RoutineProgramDraft(
                    kind = RoutineProgramKind.FiveThreeOne,
                    phaseCount = 1,
                    trainingMaxAdvanceAfterPhaseIndices = setOf(0),
                    progressionMode = RoutineProgressionMode.PerformanceInformed,
                    allowNonStandardHigherSuggestions = true,
                ),
                days = listOf(programDay("Earlier Press"), programDay("Boundary Press")),
            ),
        )

        suspend fun completeStrongSession(sessionId: Long) {
            gym.sets.first().filter { set ->
                gym.workoutExercises.first().single { it.id == set.workoutExerciseId }.sessionId == sessionId
            }.forEach { set ->
                gym.updateSet(
                    set.id,
                    WorkoutSetDraft(
                        weight = set.enteredWeight,
                        weightUnitId = set.enteredWeightUnitId ?: "pound",
                        reps = if (set.workSectionSnapshot == RoutineWorkSection.Main) 10 else 3,
                        completed = true,
                        classification = if (set.workSectionSnapshot == RoutineWorkSection.Main) {
                            WorkoutSetClassification.Amrap
                        } else {
                            WorkoutSetClassification.Working
                        },
                        rir = 3.0,
                    ),
                )
            }
        }

        val earlierSessionId = routines.startRoutine(routineId)
        completeStrongSession(earlierSessionId)
        gym.finishWorkout(earlierSessionId)
        val storedEarlier = requireNotNull(database.gymDao().getSession(earlierSessionId))
        database.gymDao().updateSession(
            storedEarlier.copy(sourceRoutineProgramKind = RoutineProgramKind.FiveSPro.name),
        )

        val boundarySessionId = routines.startRoutine(routineId)
        completeStrongSession(boundarySessionId)
        gym.finishWorkout(
            boundarySessionId,
            listOf(
                TrainingMaxCycleDecision(
                    exerciseId = liftId,
                    expectedCurrentTrainingMax = 200.0,
                    requestedDelta = 5.0,
                    standardDelta = 5.0,
                    recommendationCategory = "caller metadata is ignored",
                    recommendationDelta = 5.0,
                    confidence = 0.0,
                    reasons = emptyList(),
                    engineVersion = "caller",
                    action = TrainingMaxDecisionAction.UseSuggestion,
                ),
            ),
        )

        assertEquals(205.0, routines.exercises.first().first().trainingMaxValue!!, 0.0)
        val audit = routines.trainingMaxDecisions.first().single()
        assertEquals("StandardIncrease", audit.recommendationCategory)
        assertEquals(5.0, audit.recommendationDelta, 0.0)
    }

    @Test
    fun trainingMaxTestSetRequiresExplicitTestPhaseOneHundredPercentAndThreeToFiveReps() = runBlocking {
        val liftId = gym.createExercise(ExerciseDraft("TM Test Press", weightUnitId = "pound"))
        fun draft(
            phaseRole: RoutineProgramPhaseRole,
            percentage: Double,
            reps: Int,
            testSetCount: Int = 1,
        ) = RoutineDraft(
            name = "TM test protocol",
            program = RoutineProgramDraft(
                kind = RoutineProgramKind.FiveThreeOne,
                phaseCount = 1,
                phaseRoles = listOf(phaseRole),
            ),
            days = listOf(
                RoutineDayDraft(
                    "Test",
                    listOf(
                        RoutineExerciseDraft(
                            exerciseId = liftId,
                            trainingMaxValue = 200.0,
                            trainingMaxUnitId = "pound",
                            cycleIncrementValue = 5.0,
                            placementKind = RoutinePlacementKind.MainLift,
                            plannedSets = List(testSetCount) {
                                WorkoutSetDraft(
                                    weightUnitId = "pound",
                                    reps = reps,
                                    classification = WorkoutSetClassification.TrainingMaxTest,
                                    loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                    loadPercentage = percentage,
                                    routinePhaseIndex = 0,
                                    workSection = RoutineWorkSection.Main,
                                )
                            },
                        ),
                    ),
                ),
            ),
        )

        assertTrue(runCatching {
            routines.createRoutine(draft(RoutineProgramPhaseRole.Standard, 100.0, 3))
        }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching {
            routines.createRoutine(draft(RoutineProgramPhaseRole.TrainingMaxTest, 95.0, 3))
        }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching {
            routines.createRoutine(draft(RoutineProgramPhaseRole.TrainingMaxTest, 100.0, 1))
        }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching {
            routines.createRoutine(
                draft(RoutineProgramPhaseRole.TrainingMaxTest, 100.0, 3, testSetCount = 0),
            )
        }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching {
            routines.createRoutine(
                draft(RoutineProgramPhaseRole.TrainingMaxTest, 100.0, 3, testSetCount = 2),
            )
        }.exceptionOrNull() is IllegalArgumentException)

        val validId = routines.createRoutine(
            draft(RoutineProgramPhaseRole.TrainingMaxTest, 100.0, 3),
        )
        assertTrue(validId > 0L)
    }

    @Test
    fun repeatedMainLiftHasExactlyOneTrainingMaxTestAcrossThePhase() = runBlocking {
        val liftId = gym.createExercise(ExerciseDraft("Twice-weekly test squat", weightUnitId = "pound"))
        fun placement(includeTest: Boolean) = RoutineExerciseDraft(
            exerciseId = liftId,
            trainingMaxValue = 300.0,
            trainingMaxUnitId = "pound",
            cycleIncrementValue = 10.0,
            placementKind = RoutinePlacementKind.MainLift,
            plannedSets = buildList {
                add(
                    WorkoutSetDraft(
                        weightUnitId = "pound",
                        reps = 5,
                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                        loadPercentage = 70.0,
                        routinePhaseIndex = 0,
                        workSection = RoutineWorkSection.Main,
                    ),
                )
                if (includeTest) add(
                    WorkoutSetDraft(
                        weightUnitId = "pound",
                        reps = 3,
                        classification = WorkoutSetClassification.TrainingMaxTest,
                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                        loadPercentage = 100.0,
                        routinePhaseIndex = 0,
                        workSection = RoutineWorkSection.Main,
                    ),
                )
            },
        )
        fun draft(secondTest: Boolean) = RoutineDraft(
            name = "One TM test per lift",
            program = RoutineProgramDraft(
                kind = RoutineProgramKind.FiveThreeOne,
                phaseCount = 1,
                phaseRoles = listOf(RoutineProgramPhaseRole.TrainingMaxTest),
            ),
            days = listOf(
                RoutineDayDraft("First squat day", listOf(placement(includeTest = true))),
                RoutineDayDraft("Second squat day", listOf(placement(includeTest = secondTest))),
            ),
        )

        assertTrue(routines.createRoutine(draft(secondTest = false)) > 0L)
        val duplicateFailure = runCatching {
            routines.createRoutine(draft(secondTest = true).copy(name = "Duplicate TM test"))
        }.exceptionOrNull()
        assertTrue(requireNotNull(duplicateFailure).message.orEmpty().contains("exactly one explicit"))
    }

    @Test
    fun failedPersistedTrainingMaxTestRetainsPrescriptionIdentityAndSuggestsDecrease() = runBlocking {
        val liftId = gym.createExercise(ExerciseDraft("Failed TM test press", weightUnitId = "pound"))
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Failed TM test",
                program = RoutineProgramDraft(
                    kind = RoutineProgramKind.FiveThreeOne,
                    phaseCount = 1,
                    phaseRoles = listOf(RoutineProgramPhaseRole.TrainingMaxTest),
                    trainingMaxAdvanceAfterPhaseIndices = setOf(0),
                    progressionMode = RoutineProgressionMode.PerformanceInformed,
                ),
                days = listOf(
                    RoutineDayDraft(
                        "Press test",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = liftId,
                                trainingMaxValue = 200.0,
                                trainingMaxUnitId = "pound",
                                cycleIncrementValue = 5.0,
                                placementKind = RoutinePlacementKind.MainLift,
                                plannedSets = listOf(
                                    WorkoutSetDraft(
                                        weightUnitId = "pound",
                                        reps = 3,
                                        classification = WorkoutSetClassification.TrainingMaxTest,
                                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                        loadPercentage = 100.0,
                                        routinePhaseIndex = 0,
                                        workSection = RoutineWorkSection.Main,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val sessionId = routines.startRoutine(routineId)
        val prescribed = gym.sets.first().single()
        assertEquals(WorkoutSetClassification.TrainingMaxTest, prescribed.classification)
        assertEquals(WorkoutSetClassification.TrainingMaxTest, prescribed.prescribedClassificationSnapshot)

        gym.updateSet(
            prescribed.id,
            WorkoutSetDraft(
                weight = prescribed.enteredWeight,
                weightUnitId = prescribed.enteredWeightUnitId ?: "pound",
                reps = 2,
                completed = true,
                classification = WorkoutSetClassification.Failure,
            ),
        )
        val failed = gym.sets.first().single()
        assertEquals(WorkoutSetClassification.Failure, failed.classification)
        assertEquals(WorkoutSetClassification.TrainingMaxTest, failed.prescribedClassificationSnapshot)

        gym.finishWorkout(
            sessionId,
            listOf(
                TrainingMaxCycleDecision(
                    exerciseId = liftId,
                    expectedCurrentTrainingMax = 200.0,
                    requestedDelta = -5.0,
                    standardDelta = 5.0,
                    recommendationCategory = "caller metadata is ignored",
                    recommendationDelta = -5.0,
                    confidence = 0.0,
                    reasons = emptyList(),
                    engineVersion = "caller",
                    action = TrainingMaxDecisionAction.UseSuggestion,
                ),
            ),
        )

        assertEquals(195.0, routines.exercises.first().single().trainingMaxValue!!, 0.0)
        val audit = routines.trainingMaxDecisions.first().single()
        assertEquals("DecreaseReview", audit.recommendationCategory)
        assertEquals(-5.0, audit.recommendationDelta, 0.0)
        assertTrue(audit.reasons.any { it.contains("Training Max test failed") })
    }

    @Test
    fun manualProgramTrainingMaxEditCreatesVisibleAuditEventWithoutRewritingWorkouts() = runBlocking {
        val liftId = gym.createExercise(ExerciseDraft("Audited Bench", weightUnitId = "pound"))
        fun draft(trainingMax: Double) = RoutineDraft(
            name = "Audited program",
            program = RoutineProgramDraft(
                kind = RoutineProgramKind.FiveThreeOne,
                phaseCount = 1,
            ),
            days = listOf(
                RoutineDayDraft(
                    "Bench",
                    listOf(
                        RoutineExerciseDraft(
                            exerciseId = liftId,
                            trainingMaxValue = trainingMax,
                            trainingMaxUnitId = "pound",
                            cycleIncrementValue = 5.0,
                            placementKind = RoutinePlacementKind.MainLift,
                            plannedSets = listOf(
                                WorkoutSetDraft(
                                    weightUnitId = "pound",
                                    reps = 5,
                                    loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                    loadPercentage = 85.0,
                                    workSection = RoutineWorkSection.Main,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val routineId = routines.createRoutine(draft(200.0))
        val sessionId = routines.startRoutine(routineId)
        val workoutSnapshot = gym.workoutExercises.first().single { it.sessionId == sessionId }
        gym.discardWorkout(sessionId)

        routines.updateRoutine(routineId, draft(190.0))

        val audit = routines.trainingMaxDecisions.first().single()
        assertEquals(TrainingMaxDecisionAction.Custom, audit.action)
        assertEquals(200.0, audit.previousTrainingMax, 0.0)
        assertEquals(-10.0, audit.appliedDelta, 0.0)
        assertEquals(190.0, audit.resultingTrainingMax, 0.0)
        assertTrue(audit.sessionUuid.startsWith("routine-edit:"))
        assertEquals(200.0, workoutSnapshot.trainingMaxValueSnapshot!!, 0.0)
    }

    @Test
    fun assistanceRoleAndWorkSectionRoundTripIntoWorkoutSnapshots() = runBlocking {
        val rowId = gym.createExercise(ExerciseDraft("Chest-supported row", weightUnitId = "pound"))
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Assistance snapshot",
                days = listOf(
                    RoutineDayDraft(
                        "Pull",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = rowId,
                                assistanceRole = RoutineAssistanceRole.Pull,
                                plannedSets = listOf(
                                    WorkoutSetDraft(
                                        weight = 100.0,
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

        assertEquals(
            RoutineAssistanceRole.Pull,
            routines.exercises.first().single { it.exerciseId == rowId }.assistanceRole,
        )
        assertEquals(
            RoutinePlacementKind.Assistance,
            routines.exercises.first().single { it.exerciseId == rowId }.placementKind,
        )
        assertEquals(
            RoutineAssistanceCategory.Pull,
            routines.exercises.first().single { it.exerciseId == rowId }.assistanceCategory,
        )
        assertEquals(
            RoutineWorkSection.Assistance,
            routines.sets.first().single().draft.workSection,
        )

        val sessionId = routines.startRoutine(routineId)
        val placement = gym.workoutExercises.first().single { it.sessionId == sessionId }
        assertEquals(RoutineAssistanceRole.Pull, placement.assistanceRoleSnapshot)
        assertEquals(RoutinePlacementKind.Assistance, placement.placementKindSnapshot)
        assertEquals(RoutineAssistanceCategory.Pull, placement.assistanceCategorySnapshot)
        assertEquals(
            RoutineWorkSection.Assistance,
            gym.sets.first().single { it.workoutExerciseId == placement.id }.workSectionSnapshot,
        )
    }

    @Test
    fun partialFailureAndUnderPrescriptionMainWorkAllHoldTrainingMax() = runBlocking {
        suspend fun createScenarioRoutine(name: String): Pair<Long, Long> {
            val exerciseId = gym.createExercise(
                ExerciseDraft("$name Press", weightUnitId = "pound", weightIncrement = 5.0),
            )
            val routineId = routines.createRoutine(
                RoutineDraft(
                    name = name,
                    program = RoutineProgramDraft(
                        RoutineProgramKind.FiveThreeOne,
                        phaseCount = 1,
                        trainingMaxAdvanceAfterPhaseIndices = setOf(0),
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
                                    mainWorkScheme = RoutineMainWorkScheme.FivesPro,
                                    assistanceRole = RoutineAssistanceRole.MainLift,
                                    plannedSets = listOf(65.0, 75.0).map { percentage ->
                                        WorkoutSetDraft(
                                            weightUnitId = "pound",
                                            reps = 5,
                                            loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                            loadPercentage = percentage,
                                            workSection = RoutineWorkSection.Main,
                                        )
                                    },
                                ),
                            ),
                        ),
                    ),
                ),
            )
            return routineId to exerciseId
        }

        suspend fun sessionSets(sessionId: Long) = gym.sets.first().filter { set ->
            gym.workoutExercises.first().any { it.sessionId == sessionId && it.id == set.workoutExerciseId }
        }.sortedBy { it.position }

        suspend fun completeWith(
            setId: Long,
            classification: WorkoutSetClassification = WorkoutSetClassification.Working,
            reps: Int? = null,
            weight: Double? = null,
        ) {
            val set = gym.sets.first().single { it.id == setId }
            gym.updateSet(
                setId,
                WorkoutSetDraft(
                    weight = weight ?: set.enteredWeight,
                    weightUnitId = set.enteredWeightUnitId ?: "pound",
                    reps = reps ?: set.repetitions,
                    completed = true,
                    classification = classification,
                ),
            )
        }

        suspend fun assertHeld(name: String, arrange: suspend (List<com.whip.app.domain.WorkoutSet>) -> Unit) {
            val (routineId, exerciseId) = createScenarioRoutine(name)
            val sessionId = routines.startRoutine(routineId)
            arrange(sessionSets(sessionId))
            gym.finishWorkout(sessionId)
            assertEquals(2, routines.routines.first().single { it.id == routineId }.currentProgramCycle)
            assertEquals(
                200.0,
                routines.exercises.first().single { it.exerciseId == exerciseId }.trainingMaxValue!!,
                0.0,
            )
        }

        assertHeld("Partial") { sets ->
            gym.setSetCompleted(sets.first().id, completed = true, autoStartRest = false)
        }
        assertHeld("Failure") { sets ->
            completeWith(sets.first().id, classification = WorkoutSetClassification.Failure)
            gym.setSetCompleted(sets.last().id, completed = true, autoStartRest = false)
        }
        assertHeld("Under reps") { sets ->
            completeWith(sets.first().id, reps = 4)
            gym.setSetCompleted(sets.last().id, completed = true, autoStartRest = false)
        }
        assertHeld("Under load") { sets ->
            completeWith(sets.first().id, weight = requireNotNull(sets.first().prescribedEnteredWeight) - 5.0)
            gym.setSetCompleted(sets.last().id, completed = true, autoStartRest = false)
        }
    }

    @Test
    fun emptyTrainingMaxBoundarySetNeverImplicitlyAdvancesAtFinalPhase() = runBlocking {
        val pressId = gym.createExercise(ExerciseDraft("No-boundary Press", weightUnitId = "pound"))
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Manual TM program",
                program = RoutineProgramDraft(
                    kind = RoutineProgramKind.FiveThreeOne,
                    phaseCount = 1,
                    trainingMaxAdvanceAfterPhaseIndices = emptySet(),
                ),
                days = listOf(
                    RoutineDayDraft(
                        "Press",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = pressId,
                                trainingMaxValue = 200.0,
                                trainingMaxUnitId = "pound",
                                cycleIncrementValue = 5.0,
                                trainingMaxSource = RoutineTrainingMaxSource.Explicit,
                                mainWorkScheme = RoutineMainWorkScheme.FivesPro,
                                assistanceRole = RoutineAssistanceRole.MainLift,
                                plannedSets = listOf(
                                    WorkoutSetDraft(
                                        weightUnitId = "pound",
                                        reps = 5,
                                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                        loadPercentage = 65.0,
                                        workSection = RoutineWorkSection.Main,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val sessionId = routines.startRoutine(routineId)
        val placementId = gym.workoutExercises.first().single { it.sessionId == sessionId }.id
        gym.setSetCompleted(
            gym.sets.first().single { it.workoutExerciseId == placementId }.id,
            completed = true,
            autoStartRest = false,
        )
        gym.finishWorkout(sessionId)

        assertEquals(2, routines.routines.first().single { it.id == routineId }.currentProgramCycle)
        assertEquals(200.0, routines.exercises.first().single { it.exerciseId == pressId }.trainingMaxValue!!, 0.0)
    }

    @Test
    fun repeatedFiveThreeOneMainLiftPlacementsCannotPersistDriftingTrainingMaxes() = runBlocking {
        val squatId = gym.createExercise(ExerciseDraft("Squat", weightUnitId = "pound", weightIncrement = 5.0))
        fun squat(trainingMax: Double) = RoutineExerciseDraft(
            exerciseId = squatId,
            trainingMaxValue = trainingMax,
            trainingMaxUnitId = "pound",
            cycleIncrementValue = 10.0,
            trainingMaxSource = RoutineTrainingMaxSource.Explicit,
            mainWorkScheme = RoutineMainWorkScheme.FivesPro,
            assistanceRole = RoutineAssistanceRole.MainLift,
            plannedSets = listOf(
                WorkoutSetDraft(
                    reps = 5,
                    weightUnitId = "pound",
                    loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                    loadPercentage = 65.0,
                    workSection = RoutineWorkSection.Main,
                ),
            ),
        )

        val failure = runCatching {
            routines.createRoutine(
                RoutineDraft(
                    name = "Beginners with drift",
                    program = RoutineProgramDraft(RoutineProgramKind.FiveThreeOne, phaseCount = 1),
                    days = listOf(
                        RoutineDayDraft("Day A", listOf(squat(300.0))),
                        RoutineDayDraft("Day B", listOf(squat(310.0))),
                    ),
                ),
            )
        }.exceptionOrNull()

        assertTrue(requireNotNull(failure).message.orEmpty().contains("must share the same training max"))
        assertTrue(routines.routines.first().isEmpty())
    }

    @Test
    fun configuredPhaseBoundaryAdvancesTrainingMaxAndSnapshotsPhaseRole() = runBlocking {
        val deadliftId = gym.createExercise(ExerciseDraft("Deadlift", weightUnitId = "pound", weightIncrement = 5.0))
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Leader and test",
                program = RoutineProgramDraft(
                    kind = RoutineProgramKind.FiveThreeOne,
                    phaseCount = 2,
                    phaseLabels = listOf("Leader", "TM Test"),
                    phaseRoles = listOf(RoutineProgramPhaseRole.Leader, RoutineProgramPhaseRole.TrainingMaxTest),
                    trainingMaxAdvanceAfterPhaseIndices = setOf(0),
                ),
                days = listOf(
                    RoutineDayDraft(
                        "Deadlift",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = deadliftId,
                                trainingMaxValue = 300.0,
                                trainingMaxUnitId = "pound",
                                cycleIncrementValue = 10.0,
                                trainingMaxSource = RoutineTrainingMaxSource.Explicit,
                                mainWorkScheme = RoutineMainWorkScheme.FivesPro,
                                assistanceRole = RoutineAssistanceRole.MainLift,
                                plannedSets = listOf(
                                    WorkoutSetDraft(
                                        reps = 5,
                                        weightUnitId = "pound",
                                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                        loadPercentage = 65.0,
                                        routinePhaseIndex = 0,
                                        workSection = RoutineWorkSection.Main,
                                    ),
                                    WorkoutSetDraft(
                                        reps = 3,
                                        weightUnitId = "pound",
                                        classification = WorkoutSetClassification.TrainingMaxTest,
                                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                        loadPercentage = 100.0,
                                        routinePhaseIndex = 1,
                                        workSection = RoutineWorkSection.Main,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val leaderSession = routines.startRoutine(routineId)
        assertEquals(
            RoutineProgramPhaseRole.Leader,
            gym.sessions.first().single { it.id == leaderSession }.sourceRoutinePhaseRole,
        )
        val leaderSet = gym.sets.first().single()
        gym.setSetCompleted(leaderSet.id, completed = true, autoStartRest = false)
        gym.finishWorkout(leaderSession)

        assertEquals(310.0, routines.exercises.first().single().trainingMaxValue!!, 0.0)
        assertEquals(1, routines.routines.first().single().currentProgramPhaseIndex)
        val historicalLeader = gym.workoutExercises.first().single { it.sessionId == leaderSession }
        assertEquals(300.0, historicalLeader.trainingMaxValueSnapshot!!, 0.0)
        assertEquals(RoutineMainWorkScheme.FivesPro, historicalLeader.mainWorkSchemeSnapshot)
        assertEquals(
            RoutineWorkSection.Main,
            gym.sets.first().single { it.workoutExerciseId == historicalLeader.id }.workSectionSnapshot,
        )
        assertEquals(
            RoutineProgramPhaseRole.Leader,
            gym.sessions.first().single { it.id == leaderSession }.sourceRoutinePhaseRole,
        )

        val testSession = routines.startRoutine(routineId)
        assertEquals(
            RoutineProgramPhaseRole.TrainingMaxTest,
            gym.sessions.first().single { it.id == testSession }.sourceRoutinePhaseRole,
        )
        val testPlacementId = gym.workoutExercises.first().single { it.sessionId == testSession }.id
        gym.setSetCompleted(
            gym.sets.first().single { it.workoutExerciseId == testPlacementId }.id,
            completed = true,
            autoStartRest = false,
        )
        gym.finishWorkout(testSession)

        assertEquals(310.0, routines.exercises.first().single().trainingMaxValue!!, 0.0)
        assertEquals(2, routines.routines.first().single().currentProgramCycle)
    }

    @Test
    fun activeSetPoliciesResolveLeaderAnchorAndDeloadSnapshotsWithoutLyingAboutJokers() = runBlocking {
        val pressId = gym.createExercise(ExerciseDraft("Phase-resolved press", weightUnitId = "pound"))
        fun mainSet(
            phase: Int,
            percentage: Double,
            reps: Int,
            scheme: RoutineMainWorkScheme,
            amrap: Boolean = false,
        ) = WorkoutSetDraft(
            weightUnitId = "pound",
            reps = reps,
            classification = if (amrap) WorkoutSetClassification.Amrap else WorkoutSetClassification.Working,
            loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
            loadPercentage = percentage,
            routinePhaseIndex = phase,
            workSection = RoutineWorkSection.Main,
            mainWorkScheme = scheme,
        )
        fun supplementalSets(
            phase: Int,
            count: Int,
            reps: Int,
            percentage: Double,
            scheme: RoutineSupplementalScheme,
        ) = List(count) {
            WorkoutSetDraft(
                weightUnitId = "pound",
                reps = reps,
                classification = WorkoutSetClassification.BackOff,
                loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                loadPercentage = percentage,
                routinePhaseIndex = phase,
                workSection = RoutineWorkSection.Supplemental,
                supplementalScheme = scheme,
            )
        }
        val leaderMain = listOf(65.0, 75.0, 85.0).map { percentage ->
            mainSet(0, percentage, 5, RoutineMainWorkScheme.FivesPro)
        }
        val anchorMain = listOf(
            mainSet(1, 75.0, 5, RoutineMainWorkScheme.ClassicPrSet),
            mainSet(1, 85.0, 3, RoutineMainWorkScheme.ClassicPrSet),
            mainSet(1, 95.0, 1, RoutineMainWorkScheme.ClassicPrSet, amrap = true),
        )
        val deloadMain = listOf(40.0, 50.0, 60.0).map { percentage ->
            mainSet(2, percentage, 5, RoutineMainWorkScheme.ClassicMinimumReps)
        }
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Set-authoritative phases",
                program = RoutineProgramDraft(
                    kind = RoutineProgramKind.FiveThreeOne,
                    phaseCount = 3,
                    phaseLabels = listOf("Leader", "Anchor", "Deload"),
                    phaseRoles = listOf(
                        RoutineProgramPhaseRole.Leader,
                        RoutineProgramPhaseRole.Anchor,
                        RoutineProgramPhaseRole.Deload,
                    ),
                ),
                days = listOf(
                    RoutineDayDraft(
                        "Press",
                        listOf(
                            RoutineExerciseDraft(
                                exerciseId = pressId,
                                trainingMaxValue = 200.0,
                                trainingMaxUnitId = "pound",
                                cycleIncrementValue = 5.0,
                                trainingMaxSource = RoutineTrainingMaxSource.Explicit,
                                // These placement summaries intentionally describe only the Anchor.
                                mainWorkScheme = RoutineMainWorkScheme.ClassicPrSet,
                                supplementalScheme = RoutineSupplementalScheme.FirstSetLast,
                                assistanceRole = RoutineAssistanceRole.MainLift,
                                jokerSetsEnabled = true,
                                plannedSets = leaderMain +
                                    supplementalSets(0, 5, 10, 50.0, RoutineSupplementalScheme.BoringButBig) +
                                    anchorMain +
                                    supplementalSets(1, 5, 5, 75.0, RoutineSupplementalScheme.FirstSetLast) +
                                    WorkoutSetDraft(
                                        weightUnitId = "pound",
                                        reps = 1,
                                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                                        loadPercentage = 100.0,
                                        routinePhaseIndex = 1,
                                        workSection = RoutineWorkSection.Optional,
                                        optionalWorkKind = RoutineOptionalWorkKind.Joker,
                                    ) + deloadMain,
                            ),
                        ),
                    ),
                ),
            ),
        )

        suspend fun startAndAssert(
            expectedMain: RoutineMainWorkScheme,
            expectedSupplemental: RoutineSupplementalScheme,
            expectedJoker: Boolean,
        ): Long {
            val sessionId = routines.startRoutine(routineId)
            val placement = gym.workoutExercises.first().single { it.sessionId == sessionId }
            val sets = gym.sets.first().filter { it.workoutExerciseId == placement.id }
            assertEquals(expectedMain, placement.mainWorkSchemeSnapshot)
            assertEquals(expectedSupplemental, placement.supplementalSchemeSnapshot)
            assertEquals(expectedJoker, placement.jokerSetsEnabledSnapshot)
            assertEquals(
                expectedJoker,
                sets.any {
                    it.workSectionSnapshot == RoutineWorkSection.Optional &&
                        it.optionalWorkKindSnapshot == RoutineOptionalWorkKind.Joker
                },
            )
            return sessionId
        }
        suspend fun completeMainAndFinish(sessionId: Long) {
            val placementId = gym.workoutExercises.first().single { it.sessionId == sessionId }.id
            gym.sets.first().filter {
                it.workoutExerciseId == placementId && it.workSectionSnapshot == RoutineWorkSection.Main
            }.forEach { set -> gym.setSetCompleted(set.id, completed = true, autoStartRest = false) }
            gym.finishWorkout(sessionId)
        }

        val leader = startAndAssert(
            RoutineMainWorkScheme.FivesPro,
            RoutineSupplementalScheme.BoringButBig,
            expectedJoker = false,
        )
        assertTrue(gym.sets.first().none {
            it.workoutExerciseId == gym.workoutExercises.first().single { placement -> placement.sessionId == leader }.id &&
                it.classification == WorkoutSetClassification.Amrap
        })
        completeMainAndFinish(leader)

        val anchor = startAndAssert(
            RoutineMainWorkScheme.ClassicPrSet,
            RoutineSupplementalScheme.FirstSetLast,
            expectedJoker = true,
        )
        assertTrue(gym.sets.first().any {
            it.workoutExerciseId == gym.workoutExercises.first().single { placement -> placement.sessionId == anchor }.id &&
                it.classification == WorkoutSetClassification.Amrap
        })
        completeMainAndFinish(anchor)

        startAndAssert(
            RoutineMainWorkScheme.ClassicMinimumReps,
            RoutineSupplementalScheme.None,
            expectedJoker = false,
        )
        Unit
    }

    @Test
    fun invalidSetPolicyPlacementAndDeterministicSchemeMismatchAreRejected() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Invalid policy press", weightUnitId = "pound"))
        fun routineWith(set: WorkoutSetDraft) = RoutineDraft(
            name = "Invalid policy",
            program = RoutineProgramDraft(RoutineProgramKind.FiveThreeOne, phaseCount = 1),
            days = listOf(
                RoutineDayDraft(
                    "Press",
                    listOf(
                        RoutineExerciseDraft(
                            exerciseId = exerciseId,
                            trainingMaxValue = 200.0,
                            trainingMaxUnitId = "pound",
                            trainingMaxSource = RoutineTrainingMaxSource.Explicit,
                            cycleIncrementValue = 5.0,
                            placementKind = when (set.workSection) {
                                RoutineWorkSection.Main,
                                RoutineWorkSection.Supplemental,
                                RoutineWorkSection.Optional,
                                -> RoutinePlacementKind.MainLift
                                RoutineWorkSection.Assistance -> RoutinePlacementKind.Assistance
                                RoutineWorkSection.Unspecified -> RoutinePlacementKind.General
                            },
                            assistanceCategory = if (set.workSection == RoutineWorkSection.Assistance) {
                                RoutineAssistanceCategory.Other
                            } else {
                                RoutineAssistanceCategory.Unspecified
                            },
                            plannedSets = listOf(set),
                        ),
                    ),
                ),
            ),
        )

        val wrongSection = runCatching {
            routines.createRoutine(
                routineWith(
                    WorkoutSetDraft(
                        reps = 10,
                        workSection = RoutineWorkSection.Assistance,
                        mainWorkScheme = RoutineMainWorkScheme.FivesPro,
                    ),
                ),
            )
        }.exceptionOrNull()
        assertTrue(requireNotNull(wrongSection).message.orEmpty().contains("only be attached to Main sets"))

        val fivesProMismatch = runCatching {
            routines.createRoutine(
                routineWith(
                    WorkoutSetDraft(
                        reps = 3,
                        workSection = RoutineWorkSection.Main,
                        mainWorkScheme = RoutineMainWorkScheme.FivesPro,
                    ),
                ),
            )
        }.exceptionOrNull()
        assertTrue(requireNotNull(fivesProMismatch).message.orEmpty().contains("5s PRO requires"))

        val bbbMismatch = runCatching {
            routines.createRoutine(
                routineWith(
                    WorkoutSetDraft(
                        reps = 10,
                        workSection = RoutineWorkSection.Supplemental,
                        supplementalScheme = RoutineSupplementalScheme.BoringButBig,
                    ),
                ),
            )
        }.exceptionOrNull()
        assertTrue(requireNotNull(bbbMismatch).message.orEmpty().contains("BBB requires exactly 5"))
    }

    @Test
    fun alternateBbbUsesOwnTrainingMaxSkipsEmptyPhasesAndStaysSynchronized() = runBlocking {
        val benchId = gym.createExercise(ExerciseDraft("Bench", weightUnitId = "pound", weightIncrement = 5.0))
        val deadliftId = gym.createExercise(ExerciseDraft("Deadlift", weightUnitId = "pound", weightIncrement = 5.0))
        fun mainSets() = listOf(65.0, 75.0, 85.0).mapIndexed { index, percentage ->
            WorkoutSetDraft(
                reps = 5,
                classification = if (index == 2) WorkoutSetClassification.Amrap else WorkoutSetClassification.Working,
                loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                loadPercentage = percentage,
                routinePhaseIndex = 0,
                workSection = RoutineWorkSection.Main,
                mainWorkScheme = RoutineMainWorkScheme.ClassicPrSet,
            )
        } + listOf(65.0, 75.0, 85.0).mapIndexed { index, percentage ->
            WorkoutSetDraft(
                reps = 5,
                classification = if (index == 2) WorkoutSetClassification.Amrap else WorkoutSetClassification.Working,
                loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                loadPercentage = percentage,
                routinePhaseIndex = 1,
                workSection = RoutineWorkSection.Main,
                mainWorkScheme = RoutineMainWorkScheme.ClassicPrSet,
            )
        } + List(5) {
            WorkoutSetDraft(
                reps = 5,
                classification = WorkoutSetClassification.BackOff,
                loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                loadPercentage = 65.0,
                routinePhaseIndex = 1,
                workSection = RoutineWorkSection.Supplemental,
                supplementalScheme = RoutineSupplementalScheme.FirstSetLast,
            )
        }
        fun main(exerciseId: Long, tm: Double) = RoutineExerciseDraft(
            exerciseId = exerciseId,
            trainingMaxValue = tm,
            trainingMaxUnitId = "pound",
            trainingMaxSource = RoutineTrainingMaxSource.Explicit,
            cycleIncrementValue = 5.0,
            placementKind = RoutinePlacementKind.MainLift,
            mainWorkScheme = RoutineMainWorkScheme.ClassicPrSet,
            supplementalScheme = RoutineSupplementalScheme.Custom,
            plannedSets = mainSets(),
        )
        fun alternate(exerciseId: Long, tm: Double) = RoutineExerciseDraft(
            exerciseId = exerciseId,
            notes = "Alternate BBB",
            trainingMaxValue = tm,
            trainingMaxUnitId = "pound",
            trainingMaxSource = RoutineTrainingMaxSource.Explicit,
            cycleIncrementValue = 5.0,
            placementKind = RoutinePlacementKind.Supplemental,
            supplementalScheme = RoutineSupplementalScheme.BoringButBig,
            plannedSets = List(5) {
                WorkoutSetDraft(
                    reps = 10,
                    classification = WorkoutSetClassification.BackOff,
                    loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                    loadPercentage = 50.0,
                    routinePhaseIndex = 0,
                    workSection = RoutineWorkSection.Supplemental,
                    supplementalScheme = RoutineSupplementalScheme.BoringButBig,
                )
            },
        )
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Alternate BBB",
                program = RoutineProgramDraft(
                    kind = RoutineProgramKind.FiveThreeOne,
                    phaseCount = 2,
                    phaseLabels = listOf("Leader", "Anchor"),
                    phaseRoles = listOf(RoutineProgramPhaseRole.Leader, RoutineProgramPhaseRole.Anchor),
                    trainingMaxAdvanceAfterPhaseIndices = setOf(0),
                ),
                days = listOf(
                    RoutineDayDraft("Bench", listOf(main(benchId, 200.0), alternate(deadliftId, 300.0))),
                    RoutineDayDraft("Deadlift", listOf(main(deadliftId, 300.0), alternate(benchId, 200.0))),
                ),
            ),
        )

        repeat(2) {
            val sessionId = routines.startRoutine(routineId)
            val placements = gym.workoutExercises.first().filter { it.sessionId == sessionId }
            assertEquals(2, placements.size)
            placements.forEach { placement ->
                gym.sets.first().filter {
                    it.workoutExerciseId == placement.id && it.workSectionSnapshot == RoutineWorkSection.Main
                }.forEach { set -> gym.setSetCompleted(set.id, completed = true, autoStartRest = false) }
            }
            gym.finishWorkout(sessionId)
        }

        val savedByExercise = routines.exercises.first().groupBy { it.exerciseId }
        assertTrue(savedByExercise.getValue(benchId).all { it.trainingMaxValue == 205.0 })
        assertTrue(savedByExercise.getValue(deadliftId).all { it.trainingMaxValue == 305.0 })

        val anchorSession = routines.startRoutine(routineId)
        val anchorPlacements = gym.workoutExercises.first().filter { it.sessionId == anchorSession }
        assertEquals(1, anchorPlacements.size)
        assertEquals(benchId, anchorPlacements.single().exerciseId)
    }

    @Test
    fun beginnersProtocolsSaveEditAndRunOncePerLiftWithoutEmptyDays() = runBlocking {
        val squatId = gym.createExercise(ExerciseDraft("Squat", weightUnitId = "pound", weightIncrement = 5.0))
        val benchId = gym.createExercise(ExerciseDraft("Bench", weightUnitId = "pound", weightIncrement = 5.0))
        val deadliftId = gym.createExercise(ExerciseDraft("Deadlift", weightUnitId = "pound", weightIncrement = 5.0))
        val pressId = gym.createExercise(ExerciseDraft("Press", weightUnitId = "pound", weightIncrement = 5.0))
        data class ProtocolCase(
            val name: String,
            val role: RoutineProgramPhaseRole,
            val mainScheme: RoutineMainWorkScheme,
            val repetitions: List<Int>,
        )
        val protocols = listOf(
            ProtocolCase(
                "Deload",
                RoutineProgramPhaseRole.Deload,
                RoutineMainWorkScheme.ClassicMinimumReps,
                listOf(5, 3, 1, 1),
            ),
            ProtocolCase(
                "Training Max Test",
                RoutineProgramPhaseRole.TrainingMaxTest,
                RoutineMainWorkScheme.ClassicMinimumReps,
                listOf(5, 5, 5, 3),
            ),
            ProtocolCase(
                "PR Test",
                RoutineProgramPhaseRole.PersonalRecordTest,
                RoutineMainWorkScheme.ClassicPrSet,
                listOf(5, 5, 5, 1),
            ),
        )
        val expectedByDay = listOf(listOf(squatId), listOf(deadliftId, pressId), listOf(benchId))

        protocols.forEach { protocol ->
            fun ownsProtocol(dayIndex: Int, exerciseId: Long): Boolean = when (exerciseId) {
                squatId -> dayIndex == 0
                benchId -> dayIndex == 2
                deadliftId, pressId -> dayIndex == 1
                else -> false
            }
            fun main(dayIndex: Int, exerciseId: Long) = RoutineExerciseDraft(
                exerciseId = exerciseId,
                trainingMaxValue = 200.0,
                trainingMaxUnitId = "pound",
                trainingMaxSource = RoutineTrainingMaxSource.Explicit,
                cycleIncrementValue = 5.0,
                placementKind = RoutinePlacementKind.MainLift,
                mainWorkScheme = protocol.mainScheme,
                plannedSets = listOf(70.0, 80.0, 90.0, 100.0).mapIndexedNotNull { index, percentage ->
                    if (
                        protocol.role == RoutineProgramPhaseRole.TrainingMaxTest && index == 3 &&
                        !ownsProtocol(dayIndex, exerciseId)
                    ) return@mapIndexedNotNull null
                    WorkoutSetDraft(
                        reps = protocol.repetitions[index],
                        classification = when {
                            protocol.role == RoutineProgramPhaseRole.TrainingMaxTest && index == 3 ->
                                WorkoutSetClassification.TrainingMaxTest
                            protocol.role == RoutineProgramPhaseRole.PersonalRecordTest && index == 3 ->
                                WorkoutSetClassification.Amrap
                            else -> WorkoutSetClassification.Working
                        },
                        loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                        loadPercentage = percentage,
                        routinePhaseIndex = 0,
                        workSection = RoutineWorkSection.Main,
                        mainWorkScheme = protocol.mainScheme,
                    )
                },
            )
            fun draft(trainingMax: Double) = RoutineDraft(
                name = "Beginners ${protocol.name}",
                program = RoutineProgramDraft(
                    kind = RoutineProgramKind.FiveThreeOne,
                    phaseCount = 1,
                    phaseLabels = listOf("7th Week · ${protocol.name}"),
                    phaseRoles = listOf(protocol.role.asOncePerLiftProtocol()),
                    templateKey = RoutineProgramTemplateKey.FiveThreeOneBeginners,
                    templateRevision = 2,
                ),
                days = listOf(
                    RoutineDayDraft("Monday", listOf(main(0, squatId), main(0, benchId))),
                    RoutineDayDraft("Wednesday", listOf(main(1, deadliftId), main(1, pressId))),
                    RoutineDayDraft("Friday", listOf(main(2, benchId), main(2, squatId))),
                ).map { day ->
                    day.copy(exercises = day.exercises.map { exercise ->
                        if (exercise.exerciseId == squatId) exercise.copy(trainingMaxValue = trainingMax) else exercise
                    })
                },
            )

            val routineId = routines.createRoutine(draft(200.0))
            routines.updateRoutine(routineId, draft(205.0))
            val executedExercises = mutableListOf<Long>()
            repeat(3) { dayIndex ->
                val sessionId = routines.startRoutine(routineId)
                val placements = gym.workoutExercises.first().filter { it.sessionId == sessionId }
                assertEquals("${protocol.name} day ${dayIndex + 1}", expectedByDay[dayIndex], placements.map { it.exerciseId })
                assertTrue("${protocol.name} day ${dayIndex + 1} must not be empty", placements.isNotEmpty())
                executedExercises += placements.map { it.exerciseId }
                val placementIds = placements.map { it.id }.toSet()
                val sets = gym.sets.first().filter { it.workoutExerciseId in placementIds }
                assertEquals(placements.size * 4, sets.size)
                sets.forEach { set -> gym.setSetCompleted(set.id, completed = true, autoStartRest = false) }
                gym.finishWorkout(sessionId)
            }
            assertEquals(setOf(squatId, benchId, deadliftId, pressId), executedExercises.toSet())
            assertEquals(4, executedExercises.size)
        }
    }

    @Test
    fun legacyBeginnersDeloadKeepsEverySavedRepeatedLiftExposure() = runBlocking {
        val squatId = gym.createExercise(ExerciseDraft("Squat", weightUnitId = "pound", weightIncrement = 5.0))
        val benchId = gym.createExercise(ExerciseDraft("Bench", weightUnitId = "pound", weightIncrement = 5.0))
        val deadliftId = gym.createExercise(ExerciseDraft("Deadlift", weightUnitId = "pound", weightIncrement = 5.0))
        val pressId = gym.createExercise(ExerciseDraft("Press", weightUnitId = "pound", weightIncrement = 5.0))
        fun main(exerciseId: Long) = RoutineExerciseDraft(
            exerciseId = exerciseId,
            trainingMaxValue = 200.0,
            trainingMaxUnitId = "pound",
            trainingMaxSource = RoutineTrainingMaxSource.Explicit,
            cycleIncrementValue = 5.0,
            placementKind = RoutinePlacementKind.MainLift,
            mainWorkScheme = RoutineMainWorkScheme.ClassicMinimumReps,
            plannedSets = listOf(40.0, 50.0, 60.0).map { percentage ->
                WorkoutSetDraft(
                    reps = 5,
                    loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                    loadPercentage = percentage,
                    routinePhaseIndex = 0,
                    workSection = RoutineWorkSection.Main,
                    mainWorkScheme = RoutineMainWorkScheme.ClassicMinimumReps,
                )
            },
        )
        val expectedByDay = listOf(
            listOf(squatId, benchId),
            listOf(deadliftId, pressId),
            listOf(benchId, squatId),
        )
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Legacy Beginners deload",
                program = RoutineProgramDraft(
                    kind = RoutineProgramKind.FiveThreeOne,
                    phaseCount = 1,
                    phaseLabels = listOf("Deload"),
                    phaseRoles = listOf(RoutineProgramPhaseRole.Deload),
                    templateKey = RoutineProgramTemplateKey.FiveThreeOneBeginners,
                    templateRevision = 1,
                ),
                days = listOf(
                    RoutineDayDraft("Monday", listOf(main(squatId), main(benchId))),
                    RoutineDayDraft("Wednesday", listOf(main(deadliftId), main(pressId))),
                    RoutineDayDraft("Friday", listOf(main(benchId), main(squatId))),
                ),
            ),
        )

        val executedExercises = mutableListOf<Long>()
        repeat(3) { dayIndex ->
            val sessionId = routines.startRoutine(routineId)
            val placements = gym.workoutExercises.first().filter { it.sessionId == sessionId }
            assertEquals(expectedByDay[dayIndex], placements.map { it.exerciseId })
            executedExercises += placements.map { it.exerciseId }
            val placementIds = placements.mapTo(mutableSetOf()) { it.id }
            gym.sets.first().filter { it.workoutExerciseId in placementIds }.forEach { set ->
                gym.setSetCompleted(set.id, completed = true, autoStartRest = false)
            }
            gym.finishWorkout(sessionId)
        }

        assertEquals(2, executedExercises.count { it == squatId })
        assertEquals(2, executedExercises.count { it == benchId })
        assertEquals(1, executedExercises.count { it == deadliftId })
        assertEquals(1, executedExercises.count { it == pressId })
    }

    @Test
    fun applyingProtocolToOneLegacyPhaseDoesNotChangeAnotherPhasesRepeatedExposures() = runBlocking {
        val squatId = gym.createExercise(ExerciseDraft("Squat", weightUnitId = "pound", weightIncrement = 5.0))
        val benchId = gym.createExercise(ExerciseDraft("Bench", weightUnitId = "pound", weightIncrement = 5.0))
        val deadliftId = gym.createExercise(ExerciseDraft("Deadlift", weightUnitId = "pound", weightIncrement = 5.0))
        val pressId = gym.createExercise(ExerciseDraft("Press", weightUnitId = "pound", weightIncrement = 5.0))
        fun main(exerciseId: Long) = RoutineExerciseDraft(
            exerciseId = exerciseId,
            trainingMaxValue = 200.0,
            trainingMaxUnitId = "pound",
            trainingMaxSource = RoutineTrainingMaxSource.Explicit,
            cycleIncrementValue = 5.0,
            placementKind = RoutinePlacementKind.MainLift,
            mainWorkScheme = RoutineMainWorkScheme.ClassicMinimumReps,
            plannedSets = listOf(70.0, 80.0, 90.0, 100.0).mapIndexed { index, percentage ->
                WorkoutSetDraft(
                    reps = listOf(5, 3, 1, 1)[index],
                    loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                    loadPercentage = percentage,
                    routinePhaseIndex = 0,
                    workSection = RoutineWorkSection.Main,
                    mainWorkScheme = RoutineMainWorkScheme.ClassicMinimumReps,
                )
            } + listOf(40.0, 50.0, 60.0).map { percentage ->
                WorkoutSetDraft(
                    reps = 5,
                    loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax,
                    loadPercentage = percentage,
                    routinePhaseIndex = 1,
                    workSection = RoutineWorkSection.Main,
                    mainWorkScheme = RoutineMainWorkScheme.ClassicMinimumReps,
                )
            },
        )
        val expectedByDay = listOf(
            listOf(squatId, benchId),
            listOf(deadliftId, pressId),
            listOf(benchId, squatId),
        )
        val routineId = routines.createRoutine(
            RoutineDraft(
                name = "Partially upgraded legacy Beginners",
                program = RoutineProgramDraft(
                    kind = RoutineProgramKind.FiveThreeOne,
                    phaseCount = 2,
                    phaseLabels = listOf("7th Week · Deload", "Untouched legacy deload"),
                    phaseRoles = listOf(
                        RoutineProgramPhaseRole.OncePerLiftDeload,
                        RoutineProgramPhaseRole.Deload,
                    ),
                    templateKey = RoutineProgramTemplateKey.FiveThreeOneBeginners,
                    templateRevision = 2,
                ),
                days = listOf(
                    RoutineDayDraft("Monday", listOf(main(squatId), main(benchId))),
                    RoutineDayDraft("Wednesday", listOf(main(deadliftId), main(pressId))),
                    RoutineDayDraft("Friday", listOf(main(benchId), main(squatId))),
                ),
            ),
        )
        routines.setRoutineProgramPosition(routineId, phaseIndex = 1, dayPosition = 0, cycle = 1)

        val executedExercises = mutableListOf<Long>()
        repeat(3) { dayIndex ->
            val sessionId = routines.startRoutine(routineId)
            val placements = gym.workoutExercises.first().filter { it.sessionId == sessionId }
            assertEquals(expectedByDay[dayIndex], placements.map { it.exerciseId })
            executedExercises += placements.map { it.exerciseId }
            val placementIds = placements.mapTo(mutableSetOf()) { it.id }
            val sets = gym.sets.first().filter { it.workoutExerciseId in placementIds }
            assertEquals(placements.size * 3, sets.size)
            sets.forEach { set -> gym.setSetCompleted(set.id, completed = true, autoStartRest = false) }
            gym.finishWorkout(sessionId)
        }

        assertEquals(2, executedExercises.count { it == squatId })
        assertEquals(2, executedExercises.count { it == benchId })
        assertEquals(1, executedExercises.count { it == deadliftId })
        assertEquals(1, executedExercises.count { it == pressId })
    }

    @Test
    fun archivedMachineRemainsUsableByExistingRoutine() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Archived machine press"))
        val machineId = gym.createMachine(GymMachineDraft(exerciseId, "Known machine"))
        val routineId = routines.createRoutine(
            RoutineDraft(
                "Existing plan",
                days = listOf(RoutineDayDraft("A", listOf(RoutineExerciseDraft(exerciseId, machineId = machineId)))),
            ),
        )
        gym.setMachineArchived(machineId, true)

        routines.startRoutine(routineId)

        assertEquals(machineId, gym.workoutExercises.first().single().machineId)
        assertTrue(gym.machines.first().single().archived)
    }

    @Test
    fun graphPresetCanBeRenamedUpdatedAndDeleted() = runBlocking {
        val exerciseId = gym.createExercise(ExerciseDraft("Preset bench"))
        val id = routines.saveGraphPreset(
            "Bench trend",
            listOf(exerciseId),
            "MaxWeight",
            "ThreeMonths",
            "Workout",
        )

        routines.updateGraphPreset(id, "Bench year", listOf(exerciseId), "EstimatedOneRepMax", "Year", "Week")
        val updated = routines.graphPresets.first().single()
        assertEquals("Bench year", updated.name)
        assertEquals("EstimatedOneRepMax", updated.metric)
        assertEquals("Year", updated.dateRange)
        assertEquals("Week", updated.aggregation)

        routines.deleteGraphPreset(id)
        assertTrue(routines.graphPresets.first().isEmpty())
    }

    private object FixedClock : WhipClock {
        override fun now(): Instant = Instant.parse("2026-08-17T16:00:00Z")
        override fun today(zoneId: ZoneId): LocalDate = LocalDate.of(2026, 8, 17)
    }

    private class SequentialIds : WhipIdGenerator {
        private val count = AtomicInteger()
        override fun nextId(): String = "routine-test-${count.incrementAndGet()}"
    }
}
