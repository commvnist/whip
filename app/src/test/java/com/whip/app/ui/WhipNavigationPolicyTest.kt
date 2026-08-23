package com.whip.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class WhipNavigationPolicyTest {
    @Test
    fun searchEntryContextAlwaysProducesNamedScope() {
        WhipSearchEntryContext.entries.forEach { context ->
            val scope = context.defaultSearchScope()
            check(scope.label.isNotBlank())
            check(scope.domains.isNotEmpty())
        }
        assertEquals(
            SearchDomain.entries.toSet(),
            WhipSearchEntryContext.AllWhip.defaultSearchScope().domains,
        )
        assertEquals(
            setOf(SearchDomain.Task),
            WhipSearchEntryContext.Tasks.defaultSearchScope().domains,
        )
        assertEquals(WhipSearchEntryContext.Exercises, GymDestination.Exercises.searchEntryContext())
        assertEquals(WhipSearchEntryContext.Machines, GymDestination.Machines.searchEntryContext())
        assertEquals(WhipSearchEntryContext.Workouts, GymDestination.History.searchEntryContext())
        assertEquals(WhipSearchEntryContext.Routines, GymDestination.Routines.searchEntryContext())
        assertEquals(
            setOf(SearchDomain.Exercise, SearchDomain.Machine, SearchDomain.Workout, SearchDomain.Routine),
            WhipSearchEntryContext.Gym.defaultSearchScope().domains,
        )
    }

    @Test
    fun backUnwindsHighestTransientLayerFirst() {
        val fullyLayered = WhipBackState(
            imeVisible = true,
            transientSurfaceOpen = true,
            searchOpen = true,
            selectionOrReorderActive = true,
            childPageOpen = true,
            secondaryDestinationOpen = true,
        )
        assertEquals(WhipBackAction.HideIme, fullyLayered.nextAction())
        assertEquals(
            WhipBackAction.DismissTransientSurface,
            fullyLayered.copy(imeVisible = false).nextAction(),
        )
        assertEquals(
            WhipBackAction.CloseSearch,
            fullyLayered.copy(imeVisible = false, transientSurfaceOpen = false).nextAction(),
        )
        assertEquals(
            WhipBackAction.ExitSelectionOrReorder,
            fullyLayered.copy(
                imeVisible = false,
                transientSurfaceOpen = false,
                searchOpen = false,
            ).nextAction(),
        )
    }

    @Test
    fun homeRootIsTheOnlyStateThatExits() {
        assertEquals(WhipBackAction.ExitActivity, WhipBackState(atHomeRoot = true).nextAction())
        assertEquals(WhipBackAction.NavigateToHome, WhipBackState().nextAction())
        assertEquals(
            WhipBackAction.ReturnFromSecondaryDestination,
            WhipBackState(secondaryDestinationOpen = true).nextAction(),
        )
        assertEquals(
            WhipBackAction.NavigateToParent,
            WhipBackState(childPageOpen = true).nextAction(),
        )
    }
}
