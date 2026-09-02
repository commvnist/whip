package com.whip.app.ui

import com.whip.app.core.SavedTaskFilter
import com.whip.app.domain.Area
import com.whip.app.domain.AreaScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhipNavigationPolicyTest {
    @Test
    fun savedTaskFiltersRestoreTheirCompleteAreaScope() {
        assertEquals(AreaScope.All, SavedTaskFilter(name = "All").restoredAreaScope())
        assertEquals(
            AreaScope.One("work"),
            SavedTaskFilter(name = "Work", areaId = "work").restoredAreaScope(),
        )
    }

    @Test
    fun creationAreaDefaultsOnlyWhenScopeOrSoleAreaIsUnambiguous() {
        val work = area("work")
        val home = area("home")
        val archived = area("old", archived = true)

        assertEquals("work", AreaScope.One("work").creationDefaultAreaId(listOf(home, work)))
        assertEquals("work", AreaScope.All.creationDefaultAreaId(listOf(work, archived)))
        assertEquals(null, AreaScope.All.creationDefaultAreaId(listOf(work, home)))
        assertTrue(AreaScope.All.requiresExplicitCreationArea(listOf(work, home)))
        assertFalse(AreaScope.All.requiresExplicitCreationArea(listOf(work, archived)))
    }

    @Test
    fun anyRawUserDataPreventsFirstRunHomeFromReturning() {
        assertTrue(shouldShowHomeGettingStarted(hasAnyUserData = false))
        assertFalse(shouldShowHomeGettingStarted(hasAnyUserData = true))
    }

    @Test
    fun returningHomeOffersBoundedConcreteDestinationsInAttentionOrder() {
        assertEquals(
            listOf(
                HomeResumeDestination.Inbox,
                HomeResumeDestination.Upcoming,
                HomeResumeDestination.Habits,
            ),
            homeResumeDestinations(
                inboxTaskCount = 2,
                upcomingTaskCount = 4,
                habitCount = 3,
                goalCount = 2,
                trackCount = 1,
                gymItemCount = 5,
            ),
        )
        assertEquals(
            listOf(HomeResumeDestination.Tracks, HomeResumeDestination.Gym),
            homeResumeDestinations(0, 0, 0, 0, 2, 1),
        )
        assertTrue(homeResumeDestinations(0, 0, 0, 0, 0, 0).isEmpty())
    }

    @Test
    fun homeSummaryNeverDropsPinnedItemsAndOnlyUsesSpareSlotsForOthers() {
        val items = listOf("pinned-a", "other-a", "pinned-b", "pinned-c", "pinned-d", "other-b")

        assertEquals(
            listOf("pinned-a", "pinned-b", "pinned-c", "pinned-d"),
            pinnedHomeSummary(items, limit = 3) { it.startsWith("pinned") },
        )
        assertEquals(
            listOf("pinned-a", "pinned-b", "other-a"),
            pinnedHomeSummary(items.take(3), limit = 3) { it.startsWith("pinned") },
        )
    }

    @Test
    fun gymHomeCountUsesPinnedShortcutsWhenNoWorkoutIsActive() {
        assertEquals(2, gymHomeItemCount(hasActiveSession = false, pinnedRoutineCount = 2))
        assertEquals(1, gymHomeItemCount(hasActiveSession = true, pinnedRoutineCount = 2))
        assertEquals(0, gymHomeItemCount(hasActiveSession = false, pinnedRoutineCount = 0))
    }

    @Test
    fun globalAddIsRemovedFromTaskSelectionModeOnEveryLayout() {
        assertTrue(
            globalAddAvailable(
                appDestination = AppDestination.Tasks,
                gymDestination = GymDestination.Workout,
                gymRoutineEditorOpen = false,
                taskSelectionMode = false,
            ),
        )
        assertFalse(
            globalAddAvailable(
                appDestination = AppDestination.Tasks,
                gymDestination = GymDestination.Workout,
                gymRoutineEditorOpen = false,
                taskSelectionMode = true,
            ),
        )
        assertFalse(
            globalAddAvailable(
                appDestination = AppDestination.Settings,
                gymDestination = GymDestination.Workout,
                gymRoutineEditorOpen = false,
                taskSelectionMode = false,
            ),
        )
        assertTrue(
            globalAddAvailable(
                appDestination = AppDestination.Gym,
                gymDestination = GymDestination.Routines,
                gymRoutineEditorOpen = false,
                taskSelectionMode = false,
            ),
        )
        assertFalse(
            globalAddAvailable(
                appDestination = AppDestination.Gym,
                gymDestination = GymDestination.Tools,
                gymRoutineEditorOpen = false,
                taskSelectionMode = false,
            ),
        )
        assertFalse(
            globalAddAvailable(
                appDestination = AppDestination.Tracks,
                gymDestination = GymDestination.Workout,
                gymRoutineEditorOpen = false,
                taskSelectionMode = false,
                selectedTrackArchived = true,
            ),
        )
    }

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
        val workouts = WhipSearchEntryContext.Workouts.defaultSearchScope()
        assertEquals("Workouts", workouts.displayLabel(workouts.domains))
        assertEquals("Search workouts", workouts.placeholder(workouts.domains))
        assertEquals("All Whip", workouts.displayLabel(SearchDomain.entries.toSet()))
        assertEquals("Search all Whip", workouts.placeholder(SearchDomain.entries.toSet()))
    }

    @Test
    fun searchActionsNameTheExactScopeTheyOpen() {
        assertEquals("Search All Whip Data", WhipSearchEntryContext.AllWhip.searchActionLabel())
        assertEquals("Search Tasks & Steps", WhipSearchEntryContext.Tasks.searchActionLabel())
        assertEquals("Search Tracks & Entries", WhipSearchEntryContext.Tracks.searchActionLabel())
        assertEquals("Search Exercises", WhipSearchEntryContext.Exercises.searchActionLabel())
        assertEquals("Search Routines", WhipSearchEntryContext.Routines.searchActionLabel())
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


    private fun area(id: String, archived: Boolean = false) = Area(
        id = id,
        name = id,
        colorArgb = null,
        position = 0,
        archived = archived,
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )
}
