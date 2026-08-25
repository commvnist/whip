package com.whip.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CompactItemExpansionStateTest {
    @Test
    fun toggleAllowsMultipleCompactItemsToRemainExpanded() {
        val state = CompactItemExpansionState()

        state.toggle("task:1")
        state.toggle("habit:2")
        assertEquals(setOf("task:1", "habit:2"), state.expandedItemKeys)

        state.toggle("task:1")
        assertEquals(setOf("habit:2"), state.expandedItemKeys)

        state.collapseAll()
        assertEquals(emptySet<String>(), state.expandedItemKeys)
    }

    @Test
    fun automaticExpansionIsAdditiveButDoesNotReopenAfterATabReset() {
        val state = CompactItemExpansionState()

        state.toggle("task:1")
        state.expandAutomatically("goal:timer")
        state.expandAutomatically("habit:timer")

        assertEquals(setOf("task:1", "goal:timer", "habit:timer"), state.expandedItemKeys)

        state.collapseAll()
        state.expandAutomatically("goal:timer")
        state.expandAutomatically("habit:timer")

        assertEquals(emptySet<String>(), state.expandedItemKeys)
    }
}
