package com.whip.app.ui

import androidx.lifecycle.SavedStateHandle
import com.whip.app.core.RepPrescriptionScheme
import com.whip.app.domain.RoutineLoadPrescriptionType
import com.whip.app.domain.RoutineAssistanceRole
import com.whip.app.domain.RoutineAssistanceCategory
import com.whip.app.domain.RoutinePlacementKind
import com.whip.app.domain.RoutineProgramTemplateKey
import com.whip.app.domain.RoutineMainWorkScheme
import com.whip.app.domain.RoutineOptionalWorkKind
import com.whip.app.domain.RoutineProgramKind
import com.whip.app.domain.RoutineProgramPhaseRole
import com.whip.app.domain.RoutineSupplementalScheme
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.TrainingMaxBasisKind
import com.whip.app.domain.WorkoutSetClassification
import com.whip.app.domain.BodyweightLoadPolicy
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.Exercise
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.balancedOncePerLiftDayOwners
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineBuilderStateTest {
    @Test
    fun draftSurvivesViewModelRecreationAndClearsOnlyWhenRequested() {
        val handle = SavedStateHandle()
        val original = RoutineBuilderViewModel(handle)
        original.initialize(
            "new-routine",
            RoutineBuilderState(
                token = "new-routine",
                name = "Upper",
                days = listOf(RoutineBuilderDayState(1, "A")),
                selectedDayKey = 1,
                nextKey = 2,
            ),
        )
        original.update { state ->
            state.copy(
                notes = "Keep this draft",
                days = state.days.map { day ->
                    day.copy(placements = listOf(RoutineBuilderPlacementState(2, 99, "Bench")))
                },
                nextKey = 3,
            )
        }

        val recreated = RoutineBuilderViewModel(handle)
        assertEquals("Keep this draft", recreated.state.value.notes)
        assertEquals("Bench", recreated.state.value.days.single().placements.single().exerciseNameSnapshot)

        recreated.clear()
        assertNull(handle.get<RoutineBuilderState>("routine-builder-state"))
    }

    @Test
    fun restoredRoutineWithSameIdCannotInheritDraftFromEarlierDataGeneration() {
        val handle = SavedStateHandle()
        val beforeRestore = RoutineBuilderViewModel(handle)
        beforeRestore.initialize(
            token = "routine-7",
            initial = RoutineBuilderState(name = "Original routine"),
            dataGeneration = 4L,
        )
        beforeRestore.update { it.copy(name = "Unsaved old draft", notes = "Must not cross restore") }

        val afterProcessRecreation = RoutineBuilderViewModel(handle)
        afterProcessRecreation.initialize(
            token = "routine-7",
            initial = RoutineBuilderState(name = "Restored routine", notes = "From restored database"),
            dataGeneration = 5L,
        )

        assertEquals(5L, afterProcessRecreation.state.value.dataGeneration)
        assertEquals("Restored routine", afterProcessRecreation.state.value.name)
        assertEquals("From restored database", afterProcessRecreation.state.value.notes)
    }

    @Test
    fun groupingAndUngroupingNeverLeaveSingletonGroups() {
        val day = RoutineBuilderDayState(
            key = 1,
            name = "A",
            placements = listOf(
                RoutineBuilderPlacementState(10, 1, "Bench"),
                RoutineBuilderPlacementState(11, 2, "Row"),
                RoutineBuilderPlacementState(12, 3, "Curl"),
                RoutineBuilderPlacementState(13, 4, "Extension"),
            ),
        )

        val grouped = day.groupPlacements(10, 11)
        val group = grouped.placements.first().groupKey
        assertEquals(group, grouped.placements[1].groupKey)

        val withSecondGroup = grouped.groupPlacements(12, 13)
        val regrouped = withSecondGroup.groupPlacements(10, 12)
        assertNull(regrouped.placements.first { it.key == 11L }.groupKey)
        assertEquals(regrouped.placements.first { it.key == 10L }.groupKey, regrouped.placements.first { it.key == 12L }.groupKey)

        val removed = regrouped.removePlacementFromGroup(13).removePlacementFromGroup(10)
        assertNull(removed.placements.first { it.key == 10L }.groupKey)
        assertNull(removed.placements.first { it.key == 12L }.groupKey)
    }

    @Test
    fun dayTemplatesDuplicateAndReorderWithUniquePlacementKeys() {
        val placement = RoutineBuilderPlacementState(
            key = 2,
            exerciseId = 10,
            exerciseNameSnapshot = "Bench",
            sets = listOf(RoutineBuilderSetState(3, repetitionsMin = "8", repetitionsMax = "10")),
        )
        val initial = RoutineBuilderState(
            token = "routine",
            days = listOf(RoutineBuilderDayState(1, "Upper", listOf(placement))),
            selectedDayKey = 1,
            nextKey = 4,
        )

        val duplicated = initial.duplicateDay(1)
        assertEquals(listOf("Upper", "Upper copy"), duplicated.days.map { it.name })
        assertEquals(2, duplicated.days.sumOf { it.placements.size })
        assertEquals(2, duplicated.days.flatMap { it.placements }.map { it.key }.distinct().size)
        assertEquals(2, duplicated.days.flatMap { it.placements }.flatMap { it.sets }.map { it.key }.distinct().size)

        val reordered = duplicated.moveDay(duplicated.days.last().key, -1)
        assertEquals("Upper copy", reordered.days.first().name)
        val templated = initial.copy(days = listOf(RoutineBuilderDayState(1, "Empty")), nextKey = 2)
            .withDayTemplate(listOf("Push", "Pull", "Legs"))
        assertEquals(listOf("Push", "Pull", "Legs"), templated.days.map { it.name })
    }

    @Test
    fun placementReorderSupportsDuplicateExerciseIds() {
        val day = RoutineBuilderDayState(
            1,
            "A",
            listOf(
                RoutineBuilderPlacementState(2, 99, "Bench heavy"),
                RoutineBuilderPlacementState(3, 99, "Bench backoff"),
            ),
        )

        assertEquals(listOf(3L, 2L), day.movePlacement(3, -1).placements.map { it.key })
    }

    @Test
    fun groupedPlacementsBecomeContiguousAndOnlyMoveWithinTheirBlock() {
        val day = RoutineBuilderDayState(
            1,
            "A",
            listOf(
                RoutineBuilderPlacementState(10, 1, "Bench"),
                RoutineBuilderPlacementState(11, 2, "Row"),
                RoutineBuilderPlacementState(12, 3, "Curl"),
                RoutineBuilderPlacementState(13, 4, "Press"),
            ),
        )

        val grouped = day.groupPlacements(10, 12)
        assertEquals(listOf(10L, 12L, 11L, 13L), grouped.placements.map { it.key })
        val moved = grouped.movePlacement(12, -1)
        assertEquals(listOf(12L, 10L, 11L, 13L), moved.placements.map { it.key })
        assertEquals(1, moved.placements.take(2).mapNotNull { it.groupKey }.distinct().size)
    }

    @Test
    fun removingAndRestoringAPlacementPreservesTheGroupInvariant() {
        val grouped = RoutineBuilderDayState(
            1,
            "A",
            listOf(
                RoutineBuilderPlacementState(10, 1, "Bench", groupKey = "Superset A"),
                RoutineBuilderPlacementState(11, 2, "Row", groupKey = "Superset A"),
                RoutineBuilderPlacementState(12, 3, "Curl"),
            ),
        )

        val removed = grouped.removePlacement(10)
        assertNull(removed.placements.single { it.key == 11L }.groupKey)

        val restored = removed.restorePlacement(0, grouped.placements.first(), listOf(10L, 11L))
        assertEquals("Superset A", restored.placements.single { it.key == 10L }.groupKey)
        assertEquals("Superset A", restored.placements.single { it.key == 11L }.groupKey)
    }

    @Test
    fun movingOrCopyingOnePlacementBetweenDaysNeverCarriesAGroupDesignation() {
        val groupedDay = RoutineBuilderDayState(
            1,
            "A",
            listOf(
                RoutineBuilderPlacementState(10, 1, "Bench", groupKey = "Superset A"),
                RoutineBuilderPlacementState(11, 2, "Row", groupKey = "Superset A"),
            ),
        )
        val emptyDay = RoutineBuilderDayState(2, "B")
        val initial = RoutineBuilderState(
            token = "routine",
            days = listOf(groupedDay, emptyDay),
            selectedDayKey = 1,
            nextKey = 20,
        )

        val moved = initial.moveOrCopyPlacement(1, 2, groupedDay.placements.first(), copy = false)
        assertNull(moved.days.single { it.key == 1L }.placements.single().groupKey)
        assertNull(moved.days.single { it.key == 2L }.placements.single().groupKey)

        val copied = initial.moveOrCopyPlacement(1, 2, groupedDay.placements.first(), copy = true)
        assertEquals(2, copied.days.single { it.key == 1L }.placements.count { it.groupKey == "Superset A" })
        assertNull(copied.days.single { it.key == 2L }.placements.single().groupKey)
    }

    @Test
    fun repSchemeAppliesItsMeaningWithoutOverwritingIndependentSetInputs() {
        val existing = listOf(
            RoutineBuilderSetState(key = 7, load = "135", restSeconds = "90", note = "Keep me"),
            RoutineBuilderSetState(key = 8, load = "140", restSeconds = "100"),
        )
        val scheme = RepPrescriptionScheme(
            id = "hypertrophy",
            name = "Hypertrophy",
            setCount = 3,
            repetitionsMin = 8,
            repetitionsMax = 12,
            classification = WorkoutSetClassification.BackOff,
            restSeconds = 120,
        )

        val applied = applyRepPrescriptionScheme(existing, scheme)

        assertEquals(3, applied.size)
        assertEquals(listOf(7L, 8L, 9L), applied.map { it.key })
        assertEquals(listOf("8", "8", "8"), applied.map { it.repetitionsMin })
        assertEquals(listOf("12", "12", "12"), applied.map { it.repetitionsMax })
        assertEquals(listOf("120", "120", "120"), applied.map { it.restSeconds })
        assertEquals(listOf("BackOff", "BackOff", "BackOff"), applied.map { it.classification })
        assertEquals("135", applied.first().load)
        assertEquals("Keep me", applied.first().note)

        val repsOnly = scheme.copy(id = "reps-only", restSeconds = null, setCount = 1, repetitionsMax = 8)
        assertEquals("90", applyRepPrescriptionScheme(existing, repsOnly).single().restSeconds)
    }

    @Test
    fun classicFiveThreeOneBuildsAllFourPhasesWithExplicitAmrapTargets() {
        val config = FiveThreeOneAuthoringConfig(
            trainingMax = 200.0,
            mainScheme = FiveThreeOneMainScheme.Classic,
            phase = FiveThreeOnePhase.Threes,
            supplement = FiveThreeOneSupplement.None,
        )

        val cycle = previewFiveThreeOneCycle(config, increment = 5.0)
        val saved = fiveThreeOneBuilderSets(emptyList(), cycle)

        assertEquals(12, saved.size)
        assertEquals(listOf(0, 1, 2, 3), saved.mapNotNull { it.routinePhaseIndex }.distinct())
        assertEquals(listOf(3, 3, 3, 3), (0..3).map { phase -> saved.count { it.routinePhaseIndex == phase } })
        assertEquals(3, saved.count { it.classification == WorkoutSetClassification.Amrap.name })
        assertTrue(saved.all { it.load.isEmpty() })
        assertTrue(saved.all { it.loadPrescriptionType == RoutineLoadPrescriptionType.PercentTrainingMax.name })
        assertEquals("65", saved.first { it.routinePhaseIndex == FiveThreeOnePhase.Fives.ordinal }.loadPercentage)
        assertTrue(saved.filter { it.classification == WorkoutSetClassification.Amrap.name }.all { "minimum" in it.note })
        assertFalse(saved.filter { it.routinePhaseIndex == FiveThreeOnePhase.Deload.ordinal }
            .any { it.classification == WorkoutSetClassification.Amrap.name })
    }

    @Test
    fun fivesProAndBbbCreateFiveRepMainWorkAndUniversalFiveByTenWork() {
        val config = FiveThreeOneAuthoringConfig(
            trainingMax = 100.0,
            mainScheme = FiveThreeOneMainScheme.FivesPro,
            phase = FiveThreeOnePhase.Fives,
            supplement = FiveThreeOneSupplement.BoringButBig,
            boringButBigPercent = 50.0,
        )

        val cycle = previewFiveThreeOneCycle(config, increment = 2.5)
        val main = cycle.filter { it.plan.section == FiveThreeOneSetSection.Main }
        val supplemental = cycle.filter { it.plan.section == FiveThreeOneSetSection.Supplemental }

        assertEquals(12, main.size)
        assertTrue(main.all { it.plan.repetitions == 5 && !it.plan.amrap })
        assertEquals(5, supplemental.size)
        assertTrue(supplemental.all {
            it.plan.phase == null && it.plan.repetitions == 10 && it.plan.percentageOfTrainingMax == 50.0
        })
    }

    @Test
    fun firstSetLastTracksTrainingPhasesAndDefaultsDeloadToMainWorkOnly() {
        val config = FiveThreeOneAuthoringConfig(
            trainingMax = 100.0,
            mainScheme = FiveThreeOneMainScheme.Classic,
            phase = FiveThreeOnePhase.Fives,
            supplement = FiveThreeOneSupplement.FirstSetLast,
        )

        val supplemental = previewFiveThreeOneCycle(config, increment = 2.5)
            .filter { it.plan.section == FiveThreeOneSetSection.Supplemental }

        assertEquals(15, supplemental.size)
        assertEquals(
            listOf(65.0, 70.0, 75.0),
            FiveThreeOnePhase.entries.dropLast(1).map { phase ->
                supplemental.first { it.plan.phase == phase }.plan.percentageOfTrainingMax
            },
        )
        assertTrue(supplemental.none { it.plan.phase == FiveThreeOnePhase.Deload })
        assertTrue(supplemental.all { it.plan.repetitions == 5 })
    }

    @Test
    fun generatedDeloadSupplementalDefaultsAreTemplateSpecific() {
        val deload = FiveThreeOneAuthoringConfig(
            trainingMax = 200.0,
            mainScheme = FiveThreeOneMainScheme.Classic,
            phase = FiveThreeOnePhase.Deload,
            supplement = FiveThreeOneSupplement.None,
            jokerSetsEnabled = true,
        )

        listOf(
            FiveThreeOneSupplement.None,
            FiveThreeOneSupplement.FirstSetLast,
            FiveThreeOneSupplement.SecondSetLast,
            FiveThreeOneSupplement.BoringButStrong,
        ).forEach { supplement ->
            val plans = fiveThreeOneSetPlans(deload.copy(supplement = supplement))
            assertEquals(3, plans.size)
            assertTrue(plans.all { it.section == FiveThreeOneSetSection.Main })
        }

        val bbb = fiveThreeOneSetPlans(deload.copy(supplement = FiveThreeOneSupplement.BoringButBig))
        assertEquals(5, bbb.count { it.section == FiveThreeOneSetSection.Supplemental })
        assertTrue(bbb.filter { it.section == FiveThreeOneSetSection.Supplemental }
            .all { it.repetitions == 10 && it.percentageOfTrainingMax == 50.0 })
        assertTrue(bbb.none { it.optionalWorkKind == RoutineOptionalWorkKind.Joker })
    }

    @Test
    fun compositionalFiveThreeOneSupportsSslBoringButStrongAndOptionalJokers() {
        val ssl = FiveThreeOneAuthoringConfig(
            200.0,
            FiveThreeOneMainScheme.FivesPro,
            FiveThreeOnePhase.Fives,
            FiveThreeOneSupplement.SecondSetLast,
            jokerSetsEnabled = true,
        )
        val sslCycle = previewFiveThreeOneCycle(ssl, 5.0)
        val jokers = sslCycle.filter { it.plan.optionalWorkKind == RoutineOptionalWorkKind.Joker }
        assertEquals(3, jokers.size)
        assertTrue(jokers.all { it.plan.section == FiveThreeOneSetSection.Optional })
        assertEquals(listOf(5, 3, 1), jokers.map { it.plan.repetitions })
        assertEquals(15, sslCycle.count { it.plan.section == FiveThreeOneSetSection.Supplemental })
        assertEquals(RoutineProgramKind.FiveThreeOne, fiveThreeOneProgramKind(ssl))
        assertEquals(RoutineSupplementalScheme.SecondSetLast, fiveThreeOneSupplementalScheme(ssl))

        val bbs = previewFiveThreeOneCycle(
            ssl.copy(supplement = FiveThreeOneSupplement.BoringButStrong, jokerSetsEnabled = false),
            5.0,
        )
        assertEquals(30, bbs.count { it.plan.section == FiveThreeOneSetSection.Supplemental })
        assertTrue(bbs.filter { it.plan.section == FiveThreeOneSetSection.Supplemental }.all { it.plan.repetitions == 5 })
    }

    @Test
    fun jokerToggleIsStrictlyAdditiveAndPreservesEverySupplementalSet() {
        listOf(
            FiveThreeOneSupplement.BoringButBig to 5,
            FiveThreeOneSupplement.FirstSetLast to 5,
            FiveThreeOneSupplement.SecondSetLast to 5,
            FiveThreeOneSupplement.BoringButStrong to 10,
        ).forEach { (supplement, expectedPhaseSupplementalCount) ->
            val initial = buildFiveThreeOneProgramState(
                current = RoutineBuilderState(nextKey = 1),
                layout = FiveThreeOneProgramLayout.Custom,
                lifts = listOf(
                    FiveThreeOneProgramLift(
                        role = null,
                        exerciseId = 91,
                        exerciseName = "Zercher squat",
                        trainingMax = 200.0,
                        unitId = "pound",
                        loadIncrement = 5.0,
                        cycleIncrement = 10.0,
                    ),
                ),
                mainScheme = FiveThreeOneMainScheme.Classic,
                supplement = supplement,
                jokerSetsEnabled = false,
            )
            val before = initial.days.single().placements.single()
            val beforeRequired = before.sets.filter { set ->
                set.workSection == RoutineWorkSection.Main.name ||
                    set.workSection == RoutineWorkSection.Supplemental.name
            }

            val enabled = initial.setFiveThreeOneJokerEnabled(phaseIndex = 0, enabled = true)
            val after = enabled.days.single().placements.single()
            val afterRequired = after.sets.filter { set ->
                set.workSection == RoutineWorkSection.Main.name ||
                    set.workSection == RoutineWorkSection.Supplemental.name
            }
            val activeSupplemental = after.sets.filter { set ->
                set.workSection == RoutineWorkSection.Supplemental.name &&
                    (set.routinePhaseIndex == null || set.routinePhaseIndex == 0)
            }
            val phaseJokers = after.sets.filter { set ->
                set.routinePhaseIndex == 0 &&
                    set.optionalWorkKind == RoutineOptionalWorkKind.Joker.name
            }

            assertEquals("$supplement must preserve required set identity", beforeRequired, afterRequired)
            assertEquals(before.sets.size + 1, after.sets.size)
            assertEquals(expectedPhaseSupplementalCount, activeSupplemental.size)
            assertEquals(1, phaseJokers.size)
            val jokerIndex = after.sets.indexOf(phaseJokers.single())
            val lastMainIndex = after.sets.indexOfLast { set ->
                set.routinePhaseIndex == 0 && set.workSection == RoutineWorkSection.Main.name
            }
            val firstSupplementalIndex = after.sets.indexOfFirst { set ->
                set.workSection == RoutineWorkSection.Supplemental.name &&
                    (set.routinePhaseIndex == null || set.routinePhaseIndex == 0)
            }
            assertEquals(lastMainIndex + 1, jokerIndex)
            assertTrue(jokerIndex < firstSupplementalIndex)

            val disabled = enabled.setFiveThreeOneJokerEnabled(phaseIndex = 0, enabled = false)
            assertEquals(before.sets, disabled.days.single().placements.single().sets)
        }
    }

    @Test
    fun wholeProgramBuilderCreatesHonestFourDayAndBeginnersSchedules() {
        val lifts = FiveThreeOneLiftRole.entries.mapIndexed { index, role ->
            FiveThreeOneProgramLift(role, index + 1L, role.label, 300.0 - index * 25, "pound", 5.0, if (role in setOf(FiveThreeOneLiftRole.Squat, FiveThreeOneLiftRole.Deadlift)) 10.0 else 5.0)
        }
        val base = RoutineBuilderState(nextKey = 1)
        val fourDay = buildFiveThreeOneProgramState(
            base,
            FiveThreeOneProgramLayout.FourDay,
            lifts,
            FiveThreeOneMainScheme.Classic,
            FiveThreeOneSupplement.SecondSetLast,
            jokerSetsEnabled = false,
        )
        assertEquals(listOf("Squat", "Bench", "Deadlift", "Press"), fourDay.days.map { it.name })
        assertEquals(RoutineProgramKind.FiveThreeOne.name, fourDay.programKind)
        assertEquals(RoutineProgramTemplateKey.FiveThreeOneFourDay.name, fourDay.programTemplateKey)
        assertEquals(1, fourDay.programTemplateRevision)
        assertTrue(fourDay.days.flatMap { it.placements }.all { it.assistanceRole == RoutineAssistanceRole.MainLift.name })

        val beginners = buildFiveThreeOneProgramState(
            base,
            FiveThreeOneProgramLayout.Beginners,
            lifts,
            FiveThreeOneMainScheme.FivesPro,
            FiveThreeOneSupplement.None,
            jokerSetsEnabled = false,
        )
        assertEquals(
            listOf(listOf("Squat", "Bench Press"), listOf("Deadlift", "Overhead Press"), listOf("Bench Press", "Squat")),
            beginners.days.map { day -> day.placements.map { it.exerciseNameSnapshot } },
        )
        assertTrue(beginners.days.flatMap { it.placements }.all { it.supplementalScheme == RoutineSupplementalScheme.FirstSetLast.name })
        assertEquals(RoutineProgramTemplateKey.FiveThreeOneBeginners.name, beginners.programTemplateKey)
        val roundedSquat = previewFiveThreeOneSets(
            FiveThreeOneAuthoringConfig(300.0, FiveThreeOneMainScheme.Classic, FiveThreeOnePhase.Fives, FiveThreeOneSupplement.None),
            increment = 5.0,
        ).map { it.roundedLoad }
        assertEquals(listOf(195.0, 225.0, 255.0), roundedSquat)
    }

    @Test
    fun customProgramBuilderUsesAnyDistinctChosenLiftsInTheRequestedOrder() {
        val lifts = listOf(
            FiveThreeOneProgramLift(null, 11, "Bench Press", 200.0, "pound", 5.0, 5.0),
            FiveThreeOneProgramLift(null, 12, "Deadlift", 300.0, "pound", 5.0, 10.0),
            FiveThreeOneProgramLift(null, 13, "Zercher Squat", 250.0, "pound", 5.0, 10.0),
        )

        val custom = buildFiveThreeOneProgramState(
            current = RoutineBuilderState(nextKey = 1),
            layout = FiveThreeOneProgramLayout.Custom,
            lifts = lifts,
            mainScheme = FiveThreeOneMainScheme.Classic,
            supplement = FiveThreeOneSupplement.FirstSetLast,
            jokerSetsEnabled = false,
        )

        assertEquals("Custom 5/3/1", custom.name)
        assertEquals(RoutineProgramTemplateKey.FiveThreeOneCustom.name, custom.programTemplateKey)
        assertEquals(listOf("Bench Press", "Deadlift", "Zercher Squat"), custom.days.map { it.name })
        assertEquals(listOf(11L, 12L, 13L), custom.days.map { it.placements.single().exerciseId })
        assertTrue(custom.days.flatMap { it.placements }.all { placement ->
            placement.assistanceRole == RoutineAssistanceRole.MainLift.name &&
                placement.sets.count { it.workSection == RoutineWorkSection.Main.name } == 12 &&
                placement.sets.count { it.workSection == RoutineWorkSection.Supplemental.name } == 15
        })
        assertEquals(10.0, defaultFiveThreeOneCycleIncrease("pound", "Zercher Squat"), 0.0001)
        assertEquals(5.0, defaultFiveThreeOneCycleIncrease("pound", "Bench Press"), 0.0001)

        assertThrows(IllegalArgumentException::class.java) {
            buildFiveThreeOneProgramState(
                current = RoutineBuilderState(nextKey = 1),
                layout = FiveThreeOneProgramLayout.Custom,
                lifts = lifts + lifts.first().copy(exerciseName = "Duplicate Bench"),
                mainScheme = FiveThreeOneMainScheme.Classic,
                supplement = FiveThreeOneSupplement.None,
                jokerSetsEnabled = false,
            )
        }
    }

    @Test
    fun repeatedBeginnersLiftPlacementsStaySynchronizedAndUseUniqueSetKeys() {
        val repeated = RoutineBuilderPlacementState(
            key = 10,
            exerciseId = 1,
            exerciseNameSnapshot = "Squat",
            assistanceRole = RoutineAssistanceRole.MainLift.name,
            placementKind = RoutinePlacementKind.MainLift.name,
            trainingMaxValue = "300",
            supplementalScheme = RoutineSupplementalScheme.FirstSetLast.name,
            sets = listOf(RoutineBuilderSetState(11, workSection = RoutineWorkSection.Main.name)),
        )
        val state = RoutineBuilderState(
            programKind = RoutineProgramKind.FiveThreeOne.name,
            days = listOf(
                RoutineBuilderDayState(1, "Monday", listOf(repeated)),
                RoutineBuilderDayState(2, "Friday", listOf(repeated.copy(key = 20, sets = listOf(RoutineBuilderSetState(21))))),
            ),
            nextKey = 30,
        )
        val updated = state.updateProgramPlacement(10) { source ->
            source.copy(
                trainingMaxValue = "305",
                trainingMaxPercent = "85",
                trainingMaxBasisKind = TrainingMaxBasisKind.ActualOneRepMax.name,
                trainingMaxBasisValue = "360",
                trainingMaxBasisUnitId = "pound",
                trainingMaxIncreaseEligible = false,
                jokerSetsEnabled = true,
                sets = source.sets + RoutineBuilderSetState(12),
            )
        }
        val placements = updated.days.flatMap { it.placements }
        assertTrue(placements.all { it.trainingMaxValue == "305" && it.jokerSetsEnabled })
        assertTrue(placements.all {
            it.trainingMaxPercent == "85" &&
                it.trainingMaxBasisKind == TrainingMaxBasisKind.ActualOneRepMax.name &&
                it.trainingMaxBasisValue == "360" && it.trainingMaxBasisUnitId == "pound" &&
                !it.trainingMaxIncreaseEligible
        })
        assertEquals(4, placements.flatMap { it.sets }.map { it.key }.distinct().size)
    }

    @Test
    fun repeatedLiftSynchronizationPreservesOneTrainingMaxTestOwnerPerPhase() {
        val sharedMain = listOf(
            RoutineBuilderSetState(
                key = 11,
                repetitionsMin = "5",
                routinePhaseIndex = 0,
                workSection = RoutineWorkSection.Main.name,
            ),
            RoutineBuilderSetState(
                key = 12,
                repetitionsMin = "3",
                routinePhaseIndex = 1,
                workSection = RoutineWorkSection.Main.name,
            ),
        )
        val trainingMaxTest = RoutineBuilderSetState(
            key = 13,
            repetitionsMin = "3",
            routinePhaseIndex = 1,
            workSection = RoutineWorkSection.Main.name,
            classification = WorkoutSetClassification.TrainingMaxTest.name,
            loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax.name,
            loadPercentage = "100",
        )
        val owner = RoutineBuilderPlacementState(
            key = 10,
            exerciseId = 1,
            exerciseNameSnapshot = "Squat",
            placementKind = RoutinePlacementKind.MainLift.name,
            assistanceRole = RoutineAssistanceRole.MainLift.name,
            trainingMaxValue = "300",
            sets = sharedMain + trainingMaxTest,
        )
        val repeated = owner.copy(
            key = 20,
            sets = sharedMain.mapIndexed { index, set -> set.copy(key = 21L + index) },
        )
        val state = RoutineBuilderState(
            programKind = RoutineProgramKind.FiveThreeOne.name,
            programPhaseCount = 2,
            programPhaseRoles = listOf(
                RoutineProgramPhaseRole.Standard.name,
                RoutineProgramPhaseRole.TrainingMaxTest.name,
            ),
            days = listOf(
                RoutineBuilderDayState(1, "Squat A", listOf(owner)),
                RoutineBuilderDayState(2, "Squat B", listOf(repeated)),
            ),
            nextKey = 30,
        )

        val updated = state.updateProgramPlacement(20) { placement ->
            placement.copy(
                trainingMaxValue = "305",
                sets = placement.sets.map { set ->
                    if (set.routinePhaseIndex == 0) set.copy(repetitionsMin = "4") else set
                },
            )
        }
        val placements = updated.days.flatMap(RoutineBuilderDayState::placements)
        val testSets = placements.flatMap { placement ->
            placement.sets.filter { it.classification == WorkoutSetClassification.TrainingMaxTest.name }
                .map { placement.key to it }
        }

        assertEquals(1, testSets.size)
        assertEquals(10L, testSets.single().first)
        assertEquals(1, testSets.single().second.routinePhaseIndex)
        assertTrue(placements.all { placement ->
            placement.sets.single { it.routinePhaseIndex == 0 }.repetitionsMin == "4"
        })
        assertEquals(
            placements.sumOf { it.sets.size },
            placements.flatMap { it.sets }.map { it.key }.distinct().size,
        )
    }

    @Test
    fun choosingAnAssistanceRoleClassifiesRequiredSetsAndRemovesIncompatibleJokers() {
        val placement = RoutineBuilderPlacementState(
            key = 1,
            exerciseId = 2,
            exerciseNameSnapshot = "Rows",
            sets = listOf(
                RoutineBuilderSetState(3, workSection = RoutineWorkSection.Unspecified.name),
                RoutineBuilderSetState(4, workSection = RoutineWorkSection.Optional.name, optionalWorkKind = RoutineOptionalWorkKind.Joker.name),
            ),
        )
        val pull = placement.withAssistanceRole(RoutineAssistanceRole.Pull)
        assertEquals(RoutineAssistanceRole.Pull.name, pull.assistanceRole)
        assertEquals(RoutinePlacementKind.Assistance.name, pull.placementKind)
        assertEquals(RoutineAssistanceCategory.Pull.name, pull.assistanceCategory)
        assertEquals(RoutineWorkSection.Assistance.name, pull.sets.first().workSection)
        assertEquals(1, pull.sets.size)
        assertFalse(pull.jokerSetsEnabled)
        assertEquals(RoutineMainWorkScheme.Unspecified.name, pull.mainWorkScheme)
        assertEquals(RoutineSupplementalScheme.None.name, pull.supplementalScheme)

        val general = pull.withAssistanceRole(RoutineAssistanceRole.Unspecified)
        assertEquals(RoutinePlacementKind.General.name, general.placementKind)
        assertEquals(RoutineAssistanceCategory.Unspecified.name, general.assistanceCategory)
        assertEquals(RoutineWorkSection.Unspecified.name, general.sets.first().workSection)
    }

    @Test
    fun asynchronouslyCreatedStandardLiftsFillOnlyEmptyWizardSelections() {
        val created = listOf(
            10L to "Squat",
            11L to "Bench Press",
            12L to "Deadlift",
            13L to "Overhead Press",
        )
        assertEquals(
            listOf(10L, 11L, 12L, 13L),
            fillEmptyFiveThreeOneLiftSelections(List(4) { 0L }, created),
        )

        val deliberateCustomSquat = 99L
        assertEquals(
            listOf(deliberateCustomSquat, 11L, 12L, 13L),
            fillEmptyFiveThreeOneLiftSelections(
                listOf(deliberateCustomSquat, 0L, 0L, 0L),
                listOf(deliberateCustomSquat to "Safety Bar Good Morning") + created,
                manuallySelectedRoleIndices = setOf(0),
            ),
        )
        assertFalse(FiveThreeOneLiftRole.Press.matchesExerciseName("Leg Press"))
        assertTrue(FiveThreeOneLiftRole.Press.matchesExerciseName("Overhead Press"))

        assertEquals(
            listOf(10L, 11L, 12L, 13L),
            fillEmptyFiveThreeOneLiftSelections(
                currentIds = listOf(11L, 0L, 0L, 0L), // Bench was an automatic Squat fallback.
                candidates = created,
                manuallySelectedRoleIndices = emptySet(),
            ),
        )
    }

    @Test
    fun recentMaxSuggestionCopiesOnceIntoAStableExplicitTrainingMax() {
        val entry = FiveThreeOneTrainingMaxEntryState(
            explicitTrainingMax = "240",
            recentMaxOrEstimatedOneRepMax = "300",
            trainingMaxPercentage = "85",
        )
        assertEquals(255.0, entry.suggestionOrNull(loadIncrement = 5.0) ?: 0.0, 0.0001)

        val applied = entry.applySuggestion(loadIncrement = 5.0)
        assertEquals("255", applied.explicitTrainingMax)
        val changedSource = applied.copy(recentMaxOrEstimatedOneRepMax = "400")
        assertEquals("255", changedSource.explicitTrainingMax)
        assertEquals("235", entry.copy(trainingMaxPercentage = "79").applySuggestion(5.0).explicitTrainingMax)
        assertEquals("240", entry.copy(trainingMaxPercentage = "101").applySuggestion(5.0).explicitTrainingMax)
    }

    @Test
    fun customProgramPhasesCopyRemoveAndReindexPrescriptionsSafely() {
        fun placement(key: Long, exerciseId: Long, setKeyBase: Long) = RoutineBuilderPlacementState(
            key = key,
            exerciseId = exerciseId,
            exerciseNameSnapshot = "Lift $exerciseId",
            sets = listOf(
                RoutineBuilderSetState(setKeyBase, routinePhaseIndex = 0, repetitionsMin = "5"),
                RoutineBuilderSetState(setKeyBase + 1, routinePhaseIndex = 1, repetitionsMin = "3"),
                RoutineBuilderSetState(setKeyBase + 2, routinePhaseIndex = 2, repetitionsMin = "1"),
                RoutineBuilderSetState(setKeyBase + 3, routinePhaseIndex = 3, repetitionsMin = "5"),
                RoutineBuilderSetState(setKeyBase + 4, routinePhaseIndex = null, repetitionsMin = "10"),
            ),
        )
        val initial = RoutineBuilderState(
            programKind = RoutineProgramKind.FiveThreeOne.name,
            programPhaseCount = 4,
            programPhaseLabels = listOf("5s", "3s", "5/3/1", "Deload"),
            programPhaseRoles = listOf("Standard", "Leader", "Anchor", "Deload"),
            trainingMaxAdvanceAfterPhaseIndices = setOf(3),
            currentProgramPhaseIndexHint = 2,
            days = listOf(
                RoutineBuilderDayState(1, "A", listOf(placement(10, 100, 20))),
                RoutineBuilderDayState(2, "B", listOf(placement(11, 101, 30))),
            ),
            nextKey = 100,
        )
        assertEquals(2, initial.updateProgramPhaseMetadata(0, label = "Renamed 5s").currentProgramPhaseIndexHint)

        val added = initial.addProgramPhase(sourcePhaseIndex = 1)
            .updateProgramPhaseMetadata(4, "Leader 2", RoutineProgramPhaseRole.Leader, advancesTrainingMax = true)
        assertEquals(5, added.programPhaseCount)
        assertEquals("Leader 2", added.programPhaseLabels.last())
        assertEquals(RoutineProgramPhaseRole.Leader.name, added.programPhaseRoles.last())
        assertEquals(setOf(3, 4), added.trainingMaxAdvanceAfterPhaseIndices)
        assertEquals(2, added.days.flatMap { it.placements }.sumOf { placement -> placement.sets.count { it.routinePhaseIndex == 4 } })
        assertTrue(added.days.flatMap { it.placements }.all { placement ->
            placement.sets.indexOfFirst { it.routinePhaseIndex == 4 } < placement.sets.indexOfFirst { it.routinePhaseIndex == null }
        })
        val allAddedSetKeys = added.days.flatMap { it.placements }.flatMap { it.sets }.map { it.key }
        assertEquals(allAddedSetKeys.size, allAddedSetKeys.distinct().size)

        val reordered = added.moveProgramPhase(4, 1)
        assertEquals(listOf("5s", "Leader 2", "3s", "5/3/1", "Deload"), reordered.programPhaseLabels)
        assertEquals(setOf(1, 4), reordered.trainingMaxAdvanceAfterPhaseIndices)
        assertEquals(3, reordered.currentProgramPhaseIndexHint)
        assertTrue(reordered.days.flatMap { it.placements }.all { placement ->
            placement.sets.count { it.routinePhaseIndex == 1 } == 1
        })

        val removed = added.removeProgramPhase(1)
        assertEquals(4, removed.programPhaseCount)
        assertEquals(listOf("5s", "5/3/1", "Deload", "Leader 2"), removed.programPhaseLabels)
        assertEquals(setOf(2, 3), removed.trainingMaxAdvanceAfterPhaseIndices)
        assertEquals(1, removed.currentProgramPhaseIndexHint)
        assertTrue(removed.days.flatMap { it.placements }.flatMap { it.sets }
            .all { it.routinePhaseIndex == null || it.routinePhaseIndex in 0 until removed.programPhaseCount })
        assertEquals(2, removed.days.flatMap { it.placements }.sumOf { placement -> placement.sets.count { it.routinePhaseIndex == null } })
    }

    @Test
    fun phasePolicyRegenerationKeepsLeaderAnchorAndOtherPhasesIndependent() {
        val initial = buildFiveThreeOneProgramState(
            current = RoutineBuilderState(nextKey = 1),
            layout = FiveThreeOneProgramLayout.FourDay,
            lifts = FiveThreeOneLiftRole.entries.mapIndexed { index, role ->
                FiveThreeOneProgramLift(
                    role = role,
                    exerciseId = (index + 1).toLong(),
                    exerciseName = role.label,
                    trainingMax = 200.0 + index * 25.0,
                    unitId = "pound",
                    loadIncrement = 5.0,
                    cycleIncrement = if (role in setOf(FiveThreeOneLiftRole.Squat, FiveThreeOneLiftRole.Deadlift)) 10.0 else 5.0,
                )
            },
            mainScheme = FiveThreeOneMainScheme.Classic,
            supplement = FiveThreeOneSupplement.BoringButBig,
            jokerSetsEnabled = true,
        )
        assertTrue(initial.days.flatMap { it.placements }.all { placement ->
            placement.sets.any {
                it.workSection == RoutineWorkSection.Supplemental.name && it.routinePhaseIndex == null
            }
        })

        val withLeader = initial.applyFiveThreeOnePhasePolicy(
            phaseIndex = 0,
            mainWorkScheme = RoutineMainWorkScheme.FivesPro,
            supplementalScheme = RoutineSupplementalScheme.BoringButBig,
            jokerEnabled = false,
        )
        val customized = withLeader.applyFiveThreeOnePhasePolicy(
            phaseIndex = 2,
            mainWorkScheme = RoutineMainWorkScheme.ClassicPrSet,
            supplementalScheme = RoutineSupplementalScheme.FirstSetLast,
            jokerEnabled = true,
        )
        val firstLiftId = customized.days.first().placements.first().exerciseId
        val secondLiftId = customized.days[1].placements.first().exerciseId
        val firstLiftPlacement = customized.days.first().placements.first()
        val detailed = customized.updateProgramPlacement(firstLiftPlacement.key) { placement ->
            placement.copy(sets = placement.sets.map { set ->
                if (set.routinePhaseIndex == 2 && set.workSection == RoutineWorkSection.Supplemental.name) {
                    set.copy(note = "User tempo note", restSeconds = "75")
                } else set
            })
        }
        val jokerDisabled = detailed.applyFiveThreeOnePhasePolicy(
            phaseIndex = 2,
            mainWorkScheme = RoutineMainWorkScheme.ClassicPrSet,
            supplementalScheme = RoutineSupplementalScheme.FirstSetLast,
            jokerEnabled = false,
            exerciseId = firstLiftId,
        )
        assertTrue(jokerDisabled.days.first().placements.first().sets.any {
            it.routinePhaseIndex == 2 && it.workSection == RoutineWorkSection.Supplemental.name &&
                it.note == "User tempo note" && it.restSeconds == "75"
        })
        val perLift = customized.applyFiveThreeOnePhasePolicy(
            phaseIndex = 1,
            mainWorkScheme = RoutineMainWorkScheme.FivesPro,
            supplementalScheme = RoutineSupplementalScheme.SecondSetLast,
            jokerEnabled = false,
            exerciseId = firstLiftId,
        )
        assertEquals(RoutineMainWorkScheme.FivesPro, perLift.fiveThreeOnePhasePolicy(1, firstLiftId)?.mainWorkScheme)
        assertEquals(
            RoutineMainWorkScheme.ClassicPrSet,
            perLift.fiveThreeOnePhasePolicy(1, secondLiftId)?.mainWorkScheme,
        )
        val structuredProgram = initial.copy(programKind = RoutineProgramKind.FiveThreeOne.name)
            .applyFiveThreeOnePhasePolicy(
                phaseIndex = 0,
                mainWorkScheme = RoutineMainWorkScheme.FivesPro,
                supplementalScheme = RoutineSupplementalScheme.FirstSetLast,
                jokerEnabled = false,
            )
        assertEquals(RoutineMainWorkScheme.FivesPro, structuredProgram.fiveThreeOnePhasePolicy(0)?.mainWorkScheme)

        assertEquals(
            FiveThreeOnePhasePolicyState(
                RoutineMainWorkScheme.FivesPro,
                RoutineSupplementalScheme.BoringButBig,
                jokerEnabled = false,
            ),
            customized.fiveThreeOnePhasePolicy(0),
        )
        assertEquals(
            FiveThreeOnePhasePolicyState(
                RoutineMainWorkScheme.ClassicPrSet,
                RoutineSupplementalScheme.FirstSetLast,
                jokerEnabled = true,
            ),
            customized.fiveThreeOnePhasePolicy(2),
        )
        assertEquals(RoutineSupplementalScheme.BoringButBig, customized.fiveThreeOnePhasePolicy(1)?.supplementalScheme)
        assertEquals(RoutineSupplementalScheme.BoringButBig, customized.fiveThreeOnePhasePolicy(3)?.supplementalScheme)
        assertTrue(customized.days.flatMap { it.placements }.all { placement ->
            placement.sets.none {
                it.workSection == RoutineWorkSection.Supplemental.name && it.routinePhaseIndex == null
            }
        })
        val explicitDeloadSupplement = customized.applyFiveThreeOnePhasePolicy(
            phaseIndex = 3,
            mainWorkScheme = RoutineMainWorkScheme.ClassicMinimumReps,
            supplementalScheme = RoutineSupplementalScheme.FirstSetLast,
            jokerEnabled = false,
        )
        assertEquals(
            RoutineSupplementalScheme.FirstSetLast,
            explicitDeloadSupplement.fiveThreeOnePhasePolicy(3)?.supplementalScheme,
        )
        assertTrue(explicitDeloadSupplement.days.flatMap { it.placements }.all { placement ->
            placement.sets.count {
                it.routinePhaseIndex == 3 && it.workSection == RoutineWorkSection.Supplemental.name
            } == 5
        })
        assertTrue(customized.days.flatMap { it.placements }.all { placement ->
            val leaderMain = placement.sets.filter {
                it.routinePhaseIndex == 0 && it.workSection == RoutineWorkSection.Main.name
            }
            val anchorMain = placement.sets.filter {
                it.routinePhaseIndex == 2 && it.workSection == RoutineWorkSection.Main.name
            }
            val anchorSupplemental = placement.sets.filter {
                it.routinePhaseIndex == 2 && it.workSection == RoutineWorkSection.Supplemental.name
            }
            leaderMain.size == 3 && leaderMain.all {
                it.repetitionsMin == "5" && it.classification != WorkoutSetClassification.Amrap.name &&
                    it.mainWorkScheme == RoutineMainWorkScheme.FivesPro.name
            } && anchorMain.last().classification == WorkoutSetClassification.Amrap.name &&
                anchorSupplemental.size == 5 && anchorSupplemental.all {
                    it.repetitionsMin == "5" && it.loadPercentage == anchorMain.first().loadPercentage &&
                        it.supplementalScheme == RoutineSupplementalScheme.FirstSetLast.name
                } && placement.sets.count {
                    it.routinePhaseIndex == 2 && it.optionalWorkKind == RoutineOptionalWorkKind.Joker.name
                } == 1
        })
        val keys = customized.days.flatMap { it.placements }.flatMap { it.sets }.map { it.key }
        assertEquals(keys.size, keys.distinct().size)
    }

    @Test
    fun programmedRoutineSummaryShowsActivePhaseWorkInsteadOfCycleAggregate() {
        val sets = (0 until 4).flatMap { phase ->
            List(3) { index ->
                RoutineBuilderSetState(
                    key = (phase * 10 + index).toLong(),
                    repetitionsMin = "5",
                    routinePhaseIndex = phase,
                    workSection = RoutineWorkSection.Main.name,
                    mainWorkScheme = if (phase == 3) {
                        RoutineMainWorkScheme.ClassicMinimumReps.name
                    } else {
                        RoutineMainWorkScheme.ClassicPrSet.name
                    },
                )
            } + List(if (phase == 3) 0 else 5) { index ->
                RoutineBuilderSetState(
                    key = (phase * 10 + index + 4).toLong(),
                    repetitionsMin = "5",
                    routinePhaseIndex = phase,
                    workSection = RoutineWorkSection.Supplemental.name,
                    supplementalScheme = RoutineSupplementalScheme.FirstSetLast.name,
                )
            }
        }

        assertEquals("4 phases · 3–8 active sets/phase · Main + FSL", routineSetSummary(sets))
    }

    @Test
    fun programPhaseMetadataAndNoAdvanceBoundarySurviveBuilderStateRecreation() {
        val handle = SavedStateHandle()
        val viewModel = RoutineBuilderViewModel(handle)
        viewModel.initialize(
            "structured",
            RoutineBuilderState(
                token = "structured",
                programKind = RoutineProgramKind.FiveThreeOne.name,
                programPhaseCount = 2,
                programPhaseLabels = listOf("Leader 1", "TM Test"),
                programPhaseRoles = listOf(RoutineProgramPhaseRole.Leader.name, RoutineProgramPhaseRole.TrainingMaxTest.name),
                trainingMaxAdvanceAfterPhaseIndices = emptySet(),
            ),
        )

        val recreated = RoutineBuilderViewModel(handle).state.value
        assertEquals(listOf("Leader 1", "TM Test"), recreated.programPhaseLabels)
        assertEquals(listOf("Leader", "TrainingMaxTest"), recreated.programPhaseRoles)
        assertTrue(recreated.trainingMaxAdvanceAfterPhaseIndices.isEmpty())
    }

    @Test
    fun kgAndPoundLoadsRoundToPracticalIncrementsOrMachineChoices() {
        assertEquals(85.0, roundedFiveThreeOneLoad(85.1, increment = 2.5), 0.0001)
        assertEquals(190.0, roundedFiveThreeOneLoad(191.25, increment = 5.0), 0.0001)
        assertEquals(87.5, roundedFiveThreeOneLoad(86.0, increment = 2.5, availableLoads = listOf(80.0, 87.5, 95.0)), 0.0001)
        assertEquals(170.0, suggestedFiveThreeOneTrainingMax(200.0, increment = 5.0), 0.0001)
    }

    @Test
    fun changingMassUnitsConvertsTrainingMaxIncrementAndAbsoluteSets() {
        val placement = RoutineBuilderPlacementState(
            key = 1,
            exerciseId = 2,
            exerciseNameSnapshot = "Squat",
            trainingMaxValue = "220",
            trainingMaxUnitId = "pound",
            cycleIncrementValue = "5",
            sets = listOf(
                RoutineBuilderSetState(key = 3, load = "110"),
                RoutineBuilderSetState(
                    key = 4,
                    loadPrescriptionType = RoutineLoadPrescriptionType.PercentTrainingMax.name,
                    loadPercentage = "85",
                ),
            ),
        )

        val converted = placement.withProgramMassUnit("kilogram")

        assertEquals("kilogram", converted.trainingMaxUnitId)
        assertEquals("99.75", converted.trainingMaxValue)
        assertEquals("2.5", converted.cycleIncrementValue)
        assertEquals("50", converted.sets.first().load)
        assertEquals("85", converted.sets.last().loadPercentage)
    }

    @Test
    fun seventhWeekPresetsHaveExactEditableMatrices() {
        val deload = fiveThreeOneSeventhWeekSetPlans(FiveThreeOneSeventhWeekProtocol.Deload)
        assertEquals(listOf(70.0, 80.0, 90.0, 100.0), deload.map(FiveThreeOneSetPlan::percentageOfTrainingMax))
        assertEquals(listOf(5, 3, 1, 1), deload.map(FiveThreeOneSetPlan::repetitions))
        assertEquals(5, deload[1].repetitionsMax)

        val tmTest = fiveThreeOneSeventhWeekSetPlans(FiveThreeOneSeventhWeekProtocol.TrainingMaxTest)
        assertEquals(listOf(5, 5, 5, 3), tmTest.map(FiveThreeOneSetPlan::repetitions))
        assertEquals(5, tmTest.last().repetitionsMax)
        assertEquals(WorkoutSetClassification.TrainingMaxTest, tmTest.last().classification)

        val prTest = fiveThreeOneSeventhWeekSetPlans(FiveThreeOneSeventhWeekProtocol.PersonalRecordTest)
        assertTrue(prTest.last().amrap)
        assertEquals(100.0, prTest.last().percentageOfTrainingMax, 0.0001)
    }

    @Test
    fun applyingSeventhWeekPresetOptsInOnlyTheSelectedLegacyPhase() {
        val legacy = RoutineBuilderState(
            programKind = RoutineProgramKind.FiveThreeOne.name,
            programPhaseCount = 2,
            programPhaseLabels = listOf("Legacy deload A", "Legacy deload B"),
            programPhaseRoles = listOf(
                RoutineProgramPhaseRole.Deload.name,
                RoutineProgramPhaseRole.Deload.name,
            ),
            programTemplateKey = RoutineProgramTemplateKey.FiveThreeOneBeginners.name,
            programTemplateRevision = 1,
        )

        val updated = legacy.applyFiveThreeOneSeventhWeekProtocol(
            phaseIndex = 0,
            protocol = FiveThreeOneSeventhWeekProtocol.Deload,
        )

        assertEquals(2, updated.programTemplateRevision)
        assertEquals(
            listOf(
                RoutineProgramPhaseRole.OncePerLiftDeload.name,
                RoutineProgramPhaseRole.Deload.name,
            ),
            updated.programPhaseRoles,
        )
        assertEquals("Legacy deload B", updated.programPhaseLabels[1])
    }

    @Test
    fun programStructureJokerLadderAndSeventhWeekPresetAreAdditiveAndPhaseScoped() {
        val initial = buildFiveThreeOneProgramState(
            current = RoutineBuilderState(nextKey = 1),
            layout = FiveThreeOneProgramLayout.Custom,
            lifts = listOf(testProgramLift(1, "Zercher Squat", 200.0)),
            mainScheme = FiveThreeOneMainScheme.Classic,
            supplement = FiveThreeOneSupplement.FirstSetLast,
            jokerSetsEnabled = false,
        )
        val ladder = initial.setFiveThreeOneJokerLadder(
            phaseIndex = 0,
            count = 3,
            stepPercent = 10.0,
        )
        val ladderSets = ladder.days.single().placements.single().sets
        val jokers = ladderSets.filter {
            it.routinePhaseIndex == 0 && it.optionalWorkKind == RoutineOptionalWorkKind.Joker.name
        }
        assertEquals(listOf("95", "105", "115"), jokers.map(RoutineBuilderSetState::loadPercentage))
        assertEquals(5, ladderSets.count {
            it.routinePhaseIndex == 0 && it.workSection == RoutineWorkSection.Supplemental.name
        })
        assertTrue(ladderSets.indexOf(jokers.last()) < ladderSets.indexOfFirst {
            it.routinePhaseIndex == 0 && it.workSection == RoutineWorkSection.Supplemental.name
        })

        val phaseOneBefore = ladderSets.filter { it.routinePhaseIndex == 1 }
        val prTest = ladder.applyFiveThreeOneSeventhWeekProtocol(
            phaseIndex = 0,
            protocol = FiveThreeOneSeventhWeekProtocol.PersonalRecordTest,
        )
        val after = prTest.days.single().placements.single().sets
        assertEquals(phaseOneBefore, after.filter { it.routinePhaseIndex == 1 })
        assertEquals(
            listOf("70", "80", "90", "100"),
            after.filter { it.routinePhaseIndex == 0 }.map(RoutineBuilderSetState::loadPercentage),
        )
        assertFalse(after.any {
            it.routinePhaseIndex == 0 && (
                it.workSection == RoutineWorkSection.Supplemental.name ||
                    it.optionalWorkKind == RoutineOptionalWorkKind.Joker.name
                )
        })
        assertEquals("7th Week · PR Test", prTest.programPhaseLabels.first())
        assertEquals(RoutineProgramPhaseRole.OncePerLiftPersonalRecordTest.name, prTest.programPhaseRoles.first())
        assertTrue(0 in prTest.trainingMaxAdvanceAfterPhaseIndices)
    }

    @Test
    fun leaderAnchorPresetSupportsArbitraryLiftsAlternateBbbAndJokerLadder() {
        val bench = testProgramLift(1, "Bench Press", 200.0)
        val deadlift = testProgramLift(2, "Deadlift", 300.0)
        val zercher = testProgramLift(3, "Zercher Squat", 240.0)
        val built = buildFiveThreeOneProgramState(
            RoutineBuilderState(nextKey = 1),
            FiveThreeOneProgramRequest(
                layout = FiveThreeOneProgramLayout.Custom,
                plan = FiveThreeOneProgramPlan.ForeverBbbLeaderAnchor,
                lifts = listOf(bench, deadlift, zercher),
                mainScheme = FiveThreeOneMainScheme.Classic,
                supplement = FiveThreeOneSupplement.BoringButBig,
                closingProtocol = FiveThreeOneSeventhWeekProtocol.TrainingMaxTest,
                jokerLadder = FiveThreeOneJokerLadder(count = 3, stepPercent = 5.0),
                classicFinalSetAmrap = true,
                boringButBigPercent = 50.0,
                progressionMode = com.whip.app.domain.RoutineProgressionMode.PerformanceInformed,
                bbbLiftByMainExerciseId = mapOf(
                    bench.exerciseId to deadlift.exerciseId,
                    deadlift.exerciseId to zercher.exerciseId,
                    zercher.exerciseId to bench.exerciseId,
                ),
            ),
        )

        assertEquals(11, built.programPhaseCount)
        assertEquals(setOf(2, 6, 10), built.trainingMaxAdvanceAfterPhaseIndices)
        assertEquals(
            listOf(
                "Leader",
                "Leader",
                "Leader",
                "Leader",
                "Leader",
                "Leader",
                "OncePerLiftDeload",
                "Anchor",
                "Anchor",
                "Anchor",
                "OncePerLiftTrainingMaxTest",
            ),
            built.programPhaseRoles,
        )
        assertEquals(listOf("Bench Press", "Deadlift", "Zercher Squat"), built.days.map(RoutineBuilderDayState::name))

        val benchDay = built.days.first()
        val benchMain = benchDay.placements.first { it.placementKind == RoutinePlacementKind.MainLift.name }
        val alternateBbb = benchDay.placements.first { it.placementKind == RoutinePlacementKind.Supplemental.name }
        assertEquals(deadlift.exerciseId, alternateBbb.exerciseId)
        assertEquals("300", alternateBbb.trainingMaxValue)
        assertEquals(30, alternateBbb.sets.count { it.workSection == RoutineWorkSection.Supplemental.name })
        assertTrue(benchMain.sets.none {
            it.routinePhaseIndex in 0..5 && it.workSection == RoutineWorkSection.Supplemental.name
        })
        assertEquals(3, benchMain.sets.count {
            it.routinePhaseIndex == 7 && it.optionalWorkKind == RoutineOptionalWorkKind.Joker.name
        })
        assertEquals(listOf("90", "95", "100"), benchMain.sets.filter {
            it.routinePhaseIndex == 7 && it.optionalWorkKind == RoutineOptionalWorkKind.Joker.name
        }.map(RoutineBuilderSetState::loadPercentage))
        assertEquals(5, benchMain.sets.count {
            it.routinePhaseIndex == 7 && it.supplementalScheme == RoutineSupplementalScheme.FirstSetLast.name
        })
        assertTrue(benchMain.sets.none {
            it.routinePhaseIndex == 6 && it.optionalWorkKind == RoutineOptionalWorkKind.Joker.name
        })
        assertEquals(
            listOf("70", "80", "90", "100"),
            benchMain.sets.filter { it.routinePhaseIndex == 6 }.map(RoutineBuilderSetState::loadPercentage),
        )

        val leaderPolicy = built.fiveThreeOnePhasePolicy(0, bench.exerciseId)
        assertEquals(RoutineSupplementalScheme.BoringButBig, leaderPolicy?.supplementalScheme)
        assertEquals(deadlift.exerciseId, leaderPolicy?.alternateSupplementalExerciseId)
        assertEquals("Deadlift", leaderPolicy?.alternateSupplementalExerciseName)
        assertTrue(
            built.fiveThreeOnePhasePrescriptionSummary(0, bench.exerciseId)
                .any { it.startsWith("Supplemental · Deadlift (alternate exercise)") },
        )

        val alternatePhaseBefore = alternateBbb.sets.filter { it.routinePhaseIndex == 0 }
        val mainEdited = built.applyFiveThreeOnePhasePolicy(
            phaseIndex = 0,
            mainWorkScheme = RoutineMainWorkScheme.ClassicMinimumReps,
            supplementalScheme = RoutineSupplementalScheme.BoringButBig,
            jokerEnabled = false,
            exerciseId = bench.exerciseId,
        )
        val preservedAlternate = mainEdited.days.first().placements.first {
            it.placementKind == RoutinePlacementKind.Supplemental.name
        }
        assertEquals(alternatePhaseBefore, preservedAlternate.sets.filter { it.routinePhaseIndex == 0 })
        assertEquals(
            deadlift.exerciseId,
            mainEdited.fiveThreeOnePhasePolicy(0, bench.exerciseId)?.alternateSupplementalExerciseId,
        )

        val changedSupplemental = mainEdited.applyFiveThreeOnePhasePolicy(
            phaseIndex = 0,
            mainWorkScheme = RoutineMainWorkScheme.ClassicMinimumReps,
            supplementalScheme = RoutineSupplementalScheme.FirstSetLast,
            jokerEnabled = false,
            exerciseId = bench.exerciseId,
        )
        assertTrue(changedSupplemental.days.first().placements.first {
            it.placementKind == RoutinePlacementKind.Supplemental.name
        }.sets.none { it.routinePhaseIndex == 0 })
        assertEquals(
            RoutineSupplementalScheme.FirstSetLast,
            changedSupplemental.fiveThreeOnePhasePolicy(0, bench.exerciseId)?.supplementalScheme,
        )
        assertNull(changedSupplemental.fiveThreeOnePhasePolicy(0, bench.exerciseId)?.alternateSupplementalExerciseId)
    }

    @Test
    fun beginnersSeventhWeekProtocolsKeepRepeatedTemplatesSynchronizedAndBalanceExecutionOwners() {
        val lifts = FiveThreeOneLiftRole.entries.mapIndexed { index, role ->
            FiveThreeOneProgramLift(
                role = role,
                exerciseId = (index + 1).toLong(),
                exerciseName = role.label,
                trainingMax = 200.0 + index * 20.0,
                unitId = "pound",
                loadIncrement = 5.0,
                cycleIncrement = 5.0,
            )
        }

        FiveThreeOneSeventhWeekProtocol.entries.forEach { protocol ->
            val built = buildFiveThreeOneProgramState(
                RoutineBuilderState(nextKey = 1),
                FiveThreeOneProgramRequest(
                    layout = FiveThreeOneProgramLayout.Beginners,
                    plan = FiveThreeOneProgramPlan.SingleCycle,
                    lifts = lifts,
                    mainScheme = FiveThreeOneMainScheme.Classic,
                    supplement = FiveThreeOneSupplement.FirstSetLast,
                    closingProtocol = protocol,
                    jokerLadder = FiveThreeOneJokerLadder(),
                    classicFinalSetAmrap = true,
                    boringButBigPercent = 50.0,
                    progressionMode = com.whip.app.domain.RoutineProgressionMode.Standard,
                ),
            )

            assertEquals(
                mapOf(1L to 0, 2L to 2, 3L to 1, 4L to 1),
                balancedOncePerLiftDayOwners(
                    built.days.map { day ->
                        day.placements.filter { it.placementKind == RoutinePlacementKind.MainLift.name }
                            .map(RoutineBuilderPlacementState::exerciseId)
                    },
                ),
            )
            lifts.forEach { lift ->
                val placements = built.days.flatMap(RoutineBuilderDayState::placements)
                    .filter {
                        it.exerciseId == lift.exerciseId &&
                            it.placementKind == RoutinePlacementKind.MainLift.name
                    }
                val activeByPlacement = placements.map { placement ->
                    placement.sets.filter { it.routinePhaseIndex == 3 }
                }
                assertTrue(activeByPlacement.all { sets ->
                    sets.map { it.loadPercentage } == if (
                        protocol == FiveThreeOneSeventhWeekProtocol.TrainingMaxTest &&
                        sets.none { it.classification == WorkoutSetClassification.TrainingMaxTest.name }
                    ) listOf("70", "80", "90") else listOf("70", "80", "90", "100")
                })
                val nonTestTemplates = activeByPlacement.map { sets ->
                    sets.filterNot { it.classification == WorkoutSetClassification.TrainingMaxTest.name }
                        .map { it.copy(key = 0) }
                }
                assertEquals(1, nonTestTemplates.distinct().size)
                if (protocol == FiveThreeOneSeventhWeekProtocol.TrainingMaxTest) {
                    assertEquals(1, activeByPlacement.flatten().count {
                        it.classification == WorkoutSetClassification.TrainingMaxTest.name
                    })
                }
            }

            val reapplied = built.applyFiveThreeOneSeventhWeekProtocol(3, protocol)
            assertEquals(2, reapplied.programTemplateRevision)
            assertEquals(RoutineProgramTemplateKey.FiveThreeOneBeginners.name, reapplied.programTemplateKey)
            lifts.forEach { lift ->
                val repeated = reapplied.days.flatMap(RoutineBuilderDayState::placements)
                    .filter {
                        it.exerciseId == lift.exerciseId &&
                            it.placementKind == RoutinePlacementKind.MainLift.name
                    }
                val nonTestTemplates = repeated.map { placement ->
                    placement.sets.filter {
                        it.routinePhaseIndex == 3 &&
                            it.classification != WorkoutSetClassification.TrainingMaxTest.name
                    }.map { it.copy(key = 0) }
                }
                assertEquals(1, nonTestTemplates.distinct().size)
            }

            val firstSquat = reapplied.days.first().placements.first { it.exerciseId == 1L }
            val tmEdited = reapplied.updateProgramPlacement(firstSquat.key) {
                it.copy(trainingMaxValue = "205")
            }
            val repeatedSquat = tmEdited.days.flatMap(RoutineBuilderDayState::placements)
                .filter { it.exerciseId == 1L && it.placementKind == RoutinePlacementKind.MainLift.name }
            assertTrue(repeatedSquat.all { it.trainingMaxValue == "205" })
            assertEquals(1, repeatedSquat.map { placement ->
                placement.sets.filterNot {
                    it.classification == WorkoutSetClassification.TrainingMaxTest.name
                }.map { it.copy(key = 0) }
            }.distinct().size)
        }
    }

    @Test
    fun generatedAssistanceIsDistinctEditableWorkOnEveryDay() {
        val built = buildFiveThreeOneProgramState(
            RoutineBuilderState(),
            FiveThreeOneProgramRequest(
                layout = FiveThreeOneProgramLayout.Custom,
                plan = FiveThreeOneProgramPlan.SingleCycle,
                lifts = listOf(testProgramLift(1, "Zercher Squat", 200.0)),
                mainScheme = FiveThreeOneMainScheme.Classic,
                supplement = FiveThreeOneSupplement.FirstSetLast,
                closingProtocol = FiveThreeOneSeventhWeekProtocol.Deload,
                jokerLadder = FiveThreeOneJokerLadder(),
                classicFinalSetAmrap = true,
                boringButBigPercent = 50.0,
                progressionMode = com.whip.app.domain.RoutineProgressionMode.Standard,
                assistance = listOf(
                    FiveThreeOneAssistanceChoice(RoutineAssistanceCategory.Push, 10, "Push-up"),
                    FiveThreeOneAssistanceChoice(RoutineAssistanceCategory.Pull, 11, "Row"),
                    FiveThreeOneAssistanceChoice(RoutineAssistanceCategory.SingleLegCore, 12, "Ab Wheel"),
                ),
            ),
        )

        val assistance = built.days.single().placements.filter {
            it.placementKind == RoutinePlacementKind.Assistance.name
        }
        assertEquals(3, assistance.size)
        assertTrue(assistance.all { placement ->
            placement.sets.size == 3 && placement.sets.all {
                it.workSection == RoutineWorkSection.Assistance.name && it.repetitionsMin == "10"
            }
        })
    }

    @Test
    fun assistanceSuggestionsUseSignalsAndNeverInventMissingCategories() {
        val push = testExercise(10, "Dumbbell Press", "chest shoulders", favorite = true)
        val pull = testExercise(11, "Chest Supported Row", "upper back")
        val core = testExercise(12, "Ab Wheel", "core")
        val archived = testExercise(13, "Pull-up", "lats", archived = true)
        val unsupported = testExercise(
            14,
            "Plank",
            "core",
            trackingType = ExerciseTrackingType.DurationOnly,
        )
        val unselectedMainLift = testExercise(15, "Overhead Press", "shoulders triceps")

        val suggested = suggestFiveThreeOneAssistance(
            listOf(push, pull, core, archived, unsupported, unselectedMainLift),
            excludedExerciseIds = setOf(core.id),
        )

        assertEquals(push.id, suggested.getValue(RoutineAssistanceCategory.Push).first().id)
        assertEquals(pull.id, suggested.getValue(RoutineAssistanceCategory.Pull).first().id)
        assertTrue(suggested.getValue(RoutineAssistanceCategory.SingleLegCore).isEmpty())
        assertTrue(suggested.values.flatten().none {
            it.archived || it.id == core.id || it.id == unsupported.id || it.id == unselectedMainLift.id
        })
    }

    private fun testProgramLift(id: Long, name: String, trainingMax: Double) = FiveThreeOneProgramLift(
        role = null,
        exerciseId = id,
        exerciseName = name,
        trainingMax = trainingMax,
        unitId = "pound",
        loadIncrement = 5.0,
        cycleIncrement = 5.0,
    )

    private fun testExercise(
        id: Long,
        name: String,
        muscles: String,
        favorite: Boolean = false,
        archived: Boolean = false,
        trackingType: ExerciseTrackingType = ExerciseTrackingType.WeightReps,
    ) = Exercise(
        id = id,
        uuid = "exercise-$id",
        name = name,
        trackingType = trackingType,
        notes = "",
        equipment = "",
        primaryMuscles = muscles,
        secondaryMuscles = "",
        weightUnitId = "pound",
        weightIncrement = 5.0,
        repetitionIncrement = 1,
        defaultRestSeconds = 60,
        defaultGraphMetric = "EstimatedOneRepMax",
        oneRepMaxFormula = EstimatedOneRepMaxFormula.Epley,
        barWeightKg = null,
        availablePlatesKg = emptyList(),
        includeInVolume = true,
        includeInPersonalRecords = true,
        bodyweightLoadPolicy = BodyweightLoadPolicy.ExternalWeightOnly,
        effectiveBodyweightPercent = 100.0,
        showRpe = null,
        showRir = null,
        showTempo = null,
        favorite = favorite,
        position = id.toInt(),
        archived = archived,
        createdAtMillis = 1,
        updatedAtMillis = 1,
        loadInterpretation = LoadInterpretation.Total,
    )
}
