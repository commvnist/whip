package com.whip.app.ui

import androidx.lifecycle.SavedStateHandle
import com.whip.app.core.RepPrescriptionScheme
import com.whip.app.domain.WorkoutSetClassification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
