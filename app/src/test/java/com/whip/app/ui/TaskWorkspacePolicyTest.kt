package com.whip.app.ui

import com.whip.app.core.SavedTaskFilter
import com.whip.app.domain.OccurrenceState
import com.whip.app.domain.RecurrenceAnchor
import com.whip.app.domain.RecurrenceEnd
import com.whip.app.domain.RecurrenceRule
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.TaskPriority
import com.whip.app.domain.TaskOccurrence
import com.whip.app.domain.TaskStepState
import com.whip.app.domain.TaskStepSnapshot
import com.whip.app.domain.WhipTask
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskWorkspacePolicyTest {
    @Test
    fun collectionOmitsEmptySplitRemnantsButPreservesScheduledAndAuthoredWork() {
        val sunday = LocalDate.of(2026, 9, 27)
        val rule = RecurrenceRule(RecurrenceUnit.Weeks, startDate = sunday,
            weekdays = setOf(DayOfWeek.THURSDAY), end = RecurrenceEnd.OnDate, endDate = sunday.plusDays(3))
        val empty = WhipTask(1, "Weekly task", "", ScheduleKind.Recurring, sunday, rule,
            null, false, false, null, 0, 0)
        val current = empty.copy(id = 2, icon = "📚", date = sunday.plusDays(4),
            recurrence = rule.copy(startDate = sunday.plusDays(4), end = RecurrenceEnd.Never, endDate = null))
        val realEarlierWork = empty.copy(id = 3, recurrence = rule.copy(weekdays = setOf(DayOfWeek.MONDAY)))
        val completionAnchored = empty.copy(id = 4, recurrence = rule.copy(anchor = RecurrenceAnchor.Completion))
        val tasks = listOf(empty, current, realEarlierWork, completionAnchored,
            empty.copy(id = 5), empty.copy(id = 6), empty.copy(id = 7), empty.copy(id = 8))
        val state = buildUiState(
            tasks = tasks,
            occurrences = listOf(
                TaskOccurrence(5, sunday, sunday, OccurrenceState.Completed, 1),
                TaskOccurrence(6, sunday, sunday.plusYears(2), OccurrenceState.Open, null),
            ),
            steps = emptyList(),
            stepStates = listOf(TaskStepState(10, 7, sunday.toEpochDay(), true, 1, "Saved step")),
            stepSnapshots = listOf(TaskStepSnapshot(11, 8, sunday.toEpochDay(), "Snapshot", 0, "", false, null)),
            today = sunday.plusDays(2),
            showAllUpcomingRecurringOccurrences = false,
        )
        val collection = state.collectionTasks()
        assertEquals(listOf(2L, 3L, 4L, 5L, 6L, 7L, 8L), collection.map { it.task.id })
        assertEquals("📚", collection.single { it.task.id == 2L }.task.icon)
    }

    @Test
    fun todaySavedViewsDiscardConflictingDatesButKeepLegitimateNarrowing() {
        for (date in listOf("Today", "NoDate", "Next7Days", "PastScheduled")) {
            val view = SavedTaskFilter("Work", destination = "Today", dateMode = date,
                tags = setOf("work"), maximumDurationMinutes = 15).normalizedForWorkspace()
            assertEquals("Any", view.dateMode)
            assertEquals(setOf("work"), view.tags)
            assertEquals(15, view.maximumDurationMinutes)
        }
        assertEquals("Overdue", SavedTaskFilter("Late", destination = "Today", dateMode = "Overdue").normalizedForWorkspace().dateMode)
        for (destination in listOf("Inbox", "Upcoming", "Completed", "Archived")) {
            assertEquals("Any", SavedTaskFilter("Old", destination = destination, dateMode = "NoDate").normalizedForWorkspace().dateMode)
        }
        assertEquals("Next7Days", SavedTaskFilter("Soon", destination = "Upcoming", dateMode = "Next7Days").normalizedForWorkspace().dateMode)
    }

    @Test
    fun collectionIncludesUnprojectedSeriesAndCountsEachDefinitionOnce() {
        val today = LocalDate.of(2026, 9, 29)
        val base = com.whip.app.domain.WhipTask(1, "Undated", "", ScheduleKind.Anytime, null, null,
            null, false, false, null, 0, 0)
        val series = base.copy(id = 2, title = "Recurring", scheduleKind = ScheduleKind.Recurring,
            recurrence = com.whip.app.domain.RecurrenceRule(RecurrenceUnit.Days, startDate = today.plusYears(1)))
        val scheduled = base.copy(id = 3, scheduleKind = ScheduleKind.Once, date = today.plusYears(2))
        val first = com.whip.app.domain.ScheduledTask(scheduled, scheduled.date, scheduled.date)
        val state = TaskUiState(taskEntities = listOf(base, series, scheduled, base.copy(id = 4, archived = true)),
            upcoming = listOf(first), planning = listOf(first), currentDate = today)
        val collection = state.collectionTasks()
        assertEquals(listOf(1L, 2L, 3L), collection.map { it.task.id })
        assertEquals(false, collection.single { it.task.id == 2L }.matchesTaskDateFilter(SavedTaskFilter("Undated", dateMode = "NoDate"), today, java.time.ZoneOffset.UTC))
        assertEquals(true, collection.single { it.task.id == 1L }.matchesTaskDateFilter(SavedTaskFilter("Undated", dateMode = "NoDate"), today, java.time.ZoneOffset.UTC))
    }

    @Test
    fun dayPlanningKeepsScopeAndTaskFiltersWithoutTodaysScheduledDateConstraint() {
        val filter = SavedTaskFilter(name = "Work", areaId = "work", dateMode = "Today",
            textQuery = "report", tags = setOf("urgent"), destination = "Today")
        val planned = filter.forDayPlanning(TaskDestination.Today)
        assertEquals("Any", planned.dateMode)
        assertEquals(true, planned.inboxOnly)
        assertEquals("Inbox", planned.destination)
        assertEquals(filter.areaId, planned.areaId)
        assertEquals(filter.textQuery, planned.textQuery)
        assertEquals(filter.tags, planned.tags)
        assertEquals("Today", filter.forDayPlanning(TaskDestination.Inbox).dateMode)
    }
    @Test
    fun allTaskDestinationsRemainDirectInTheirStableOrder() {
        assertEquals(
            listOf(
                TaskWorkspaceDestination.Tasks,
                TaskWorkspaceDestination.Today,
                TaskWorkspaceDestination.History,
            ),
            primaryTaskWorkspaceDestinations,
        )
    }

    @Test
    fun taskCreationDefaultsMatchTheWorkspaceThatInvokedThem() {
        assertEquals(TaskPlacement.Inbox, TaskDestination.Inbox.creationPlacement())
        assertEquals(TaskPlacement.Scheduled, TaskDestination.Today.creationPlacement())
        assertEquals(TaskPlacement.Inbox, TaskDestination.Upcoming.creationPlacement())
        assertEquals(TaskPlacement.Inbox, TaskDestination.All.creationPlacement())
    }

    @Test
    fun onlyCollectionOffersPlanningViews() {
        TaskWorkspaceDestination.entries.forEach { destination ->
            val expected = if (destination == TaskWorkspaceDestination.Tasks) {
                TaskPlanningView.entries
            } else {
                listOf(TaskPlanningView.List)
            }
            assertEquals(destination.name, expected, destination.allowedPlanningViews())
        }
    }

    @Test
    fun historyPreservesCompletedAndArchivedSemantics() {
        TaskDestination.entries.forEach { destination ->
            assertEquals(if (destination in setOf(TaskDestination.Inbox, TaskDestination.Upcoming)) TaskDestination.All else destination, destination.toWorkspaceRoute().dataDestination())
        }
        assertEquals(
            TaskWorkspaceRoute(TaskWorkspaceDestination.History, TaskHistorySection.Completed),
            TaskDestination.Completed.toWorkspaceRoute(),
        )
        assertEquals(
            TaskWorkspaceRoute(TaskWorkspaceDestination.History, TaskHistorySection.Archived),
            TaskDestination.Archived.toWorkspaceRoute(),
        )
    }

    @Test
    fun connectedFiltersNormalizeInvalidDestinationViewPairs() {
        TaskDestination.entries.forEach { destination ->
            TaskPlanningView.entries.forEach { view ->
                val normalized = SavedTaskFilter(
                    name = "${destination.name}-${view.name}",
                    destination = destination.name,
                    planningView = view.name,
                ).normalizedForWorkspace()
                val expectedView = if (destination in setOf(TaskDestination.All, TaskDestination.Upcoming)) view else TaskPlanningView.List
                assertEquals(destination.name, normalized.destination)
                assertEquals(expectedView.name, normalized.planningView)
            }
        }
    }

    @Test
    fun unknownSavedDestinationUsesSafeTodayList() {
        assertEquals(
            SavedTaskFilter(
                name = "Unknown",
                destination = TaskDestination.Today.name,
                planningView = TaskPlanningView.List.name,
            ),
            SavedTaskFilter(
                name = "Unknown",
                destination = "PlannerFromAnOldBuild",
                planningView = TaskPlanningView.Calendar.name,
            ).normalizedForWorkspace(),
        )
    }

    @Test
    fun removedAnytimeSavedDestinationMigratesToInboxList() {
        assertEquals(
            SavedTaskFilter(
                name = "Connected Anytime",
                destination = TaskDestination.Inbox.name,
                planningView = TaskPlanningView.List.name,
            ),
            SavedTaskFilter(
                name = "Connected Anytime",
                destination = "Anytime",
                planningView = TaskPlanningView.Calendar.name,
            ).normalizedForWorkspace(),
        )
    }

    @Test
    fun destinationlessFilterMigratesToCollectionListAndPreservesItsCriteria() {
        val normalized = SavedTaskFilter(
            name = "No route",
            planningView = TaskPlanningView.Calendar.name,
            dateMode = "NoDate",
        ).normalizedForWorkspace()
        assertEquals(TaskDestination.All.name, normalized.destination)
        assertEquals(TaskPlanningView.List.name, normalized.planningView)
        assertEquals("NoDate", normalized.dateMode)
        assertEquals(TaskDestination.Inbox.name, normalized.copy(destination = "", inboxOnly = true).normalizedForWorkspace().destination)
    }

    @Test
    fun quickAddUsesDestinationDefaultsAndArea() {
        val today = LocalDate.of(2026, 8, 22)
        val todayDraft = requireNotNull(
            buildQuickAddTaskDraft("Clean room", today, areaId = "personal"),
        )
        assertEquals("Clean room", todayDraft.title)
        assertEquals(ScheduleKind.Once, todayDraft.scheduleKind)
        assertEquals(today, todayDraft.date)
        assertEquals(false, todayDraft.inbox)
        assertEquals("personal", todayDraft.areaId)

        val inboxDraft = requireNotNull(
            buildQuickAddTaskDraft("Research desk", null, areaId = "work"),
        )
        assertEquals(ScheduleKind.Anytime, inboxDraft.scheduleKind)
        assertEquals(null, inboxDraft.date)
        assertEquals(true, inboxDraft.inbox)
    }

    @Test
    fun quickCaptureIsLiteralAndMultilineCreatesSteps() {
        val draft = requireNotNull(
            buildQuickAddTaskDraft(
                "Prepare report tomorrow\nDraft outline\nReview figures",
                defaultDate = null,
                areaId = "work",
            ),
        )
        assertEquals("Prepare report tomorrow", draft.title)
        assertEquals(ScheduleKind.Anytime, draft.scheduleKind)
        assertEquals(null, draft.date)
        assertEquals(true, draft.inbox)
        assertEquals(listOf("Draft outline", "Review figures"), draft.steps.map { it.title })
    }

    @Test
    fun enabledSmartCaptureAppliesOnlyRecognizedAssumptions() {
        val today = LocalDate.of(2026, 8, 25)
        val draft = requireNotNull(
            buildQuickAddTaskDraft(
                capture = "Prepare report every 2 weeks on 2026-09-01 deadline 2026-10-01",
                defaultDate = null,
                areaId = "work",
                smartCaptureToday = today,
            ),
        )

        assertEquals("Prepare report", draft.title)
        assertEquals(ScheduleKind.Recurring, draft.scheduleKind)
        assertEquals(null, draft.date)
        assertEquals(LocalDate.of(2026, 9, 1), draft.recurrence?.startDate)
        assertEquals(RecurrenceUnit.Weeks, draft.recurrence?.unit)
        assertEquals(2, draft.recurrence?.interval)
        assertEquals(LocalDate.of(2026, 10, 1), draft.deadline)
        assertEquals(false, draft.inbox)
        assertEquals("work", draft.areaId)
    }

    @Test
    fun enabledSmartCapturePersistsNamedWeekdaySchedule() {
        val today = LocalDate.of(2026, 8, 26)
        val draft = requireNotNull(
            buildQuickAddTaskDraft(
                capture = "TRT every Monday and Thursday",
                defaultDate = today,
                areaId = "health",
                smartCaptureToday = today,
            ),
        )

        assertEquals("TRT", draft.title)
        assertEquals(ScheduleKind.Recurring, draft.scheduleKind)
        assertEquals(null, draft.date)
        assertEquals(RecurrenceUnit.Weeks, draft.recurrence?.unit)
        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            draft.recurrence?.weekdays,
        )
        assertEquals(today, draft.recurrence?.startDate)
        assertEquals(false, draft.inbox)
    }

    @Test
    fun enabledSmartCapturePersistsEveryRecognizedPlanningDetail() {
        val today = LocalDate.of(2026, 8, 26)
        val draft = requireNotNull(
            buildQuickAddTaskDraft(
                capture = "Send proposal tomorrow at 9am by next Friday !high for 45m light effort #work remind me",
                defaultDate = null,
                areaId = "main",
                smartCaptureToday = today,
            ),
        )

        assertEquals("Send proposal", draft.title)
        assertEquals(ScheduleKind.Once, draft.scheduleKind)
        assertEquals(LocalDate.of(2026, 8, 27), draft.date)
        assertEquals(LocalDate.of(2026, 8, 28), draft.deadline)
        assertEquals(9 * 60, draft.timeMinutes)
        assertEquals(true, draft.reminderEnabled)
        assertEquals(listOf(0), draft.reminderOffsetsMinutes)
        assertEquals(TaskPriority.High, draft.priority)
        assertEquals(45, draft.durationMinutes)
        assertEquals(TaskEffort.Light, draft.effort)
        assertEquals(setOf("work"), draft.tags)
        assertEquals(false, draft.inbox)
    }

    @Test
    fun enabledSmartCaptureKeepsDestinationDefaultsWhenNothingIsRecognized() {
        val today = LocalDate.of(2026, 8, 25)
        val draft = requireNotNull(
            buildQuickAddTaskDraft(
                capture = "Discuss tomorrow's release",
                defaultDate = today,
                areaId = null,
                smartCaptureToday = today,
            ),
        )

        assertEquals("Discuss tomorrow's release", draft.title)
        assertEquals(ScheduleKind.Once, draft.scheduleKind)
        assertEquals(today, draft.date)
    }

    @Test
    fun blankQuickAddDoesNothing() {
        assertEquals(
            null,
            buildQuickAddTaskDraft("\n  ", null, "main"),
        )
    }
}
