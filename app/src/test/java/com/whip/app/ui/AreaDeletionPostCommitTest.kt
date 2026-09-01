package com.whip.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AreaDeletionPostCommitTest {
    @Test
    fun ordinaryReconciliationFailuresRemainCommittedWarningsAndLaterStepsStillRun() {
        val completed = mutableListOf<String>()

        val warnings = reconcileCommittedAreaDeletion(
            "Settings cleanup" to { throw IllegalStateException("preferences unavailable") },
            "Focus timer cleanup" to { completed += "focus" },
            "Widget cleanup" to { throw UnsupportedOperationException("widget unavailable") },
        )

        assertEquals(listOf("focus"), completed)
        assertEquals(2, warnings.size)
        assertTrue(warnings.all { it.contains("Area deletion was committed") })
    }

    @Test
    fun fatalReconciliationFailureIsNeverConvertedIntoAWarning() {
        val result = runCatching {
            reconcileCommittedAreaDeletion(
                "Settings cleanup" to { throw AssertionError("corrupt runtime") },
            )
        }

        assertTrue(result.exceptionOrNull() is AssertionError)
    }
}
