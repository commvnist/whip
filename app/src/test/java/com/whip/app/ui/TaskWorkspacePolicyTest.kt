package com.whip.app.ui

import com.whip.app.core.SavedTaskFilter
import com.whip.app.domain.ScheduleKind
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskWorkspacePolicyTest {
    @Test
    fun todayIsTheFirstTaskHeadingFollowedByInbox() {
        assertEquals(
            listOf(
                TaskWorkspaceDestination.Today,
                TaskWorkspaceDestination.Inbox,
                TaskWorkspaceDestination.Upcoming,
                TaskWorkspaceDestination.Anytime,
            ),
            primaryTaskWorkspaceDestinations,
        )
    }

    @Test
    fun taskCreationDefaultsMatchTheWorkspaceThatInvokedThem() {
        assertEquals(TaskPlacement.Inbox, TaskDestination.Inbox.creationPlacement())
        assertEquals(TaskPlacement.Scheduled, TaskDestination.Today.creationPlacement())
        assertEquals(TaskPlacement.Scheduled, TaskDestination.Upcoming.creationPlacement())
        assertEquals(TaskPlacement.Anytime, TaskDestination.Anytime.creationPlacement())
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
    fun legacyFiltersNormalizeInvalidDestinationViewPairs() {
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
            buildQuickAddTaskDraft("Clean room", today, inbox = false, areaId = "personal"),
        )
        assertEquals("Clean room", todayDraft.title)
        assertEquals(ScheduleKind.Once, todayDraft.scheduleKind)
        assertEquals(today, todayDraft.date)
        assertEquals(false, todayDraft.inbox)
        assertEquals("personal", todayDraft.areaId)

        val inboxDraft = requireNotNull(
            buildQuickAddTaskDraft("Research desk", null, inbox = true, areaId = "work"),
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
                inbox = true,
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
    fun blankQuickAddDoesNothing() {
        assertEquals(
            null,
            buildQuickAddTaskDraft("\n  ", null, true, "main"),
        )
    }
}
