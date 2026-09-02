package com.whip.app.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitTimerDomainTest {
    private val boundary = HabitTimerBoundary(7L, "habit-uuid", "timer-session")

    @Test
    fun boundaryAndStartRequestRetainExactActionIdentity() {
        val request = HabitTimerStartRequest(boundary.habitId, boundary.habitUuid, boundary.sessionId)

        assertEquals(7L, request.habitId)
        assertEquals("habit-uuid", request.habitUuid)
        assertEquals("timer-session", request.requestId)
        assertEquals(boundary, HabitTimerBoundary(request.habitId, request.habitUuid, request.requestId))
    }

    @Test
    fun startOutcomesDistinguishCreatedRunningAndConsumedRequests() {
        val started = HabitTimerStartOutcome.Started(boundary, needsReview = true)
        val running = HabitTimerStartOutcome.AlreadyRunning(boundary, needsReview = false)

        assertEquals(boundary, started.boundary)
        assertTrue(started.needsReview)
        assertEquals(boundary, running.boundary)
        assertFalse(running.needsReview)
        assertEquals(HabitTimerStartOutcome.AlreadyResolved, HabitTimerStartOutcome.AlreadyResolved)
    }

    @Test
    fun stopAndReviewOutcomesKeepCanonicalSecondsAndResolutionIntent() {
        val stopped = HabitTimerStopOutcome.Stopped(boundary, canonicalSeconds = 300.0, logId = 11L)
        val review = HabitTimerStopOutcome.ReviewRequired(boundary, estimatedCanonicalSeconds = 295.0)
        val continued = HabitTimerStopOutcome.Continued(boundary, canonicalSeconds = 295.0)
        val completed = HabitTimerStopOutcome.AlreadyCompleted(historyPresent = true)
        val date = LocalDate.of(2026, 9, 2)
        val stopResolution = HabitTimerReviewResolution.StopAndLog(295.0, date)
        val continueResolution = HabitTimerReviewResolution.Continue(295.0)

        assertEquals(300.0, stopped.canonicalSeconds, 0.0)
        assertEquals(11L, stopped.logId)
        assertEquals(295.0, review.estimatedCanonicalSeconds, 0.0)
        assertEquals(boundary, review.boundary)
        assertEquals(295.0, continued.canonicalSeconds, 0.0)
        assertEquals(boundary, continued.boundary)
        assertTrue(completed.historyPresent)
        assertEquals(date, stopResolution.date)
        assertEquals(295.0, stopResolution.canonicalSeconds, 0.0)
        assertEquals(295.0, continueResolution.canonicalSeconds, 0.0)
        assertEquals(HabitTimerReviewResolution.Discard, HabitTimerReviewResolution.Discard)
        assertEquals(HabitTimerStopOutcome.AlreadyDiscarded, HabitTimerStopOutcome.AlreadyDiscarded)
        assertEquals(HabitTimerStopOutcome.Discarded, HabitTimerStopOutcome.Discarded)
        assertEquals(
            listOf("Running", "ReviewRequired", "Completed", "Discarded"),
            HabitTimerSessionState.entries.map(HabitTimerSessionState::name),
        )
    }
}
