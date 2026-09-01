package com.whip.app.reminders

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderBatchSyncTest {
    @Test
    fun oneFailureDoesNotPreventLaterTaskReminderRefreshes() = runBlocking {
        val attempted = mutableListOf<Long>()

        val failures = attemptEveryTaskReminderSync(listOf(3L, 1L, 2L, 2L)) { taskId ->
            attempted += taskId
            if (taskId == 1L) error("simulated scheduler failure")
        }

        assertEquals(listOf(1L, 2L, 3L), attempted)
        assertEquals(listOf(1L), failures)
    }
}
