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

        state.collapse("habit:2")
        assertEquals(emptySet<String>(), state.expandedItemKeys)

        state.collapse("habit:2")
        assertEquals(emptySet<String>(), state.expandedItemKeys)

        state.collapseAll()
        assertEquals(emptySet<String>(), state.expandedItemKeys)
    }

    @Test
    fun freshStateStartsCollapsedAndSavedDisclosuresCanBeRestored() {
        assertEquals(emptySet<String>(), CompactItemExpansionState().expandedItemKeys)
        assertEquals(
            setOf("task:1", "goal:timer", "habit:timer"),
            CompactItemExpansionState(setOf("task:1", "goal:timer", "habit:timer")).expandedItemKeys,
        )
    }
}
