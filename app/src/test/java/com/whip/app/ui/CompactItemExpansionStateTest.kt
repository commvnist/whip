package com.whip.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompactItemExpansionStateTest {
    @Test
    fun toggleKeepsAtMostOneCompactItemExpanded() {
        val state = CompactItemExpansionState()

        state.toggle("task:1")
        assertEquals("task:1", state.expandedItemKey)

        state.toggle("habit:2")
        assertEquals("habit:2", state.expandedItemKey)

        state.toggle("habit:2")
        assertNull(state.expandedItemKey)
    }

    @Test
    fun automaticExpansionDoesNotReplaceTheUsersCurrentDisclosure() {
        val state = CompactItemExpansionState()

        state.expandIfNone("goal:timer")
        state.expandIfNone("habit:timer")

        assertEquals("goal:timer", state.expandedItemKey)
    }
}
