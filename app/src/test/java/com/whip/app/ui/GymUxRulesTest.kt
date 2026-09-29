package com.whip.app.ui

import com.whip.app.core.OperationStatus
import com.whip.app.core.WhipResult
import com.whip.app.core.TrackedGymRecord
import com.whip.app.core.isSupportedFor
import com.whip.app.domain.BodyweightLoadPolicy
import com.whip.app.domain.EstimatedOneRepMaxFormula
import com.whip.app.domain.Exercise
import com.whip.app.domain.GymGraphRange
import com.whip.app.domain.ExerciseTrackingType
import com.whip.app.domain.LoadInterpretation
import com.whip.app.domain.GymMachine
import com.whip.app.domain.MachineLoadType
import com.whip.app.domain.PersonalRecord
import com.whip.app.domain.PersonalRecordType
import com.whip.app.domain.RoutineOptionalWorkKind
import com.whip.app.domain.RoutineWorkSection
import com.whip.app.domain.WorkoutExercise
import com.whip.app.domain.WorkoutExerciseCopyBoundary
import com.whip.app.domain.WorkoutExerciseOutcome
import com.whip.app.domain.WorkoutFinishBoundary
import com.whip.app.domain.WorkoutLayoutSnapshot
import com.whip.app.domain.WorkoutStructureBoundary
import com.whip.app.domain.WorkoutGroup
import com.whip.app.domain.WorkoutGroupType
import com.whip.app.domain.WorkoutSession
import com.whip.app.domain.WorkoutSessionState
import com.whip.app.domain.WorkoutSet
import com.whip.app.domain.WorkoutSetCopyBoundary
import com.whip.app.domain.WorkoutSetClassification
import java.time.LocalDate
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class GymUxRulesTest {
    @Test
    fun progressStartsFromPerformedHistoryAndSettingsSearchNamesExactControls() {
        val exercises = (1L..5L).map { testExercise(it, "Exercise $it", "", "") }
        val finished = WorkoutSession(1, "session", "Training", "", Instant.EPOCH, Instant.EPOCH.plusSeconds(60),
            LocalDate.of(2026, 9, 29), "UTC", WorkoutSessionState.Finished, false, null, null, false, 1, 2)
        val later = finished.copy(id = 2, uuid = "later", startedAt = Instant.EPOCH.plusSeconds(100))
        val firstPlacement = WorkoutExercise(1, "placement", 1, 3, 0, "", null, 1, 1)
        val lastPlacement = firstPlacement.copy(id = 2, sessionId = 2, exerciseId = 4)
        val placements = listOf(firstPlacement, lastPlacement)
        val scopedPlacements = listOf(firstPlacement.copy(exerciseId = 4, machineProfileUuidSnapshot = "old"),
            lastPlacement.copy(machineProfileUuidSnapshot = "recent"))
        val performed = performanceSet(1).copy(workoutExerciseId = 1)
        val latest = performed.copy(id = 2, workoutExerciseId = 2)
        assertEquals("recent", mostRecentlyPerformedPlacement(setOf(4), listOf(finished, later), scopedPlacements, listOf(performed, latest))?.machineProfileUuidSnapshot)
        assertEquals("old", mostRecentlyPerformedPlacement(setOf(4), listOf(finished, later), scopedPlacements, listOf(performed, latest.copy(completed = false)))?.machineProfileUuidSnapshot)
        assertEquals(null, mostRecentlyPerformedPlacement(setOf(4), listOf(finished, later),
            listOf(scopedPlacements.first(), lastPlacement), listOf(performed, latest))?.machineProfileUuidSnapshot)
        assertEquals(4L, mostRecentlyPerformedExerciseId(exercises, listOf(finished, later), placements, listOf(performed, latest)))
        assertEquals(3L, mostRecentlyPerformedExerciseId(exercises, listOf(finished, later), placements, listOf(performed, latest.copy(completed = false))))
        assertEquals(3L, mostRecentlyPerformedExerciseId(exercises, listOf(finished, later.copy(state = WorkoutSessionState.Active)), placements, listOf(performed, latest)))
        assertEquals(3L, mostRecentlyPerformedExerciseId(exercises, listOf(finished, later), placements, listOf(performed, latest.copy(deletedAtMillis = 5))))
        assertEquals(1L, mostRecentlyPerformedExerciseId(exercises, emptyList(), placements, listOf(performed)))
        val matureSessions = (1L..500L).map { id -> finished.copy(id = id, uuid = "session-$id",
            startedAt = Instant.EPOCH.plusSeconds(id * 3600), localDate = finished.localDate.minusDays(500 - id)) }
        val maturePlacements = matureSessions.map { firstPlacement.copy(id = it.id, uuid = "placement-${it.id}", sessionId = it.id) }
        val matureSets = maturePlacements.flatMap { placement -> List(24) { position ->
            performed.copy(id = placement.id * 24 + position, uuid = "set-${placement.id}-$position",
                workoutExerciseId = placement.id, position = position)
        } }
        assertEquals(12_000, matureSets.size)
        assertEquals(3L, mostRecentlyPerformedExerciseId(exercises, matureSessions, maturePlacements, matureSets))
        val points = com.whip.app.domain.buildExerciseGraph(exercises[2], matureSessions, maturePlacements,
            matureSets, com.whip.app.domain.GymGraphMetric.MaxWeight)
        assertEquals(500, points.size)
        assertEquals(500L, points.last().sourceSessionId)
        val corrected = matureSets.map { if (it.workoutExerciseId == 500L) it.copy(canonicalWeightKg = 110.0) else it }
        val changedPoints = com.whip.app.domain.buildExerciseGraph(exercises[2], matureSessions, maturePlacements,
            corrected, com.whip.app.domain.GymGraphMetric.MaxWeight)
        assertEquals(110.0, changedPoints.last().value, 0.0)
        assertEquals(points.dropLast(1), changedPoints.dropLast(1))
        assertEquals(SettingsSearchEntries.size, SettingsSearchEntries.map { it.anchor }.distinct().size)
        assertEquals(listOf("setting-gym-mass"), searchSettings("gym pounds").map { it.anchor })
        assertEquals(listOf("setting-cutoff"), searchSettings("late cutoff").map { it.anchor })
        assertEquals(listOf("setting-csv"), searchSettings("export spreadsheet").map { it.anchor })
        assertTrue(searchSettings("not-a-whip-setting").isEmpty())
    }

    @Test
    fun explicitWorkoutSetChoiceRetainsOptionalGatesAndReturnsToQueue() {
        val exercise = testExercise(1, "Squat", "", "")
        val placement = WorkoutExercise(1, "placement", 1, 1, 0, "", null, 1, 1)
        val queued = performanceSet(1).copy(id = 1, completed = false)
        val chosen = queued.copy(id = 2, position = 1)
        val joker = queued.copy(id = 3, position = 2, optionalWorkKindSnapshot = RoutineOptionalWorkKind.Joker,
            workSectionSnapshot = RoutineWorkSection.Optional)
        val item = WorkoutExerciseUi(placement, exercise, listOf(queued, chosen, joker), emptyList(), 0, null, null)
        assertEquals(2L, selectedWorkoutExecutionSet(listOf(item), 2, emptySet())?.second?.id)
        assertNull(selectedWorkoutExecutionSet(listOf(item), 3, emptySet()))
        assertEquals(3L, selectedWorkoutExecutionSet(listOf(item), 3, setOf(3))?.second?.id)
        val completed = item.copy(sets = listOf(queued, chosen.copy(completed = true), joker))
        assertNull(selectedWorkoutExecutionSet(listOf(completed), 2, emptySet()))
        assertEquals(1L, selectNextWorkoutSet(listOf(completed))?.second?.id)
        assertNull(selectedWorkoutExecutionSet(listOf(item.copy(workoutExercise = placement.copy(
            outcome = WorkoutExerciseOutcome.Removed))), 2, emptySet()))
        assertNull(selectedWorkoutExecutionSet(listOf(item.copy(sets = listOf(chosen.copy(deletedAtMillis = 4)))), 2, emptySet()))
    }

    @Test
    fun previousSetSuggestionsPreserveUnitsMeaningAndPerformedEvidence() {
        val source = WorkoutExercise(1, "source", 1, 1, 0, "", null, 1, 1,
            loadInterpretationSnapshot = LoadInterpretation.PerHand, exerciseWeightUnitSnapshot = "pound")
        val receiving = source.copy(id = 2, uuid = "current", sessionId = 2, exerciseWeightUnitSnapshot = "kilogram")
        val previous = performanceSet(1).copy(enteredWeight = 100.0, enteredWeightUnitId = "pound",
            canonicalWeightKg = 90.718474, enteredDistance = 1.0, enteredDistanceUnitId = "mile",
            canonicalDistanceMetres = 1609.344)
        assertTrue(compatibleQuickSetHistory(receiving, source))
        val converted = requireNotNull(convertedQuickSetSuggestion(previous, source, "kilogram", "kilometre"))
        assertEquals(45.359237, requireNotNull(converted.enteredWeight), 0.000001)
        assertEquals(1.609344, requireNotNull(converted.enteredDistance), 0.000001)
        assertEquals("kilogram", converted.enteredWeightUnitId)
        assertEquals("kilometre", converted.enteredDistanceUnitId)
        assertFalse(compatibleQuickSetHistory(receiving.copy(loadInterpretationSnapshot = LoadInterpretation.Total), source))
        assertFalse(compatibleQuickSetHistory(receiving.copy(trackingTypeSnapshot = ExerciseTrackingType.RepsOnly), source))
        assertFalse(compatibleQuickSetHistory(receiving.copy(machineProfileUuidSnapshot = "other"), source))
        val legacy = requireNotNull(convertedQuickSetSuggestion(previous.copy(enteredWeight = null), source, "kilogram", "kilometre"))
        assertEquals(45.359237, requireNotNull(legacy.enteredWeight), 0.000001)
        val ordinal = source.copy(machineLoadTypeSnapshot = MachineLoadType.Level,
            loadInterpretationSnapshot = LoadInterpretation.OrdinalSetting)
        val level = requireNotNull(convertedQuickSetSuggestion(previous.copy(machineLoadValue = 7.0), ordinal, "kilogram", "kilometre"))
        assertNull(level.enteredWeight)
        assertEquals(7.0, requireNotNull(level.machineLoadValue), 0.0)
        assertNull(convertedQuickSetSuggestion(previous.copy(completed = false), source, "kilogram", "kilometre"))
        assertNull(convertedQuickSetSuggestion(previous.copy(deletedAtMillis = 9), source, "kilogram", "kilometre"))
        val history = (1L..20L).map { previous.copy(id = it, completedAtMillis = it) } +
            previous.copy(id = 21, completed = false, completedAtMillis = 21)
        val summary = summarizePreviousSets(history, source.id)
        assertEquals(20, summary.totalCount)
        assertEquals(12, summary.sets.size)
        assertEquals(20L, summary.latestPerformedSet?.id)
    }

    @Test
    fun workoutLaunchChoicesAndRoutineSearchKeepAuthoredScope() {
        fun routine(id: Long, pinned: Boolean = false) = com.whip.app.domain.GymRoutine(id, "r$id", "Plan $id", "steady", id.toInt(), false, pinned, 1, 1)
        val first = routine(1)
        val recent = routine(2)
        val pinned = routine(3, true)
        val archived = routine(4, true).copy(archived = true)
        val session = WorkoutSession(1, "session", "Training", "", Instant.EPOCH, Instant.EPOCH.plusSeconds(60),
            LocalDate.of(2026, 9, 28), "UTC", WorkoutSessionState.Finished, false, null, null, false, 1, 2, sourceRoutineId = recent.id)
        assertEquals(listOf(pinned.id, recent.id), workoutQuickRoutines(GymUiState(
            routines = listOf(first, recent, pinned, pinned, archived), history = listOf(session))).map { it.id })
        val monday = com.whip.app.domain.RoutineDay(1, "day1", first.id, "Monday", 0, 1, 1)
        val friday = monday.copy(id = 2, uuid = "day2", name = "Friday", position = 1)
        assertEquals(monday, routineStartDay(first, listOf(monday)))
        assertNull(routineStartDay(first, listOf(monday, friday)))
        assertEquals(friday, routineStartDay(first.copy(programKind = com.whip.app.domain.RoutineProgramKind.Custom,
            nextProgramDayPosition = 1), listOf(friday, monday)))
        val exercise = testExercise(9, "Goblet Squat", "", "").copy(archived = true)
        val placement = com.whip.app.domain.RoutineExercise(7, "p7", monday.id, exercise.id, 0, "", null, false, 1, 1)
        val result = WhipSearchResult(SearchDomain.Routine, first.id, first.name, first.notes,
            searchText = first.searchText(listOf(monday), listOf(placement), listOf(exercise)))
        assertTrue(result.matchesQuery("MONday squat steady"))
        assertFalse(result.matchesQuery("friday"))
        assertFalse(result.copy(id = recent.id, title = recent.name,
            searchText = recent.searchText(listOf(monday), listOf(placement), listOf(exercise))).matchesQuery("squat"))
    }

    @Test
    fun clockTickPreservesProjectedTotalsAndOnlyUpdatesTime() {
        val library = (1L..200L).map { testExercise(it, "Exercise $it", "", "") }
        val placements = (1L..20L).map { WorkoutExercise(it, "p$it", 1, it, it.toInt(), "", null, 1, 1) }
        val sets = placements.flatMap { placement -> (0..5).map { index ->
            performanceSet(placement.id).copy(id = placement.id * 10 + index, uuid = "s${placement.id}-$index", position = index)
        } }
        val active = WorkoutSession(1, "session", "Training", "", Instant.ofEpochMilli(10_000), null,
            LocalDate.of(2026, 9, 28), "UTC", WorkoutSessionState.Active, false, 70_000, 60, false, 1, 2)
        fun summary(session: WorkoutSession, now: Long, inputs: List<WorkoutSet> = sets) = com.whip.app.domain.calculateWorkoutSummary(
            session, placements, inputs, library.associateBy(Exercise::id), now)
        val state = GymUiState(activeSession = active, exercises = library, summary = summary(active, 20_000))
        listOf(9_000L, 10_000L, 20_000L, 70_000L, 90_000L).forEach { now ->
            val tick = state.withClockTick(now)
            assertEquals(summary(active, now), tick.summary)
            assertEquals(restTimerRemainingSeconds(70_000, now, 60), tick.restSecondsRemaining)
            assertEquals(now, tick.nowMillis)
        }
        val ended = active.copy(endedAt = Instant.ofEpochMilli(50_000))
        assertEquals(summary(ended, 100_000), state.copy(activeSession = ended).withClockTick(100_000).summary)
        val refreshed = summary(active, 20_000, sets.map { it.copy(repetitions = 8) })
        assertEquals(refreshed.copy(elapsedSeconds = 20), state.copy(summary = refreshed).withClockTick(30_000).summary)
        assertNull(state.copy(activeSession = null).withClockTick(30_000).summary)
        assertNull(state.copy(activeSession = null).withClockTick(30_000).restSecondsRemaining)
        repeat(100) { summary(active, 30_000); state.withClockTick(30_000) }
        fun medianNanos(action: () -> Unit): Long = List(7) {
            val start = System.nanoTime()
            repeat(100) { action() }
            (System.nanoTime() - start) / 100
        }.sorted()[3]
        var observed = 0L
        val before = medianNanos { observed += summary(active, 30_000).elapsedSeconds }
        val after = medianNanos { observed += state.withClockTick(30_000).summary!!.elapsedSeconds }
        assertTrue(observed > 0)
        println("Gym clock projection, 200 exercises / 120 sets: median old full calculation $before ns; new clock-only copy $after ns per tick. No frame-rate claim.")
    }

    @Test
    fun trackedMachineRecordsRemainEligibleForArchivedAndRemovedProfiles() {
        val exercise = testExercise(1, "Row", "Machine", "Back")
        val selection = TrackedGymRecord(exercise.uuid, PersonalRecordType.MaxMachineSetting, machineProfileUuid = "level-profile")
        val machine = GymMachine(9, "level-profile", 1, "Cable", "", "", MachineLoadType.Level,
            "", "level", listOf(1.0, 2.0), LoadInterpretation.OrdinalSetting, null, false, 1, 1)
        assertTrue(selection.isSupportedFor(exercise, listOf(machine), emptyList()))
        assertTrue(selection.isSupportedFor(exercise, listOf(machine.copy(archived = true)), emptyList()))
        val record = PersonalRecord(uuid = "level-record", exerciseId = exercise.id, type = selection.type,
            value = 2.0, secondaryValue = null, unitId = "level", sourceSetId = 90, sourceSessionId = 1,
            achievedAtMillis = 2, current = true, imported = false, createdAtMillis = 1, updatedAtMillis = 2,
            machineProfileUuidSnapshot = machine.uuid)
        assertTrue(selection.isSupportedFor(exercise, emptyList(), listOf(record)))
        assertFalse(selection.copy(machineProfileUuid = "other").isSupportedFor(exercise, listOf(machine), listOf(record)))
        assertFalse(selection.isSupportedFor(exercise, emptyList(), emptyList()))
        assertFalse(selection.copy(type = PersonalRecordType.MaxDistance).isSupportedFor(exercise, listOf(machine), listOf(record)))
    }

    @Test
    fun historicalEnteredLoadsConvertBeforeTheirQualifier() {
        val original = performanceSet(2).copy(canonicalWeightKg = 40.0, enteredWeight = 20.0, repetitions = 5)
        val placement = WorkoutExercise(2, "placement", 1, 1, 0, "", null, 1, 1,
            loadInterpretationSnapshot = LoadInterpretation.PerHand)
        listOf(LoadInterpretation.PerHand to "per hand", LoadInterpretation.PerSide to "per side",
            LoadInterpretation.AddedLoad to "added", LoadInterpretation.AssistedSubtraction to "assistance").forEach { (meaning, qualifier) ->
            val label = original.shortLabel("pound", "kilometre", 1, placement.copy(loadInterpretationSnapshot = meaning), "pound")
            assertTrue(label, label.contains("44.1 lb $qualifier"))
            assertFalse(label, label.contains("20 lb"))
        }
        assertTrue(original.copy(enteredWeight = 44.0924524369755, enteredWeightUnitId = "pound")
            .shortLabel("kilogram", "kilometre", 1, placement, "kilogram").contains("20 kg per hand (40 total)"))
        assertTrue(original.shortLabel("pound", "kilometre", 1, placement.copy(loadInterpretationSnapshot = LoadInterpretation.Total), "pound")
            .contains("88.2 lb"))
        assertTrue(original.copy(machineLoadValue = 3.0).shortLabel("pound", "kilometre", 1,
            placement.copy(machineLoadTypeSnapshot = MachineLoadType.Level, machineLevelLabelSnapshot = "level"), "pound").contains("level 3"))
        assertEquals(20.0, original.enteredWeight!!, 0.0)
        assertEquals("kilogram", original.enteredWeightUnitId)
    }

    @Test
    fun sharedWorkoutRetainsArchivedExerciseIdentity() {
        val exercise = testExercise(1, "Archived dumbbell row", "Dumbbell", "Back").copy(archived = true)
        val placement = WorkoutExercise(2, "placement", 1, 1, 0, "", null, 1, 1)
        val session = WorkoutSession(1, "session", "Training", "", Instant.EPOCH, Instant.EPOCH.plusSeconds(60),
            LocalDate.of(2026, 9, 28), "UTC", WorkoutSessionState.Finished, false, null, null, false, 1, 2)
        val text = workoutShareText(session, GymUiState(archivedExercises = listOf(exercise),
            allWorkoutExercises = listOf(placement), allSets = listOf(performanceSet(2))))
        assertTrue(text, text.contains(exercise.name))
        assertTrue(text, text.contains("100 kg"))
    }

    @Test
    fun workoutUndoVisibilityIsScopedToActiveSessionAndDataGeneration() {
        val undo = WorkoutLayoutUndo(
            boundary = WorkoutStructureBoundary(7, "session-a", "fingerprint"),
            snapshot = WorkoutLayoutSnapshot(emptyList(), emptyList(), emptyMap(), emptyList()),
            label = "Undo arrange",
        )

        assertSame(undo, undo.visibleForActiveSession("session-a"))
        assertNull(undo.visibleForActiveSession("session-b"))
        assertNull(undo.visibleForActiveSession(null))
        assertEquals(41L, visibleSkippedOptionalSetId(41L, "session-a", 9L, "session-a", 9L))
        assertNull(visibleSkippedOptionalSetId(41L, "session-a", 9L, "session-b", 9L))
        assertNull(visibleSkippedOptionalSetId(41L, "session-a", 9L, "session-a", 10L))
        assertNull(visibleSkippedOptionalSetId(41L, "session-a", 9L, null, 9L))
    }

    @Test
    fun dataGenerationChangeClosesEveryWorkoutOwnedExerciseEditor() {
        assertTrue(
            shouldCloseWorkoutAuthoredExerciseEditor(
                directWorkoutExerciseEditorOpen = true,
                inlineMachineEditorOpen = false,
                creatingExerciseForMachine = false,
            ),
        )
        assertTrue(
            shouldCloseWorkoutAuthoredExerciseEditor(
                directWorkoutExerciseEditorOpen = false,
                inlineMachineEditorOpen = true,
                creatingExerciseForMachine = true,
            ),
        )
        assertEquals(
            false,
            shouldCloseWorkoutAuthoredExerciseEditor(
                directWorkoutExerciseEditorOpen = false,
                inlineMachineEditorOpen = true,
                creatingExerciseForMachine = false,
            ),
        )
    }

    @Test
    fun historyCopyAndSharedMutationStateBlockCompetingWorkoutActions() {
        assertTrue(hasActiveWorkoutMutation(false, true, true))
        assertTrue(hasActiveWorkoutMutation(false, false, true))
        assertTrue(hasActiveWorkoutMutation(true, false, false))
        assertEquals(false, hasActiveWorkoutMutation(false, false, false))
    }

    @Test
    fun historyCopyAuthorshipCodecPreservesExactRetryIdentityAndRejectsDamage() {
        val expected = HistoryCopyAuthorship(
            boundary = WorkoutExerciseCopyBoundary(
                sourceSessionId = 11,
                sourceSessionUuid = "source-session",
                sourceWorkoutExerciseId = 12,
                sourceWorkoutExerciseUuid = "source-placement",
                sourceWorkoutExerciseUpdatedAtMillis = 13,
                target = WorkoutStructureBoundary(21, "target-session", "target-fingerprint"),
                sourceSets = listOf(
                    WorkoutSetCopyBoundary(31, "source-set-a", 32),
                    WorkoutSetCopyBoundary(33, "source-set-b", 34),
                ),
            ),
            requestedWorkoutExerciseUuid = "requested-placement",
            requestedSetUuids = listOf("requested-set-a", "requested-set-b"),
            dataGeneration = 41,
        )

        val encoded = encodeHistoryCopyAuthorship(expected)
        assertEquals(expected, decodeHistoryCopyAuthorship(encoded))
        assertNull(decodeHistoryCopyAuthorship(encoded.dropLast(1)))
        assertNull(decodeHistoryCopyAuthorship(encoded.toMutableList().also { it[12] = "500" }))
    }

    private val today = LocalDate.of(2026, 8, 22)

    @Test
    fun restTimerNeverRendersAboveTheSelectedDurationWhenTheUiClockIsStale() {
        val selectedDuration = 5 * 60
        val timerStartedAt = 1_000_500L
        val deadline = timerStartedAt + selectedDuration * 1_000L

        assertEquals(
            selectedDuration,
            restTimerRemainingSeconds(
                deadlineMillis = deadline,
                nowMillis = 1_000_000L,
                configuredDurationSeconds = selectedDuration,
            ),
        )
        assertEquals(
            selectedDuration - 1,
            restTimerRemainingSeconds(
                deadlineMillis = deadline,
                nowMillis = timerStartedAt + 1_000L,
                configuredDurationSeconds = selectedDuration,
            ),
        )
        assertEquals(
            selectedDuration,
            restTimerRemainingSeconds(deadline, timerStartedAt + 1L, selectedDuration),
        )
        assertEquals(
            selectedDuration,
            restTimerRemainingSeconds(deadline, timerStartedAt + 999L, selectedDuration),
        )
        assertNull(
            restTimerRemainingSeconds(
                deadlineMillis = deadline,
                nowMillis = deadline,
                configuredDurationSeconds = selectedDuration,
            ),
        )
    }

    @Test
    fun finishReviewMatchesOnlyTheExactSessionIdentityAndRevision() {
        val session = WorkoutSession(
            id = 7,
            uuid = "session-7",
            name = "Workout",
            notes = "",
            startedAt = Instant.ofEpochMilli(1),
            endedAt = null,
            localDate = today,
            zoneId = "UTC",
            state = WorkoutSessionState.Active,
            keepScreenAwake = false,
            restTimerDeadlineMillis = null,
            restTimerDurationSeconds = null,
            archived = false,
            createdAtMillis = 1,
            updatedAtMillis = 1,
            workoutRevision = 4,
        )

        assertTrue(session.matchesFinishReview(WorkoutFinishBoundary(7, "session-7", 4)))
        assertTrue(!session.matchesFinishReview(WorkoutFinishBoundary(7, "session-7", 5)))
        assertTrue(!session.matchesFinishReview(WorkoutFinishBoundary(7, "other-session", 4)))
    }

    @Test
    fun personalRecordRecoveryKeepsAStaleRecordInTheRetrySetAfterItsLastSetWasRemoved() {
        val placement = WorkoutExercise(
            id = 11,
            uuid = "placement-11",
            sessionId = 7,
            exerciseId = 22,
            position = 0,
            notes = "",
            groupId = null,
            createdAtMillis = 1,
            updatedAtMillis = 1,
            loadInterpretationSnapshot = LoadInterpretation.Total,
        )
        val removedLastSet = performanceSet(placement.id).copy(
            completed = true,
            deletedAtMillis = 3,
        )
        val staleRecord = PersonalRecord(
            uuid = "stale-pr",
            exerciseId = placement.exerciseId,
            type = PersonalRecordType.MaxWeight,
            value = 100.0,
            secondaryValue = null,
            unitId = "kilogram",
            sourceSetId = removedLastSet.id,
            sourceSessionId = placement.sessionId,
            achievedAtMillis = 2,
            current = true,
            imported = false,
            createdAtMillis = 2,
            updatedAtMillis = 2,
        )

        val firstAttempt = personalRecordReconciliationExerciseIds(
            listOf(removedLastSet),
            listOf(placement),
            listOf(staleRecord),
        )
        // A failed first rebuild leaves the PR row in Room, so a recreated ViewModel derives
        // the same target and retries instead of losing the exercise from discovery.
        val recreatedAttempt = personalRecordReconciliationExerciseIds(
            listOf(removedLastSet),
            listOf(placement),
            listOf(staleRecord),
        )

        assertEquals(setOf(placement.exerciseId), firstAttempt)
        assertEquals(firstAttempt, recreatedAttempt)
    }

    @Test
    fun relativeGraphRangeAlwaysEndsToday() {
        val result = validateGymGraphRange(GymGraphRange.ThreeMonths, "", "", today)

        assertEquals(today.minusMonths(3), result.from)
        assertEquals(today, result.to)
        assertNull(result.error)
    }

    @Test
    fun customGraphRangeRequiresBothStrictOrderedDates() {
        assertEquals(
            "Enter both From and To dates",
            validateGymGraphRange(GymGraphRange.Custom, "", "", today).error,
        )
        assertEquals(
            "From must use YYYY-MM-DD",
            validateGymGraphRange(GymGraphRange.Custom, "08/01/2026", "2026-08-22", today).error,
        )
        assertEquals(
            "From must be on or before To",
            validateGymGraphRange(GymGraphRange.Custom, "2026-08-23", "2026-08-22", today).error,
        )

        val valid = validateGymGraphRange(GymGraphRange.Custom, "2026-08-01", "2026-08-22", today)
        assertEquals(LocalDate.of(2026, 8, 1), valid.from)
        assertEquals(today, valid.to)
        assertNull(valid.error)
    }

    @Test
    fun positiveNumberListReportsTheExactInvalidToken() {
        val valid = parsePositiveNumberList("45, 25, 10, 2.5", "plate")
        assertEquals(listOf(45.0, 25.0, 10.0, 2.5), valid.values)
        assertNull(valid.error)

        val invalid = parsePositiveNumberList("45, banana, 10", "plate")
        assertTrue(invalid.values.isEmpty())
        assertEquals("Plate 2 (“banana”) must be a positive number", invalid.error)
    }

    @Test
    fun positiveNumberListDoesNotTurnBlankInputIntoUnlimitedInventory() {
        val result = parsePositiveNumberList("   ", "plate")

        assertTrue(result.values.isEmpty())
        assertEquals("Enter at least one plate", result.error)
    }

    @Test
    fun quantityLabelsUseTheSingularOnlyForOne() {
        assertEquals("0 exercises", quantityLabel(0, "exercise"))
        assertEquals("1 exercise", quantityLabel(1, "exercise"))
        assertEquals("2 exercises", quantityLabel(2, "exercise"))
        assertEquals("1 entry", quantityLabel(1, "entry", "entries"))
        assertEquals("2 entries", quantityLabel(2, "entry"))
        assertEquals("2 boxes", quantityLabel(2, "box"))
    }

    @Test
    fun gymPersistenceResultPreservesFailureForTheCallingEditor() {
        val cause = IllegalStateException("write failed")
        val failure = gymPersistenceResult(
            false,
            OperationStatus.Failed("Could not persist", cause),
        ) as WhipResult.Failure

        assertEquals("Could not persist", failure.message)
        assertSame(cause, failure.cause)
        assertTrue(gymPersistenceResult(true, OperationStatus.Succeeded("Saved")) is WhipResult.Success)
    }

    @Test
    fun sharedExerciseSearchScalesAndMatchesMeaningfulMetadata() {
        val library = (1L..1_000L).map { id ->
            testExercise(
                id = id,
                name = "Exercise $id",
                equipment = if (id == 999L) "Cable tower" else "Dumbbell",
                primaryMuscles = if (id == 999L) "Rear deltoid" else "Chest",
            )
        }

        val match = library.filter { exerciseMatchesQuery(it, "cable rear") }
        assertEquals(listOf(999L), match.map(Exercise::id))
        assertTrue(exerciseMatchesQuery(library.first(), "weight repetitions"))
        assertTrue(exerciseMatchesQuery(library.first(), "garage stack", "Garage Stack v2"))
        val result = WhipSearchResult(SearchDomain.Exercise, library.first().id, library.first().name, "",
            searchText = library.first().searchText("Garage Stack v2"))
        assertTrue(result.matchesQuery("garage stack"))
        assertFalse(result.matchesQuery("cable rear"))
    }

    @Test
    fun nextSetSelectionHonorsGroupRotationAndFallsBackWhenThatMemberIsDone() {
        val group = WorkoutGroup(50, "group", 1, "Superset", WorkoutGroupType.Superset, 0, 1, 1)
        fun item(id: Long, position: Int, completed: Boolean = false): WorkoutExerciseUi {
            val exercise = testExercise(id, "Exercise $id", "Barbell", "Chest")
            val placement = WorkoutExercise(
                id = id,
                uuid = "placement-$id",
                sessionId = 1,
                exerciseId = id,
                position = position,
                notes = "",
                groupId = group.id,
                createdAtMillis = 1,
                updatedAtMillis = 1,
                loadInterpretationSnapshot = LoadInterpretation.Total,
            )
            val set = WorkoutSet(
                id = id * 10,
                uuid = "set-$id",
                workoutExerciseId = id,
                position = 0,
                classification = WorkoutSetClassification.Working,
                planned = false,
                completed = completed,
                canonicalWeightKg = 50.0,
                enteredWeight = 50.0,
                enteredWeightUnitId = "kilogram",
                repetitions = 5,
                canonicalDistanceMetres = null,
                enteredDistance = null,
                enteredDistanceUnitId = null,
                durationSeconds = null,
                bodyweightKg = null,
                note = "",
                rpe = null,
                rir = null,
                tempo = "",
                restSeconds = 120,
                completedAtMillis = 2.takeIf { completed }?.toLong(),
                deletedAtMillis = null,
                createdAtMillis = 1,
                updatedAtMillis = 1,
            )
            return WorkoutExerciseUi(placement, exercise, listOf(set), emptyList(), 0, group, null)
        }

        val first = item(1, 0)
        val second = item(2, 1)
        assertEquals(second.workoutExercise.id, selectNextWorkoutSet(listOf(first, second), mapOf(group.id to second.workoutExercise.id))?.first?.workoutExercise?.id)

        val completedFirst = item(1, 0, completed = true)
        assertEquals(second.workoutExercise.id, selectNextWorkoutSet(listOf(completedFirst, second), mapOf(group.id to first.workoutExercise.id))?.first?.workoutExercise?.id)

        val independent = item(3, 1).let { candidate ->
            candidate.copy(
                workoutExercise = candidate.workoutExercise.copy(position = 1, groupId = null),
                group = null,
            )
        }
        val trailingGroupMember = second.copy(workoutExercise = second.workoutExercise.copy(position = 2))
        assertEquals(
            trailingGroupMember.workoutExercise.id,
            selectNextWorkoutSet(
                listOf(first, independent, trailingGroupMember),
                mapOf(group.id to trailingGroupMember.workoutExercise.id),
            )?.first?.workoutExercise?.id,
        )
        assertEquals(second.workoutExercise.id, selectNextWorkoutSet(listOf(completedFirst, second))?.first?.workoutExercise?.id)
    }

    @Test
    fun programmedExerciseFinishesMainThenSupplementalBeforeChangingStationsAndOptionalRequiresAcceptance() {
        fun item(id: Long, position: Int, section: RoutineWorkSection, completed: Boolean = false): WorkoutExerciseUi {
            val exercise = testExercise(id, "Exercise $id", "Barbell", "Chest")
            val placement = WorkoutExercise(
                id = id,
                uuid = "placement-$id",
                sessionId = 1,
                exerciseId = id,
                position = position,
                notes = "",
                groupId = null,
                createdAtMillis = 1,
                updatedAtMillis = 1,
                loadInterpretationSnapshot = LoadInterpretation.Total,
            )
            val set = WorkoutSet(
                id = id * 10,
                uuid = "set-$id",
                workoutExerciseId = id,
                position = 0,
                classification = WorkoutSetClassification.Working,
                planned = true,
                completed = completed,
                canonicalWeightKg = 50.0,
                enteredWeight = 50.0,
                enteredWeightUnitId = "kilogram",
                repetitions = 5,
                canonicalDistanceMetres = null,
                enteredDistance = null,
                enteredDistanceUnitId = null,
                durationSeconds = null,
                bodyweightKg = null,
                note = "",
                rpe = null,
                rir = null,
                tempo = "",
                restSeconds = 120,
                completedAtMillis = 2L.takeIf { completed },
                deletedAtMillis = null,
                createdAtMillis = 1,
                updatedAtMillis = 1,
                workSectionSnapshot = section,
                optionalWorkKindSnapshot = if (section == RoutineWorkSection.Optional) RoutineOptionalWorkKind.Joker else RoutineOptionalWorkKind.None,
            )
            return WorkoutExerciseUi(placement, exercise, listOf(set), emptyList(), 0, null, null)
        }

        val supplemental = item(1, 0, RoutineWorkSection.Supplemental)
        val main = item(2, 1, RoutineWorkSection.Main)
        assertEquals(supplemental.workoutExercise.id, selectNextWorkoutSet(listOf(supplemental, main))?.first?.workoutExercise?.id)

        val completedMain = main.sets.single().copy(completed = true, completedAtMillis = 2L)
        val sameExerciseSupplemental = supplemental.sets.single().copy(
            id = 21L,
            workoutExerciseId = main.workoutExercise.id,
        )
        val squatStation = main.copy(sets = listOf(completedMain, sameExerciseSupplemental))
        val benchMain = item(4, 2, RoutineWorkSection.Main)
        assertEquals(
            squatStation.workoutExercise.id,
            selectNextWorkoutSet(listOf(squatStation, benchMain))?.first?.workoutExercise?.id,
        )

        val optional = item(3, 2, RoutineWorkSection.Optional)
        assertEquals(supplemental.workoutExercise.id, selectNextWorkoutSet(listOf(supplemental, optional))?.first?.workoutExercise?.id)
        val optionalWithCompletedMain = optional.copy(
            sets = listOf(
                optional.sets.single().copy(
                    id = 29L,
                    position = 0,
                    completed = true,
                    completedAtMillis = 2L,
                    workSectionSnapshot = RoutineWorkSection.Main,
                    optionalWorkKindSnapshot = RoutineOptionalWorkKind.None,
                ),
                optional.sets.single().copy(position = 1),
            ),
        )
        assertEquals(
            optional.workoutExercise.id,
            selectPendingOptionalWorkoutSet(listOf(optionalWithCompletedMain))?.first?.workoutExercise?.id,
        )
        assertNull(selectPendingOptionalWorkoutSet(listOf(optional)))
        assertNull(selectNextWorkoutSet(listOf(optional)))
        assertEquals(optional.workoutExercise.id, selectNextWorkoutSet(listOf(optional), acceptedOptionalSetIds = setOf(30L))?.first?.workoutExercise?.id)
        assertTrue(!optional.sets.single().isIncompleteRequiredWork())
        val workoutOnlyOptional = optional.copy(
            sets = listOf(optional.sets.single().copy(optionalWorkKindSnapshot = RoutineOptionalWorkKind.None)),
        )
        assertNull(selectPendingOptionalWorkoutSet(listOf(workoutOnlyOptional)))
        assertEquals(
            workoutOnlyOptional.workoutExercise.id,
            selectNextWorkoutSet(listOf(workoutOnlyOptional))?.first?.workoutExercise?.id,
        )
        assertEquals(
            workoutOnlyOptional.workoutExercise.id,
            selectRequestedWorkoutSet(
                listOf(main, workoutOnlyOptional),
                workoutOnlyOptional.workoutExercise.id,
            )?.first?.workoutExercise?.id,
        )
    }

    @Test
    fun jokerLadderRequiresTargetsAndStopsOnSkipFailureOrHighEffort() {
        val exercise = testExercise(1, "Squat", "Barbell", "Legs")
        val placement = WorkoutExercise(
            id = 1,
            uuid = "placement-1",
            sessionId = 1,
            exerciseId = 1,
            position = 0,
            notes = "",
            groupId = null,
            createdAtMillis = 1,
            updatedAtMillis = 1,
        )
        fun set(
            id: Long,
            position: Int,
            section: RoutineWorkSection,
            completed: Boolean,
            deleted: Boolean = false,
            actualReps: Int = 5,
            rpe: Double? = null,
            failure: Boolean = false,
        ) = WorkoutSet(
            id = id,
            uuid = "set-$id",
            workoutExerciseId = 1,
            position = position,
            classification = if (failure) WorkoutSetClassification.Failure else WorkoutSetClassification.Working,
            planned = true,
            completed = completed,
            canonicalWeightKg = 100.0,
            enteredWeight = 100.0,
            enteredWeightUnitId = "kilogram",
            repetitions = actualReps,
            canonicalDistanceMetres = null,
            enteredDistance = null,
            enteredDistanceUnitId = null,
            durationSeconds = null,
            bodyweightKg = null,
            note = "",
            rpe = rpe,
            rir = null,
            tempo = "",
            restSeconds = null,
            completedAtMillis = 2L.takeIf { completed },
            deletedAtMillis = 3L.takeIf { deleted },
            createdAtMillis = 1,
            updatedAtMillis = 1,
            prescribedCanonicalWeightKg = 100.0,
            prescribedRepetitions = 5,
            workSectionSnapshot = section,
            optionalWorkKindSnapshot = if (section == RoutineWorkSection.Optional) RoutineOptionalWorkKind.Joker else RoutineOptionalWorkKind.None,
        )
        val main = set(1, 0, RoutineWorkSection.Main, completed = true)
        val first = set(2, 1, RoutineWorkSection.Optional, completed = false)
        val second = set(3, 2, RoutineWorkSection.Optional, completed = false)
        fun item(sets: List<WorkoutSet>) = WorkoutExerciseUi(placement, exercise, sets, emptyList(), 0, null, null)

        assertEquals(2L, selectPendingOptionalWorkoutSet(listOf(item(listOf(main, first, second))))?.second?.id)
        assertNull(selectPendingOptionalWorkoutSet(listOf(item(listOf(main, first.copy(deletedAtMillis = 3L), second)))))
        assertNull(selectPendingOptionalWorkoutSet(listOf(item(listOf(main, first.copy(completed = true, repetitions = 4), second)))))
        assertNull(selectPendingOptionalWorkoutSet(listOf(item(listOf(main, first.copy(completed = true, classification = WorkoutSetClassification.Failure), second)))))
        assertNull(selectPendingOptionalWorkoutSet(listOf(item(listOf(main, first.copy(completed = true, rpe = 9.0), second)))))
        assertEquals(3L, selectPendingOptionalWorkoutSet(listOf(item(listOf(main, first.copy(completed = true), second))))?.second?.id)
        assertNull(selectPendingOptionalWorkoutSet(listOf(item(listOf(main.copy(repetitions = 4), first, second)))))
    }

    @Test
    fun activePerformanceKeepsCompletedRetiredWorkWithoutResurrectingEmptyPlacements() {
        fun placement(id: Long, outcome: WorkoutExerciseOutcome) = WorkoutExercise(
            id = id,
            uuid = "placement-$id",
            sessionId = 1,
            exerciseId = id,
            position = id.toInt(),
            notes = "",
            groupId = null,
            createdAtMillis = 1,
            updatedAtMillis = 1,
            loadInterpretationSnapshot = LoadInterpretation.Total,
            outcome = outcome,
        )
        val active = placement(1, WorkoutExerciseOutcome.Active)
        val performedRetired = placement(2, WorkoutExerciseOutcome.Substituted)
        val emptyRetired = placement(3, WorkoutExerciseOutcome.Removed)

        assertEquals(
            listOf(active.id, performedRetired.id),
            selectWorkoutPerformancePlacements(
                listOf(active, performedRetired, emptyRetired),
                listOf(performanceSet(workoutExerciseId = performedRetired.id)),
                sessionId = 1,
            ).map(WorkoutExercise::id),
        )
    }

    private fun performanceSet(workoutExerciseId: Long) = WorkoutSet(
        id = 90,
        uuid = "performed-set",
        workoutExerciseId = workoutExerciseId,
        position = 0,
        classification = WorkoutSetClassification.Working,
        planned = false,
        completed = true,
        canonicalWeightKg = 100.0,
        enteredWeight = 100.0,
        enteredWeightUnitId = "kilogram",
        repetitions = 5,
        canonicalDistanceMetres = null,
        enteredDistance = null,
        enteredDistanceUnitId = null,
        durationSeconds = null,
        bodyweightKg = null,
        note = "",
        rpe = null,
        rir = null,
        tempo = "",
        restSeconds = 120,
        completedAtMillis = 2,
        deletedAtMillis = null,
        createdAtMillis = 1,
        updatedAtMillis = 2,
    )

    private fun testExercise(
        id: Long,
        name: String,
        equipment: String,
        primaryMuscles: String,
    ) = Exercise(
        id = id,
        uuid = "exercise-$id",
        name = name,
        trackingType = ExerciseTrackingType.WeightReps,
        notes = "",
        equipment = equipment,
        primaryMuscles = primaryMuscles,
        secondaryMuscles = "",
        weightUnitId = "kilogram",
        weightIncrement = 2.5,
        repetitionIncrement = 1,
        defaultRestSeconds = 120,
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
        favorite = false,
        position = id.toInt(),
        archived = false,
        createdAtMillis = id,
        updatedAtMillis = id,
    )
}
