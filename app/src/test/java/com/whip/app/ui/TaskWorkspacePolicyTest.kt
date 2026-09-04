package com.whip.app.ui

import com.whip.app.core.SavedTaskFilter
import com.whip.app.domain.RecurrenceUnit
import com.whip.app.domain.ScheduleKind
import com.whip.app.domain.TaskEffort
import com.whip.app.domain.TaskPriority
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskWorkspacePolicyTest {
    @Test
    fun allTaskDestinationsRemainDirectInTheirStableOrder() {
        assertEquals(
            listOf(
                TaskWorkspaceDestination.Today,
                TaskWorkspaceDestination.Inbox,
                TaskWorkspaceDestination.Upcoming,
                TaskWorkspaceDestination.History,
            ),
            primaryTaskWorkspaceDestinations,
        )
    }

    @Test
    fun taskCreationDefaultsMatchTheWorkspaceThatInvokedThem() {
        assertEquals(TaskPlacement.Inbox, TaskDestination.Inbox.creationPlacement())
        assertEquals(TaskPlacement.Scheduled, TaskDestination.Today.creationPlacement())
        assertEquals(TaskPlacement.Scheduled, TaskDestination.Upcoming.creationPlacement())
    }

    @Test
    fun onlyUpcomingOffersPlanningViews() {
        TaskWorkspaceDestination.entries.forEach { destination ->
            val expected = if (destination == TaskWorkspaceDestination.Upcoming) {
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
            assertEquals(destination, destination.toWorkspaceRoute().dataDestination())
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
                val expectedView = destination.toWorkspaceRoute().destination.normalizePlanningView(view)
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
    fun destinationlessFilterDoesNotNavigateButStillUsesList() {
        val normalized = SavedTaskFilter(
            name = "No route",
            planningView = TaskPlanningView.Calendar.name,
        ).normalizedForWorkspace()
        assertEquals("", normalized.destination)
        assertEquals(TaskPlanningView.List.name, normalized.planningView)
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
