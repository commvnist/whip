package com.whip.app.ui

internal enum class WhipSearchEntryContext {
    AllWhip,
    Tasks,
    Habits,
    Goals,
    Tracks,
    Gym,
    Exercises,
    Machines,
    Workouts,
    Routines,
}

internal data class WhipSearchScope(
    val label: String,
    val domains: Set<SearchDomain>,
) {
    val isAllWhip: Boolean get() = domains == SearchDomain.entries.toSet()
}

internal fun WhipSearchEntryContext.defaultSearchScope(): WhipSearchScope = when (this) {
    WhipSearchEntryContext.AllWhip -> WhipSearchScope("All Whip", SearchDomain.entries.toSet())
    WhipSearchEntryContext.Tasks -> WhipSearchScope("Tasks & Steps", setOf(SearchDomain.Task))
    WhipSearchEntryContext.Habits -> WhipSearchScope("Habits", setOf(SearchDomain.Habit))
    WhipSearchEntryContext.Goals -> WhipSearchScope("Goals", setOf(SearchDomain.Goal))
    WhipSearchEntryContext.Tracks -> WhipSearchScope("Tracks & Entries", setOf(SearchDomain.Track, SearchDomain.TrackEntry))
    WhipSearchEntryContext.Gym -> WhipSearchScope(
        "Gym",
        setOf(SearchDomain.Exercise, SearchDomain.Machine, SearchDomain.Workout, SearchDomain.Routine),
    )
    WhipSearchEntryContext.Exercises -> WhipSearchScope("Exercises", setOf(SearchDomain.Exercise))
    WhipSearchEntryContext.Machines -> WhipSearchScope("Machines", setOf(SearchDomain.Machine))
    WhipSearchEntryContext.Workouts -> WhipSearchScope("Workouts", setOf(SearchDomain.Workout))
    WhipSearchEntryContext.Routines -> WhipSearchScope("Routines", setOf(SearchDomain.Routine))
}

internal fun GymDestination.searchEntryContext(): WhipSearchEntryContext = when (this) {
    GymDestination.Exercises, GymDestination.Progress, GymDestination.Categories -> WhipSearchEntryContext.Exercises
    GymDestination.Machines -> WhipSearchEntryContext.Machines
    GymDestination.Workout, GymDestination.History -> WhipSearchEntryContext.Workouts
    GymDestination.Routines -> WhipSearchEntryContext.Routines
    GymDestination.Library, GymDestination.Tools -> WhipSearchEntryContext.Gym
}

internal fun AppDestination.searchEntryContext(gymDestination: GymDestination): WhipSearchEntryContext = when (this) {
    AppDestination.Home -> WhipSearchEntryContext.AllWhip
    AppDestination.Tasks -> WhipSearchEntryContext.Tasks
    AppDestination.Habits -> WhipSearchEntryContext.Habits
    AppDestination.Goals -> WhipSearchEntryContext.Goals
    AppDestination.Tracks -> WhipSearchEntryContext.Tracks
    AppDestination.Gym -> gymDestination.searchEntryContext()
    AppDestination.Settings -> WhipSearchEntryContext.AllWhip
}

internal enum class WhipBackAction {
    HideIme,
    DismissTransientSurface,
    CloseSearch,
    ExitSelectionOrReorder,
    NavigateToParent,
    ReturnFromSecondaryDestination,
    NavigateToHome,
    ExitActivity,
}

internal data class WhipBackState(
    val imeVisible: Boolean = false,
    val transientSurfaceOpen: Boolean = false,
    val searchOpen: Boolean = false,
    val selectionOrReorderActive: Boolean = false,
    val childPageOpen: Boolean = false,
    val secondaryDestinationOpen: Boolean = false,
    val atHomeRoot: Boolean = false,
)

internal fun WhipBackState.nextAction(): WhipBackAction = when {
    imeVisible -> WhipBackAction.HideIme
    transientSurfaceOpen -> WhipBackAction.DismissTransientSurface
    searchOpen -> WhipBackAction.CloseSearch
    selectionOrReorderActive -> WhipBackAction.ExitSelectionOrReorder
    childPageOpen -> WhipBackAction.NavigateToParent
    secondaryDestinationOpen -> WhipBackAction.ReturnFromSecondaryDestination
    !atHomeRoot -> WhipBackAction.NavigateToHome
    else -> WhipBackAction.ExitActivity
}
